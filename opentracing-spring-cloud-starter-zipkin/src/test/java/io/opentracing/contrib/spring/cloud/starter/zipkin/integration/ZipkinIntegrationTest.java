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
package io.opentracing.contrib.spring.cloud.starter.zipkin.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import zipkin.Span;
import zipkin.junit.ZipkinRule;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = ZipkinIntegrationTest.DemoSpringApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers = ZipkinIntegrationTest.Initializer.class)
public class ZipkinIntegrationTest {

  private static final String SERVICE_NAME = "spring-boot-test";
  @ClassRule
  public static ZipkinRule zipkin = new ZipkinRule();

  @Autowired
  private TestRestTemplate testRestTemplate;

  @Test
  public void testZipkinCollectsTraces() {
    assertThat(testRestTemplate.getForObject("/hello", String.class)).isNotBlank();
    assertSpanIsCorrect();
  }

  private void assertSpanIsCorrect() {
    await().atMost(10, TimeUnit.SECONDS).until(() -> zipkin.getTraces().size() == 1);
    final List<List<Span>> traces = zipkin.getTraces();
    final List<Span> spans = traces.get(0);
    assertThat(spans.get(0).serviceNames()).containsExactly(SERVICE_NAME);
  }

  //create the opentracing.jaeger properties from the information of the running container
  public static class Initializer implements
      ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
      EnvironmentTestUtils
          .addEnvironment("zipkinrule", configurableApplicationContext.getEnvironment(),
              String.format("opentracing.zipkin.http-sender.baseUrl=%s", zipkin.httpUrl()),
              String.format("spring.application.name=%s", SERVICE_NAME)
      );
    }
  }

  @SpringBootApplication
  @RestController
  public static class DemoSpringApplication {

    public static void main(String[] args) {
      SpringApplication.run(DemoSpringApplication.class, args);
    }

    @GetMapping("/hello")
    public String hello() {
      return "hello";
    }
  }
}
