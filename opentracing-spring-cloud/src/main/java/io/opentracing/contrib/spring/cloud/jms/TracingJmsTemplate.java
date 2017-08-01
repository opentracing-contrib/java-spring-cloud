package io.opentracing.contrib.spring.cloud.jms;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;

import io.opentracing.ActiveSpan;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.JmsHeaderMapper;
import org.springframework.stereotype.Component;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
@Component
public class TracingJmsTemplate extends JmsTemplate {
  private Tracer tracer;
  private JmsHeaderMapper mapper;

  @Autowired
  public TracingJmsTemplate(ConnectionFactory connectionFactory, Tracer tracer, JmsHeaderMapper mapper) {
    super(connectionFactory);
    this.tracer = tracer;
    this.mapper = mapper;
  }

  @Override
  protected void doSend(MessageProducer producer, Message message) throws JMSException {
    MessageSpanTextMapAdapter.MessagingTextMap carrier = MessageSpanTextMapAdapter.convert(mapper, message);
    ActiveSpan span = tracer.activeSpan();
    tracer.inject(span.context(), Format.Builtin.TEXT_MAP, carrier);
    mapper.fromHeaders(carrier.getMessageHeaders(), message);
    super.doSend(producer, message);
  }
}
