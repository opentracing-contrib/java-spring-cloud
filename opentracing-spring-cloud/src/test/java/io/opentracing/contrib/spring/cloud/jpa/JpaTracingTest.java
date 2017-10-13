package io.opentracing.contrib.spring.cloud.jpa;

import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {MockTracingConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureTestDatabase
public class JpaTracingTest {

    @Autowired
    private TestEntityRepository testEntityRepository;

    @Autowired
    private MockTracer mockTracer;

    @Test
    public void jpaTest() {
        TestEntity entity = testEntityRepository.save(new TestEntity());
        assertNotNull(entity.getId());

        List<MockSpan> spans = mockTracer.finishedSpans();
        assertEquals(1, spans.size());
        assertEquals("java-jdbc", spans.get(0).tags().get(Tags.COMPONENT.getKey()));
    }

}
