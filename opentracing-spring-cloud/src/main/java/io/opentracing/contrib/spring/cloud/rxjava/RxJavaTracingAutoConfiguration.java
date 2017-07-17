package io.opentracing.contrib.spring.cloud.rxjava;


import io.opentracing.Tracer;
import io.opentracing.rxjava.TracingRxJavaUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RxJavaTracingAutoConfiguration {

  @Autowired
  public RxJavaTracingAutoConfiguration(Tracer tracer) {
    TracingRxJavaUtils.enableTracing(tracer);
  }
}
