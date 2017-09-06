package io.opentracing.contrib.spring.cloud.async;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.scheduling.annotation.AsyncConfigurer;

import java.util.concurrent.Executor;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author kameshs
 */
@RunWith(MockitoJUnitRunner.class)
public class LazyTraceExecutorTest {

    @Mock
    BeanFactory beanFactory;

    @Mock
    AsyncConfigurer asyncConfigurer;

    @InjectMocks
    LazyTaceAsyncCustomizer lazyTaceAsyncCustomizer;

    @Test
    public void should_wrap_async_executor_in_trace_version() throws Exception {
        Executor executor = this.lazyTaceAsyncCustomizer.getAsyncExecutor();

        then(executor).isExactlyInstanceOf(LazyTraceExecutor.class);
    }
}
