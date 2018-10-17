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
package io.opentracing.contrib.spring.cloud.feign;

import feign.Request;
import feign.Request.Options;
import feign.Response;
import feign.opentracing.FeignSpanDecorator;
import io.opentracing.Span;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Emerson Oliveira
 */
@Configuration
public class FeignSpanDecoratorConfiguration {

  @Bean
  public FeignSpanDecorator feignSpanDecorator() {
    return new MyFeignSpanDecorator();
  }

  @Bean
  public FeignSpanDecorator anotherFeignSpanDecorator() {
    return new AnotherFeignSpanDecorator();
  }

  static class MyFeignSpanDecorator implements FeignSpanDecorator {

    static final String TAG_NAME = "MY_TAG";
    static final String TAG_VALUE = "MY_TAG_VALUE";

    @Override
    public void onRequest(Request request, Options options, Span span) {
      span.setTag(TAG_NAME, TAG_VALUE);
    }

    @Override
    public void onResponse(Response response, Options options, Span span) {
    }

    @Override
    public void onError(Exception e, Request request, Span span) {
    }
  }

  static class AnotherFeignSpanDecorator implements FeignSpanDecorator {

    static final String TAG_NAME = "ANOTHER_TAG";
    static final String TAG_VALUE = "ANOTHER_TAG_VALUE";

    @Override
    public void onRequest(Request request, Options options, Span span) {
      span.setTag(TAG_NAME, TAG_VALUE);
    }

    @Override
    public void onResponse(Response response, Options options, Span span) {
    }

    @Override
    public void onError(Exception e, Request request, Span span) {
    }
  }

}
