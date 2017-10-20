/**
 * Copyright 2017 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.opentracing.contrib.spring.cloud.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {MockTracingConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase
public class SpringDataTracingTest {

  @Autowired
  private TestEntityRepository testEntityRepository;

  @Autowired
  private MockTracer mockTracer;

  @Autowired
  private TestRestTemplate testRestTemplate;

  @Before
  public void clearGlobalTracer() {
    mockTracer.reset();
  }

  @Test
  public void jpaTest() {
    TestEntity entity = testEntityRepository.save(new TestEntity());
    assertNotNull(entity.getId());

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(1, spans.size());
    assertEquals("java-jdbc", spans.get(0).tags().get(Tags.COMPONENT.getKey()));
  }

  @Test
  public void dataRestTest() {
    ResponseEntity<String> responseEntity = testRestTemplate
        .getForEntity("/test-entities/1", String.class);
    assertEquals(200, responseEntity.getStatusCode().value());

    List<MockSpan> spans = mockTracer.finishedSpans();
    // One span from java-web-servlet
    // One span from java-jdbc
    assertEquals(2, spans.size());

    assertEquals("java-jdbc", spans.get(0).tags().get(Tags.COMPONENT.getKey()));
    assertEquals("java-web-servlet", spans.get(1).tags().get(Tags.COMPONENT.getKey()));
  }

}
