/*
 * Copyright 2013-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentracing.contrib.spring.cloud.hystrix;

import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestVariable;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestVariableLifecycle;
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifier;
import com.netflix.hystrix.strategy.executionhook.HystrixCommandExecutionHook;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisher;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategy;
import com.netflix.hystrix.strategy.properties.HystrixProperty;
import io.opentracing.Tracer;
import io.opentracing.contrib.concurrent.TracedCallable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A {@link HystrixConcurrencyStrategy} that wraps a {@link Callable} in a
 * {@link Callable} that either starts a new span or continues one if the tracing was
 * already running before the command was executed.
 *
 * @author Marcin Grzejszczak
 * NOTE: Modified original by @author kameshsampath to suit OpenTracing
 */
public class SleuthHystrixConcurrencyStrategy extends HystrixConcurrencyStrategy {

	private static final String HYSTRIX_COMPONENT = "hystrix";
	private static final Log log = LogFactory
			.getLog(SleuthHystrixConcurrencyStrategy.class);

	private final Tracer tracer;
	private HystrixConcurrencyStrategy delegate;

	public SleuthHystrixConcurrencyStrategy(Tracer tracer) {
		this.tracer = tracer;
		try {
			this.delegate = HystrixPlugins.getInstance().getConcurrencyStrategy();
			if (this.delegate instanceof SleuthHystrixConcurrencyStrategy) {
				// Welcome to singleton hell...
				return;
			}
			HystrixCommandExecutionHook commandExecutionHook = HystrixPlugins
					.getInstance().getCommandExecutionHook();
			HystrixEventNotifier eventNotifier = HystrixPlugins.getInstance()
					.getEventNotifier();
			HystrixMetricsPublisher metricsPublisher = HystrixPlugins.getInstance()
					.getMetricsPublisher();
			HystrixPropertiesStrategy propertiesStrategy = HystrixPlugins.getInstance()
					.getPropertiesStrategy();
			logCurrentStateOfHysrixPlugins(eventNotifier, metricsPublisher,
					propertiesStrategy);
			HystrixPlugins.reset();
			HystrixPlugins.getInstance().registerConcurrencyStrategy(this);
			HystrixPlugins.getInstance()
					.registerCommandExecutionHook(commandExecutionHook);
			HystrixPlugins.getInstance().registerEventNotifier(eventNotifier);
			HystrixPlugins.getInstance().registerMetricsPublisher(metricsPublisher);
			HystrixPlugins.getInstance().registerPropertiesStrategy(propertiesStrategy);
		}
		catch (Exception e) {
			log.error("Failed to register Sleuth Hystrix Concurrency Strategy", e);
		}
	}

	private void logCurrentStateOfHysrixPlugins(HystrixEventNotifier eventNotifier,
			HystrixMetricsPublisher metricsPublisher,
			HystrixPropertiesStrategy propertiesStrategy) {
		if (log.isDebugEnabled()) {
			log.debug("Current Hystrix plugins configuration is [" + "concurrencyStrategy ["
					+ this.delegate + "]," + "eventNotifier [" + eventNotifier + "],"
					+ "metricPublisher [" + metricsPublisher + "]," + "propertiesStrategy ["
					+ propertiesStrategy + "]," + "]");
			log.debug("Registering Sleuth Hystrix Concurrency Strategy.");
		}
	}

	@Override
	public <T> Callable<T> wrapCallable(Callable<T> callable) {
		//TODO: Cleanup Since TracedCallable does everything needed its not needed to wrap it in another callable
//		if (callable instanceof HystrixTraceCallable) {
//			return callable;
//		}
//		Callable<T> wrappedCallable = this.delegate != null
//				? this.delegate.wrapCallable(callable) : callable;
//		if (wrappedCallable instanceof HystrixTraceCallable) {
//			return wrappedCallable;
//		}

		return new TracedCallable<>(callable,tracer.activeSpan());
	}

	@Override
	public ThreadPoolExecutor getThreadPool(HystrixThreadPoolKey threadPoolKey,
			HystrixProperty<Integer> corePoolSize,
			HystrixProperty<Integer> maximumPoolSize,
			HystrixProperty<Integer> keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue) {
		return this.delegate.getThreadPool(threadPoolKey, corePoolSize, maximumPoolSize,
				keepAliveTime, unit, workQueue);
	}


	@Override
	public BlockingQueue<Runnable> getBlockingQueue(int maxQueueSize) {
		return this.delegate.getBlockingQueue(maxQueueSize);
	}

	@Override
	public <T> HystrixRequestVariable<T> getRequestVariable(
			HystrixRequestVariableLifecycle<T> rv) {
		return this.delegate.getRequestVariable(rv);
	}

	/* //TODO: Cleanup revisit - guess this is not needed will remove it off once @Pavol feels its good to use TraceableCallable
	static class HystrixTraceCallable<S> implements Callable<S> {

		private static final Log log = LogFactory.getLog(MethodHandles.lookup().lookupClass());

		private final Tracer tracer;
		private final Callable<S> callable;

		public HystrixTraceCallable(Tracer tracer,Callable<S> callable) {
			this.tracer = tracer;
			this.callable = callable;
		}

		@Override
		public S call() throws Exception {
			Span span = null;
			boolean created = false;
			if (span != null) {
				span = this.tracer.continueSpan(span);
				if (log.isDebugEnabled()) {
					log.debug("Continuing span " + span);
				}
			}
			else {
				span = this.tracer.createSpan(HYSTRIX_COMPONENT);
				created = true;
				if (log.isDebugEnabled()) {
					log.debug("Creating new span " + span);
				}
			}
			if (!span.tags().containsKey(Span.SPAN_LOCAL_COMPONENT_TAG_NAME)) {
				this.tracer.addTag(Span.SPAN_LOCAL_COMPONENT_TAG_NAME, HYSTRIX_COMPONENT);
			}
			String asyncKey = this.traceKeys.getAsync().getPrefix()
					+ this.traceKeys.getAsync().getThreadNameKey();
			if (!span.tags().containsKey(asyncKey)) {
				this.tracer.addTag(asyncKey, Thread.currentThread().getName());
			}
			try {
				return this.callable.call();
			}
			finally {
				if (created) {
					if (log.isDebugEnabled()) {
						log.debug("Closing span since it was created" + span);
					}
					this.tracer.close(span);
				}
				else if(this.tracer.isTracing()) {
					if (log.isDebugEnabled()) {
						log.debug("Detaching span since it was continued " + span);
					}
					this.tracer.detach(span);
				}
			}
		}

	}
	*/
}
