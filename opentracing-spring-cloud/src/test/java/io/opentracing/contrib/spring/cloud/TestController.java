package io.opentracing.contrib.spring.cloud;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
@RestController
public class TestController {
  @RequestMapping("/hello")
  public String hello() {
    return "Hello";
  }

  @RequestMapping("/notTraced")
  public String notTraced() {
    return "Not traced";
  }
}
