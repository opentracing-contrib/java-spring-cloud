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
package io.opentracing.contrib.spring.cloud.api.extensions;

import static org.junit.Assert.assertNotEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.opentracing.Tracer;
import io.opentracing.contrib.api.SpanData;
import io.opentracing.contrib.api.TracerObserver;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.ThreadLocalActiveSpanSource;

@SpringBootTest(
        classes = {TracerBeanPostProcessorTest.SpringConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class TracerBeanPostProcessorTest {

    private static final MockTracer mockTracer = new MockTracer(new ThreadLocalActiveSpanSource());

    private static final TracerObserver tracerObserver = Mockito.mock(TracerObserver.class);

    @Configuration
    @EnableAutoConfiguration
    public static class SpringConfiguration {
        @Bean
        public Tracer tracer() {
            return mockTracer;
        }

        @Bean
        public TracerObserver observer() {
            return tracerObserver;
        }
    }

    @Autowired
    protected Tracer tracer;

    @Before
    public void before() {
        mockTracer.reset();
    }

    @Test
    public void testTracerWrapped() {
        assertNotEquals(MockTracer.class, tracer.getClass());

        tracer.buildSpan("testop").startManual();

        Mockito.verify(tracerObserver).onStart(Matchers.any(SpanData.class));
    }

}
