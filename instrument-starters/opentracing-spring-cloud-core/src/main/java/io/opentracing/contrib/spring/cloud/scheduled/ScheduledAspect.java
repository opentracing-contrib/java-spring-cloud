/**
 * Copyright 2017-2019 The OpenTracing Authors
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
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.contrib.spring.cloud.ExtensionTags;
import io.opentracing.contrib.spring.cloud.SpanUtils;
import io.opentracing.contrib.spring.cloud.aop.BaseTracingAspect;
import io.opentracing.contrib.spring.cloud.aop.MethodInterceptorSpanDecorator;
import io.opentracing.tag.Tags;
import java.util.List;
import java.util.regex.Pattern;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * @author Pavol Loffay
 */
@Aspect
public class ScheduledAspect extends BaseTracingAspect {

  private Tracer tracer;

  public ScheduledAspect(Tracer tracer, ScheduledTracingProperties scheduledTracingProperties, List<MethodInterceptorSpanDecorator> decorators) {
    super(tracer, decorators, Scheduled.class, Pattern.compile(scheduledTracingProperties.getSkipPattern()));
  }

  @Around("execution (@org.springframework.scheduling.annotation.Scheduled  * *.*(..))")
  public Object trace(final ProceedingJoinPoint pjp) throws Throwable {
    return this.internalTrace(pjp);
  }
}
