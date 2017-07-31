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
import java.util.regex.Pattern;

import org.springframework.context.annotation.Configuration;

import io.opentracing.contrib.web.servlet.filter.TracingFilter;
import io.prometheus.client.exporter.MetricsServlet;

@Configuration
@ConditionalOnClass(value = {MetricsServlet.class, TracingFilter.class})
@ConditionalOnProperty(name="opentracing.metrics.exporter.http.path")
public class PrometheusServletSkipPatternConfiguration implements javax.servlet.ServletContextListener {

    @Value("${opentracing.metrics.exporter.http.path:false}")
    private String metricsPath;

    @Override
    public void contextInitialized(javax.servlet.ServletContextEvent sce) {
        sce.getServletContext().setAttribute(TracingFilter.SKIP_PATTERN, Pattern.compile(metricsPath));
    }

    @Override
    public void contextDestroyed(javax.servlet.ServletContextEvent sce) {
    }

}
