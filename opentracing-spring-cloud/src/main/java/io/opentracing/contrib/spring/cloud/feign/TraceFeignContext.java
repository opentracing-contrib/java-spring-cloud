package io.opentracing.contrib.spring.cloud.feign;

import feign.Client;
import feign.opentracing.TracingClient;
import io.opentracing.Tracer;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.cloud.netflix.feign.FeignContext;
import org.springframework.cloud.netflix.feign.ribbon.CachingSpringLoadBalancerFactory;
import org.springframework.cloud.netflix.feign.ribbon.LoadBalancerFeignClient;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;

/**
 * @author Pavol Loffay
 */
public class TraceFeignContext extends FeignContext {

  private FeignContext delegate;
  private Tracer tracer;
  private BeanFactory beanFactory;

  TraceFeignContext(Tracer tracer, FeignContext delegate, BeanFactory beanFactory) {
    this.delegate = delegate;
    this.tracer = tracer;
    this.beanFactory = beanFactory;
  }

  @Override
  public <T> T getInstance(String name, Class<T> type) {
    T object = this.delegate.getInstance(name, type);
    return (T) this.addTracingClient(object);
  }

  @Override
  public <T> Map<String, T> getInstances(String name, Class<T> type) {
    Map<String, T> instances = this.delegate.getInstances(name, type);
    if (instances == null) {
      return null;
    }
    Map<String, T> tracedInstances = new HashMap<>();
    for (Map.Entry<String, T> instanceEntry : instances.entrySet()) {
      tracedInstances.put(instanceEntry.getKey(), (T) this.addTracingClient(instanceEntry.getValue()));
    }
    return tracedInstances;
  }

  private Object addTracingClient(Object bean) {
    if (bean instanceof TracingClient || bean instanceof LoadBalancedTracedFeign) {
      return bean;
    }

    if (bean instanceof Client) {
      if (bean instanceof LoadBalancerFeignClient && !(bean instanceof LoadBalancedTracedFeign)) {
        return new LoadBalancedTracedFeign(new TracingClient(((LoadBalancerFeignClient)bean).getDelegate(), tracer),
            beanFactory.getBean(CachingSpringLoadBalancerFactory.class), beanFactory.getBean(SpringClientFactory.class));
      }
      return new TracingClient((Client) bean, tracer);
    }

    return bean;
  }

  /**
   * Needed for cast in {@link org.springframework.cloud.netflix.feign.FeignClientFactoryBean}
   */
  static class LoadBalancedTracedFeign extends LoadBalancerFeignClient {
    public LoadBalancedTracedFeign(Client delegate,
        CachingSpringLoadBalancerFactory lbClientFactory,
        SpringClientFactory clientFactory) {
      super(delegate, lbClientFactory, clientFactory);
    }
  }
}
