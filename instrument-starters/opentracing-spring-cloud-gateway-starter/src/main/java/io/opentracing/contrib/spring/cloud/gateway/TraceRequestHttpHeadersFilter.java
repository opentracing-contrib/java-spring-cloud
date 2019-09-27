package io.opentracing.contrib.spring.cloud.gateway;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.contrib.spring.web.client.HttpHeadersCarrier;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;



/**
 * TraceRequestHttpHeadersFilter
 *
 * @author Weichao Li (liweichao0102@gmail.com)
 * @since 2019/9/26
 */
final class TraceRequestHttpHeadersFilter extends AbstractHttpHeadersFilter {

  private final Logger log = LoggerFactory.getLogger(TraceRequestHttpHeadersFilter.class);

  private final String ROUTE_ID = "route.id";

  private final String COMPONENT = "java-spring-cloud-gateway";

  protected TraceRequestHttpHeadersFilter(Tracer tracer) {
    super(tracer);
  }

  @Override
  public HttpHeaders filter(HttpHeaders input, ServerWebExchange exchange) {
    log.debug("Will instrument spring cloud gateway the HTTP request headers");
    ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
    Span span = this.tracer.buildSpan(path(builder))
        .asChildOf(tracer.activeSpan())
        .withTag(Tags.COMPONENT.getKey(), COMPONENT)
        .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT)
        .withTag(ROUTE_ID, getRouteId(exchange))
        .start();
    log.debug("Client span {} created for the request. New headers are {}", span, builder.build().getHeaders().toSingleValueMap());
    exchange.getAttributes().put(SPAN_ATTRIBUTE, span);
    HttpHeaders headersWithInput = new HttpHeaders();
    try {
      this.tracer.inject(span.context(), Format.Builtin.HTTP_HEADERS, new HttpHeadersCarrier(headersWithInput));
    } catch (Exception ignore) {
      log.error("TraceRequestHttpHeadersFilter error", ignore);
    }
    headersWithInput.addAll(input);
    addHeadersWithInput(builder, headersWithInput);
    return headersWithInput;
  }

  private String getRouteId(ServerWebExchange exchange) {
    String routeId = "unknown";
    Route route = exchange.getAttribute(ROUTE_ATTRIBUTE);
    if (Objects.nonNull(route)) {
      return route.getId();
    }
    return routeId;
  }

  private void addHeadersWithInput(ServerHttpRequest.Builder builder,
                                   HttpHeaders headersWithInput) {
    for (Map.Entry<String, List<String>> entry : builder.build().getHeaders()
        .entrySet()) {
      String key = entry.getKey();
      List<String> value = entry.getValue();
      headersWithInput.put(key, value);
    }
  }

  @Override
  public boolean supports(Type type) {
    return type.equals(Type.REQUEST);
  }
}

final class TraceResponseHttpHeadersFilter extends AbstractHttpHeadersFilter {

  private final Logger log = LoggerFactory.getLogger(TraceResponseHttpHeadersFilter.class);

  protected TraceResponseHttpHeadersFilter(Tracer tracer) {
    super(tracer);
  }

  @Override
  public HttpHeaders filter(HttpHeaders input, ServerWebExchange exchange) {
    Object storedSpan = exchange.getAttribute(SPAN_ATTRIBUTE);
    if (storedSpan == null) {
      return input;
    }
    log.debug("Will instrument the response");
    Span span = (Span) storedSpan;
    if (Objects.nonNull(span)) {
      span.finish();
    }
    log.debug("The response was handled for span " + storedSpan);
    return input;
  }

  @Override
  public boolean supports(Type type) {
    return type.equals(Type.RESPONSE);
  }
}

abstract class AbstractHttpHeadersFilter implements HttpHeadersFilter {

  protected final String SPAN_ATTRIBUTE = Span.class.getName();

  protected final String ROUTE_ATTRIBUTE = ServerWebExchangeUtils.class.getName() + ".gatewayRoute";

  final Tracer tracer;

  protected AbstractHttpHeadersFilter(Tracer tracer) {
    this.tracer = tracer;
  }

  public String path(ServerHttpRequest.Builder request) {
    return request.build().getPath().value();
  }
}
