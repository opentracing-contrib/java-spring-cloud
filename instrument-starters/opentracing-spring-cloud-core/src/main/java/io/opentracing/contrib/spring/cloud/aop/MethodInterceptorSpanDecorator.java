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

import io.opentracing.Span;
import io.opentracing.contrib.spring.cloud.ExtensionTags;
import io.opentracing.tag.Tags;
import java.util.HashMap;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

public interface MethodInterceptorSpanDecorator {

  /**
   * Decorate span before invocation is done, e.g. before
   * {@link ProceedingJoinPoint#proceed()}
   * is called
   *
   * @param ProceedingJoinPoint pjp
   * @param Object result
   * @param span span
   */
  void onPreProceed(ProceedingJoinPoint pjp, Span span);

  /**
   * Decorate span after invocation is done, e.g. after
   * {@link ProceedingJoinPoint#proceed()}
   * is called
   *
   * @param ProceedingJoinPoint pjp
   * @param Object result
   * @param span span
   */
  void onPostProceed(ProceedingJoinPoint pjp, Object result, Span span);

  /**
   * Decorate span when exception is thrown during the invocation, e.g. during
   * {@link ProceedingJoinPoint#proceed()}
   * is processing.
   *
   * @param ProceedingJoinPoint pjp
   * @param ex exception
   * @param span span
   */
  void onError(ProceedingJoinPoint pjp, Throwable ex, Span span);

  /**
   * This decorator adds set of standard tags to the span.
   */
  class StandardTags implements MethodInterceptorSpanDecorator {

    public static Map<String, Object> errorLogs(Throwable ex) {
      Map<String, Object> errorLogs = new HashMap<>(2);
      errorLogs.put("event", Tags.ERROR.getKey());
      errorLogs.put("error.object", ex);
      return errorLogs;
    }

    @Override
    public void onPreProceed(ProceedingJoinPoint pjp, Span span) {
      ExtensionTags.CLASS_TAG.set(span, pjp.getTarget().getClass().getSimpleName());
      ExtensionTags.METHOD_TAG.set(span, ((MethodSignature) pjp.getSignature()).getName());
    }

    @Override
    public void onPostProceed(ProceedingJoinPoint pjp, Object result, Span span) {
    }

    @Override
    public void onError(ProceedingJoinPoint pjp, Throwable ex, Span span) {
      Tags.ERROR.set(span, Boolean.TRUE);
      span.log(errorLogs(ex));
    }
  }
}
