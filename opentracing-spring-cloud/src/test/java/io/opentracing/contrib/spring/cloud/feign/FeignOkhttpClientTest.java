package io.opentracing.contrib.spring.cloud.feign;

import org.springframework.test.context.TestPropertySource;

/**
 * @author Pavol Loffay
 */
@TestPropertySource(properties = {"feign.okhttp.enabled=true"})
public class FeignOkhttpClientTest extends FeignTest {

}
