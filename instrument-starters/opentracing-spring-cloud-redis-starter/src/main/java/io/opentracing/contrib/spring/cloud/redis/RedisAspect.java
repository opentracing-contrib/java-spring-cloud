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
package io.opentracing.contrib.spring.cloud.redis;

import io.opentracing.contrib.redis.spring.data2.connection.TracingRedisClusterConnection;
import io.opentracing.contrib.redis.spring.data2.connection.TracingRedisConnection;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Spring AOP Aspect wrapping Redis-related calls, delegating as much as possible to the official
 * OpenTracing Java Redis framework integration.
 *
 * @author Daniel del Castillo
 */
@Aspect
public class RedisAspect {

  @Pointcut("target(org.springframework.data.redis.connection.RedisConnectionFactory)")
  public void connectionFactory() {}

  @Pointcut("execution(org.springframework.data.redis.connection.RedisConnection *.getConnection(..))")
  public void getConnection() {}

  @Pointcut("execution(org.springframework.data.redis.connection.RedisClusterConnection *.getClusterConnection(..))")
  public void getClusterConnection() {}


  /**
   * Intercepts calls to {@link RedisConnectionFactory#getConnection()} (and related), wrapping the
   * outcome in a {@link TracingRedisConnection}
   *
   * @param pjp the intercepted join point
   * @return a new {@link TracingRedisConnection} wrapping the result of the joint point
   */
  @Around("getConnection() && connectionFactory()")
  public Object aroundGetConnection(final ProceedingJoinPoint pjp) throws Throwable {
    RedisConnection connection = (RedisConnection) pjp.proceed();
    return new TracingRedisConnection(connection, false, null);
  }

  /**
   * Intercepts calls to {@link RedisConnectionFactory#getClusterConnection()} (and related),
   * wrapping the outcome in a {@link TracingRedisClusterConnection}
   *
   * @param pjp the intercepted join point
   * @return a new {@link TracingRedisClusterConnection} wrapping the result of the joint point
   */
  @Around("getClusterConnection() && connectionFactory()")
  public Object aroundGetClusterConnection(final ProceedingJoinPoint pjp) throws Throwable {
    RedisClusterConnection clusterConnection = (RedisClusterConnection) pjp.proceed();
    return new TracingRedisClusterConnection(clusterConnection, false, null);
  }

}
