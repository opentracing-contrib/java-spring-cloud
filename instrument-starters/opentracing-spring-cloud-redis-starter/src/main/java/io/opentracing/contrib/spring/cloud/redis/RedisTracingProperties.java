package io.opentracing.contrib.spring.cloud.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 *
 * @author Luram Archanjo
 */
@ConfigurationProperties("opentracing.spring.cloud.redis")
public class RedisTracingProperties {

  private boolean enabled = true;

  private String prefixOperationName;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getPrefixOperationName() {
    return prefixOperationName;
  }

  public void setPrefixOperationName(String prefixOperationName) {
    this.prefixOperationName = prefixOperationName;
  }

}
