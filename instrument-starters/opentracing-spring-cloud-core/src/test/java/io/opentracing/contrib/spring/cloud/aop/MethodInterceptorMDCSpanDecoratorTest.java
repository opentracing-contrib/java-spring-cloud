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

import static org.mockito.Matchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import io.opentracing.Span;
import io.opentracing.contrib.mdc.MDCSpanDecorator;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MethodInterceptorMDCSpanDecoratorTest {

  @Mock
  private MDCSpanDecorator mdcDecorator;
  @Mock
  private ProceedingJoinPoint pjp;
  @Mock
  private Object result;
  @Mock
  private Throwable throwable;
  @Mock
  private Span span;

  @InjectMocks
  private MethodInterceptorMDCSpanDecorator decorator;

  @Test
  public void givenMdcDecorator_whenPreProceedIsCalled_thenDecoratorIsCalled() {
    decorator.onPreProceed(pjp, span);

    verify(mdcDecorator).decorate(same(span));
  }

  @Test
  public void givenMdcDecorator_whenPreProceedIsCalled_thenParamAreNeverUsed() {
    decorator.onPreProceed(pjp, span);

    verifyZeroInteractions(span);
    verifyZeroInteractions(pjp);
  }

  @Test
  public void givenMdcDecorator_whenPostProceedIsCalled_thenParamAreNeverUsed() {
    decorator.onPostProceed(pjp, result, span);

    verifyZeroInteractions(span);
    verifyZeroInteractions(pjp);
    verifyZeroInteractions(result);
  }

  @Test
  public void givenMdcDecorator_whenOnErrorIsCalled_thenParamAreNeverUsed() {
    decorator.onError(pjp, throwable, span);

    verifyZeroInteractions(span);
    verifyZeroInteractions(throwable);
    verifyZeroInteractions(pjp);
  }

  @Test
  public void givenMdcDecorator_whenPostProceedIsCalled_thenMdcDecoratorIsNeverUsed() {
    decorator.onPostProceed(pjp, result, span);

    verifyZeroInteractions(mdcDecorator);
  }

  @Test
  public void givenMdcDecorator_whenOnErrorIsCalled_thenMdcDecoratorIsNeverUsed() {
    decorator.onError(pjp, throwable, span);

    verifyZeroInteractions(mdcDecorator);
  }

}
