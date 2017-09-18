/**
 * Copyright 2017 The OpenTracing Authors
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

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;

@Configuration
@ConditionalOnProperty(name = "opentracing.spring.cloud.websocket.enabled", havingValue = "true", matchIfMissing = true)
public class WebsocketAutoConfiguration {

    @Autowired
    Tracer tracer;

    @Autowired(required=false)
    private ExecutorSubscribableChannel clientInboundChannel;
    
    @Autowired(required=false)
    private ExecutorSubscribableChannel clientOutboundChannel; 

    @Bean
    public TracingChannelInterceptor tracingInboundChannelInterceptor() {
        return new TracingChannelInterceptor(tracer, Tags.SPAN_KIND_SERVER);
    }

    @Bean
    public TracingChannelInterceptor tracingOutboundChannelInterceptor() {
        return new TracingChannelInterceptor(tracer, Tags.SPAN_KIND_CLIENT);
    }

    @PostConstruct
    private void addTraceInterceptor() {
        if (clientInboundChannel != null) {
            clientInboundChannel.addInterceptor(tracingInboundChannelInterceptor());
        }
        if (clientOutboundChannel != null) {
            clientOutboundChannel.addInterceptor(tracingOutboundChannelInterceptor());
        }
    }
}