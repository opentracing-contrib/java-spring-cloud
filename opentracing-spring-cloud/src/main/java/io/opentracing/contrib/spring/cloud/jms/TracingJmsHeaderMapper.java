package io.opentracing.contrib.spring.cloud.jms;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.opentracing.Tracer;
import org.springframework.jms.support.SimpleJmsHeaderMapper;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class TracingJmsHeaderMapper extends SimpleJmsHeaderMapper {

  public static final String SPAN_ID_NAME = "spanId";
  public static final String SAMPLED_NAME = "spanSampled";
  public static final String PROCESS_ID_NAME = "spanProcessId";
  public static final String PARENT_ID_NAME = "spanParentSpanId";
  public static final String TRACE_ID_NAME = "spanTraceId";
  public static final String SPAN_NAME_NAME = "spanName";
  public static final String SPAN_FLAGS_NAME = "spanFlags";

  static final String MESSAGE_SENT_FROM_CLIENT = "messageSent";
  static final String HEADER_DELIMITER = "_";

  private BiMap<String, String> map = HashBiMap.create();

  public static TracingJmsHeaderMapper braveHeaderMapper() {
    TracingJmsHeaderMapper mapper = new TracingJmsHeaderMapper();
    mapper.addMapping(SPAN_ID_NAME, "X-B3-SpanId");
    mapper.addMapping(SAMPLED_NAME, "X-B3-Sampled");
    mapper.addMapping(PARENT_ID_NAME, "X-B3-ParentSpanId");
    mapper.addMapping(TRACE_ID_NAME, "X-B3-TraceId");
    mapper.addMapping(SPAN_FLAGS_NAME, "X-B3-Flags");
    return mapper;
  }

  public static TracingJmsHeaderMapper jaegerHeaderMapper() {
    TracingJmsHeaderMapper mapper = new TracingJmsHeaderMapper();
    mapper.addMapping("jaegerDebugId", "jaeger-debug-id");
    mapper.addMapping(TRACE_ID_NAME, "uber-trace-id");
    return mapper;
  }

  public static TracingJmsHeaderMapper fromTracer(Tracer tracer) {
    Class<?> tracerClass = tracer.getClass();
    String tracerClassName = tracerClass.getName();

    if (tracerClassName.contains("jaeger.")) {
      return jaegerHeaderMapper();
    } else if (tracerClassName.contains("brave.")) {
      return braveHeaderMapper();
    } else {
      return new TracingJmsHeaderMapper();
    }
  }

  public void addMapping(String otKey, String implKey) {
    map.put(otKey, implKey);
  }

  @Override
  protected String fromHeaderName(String headerName) {
    String value = map.inverse().get(headerName);
    if (value != null) {
      return value;
    }
    return super.fromHeaderName(headerName);
  }

  @Override
  protected String toHeaderName(String propertyName) {
    String value = map.get(propertyName);
    if (value != null) {
      return value;
    }
    return super.toHeaderName(propertyName);
  }
}
