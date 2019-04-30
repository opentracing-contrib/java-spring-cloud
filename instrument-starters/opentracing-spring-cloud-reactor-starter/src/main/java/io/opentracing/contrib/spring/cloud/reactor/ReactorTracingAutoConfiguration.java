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
import java.util.function.Function;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import org.reactivestreams.Publisher;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
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
@ConditionalOnBean(Tracer.class)
@ConditionalOnClass(Hooks.class)
@ConditionalOnProperty(name = "opentracing.spring.cloud.reactor.enabled", havingValue = "true", matchIfMissing = true)
public class ReactorTracingAutoConfiguration {

  private static final Logger log = Logger.getLogger(ReactorTracingAutoConfiguration.class.getName());

  private static final String EXECUTOR_SERVICE_DECORATOR_KEY = ReactorTracingAutoConfiguration.class.getName();
  private static final String HOOK_KEY = ReactorTracingAutoConfiguration.class.getName();

  @Bean
  static HookRegisteringPostProcessor hookRegisteringPostProcessor(ObjectProvider<Tracer> tracerProvider) {
    return new HookRegisteringPostProcessor(tracerProvider);
  }

  // Use a BeanDefinitionRegistryPostProcessor here to decorate Reactor as soon as possible before potential early initialization of Reactor schedulers
  // For more detailed description, please refer to https://github.com/opentracing-contrib/java-spring-cloud/issues/214
  private static class HookRegisteringPostProcessor implements BeanDefinitionRegistryPostProcessor {

    private final ObjectProvider<Tracer> tracerProvider;

    private volatile Function<? super Publisher<Object>, ? extends Publisher<Object>> hookFunction;

    private HookRegisteringPostProcessor(ObjectProvider<Tracer> tracerProvider) {
      this.tracerProvider = tracerProvider;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
      Hooks.onEachOperator(HOOK_KEY, publisher -> {
        try {
          return getHookFunction().apply(publisher);
        } catch (Exception e) {
          log.severe("Encountered error while retrieving Tracer instance! Fallback to original publisher without hook function..");
          return publisher;
        }
      });

      Schedulers.setExecutorServiceDecorator(
          EXECUTOR_SERVICE_DECORATOR_KEY,
          (scheduler, scheduledExecutorService) -> {
            try {
              return new TracedScheduledExecutorService(scheduledExecutorService, tracerProvider.getIfAvailable());
            } catch (Exception e) {
              log.severe("Encountered error while retrieving Tracer instance! Fallback to original executor without being decorated..");
              return scheduledExecutorService;
            }
          }
      );
    }

    private Function<? super Publisher<Object>, ? extends Publisher<Object>> getHookFunction() {
      if (hookFunction == null) {
        hookFunction = TracedSubscriber.asOperator(tracerProvider.getIfAvailable());
      }
      return hookFunction;
    }

    @PreDestroy
    public void cleanupHooks() {
      Hooks.resetOnEachOperator(HOOK_KEY);
      Schedulers.removeExecutorServiceDecorator(EXECUTOR_SERVICE_DECORATOR_KEY);
    }

  }

}
