/**
 * Copyright 2017 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.opentracing.contrib.spring.cloud.jms;

import io.opentracing.Tracer;
import io.opentracing.contrib.jms.common.TracingMessageListener;
import javax.jms.Message;
import javax.jms.MessageListener;
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
