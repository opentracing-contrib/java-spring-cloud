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
package io.opentracing.contrib.spring.cloud.metrics;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.opentracing.contrib.api.SpanData;
import io.opentracing.contrib.api.TracerObserver;
import io.opentracing.contrib.metrics.MetricsReporter;

@SpringBootTest(
        classes = {MetricsTracerObserverConfigurationWithReportersTest.SpringConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class MetricsTracerObserverConfigurationWithReportersTest {

    private static final MetricsReporter metricsReporter = Mockito.mock(MetricsReporter.class);

    @Configuration
    @EnableAutoConfiguration
    public static class SpringConfiguration {
        @Bean
        public MetricsReporter reporter() {
            return metricsReporter;
        }
    }

    @Autowired(required=false)
    protected TracerObserver tracerObserver;

    @Test
    public void testTracerObserverWithReporters() {
        assertNotNull(tracerObserver);
 
        SpanData spanData = Mockito.mock(SpanData.class);

        tracerObserver.onStart(spanData).onFinish(spanData, System.currentTimeMillis());

        Mockito.verify(metricsReporter).reportSpan(spanData);
    }
}
