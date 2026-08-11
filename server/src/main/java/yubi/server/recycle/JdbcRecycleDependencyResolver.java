package yubi.server.recycle;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

@Component
final class JdbcRecycleDependencyResolver implements RecycleDependencyResolver {

    private final JdbcTemplate jdbc;
    private final RecycleDependencyReadAccess readAccess;

    JdbcRecycleDependencyResolver(JdbcTemplate jdbc,
                                  RecycleDependencyReadAccess readAccess) {
        this.jdbc = Objects.requireNonNull(jdbc, "JdbcTemplate 不能为空");
        this.readAccess = Objects.requireNonNull(readAccess, "依赖读取权限不能为空");
    }

    @Override
    public List<RecycleDependency> find(RecycleAccess access,
                                        RecycleResourceType resourceType,
                                        String resourceId) {
        Map<String, DependencyNode> found = new LinkedHashMap<>();
        Queue<DependencyNode> pending = new ArrayDeque<>();
        direct(access.organizationId(), resourceType, resourceId).forEach(node ->
                pending.add(node.withDepth(RecycleDependencyDepth.DIRECT)));
        while (!pending.isEmpty()) {
            DependencyNode current = pending.remove();
            String key = current.type() + ":" + current.id();
            if (found.putIfAbsent(key, current) != null) {
                continue;
            }
            direct(access.organizationId(), current.type(), current.id()).forEach(node ->
                    pending.add(node.withDepth(RecycleDependencyDepth.INDIRECT)));
        }
        return found.values().stream()
                .map(node -> toDependency(access, node))
                .toList();
    }

    private List<DependencyNode> direct(String organizationId,
                                        RecycleResourceType type,
                                        String resourceId) {
        return switch (type) {
            case SOURCE -> query("""
                    SELECT id, name, parent_id location, create_by owner_id
                    FROM `view`
                    WHERE org_id = ? AND source_id = ? AND status != 0
                    ORDER BY id
                    """, organizationId, resourceId, RecycleResourceType.VIEW);
            case VIEW -> combine(
                    query("""
                            SELECT id, name, NULL location, create_by owner_id
                            FROM datachart
                            WHERE org_id = ? AND view_id = ? AND status != 0
                            ORDER BY id
                            """, organizationId, resourceId, RecycleResourceType.DATACHART),
                    dashboards(organizationId, "VIEW", resourceId));
            case DATACHART -> dashboards(organizationId, "DATACHART", resourceId);
            case DASHBOARD -> storyboards(organizationId, resourceId);
            case SCHEDULE, STORYBOARD -> List.of();
        };
    }

    private List<DependencyNode> storyboards(String organizationId,
                                             String dashboardId) {
        List<String> storyboardIds = jdbc.queryForList("""
                SELECT DISTINCT storyboard_id
                FROM storypage
                WHERE rel_type = 'DASHBOARD' AND rel_id = ?
                ORDER BY storyboard_id
                """, String.class, dashboardId);
        List<DependencyNode> result = new ArrayList<>();
        for (String storyboardId : storyboardIds) {
            result.addAll(query("""
                    SELECT id, name, NULL location, create_by owner_id
                    FROM storyboard
                    WHERE org_id = ? AND id = ? AND status != 0
                    """, organizationId, storyboardId, RecycleResourceType.STORYBOARD));
        }
        return result;
    }

    private List<DependencyNode> dashboards(String organizationId,
                                            String relationType,
                                            String resourceId) {
        return query("""
                SELECT DISTINCT d.id, d.name, NULL location, d.create_by owner_id
                FROM rel_widget_element rwe
                JOIN widget w ON w.id = rwe.widget_id
                JOIN dashboard d ON d.id = w.dashboard_id
                WHERE d.org_id = ? AND rwe.rel_type = ? AND rwe.rel_id = ?
                  AND d.status != 0
                ORDER BY d.id
                """, organizationId, relationType, resourceId,
                RecycleResourceType.DASHBOARD);
    }

    private List<DependencyNode> query(String sql,
                                       String organizationId,
                                       String resourceId,
                                       RecycleResourceType type) {
        return jdbc.query(sql, (resultSet, rowNumber) -> new DependencyNode(
                        type, resultSet.getString("id"), resultSet.getString("name"),
                        resultSet.getString("location"), resultSet.getString("owner_id"),
                        RecycleDependencyDepth.DIRECT),
                organizationId, resourceId);
    }

    private List<DependencyNode> query(String sql,
                                       String organizationId,
                                       String relationType,
                                       String resourceId,
                                       RecycleResourceType type) {
        return jdbc.query(sql, (resultSet, rowNumber) -> new DependencyNode(
                        type, resultSet.getString("id"), resultSet.getString("name"),
                        resultSet.getString("location"), resultSet.getString("owner_id"),
                        RecycleDependencyDepth.DIRECT),
                organizationId, relationType, resourceId);
    }

    private RecycleDependency toDependency(RecycleAccess access, DependencyNode node) {
        boolean readable = readAccess.canRead(access, node.type(), node.id());
        return new RecycleDependency(
                node.id(), node.name(), node.type(), node.depth(), readable,
                node.location(), node.ownerId(), route(access.organizationId(), node));
    }

    private String route(String organizationId, DependencyNode node) {
        return switch (node.type()) {
            case SOURCE -> "/organizations/" + organizationId + "/sources/" + node.id();
            case VIEW -> "/organizations/" + organizationId + "/views/" + node.id();
            case SCHEDULE -> "/organizations/" + organizationId + "/schedules/" + node.id();
            case DATACHART, DASHBOARD, STORYBOARD ->
                    "/organizations/" + organizationId + "/vizs/" + node.id();
        };
    }

    @SafeVarargs
    private final List<DependencyNode> combine(List<DependencyNode>... groups) {
        List<DependencyNode> result = new ArrayList<>();
        for (List<DependencyNode> group : groups) {
            result.addAll(group);
        }
        return result;
    }

    private record DependencyNode(RecycleResourceType type,
                                  String id,
                                  String name,
                                  String location,
                                  String ownerId,
                                  RecycleDependencyDepth depth) {

        DependencyNode withDepth(RecycleDependencyDepth nextDepth) {
            return new DependencyNode(type, id, name, location, ownerId, nextDepth);
        }
    }
}
