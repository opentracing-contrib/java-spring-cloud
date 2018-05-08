# Zipkin OpenTracing Spring Boot starter

This starter provides a single entry point for Spring Boot application to use in order to setup 
OpenTracing instrumentation reporting tracing data to Zipkin server.

## Configuration

```xml
<dependency>
  <groupId>io.opentracing.contrib</groupId>
  <artifactId>opentracing-spring-cloud-starter-zipkin</artifactId>
</dependency>
```

Simply adding the dependency to the application will ensure that all required OpenTracing and Zipkin dependencies are transitively resolved.
Furthermore the dependency will ensure that Spring Boot will auto configure all the necessary OpenTracing beans when the application starts.


By default, the Zipkin server is expected to collect traces at `http://localhost:9411/api/v2/spans`
encoded with `JSON_V2`. 
To change the default simply set the following the property:

```
opentracing.zipkin.http-sender.baseUrl=http://<host>:<port>
```

## Configuration options

All the available configuration options can be seen in [ZipkinConfigurationProperties](src/main/java/io/opentracing/contrib/spring/cloud/starter/zipkin/ZipkinConfigurationProperties.java).
The prefix to be used for these properties is `opentracing.zipkin`.
Furthermore, the service name is configured via the standard Spring Cloud `spring.application.name` property.

Beware to use the correct syntax for properties that are camel-case in `ZipkinConfigurationProperties`.

* For properties / yaml files use `-`. For example `opentracing.zipkin.http-sender.url=http://somehost:someport`
* For environment variables use `_`. For example `OPENTRACING_ZIPKIN_HTTP_SENDER_URL=http://somehost:someport` 

## Defaults

If no configuration options are changed and the user does not manually provide any of the beans that the 
auto-configuration process provides, the following defaults are used:

* `unknown-spring-boot` Will be used as the service-name if no value has been specified to the property `spring.application.name`. 
* `Sampler.ALWAYS_SAMPLE`
* `AsyncReporter` (using an `OkHttpSender`)


## Common cases

### Set service name 

Set `spring.application.name` to the desired name


### Sampling

* Boundary sampler

  `opentracing.zipkin.boundary-sampler.rate = value`
  
  Where `value` is between `0.0` (no sampling) and `1.0` (sampling of every request) 

* Counting sampler

  `opentracing.zipkin.counting-sampler.rate = value` 
  
  Where `value` is between `0.0` (no sampling) and `1.0` (sampling of every request)
  
  
The samplers above are mutually exclusive.

A custom sampler could of course be provided by declaring a bean of type `brave.sampler.Sampler`

### Reporter

By default starter configures `AsyncRepoter` using `OkHttpSender` with `JSON_V2` encoding.
Following properties can be changed to configure the reporter.

* `opentracing.zipkin.http-sender.encoder` - encoding of spans e.g. `JSON_V1`, `JSON_V2`, `PROTO3`
* `opentracing.zipkin.http-sernder.baseUrl` - set base url e.g. `http://zipkin:9411/`

## Advanced cases

### Manual bean provisioning

Any of the following beans can be provided by the application (by adding configuring them as bean with `@Bean` for example)
and will be used to by the Tracer instead of the auto-configured beans.

* `brave.sampler.Sampler`
* `zipkin2.reporter.Reporter`  

### brave.Tracing.Builder customization

Right before the `Tracer` is created, it is possible to provide arbitrary customizations to `Tracing.Builder` by providing a bean
of type `ZipkinTracerCustomizer`

## Caution

### Beware of the default sampler in production

In a high traffic environment, the default sampler that is configured is very unsafe since it samples every request.
It is therefore highly recommended to explicitly configure on of the other options in a production environment
