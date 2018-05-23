package io.opentracing.contrib.spring.cloud.mongo;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import com.mongodb.MongoClient;
import io.opentracing.Tracer;
import org.junit.Test;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author  Vivien Maleze
 */
public class MongoTracingAutoConfigurationTest {

    @Test
    public void loadMongoTracingByDefault() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(TracerConfig.class, MongoTracingAutoConfiguration.class);
        context.refresh();
        MongoClient mongoClient = context.getBean(MongoClient.class);
        assertNotNull(mongoClient);
    }

    @Test
    public void disableMongoTracing() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(TracerConfig.class, MongoTracingAutoConfiguration.class);
        EnvironmentTestUtils.addEnvironment(context, "opentracing.spring.cloud.mongo.enabled:false");
        context.refresh();
        String[] tracingMongoClientBeans = context.getBeanNamesForType(MongoClient.class);
        assertThat(tracingMongoClientBeans.length, is(0));
    }

    @Configuration
    static class TracerConfig {

        @Bean
        public Tracer tracer() {
            return mock(Tracer.class);
        }
    }
}