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

    private String url = "http://localhost:9411/api/v2/spans";

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = url;
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
