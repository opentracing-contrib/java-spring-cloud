[![Build Status][ci-img]][ci] [![Released Version][maven-img]][maven]

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
* RxJava
* Standard logging - logs are added to active span
* Spring Messaging - trace messages being sent through [Messaging Channels](https://docs.spring.io/spring-integration/reference/html/messaging-channels-section.html)

This library is compatible with [Spring Cloud](http://projects.spring.io/spring-cloud/) `Camden.SR7`, `Dalston.SR3`
 and `Edgware.RELEASE`

## Comparison to `spring-cloud-sleuth`
This project is similar to [spring-cloud-sleuth](https://github.com/spring-cloud/spring-cloud-sleuth), 
both provide out of the box tracing solution for Spring Boot/Cloud. Some of the instrumentations in this 
package are based on original `sleuth` work.

However there are a couple of differences:
* OpenTracing support in `sleuth` is limited to one tracer implementation - [brave-opentracing](https://github.com/openzipkin-contrib/brave-opentracing). In other words it's not possible to use arbitrary OpenTracing tracer with `sleuth`.
* `sleuth` might support different set of instrumentations.
* Instrumentations in `sleuth` might add different set of tags and logs to represent the same events.

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

## Using the Jaeger auto-configuration module

See the [README.md](opentracing-spring-cloud-starter-jaeger/README.md) file

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
