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
package io.opentracing.contrib.spring.cloud.aop;

import io.opentracing.Scope;
import io.opentracing.Tracer;
import io.opentracing.contrib.spring.cloud.SpanUtils;
import io.opentracing.noop.NoopScopeManager.NoopScope;
import io.opentracing.tag.Tags;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseTracingAspect {

  private static final Logger log = LoggerFactory.getLogger(BaseTracingAspect.class);
  private final Tracer tracer;
  private final Class<? extends Annotation> annotation;
  private final List<MethodInterceptorSpanDecorator> decorators;

  public BaseTracingAspect(Tracer tracer, List<MethodInterceptorSpanDecorator> decorators,
      Class<? extends Annotation> annotation) {
    this.tracer = tracer;
    this.decorators = decorators != null ? decorators : new ArrayList<>();
    this.annotation = annotation;
  }

  /**
   * This method should be overridden to add the proper annotation and call {@link
   * BaseTracingAspect#internalTrace(ProceedingJoinPoint)}
   */
  public abstract Object trace(ProceedingJoinPoint pjp) throws Throwable;

  protected Object internalTrace(ProceedingJoinPoint pjp) throws Throwable {
    Scope scope = getScope(pjp);

    for (MethodInterceptorSpanDecorator spanDecorator : decorators) {
      try {
        spanDecorator.onPreProceed(pjp, scope.span());
      } catch (RuntimeException exDecorator) {
        log.error("Exception during decorating span", exDecorator);
      }
    }
    try {
      Object result = pjp.proceed();
      for (MethodInterceptorSpanDecorator spanDecorator : decorators) {
        try {
          spanDecorator.onPostProceed(pjp, result, scope.span());
        } catch (RuntimeException exDecorator) {
          log.error("Exception during decorating span", exDecorator);
        }
      }
      return result;
    } catch (Exception ex) {
      SpanUtils.captureException(scope.span(), ex);
      for (MethodInterceptorSpanDecorator spanDecorator : decorators) {
        spanDecorator.onError(pjp, ex, scope.span());
      }
      throw ex;
    } finally {
      scope.close();
    }
  }

  protected Scope getScope(ProceedingJoinPoint pjp) {
    if (shouldTrace(pjp)) {
      return tracer.buildSpan(getOperationName(pjp))
          .withTag(Tags.COMPONENT.getKey(), getComponent(pjp))
          .startActive(true);
    }
    return NoopScope.INSTANCE;
  }

  protected String getOperationName(ProceedingJoinPoint pjp) {
    return ((MethodSignature) pjp.getSignature()).getMethod().getName();
  }

  protected String getComponent(ProceedingJoinPoint pjp) {
    return annotation.getSimpleName().toLowerCase();
  }

  protected boolean shouldTrace(ProceedingJoinPoint pjp) {
    return true;
  }

  protected Tracer getTracer() {
    return this.tracer;
  }

  protected Class<? extends Annotation> getAnnotation() {
    return this.annotation;
  }

  protected List<MethodInterceptorSpanDecorator> getDecorators() {
    return this.decorators;
  }
}
