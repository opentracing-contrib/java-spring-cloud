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
package io.opentracing.contrib.spring.cloud.mongo;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import io.opentracing.Tracer;
import io.opentracing.contrib.mongo.TracingMongoClient;
import io.opentracing.contrib.spring.tracer.configuration.TracerAutoConfiguration;
import java.net.UnknownHostException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * @author Vivien Maleze
 */
@Configuration
@AutoConfigureBefore(MongoAutoConfiguration.class)
@ConditionalOnClass(MongoClient.class)
@ConditionalOnBean(Tracer.class)
@AutoConfigureAfter(TracerAutoConfiguration.class)
@ConditionalOnProperty(name = "opentracing.spring.cloud.mongo.enabled", havingValue = "true", matchIfMissing = true)
public class MongoTracingAutoConfiguration extends MongoAutoConfiguration {

  private Tracer tracer;

  public MongoTracingAutoConfiguration(@Autowired(required = false) MongoProperties properties,
      ObjectProvider<MongoClientOptions> options, Environment environment, Tracer tracer) {
    super(properties, options, environment);
    this.tracer = tracer;
  }

  @Override
  public MongoClient mongo() throws UnknownHostException {
    MongoClient mongo = super.mongo();
    return new TracingMongoClient(tracer, mongo.getAllAddress(), mongo.getMongoClientOptions());
  }
}
