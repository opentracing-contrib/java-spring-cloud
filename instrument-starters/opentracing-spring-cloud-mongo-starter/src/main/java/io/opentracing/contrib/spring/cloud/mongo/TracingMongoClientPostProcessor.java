package io.opentracing.contrib.spring.cloud.mongo;

import com.mongodb.MongoClient;
import io.opentracing.Tracer;
import io.opentracing.contrib.mongo.TracingMongoClient;
import io.opentracing.contrib.mongo.common.TracingCommandListener;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class TracingMongoClientPostProcessor implements BeanPostProcessor {

  private final Tracer tracer;

  TracingMongoClientPostProcessor(Tracer tracer) {
    this.tracer = tracer;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

    if (bean instanceof MongoClient) {
      final MongoClient client = (MongoClient) bean;

      final TracingCommandListener commandListener = new TracingCommandListener.Builder(tracer)
          .build();

      return new TracingMongoClient(commandListener, client.getAllAddress(), client.getCredentialsList(), client.getMongoClientOptions());
    }

    return bean;
  }
}
