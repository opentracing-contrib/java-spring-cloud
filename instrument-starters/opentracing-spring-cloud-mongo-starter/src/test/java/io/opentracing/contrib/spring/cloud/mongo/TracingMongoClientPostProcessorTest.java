/**
 * Copyright 2017-2019 The OpenTracing Authors
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

import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import io.opentracing.Tracer;
import io.opentracing.contrib.mongo.TracingMongoClient;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class TracingMongoClientPostProcessorTest {

  @Mock
  private Tracer tracer;

  @Mock
  private TracingMongoClient tracingClient;

  private TracingMongoClientPostProcessor processor;

  private MongodProcess process;

  @Before
  public void setup() throws IOException {
    processor = new TracingMongoClientPostProcessor(tracer);

    MongodStarter starter = MongodStarter.getDefaultInstance();
    MongodExecutable executable = starter.prepare(new MongodConfigBuilder()
        .version(Version.Main.PRODUCTION)
        .net(new Net(27017, Network.localhostIsIPv6()))
        .build());
    process = executable.start();
  }

  @After
  public void tearDown() {
    process.stop();
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

  @Test
  public void testTracingMongoClientBeanNotWrapped() {

    final Object actual = processor.postProcessAfterInitialization(tracingClient, "any-bean-name");

    assertThat(actual).isSameAs(tracingClient);
  }
}
