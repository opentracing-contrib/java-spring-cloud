package io.opentracing.contrib.spring.cloud.async;

import io.opentracing.Span;
import io.opentracing.mock.MockTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Future;

/**
 * @author kameshs
 */
@Component
public class HttpBinService {

    private static final Logger LOG = LoggerFactory.getLogger(HttpBinService.class);

    @Autowired
    MockTracer tracer;

    @Autowired
    RestTemplate restTemplate;

    @Async
    public Future<String> delayer(int seconds) {
        String response;
        try {
            response = restTemplate.getForObject("http://httpbin.org/delay/" + seconds, String.class);
            LOG.trace("Got Response:{}", response);
        } catch (Exception e) {
            response = "Fallback(not working)";
        }
        return new AsyncResult<>(response);
    }
}
