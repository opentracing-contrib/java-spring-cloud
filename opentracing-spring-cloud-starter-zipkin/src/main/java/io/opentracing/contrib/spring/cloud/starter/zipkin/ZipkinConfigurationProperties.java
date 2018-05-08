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

import org.springframework.boot.context.properties.ConfigurationProperties;
import zipkin2.codec.SpanBytesEncoder;

@ConfigurationProperties("opentracing.zipkin")
public class ZipkinConfigurationProperties {

  private final HttpSender httpSender = new HttpSender();
  private final BoundarySampler boundarySampler = new BoundarySampler();
  private final CountingSampler countingSampler = new CountingSampler();

  /**
   * Enable Zipkin/Brave Tracer
   */
  private boolean enabled = true;


  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public HttpSender getHttpSender() {
    return httpSender;
  }

  public BoundarySampler getBoundarySampler() {
    return boundarySampler;
  }

  public CountingSampler getCountingSampler() {
    return countingSampler;
  }

  public static class HttpSender {
    /**
     * Zipkin base URL without endpoint e.g. /api/v2/spans
     */
    private String baseUrl = "http://localhost:9411/";

    public String getBaseUrl() {
      return baseUrl;
    }

    public void setBaseUrl(String url) {
      this.baseUrl = url;
    }

    /**
     * Encoding of spans sent to Zipkin server. Use {@link SpanBytesEncoder#JSON_V1} if you are using
     * older server.
     */
    private SpanBytesEncoder encoder = SpanBytesEncoder.JSON_V2;

    public SpanBytesEncoder getEncoder() {
      return encoder;
    }

    public void setEncoder(SpanBytesEncoder encoder) {
      this.encoder = encoder;
    }
  }

  public static class BoundarySampler {

    private Float rate;

    public Float getRate() {
      return rate;
    }

    public void setRate(Float rate) {
      this.rate = rate;
    }
  }

  public static class CountingSampler {

    private Float rate;

    public Float getRate() {
      return rate;
    }

    public void setRate(Float rate) {
      this.rate = rate;
    }
  }
}
