package io.opentracing.contrib.spring.cloud.gateway.propagation;

import io.opentracing.propagation.TextMap;
import io.opentracing.propagation.TextMapExtractAdapter;
import org.springframework.http.server.reactive.ServerHttpRequest;

public class TextMapAdapter extends TextMapExtractAdapter implements TextMap {
  private ServerHttpRequest.Builder requestBuilder;

  public TextMapAdapter(ServerHttpRequest serverHttpRequest, ServerHttpRequest.Builder requestBuilder) {
    super(serverHttpRequest.getHeaders().toSingleValueMap());
    this.requestBuilder = requestBuilder;
  }

  @Override
  public void put(String key, String value) {
    this.requestBuilder.header(key, value);
  }
}

