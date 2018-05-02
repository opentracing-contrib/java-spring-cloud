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
package io.opentracing.contrib.spring.cloud.starter.jaeger;

import io.jaegertracing.Tracer.Builder;
import io.jaegertracing.metrics.Metrics;
import io.jaegertracing.metrics.MetricsFactory;
import io.jaegertracing.metrics.NoopMetricsFactory;
import io.jaegertracing.reporters.CompositeReporter;
import io.jaegertracing.reporters.LoggingReporter;
import io.jaegertracing.reporters.Reporter;
import io.jaegertracing.samplers.ConstSampler;
import io.jaegertracing.samplers.HttpSamplingManager;
import io.jaegertracing.samplers.ProbabilisticSampler;
import io.jaegertracing.samplers.RateLimitingSampler;
import io.jaegertracing.samplers.RemoteControlledSampler;
import io.jaegertracing.samplers.Sampler;
import io.jaegertracing.senders.HttpSender;
import io.jaegertracing.senders.Sender;
import io.opentracing.contrib.spring.cloud.starter.jaeger.JaegerConfigurationProperties.RemoteReporter;
import io.opentracing.contrib.spring.cloud.starter.jaeger.customizers.B3CodecTracerBuilderCustomizer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@Configuration
@ConditionalOnClass(io.jaegertracing.Tracer.class)
@ConditionalOnMissingBean(io.opentracing.Tracer.class)
@ConditionalOnProperty(value = "opentracing.jaeger.enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureBefore(name = "io.opentracing.contrib.spring.web.autoconfig.TracerAutoConfiguration")
@EnableConfigurationProperties(JaegerConfigurationProperties.class)
public class JaegerAutoConfiguration {

  @Autowired(required = false)
  private List<TracerBuilderCustomizer> tracerCustomizers = Collections.emptyList();

  @Value("${spring.application.name:unknown-spring-boot}")
  private String serviceName;

  @Bean
  public io.opentracing.Tracer tracer(JaegerConfigurationProperties jaegerConfigurationProperties,
        Sampler sampler,
        Reporter reporter) {

    final Builder builder =
        new Builder(serviceName)
            .withReporter(reporter)
            .withSampler(sampler);

    tracerCustomizers.forEach(c -> c.customize(builder));

    return builder.build();
  }

  @ConditionalOnMissingBean
  @Bean
  public Reporter reporter(JaegerConfigurationProperties properties,
      Metrics metrics,
      @Autowired(required = false) ReporterAppender reporterAppender) {

    List<Reporter> reporters = new LinkedList<>();
    RemoteReporter remoteReporter = properties.getRemoteReporter();

    JaegerConfigurationProperties.HttpSender httpSender = properties.getHttpSender();
    if (!StringUtils.isEmpty(httpSender.getUrl())) {
      reporters.add(getHttpReporter(metrics, remoteReporter, httpSender));
    } else {
      reporters.add(getUdpReporter(metrics, remoteReporter, properties.getUdpSender()));
    }

    if (properties.isLogSpans()) {
      reporters.add(new LoggingReporter());
    }

    if (reporterAppender != null) {
      reporterAppender.append(reporters);
    }

    return new CompositeReporter(reporters.toArray(new Reporter[reporters.size()]));
  }

  private Reporter getUdpReporter(Metrics metrics,
      RemoteReporter remoteReporter,
      JaegerConfigurationProperties.UdpSender udpSenderProperties) {
    io.jaegertracing.senders.UdpSender udpSender = new io.jaegertracing.senders.UdpSender(
        udpSenderProperties.getHost(), udpSenderProperties.getPort(),
        udpSenderProperties.getMaxPacketSize());

    return createReporter(metrics, remoteReporter, udpSender);
  }

  private Reporter getHttpReporter(Metrics metrics,
      RemoteReporter remoteReporter,
      JaegerConfigurationProperties.HttpSender httpSenderProperties) {
    HttpSender.Builder builder = new HttpSender.Builder(httpSenderProperties.getUrl());
    if (httpSenderProperties.getMaxPayload() != null) {
      builder = builder.withMaxPacketSize(httpSenderProperties.getMaxPayload());
    }
    if (!StringUtils.isEmpty(httpSenderProperties.getUsername())
        && !StringUtils.isEmpty(httpSenderProperties.getPassword())) {
      builder.withAuth(httpSenderProperties.getUsername(), httpSenderProperties.getPassword());
    } else if (!StringUtils.isEmpty(httpSenderProperties.getAuthToken())) {
      builder.withAuth(httpSenderProperties.getAuthToken());
    }

    return createReporter(metrics, remoteReporter, builder.build());
  }

  private Reporter createReporter(Metrics metrics,
      RemoteReporter remoteReporter, Sender udpSender) {
    io.jaegertracing.reporters.RemoteReporter.Builder builder =
        new io.jaegertracing.reporters.RemoteReporter.Builder()
            .withSender(udpSender)
            .withMetrics(metrics);

    if (remoteReporter.getFlushInterval() != null) {
      builder.withFlushInterval(remoteReporter.getFlushInterval());
    }
    if (remoteReporter.getMaxQueueSize() != null) {
      builder.withMaxQueueSize(remoteReporter.getMaxQueueSize());
    }

    return builder.build();
  }

  @ConditionalOnMissingBean
  @Bean
  public Metrics reporterMetrics(MetricsFactory metricsFactory) {
    return new Metrics(metricsFactory);
  }

  @ConditionalOnMissingBean
  @Bean
  public MetricsFactory metricsFactory() {
    return new NoopMetricsFactory();
  }

  @ConditionalOnProperty(value = "opentracing.jaeger.enable-b3-propagation", havingValue = "true")
  @Bean
  public TracerBuilderCustomizer b3CodecJaegerTracerCustomizer() {
    return new B3CodecTracerBuilderCustomizer();
  }

  /**
   * Decide on what Sampler to use based on the various configuration options in
   * JaegerConfigurationProperties Fallback to ConstSampler(true) when no Sampler is configured
   */
  @ConditionalOnMissingBean
  @Bean
  public Sampler sampler(JaegerConfigurationProperties properties, Metrics metrics) {
    if (properties.getConstSampler().getDecision() != null) {
      return new ConstSampler(properties.getConstSampler().getDecision());
    }

    if (properties.getProbabilisticSampler().getSamplingRate() != null) {
      return new ProbabilisticSampler(properties.getProbabilisticSampler().getSamplingRate());
    }

    if (properties.getRateLimitingSampler().getMaxTracesPerSecond() != null) {
      return new RateLimitingSampler(properties.getRateLimitingSampler().getMaxTracesPerSecond());
    }

    if (!StringUtils.isEmpty(properties.getRemoteControlledSampler().getHostPort())) {
      JaegerConfigurationProperties.RemoteControlledSampler samplerProperties
          = properties.getRemoteControlledSampler();

      return new RemoteControlledSampler.Builder(serviceName)
          .withSamplingManager(new HttpSamplingManager(samplerProperties.getHostPort()))
          .withInitialSampler(
              new ProbabilisticSampler(samplerProperties.getSamplingRate()))
          .withMetrics(metrics)
          .build();
    }

    //fallback to sampling every trace
    return new ConstSampler(true);
  }

}
