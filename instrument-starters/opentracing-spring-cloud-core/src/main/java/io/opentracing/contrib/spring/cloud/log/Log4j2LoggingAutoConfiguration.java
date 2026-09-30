/*
 * Copyright © 2017-2026 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.opentracing.contrib.spring.cloud.log;

import io.opentracing.Tracer;
import io.opentracing.contrib.spring.tracer.configuration.TracerAutoConfiguration;
import javax.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnBean(Tracer.class)
@AutoConfigureAfter(TracerAutoConfiguration.class)
@ConditionalOnClass(name = "org.apache.logging.log4j.core.LoggerContext")
@ConditionalOnProperty(name = "opentracing.spring.cloud.log.enabled", havingValue = "true", matchIfMissing = true)
public class Log4j2LoggingAutoConfiguration {

  @Autowired
  private Tracer tracer;

  @PostConstruct
  public void postConstruct() {
    LoggerContext context = (LoggerContext) LogManager.getContext(false);
    LoggerConfig rootLogger = context.getConfiguration().getRootLogger();
    Log4j2SpanLogsAppender appender = new Log4j2SpanLogsAppender(tracer);
    appender.start();
    rootLogger.addAppender(appender, null, null);
    context.updateLoggers();
  }
}
