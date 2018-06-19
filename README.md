[![Build Status][ci-img]][ci] [![Released Version][maven-img]][maven]

# OpenTracing Spring Cloud
This repository provides OpenTracing instrumentation for Spring Boot and its various extensions. It can be used with any OpenTracing
compatible implementation.

It contains auto-configurations which instruments and trace following Spring Boot projects:
* Spring Web (RestControllers, RestTemplates, WebAsyncTask)
* @Async, @Scheduled, Executors
* WebSocket STOMP
* Feign, HystrixFeign
* Hystrix
* JMS
* JDBC
* Mongo
* Zuul
* RxJava
* Standard logging - logs are added to active span
* Spring Messaging - trace messages being sent through [Messaging Channels](https://docs.spring.io/spring-integration/reference/html/messaging-channels-section.html)
* RabbitMQ

This library is compatible with [Spring Cloud](http://projects.spring.io/spring-cloud/) `Camden.SR7`, `Dalston.SR3`
 and `Edgware.RELEASE`

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
