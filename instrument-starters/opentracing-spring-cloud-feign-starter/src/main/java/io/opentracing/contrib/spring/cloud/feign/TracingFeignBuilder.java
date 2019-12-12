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
package io.opentracing.contrib.spring.cloud.feign;

import feign.Client;
import feign.Contract;
import feign.ExceptionPropagationPolicy;
import feign.Feign;
import feign.InvocationHandlerFactory;
import feign.Logger;
import feign.QueryMapEncoder;
import feign.Request;
import feign.RequestInterceptor;
import feign.ResponseMapper;
import feign.Retryer;
import feign.Target;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;

public class TracingFeignBuilder extends Feign.Builder {

  private final TracedFeignBeanFactory tracedFeignBeanFactory;
  private final Feign.Builder delegate;

  public TracingFeignBuilder(Feign.Builder delegate, TracedFeignBeanFactory tracedFeignBeanFactory) {
    this.tracedFeignBeanFactory = tracedFeignBeanFactory;
    this.delegate = delegate;
    client(new Client.Default(null, null));
  }

  @Override
  public Feign build() {
    return super.build();
  }

  @Override
  public Feign.Builder logLevel(Logger.Level logLevel) {
    delegate.logLevel(logLevel);
    return this;
  }

  @Override
  public Feign.Builder contract(Contract contract) {
    delegate.contract(contract);
    return this;
  }

  @Override
  public Feign.Builder client(Client client) {
    delegate.client((Client) tracedFeignBeanFactory.from(client));
    return this;
  }

  @Override
  public Feign.Builder retryer(Retryer retryer) {
    delegate.retryer(retryer);
    return this;
  }

  @Override
  public Feign.Builder logger(Logger logger) {
    delegate.logger(logger);
    return this;
  }

  @Override
  public Feign.Builder encoder(Encoder encoder) {
    delegate.encoder(encoder);
    return this;
  }

  @Override
  public Feign.Builder decoder(Decoder decoder) {
    delegate.decoder(decoder);
    return this;
  }

  @Override
  public Feign.Builder queryMapEncoder(QueryMapEncoder queryMapEncoder) {
    delegate.queryMapEncoder(queryMapEncoder);
    return this;
  }

  @Override
  public Feign.Builder mapAndDecode(ResponseMapper mapper, Decoder decoder) {
    delegate.mapAndDecode(mapper, decoder);
    return this;
  }

  @Override
  public Feign.Builder decode404() {
    delegate.decode404();
    return this;
  }

  @Override
  public Feign.Builder errorDecoder(ErrorDecoder errorDecoder) {
    delegate.errorDecoder(errorDecoder);
    return this;
  }

  @Override
  public Feign.Builder options(Request.Options options) {
    delegate.options(options);
    return this;
  }

  @Override
  public Feign.Builder requestInterceptor(RequestInterceptor requestInterceptor) {
    delegate.requestInterceptor(requestInterceptor);
    return this;
  }

  @Override
  public Feign.Builder requestInterceptors(Iterable<RequestInterceptor> requestInterceptors) {
    delegate.requestInterceptors(requestInterceptors);
    return this;
  }

  @Override
  public Feign.Builder invocationHandlerFactory(InvocationHandlerFactory invocationHandlerFactory) {
    delegate.invocationHandlerFactory(invocationHandlerFactory);
    return this;
  }

  @Override
  public Feign.Builder doNotCloseAfterDecode() {
    delegate.doNotCloseAfterDecode();
    return this;
  }

  @Override
  public Feign.Builder exceptionPropagationPolicy(ExceptionPropagationPolicy propagationPolicy) {
    delegate.exceptionPropagationPolicy(propagationPolicy);
    return this;
  }

  @Override
  public <T> T target(Class<T> apiType, String url) {
    return delegate.target(apiType, url);
  }

  @Override
  public <T> T target(Target<T> target) {
    return delegate.target(target);
  }
}
