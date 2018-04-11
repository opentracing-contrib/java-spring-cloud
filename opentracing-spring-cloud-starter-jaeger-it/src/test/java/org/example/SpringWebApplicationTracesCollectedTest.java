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
package org.example;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.LogMessageWaitStrategy;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = DemoSpringBootWebApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers = SpringWebApplicationTracesCollectedTest.Initializer.class)
public class SpringWebApplicationTracesCollectedTest {


  private static final int QUERY_PORT = 16686;
  private static final int COLLECTOR_PORT = 14268;
  private static final String SERVICE_NAME = "spring-boot-test";

  @ClassRule
  public static GenericContainer jaeger
      = new GenericContainer("jaegertracing/all-in-one:1.3")
      .withExposedPorts(COLLECTOR_PORT, QUERY_PORT)
      //make sure we wait until the collector is ready
      .waitingFor(new LogMessageWaitStrategy().withRegEx(".*jaeger-query.*HTTP server.*\n"));

  @Autowired
  private TestRestTemplate restTemplate;

  private OkHttpClient okHttpClient = new OkHttpClient.Builder().build();

  @Test
  public void testJaegerCollectsTraces() {
    ensureThatJaegerCollectorHasInitialized();

    final String operation = "hello";
    assertThat(restTemplate.getForObject("/" + operation, String.class)).isNotBlank();

    waitJaegerQueryContains(SERVICE_NAME, operation);
  }

  private void ensureThatJaegerCollectorHasInitialized() {
    try {
      Thread.sleep(5000);
    } catch (InterruptedException ignored) {
      //comment to keep linter happy
    }
  }

  private void waitJaegerQueryContains(String serviceName, String operation) {
    final Request request = new Request.Builder()
        .url(
            String.format(
                "%s/api/traces?service=%s",
                String.format(
                    "http://localhost:%d",
                    jaeger.getMappedPort(QUERY_PORT)
                ),
                serviceName
            )
        )
        .get()
        .build();

    await().atMost(30, TimeUnit.SECONDS).until(() -> {
      try {
        Response response = okHttpClient.newCall(request).execute();
        final String output = response.body().string();
        return output.contains(operation);
      } catch (IOException e) {
        return false;
      }
    });
  }

  //create the opentracing.jaeger properties from the information of the running container
  public static class Initializer implements
      ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
      EnvironmentTestUtils
          .addEnvironment("testcontainers", configurableApplicationContext.getEnvironment(),
              String.format(
                  "opentracing.jaeger.http-sender.url=http://%s:%d/api/traces",
                  jaeger.getContainerIpAddress(),
                  jaeger.getMappedPort(COLLECTOR_PORT)
              ),
              String.format("spring.application.name=%s", SERVICE_NAME)
      );
    }
  }

}
