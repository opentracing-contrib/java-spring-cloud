/**
 * Copyright 2017-2019 The OpenTracing Authors
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
package io.opentracing.contrib.spring.cloud.async.instrument;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.opentracing.Tracer;
import io.opentracing.contrib.concurrent.TracedCallable;
import io.opentracing.contrib.concurrent.TracedRunnable;
import io.opentracing.contrib.spring.cloud.async.TracedExecutorTest;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ErrorHandler;
import org.springframework.util.ReflectionUtils;


/**
 * Unit tests all API surface area for TracedThreadPoolTaskScheduler. There is value in integration testing these
 * via {@code SpringBootTest} but due to the amount of methods we rely on unit tests as a whole and then run a
 * couple of the methods through integration tests in {@link TracedThreadPoolTaskSchedulerIntegrationTest}. The
 * thinking is that the plumping that makes it all work is the wrapping in TracedRunnable/TracedCallable so
 * long as we verify that in unit tests all should be good.
 *
 * @author cbono
 */
public class TracedThreadPoolTaskSchedulerTest {

  private static final Field TRACED_RUNNABLE_DELEGATE_FIELD = ReflectionUtils.findField(TracedRunnable.class, "delegate");
  private static final Field TRACED_RUNNABLE_TRACER_FIELD = ReflectionUtils.findField(TracedRunnable.class, "tracer");
  private static final Field TRACED_CALLABLE_DELEGATE_FIELD = ReflectionUtils.findField(TracedCallable.class, "delegate");
  private static final Field TRACED_CALLABLE_TRACER_FIELD = ReflectionUtils.findField(TracedCallable.class, "tracer");
  static {
    ReflectionUtils.makeAccessible(TRACED_RUNNABLE_DELEGATE_FIELD);
    ReflectionUtils.makeAccessible(TRACED_RUNNABLE_TRACER_FIELD);
    ReflectionUtils.makeAccessible(TRACED_CALLABLE_DELEGATE_FIELD);
    ReflectionUtils.makeAccessible(TRACED_CALLABLE_TRACER_FIELD);
  }

  private final Runnable mockRunnable = mock(Runnable.class);
  private final Callable mockCallable = mock(Callable.class);
  private final Tracer mockTracer = mock(Tracer.class);
  private final ThreadPoolTaskScheduler delegate = mock(ThreadPoolTaskScheduler.class);
  private final TracedThreadPoolTaskScheduler scheduler = new TracedThreadPoolTaskScheduler(
      mockTracer, delegate);

  @Test
  public void setPoolSize() {
    scheduler.setPoolSize(10);
    verify(delegate).setPoolSize(10);
  }

  @Test
  public void setRemoveOnCancelPolicy() {
    scheduler.setRemoveOnCancelPolicy(true);
    verify(delegate).setRemoveOnCancelPolicy(true);
  }

  @Test
  public void setErrorHandler() {
    final ErrorHandler errorHandler = mock(ErrorHandler.class);
    scheduler.setErrorHandler(errorHandler);
    verify(delegate).setErrorHandler(errorHandler);
  }

  @Test
  public void getScheduledExecutor() {
    scheduler.getScheduledExecutor();
    verify(delegate).getScheduledExecutor();
  }

  @Test
  public void getScheduledThreadPoolExecutor() {
    scheduler.getScheduledThreadPoolExecutor();
    verify(delegate).getScheduledThreadPoolExecutor();
  }

  @Test
  public void getPoolSize() {
    scheduler.getPoolSize();
    verify(delegate).getPoolSize();
  }

  @Test
  public void isRemoveOnCancelPolicy() {
    scheduler.isRemoveOnCancelPolicy();
    verify(delegate).isRemoveOnCancelPolicy();
  }

  @Test
  public void getActiveCount() {
    scheduler.getActiveCount();
    verify(delegate).getActiveCount();
  }

  @Test
  public void execute() {
    final ArgumentCaptor<TracedRunnable> argumentCaptor = ArgumentCaptor.forClass(TracedRunnable.class);
    scheduler.execute(mockRunnable);
    verify(delegate).execute(argumentCaptor.capture());
    verifyTracedRunnable(argumentCaptor.getValue(), mockRunnable, mockTracer);
  }

  @Test
  public void executeWithTimeout() {
    final ArgumentCaptor<TracedRunnable> argumentCaptor = ArgumentCaptor.forClass(TracedRunnable.class);
    scheduler.execute(mockRunnable, 1000L);
    verify(delegate).execute(argumentCaptor.capture(), eq(1000L));
    verifyTracedRunnable(argumentCaptor.getValue(), mockRunnable, mockTracer);
  }

  @Test
  public void submitRunnable() {
    final ArgumentCaptor<TracedRunnable> argumentCaptor = ArgumentCaptor.forClass(TracedRunnable.class);
    scheduler.submit(mockRunnable);
    verify(delegate).submit(argumentCaptor.capture());
    verifyTracedRunnable(argumentCaptor.getValue(), mockRunnable, mockTracer);
  }

  @Test
  public void submitCallable() {
    final ArgumentCaptor<TracedCallable> argumentCaptor = ArgumentCaptor.forClass(TracedCallable.class);
    scheduler.submit(mockCallable);
    verify(delegate).submit(argumentCaptor.capture());
    verifyTracedCallable(argumentCaptor.getValue(), mockCallable, mockTracer);
  }

  @Test
  public void submitListenableRunnable() {
    final ArgumentCaptor<TracedRunnable> argumentCaptor = ArgumentCaptor.forClass(TracedRunnable.class);
    scheduler.submitListenable(mockRunnable);
    verify(delegate).submitListenable(argumentCaptor.capture());
    verifyTracedRunnable(argumentCaptor.getValue(), mockRunnable, mockTracer);
  }

  @Test
  public void submitListenableCallable() {
    final ArgumentCaptor<TracedCallable> argumentCaptor = ArgumentCaptor.forClass(TracedCallable.class);
    scheduler.submitListenable(mockCallable);
    verify(delegate).submitListenable(argumentCaptor.capture());
    verifyTracedCallable(argumentCaptor.getValue(), mockCallable, mockTracer);
  }

  @Test
  public void scheduleWithTrigger() {
    final ArgumentCaptor<TracedRunnable> argumentCaptor = ArgumentCaptor.forClass(TracedRunnable.class);
    final Trigger trigger = mock(Trigger.class);
    scheduler.schedule(mockRunnable, trigger);
    verify(delegate).schedule(argumentCaptor.capture(), eq(trigger));
    verifyTracedRunnable(argumentCaptor.getValue(), mockRunnable, mockTracer);
  }

  @Test
  public void scheduleWithDate() {
    final ArgumentCaptor<TracedRunnable> argumentCaptor = ArgumentCaptor.forClass(TracedRunnable.class);
    final Date date = mock(Date.class);
    scheduler.schedule(mockRunnable, date);
    verify(delegate).schedule(argumentCaptor.capture(), eq(date));
    verifyTracedRunnable(argumentCaptor.getValue(), mockRunnable, mockTracer);
  }

  @Test
  public void scheduleWithInstant() {
    final ArgumentCaptor<TracedRunnable> argumentCaptor = ArgumentCaptor.forClass(TracedRunnable.class);
    final Instant instant = Instant.now();
    scheduler.schedule(mockRunnable, instant);
    verify(delegate).schedule(argumentCaptor.capture(), eq(instant));
    verifyTracedRunnable(argumentCaptor.getValue(), mockRunnable, mockTracer);
  }

  @Test
  public void scheduleAtFixedRateWithDateAndLong() {
    final ArgumentCaptor<TracedRunnable> argumentCaptor = ArgumentCaptor.forClass(TracedRunnable.class);
    final Date date = new Date();
    scheduler.scheduleAtFixedRate(mockRunnable, date, 1000L);
    verify(delegate).scheduleAtFixedRate(argumentCaptor.capture(), eq(date), eq(1000L));
    verifyTracedRunnable(argumentCaptor.getValue(), mockRunnable, mockTracer);
  }

  @Test
  public void scheduleAtFixedRateWithInstantAndDuration() {
    final ArgumentCaptor<TracedRunnable> argumentCaptor = ArgumentCaptor.forClass(TracedRunnable.class);
    final Instant instant = Instant.now();
    final Duration duration = Duration.ofMinutes(1);
    scheduler.scheduleAtFixedRate(mockRunnable, instant, duration);
    verify(delegate).scheduleAtFixedRate(argumentCaptor.capture(), eq(instant), eq(duration));
    verifyTracedRunnable(argumentCaptor.getValue(), mockRunnable, mockTracer);
  }

  @Test
  public void scheduleAtFixedRateWithLong() {
    final ArgumentCaptor<TracedRunnable> argumentCaptor = ArgumentCaptor.forClass(TracedRunnable.class);
    scheduler.scheduleAtFixedRate(mockRunnable, 1000L);
    verify(delegate).scheduleAtFixedRate(argumentCaptor.capture(), eq(1000L));
    verifyTracedRunnable(argumentCaptor.getValue(), mockRunnable, mockTracer);
  }

  @Test
  public void scheduleAtFixedRateWithDuration() {
    final ArgumentCaptor<TracedRunnable> argumentCaptor = ArgumentCaptor.forClass(TracedRunnable.class);
    final Duration duration = Duration.ofMinutes(5);
    scheduler.scheduleAtFixedRate(mockRunnable, duration);
    verify(delegate).scheduleAtFixedRate(argumentCaptor.capture(), eq(duration));
    verifyTracedRunnable(argumentCaptor.getValue(), mockRunnable, mockTracer);
  }

  @Test
  public void scheduleWithFixedDelayWithDateAndLong() {
    final ArgumentCaptor<TracedRunnable> argumentCaptor = ArgumentCaptor.forClass(TracedRunnable.class);
    final Date date = new Date();
    scheduler.scheduleWithFixedDelay(mockRunnable, date, 1000L);
    verify(delegate).scheduleWithFixedDelay(argumentCaptor.capture(), eq(date), eq(1000L));
    verifyTracedRunnable(argumentCaptor.getValue(), mockRunnable, mockTracer);
  }

  @Test
  public void scheduleWithFixedDelayWithLong() {
    final ArgumentCaptor<TracedRunnable> argumentCaptor = ArgumentCaptor.forClass(TracedRunnable.class);
    scheduler.scheduleWithFixedDelay(mockRunnable, 1000L);
    verify(delegate).scheduleWithFixedDelay(argumentCaptor.capture(), eq(1000L));
    verifyTracedRunnable(argumentCaptor.getValue(), mockRunnable, mockTracer);
  }

  @Test
  public void scheduleWithFixedDelayWithInstantAndDuration() {
    final ArgumentCaptor<TracedRunnable> argumentCaptor = ArgumentCaptor.forClass(TracedRunnable.class);
    final Instant instant = Instant.now();
    final Duration duration = Duration.ofMinutes(1);
    scheduler.scheduleWithFixedDelay(mockRunnable, instant, duration);
    verify(delegate).scheduleWithFixedDelay(argumentCaptor.capture(), eq(instant), eq(duration));
    verifyTracedRunnable(argumentCaptor.getValue(), mockRunnable, mockTracer);
  }

  @Test
  public void scheduleWithFixedDelayWithDuration() {
    final ArgumentCaptor<TracedRunnable> argumentCaptor = ArgumentCaptor.forClass(TracedRunnable.class);
    final Duration duration = Duration.ofMinutes(5);
    scheduler.scheduleWithFixedDelay(mockRunnable, duration);
    verify(delegate).scheduleWithFixedDelay(argumentCaptor.capture(), eq(duration));
    verifyTracedRunnable(argumentCaptor.getValue(), mockRunnable, mockTracer);
  }

  private void verifyTracedRunnable(final TracedRunnable tracedRunnable, final Runnable task, final Tracer tracer) {
    final Runnable actualTask = (Runnable) ReflectionUtils.getField(TRACED_RUNNABLE_DELEGATE_FIELD, tracedRunnable);
    final Tracer actualTracer = (Tracer) ReflectionUtils.getField(TRACED_RUNNABLE_TRACER_FIELD, tracedRunnable);
    assertThat(actualTask).isEqualTo(task);
    assertThat(actualTracer).isEqualTo(tracer);
  }

  private void verifyTracedCallable(final TracedCallable tracedCallable, final Callable task, final Tracer tracer) {
    final Callable actualTask = (Callable) ReflectionUtils.getField(TRACED_CALLABLE_DELEGATE_FIELD, tracedCallable);
    final Tracer actualTracer = (Tracer) ReflectionUtils.getField(TRACED_CALLABLE_TRACER_FIELD, tracedCallable);
    assertThat(actualTask).isEqualTo(task);
    assertThat(actualTracer).isEqualTo(tracer);
  }

  @Test
  public void setThreadFactory() {
    final ThreadFactory threadFactory = mock(ThreadFactory.class);
    scheduler.setThreadFactory(threadFactory);
    verify(delegate).setThreadFactory(threadFactory);
  }

  @Test
  public void setThreadNamePrefix() {
    final String threadNamePrefix = "c137";
    scheduler.setThreadNamePrefix(threadNamePrefix);
    verify(delegate).setThreadNamePrefix(threadNamePrefix);
  }

  @Test
  public void setRejectedExecutionHandler() {
    final RejectedExecutionHandler rejectedExecutionHandler = mock(RejectedExecutionHandler.class);
    scheduler.setRejectedExecutionHandler(rejectedExecutionHandler);
    verify(delegate).setRejectedExecutionHandler(rejectedExecutionHandler);
  }

  @Test
  public void setWaitForTasksToCompleteOnShutdown() {
    scheduler.setWaitForTasksToCompleteOnShutdown(true);
    verify(delegate).setWaitForTasksToCompleteOnShutdown(true);
  }

  @Test
  public void setAwaitTerminationSeconds() {
    scheduler.setAwaitTerminationSeconds(5);
    verify(delegate).setAwaitTerminationSeconds(5);
  }

  @Test
  public void setBeanName() {
    final String name = "gazorp";
    scheduler.setBeanName(name);
    verify(delegate).setBeanName(name);
  }

  @Test
  public void afterPropertiesSet() {
    scheduler.afterPropertiesSet();
    verify(delegate).afterPropertiesSet();
  }

  @Test
  public void initialize() {
    scheduler.initialize();
    verify(delegate).initialize();
  }

  @Test
  public void destroy() {
    scheduler.destroy();
    verify(delegate).destroy();
  }

  @Test
  public void shutdown() {
    scheduler.shutdown();
    verify(delegate).shutdown();
  }

  @Test
  public void newThread() {
    final Runnable runnable = mock(Runnable.class);
    scheduler.newThread(runnable);
    verify(delegate).newThread(runnable);
  }

  @Test
  public void getThreadNamePrefix() {
    scheduler.getThreadNamePrefix();
    verify(delegate).getThreadNamePrefix();
  }

  @Test
  public void setThreadPriority() {
    final int threadPriority = 5150;
    scheduler.setThreadPriority(threadPriority);
    verify(delegate).setThreadPriority(threadPriority);
  }

  @Test
  public void getThreadPriority() {
    scheduler.getThreadPriority();
    verify(delegate).getThreadPriority();
  }

  @Test
  public void setDaemon() {
    scheduler.setDaemon(true);
    verify(delegate).setDaemon(true);
  }

  @Test
  public void isDaemon() {
    scheduler.isDaemon();
    verify(delegate).isDaemon();
  }

  @Test
  public void setThreadGroupName() {
    final String name = "crombopulous";
    scheduler.setThreadGroupName(name);
    verify(delegate).setThreadGroupName(name);
  }

  @Test
  public void setThreadGroup() {
    final ThreadGroup threadGroup = mock(ThreadGroup.class);
    scheduler.setThreadGroup(threadGroup);
    verify(delegate).setThreadGroup(threadGroup);
  }

  @Test
  public void getThreadGroup() {
    scheduler.getThreadGroup();
    verify(delegate).getThreadGroup();
  }

  @Test
  public void createThread() {
    final Runnable runnable = mock(Runnable.class);
    scheduler.createThread(runnable);
    verify(delegate).createThread(runnable);
  }

  @Test
  public void prefersShortLivedTasks() {
    scheduler.prefersShortLivedTasks();
    verify(delegate).prefersShortLivedTasks();
  }
}
