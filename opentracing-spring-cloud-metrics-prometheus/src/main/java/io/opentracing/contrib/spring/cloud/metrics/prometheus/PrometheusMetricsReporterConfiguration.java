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
package io.opentracing.contrib.spring.cloud.metrics.prometheus;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.opentracing.contrib.metrics.MetricLabel;
import io.opentracing.contrib.metrics.MetricsReporter;
import io.opentracing.contrib.metrics.prometheus.PrometheusMetricsReporter;
import io.prometheus.client.CollectorRegistry;

@Configuration
@ConditionalOnClass(value = {CollectorRegistry.class})
public class PrometheusMetricsReporterConfiguration {

    @Autowired
    private CollectorRegistry collectorRegistry;

    @Value("${opentracing.metrics.name:}")
    private String metricsName;

    @Autowired(required=false)
    private Set<MetricLabel> metricLabels;

    @Bean
    public MetricsReporter prometheusMetricsReporter() {
        PrometheusMetricsReporter.Builder builder = PrometheusMetricsReporter.newMetricsReporter();
        if (metricsName != null && !metricsName.isEmpty()) {
            builder.withName(metricsName);
        }
        builder.withCollectorRegistry(collectorRegistry);
        if (metricLabels != null && !metricLabels.isEmpty()) {
            for (MetricLabel label : metricLabels) {
                builder.withCustomLabel(label);
            }
        }
        return builder.build();
    }

}
