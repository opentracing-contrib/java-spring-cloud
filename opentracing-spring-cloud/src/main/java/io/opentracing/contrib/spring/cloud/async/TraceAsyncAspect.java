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
package io.opentracing.contrib.spring.cloud.async;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.contrib.spring.cloud.ExtensionTags;
import io.opentracing.contrib.spring.cloud.SpanUtils;
import io.opentracing.tag.Tags;
import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ReflectionUtils;

/**
 * @author kameshsampath
 */
@Aspect
public class TraceAsyncAspect {

  static final String TAG_COMPONENT = "async";

  @Autowired
  private Tracer tracer;

  public TraceAsyncAspect(Tracer tracer) {
    this.tracer = tracer;
  }

  @Around("execution (@org.springframework.scheduling.annotation.Async * *.*(..))")
  public Object traceBackgroundThread(final ProceedingJoinPoint pjp) throws Throwable {
    /**
     * We create a span because parent span might be missing. E.g. method is invoked
     */
    Span span = null;
    try {
      span = this.tracer.buildSpan(operationName(pjp))
          .withTag(Tags.COMPONENT.getKey(), TAG_COMPONENT)
          .withTag(ExtensionTags.CLASS_TAG.getKey(), pjp.getTarget().getClass().getSimpleName())
          .withTag(ExtensionTags.METHOD_TAG.getKey(), pjp.getSignature().getName())
          .startManual();
      return pjp.proceed();
    } catch (Exception ex) {
      SpanUtils.captureException(span, ex);
      throw ex;
    } finally {
      span.finish();
    }
  }

  private static String operationName(ProceedingJoinPoint pjp) {
    return getMethod(pjp, pjp.getTarget()).getName();
  }

  private static Method getMethod(ProceedingJoinPoint pjp, Object object) {
    MethodSignature signature = (MethodSignature) pjp.getSignature();
    Method method = signature.getMethod();
    return ReflectionUtils
        .findMethod(object.getClass(), method.getName(), method.getParameterTypes());
  }
}
