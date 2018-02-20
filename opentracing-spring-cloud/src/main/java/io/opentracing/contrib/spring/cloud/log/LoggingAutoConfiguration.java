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
package io.opentracing.contrib.spring.cloud.log;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.filter.EvaluatorFilter;
import io.opentracing.Tracer;
import io.opentracing.contrib.spring.web.autoconfig.TracerAutoConfiguration;
import javax.annotation.PostConstruct;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * @author Pavol Loffay
 */
@Configuration
@ConditionalOnBean(Tracer.class)
@AutoConfigureAfter(TracerAutoConfiguration.class)
@ConditionalOnClass(ch.qos.logback.classic.Logger.class)
@ConditionalOnProperty(name = "opentracing.spring.cloud.log.enabled", havingValue = "true", matchIfMissing = true)
public class LoggingAutoConfiguration {

  @Autowired
  private Tracer tracer;

  @PostConstruct
  public void postConstruct() {
    SpanLogsAppender spanLogsAppender = new SpanLogsAppender(tracer);
    spanLogsAppender.start();
    Logger rootLogger = getRootLogger();
    rootLogger.addAppender(spanLogsAppender);
  }

  private Logger getRootLogger() {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    return context.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
  }
}
