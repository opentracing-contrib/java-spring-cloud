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
package io.opentracing.contrib.spring.cloud.rabbitmq;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.qpid.server.SystemLauncher;

/**
 * @author Gilles Robert
 */
class EmbeddedQpidBroker {

  static final int BROKER_PORT = 5673;
  private static final String INITIAL_CONFIGURATION = "qpid-config.json";
  private final SystemLauncher systemLauncher = new SystemLauncher();
  private static Map<String, Object> PROPERTIES = new HashMap<>();

  static {
    URL initialConfig =
        EmbeddedQpidBroker.class.getClassLoader().getResource(INITIAL_CONFIGURATION);
    assert initialConfig != null;
    PROPERTIES.put("qpid.amqp_port", String.valueOf(BROKER_PORT));
    PROPERTIES.put("type", "Memory");
    PROPERTIES.put("initialConfigurationLocation", initialConfig.toExternalForm());
    PROPERTIES.put("startupLoggedToSystemOut", true);
  }

  void start() throws Exception {
    systemLauncher.startup(PROPERTIES);
  }

  void stop() {
    systemLauncher.shutdown();
  }
}
