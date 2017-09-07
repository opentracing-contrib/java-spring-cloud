package io.opentracing.contrib.spring.cloud.async;

import io.opentracing.mock.MockTracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.Future;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author kameshsampath
 */
@Component
public class DelayAsyncService {

    @Autowired
    MockTracer tracer;

    @Async
    public Future<String> delayer(int seconds) {
        String response;
        try {
            SECONDS.sleep(seconds);
            response = String.format("After %d seconds", seconds);
        } catch (Exception e) {
            response = "Fallback(not working)";
        }
        return new AsyncResult<>(response);
    }
}
