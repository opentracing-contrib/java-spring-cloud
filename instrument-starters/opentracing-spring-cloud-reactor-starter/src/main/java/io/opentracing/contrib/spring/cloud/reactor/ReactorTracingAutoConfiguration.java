/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.opentracing.contrib.spring.cloud.reactor;

import io.opentracing.Tracer;
import io.opentracing.contrib.concurrent.TracedScheduledExecutorService;
import io.opentracing.contrib.reactor.TracedSubscriber;
import io.opentracing.contrib.spring.tracer.configuration.TracerAutoConfiguration;
import javax.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;
import reactor.core.scheduler.Schedulers;

/**
 * Spring Boot auto-configuration to enable tracing of Reactor components.
 *
 * Similar to {@code TraceReactorAutoConfiguration} from spring-cloud-sleuth.
 *
 * @author Csaba Kos
 */
@Configuration
@AutoConfigureAfter(TracerAutoConfiguration.class)
@ConditionalOnBean(Tracer.class)
@ConditionalOnClass(Hooks.class)
@ConditionalOnProperty(name = "opentracing.spring.cloud.reactor.enabled", havingValue = "true", matchIfMissing = true)
public class ReactorTracingAutoConfiguration {
  private static final String EXECUTOR_SERVICE_DECORATOR_KEY = ReactorTracingAutoConfiguration.class.getName();
  private static final String HOOK_KEY = ReactorTracingAutoConfiguration.class.getName();

  @Autowired
  public ReactorTracingAutoConfiguration(final Tracer tracer) {
    Hooks.onEachOperator(HOOK_KEY, TracedSubscriber.asOperator(tracer));
    Schedulers.setExecutorServiceDecorator(
        EXECUTOR_SERVICE_DECORATOR_KEY,
        (scheduler, scheduledExecutorService) ->
            new TracedScheduledExecutorService(scheduledExecutorService, tracer)
    );
  }

  @PreDestroy
  public void cleanupHooks() {
    Hooks.resetOnEachOperator(HOOK_KEY);
    Schedulers.removeExecutorServiceDecorator(EXECUTOR_SERVICE_DECORATOR_KEY);
  }
}
