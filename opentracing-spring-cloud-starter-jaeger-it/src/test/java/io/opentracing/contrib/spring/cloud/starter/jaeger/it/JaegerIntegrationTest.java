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
package io.opentracing.contrib.spring.cloud.starter.jaeger.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;
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
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.LogMessageWaitStrategy;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = DemoSpringBootWebApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers = JaegerIntegrationTest.Initializer.class)
public class JaegerIntegrationTest {


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
  private TestRestTemplate testRestTemplate;

  private RestTemplate restTemplate = new RestTemplate();

  @Test
  public void testJaegerCollectsTraces() {
    final String operation = "hello";
    assertThat(testRestTemplate.getForObject("/" + operation, String.class)).isNotBlank();

    waitJaegerQueryContains(SERVICE_NAME, operation);
  }

  private void waitJaegerQueryContains(String serviceName, String str) {
    await().atMost(30, TimeUnit.SECONDS).until(() -> {
      try {
        final String output = restTemplate.getForObject(
            String.format(
                "%s/api/traces?service=%s",
                String.format(
                    "http://localhost:%d",
                    jaeger.getMappedPort(QUERY_PORT)
                ),
                serviceName
            ),
            String.class
        );
        return output.contains(str);
      } catch (Exception e) {
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
