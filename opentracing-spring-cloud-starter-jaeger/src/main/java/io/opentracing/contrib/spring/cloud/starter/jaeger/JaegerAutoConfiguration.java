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

import com.uber.jaeger.Tracer.Builder;
import com.uber.jaeger.metrics.InMemoryMetricsFactory;
import com.uber.jaeger.metrics.Metrics;
import com.uber.jaeger.metrics.MetricsFactory;
import com.uber.jaeger.metrics.NoopMetricsFactory;
import com.uber.jaeger.reporters.CompositeReporter;
import com.uber.jaeger.reporters.LoggingReporter;
import com.uber.jaeger.reporters.Reporter;
import com.uber.jaeger.samplers.ConstSampler;
import com.uber.jaeger.samplers.HttpSamplingManager;
import com.uber.jaeger.samplers.ProbabilisticSampler;
import com.uber.jaeger.samplers.RateLimitingSampler;
import com.uber.jaeger.samplers.RemoteControlledSampler;
import com.uber.jaeger.samplers.Sampler;
import com.uber.jaeger.senders.Sender;
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
@ConditionalOnClass(com.uber.jaeger.Tracer.class)
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
    com.uber.jaeger.senders.UdpSender udpSender = new com.uber.jaeger.senders.UdpSender(
        udpSenderProperties.getHost(), udpSenderProperties.getPort(),
        udpSenderProperties.getMaxPacketSize());

    return createReporter(metrics, remoteReporter, udpSender);
  }

  private Reporter getHttpReporter(Metrics metrics,
      RemoteReporter remoteReporter,
      JaegerConfigurationProperties.HttpSender httpSenderProperties) {
    /*
     * this would have been changed to use HttpSender.Builder,
     * but HttpSender.Builder.withMaxPacketSize is private
     */
    com.uber.jaeger.senders.HttpSender httpSender = new com.uber.jaeger.senders.HttpSender(
        httpSenderProperties.getUrl(), httpSenderProperties.getMaxPayload());

    return createReporter(metrics, remoteReporter, httpSender);
  }

  private Reporter createReporter(Metrics metrics,
      RemoteReporter remoteReporter, Sender udpSender) {
    com.uber.jaeger.reporters.RemoteReporter.Builder builder =
        new com.uber.jaeger.reporters.RemoteReporter.Builder()
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

  @ConditionalOnProperty(value = "opentracing.jaeger.enableB3Propagation", havingValue = "true")
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
