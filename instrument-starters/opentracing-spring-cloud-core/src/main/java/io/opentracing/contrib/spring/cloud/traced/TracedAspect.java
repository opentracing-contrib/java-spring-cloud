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
package io.opentracing.contrib.spring.cloud.traced;

import io.opentracing.Tracer;
import io.opentracing.contrib.spring.cloud.aop.BaseTracingAspect;
import io.opentracing.contrib.spring.cloud.aop.MethodInterceptorSpanDecorator;
import io.opentracing.contrib.spring.cloud.aop.Traced;
import java.util.List;
import java.util.regex.Pattern;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;

@Aspect
public class TracedAspect extends BaseTracingAspect {

  public TracedAspect(Tracer tracer, TracedTracingProperties tracedTracingProperties, List<MethodInterceptorSpanDecorator> decorators) {
    super(tracer, decorators, Traced.class, Pattern.compile(tracedTracingProperties.getSkipPattern()));
  }

  private Traced findAnnotation(ProceedingJoinPoint pjp) {
    return AnnotationUtils
        .findAnnotation(((MethodSignature) pjp.getSignature()).getMethod(), Traced.class);
  }

  @Override
  @Around("execution (@io.opentracing.contrib.spring.cloud.aop.Traced * *.*(..))")
  public Object trace(ProceedingJoinPoint pjp) throws Throwable {
    return super.internalTrace(pjp);
  }

  @Override
  protected String getComponent(ProceedingJoinPoint pjp) {
    return findAnnotation(pjp).component();
  }

  @Override
  protected String getOperationName(ProceedingJoinPoint pjp) {
    String operationName = findAnnotation(pjp).operationName();
    if (operationName == null || "".equals(operationName)) {
      operationName = super.getOperationName(pjp);
    }
    return operationName;
  }
}
