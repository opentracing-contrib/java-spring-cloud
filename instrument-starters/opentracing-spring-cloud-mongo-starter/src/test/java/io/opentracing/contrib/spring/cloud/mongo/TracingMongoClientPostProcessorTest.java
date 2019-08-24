package io.opentracing.contrib.spring.cloud.mongo;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import io.opentracing.Tracer;
import io.opentracing.contrib.mongo.TracingMongoClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;

public class TracingMongoClientPostProcessorTest {

  @Mock
  private Tracer tracer;

  private TracingMongoClientPostProcessor processor;

  @Before
  public void setup() {
    processor = new TracingMongoClientPostProcessor(tracer);
  }

  @Test
  public void testNonMongoClientBeansAreReturnedUnaltered() {

    final Object expected = new Object();

    final Object actual = processor.postProcessAfterInitialization(expected, "any-bean-name");

    assertThat(actual).isSameAs(expected);
  }

  @Test
  public void testMongoClientBeansReplacedWithTracingClient() {

    final MongoClient client = new MongoClient(new MongoClientURI("mongodb://localhost/test", MongoClientOptions.builder()));

    final Object actual = processor.postProcessAfterInitialization(client, "any-bean-name");

    assertThat(actual).isInstanceOf(TracingMongoClient.class);
  }
}
