/**
 * Copyright 2013-2017 the original author or authors. Copyright 2017 The OpenTracing Authors
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
package io.opentracing.contrib.spring.cloud.async;

import io.opentracing.Tracer;
import io.opentracing.contrib.concurrent.TracedExecutor;
import io.opentracing.contrib.concurrent.TracedExecutorService;
import io.opentracing.contrib.spring.cloud.async.instrument.TracedThreadPoolTaskExecutor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ReflectionUtils;

/**
 * Bean post processor that wraps a call to an {@link Executor} either in a
 * JDK or CGLIB proxy. Depending on whether the implementation has a final
 * method or is final.
 *
 * @author Marcin Grzejszczak
 */
class ExecutorBeanPostProcessor implements BeanPostProcessor {
  private final Tracer tracer;

  ExecutorBeanPostProcessor(Tracer tracer) {
    this.tracer = tracer;
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName)
      throws BeansException {
    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    if (bean instanceof Executor) {
      if (bean instanceof ExecutorService) {
        ExecutorService executorService = (ExecutorService) bean;
        return proxify(
            executorService,
            TracedExecutorService::new,
            shouldUseCGLibProxy(executorService, ExecutorService.class)
        );
      } else if (bean instanceof ThreadPoolTaskExecutor) {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = (ThreadPoolTaskExecutor) bean;
        boolean classNotFinal = !Modifier.isFinal(threadPoolTaskExecutor.getClass().getModifiers());
        if (classNotFinal) {
          return proxify(
              threadPoolTaskExecutor,
              (e, t) -> new TracedThreadPoolTaskExecutor(t, e),
              true
          );
        } else {
          // Bean class is final, and extends ThreadPoolTaskExecutor
          // Can't use cglib, nor jdk proxy.
          return bean;
        }
      } else {
        Executor executor = (Executor) bean;
        return proxify(
            executor,
            TracedExecutor::new,
            shouldUseCGLibProxy(executor, Executor.class)
        );
      }
    }
    return bean;
  }

  private <T extends Executor> Object proxify(T executor, BiFunction<T, Tracer, T> tracingExecutorProvider, boolean useCglib) {
    ProxyFactoryBean factory = new ProxyFactoryBean();
    factory.setProxyTargetClass(useCglib);
    factory.addAdvice(new ExecutorMethodInterceptor<>(executor, tracingExecutorProvider, tracer));
    factory.setTarget(executor);
    return factory.getObject();
  }

  private boolean shouldUseCGLibProxy(Executor executor, Class<? extends Executor> iface) {
    boolean anyMethodFinal = Stream.of(ReflectionUtils.getAllDeclaredMethods(iface))
        .map(method -> ReflectionUtils.findMethod(executor.getClass(), method.getName(), method.getParameterTypes()))
        .map(Optional::ofNullable)
        .map(method -> method.orElseThrow(NoSuchMethodError::new))
        .map(Method::getModifiers)
        .anyMatch(Modifier::isFinal);
    boolean classFinal = Modifier.isFinal(executor.getClass().getModifiers());
    return !anyMethodFinal && !classFinal;
  }
}

class ExecutorMethodInterceptor<T extends Executor> implements MethodInterceptor {

  private final T delegate;
  private final Tracer tracer;
  private final BiFunction<T, Tracer, T> tracedExecutorProvider;

  ExecutorMethodInterceptor(T delegate, BiFunction<T, Tracer, T> tracedExecutorProvider, Tracer tracer) {
    this.delegate = delegate;
    this.tracedExecutorProvider = tracedExecutorProvider;
    this.tracer = tracer;
  }

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    T tracedExecutor = tracedExecutorProvider.apply(delegate, tracer);
    Method methodOnTracedBean = getMethod(invocation, tracedExecutor);
    if (methodOnTracedBean != null) {
      try {
        return methodOnTracedBean.invoke(tracedExecutor, invocation.getArguments());
      } catch (InvocationTargetException ex) {
        throw ex.getCause();
      }
    }
    return invocation.proceed();
  }

  private Method getMethod(MethodInvocation invocation, Object object) {
    Method method = invocation.getMethod();
    return ReflectionUtils
        .findMethod(object.getClass(), method.getName(), method.getParameterTypes());
  }
}

