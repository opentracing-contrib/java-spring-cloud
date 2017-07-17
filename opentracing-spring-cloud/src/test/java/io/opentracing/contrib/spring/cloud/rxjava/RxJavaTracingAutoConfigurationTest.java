package io.opentracing.contrib.spring.cloud.rxjava;

import static org.junit.Assert.assertEquals;

import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import rx.Observable;


@SpringBootTest(
    webEnvironment = WebEnvironment.MOCK,
    classes = {MockTracingConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class RxJavaTracingAutoConfigurationTest {

  @Autowired
  private MockTracer mockTracer;

  @Test
  public void test() {
    Observable<Integer> observable = Observable.range(1, 2)
        .map(integer -> integer * 3);

    observable.subscribe(System.out::println);

    List<MockSpan> spans = mockTracer.finishedSpans();
    assertEquals(2, spans.size());
  }
}