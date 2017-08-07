package io.opentracing.contrib.spring.cloud.jms;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jms.Message;

import io.opentracing.propagation.TextMap;
import org.springframework.jms.support.JmsHeaderMapper;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;
import org.springframework.util.StringUtils;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
class MessageSpanTextMapAdapter {
  static MessagingTextMap convert(JmsHeaderMapper mapper, Message msg) {
    org.springframework.messaging.Message<Message> springMsg = new GenericMessage<>(msg, mapper.toHeaders(msg));
    MessageBuilder<Message> delegate = MessageBuilder.fromMessage(springMsg);
    return new MessagingTextMap(delegate);
  }

  static class MessagingTextMap implements TextMap {

    private final MessageBuilder delegate;

    MessagingTextMap(MessageBuilder delegate) {
      this.delegate = delegate;
    }

    MessageHeaders getMessageHeaders() {
      return delegate.build().getHeaders();
    }

    public Iterator<Map.Entry<String, String>> iterator() {
      Map<String, String> map = new HashMap<>();
      for (Map.Entry<String, Object> entry : delegate.build().getHeaders().entrySet()) {
        map.put(entry.getKey(), String.valueOf(entry.getValue()));
      }
      return map.entrySet().iterator();
    }

    @SuppressWarnings("unchecked")
    public void put(String key, String value) {
      if (!StringUtils.hasText(value)) {
        return;
      }
      org.springframework.messaging.Message<?> initialMessage = delegate.build();
      MessageHeaderAccessor accessor = MessageHeaderAccessor.getMutableAccessor(initialMessage);
      accessor.setHeader(key, value);
      if (accessor instanceof NativeMessageHeaderAccessor) {
        NativeMessageHeaderAccessor nativeAccessor = (NativeMessageHeaderAccessor) accessor;
        nativeAccessor.setNativeHeader(key, value);
      }
      delegate.copyHeaders(accessor.toMessageHeaders());
    }
  }

}
