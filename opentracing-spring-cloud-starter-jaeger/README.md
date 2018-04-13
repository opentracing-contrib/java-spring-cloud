# Jaeger OpenTracing Spring Boot starter

This starter provides a single entry point for Spring Boot application to use in order to setup OpenTracing instrumentation 

## Configuration

```xml
<dependency>
  <groupId>io.opentracing.contrib</groupId>
  <artifactId>opentracing-spring-cloud-starter-jaeger</artifactId>
</dependency>
```

Simply adding the dependency to the application will ensure that all required OpenTracing and Jaeger dependencies are transitively resolved.
Furthermore the dependency will ensure that Spring Boot will auto configure all the necessary OpenTracing beans when the application starts.  

## Configuration options

All the available configuration options can be seen in [JaegerConfigurationProperties](src/main/java/io/opentracing/contrib/spring/cloud/starter/jaeger/JaegerAutoConfiguration.java).
The prefix to be used for these properties is `opentracing.jaeger`.
Furthermore, the service name is configured via the standard Spring Cloud `spring.application.name` property.

Beware to use the correct syntax for properties that are camel-case in `JaegerConfigurationProperties`.

* For properties / yaml files use `-`. For example `opentracing.jaeger.log-spans=true`
* For environment variables use `_`. For example `OPENTRACING_JAEGER_LOG_SPANS` 

## Defaults

If no configuration options are changed and the user does not manually provide any of the beans that the 
auto-configuration process provides, the following defaults are used:

* `unknown-spring-boot` Will be used as the service-name if no value has been specified to the property `spring.application.name`. 
* `CompositeReporter` is provided which contains the following delegates:
  - `LoggingReporter` for reporting spans to the console
  - `RemoteReporter` that contains a `UdpSender` that sends spans to `localhost:6831` 
* `ConstSampler` with the value of `true`. This means that every trace will be sampled
* `NoopMetricsFactory` is used - effectively meaning that no metrics will be collected

## Common cases

### Set service name 

Set `spring.application.name` to the desired name

### HTTP Sender

`opentracing.jaeger.http-sender.url = http://jaegerhost:portNumber` 

Note that when an HTTP Sender is defined, the UDP sender is not used, even if it has been configured

### UDP Sender

`opentracing.jaeger.udp-sender.host=jaegerhost`
`opentracing.jaeger.udp-sender.port=portNumber`

### Log Spans

Be default spans are logged to the console. This can be disabled by setting:

`opentracing.jaeger.log-spans = false`

### Additional reporters

By defining a bean of type `ReporterAppender`, the code has the chance to add any Reporter without 
having to forgo what the auto-configuration provides  

### Sampling

* Const sampler

  `opentracing.jaeger.const-sampler.decision = true | false` 

* Probabilistic sampler

  `opentracing.jaeger.probabilistic-sampler.sampling-rate = value` 
  
  Where `value` is between `0.0` (no sampling) and `1.0` (sampling of every request)

* Rate-limiting sampler

  `opentracing.jaeger.rate-limiting-max-traces-per-second = value` 
  
  Where `value` is between `0.0` (no sampling) and `1.0` (sampling of every request)
  
  
The samplers above are mutually exclusive.

A custom sampler could of course be provided by declaring a bean of type `com.uber.jaeger.samplers.Sampler`

### Propagate headers in B3 format (for compatibility with Zipkin collectors)

`opentracing.jaeger.enable-b3-propagation = true`

## Advanced cases

### Manual bean provisioning

Any of the following beans can be provided by the application (by adding configuring them as bean with `@Bean` for example)
and will be used to by the Tracer instead of the auto-configured beans.

* `com.uber.jaeger.samplers.Sampler`  
* `com.uber.jaeger.metrics.MetricsFactory`  
* `com.uber.jaeger.metrics.Metrics`  
* `com.uber.jaeger.reporters.Reporter`

### com.uber.jaeger.Tracer.Builder customization

Right before the `Tracer` is created, it is possible to provide arbitrary customizations to `Tracer.Builder` by providing a bean
of type `JaegerTracerCustomizer`

## Caution

### Beware of the default sampler in production

In a high traffic environment, the default sampler that is configured is very unsafe since it samples every request.
It is therefore highly recommended to explicitly configure on of the other options in a production environment
