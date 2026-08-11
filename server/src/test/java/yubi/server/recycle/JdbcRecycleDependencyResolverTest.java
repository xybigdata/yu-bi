package yubi.server.recycle;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JdbcRecycleDependencyResolverTest {

    @Test
    void shouldResolveCrossModuleDependencyChainAndHideUnreadableDetails() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:recycle_dependencies;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        createSchema(jdbc);
        jdbc.update("INSERT INTO `view` VALUES (?, ?, ?, ?, ?, ?, ?)",
                "view-1", "订单视图", "org-1", "source-1", "view-folder", "owner-view", 1);
        jdbc.update("INSERT INTO datachart VALUES (?, ?, ?, ?, ?, ?)",
                "chart-1", "订单图表", "org-1", "view-1", "owner-chart", 1);
        jdbc.update("INSERT INTO dashboard VALUES (?, ?, ?, ?, ?)",
                "dashboard-1", "经营看板", "org-1", "owner-dashboard", 1);
        jdbc.update("INSERT INTO widget VALUES (?, ?)", "widget-1", "dashboard-1");
        jdbc.update("INSERT INTO rel_widget_element VALUES (?, ?, ?, ?)",
                "relation-1", "widget-1", "DATACHART", "chart-1");
        jdbc.update("INSERT INTO storyboard VALUES (?, ?, ?, ?, ?)",
                "story-1", "经营故事", "org-1", "owner-story", 1);
        jdbc.update("INSERT INTO storypage VALUES (?, ?, ?, ?)",
                "page-1", "story-1", "DASHBOARD", "dashboard-1");
        RecycleDependencyResolver resolver = new JdbcRecycleDependencyResolver(
                jdbc, (access, type, id) -> !id.equals("dashboard-1"));

        List<RecycleDependency> dependencies = resolver.find(
                RecycleAccess.authenticated("user-1", "org-1", false),
                RecycleResourceType.SOURCE,
                "source-1");

        assertEquals(List.of(
                        "VIEW:DIRECT:view-1",
                        "DATACHART:INDIRECT:chart-1",
                        "DASHBOARD:INDIRECT:dashboard-1",
                        "STORYBOARD:INDIRECT:story-1"),
                dependencies.stream()
                        .map(item -> item.type() + ":" + item.depth() + ":" + item.id())
                        .toList());
        assertEquals("订单视图", dependencies.getFirst().name());
        assertEquals("view-folder", dependencies.getFirst().location());
        assertEquals("owner-view", dependencies.getFirst().ownerId());
        assertEquals("/organizations/org-1/views/view-1", dependencies.getFirst().route());
        assertNull(dependencies.get(2).name());
        assertNull(dependencies.get(2).location());
        assertNull(dependencies.get(2).ownerId());
        assertNull(dependencies.get(2).route());
        assertNull(dependencies.get(3).location());
    }

    private void createSchema(JdbcTemplate jdbc) {
        jdbc.execute("CREATE TABLE `view` (id varchar, name varchar, org_id varchar, "
                + "source_id varchar, parent_id varchar, create_by varchar, status int)");
        jdbc.execute("CREATE TABLE datachart (id varchar, name varchar, org_id varchar, "
                + "view_id varchar, create_by varchar, status int)");
        jdbc.execute("CREATE TABLE dashboard (id varchar, name varchar, org_id varchar, "
                + "create_by varchar, status int)");
        jdbc.execute("CREATE TABLE widget (id varchar, dashboard_id varchar)");
        jdbc.execute("CREATE TABLE rel_widget_element (id varchar, widget_id varchar, "
                + "rel_type varchar, rel_id varchar)");
        jdbc.execute("CREATE TABLE storyboard (id varchar, name varchar, org_id varchar, "
                + "create_by varchar, status int)");
        jdbc.execute("CREATE TABLE storypage (id varchar, storyboard_id varchar, "
                + "rel_type varchar, rel_id varchar)");
    }
}
