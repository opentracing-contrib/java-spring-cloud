package io.opentracing.contrib.spring.cloud.async;

import com.jayway.jsonpath.JsonPath;
import io.opentracing.Span;
import io.opentracing.mock.MockTracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.async.WebAsyncTask;

@RestController
public class HelloWorldController {

    @Autowired
    HttpBinService httpBinService;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    MockTracer mockTracer;

    @RequestMapping("/myip")
    public WebAsyncTask<String> myIp() {

        Span span = mockTracer.buildSpan("myIp")
                .startManual();
        try {
            return new WebAsyncTask<>(() -> {

                String response = httpBinService.delayer(3).get();

                System.out.println("Delayer Response :" + response);

                response = restTemplate.getForObject("http://httpbin.org/ip", String.class);

                String ip = JsonPath.parse(response)
                        .read("$.origin");

                System.out.println(">>>>> IP <<<<<<<<" + ip);

                return String.format("You called me from %s", ip);
            });
        } finally {
            span.finish();
        }

    }
}
