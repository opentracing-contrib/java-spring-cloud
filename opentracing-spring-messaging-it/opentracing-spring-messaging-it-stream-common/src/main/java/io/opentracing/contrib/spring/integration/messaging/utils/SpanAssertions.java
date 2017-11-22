/**
 * Copyright 2017 The OpenTracing Authors
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

package io.opentracing.contrib.spring.integration.messaging.utils;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentracing.mock.MockSpan;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public interface SpanAssertions {

  static void assertEvents(MockSpan span, List<String> expectedEvents) {
    List<String> actualEvents = span.logEntries()
            .stream()
            .map(MockSpan.LogEntry::fields)
            .map(Map::entrySet)
            .flatMap(Set::stream)
            .filter(e -> "event".equals(e.getKey()))
            .map(Map.Entry::getValue)
            .map(String::valueOf)
            .collect(Collectors.toList());

    assertThat(actualEvents).containsAll(expectedEvents);
  }

}
