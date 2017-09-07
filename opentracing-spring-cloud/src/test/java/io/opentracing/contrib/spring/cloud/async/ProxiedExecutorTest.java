package io.opentracing.contrib.spring.cloud.async;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.aop.framework.AopConfigException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ClassUtils;

import java.util.concurrent.Executor;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

@RunWith(MockitoJUnitRunner.class)
public class ProxiedExecutorTest {

    @Mock
    BeanFactory beanFactory;


    @Test
    public void this_should_be_cglib_proxy_executor() throws Exception {
        Object o = new ProxiedExecutor(this.beanFactory)
                .postProcessAfterInitialization(new HelloWorld(), "helloWorld");

        then(o).isInstanceOf(HelloWorld.class);
        then(ClassUtils.isCglibProxy(o)).isTrue();
    }


    @Test
    public void this_should_be_cglib_proxy_thread_pool_task_executor() throws Exception {
        Object o = new ProxiedExecutor(this.beanFactory)
                .postProcessAfterInitialization(new HelloWorldTasks(), "helloWorldTasks");

        then(o).isInstanceOf(HelloWorldTasks.class);
        then(ClassUtils.isCglibProxy(o)).isTrue();
    }

    @Test
    public void this_should_be_java_proxy() throws Exception {
        Object o = new ProxiedExecutor(this.beanFactory)
                .postProcessAfterInitialization(new FinalHelloWorld(), "finalHelloWorld");

        then(o).isNotInstanceOf(FinalHelloWorld.class);
        then(ClassUtils.isCglibProxy(o)).isFalse();
    }

    @Test
    public void throw_aop_exeception_when_unable_to_create_executor_proxy() throws Exception {
        ProxiedExecutor proxiedExecutor = new ProxiedExecutor(this.beanFactory) {
            @Override
            Object createProxy(Object bean, boolean cglibProxy, Executor executor) {
                throw new AopConfigException("forTest");
            }
        };

        thenThrownBy(() -> proxiedExecutor.postProcessAfterInitialization(new HelloWorld(), "hw"))
                .isInstanceOf(AopConfigException.class)
                .hasMessage("forTest");
    }

    @Test
    public void throw_aop_exeception_when_unable_to_create_thread_pool_task_executor_proxy() throws Exception {
        ProxiedExecutor proxiedExecutor = new ProxiedExecutor(this.beanFactory) {
            @Override
            Object createThreadPoolTaskExecutorProxy(Object bean, boolean cglibProxy, ThreadPoolTaskExecutor executor) {
                throw new AopConfigException("forTpEx");
            }
        };

        thenThrownBy(() -> proxiedExecutor.postProcessAfterInitialization(new HelloWorldTasks(), "hw"))
                .isInstanceOf(AopConfigException.class)
                .hasMessage("forTpEx");
    }


    class HelloWorld implements Executor {
        @Override
        public void execute(Runnable command) {

        }
    }

    final class FinalHelloWorld implements Executor {
        @Override
        public void execute(Runnable command) {

        }
    }

    class HelloWorldTasks extends ThreadPoolTaskExecutor {

    }
}
