package io.opentracing.contrib.spring.cloud.rxjava;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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


@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    classes = {MockTracingConfiguration.class, TestController.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class RxJavaTracingAutoConfigurationTest {

  @RestController
  public static class TestController {

    @RequestMapping(method = RequestMethod.GET, value = "/single")
    public Single<String> single() {
      return Single.just("single value");
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
  public void testControllerTracing() {
    ResponseEntity<String> responseEntity = testRestTemplate.getForEntity("/single", String.class);

    await().until(() -> mockTracer.finishedSpans().size() == 2);
    assertEquals(200, responseEntity.getStatusCode().value());
    assertEquals(2, mockTracer.finishedSpans().size());

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertTrue(spans.get(1).parentId() == spans.get(0).context().spanId()
        || spans.get(0).parentId() == spans.get(1).context().spanId());
  }

  @Test
  public void testWithoutController() {
    Observable<Integer> observable = Observable.range(1, 2)
        .map(integer -> integer * 3);

    observable.subscribe(System.out::println);

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(2, spans.size());
  }
}
