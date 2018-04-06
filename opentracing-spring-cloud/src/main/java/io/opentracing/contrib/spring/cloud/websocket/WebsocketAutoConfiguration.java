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
package io.opentracing.contrib.spring.cloud.websocket;

import io.opentracing.Tracer;
import io.opentracing.contrib.spring.web.autoconfig.TracerAutoConfiguration;
import io.opentracing.tag.Tags;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurationSupport;

@Configuration
@ConditionalOnBean(Tracer.class)
@AutoConfigureAfter(TracerAutoConfiguration.class)
@ConditionalOnClass({ChannelInterceptor.class,WebSocketMessageBrokerConfigurationSupport.class})
@ConditionalOnProperty(name = "opentracing.spring.cloud.websocket.enabled", havingValue = "true", matchIfMissing = true)
public class WebsocketAutoConfiguration {

  @Autowired
  private Tracer tracer;

  @Bean
  @ConditionalOnBean(WebSocketMessageBrokerConfigurationSupport.class)
  public TracingChannelInterceptor tracingInboundChannelInterceptor(
      WebSocketMessageBrokerConfigurationSupport config) {
    TracingChannelInterceptor interceptor = new TracingChannelInterceptor(tracer,
        Tags.SPAN_KIND_SERVER);
    config.clientInboundChannel().addInterceptor(interceptor);
    return interceptor;
  }

  @Bean
  @ConditionalOnBean(WebSocketMessageBrokerConfigurationSupport.class)
  public TracingChannelInterceptor tracingOutboundChannelInterceptor(
      WebSocketMessageBrokerConfigurationSupport config) {
    TracingChannelInterceptor interceptor = new TracingChannelInterceptor(tracer,
        Tags.SPAN_KIND_CLIENT);
    config.clientOutboundChannel().addInterceptor(interceptor);
    return interceptor;
  }
}