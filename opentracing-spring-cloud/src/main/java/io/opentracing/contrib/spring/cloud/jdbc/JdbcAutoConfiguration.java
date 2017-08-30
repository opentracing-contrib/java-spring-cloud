package io.opentracing.contrib.spring.cloud.jdbc;


import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Loads the integration with OpenTracing JDBC if it's included in the classpath.
 *
 * @author Juraci Paixão Kröhling
 */
@Configuration
@ConditionalOnProperty(name = "opentracing.spring.cloud.jdbc.enabled", havingValue = "true", matchIfMissing = true)
public class JdbcAutoConfiguration {

  @Bean
  public JdbcAspect jdbcAspect() {
    return new JdbcAspect();
  }
}
