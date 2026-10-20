package com.summa.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pass 348's degraded-503 health contract: when the database or the git
 * store check is DOWN, `/health` answers 503 (not 200) so container
 * healthchecks and load balancers fail fast instead of routing to a
 * degraded service.
 */
class HealthControllerTest {

    @TempDir
    Path tempDir;

    @Test
    void health_whenAllUp_returns200() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        HealthController controller = controller(jdbc, tempDir);

        var res = controller.health();

        assertEquals(HttpStatus.OK, res.getStatusCode());
        Map<String, Object> body = res.getBody();
        assertNotNull(body);
        assertEquals("UP", body.get("status"));
        assertEquals(Map.of("database", "UP", "git_store", "UP"), body.get("checks"));
    }

    @Test
    void health_whenDatabaseDown_returns503Degraded() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject("SELECT 1", Integer.class)).thenThrow(new RuntimeException("no db"));
        HealthController controller = controller(jdbc, tempDir);

        var res = controller.health();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, res.getStatusCode());
        assertEquals("DEGRADED", res.getBody().get("status"));
    }

    @Test
    void health_whenGitStoreDown_returns503Degraded() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        Path missing = tempDir.resolve("does-not-exist");
        HealthController controller = controller(jdbc, missing);

        var res = controller.health();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, res.getStatusCode());
        assertEquals("DEGRADED", res.getBody().get("status"));
    }

    private HealthController controller(JdbcTemplate jdbc, Path gitRepoPath) {
        HealthController controller = new HealthController(jdbc);
        setField(controller, "mode", "single-process");
        setField(controller, "dnaRepoPath", gitRepoPath.toString());
        return controller;
    }

    private static void setField(Object target, String name, Object value) {
        try {
            java.lang.reflect.Field f = HealthController.class.getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
