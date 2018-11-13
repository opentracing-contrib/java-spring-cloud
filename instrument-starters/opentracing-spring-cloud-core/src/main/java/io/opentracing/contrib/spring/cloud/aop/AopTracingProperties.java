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
package io.opentracing.contrib.spring.cloud.aop;

import io.opentracing.contrib.spring.web.starter.properties.MDCProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("opentracing.spring.cloud.aop")
public class AopTracingProperties {

  /**
   * Enable tracing for {@link org.springframework.scheduling.annotation.Scheduled}
   */
  private boolean enabled = true;
  private MDCProperties mdc = new MDCProperties();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public MDCProperties getMdc() {
    return mdc;
  }

  public void setMdc(MDCProperties mdc) {
    this.mdc = mdc;
  }
}