/**
 * Copyright 2017-2018 The OpenTracing Authors
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
package io.opentracing.contrib.spring.cloud.rxjava;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import io.opentracing.contrib.spring.cloud.rxjava.RxJavaTracingAutoConfigurationTest.TestController;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import rx.Observable;
import rx.Single;
import rx.schedulers.Schedulers;


@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    classes = {MockTracingConfiguration.class, TestController.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class RxJavaTracingAutoConfigurationTest {

  @RestController
  public static class TestController {

    @Autowired
    private MockTracer mockTracer;

    @RequestMapping(method = RequestMethod.GET, value = "/single")
    public Single<Integer> single() {
      return Observable.range(1, 10)
          .subscribeOn(Schedulers.io())
          .observeOn(Schedulers.computation())
          .map(x -> {
            // without enabled RxJava instrumentation active span will be null
            assertNotNull(mockTracer.activeSpan());
            mockTracer.activeSpan().setTag("rx", "rx");
            return x * 2;
          }).take(1).toSingle();
    }
  }

  @Autowired
  private MockTracer mockTracer;

  @Autowired
  private TestRestTemplate testRestTemplate;

  @Before
  public void before() {
    mockTracer.reset();
  }

  @Test
  public void testControllerTracing() throws Exception {
    ResponseEntity<Integer> responseEntity = testRestTemplate
        .getForEntity("/single", Integer.class);

    // span is created in spring-web
    await().until(() -> mockTracer.finishedSpans().size() == 1);
    assertEquals(200, responseEntity.getStatusCode().value());
    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(1, spans.size());
    assertEquals("rx", spans.get(0).tags().get("rx"));
  }
}