package io.opentracing.contrib.spring.cloud;

import org.junit.Test;

/**
 * @author Pavol Loffay
 */
public class NoDepsTest {

    @Test(expected = ClassNotFoundException.class)
    public void testNoFeign() throws ClassNotFoundException {
        this.getClass().getClassLoader().loadClass("feign.Client");
    }

    @Test(expected = ClassNotFoundException.class)
    public void testNoHystrixFeign() throws ClassNotFoundException {
        this.getClass().getClassLoader().loadClass("feign.hystrix.HystrixFeign");
    }

    @Test(expected = ClassNotFoundException.class)
    public void testNoJMS() throws ClassNotFoundException {
        this.getClass().getClassLoader().loadClass("javax.jms.Message");
    }

    @Test(expected = ClassNotFoundException.class)
    public void testNoJMSTemplate() throws ClassNotFoundException {
        this.getClass().getClassLoader().loadClass("org.springframework.jms.core.JmsTemplate");
    }
}
