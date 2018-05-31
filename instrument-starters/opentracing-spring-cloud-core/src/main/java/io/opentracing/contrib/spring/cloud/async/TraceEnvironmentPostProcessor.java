/**
 * Copyright 2017 The OpenTracing Authors Copyright 2013-2017 the original author or authors.
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
package io.opentracing.contrib.spring.cloud.async;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

/**
 * Adds default properties for the application:
 * <ul>
 *     <li>logging pattern level that prints trace information (e.g. trace ids)</li>
 *     <li>enables usage of subclass-based (CGLIB) proxies are to be created as opposed
 * to standard Java interface-based proxies</li>
 * </ul>
 * It's required for the tracing aspects like
 * {@link io.opentracing.contrib.spring.cloud.async.TraceAsyncAspect}
 *
 * @author Dave Syer
 * @author Pavol Loffay
 */
public class TraceEnvironmentPostProcessor implements EnvironmentPostProcessor {

  private static final String PROPERTY_SOURCE_NAME = "defaultProperties";
  private static final String SPRING_AOP_PROXY_TARGET_CLASS = "spring.aop.proxyTargetClass";

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment,
      SpringApplication application) {
    Map<String, Object> map = new HashMap<>();

    if (!environment.containsProperty(SPRING_AOP_PROXY_TARGET_CLASS)) {
      map.put(SPRING_AOP_PROXY_TARGET_CLASS, "true");
    }

    addOrReplace(environment.getPropertySources(), map);
  }

  private void addOrReplace(MutablePropertySources propertySources,
      Map<String, Object> map) {
    MapPropertySource target = null;
    if (propertySources.contains(PROPERTY_SOURCE_NAME)) {
      PropertySource<?> source = propertySources.get(PROPERTY_SOURCE_NAME);
      if (source instanceof MapPropertySource) {
        target = (MapPropertySource) source;
        for (String key : map.keySet()) {
          if (!target.containsProperty(key)) {
            target.getSource().put(key, map.get(key));
          }
        }
      }
    }
    if (target == null) {
      target = new MapPropertySource(PROPERTY_SOURCE_NAME, map);
    }
    if (!propertySources.contains(PROPERTY_SOURCE_NAME)) {
      propertySources.addLast(target);
    }
  }
}
