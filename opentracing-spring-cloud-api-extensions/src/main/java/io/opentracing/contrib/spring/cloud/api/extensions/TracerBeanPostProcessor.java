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
package io.opentracing.contrib.spring.cloud.api.extensions;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;

import io.opentracing.Tracer;
import io.opentracing.contrib.api.TracerObserver;
import io.opentracing.contrib.api.tracer.APIExtensionsTracer;

@Configuration
public class TracerBeanPostProcessor implements BeanPostProcessor {

    private static final Log log = LogFactory.getLog(TracerBeanPostProcessor.class);

    @Autowired(required=false)
    private Set<TracerObserver> tracerObservers;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (tracerObservers != null && bean instanceof Tracer) {
            boolean observerFound = false;
            APIExtensionsTracer tracer = new APIExtensionsTracer((Tracer)bean);
            for (TracerObserver observer : tracerObservers) {
                if (observer != null) {
                    observerFound = true;
                    tracer.addTracerObserver(observer);
                }
            }
            if (observerFound) {
                log.info("Use extensions API tracer with observers=" + tracerObservers);
                return tracer;
            }
        }
        return bean;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

}
