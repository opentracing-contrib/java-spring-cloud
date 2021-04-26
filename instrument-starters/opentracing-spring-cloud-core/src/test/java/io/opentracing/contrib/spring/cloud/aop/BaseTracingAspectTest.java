/**
 * Copyright 2017-2021 The OpenTracing Authors
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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.mock.MockTracer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.assertj.core.api.ThrowableAssert;
import org.assertj.core.api.WithAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.stereotype.Component;

@RunWith(MockitoJUnitRunner.class)
public class BaseTracingAspectTest implements WithAssertions {

  private MockTracer tracer = new MockTracer();
  private List<MethodInterceptorSpanDecorator> decorators = new ArrayList<MethodInterceptorSpanDecorator>();
  @Mock
  private MethodInterceptorSpanDecorator decorator;
  @Mock
  private ProceedingJoinPoint pjp;
  @Mock
  private MethodSignature methodSignature;

  private TestTracingAspect aspect;

  private Method method;
  @Mock
  private Object result;

  @Before
  public void init() throws Throwable {
    method = Object.class.getMethod("toString", (Class<?>[]) null);
    aspect = new TestTracingAspect(tracer, decorators, Component.class);
    decorators.add(decorator);
    when(pjp.getSignature()).thenReturn(methodSignature);
    when(methodSignature.getMethod()).thenReturn(method);
    when(pjp.proceed()).thenReturn(result);
    when(pjp.getTarget()).thenReturn(Object.class);
  }

  @Test
  public void givenDecorators_whenMethodIsIntercepted_thenPreProceedShouldBeCalled()
      throws Throwable {
    aspect.trace(pjp);

    verify(decorator).onPreProceed(same(pjp), Matchers.<Span>any());
  }

  @Test
  public void givenDecorators_whenMethodIsIntercepted_thenPostProceedShouldBeCalled()
      throws Throwable {
    Object result = aspect.trace(pjp);

    verify(decorator).onPostProceed(same(pjp), same(result), Matchers.<Span>any());
  }

  @Test
  public void givenNoExceptionOnProceed_whenMethodIsIntercepted_thenOnErrorShouldNotBeCalled()
      throws Throwable {
    aspect.trace(pjp);

    verify(decorator, never())
        .onError(any(ProceedingJoinPoint.class), any(Exception.class), any(Span.class));
  }

  @Test
  public void givenExceptionOnProceed_whenMethodIsIntercepted_thenExceptionShouldBeRethrowed()
      throws Throwable {
    RuntimeException e = new RuntimeException();
    when(pjp.proceed()).thenThrow(e);
    assertThatThrownBy(new ThrowableAssert.ThrowingCallable() {
      @Override
      public void call() throws Throwable {
        aspect.trace(pjp);
      }
    }).isSameAs(e);
  }

  @Test
  public void givenExceptionOnProceed_whenMethodIsIntercepted_thenOnErrorShouldBeCalled()
      throws Throwable {
    RuntimeException e = new RuntimeException();
    when(pjp.proceed()).thenThrow(e);

    assertThatThrownBy(new ThrowableAssert.ThrowingCallable() {
      @Override
      public void call() throws Throwable {
        aspect.trace(pjp);
      }
    }).isSameAs(e);

    verify(decorator).onError(same(pjp), same(e), Matchers.<Span>any());
  }

  public static class TestTracingAspect extends BaseTracingAspect {

    public TestTracingAspect(Tracer tracer, List<MethodInterceptorSpanDecorator> decorators,
        Class<? extends Annotation> annotation) {
      super(tracer, decorators, annotation, Pattern.compile(""));
    }

    @Override
    public Object trace(ProceedingJoinPoint pjp) throws Throwable {
      return super.internalTrace(pjp);
    }

  }

}
