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
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.contrib.spring.cloud.ExtensionTags;
import io.opentracing.tag.Tags;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseTracingAspect {

  private static final Logger log = LoggerFactory.getLogger(BaseTracingAspect.class);
  private final Tracer tracer;
  private final Class<? extends Annotation> annotation;
  private final List<MethodInterceptorSpanDecorator> decorators;
  private Pattern skipPattern;

  public BaseTracingAspect(Tracer tracer, List<MethodInterceptorSpanDecorator> decorators,
      Class<? extends Annotation> annotation, Pattern skipPattern) {
    this.tracer = tracer;
    this.decorators = decorators != null ? decorators : new ArrayList<>();
    this.annotation = annotation;
    this.skipPattern = skipPattern;
  }

  /**
   * This method should be overridden to add the proper annotation and call {@link
   * BaseTracingAspect#internalTrace(ProceedingJoinPoint)}
   */
  public abstract Object trace(ProceedingJoinPoint pjp) throws Throwable;

  protected Object internalTrace(ProceedingJoinPoint pjp) throws Throwable {
    if (skipPattern.matcher(pjp.getTarget().getClass().getName()).matches()) {
      return pjp.proceed();
    }

    // operation name is method name
    Span span = tracer.buildSpan(getOperationName(pjp))
        .withTag(Tags.COMPONENT.getKey(), getComponent(pjp))
        .withTag(ExtensionTags.CLASS_TAG.getKey(), pjp.getTarget().getClass().getSimpleName())
        .withTag(ExtensionTags.METHOD_TAG.getKey(), pjp.getSignature().getName())
        .start();

    try {
      try (Scope scope = tracer.activateSpan(span)) {
        decoratePreProceed(pjp, span);
        Object result = pjp.proceed();
        decoratePostProceed(pjp, span, result);
        return result;
      }
    } catch (Exception ex) {
      decorateOnError(pjp, span, ex);
      throw ex;
    } finally {
      span.finish();
    }
  }

  protected void decoratePreProceed(ProceedingJoinPoint pjp, Span span) {
    for (MethodInterceptorSpanDecorator spanDecorator : decorators) {
      try {
        spanDecorator.onPreProceed(pjp, span);
      } catch (RuntimeException exDecorator) {
        log.error("Exception during decorating span", exDecorator);
      }
    }
  }

  protected void decoratePostProceed(ProceedingJoinPoint pjp, Span span, Object result) {
    for (MethodInterceptorSpanDecorator spanDecorator : decorators) {
      try {
        spanDecorator.onPostProceed(pjp, result, span);
      } catch (RuntimeException exDecorator) {
        log.error("Exception during decorating span", exDecorator);
      }
    }
  }

  protected void decorateOnError(ProceedingJoinPoint pjp, Span span, Exception ex) {
    for (MethodInterceptorSpanDecorator spanDecorator : decorators) {
      spanDecorator.onError(pjp, ex, span);
    }
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

}
