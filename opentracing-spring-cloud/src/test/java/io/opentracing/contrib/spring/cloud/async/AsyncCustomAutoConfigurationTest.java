package io.opentracing.contrib.spring.cloud.async;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.springframework.scheduling.annotation.AsyncConfigurer;

import io.opentracing.contrib.spring.cloud.async.instrument.TracedAsyncConfigurer;

/**
 * @author kameshs
 */
public class AsyncCustomAutoConfigurationTest {

    @Test
    public void should_return_bean_when_its_not_async_configurer() {
        AsyncCustomAutoConfiguration configuration = new AsyncCustomAutoConfiguration();
        Object bean = configuration.postProcessAfterInitialization(new Object(), "someBean");
        then(bean).isNotInstanceOf(TracedAsyncConfigurer.class);
    }

    @Test
    public void should_return_async_configurer_when_bean_instance_of_it() {
        AsyncCustomAutoConfiguration configuration = new AsyncCustomAutoConfiguration();
        Object bean = configuration.postProcessAfterInitialization(mock(AsyncConfigurer.class), "myAsync");
        then(bean).isInstanceOf(TracedAsyncConfigurer.class);
    }
}
