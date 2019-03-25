/**
 * Copyright 2017-2019 The OpenTracing Authors
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
package io.opentracing.contrib.spring.cloud.jdbc;

import io.opentracing.contrib.jdbc.ConnectionInfo;
import io.opentracing.contrib.jdbc.TracingConnection;
import io.opentracing.contrib.jdbc.parser.URLParser;
import java.sql.Connection;
import java.util.Set;
import javax.sql.DataSource;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * Spring AOP Aspect wrapping JDBC-related calls, delegating as much as possible to the official
 * OpenTracing Java JDBC framework integration.
 *
 * @author Juraci Paixão Kröhling
 */
@Aspect
public class JdbcAspect {

  private final boolean withActiveSpanOnly;
  private final Set<String> ignoredStatements;

  public JdbcAspect(boolean withActiveSpanOnly, Set<String> ignoredStatements) {
    this.withActiveSpanOnly = withActiveSpanOnly;
    this.ignoredStatements = ignoredStatements;
  }

  /**
   * Intercepts calls to {@link DataSource#getConnection()} (and related), wrapping the outcome in a
   * {@link TracingConnection}
   *
   * @param pjp the intercepted join point
   * @return a new {@link TracingConnection} wrapping the result of the joint point
   * @throws Throwable in case of wrong JDBC URL
   */
  @Around("execution(java.sql.Connection *.getConnection(..)) && target(javax.sql.DataSource)")
  public Object getConnection(final ProceedingJoinPoint pjp) throws Throwable {
    Connection conn = (Connection) pjp.proceed();
    if (conn instanceof TracingConnection) {      
      return conn;
    }
    String url = conn.getMetaData().getURL();
    ConnectionInfo connectionInfo = URLParser.parser(url);
    return new TracingConnection(conn, connectionInfo, withActiveSpanOnly, ignoredStatements);
  }
}
