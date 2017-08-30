package io.opentracing.contrib.spring.cloud.jdbc;

import io.opentracing.contrib.jdbc.TracingConnection;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Spring AOP Aspect wrapping JDBC-related calls, delegating as much as possible to the official OpenTracing Java JDBC
 * framework integration.
 *
 * @author Juraci Paixão Kröhling
 */
@Aspect
public class JdbcAspect {

  /**
   * Intercepts calls to {@link DataSource#getConnection()} (and related), wrapping the outcome in a {@link TracingConnection}
   * @param pjp the intercepted join point
   * @return  a new {@link TracingConnection} wrapping the result of the joint point
   */
  @Around("execution(java.sql.Connection *.getConnection(..)) && target(javax.sql.DataSource)")
  public Object getConnection(final ProceedingJoinPoint pjp) throws Throwable {
    Connection conn = (Connection) pjp.proceed();
    String url = conn.getMetaData().getURL();
    String user = conn.getMetaData().getUserName();
    String dbType;
    try {
      dbType = url.split(":")[1];
    } catch (Throwable t) {
      throw new IllegalArgumentException("Invalid JDBC URL. Expected to find the database type after the first ':'. URL: " + url);
    }
    return new TracingConnection(conn, dbType, user);
  }
}
