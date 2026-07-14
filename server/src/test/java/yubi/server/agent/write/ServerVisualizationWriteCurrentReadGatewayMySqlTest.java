package yubi.server.agent.write;

import com.mysql.cj.jdbc.MysqlDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import yubi.core.base.consts.Const;
import yubi.core.mappers.ext.DashboardMapperExt;
import yubi.core.mappers.ext.FolderMapperExt;
import yubi.core.mappers.ext.OrganizationMapperExt;
import yubi.core.mappers.ext.RelRoleResourceMapperExt;
import yubi.core.mappers.ext.RelRoleUserMapperExt;
import yubi.core.mappers.ext.RoleMapperExt;
import yubi.core.mappers.ext.ViewMapperExt;
import yubi.security.base.ResourceType;

import javax.sql.DataSource;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "YUBI_TEST_MYSQL_URL", matches = "jdbc:mysql:.*")
class ServerVisualizationWriteCurrentReadGatewayMySqlTest {

    private static final String ORGANIZATION_ID = "org-1";
    private static final String USER_ID = "user-1";
    private static final String DASHBOARD_ID = "dashboard-1";
    private static final String DASHBOARD_FOLDER_ID = "dashboard-folder-1";
    private static final String DASHBOARD_PARENT_ID = "dashboard-parent-1";
    private static final String ROLE_ID = "role-1";

    private String databaseName;
    private JdbcTemplate admin;
    private JdbcTemplate jdbc;
    private DataSourceTransactionManager transactionManager;
    private SqlSessionFactory sessionFactory;
    private SqlSessionTemplate sessions;
    private DashboardMapperExt dashboardMapper;
    private FolderMapperExt folderMapper;
    private RoleMapperExt roleMapper;
    private RelRoleUserMapperExt roleUserMapper;
    private RelRoleResourceMapperExt roleResourceMapper;
    private ServerVisualizationWriteCurrentReadGateway gateway;

    @BeforeEach
    void setUp() {
        String baseUrl = System.getenv("YUBI_TEST_MYSQL_URL");
        String username = environment("YUBI_TEST_MYSQL_USERNAME", "root");
        String password = environment("YUBI_TEST_MYSQL_PASSWORD", "");
        admin = new JdbcTemplate(dataSource(baseUrl, username, password));
        databaseName = "yubi_current_read_" + UUID.randomUUID().toString().replace("-", "");
        admin.execute("CREATE DATABASE `" + databaseName
                + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");

        DataSource dataSource = dataSource(databaseUrl(baseUrl, databaseName), username, password);
        jdbc = new JdbcTemplate(dataSource);
        transactionManager = new DataSourceTransactionManager(dataSource);
        assertEquals("REPEATABLE-READ", jdbc.queryForObject(
                "SELECT @@transaction_isolation", String.class));
        createSchema();
        seedData();

        sessionFactory = sessionFactory(dataSource);
        sessions = new SqlSessionTemplate(sessionFactory);
        ViewMapperExt viewMapper = sessions.getMapper(ViewMapperExt.class);
        folderMapper = sessions.getMapper(FolderMapperExt.class);
        dashboardMapper = sessions.getMapper(DashboardMapperExt.class);
        OrganizationMapperExt organizationMapper = sessions.getMapper(OrganizationMapperExt.class);
        roleMapper = sessions.getMapper(RoleMapperExt.class);
        roleUserMapper = sessions.getMapper(RelRoleUserMapperExt.class);
        roleResourceMapper = sessions.getMapper(RelRoleResourceMapperExt.class);
        gateway = new ServerVisualizationWriteCurrentReadGateway(viewMapper, folderMapper, dashboardMapper,
                organizationMapper, roleMapper, roleResourceMapper);
    }

    @AfterEach
    void tearDown() {
        if (admin != null && databaseName != null) {
            admin.execute("DROP DATABASE IF EXISTS `" + databaseName + "`");
        }
    }

    @Test
    void shouldBypassStaleVisualizationCacheAndKeepLaterOrdinaryReadsCurrent() {
        primeVisualizationCache("审批后名称");
        jdbc.update("UPDATE dashboard SET status=0 WHERE id=?", DASHBOARD_ID);
        jdbc.update("""
                INSERT INTO folder (id, name, org_id, rel_type, rel_id, parent_id, `index`)
                VALUES ('conflict-folder', '审批后名称', ?, 'DATACHART', 'chart-conflict', NULL, 0)
                """, ORGANIZATION_ID);

        try (SqlSession stale = sessionFactory.openSession()) {
            assertEquals(Const.DATA_STATUS_ACTIVE,
                    stale.getMapper(DashboardMapperExt.class).selectByPrimaryKey(DASHBOARD_ID).getStatus());
            assertTrue(stale.getMapper(FolderMapperExt.class)
                    .checkVizName(ORGANIZATION_ID, null, "审批后名称").isEmpty());
        }

        repeatableRead().executeWithoutResult(status -> {
            assertRepeatableRead();
            var locked = gateway.lockDashboardTarget(DASHBOARD_ID).orElseThrow();
            assertEquals(0, locked.dashboard().getStatus().intValue());
            assertTrue(gateway.nameExists(ORGANIZATION_ID, null, "审批后名称"));

            assertEquals(0, dashboardMapper.selectByPrimaryKey(DASHBOARD_ID).getStatus().intValue());
            assertEquals(1, folderMapper.checkVizName(
                    ORGANIZATION_ID, null, "审批后名称").size());
        });
    }

    @Test
    void shouldClearRoleResourceAndRoleUserSharedCacheBeforeAuthorizationRecheck() {
        primeAuthorizationCache();
        jdbc.update("DELETE FROM rel_role_resource WHERE role_id=?", ROLE_ID);
        jdbc.update("DELETE FROM rel_role_user WHERE role_id=? AND user_id=?", ROLE_ID, USER_ID);

        try (SqlSession stale = sessionFactory.openSession()) {
            assertEquals(1, stale.getMapper(RoleMapperExt.class)
                    .selectUserRoles(ORGANIZATION_ID, USER_ID).size());
            assertEquals(1, stale.getMapper(RelRoleResourceMapperExt.class)
                    .countRolePermission(DASHBOARD_FOLDER_ID, ROLE_ID));
            assertEquals(ROLE_ID, stale.getMapper(RelRoleUserMapperExt.class)
                    .selectByUserAndRole(USER_ID, ROLE_ID).getRoleId());
        }

        repeatableRead().executeWithoutResult(status -> {
            assertRepeatableRead();
            assertTrue(gateway.lockTrustedAuthorization(ORGANIZATION_ID, USER_ID));
            assertTrue(roleMapper.selectUserRoles(ORGANIZATION_ID, USER_ID).isEmpty());
            assertEquals(0, roleResourceMapper.countRolePermission(DASHBOARD_FOLDER_ID, ROLE_ID));
            assertEquals(null, roleUserMapper.selectByUserAndRole(USER_ID, ROLE_ID));
        });
    }

    @Test
    void shouldRejectMissingCurrentMembership() {
        jdbc.update("DELETE FROM rel_user_organization WHERE org_id=? AND user_id=?",
                ORGANIZATION_ID, USER_ID);

        repeatableRead().executeWithoutResult(status -> {
            assertRepeatableRead();
            assertFalse(gateway.lockTrustedAuthorization(ORGANIZATION_ID, USER_ID));
        });
    }

    @Test
    void shouldBlockExplicitPermissionDowngradeInsertedIntoMissingAuthorizationGap() throws Exception {
        jdbc.update("DELETE FROM rel_role_resource WHERE role_id=?", ROLE_ID);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch authorizationLocked = new CountDownLatch(1);
        CountDownLatch releaseApproval = new CountDownLatch(1);
        try {
            Future<?> approval = executor.submit(() -> repeatableRead().executeWithoutResult(status -> {
                assertRepeatableRead();
                assertTrue(gateway.lockTrustedAuthorization(ORGANIZATION_ID, USER_ID));
                authorizationLocked.countDown();
                await(releaseApproval);
            }));
            assertTrue(authorizationLocked.await(5, TimeUnit.SECONDS));

            Future<Integer> downgrade = executor.submit(() -> jdbc.update("""
                    INSERT INTO rel_role_resource
                      (id, role_id, resource_id, resource_type, org_id, permission)
                    VALUES ('explicit-downgrade', ?, ?, 'DASHBOARD', ?, 0)
                    """, ROLE_ID, DASHBOARD_FOLDER_ID, ORGANIZATION_ID));
            org.junit.jupiter.api.Assertions.assertThrows(TimeoutException.class,
                    () -> downgrade.get(300, TimeUnit.MILLISECONDS));

            releaseApproval.countDown();
            approval.get(5, TimeUnit.SECONDS);
            assertEquals(1, downgrade.get(5, TimeUnit.SECONDS));
        } finally {
            releaseApproval.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private void primeVisualizationCache(String candidateName) {
        try (SqlSession session = sessionFactory.openSession()) {
            assertEquals(Const.DATA_STATUS_ACTIVE,
                    session.getMapper(DashboardMapperExt.class).selectByPrimaryKey(DASHBOARD_ID).getStatus());
            assertEquals(DASHBOARD_FOLDER_ID, session.getMapper(FolderMapperExt.class)
                    .selectByRelTypeAndId(ResourceType.DASHBOARD.name(), DASHBOARD_ID).getId());
            assertTrue(session.getMapper(FolderMapperExt.class)
                    .checkVizName(ORGANIZATION_ID, null, candidateName).isEmpty());
            session.commit();
        }
    }

    private void primeAuthorizationCache() {
        try (SqlSession session = sessionFactory.openSession()) {
            assertEquals(1, session.getMapper(RoleMapperExt.class)
                    .selectUserRoles(ORGANIZATION_ID, USER_ID).size());
            assertEquals(1, session.getMapper(RelRoleResourceMapperExt.class)
                    .countRolePermission(DASHBOARD_FOLDER_ID, ROLE_ID));
            assertEquals(ROLE_ID, session.getMapper(RelRoleUserMapperExt.class)
                    .selectByUserAndRole(USER_ID, ROLE_ID).getRoleId());
            session.commit();
        }
    }

    private TransactionTemplate repeatableRead() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        return transaction;
    }

    private void assertRepeatableRead() {
        assertEquals("REPEATABLE-READ", jdbc.queryForObject(
                "SELECT @@transaction_isolation", String.class));
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("等待并发测试门闩超时");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待并发测试门闩被中断", exception);
        }
    }

    private SqlSessionFactory sessionFactory(DataSource dataSource) {
        Environment environment = new Environment("controlled-write-current-read",
                new SpringManagedTransactionFactory(), dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.setCacheEnabled(true);
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(ViewMapperExt.class);
        configuration.addMapper(FolderMapperExt.class);
        configuration.addMapper(DashboardMapperExt.class);
        configuration.addMapper(OrganizationMapperExt.class);
        configuration.addMapper(RoleMapperExt.class);
        configuration.addMapper(RelRoleUserMapperExt.class);
        configuration.addMapper(RelRoleResourceMapperExt.class);
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    private void createSchema() {
        jdbc.execute("""
                CREATE TABLE organization (
                  id varchar(64) PRIMARY KEY, name varchar(255), avatar varchar(255), description varchar(255),
                  create_time timestamp NULL, create_by varchar(64), update_time timestamp NULL,
                  update_by varchar(64)
                ) ENGINE=InnoDB
                """);
        jdbc.execute("""
                CREATE TABLE rel_user_organization (
                  id varchar(64) PRIMARY KEY, user_id varchar(64) NOT NULL, org_id varchar(64) NOT NULL,
                  UNIQUE KEY uq_membership (user_id, org_id)
                ) ENGINE=InnoDB
                """);
        jdbc.execute("""
                CREATE TABLE `view` (
                  id varchar(64) PRIMARY KEY, name varchar(255), description varchar(255), org_id varchar(64),
                  source_id varchar(64), script text, `type` varchar(64), model text, config text,
                  create_by varchar(64), create_time timestamp NULL, update_by varchar(64),
                  update_time timestamp NULL, parent_id varchar(64), is_folder tinyint, `index` double,
                  status tinyint
                ) ENGINE=InnoDB
                """);
        jdbc.execute("""
                CREATE TABLE dashboard (
                  id varchar(64) PRIMARY KEY, name varchar(255), org_id varchar(64), config text,
                  thumbnail varchar(255), create_by varchar(64), create_time timestamp NULL,
                  update_by varchar(64), update_time timestamp NULL, status tinyint
                ) ENGINE=InnoDB
                """);
        jdbc.execute("""
                CREATE TABLE folder (
                  id varchar(64) PRIMARY KEY, name varchar(255), org_id varchar(64), rel_type varchar(64),
                  sub_type varchar(64), rel_id varchar(64), avatar varchar(255), parent_id varchar(64),
                  `index` double, KEY idx_folder_relation (rel_type, rel_id),
                  KEY idx_folder_name (org_id, parent_id, name)
                ) ENGINE=InnoDB
                """);
        jdbc.execute("""
                CREATE TABLE role (
                  id varchar(64) PRIMARY KEY, org_id varchar(64), name varchar(255), type varchar(64),
                  description varchar(255), create_by varchar(64), create_time timestamp NULL,
                  update_by varchar(64), update_time timestamp NULL, avatar varchar(255)
                ) ENGINE=InnoDB
                """);
        jdbc.execute("""
                CREATE TABLE rel_role_user (
                  id varchar(64) PRIMARY KEY, user_id varchar(64), role_id varchar(64),
                  create_by varchar(64), create_time timestamp NULL,
                  KEY idx_role_user (role_id, user_id)
                ) ENGINE=InnoDB
                """);
        jdbc.execute("""
                CREATE TABLE rel_role_resource (
                  id varchar(64) PRIMARY KEY, role_id varchar(64), resource_id varchar(64),
                  resource_type varchar(64), org_id varchar(64), permission int,
                  create_by varchar(64), create_time timestamp NULL, update_by varchar(64),
                  update_time timestamp NULL,
                  UNIQUE KEY role_id_2 (role_id, resource_id, resource_type),
                  KEY role_id (role_id), KEY resource_id (resource_id),
                  KEY resource_type (resource_type), KEY org_id (org_id)
                ) ENGINE=InnoDB
                """);
    }

    private void seedData() {
        jdbc.update("INSERT INTO organization (id, name) VALUES (?, '测试组织')", ORGANIZATION_ID);
        jdbc.update("INSERT INTO rel_user_organization (id, user_id, org_id) VALUES ('membership-1', ?, ?)",
                USER_ID, ORGANIZATION_ID);
        jdbc.update("INSERT INTO `view` (id, name, org_id, status) VALUES ('view-1', '测试视图', ?, 1)",
                ORGANIZATION_ID);
        jdbc.update("INSERT INTO dashboard (id, name, org_id, config, status) VALUES (?, '原仪表盘', ?, '{}', 1)",
                DASHBOARD_ID, ORGANIZATION_ID);
        jdbc.update("""
                INSERT INTO folder (id, name, org_id, rel_type, rel_id, parent_id, `index`)
                VALUES (?, '仪表盘目录', ?, 'FOLDER', NULL, NULL, 0),
                       (?, '原仪表盘', ?, 'DASHBOARD', ?, ?, 1)
                """, DASHBOARD_PARENT_ID, ORGANIZATION_ID, DASHBOARD_FOLDER_ID,
                ORGANIZATION_ID, DASHBOARD_ID, DASHBOARD_PARENT_ID);
        jdbc.update("INSERT INTO role (id, org_id, name, type) VALUES (?, ?, '测试角色', 'NORMAL')",
                ROLE_ID, ORGANIZATION_ID);
        jdbc.update("INSERT INTO rel_role_user (id, user_id, role_id) VALUES ('role-user-1', ?, ?)",
                USER_ID, ROLE_ID);
        jdbc.update("""
                INSERT INTO rel_role_resource (id, role_id, resource_id, resource_type, org_id, permission)
                VALUES ('grant-1', ?, ?, 'DASHBOARD', ?, ?)
                """, ROLE_ID, DASHBOARD_FOLDER_ID, ORGANIZATION_ID, Const.MANAGE);
    }

    private MysqlDataSource dataSource(String url, String username, String password) {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setURL(url);
        dataSource.setUser(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    private String databaseUrl(String baseUrl, String database) {
        int query = baseUrl.indexOf('?');
        String prefix = query < 0 ? baseUrl : baseUrl.substring(0, query);
        String suffix = query < 0 ? "" : baseUrl.substring(query);
        return (prefix.endsWith("/") ? prefix : prefix + "/") + database + suffix;
    }

    private String environment(String name, String fallback) {
        String value = System.getenv(name);
        return value == null ? fallback : value;
    }
}
