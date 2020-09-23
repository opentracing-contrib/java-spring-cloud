[![Build Status][ci-img]][ci] [![Released Version][maven-img]][maven]

# OpenTracing Spring Cloud
This repository provides OpenTracing instrumentation for Spring Boot and its various extensions. It can be used with any OpenTracing
compatible implementation.

It contains auto-configurations which instruments and trace following Spring Boot projects:
* Spring Web (RestControllers, RestTemplates, WebAsyncTask, WebClient, WebFlux)
* @Async, @Scheduled, Executors
* WebSocket STOMP
* Feign, HystrixFeign
* Hystrix
* JMS
* JDBC
* Kafka
* Mongo
* Zuul
* Reactor
* RxJava
* Redis
* Standard logging - logs are added to active span
* Spring Messaging - trace messages being sent through [Messaging Channels](https://docs.spring.io/spring-integration/reference/html/#messaging-channels-section)
* RabbitMQ

## Compatibility table

The following table shows versions with compatible [Spring Cloud](http://projects.spring.io/spring-cloud/) releases.

opentracing-spring-cloud version | OpenTracing API | Spring Cloud version
--- | --- | ---
0.5.x | 0.33.0 | `Hoxton`
0.4.x | 0.32.0 | `Hoxton`
0.3.x | 0.32.0 | `Greenwich`
0.2.x | 0.31.0 | `Finchley`, `Greenwich`
0.1.x | 0.31.0 | `Dalston`, `Edgware`


## Comparison to `spring-cloud-sleuth`
This project is similar to [spring-cloud-sleuth](https://github.com/spring-cloud/spring-cloud-sleuth),
both provide out of the box tracing solution for Spring Boot/Cloud. Some of the instrumentations in this
package are based on original `sleuth` work.

However there are a couple of differences:
* OpenTracing support in `sleuth` is limited to only one tracer implementation - [brave-opentracing](https://github.com/openzipkin-contrib/brave-opentracing). In other words it's not possible to use arbitrary OpenTracing tracer with `sleuth`.
* `sleuth` might support different set of instrumentations.
* Instrumentations in `sleuth` might add different set of tags and logs to represent the same events.

## Note on dependencies

It's worth noting that the although OpenTracing Spring Cloud contains code for instrumenting a wealth of Spring projects,
it however does not pull those dependencies automatically, marking them as optional dependencies instead.

That means that for example a simple Spring Boot REST API application can include OpenTracing Spring Cloud without the fear
of polluting the classpath with Spring Cloud dependencies that are otherwise unneeded

## Configuration

The preferred way to use this library is via vendored starters. These starters use
instrumentations from this library and expose specific tracer configuration in Spring
native way:

* [Jaeger](https://github.com/opentracing-contrib/java-spring-jaeger)
* [Zipkin](https://github.com/opentracing-contrib/java-spring-zipkin)

### Explicitly tracer configuration

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

### Properties

Property| Default| Description
------------- | ------------- | -------------
opentracing.spring.cloud.reactor.enabled|true|Enable Reactor tracing.
opentracing.spring.cloud.async.enabled|true|Enable tracing for @Async, Executor and WebAsyncTask/Callable.
opentracing.spring.cloud.log.enabled|true|Add standard logging output to tracing system.
opentracing.spring.cloud.scheduled.enabled|true|Enable @Scheduled tracing.
opentracing.spring.cloud.feign.enabled|true|Enable Feign tracing.
opentracing.spring.cloud.gateway.enabled|true|Enable Gateway tracing.
opentracing.spring.cloud.hystrix.strategy.enabled|true|Enable Propagation of spans across threads using in Hystrix command tracing.
opentracing.spring.cloud.jdbc.enabled|true|Enable JDBC tracing.
opentracing.spring.cloud.jms.enabled|true|Enable JMS tracing.
opentracing.spring.cloud.kafka.enabled|true|Enable Kafka tracing.
opentracing.spring.cloud.mongo.enabled|true|Enable MongoDB tracing.
opentracing.spring.cloud.reactor.enabled|true|Enable Reactor tracing.
opentracing.spring.cloud.rxjava.enabled|true|Enable RxJava tracing.
opentracing.spring.cloud.websocket.enabled|true|Enable Websocket tracing.
opentracing.spring.cloud.zuul.enabled|true|Enable Zuul tracing.
opentracing.spring.cloud.redis.enabled|true|Enable Redis tracing.
opentracing.spring.cloud.redis.prefixOperationName|""|Set a prefix for each Redis operation, e.g: MyPrefix.SET.
opentracing.spring.cloud.jdbc.withActiveSpanOnly|false|Only trace JDBC calls if they are part of an active Span.
opentracing.spring.cloud.jdbc.ignoreStatements|null|Set of JDBC statements to not trace.

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

## License

[Apache 2.0 License](./LICENSE).
