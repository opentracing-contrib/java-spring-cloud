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
package io.opentracing.contrib.spring.cloud;

import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;

import org.springframework.beans.factory.BeanFactory;

public class TracerUtils {

  private TracerUtils() {
  }

  /**
   * This method returns a proxy that will lazily retrieve the Tracer
   * bean when first used. This is to avoid premature resolution of
   * the Tracer bean during evaluation of BeanPostProcessors, which
   * would subsequently prevent the Tracer bean itself from being
   * post processed.
   *
   * @param beanFactory The bean factory
   * @return The tracer proxy
   */
  public static Tracer getTracer(BeanFactory beanFactory) {
    return new TracerProxy(beanFactory);
  }

  public static class TracerProxy implements Tracer {
    private BeanFactory beanFactory;
    private Tracer tracer = null;

    public TracerProxy(BeanFactory beanFactory) {
      this.beanFactory = beanFactory;   
    }

    protected synchronized Tracer getTracer() {
      if (tracer == null) {
        tracer = beanFactory.getBean(Tracer.class);
      }
      return tracer;
    }

    @Override
    public ScopeManager scopeManager() {
      return getTracer() == null ? null : getTracer().scopeManager();
    }

    @Override
    public Span activeSpan() {
      return getTracer() == null ? null : getTracer().activeSpan();
    }

    @Override
    public SpanBuilder buildSpan(String operationName) {
      return getTracer() == null ? null : getTracer().buildSpan(operationName);
    }

    @Override
    public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
      if (getTracer() != null) {
        getTracer().inject(spanContext, format, carrier);
      }
    }

    @Override
    public <C> SpanContext extract(Format<C> format, C carrier) {
      return getTracer() == null ? null : getTracer().extract(format, carrier);
    }
      
  }
}
