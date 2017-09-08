package io.opentracing.contrib.spring.cloud.async.instrument;

import java.util.concurrent.Executor;

import io.opentracing.Tracer;
import io.opentracing.contrib.concurrent.TracedRunnable;

/**
 * {@link Executor} that wraps {@link Runnable} in a
 * {@link TracedRunnable} that propagates parent span inside the runnable.
 *
 * @author kameshsampath
 */
public class TracedExecutor implements Executor {

    private final Executor delegate;
    private final Tracer tracer;

    public TracedExecutor(Tracer tracer, Executor delegate) {
        this.tracer = tracer;
        this.delegate = delegate;
    }

    @Override
    public void execute(Runnable command) {
        delegate.execute(new TracedRunnable(command, tracer.activeSpan()));
    }
}
