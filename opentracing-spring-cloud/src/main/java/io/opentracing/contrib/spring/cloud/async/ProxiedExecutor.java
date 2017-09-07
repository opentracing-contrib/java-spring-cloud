package io.opentracing.contrib.spring.cloud.async;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopConfigException;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.Executor;

//TODO remove all unwanted logs or move the level up post testing and docs

/**
 * @author kameshsampath
 */
public class ProxiedExecutor implements BeanPostProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxiedExecutor.class);

    private final BeanFactory beanFactory;

    public ProxiedExecutor(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

        if (bean instanceof Executor && !(bean instanceof ThreadPoolTaskExecutor)) {
            LOGGER.info("Bean {} is an instance of Executor and not a ThreadPoolTaskExecutor ", bean);
            Method execute = ReflectionUtils.findMethod(bean.getClass(), "execute", Runnable.class);
            boolean methodFinal = Modifier.isFinal(execute.getModifiers());
            boolean clazzFinal = Modifier.isFinal(bean.getClass().getModifiers());
            boolean cglibProxy = !methodFinal && !clazzFinal;
            Executor executor = (Executor) bean;

            try {
                return createProxy(bean, cglibProxy, executor);
            } catch (AopConfigException e) {
                if (cglibProxy) {
                    LOGGER.debug("Error creating cglib proxy {} , fallback to Java Proxy",e.getMessage());
                    return createProxy(bean, false, executor);
                }
                LOGGER.error("Unable to create proxy {}",e.getMessage(),e);
                throw e;
            }
        } else if (bean instanceof ThreadPoolTaskExecutor) {
            boolean clazzFinal = Modifier.isFinal(bean.getClass().getModifiers());
            boolean cglibProxy = !clazzFinal;
            ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) bean;
            return createThreadPoolTaskExecutorProxy(bean, cglibProxy, executor);
        }

        return bean;
    }

    /**
     *
     * @param bean
     * @param cglibProxy
     * @param executor
     * @return
     */
     Object createThreadPoolTaskExecutorProxy(Object bean, boolean cglibProxy,
                                                     ThreadPoolTaskExecutor executor) {
        ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();
        proxyFactoryBean.setProxyTargetClass(cglibProxy);
        proxyFactoryBean.addAdvice(new ExecutorMethodInterceptor<ThreadPoolTaskExecutor>(executor, this.beanFactory) {
            @Override
            Executor executor(BeanFactory beanFactory, ThreadPoolTaskExecutor executor) {
                return new TracedThreadPoolTaskExecutor(beanFactory, executor);
            }
        });
        proxyFactoryBean.setTarget(bean);
        return proxyFactoryBean.getObject();
    }

    /**
     *
     * @param bean
     * @param cglibProxy
     * @param executor
     * @return
     */
     Object createProxy(Object bean, boolean cglibProxy, Executor executor) {
        ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();
        proxyFactoryBean.setProxyTargetClass(cglibProxy);
        proxyFactoryBean.addAdvice(new ExecutorMethodInterceptor<>(executor, this.beanFactory));
        proxyFactoryBean.setTarget(bean);
        return proxyFactoryBean.getObject();
    }

    /**
     *
     * @param <T>
     */
    class ExecutorMethodInterceptor<T extends Executor> implements MethodInterceptor {

        private final T delegate;
        private final BeanFactory beanFactory;

        public ExecutorMethodInterceptor(T delegate, BeanFactory beanFactory) {
            this.delegate = delegate;
            this.beanFactory = beanFactory;
        }

        @Override
        public Object invoke(MethodInvocation methodInvocation) throws Throwable {
            Executor executor = executor(this.beanFactory, this.delegate);
            Method beanMethod = reflectMethod(methodInvocation, executor);
            if (beanMethod != null) {
                return beanMethod.invoke(executor, methodInvocation.getArguments());
            }
            return methodInvocation.proceed();
        }

        private Method reflectMethod(MethodInvocation methodInvocation, Executor executor) {
            Method method = methodInvocation.getMethod();
            return ReflectionUtils.findMethod(executor.getClass(), method.getName(), method.getParameterTypes());
        }

        Executor executor(BeanFactory beanFactory, T executor) {
            return new TraceableExecutor(beanFactory, executor);
        }
    }
}
