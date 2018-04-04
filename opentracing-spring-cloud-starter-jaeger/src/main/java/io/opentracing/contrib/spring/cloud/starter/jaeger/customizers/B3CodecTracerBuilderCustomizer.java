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
package io.opentracing.contrib.spring.cloud.starter.jaeger.customizers;

import com.uber.jaeger.Tracer;
import com.uber.jaeger.propagation.B3TextMapCodec;
import io.opentracing.contrib.spring.cloud.starter.jaeger.TracerBuilderCustomizer;
import io.opentracing.propagation.Format;

public class B3CodecTracerBuilderCustomizer implements TracerBuilderCustomizer {

  @Override
  public void customize(Tracer.Builder builder) {
    B3TextMapCodec injector = new B3TextMapCodec();

    builder.registerInjector(Format.Builtin.HTTP_HEADERS, injector)
        .registerExtractor(Format.Builtin.HTTP_HEADERS, injector);
  }
}
