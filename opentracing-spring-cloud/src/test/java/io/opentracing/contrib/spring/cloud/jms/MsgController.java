package io.opentracing.contrib.spring.cloud.jms;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Pavol Loffay
 */
@RestController
public class MsgController {
    @Autowired
    JmsTemplate jmsTemplate;

    @RequestMapping("/hello")
    public String hello() {
        String message = "Hello!";
        jmsTemplate.convertAndSend("fooQueue", message);
        return message;
    }
}
