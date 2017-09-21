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
package io.opentracing.contrib.spring.cloud.hystrix;

import com.netflix.hystrix.HystrixCommand;
import io.opentracing.ActiveSpan;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;

/**
 * Implement this class if you need traced variant of {@link HystrixCommand}. It creates
 * a span that wraps the whole invocation.
 */
public abstract class TracedHystrixCommand<R> extends HystrixCommand<R> {

  static final String TAG_HYSTRIX_COMPONENT = "hystrix";
  static final String TAG_COMMAND_KEY = "commandKey";
  static final String TAG_COMMAND_GROUP = "commandGroup";
  static final String TAG_THREAD_POOL_KEY = "threadPoolKey";

  private final Tracer tracer;

  protected TracedHystrixCommand(Tracer tracer, Setter setter) {
    super(setter);
    this.tracer = tracer;
  }

  @Override
  protected R run() throws Exception {
    String commandKeyName = getCommandKey().name();

    try (ActiveSpan span = this.tracer.buildSpan(commandKeyName)
      .withTag(Tags.COMPONENT.getKey(), TAG_HYSTRIX_COMPONENT)
      .withTag(TAG_COMMAND_KEY, commandKeyName)
      .withTag(TAG_COMMAND_GROUP, commandGroup.name())
      .withTag(TAG_THREAD_POOL_KEY, threadPoolKey.name())
        .startActive()) {
        return doRun();
        }
    }

  public abstract R doRun() throws Exception;
}
