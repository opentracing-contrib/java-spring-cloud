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
import io.opentracing.contrib.spring.cloud.async.instrument.TracedThreadPoolTaskExecutor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.Executor;
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
    if (bean instanceof Executor && !(bean instanceof ThreadPoolTaskExecutor)) {
      Method execute = ReflectionUtils.findMethod(bean.getClass(), "execute", Runnable.class);
      boolean methodFinal = Modifier.isFinal(execute.getModifiers());
      boolean classFinal = Modifier.isFinal(bean.getClass().getModifiers());
      boolean cglibProxy = !methodFinal && !classFinal;
      Executor executor = (Executor) bean;
      ProxyFactoryBean factory = new ProxyFactoryBean();
      factory.setProxyTargetClass(cglibProxy);
      factory.addAdvice(new ExecutorMethodInterceptor<>(executor, tracer));
      factory.setTarget(bean);
      return factory.getObject();
    } else if (bean instanceof ThreadPoolTaskExecutor) {
      boolean classFinal = Modifier.isFinal(bean.getClass().getModifiers());
      boolean cglibProxy = !classFinal;
      ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) bean;
      ProxyFactoryBean factory = new ProxyFactoryBean();
      factory.setProxyTargetClass(cglibProxy);
      factory.addAdvice(new ExecutorMethodInterceptor<ThreadPoolTaskExecutor>(executor, tracer) {
        @Override
        Executor tracedExecutor(Tracer tracer, ThreadPoolTaskExecutor executor) {
          return new TracedThreadPoolTaskExecutor(tracer, executor);
        }
      });
      factory.setTarget(bean);
      return factory.getObject();
    }
    return bean;
  }
}

class ExecutorMethodInterceptor<T extends Executor> implements MethodInterceptor {

  private final T delegate;
  private final Tracer tracer;

  ExecutorMethodInterceptor(T delegate, Tracer tracer) {
    this.delegate = delegate;
    this.tracer = tracer;
  }

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    Executor tracedExecutor = tracedExecutor(tracer, delegate);
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

  Executor tracedExecutor(Tracer tracer, T executor) {
    return new TracedExecutor(executor, tracer);
  }
}

