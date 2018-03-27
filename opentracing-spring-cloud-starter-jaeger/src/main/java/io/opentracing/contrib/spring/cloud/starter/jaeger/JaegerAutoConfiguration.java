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
import com.uber.jaeger.metrics.InMemoryStatsReporter;
import com.uber.jaeger.metrics.Metrics;
import com.uber.jaeger.metrics.NullStatsReporter;
import com.uber.jaeger.metrics.StatsReporter;
import com.uber.jaeger.reporters.CompositeReporter;
import com.uber.jaeger.reporters.LoggingReporter;
import com.uber.jaeger.reporters.RemoteReporter;
import com.uber.jaeger.reporters.Reporter;
import com.uber.jaeger.samplers.ConstSampler;
import com.uber.jaeger.samplers.HttpSamplingManager;
import com.uber.jaeger.samplers.ProbabilisticSampler;
import com.uber.jaeger.samplers.RateLimitingSampler;
import com.uber.jaeger.samplers.RemoteControlledSampler;
import com.uber.jaeger.samplers.Sampler;
import com.uber.jaeger.tracerresolver.JaegerTracerResolver;
import io.opentracing.contrib.spring.cloud.starter.jaeger.customizers.B3CodecJaegerTracerCustomizer;
import io.opentracing.contrib.tracerresolver.TracerResolver;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.StreamSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.util.StringUtils;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@Configuration
@ConditionalOnClass(com.uber.jaeger.Tracer.class)
@ConditionalOnMissingBean(io.opentracing.Tracer.class)
@ConditionalOnProperty(value = "opentracing.jaeger.enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureBefore(name = "io.opentracing.contrib.spring.web.autoconfig.TracerAutoConfiguration")
public class JaegerAutoConfiguration {

  @Configuration
  @ConditionalOnProperty(value = "opentracing.jaeger.useTracerResolver", havingValue = "false", matchIfMissing = true)
  @EnableConfigurationProperties(JaegerConfigurationProperties.class)
  public static class ExplicitConfiguration {

    @Autowired(required = false)
    private List<JaegerTracerCustomizer> tracerCustomizers = Collections.emptyList();

    @Bean
    public io.opentracing.Tracer tracer(JaegerConfigurationProperties jaegerConfigurationProperties,
        Sampler sampler,
        Reporter reporter) {

      final Builder builder = new Builder(jaegerConfigurationProperties.getServiceName(), reporter,
          sampler);

      tracerCustomizers.forEach(c -> c.customize(builder));

      return builder.build();
    }

    @ConditionalOnMissingBean
    @Bean
    public Reporter reporter(JaegerConfigurationProperties properties,
        Metrics metrics,
        @Autowired(required = false) ReporterAppender reporterAppender) {
      List<Reporter> reporters = new LinkedList<>();

      JaegerConfigurationProperties.RemoteReporterProperties remoteReporterProperties =
          properties.getRemoteReporterProperties();
      JaegerConfigurationProperties.HttpSender httpSender =
          properties.getHttpSender();
      if (!StringUtils.isEmpty(httpSender.getUrl()) && !httpSender.isDisable()) {
        reporters.add(getHttpReporter(metrics, remoteReporterProperties, httpSender));
      }

      JaegerConfigurationProperties.UdpSender udpSender =
          properties.getUdpSender();
      if (!StringUtils.isEmpty(udpSender.getHost()) && !udpSender.isDisable()) {
        reporters.add(getUdpReporter(metrics, remoteReporterProperties, udpSender));
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
        JaegerConfigurationProperties.RemoteReporterProperties remoteReporterProperties,
        JaegerConfigurationProperties.UdpSender udpSenderProperties) {
      com.uber.jaeger.senders.UdpSender udpSender = new com.uber.jaeger.senders.UdpSender(
          udpSenderProperties.getHost(), udpSenderProperties.getPort(),
          udpSenderProperties.getMaxPacketSize());
      return new RemoteReporter(udpSender, remoteReporterProperties.getFlushInterval(),
          remoteReporterProperties.getMaxQueueSize(), metrics);
    }

    private Reporter getHttpReporter(Metrics metrics,
        JaegerConfigurationProperties.RemoteReporterProperties remoteReporterProperties,
        JaegerConfigurationProperties.HttpSender httpSenderProperties) {
      com.uber.jaeger.senders.HttpSender httpSender = new com.uber.jaeger.senders.HttpSender(
          httpSenderProperties.getUrl(), httpSenderProperties.getMaxPayload());
      return new RemoteReporter(httpSender, remoteReporterProperties.getFlushInterval(),
          remoteReporterProperties.getMaxQueueSize(), metrics);
    }

    @ConditionalOnMissingBean
    @Bean
    public Metrics reporterMetrics(StatsReporter statsReporter) {
      return Metrics.fromStatsReporter(statsReporter);
    }

    @ConditionalOnMissingBean
    @Bean
    public StatsReporter statsReporter(JaegerConfigurationProperties properties) {
      if (properties.isEnableMetrics()) {
        return new InMemoryStatsReporter();
      }
      return new NullStatsReporter();
    }

    @ConditionalOnProperty(value = "opentracing.jaeger.enableB3Propagation", havingValue = "true")
    @Bean
    public JaegerTracerCustomizer b3CodecJaegerTracerCustomizer() {
      return new B3CodecJaegerTracerCustomizer();
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

        Sampler initialSampler = new ProbabilisticSampler(samplerProperties.getSamplingRate());
        HttpSamplingManager manager = new HttpSamplingManager(samplerProperties.getHostPort());

        return new RemoteControlledSampler(properties.getServiceName(), manager, initialSampler,
            metrics);
      }

      //fallback to sampling every trace
      return new ConstSampler(true);
    }

  }


  @Configuration
  @ConditionalOnProperty(value = "opentracing.jaeger.useTracerResolver", havingValue = "true")
  @ConditionalOnClass(JaegerTracerResolver.class)
  public static class TracerResolverConfiguration {

    @Bean
    public io.opentracing.Tracer tracer(AbstractEnvironment environment) {
      copyJaegerPropertiesFromSpringEnvToSystemProps(environment);
      return TracerResolver.resolveTracer();
    }

    private void copyJaegerPropertiesFromSpringEnvToSystemProps(AbstractEnvironment environment) {
      StreamSupport.stream(environment.getPropertySources().spliterator(), false)
          .filter(ps -> ps instanceof EnumerablePropertySource)
          .map(ps -> ((EnumerablePropertySource) ps).getPropertyNames())
          .flatMap(Arrays::<String>stream)
          .map(prop -> new Tuple2<>(prop, prop.toUpperCase().replace('.', '_')))
          .filter(t -> t.getV2().startsWith("JAEGER"))
          .filter(t -> !System.getProperties()
              .containsKey(t.getV2())) //don't override explicitly set props
          .forEach(t -> System.setProperty(t.getV2(), environment.getProperty(t.getV1())));
    }

    private static class Tuple2<T1, T2> {

      private final T1 v1;
      private final T2 v2;

      Tuple2(T1 v1, T2 v2) {
        this.v1 = v1;
        this.v2 = v2;
      }

      public T1 getV1() {
        return v1;
      }

      public T2 getV2() {
        return v2;
      }
    }
  }
}
