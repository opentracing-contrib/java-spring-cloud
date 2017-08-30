package io.opentracing.contrib.spring.cloud;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Pavol Loffay
 */
public class NoDepsTest {

    @Test
    public void testNoFeign() {
        try {
            this.getClass().getClassLoader().loadClass("feign.Client");
            this.getClass().getClassLoader().loadClass("feign.hystrix.HystrixFeign");
            Assert.fail();
        } catch (ClassNotFoundException e) {
        }
    }

    @Test
    public void testNoJMS() {
        try {
            this.getClass().getClassLoader().loadClass("javax.jms.Message");
            this.getClass().getClassLoader().loadClass("org.springframework.jms.core.JmsTemplate");
            Assert.fail();
        } catch (ClassNotFoundException e) {
        }
    }
}
