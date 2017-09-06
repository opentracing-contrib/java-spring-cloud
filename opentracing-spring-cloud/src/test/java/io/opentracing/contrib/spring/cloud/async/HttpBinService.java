package io.opentracing.contrib.spring.cloud.async;

import io.opentracing.ActiveSpan;
import io.opentracing.mock.MockTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

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
    HttpBinServiceClient httpBinServiceClient;

    @Async
    public Future<String> delayer(int seconds) {
        ActiveSpan activeSpan = tracer
                .buildSpan("delayer")
                .withTag("myTag", "hello")
                .startActive();
        String response;
        try {
            response = httpBinServiceClient.delay(seconds);
            LOG.info("Got Response:{}", response);
        } catch (Exception e) {
            response = "Fallback(not working)";
        } finally {
            activeSpan.close();
        }
        return new AsyncResult<>(response);
    }
}
