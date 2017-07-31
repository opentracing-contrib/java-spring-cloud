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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.MetricsServlet;

@Configuration
@ConditionalOnClass(value = {MetricsServlet.class})
public class PrometheusServletConfiguration {

    @Value("${opentracing.metrics.exporter.http.path:false}")
    private String metricsPath;

    @Bean
    @ConditionalOnProperty(name="opentracing.metrics.exporter.http.path")
    ServletRegistrationBean registerPrometheusExporterServlet(CollectorRegistry metricRegistry) {
          return new ServletRegistrationBean(new MetricsServlet(metricRegistry), metricsPath);
    }

}
