package io.opentracing.contrib.spring.cloud.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import io.opentracing.mock.MockTracer;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test behaviour when ignored statements are configured
 * @author Will Penington
 */
@SpringBootTest(classes = {MockTracingConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties = {
        "opentracing.spring.cloud.jdbc.ignoreStatements=SELECT 1"
})
public class JdbcIgnoredStatements {

    @Autowired
    MockTracer tracer;

    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Before
    public void before() {
        tracer.reset();
    }

    /**
     * Make sure ignored statements aren't traced
     */
    @Test
    public void spanIsNotCreatedForIgnoredStatement() throws SQLException {
        PreparedStatement pstmt = dataSource.getConnection().prepareStatement("SELECT 1");
        assertTrue(pstmt.execute());
        assertEquals(0, tracer.finishedSpans().size());
    }

    /**
     * Make sure statements that aren't ignored aren't affected
     */
    @Test
    public void spanIsCreatedNonIgnoredStatement() throws SQLException {
        PreparedStatement pstmt = dataSource.getConnection().prepareStatement("SELECT 2");
        assertTrue(pstmt.execute());
        assertEquals(1, tracer.finishedSpans().size());
    }
}
