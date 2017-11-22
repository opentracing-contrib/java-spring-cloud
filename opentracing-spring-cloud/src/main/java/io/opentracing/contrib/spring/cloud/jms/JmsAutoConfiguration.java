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
package io.opentracing.contrib.spring.cloud.jms;

import io.opentracing.Tracer;
import io.opentracing.contrib.jms.spring.TracingJmsConfiguration;
import io.opentracing.contrib.spring.web.autoconfig.TracerAutoConfiguration;

import javax.jms.Message;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jms.core.JmsTemplate;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @author Eddú Meléndez
 */
@Configuration
@ConditionalOnClass({Message.class, JmsTemplate.class})
@ConditionalOnBean(Tracer.class)
@AutoConfigureAfter(TracerAutoConfiguration.class)
@ConditionalOnProperty(name = "opentracing.spring.cloud.jms.enabled", havingValue = "true", matchIfMissing = true)
@Import(TracingJmsConfiguration.class)
public class JmsAutoConfiguration {

}
