package io.opentracing.contrib.spring.cloud.async;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

public interface HttpBinServiceClient {


    @RequestLine("GET /delay/{seconds}")
    @Headers({"accept: application/json"})
    String delay(@Param("seconds") int seconds);
}
