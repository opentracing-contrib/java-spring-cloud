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

package io.opentracing.contrib.spring.cloud.starter.zipkin;

import io.opentracing.Tracer;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import zipkin2.reporter.AsyncReporter;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    ZipkinAutoConfiguration.class
})
public abstract class AbstractZipkinTracerSpringTest {

  @Autowired(required = false)
  protected Tracer tracer;

  @Autowired(required = false)
  protected AsyncReporter<zipkin2.Span> reporter;


  protected brave.opentracing.BraveTracer getTracer() {
    return (brave.opentracing.BraveTracer) tracer;
  }
}
