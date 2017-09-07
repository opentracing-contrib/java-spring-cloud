package io.opentracing.contrib.spring.cloud.async;

import io.opentracing.mock.MockTracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.WebAsyncTask;

import java.util.concurrent.Callable;

@RestController
public class HelloWorldController {

    @Autowired
    MockTracer mockTracer;

    @RequestMapping("/hello")
    public WebAsyncTask<String> hello() {
        return new WebAsyncTask<>(() -> "Hello!");
    }

    @RequestMapping("/hola")
    public Callable<String> hola() {
        return () -> "hola!";
    }
}
