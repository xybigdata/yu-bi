/*
 * YuBi
 * <p>
 * Copyright 2021 (originally Datart by running-elephant)
 * Copyright 2024-2026 YuBi Contributors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package yubi.server.validation;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

public final class DemoDownloadSchemaCheck {

    private static final List<ColumnSpec> REQUIRED_COLUMNS = List.of(
            new ColumnSpec("id", "varchar_ignorecase", 32L, null, false, null),
            new ColumnSpec("name", "varchar_ignorecase", 255L, null, false, null),
            new ColumnSpec("path", "varchar_ignorecase", 512L, null, true, null),
            new ColumnSpec("last_download_time", "timestamp", null, 0, true, null),
            new ColumnSpec("create_time", "timestamp", null, 0, true, null),
            new ColumnSpec("create_by", "varchar_ignorecase", 128L, null, false, null),
            new ColumnSpec("status", "tinyint", null, null, false, null),
            new ColumnSpec("owner_type", "varchar_ignorecase", 24L, null, false, null),
            new ColumnSpec("owner_id", "varchar_ignorecase", 64L, null, true, null),
            new ColumnSpec("share_id", "varchar_ignorecase", 32L, null, true, null),
            new ColumnSpec("failure_code", "varchar_ignorecase", 32L, null, true, null),
            new ColumnSpec("deleted_at", "timestamp", null, 3, true, null)
    );
    private static final Set<String> REQUIRED_CONSTRAINTS = Set.of(
            "chk_download_owner_scope",
            "chk_download_failure_code"
    );
    private static final List<String> ALLOWED_FAILURE_CODES = List.of(
            "PERMISSION_DENIED",
            "DEFINITION_INVALID",
            "QUERY_FAILED",
            "FILE_GENERATION_FAILED",
            "TASK_INTERRUPTED",
            "INTERNAL_FAILURE"
    );
    private static final Map<String, List<IndexColumnSpec>> REQUIRED_INDEXES = requiredIndexes();

    private DemoDownloadSchemaCheck() {
    }

    public static void main(String[] args) {
        if (args.length != 1 || args[0] == null || !args[0].startsWith("jdbc:h2:file:")) {
            throw new IllegalArgumentException("demo 下载 Schema 校验参数无效");
        }
        try (Connection connection = DriverManager.getConnection(args[0], "", "")) {
            verify(connection);
            System.out.println("demo 下载任务安全 Schema 校验通过");
        } catch (SQLException exception) {
            throw new IllegalStateException("demo 下载任务安全 Schema 校验失败");
        }
    }

    public static void verify(Connection connection) throws SQLException {
        requireColumns(connection);
        requirePrimaryKey(connection);
        requireValues(connection, """
                SELECT constraint_name
                FROM information_schema.table_constraints
                WHERE table_name = 'download'
                  AND constraint_type = 'CHECK'
                  AND constraint_name IN ('chk_download_owner_scope', 'chk_download_failure_code')
                """, REQUIRED_CONSTRAINTS, "demo download constraints");
        requireConstraintDefinitions(connection);
        requireIndexes(connection);
        requireZero(connection, """
                SELECT COUNT(*)
                FROM download
                WHERE owner_type IS NULL
                   OR (owner_type = 'LEGACY_INACCESSIBLE'
                       AND (owner_id IS NOT NULL OR share_id IS NOT NULL))
                """);
        requireConstraintBehavior(connection);
        requireStableCreateTime(connection);
    }

    private static void requirePrimaryKey(Connection connection) throws SQLException {
        List<String> names = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                    SELECT constraint_name
                    FROM information_schema.table_constraints
                    WHERE table_name = 'download' AND constraint_type = 'PRIMARY KEY'
                    """)) {
            while (resultSet.next()) {
                names.add(resultSet.getString(1));
            }
        }
        if (names.size() != 1) {
            throw new IllegalStateException("demo download primary key unavailable");
        }
        List<String> columns = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT column_name
                FROM information_schema.key_column_usage
                WHERE table_name = 'download' AND constraint_name = ?
                ORDER BY ordinal_position
                """)) {
            statement.setString(1, names.getFirst());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    columns.add(resultSet.getString(1).toLowerCase());
                }
            }
        }
        if (!columns.equals(List.of("id"))) {
            throw new IllegalStateException("demo download primary key unavailable");
        }
    }

    private static void requireColumns(Connection connection) throws SQLException {
        List<ColumnSpec> actual = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                SELECT column_name, data_type, character_maximum_length,
                       datetime_precision, is_nullable, column_default
                FROM information_schema.columns
                WHERE table_name = 'download'
                ORDER BY ordinal_position
                """)) {
            while (resultSet.next()) {
                actual.add(new ColumnSpec(
                        resultSet.getString("column_name").toLowerCase(),
                        resultSet.getString("data_type").toLowerCase(),
                        nullableLong(resultSet, "character_maximum_length"),
                        nullableInteger(resultSet, "datetime_precision"),
                        "YES".equalsIgnoreCase(resultSet.getString("is_nullable")),
                        normalizeDefault(resultSet.getString("column_default"))
                ));
            }
        }
        if (!actual.equals(REQUIRED_COLUMNS)) {
            throw new IllegalStateException("demo download columns unavailable");
        }
    }

    private static void requireIndexes(Connection connection) throws SQLException {
        Map<String, List<IndexColumnSpec>> actual = new LinkedHashMap<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                SELECT index_name, column_name, ordering_specification
                FROM information_schema.index_columns
                WHERE table_name = 'download'
                  AND index_name IN ('idx_download_authenticated_scope',
                                     'idx_download_share_scope', 'idx_download_cleanup')
                ORDER BY index_name, ordinal_position
                """)) {
            while (resultSet.next()) {
                String indexName = resultSet.getString("index_name").toLowerCase();
                actual.computeIfAbsent(indexName, ignored -> new ArrayList<>())
                        .add(new IndexColumnSpec(
                                resultSet.getString("column_name").toLowerCase(),
                                resultSet.getString("ordering_specification").toUpperCase()
                        ));
            }
        }
        if (!actual.equals(REQUIRED_INDEXES)) {
            throw new IllegalStateException("demo download indexes unavailable");
        }
    }

    private static void requireValues(Connection connection,
                                      String sql,
                                      Set<String> expected,
                                      String contract) throws SQLException {
        Set<String> actual = new TreeSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                actual.add(resultSet.getString(1).toLowerCase());
            }
        }
        if (!actual.equals(expected)) {
            throw new IllegalStateException(contract + " unavailable");
        }
    }

    private static void requireZero(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            if (!resultSet.next() || resultSet.getInt(1) != 0) {
                throw new IllegalStateException("demo legacy download ownership is unsafe");
            }
        }
    }

    static void requireConstraintBehavior(Connection connection) throws SQLException {
        requireOwnerScopeMatrix(connection);
        requireFailureCodeMatrix(connection);
    }

    private static void requireConstraintDefinitions(Connection connection) throws SQLException {
        Map<String, String> clauses = new LinkedHashMap<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                    SELECT constraint_name, check_clause
                    FROM information_schema.check_constraints
                    WHERE constraint_name IN ('chk_download_owner_scope', 'chk_download_failure_code')
                    """)) {
            while (resultSet.next()) {
                clauses.put(resultSet.getString(1).toLowerCase(), normalizeClause(resultSet.getString(2)));
            }
        }
        String owner = clauses.get("chk_download_owner_scope");
        if (owner == null
                || !owner.contains("owner_type=cast('authenticated'")
                || !owner.contains("owner_type=cast('share'")
                || !owner.contains("owner_type=cast('legacy_inaccessible'")
                || occurrences(owner, "owner_idisnotnull") != 2
                || occurrences(owner, "owner_idisnull") != 1
                || occurrences(owner, "share_idisnotnull") != 1
                || occurrences(owner, "share_idisnull") != 2) {
            throw new IllegalStateException("demo download owner constraint unavailable");
        }
        String failure = clauses.get("chk_download_failure_code");
        if (failure == null || !failure.contains("failure_codeisnull")) {
            throw new IllegalStateException("demo download failure constraint unavailable");
        }
        for (String code : ALLOWED_FAILURE_CODES) {
            if (!failure.contains("'" + code.toLowerCase() + "'")) {
                throw new IllegalStateException("demo download failure constraint unavailable");
            }
        }
    }

    private static void requireOwnerScopeMatrix(Connection connection) throws SQLException {
        requireAccepted(connection, "AUTHENTICATED", "schema-probe", null, null);
        requireAccepted(connection, "SHARE", "a".repeat(64), "schema-share", null);
        requireAccepted(connection, "LEGACY_INACCESSIBLE", null, null, null);

        requireCheckViolation(connection, "AUTHENTICATED", null, null, null, "chk_download_owner_scope");
        requireCheckViolation(connection, "AUTHENTICATED", "schema-probe", "schema-share", null,
                "chk_download_owner_scope");
        requireCheckViolation(connection, "SHARE", null, "schema-share", null, "chk_download_owner_scope");
        requireCheckViolation(connection, "SHARE", "schema-digest", null, null, "chk_download_owner_scope");
        requireCheckViolation(connection, "LEGACY_INACCESSIBLE", "schema-probe", null, null,
                "chk_download_owner_scope");
        requireCheckViolation(connection, "LEGACY_INACCESSIBLE", null, "schema-share", null,
                "chk_download_owner_scope");
        requireCheckViolation(connection, "EVIL", "schema-probe", null, null, "chk_download_owner_scope");
    }

    private static void requireFailureCodeMatrix(Connection connection) throws SQLException {
        requireAccepted(connection, "AUTHENTICATED", "schema-probe", null, null);
        for (String failureCode : ALLOWED_FAILURE_CODES) {
            requireAccepted(connection, "AUTHENTICATED", "schema-probe", null, failureCode);
        }
        requireCheckViolation(connection, "AUTHENTICATED", "schema-probe", null, "RAW_EXCEPTION",
                "chk_download_failure_code");
        requireCheckViolation(connection, "AUTHENTICATED", "schema-probe", null, "ANY_CODE",
                "chk_download_failure_code");
    }

    private static void requireAccepted(Connection connection,
                                        String ownerType,
                                        String ownerId,
                                        String shareId,
                                        String failureCode) throws SQLException {
        String id = null;
        try {
            id = insertProbe(connection, ownerType, ownerId, shareId, failureCode);
        } catch (SQLException exception) {
            String value = failureCode == null ? ownerType : failureCode;
            throw new IllegalStateException(
                    "demo download constraint rejected supported value "
                            + value + " (SQL error " + exception.getErrorCode() + ")"
            );
        } finally {
            if (id != null) {
                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM download WHERE id = ?")) {
                    statement.setString(1, id);
                    statement.executeUpdate();
                }
            }
        }
    }

    private static void requireCheckViolation(Connection connection,
                                              String ownerType,
                                              String ownerId,
                                              String shareId,
                                              String failureCode,
                                              String constraintName) throws SQLException {
        boolean rejected = false;
        String id = null;
        try {
            id = insertProbe(connection, ownerType, ownerId, shareId, failureCode);
        } catch (SQLException exception) {
            if (exception instanceof SQLIntegrityConstraintViolationException) {
                rejected = true;
            } else {
                throw new IllegalStateException(
                        "demo download constraint " + constraintName + " unavailable"
                );
            }
        } finally {
            if (id != null) {
                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM download WHERE id = ?")) {
                    statement.setString(1, id);
                    statement.executeUpdate();
                }
            }
        }
        if (!rejected) {
            throw new IllegalStateException(
                    "demo download constraint " + constraintName + " unavailable"
            );
        }
    }

    private static String insertProbe(Connection connection,
                                      String ownerType,
                                      String ownerId,
                                      String shareId,
                                      String failureCode) throws SQLException {
        String id = UUID.randomUUID().toString().replace("-", "");
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO download
                    (id, name, create_time, create_by, status,
                     owner_type, owner_id, share_id, failure_code)
                VALUES (?, 'schema-probe', CURRENT_TIMESTAMP, 'schema-probe', 0,
                        ?, ?, ?, ?)
                """)) {
            statement.setString(1, id);
            statement.setString(2, ownerType);
            statement.setString(3, ownerId);
            statement.setString(4, shareId);
            statement.setString(5, failureCode);
            statement.executeUpdate();
        }
        return id;
    }

    private static void requireStableCreateTime(Connection connection) throws SQLException {
        String id = UUID.randomUUID().toString().replace("-", "");
        Timestamp expected = Timestamp.valueOf("2001-02-03 04:05:06");
        try {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO download
                        (id, name, create_time, create_by, status, owner_type, owner_id)
                    VALUES (?, 'schema-probe', ?, 'schema-probe', 0, 'AUTHENTICATED', 'schema-probe')
                    """)) {
                statement.setString(1, id);
                statement.setTimestamp(2, expected);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE download SET status = 1 WHERE id = ?"
            )) {
                statement.setString(1, id);
                if (statement.executeUpdate() != 1) {
                    throw new IllegalStateException("demo download create_time semantics unavailable");
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT create_time FROM download WHERE id = ?"
            )) {
                statement.setString(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next() || !expected.equals(resultSet.getTimestamp(1))) {
                        throw new IllegalStateException("demo download create_time semantics unavailable");
                    }
                }
            }
        } finally {
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM download WHERE id = ?")) {
                statement.setString(1, id);
                statement.executeUpdate();
            }
        }
    }

    private static Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private static Integer nullableInteger(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }

    private static String normalizeDefault(String value) {
        return value == null || "NULL".equalsIgnoreCase(value) ? null : value;
    }

    private static String normalizeClause(String value) {
        return value == null ? "" : value.toLowerCase().replace("\"", "").replaceAll("\\s+", "");
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        int index = value.indexOf(needle);
        while (index >= 0) {
            count++;
            index = value.indexOf(needle, index + needle.length());
        }
        return count;
    }

    private static Map<String, List<IndexColumnSpec>> requiredIndexes() {
        Map<String, List<IndexColumnSpec>> indexes = new LinkedHashMap<>();
        indexes.put("idx_download_authenticated_scope", List.of(
                new IndexColumnSpec("owner_type", "ASC"),
                new IndexColumnSpec("owner_id", "ASC"),
                new IndexColumnSpec("deleted_at", "ASC"),
                new IndexColumnSpec("create_time", "ASC")
        ));
        indexes.put("idx_download_cleanup", List.of(
                new IndexColumnSpec("deleted_at", "ASC"),
                new IndexColumnSpec("create_time", "ASC")
        ));
        indexes.put("idx_download_share_scope", List.of(
                new IndexColumnSpec("owner_type", "ASC"),
                new IndexColumnSpec("share_id", "ASC"),
                new IndexColumnSpec("owner_id", "ASC"),
                new IndexColumnSpec("deleted_at", "ASC"),
                new IndexColumnSpec("create_time", "ASC")
        ));
        return indexes;
    }

    private record ColumnSpec(String name,
                              String dataType,
                              Long characterLength,
                              Integer datetimePrecision,
                              boolean nullable,
                              String defaultValue) {
    }

    private record IndexColumnSpec(String name, String order) {
    }
}
