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
package io.opentracing.contrib.spring.cloud.scheduled;

import io.opentracing.Scope;
import io.opentracing.Tracer;
import io.opentracing.contrib.spring.cloud.ExtensionTags;
import io.opentracing.contrib.spring.cloud.SpanUtils;
import io.opentracing.tag.Tags;
import java.util.regex.Pattern;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * @author Pavol Loffay
 */
@Aspect
public class ScheduledAspect {

  static final String COMPONENT_NAME = "scheduled";

  private Tracer tracer;
  private Pattern skipPattern;

  public ScheduledAspect(Tracer tracer, ScheduledTracingProperties scheduledTracingProperties) {
    this.tracer = tracer;
    this.skipPattern = Pattern.compile(scheduledTracingProperties.getSkipPattern());
  }

  @Around("execution (@org.springframework.scheduling.annotation.Scheduled  * *.*(..))")
  public Object traceBackgroundThread(final ProceedingJoinPoint pjp) throws Throwable {
    if (skipPattern.matcher(pjp.getTarget().getClass().getName()).matches()) {
      return pjp.proceed();
    }

    // operation name is method name
    Scope scope = tracer.buildSpan(pjp.getSignature().getName())
        .withTag(Tags.COMPONENT.getKey(), COMPONENT_NAME)
        .withTag(ExtensionTags.CLASS_TAG.getKey(), pjp.getTarget().getClass().getSimpleName())
        .withTag(ExtensionTags.METHOD_TAG.getKey(), pjp.getSignature().getName())
        .startActive(true);
    try {
      return pjp.proceed();
    } catch (Exception ex) {
      SpanUtils.captureException(scope.span(), ex);
      throw ex;
    } finally {
      scope.close();
    }
  }
}
