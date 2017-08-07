package io.opentracing.contrib.spring.cloud.jms;

import javax.jms.Message;
import javax.jms.MessageListener;

import io.opentracing.Tracer;
import io.opentracing.contrib.jms.common.TracingMessageListener;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
@Aspect
public class JmsListenerAspect {
  @Autowired
  Tracer tracer;

  @Around("@annotation(org.springframework.jms.annotation.JmsListener) && args(msg)")
  public Object aroundListenerMethod(final ProceedingJoinPoint pjp, Message msg) throws Throwable {
    JoinPointMessageListener pjpListener = new JoinPointMessageListener(pjp);
    MessageListener listener = new TracingMessageListener(pjpListener, tracer);
    listener.onMessage(msg);
    return pjpListener.returnValue;
  }

  private static class JoinPointMessageListener implements MessageListener {
    private final ProceedingJoinPoint pjp;
    private Object returnValue;

    public JoinPointMessageListener(ProceedingJoinPoint pjp) {
      this.pjp = pjp;
    }

    @Override
    public void onMessage(Message message) {
      try {
        returnValue = pjp.proceed();
      } catch (Throwable throwable) {
        throw new RuntimeException(throwable);
      }
    }
  }
}
