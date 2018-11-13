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

import static org.mockito.Mockito.when;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.contrib.spring.cloud.aop.MethodInterceptorSpanDecorator;
import io.opentracing.contrib.spring.cloud.aop.Traced;
import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.assertj.core.api.WithAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TracedAspectTest implements WithAssertions {

  @Mock
  private Span span;
  @Mock
  private Tracer tracer;
  @Mock
  private List<MethodInterceptorSpanDecorator> decorators;
  @Mock
  private ProceedingJoinPoint pjp;
  @Mock
  private MethodSignature signature;

  @InjectMocks
  private TracedAspect aspect;

  @Before
  public void init() {
    when(pjp.getSignature()).thenReturn(signature);
  }

  @Test
  public void givenWithActiveSpanOnlyToFalse_whenNoActiveSpan_thenShouldTraceShouldReturnTrue()
      throws Throwable {
    when(signature.getMethod())
        .thenReturn(TracedClass.class.getMethod("tracedWithoutActiveSpanOnly"));

    assertThat(aspect.shouldTrace(pjp)).isTrue();
  }

  @Test
  public void givenWithActiveSpanOnlyToTrue_whenNoActiveSpan_thenShouldTraceShouldReturnFalse()
      throws Throwable {
    when(signature.getMethod()).thenReturn(TracedClass.class.getMethod("traced"));

    assertThat(aspect.shouldTrace(pjp)).isFalse();
  }

  @Test
  public void givenWithActiveSpanOnlyToTrue_whenActiveSpan_thenShouldTraceShouldReturnTrue()
      throws Throwable {
    when(tracer.activeSpan()).thenReturn(span);
    when(signature.getMethod()).thenReturn(TracedClass.class.getMethod("traced"));

    assertThat(aspect.shouldTrace(pjp)).isTrue();
  }

  @Test
  public void givenCustomOperation_whenGettingOperationName_thenOperationNameShouldBeOverriden()
      throws Throwable {
    when(tracer.activeSpan()).thenReturn(span);
    when(signature.getMethod()).thenReturn(TracedClass.class.getMethod("tracedOperation"));

    assertThat(aspect.getOperationName(pjp)).isEqualTo("customOps");
  }

  @Test
  public void givenEmptyOperation_whenGettingOperationName_thenOperationNameShouldBeMethodName()
      throws Throwable {
    when(tracer.activeSpan()).thenReturn(span);
    when(signature.getMethod()).thenReturn(TracedClass.class.getMethod("traced"));

    assertThat(aspect.getOperationName(pjp)).isEqualTo("traced");
  }


  @Test
  public void givenCustomComponent_whenGettingComponent_thenComponentNameShouldBeOverriden()
      throws Throwable {
    when(signature.getMethod()).thenReturn(TracedClass.class.getMethod("tracedComponent"));

    assertThat(aspect.getComponent(pjp)).isEqualTo("custom");
  }

  @Test
  public void givenDefaultComponent_whenGettingComponent_thenComponentNameShouldBeOverriden()
      throws Throwable {
    when(signature.getMethod()).thenReturn(TracedClass.class.getMethod("traced"));

    assertThat(aspect.getComponent(pjp)).isEqualTo("traced");
  }

  public static class TracedClass {

    @Traced
    public void traced() {

    }

    @Traced(operationName = "customOps")
    public void tracedOperation() {

    }

    @Traced(component = "custom")
    public void tracedComponent() {

    }

    @Traced(withActiveSpanOnly = false)
    public void tracedWithoutActiveSpanOnly() {

    }

  }

}
