[![Build Status][ci-img]][ci] [![Released Version][maven-img]][maven]

Note: this is under an active development!

# OpenTracing Spring Cloud
This repository provides OpenTracing instrumentation for Spring Cloud. It can be used with any OpenTracing
compatible implementation.

It contains auto-configurations for Spring Boot which will instrument and trace several Spring Cloud and other integrations:
* Spring Web (RestControllers, RestTemplates, WebAsyncTask)
* @Async, @Scheduled, Executors
* WebSocket STOMP
* Feign, HystrixFeign
* Hystrix
* JMS
* JDBC
* Zuul
* Standard logging - logs are added to active span

This library is compatible with [Spring Cloud](http://projects.spring.io/spring-cloud/) `Camden.SR7`, `Dalston.SR3`
 and `Edgware.RELEASE`

## Comparison to `spring-cloud-sleuth`
[spring-cloud-sleuth](https://github.com/spring-cloud/spring-cloud-sleuth) also instruments 
a number of different frameworks. However, it is not currently possible to use it with the OpenTracing API, or
wire different instrumentations that are not supported by sleuth.

## Configuration
Just add the following dependency in your pom.xml:
```xml
<dependency>
  <groupId>io.opentracing.contrib</groupId>
  <artifactId>opentracing-spring-cloud-starter</artifactId>
</dependency>
```
, and provide OpenTracing tracer bean:
```java
@Bean
public io.opentracing.Tracer tracer() {
  return new // tracer instance of your choice (Zipkin, Jaeger, LightStep)
}
```

## Development
Maven checkstyle plugin is used to maintain consistent code style based on [Google Style Guides](https://github.com/google/styleguide)

```shell
./mvnw clean install
make // to run tests including dependency tests, a specific profile can be specified by make PROFILES=nodeps
```

## Release
Follow instructions in [RELEASE](RELEASE.md)

   [ci-img]: https://travis-ci.org/opentracing-contrib/java-spring-cloud.svg?branch=master
   [ci]: https://travis-ci.org/opentracing-contrib/java-spring-cloud
   [maven-img]: https://img.shields.io/maven-central/v/io.opentracing.contrib/opentracing-spring-cloud.svg?maxAge=2592000
   [maven]: http://search.maven.org/#search%7Cga%7C1%7Copentracing-spring-cloud
