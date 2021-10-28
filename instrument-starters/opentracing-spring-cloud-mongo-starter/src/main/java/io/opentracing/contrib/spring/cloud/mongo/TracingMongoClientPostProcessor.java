/*
 * Copyright 2017-2021 The OpenTracing Authors
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
import io.opentracing.Tracer;
import io.opentracing.contrib.mongo.TracingMongoClient;
import io.opentracing.contrib.mongo.common.TracingCommandListener;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

class TracingMongoClientPostProcessor implements BeanPostProcessor {

  private final Tracer tracer;

  TracingMongoClientPostProcessor(Tracer tracer) {
    this.tracer = tracer;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

    if (bean instanceof MongoClient && !(bean instanceof TracingMongoClient)) {
      final MongoClient client = (MongoClient) bean;

      final TracingCommandListener commandListener = new TracingCommandListener.Builder(tracer)
          .build();

      return new TracingMongoClient(commandListener, client.getServerAddressList(), client.getCredentialsList(), client.getMongoClientOptions());
    }

    return bean;
  }
}
