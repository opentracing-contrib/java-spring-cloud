/**
 * Copyright 2017-2020 The OpenTracing Authors
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

import io.opentracing.Tracer;
import io.opentracing.contrib.concurrent.TracedCallable;
import io.opentracing.contrib.concurrent.TracedRunnable;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

import org.springframework.lang.Nullable;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ErrorHandler;
import org.springframework.util.concurrent.ListenableFuture;


/**
 * A traced version of {@code TracedThreadPoolTaskScheduler}.
 *
 * @author cbono
 */
public class TracedThreadPoolTaskScheduler
    extends ThreadPoolTaskScheduler {

  private final Tracer tracer;
  private final ThreadPoolTaskScheduler delegate;

  public TracedThreadPoolTaskScheduler(Tracer tracer, ThreadPoolTaskScheduler delegate) {
    this.tracer = tracer;
    this.delegate = delegate;
  }

  @Override
  public void setPoolSize(int poolSize) {
    delegate.setPoolSize(poolSize);
  }

  @Override
  public void setRemoveOnCancelPolicy(boolean removeOnCancelPolicy) {
    delegate.setRemoveOnCancelPolicy(removeOnCancelPolicy);
  }

  @Override
  public void setErrorHandler(ErrorHandler errorHandler) {
    delegate.setErrorHandler(errorHandler);
  }

  @Override
  public ScheduledExecutorService getScheduledExecutor()
      throws IllegalStateException {
    return delegate.getScheduledExecutor();
  }

  @Override
  public ScheduledThreadPoolExecutor getScheduledThreadPoolExecutor()
      throws IllegalStateException {
    return delegate.getScheduledThreadPoolExecutor();
  }

  @Override
  public int getPoolSize() {
    return delegate.getPoolSize();
  }

  @Override
  public boolean isRemoveOnCancelPolicy() {
    return delegate.isRemoveOnCancelPolicy();
  }

  @Override
  public int getActiveCount() {
    return delegate.getActiveCount();
  }

  @Override
  public void execute(Runnable task) {
    delegate.execute(new TracedRunnable(task, tracer));
  }

  @Override
  public void execute(Runnable task, long startTimeout) {
    delegate.execute(new TracedRunnable(task, tracer), startTimeout);
  }

  @Override
  public Future<?> submit(Runnable task) {
    return delegate.submit(new TracedRunnable(task, tracer));
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    return delegate.submit(new TracedCallable<>(task, tracer));
  }

  @Override
  public ListenableFuture<?> submitListenable(Runnable task) {
    return new TracedListenableFuture<>(delegate.submitListenable(new TracedRunnable(task, tracer)), tracer);
  }

  @Override
  public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
    return new TracedListenableFuture<>(delegate.submitListenable(new TracedCallable<>(task, tracer)), tracer);
  }

  @Override
  @Nullable
  public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
    return delegate.schedule(new TracedRunnable(task, tracer), trigger);
  }

  @Override
  public ScheduledFuture<?> schedule(Runnable task, Date startTime) {
    return delegate.schedule(new TracedRunnable(task, tracer), startTime);
  }

  @Override
  public ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
    return delegate.schedule(new TracedRunnable(task, tracer), startTime);
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Date startTime, long period) {
    return delegate.scheduleAtFixedRate(new TracedRunnable(task, tracer), startTime, period);
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, Duration period) {
    return delegate.scheduleAtFixedRate(new TracedRunnable(task, tracer), startTime, period);
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long period) {
    return delegate.scheduleAtFixedRate(new TracedRunnable(task, tracer), period);
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period) {
    return delegate.scheduleAtFixedRate(new TracedRunnable(task, tracer), period);
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Date startTime, long delay) {
    return delegate.scheduleWithFixedDelay(new TracedRunnable(task, tracer), startTime, delay);
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long delay) {
    return delegate.scheduleWithFixedDelay(new TracedRunnable(task, tracer), delay);
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Instant startTime,
      Duration delay) {
    return delegate.scheduleWithFixedDelay(new TracedRunnable(task, tracer), startTime, delay);
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay) {
    return delegate.scheduleWithFixedDelay(new TracedRunnable(task, tracer), delay);
  }

  @Override
  public void setThreadFactory(ThreadFactory threadFactory) {
    delegate.setThreadFactory(threadFactory);
  }

  @Override
  public void setThreadNamePrefix(String threadNamePrefix) {
    delegate.setThreadNamePrefix(threadNamePrefix);
  }

  @Override
  public void setRejectedExecutionHandler(RejectedExecutionHandler rejectedExecutionHandler) {
    delegate.setRejectedExecutionHandler(rejectedExecutionHandler);
  }

  @Override
  public void setWaitForTasksToCompleteOnShutdown(boolean waitForJobsToCompleteOnShutdown) {
    delegate.setWaitForTasksToCompleteOnShutdown(waitForJobsToCompleteOnShutdown);
  }

  @Override
  public void setAwaitTerminationSeconds(int awaitTerminationSeconds) {
    delegate.setAwaitTerminationSeconds(awaitTerminationSeconds);
  }

  @Override
  public void setBeanName(String name) {
    delegate.setBeanName(name);
  }

  @Override
  public void afterPropertiesSet() {
    delegate.afterPropertiesSet();
  }

  @Override
  public void initialize() {
    delegate.initialize();
  }

  @Override
  public void destroy() {
    delegate.destroy();
  }

  @Override
  public void shutdown() {
    delegate.shutdown();
  }

  @Override
  public Thread newThread(Runnable runnable) {
    return delegate.newThread(runnable);
  }

  @Override
  public String getThreadNamePrefix() {
    return delegate.getThreadNamePrefix();
  }

  @Override
  public void setThreadPriority(int threadPriority) {
    delegate.setThreadPriority(threadPriority);
  }

  @Override
  public int getThreadPriority() {
    return delegate.getThreadPriority();
  }

  @Override
  public void setDaemon(boolean daemon) {
    delegate.setDaemon(daemon);
  }

  @Override
  public boolean isDaemon() {
    return delegate.isDaemon();
  }

  @Override
  public void setThreadGroupName(String name) {
    delegate.setThreadGroupName(name);
  }

  @Override
  public void setThreadGroup(ThreadGroup threadGroup) {
    delegate.setThreadGroup(threadGroup);
  }

  @Override
  @Nullable
  public ThreadGroup getThreadGroup() {
    return delegate.getThreadGroup();
  }

  @Override
  public Thread createThread(Runnable runnable) {
    return delegate.createThread(runnable);
  }

  @Override
  public boolean prefersShortLivedTasks() {
    return delegate.prefersShortLivedTasks();
  }
}
