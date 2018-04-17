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

package io.opentracing.contrib.spring.cloud.starter.zipkin;

import brave.Tracing;
import brave.opentracing.BraveTracer;
import brave.sampler.BoundarySampler;
import brave.sampler.CountingSampler;
import brave.sampler.Sampler;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zipkin2.Span;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.Sender;
import zipkin2.reporter.okhttp3.OkHttpSender;

@Configuration
@ConditionalOnClass(brave.opentracing.BraveTracer.class)
@ConditionalOnMissingBean(io.opentracing.Tracer.class)
@ConditionalOnProperty(value = "opentracing.zipkin.enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureAfter(name = "me.snowdrop.opentracing.tracer.jaeger.JaegerAutoConfiguration")
@AutoConfigureBefore(name = "io.opentracing.contrib.spring.web.autoconfig.TracerAutoConfiguration")
@EnableConfigurationProperties(ZipkinConfigurationProperties.class)
public class ZipkinAutoConfiguration {

  @Autowired(required = false)
  private List<ZipkinTracerCustomizer> tracerCustomizers = Collections.emptyList();

  @Value("${spring.application.name:unknown-spring-boot}")
  private String serviceName;

  @Bean
  @ConditionalOnMissingBean
  public io.opentracing.Tracer tracer(Reporter<Span> reporter, Sampler sampler) {

    final Tracing.Builder builder = Tracing.newBuilder()
        .sampler(sampler)
        .localServiceName(serviceName)
        .spanReporter(reporter);

    tracerCustomizers.forEach(c -> c.customize(builder));

    return BraveTracer.create(builder.build());
  }

  @Bean
  @ConditionalOnMissingBean
  public Sender sender(ZipkinConfigurationProperties properties) {
    return OkHttpSender.create(properties.getHttpSender().getUrl());
  }


  @Bean
  @ConditionalOnMissingBean
  public Reporter<Span> reporter(Sender sender) {
    return AsyncReporter.create(sender);
  }


  @Bean
  @ConditionalOnMissingBean
  public Sampler sampler(ZipkinConfigurationProperties properties) {
    if (properties.getBoundarySampler().getRate() != null) {
      return BoundarySampler.create(properties.getBoundarySampler().getRate());
    }

    if (properties.getCountingSampler().getRate() != null) {
      return CountingSampler.create(properties.getCountingSampler().getRate());
    }

    return Sampler.ALWAYS_SAMPLE;
  }
}
