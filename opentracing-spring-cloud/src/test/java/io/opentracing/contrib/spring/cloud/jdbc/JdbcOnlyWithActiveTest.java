package io.opentracing.contrib.spring.cloud.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.opentracing.Scope;
import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;
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
 * Test behaviour when withActiveSpanOnly is set
 * @author Will Penington
 */
@SpringBootTest(classes = {MockTracingConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties = {
        "opentracing.spring.cloud.jdbc.withActiveSpanOnly=true"
})
public class JdbcOnlyWithActiveTest {

    @Autowired
    MockTracer tracer;

    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    JdbcAutoConfiguration jdbcAutoConfiguration;

    @Before
    public void before() {
        tracer.reset();
    }

    /**
     * Make sure that a span is created when an active span exists.
     */
    @Test
    public void spanJoinsActiveSpan() throws SQLException {
        try (Scope ignored = tracer.buildSpan("parent").startActive(true)) {
            assertTrue(dataSource.getConnection().prepareStatement("select 1").execute());
            assertEquals(1, tracer.finishedSpans().size());
        }

        assertEquals(2, tracer.finishedSpans().size());

        Optional<MockSpan> jdbcSpan = tracer
                .finishedSpans()
                .stream()
                .filter((s) -> "java-jdbc".equals(s.tags().get(Tags.COMPONENT.getKey())))
                .findFirst();

        assertTrue(jdbcSpan.isPresent());
    }

    /**
     * Make sure a new span is not created when there is no active parent span.
     */
    @Test
    public void spanIsCreatedForPreparedStatement() throws SQLException {
        PreparedStatement pstmt = dataSource.getConnection().prepareStatement("select 1");
        assertTrue(pstmt.execute());
        assertEquals(0, tracer.finishedSpans().size());
    }

}
