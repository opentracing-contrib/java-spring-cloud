## Configuration options

All the available configuration options can be seen in `io.opentracing.contrib.spring.cloud.starter.jaeger.JaegerConfigurationProperties`.
The prefix to be used for these properties is `opentracing.jaeger`.

Beware to use the correct syntax for properties that are camel-case in `JaegerConfigurationProperties`.

* For properties / yaml files use `-`. For example `opentracing.jaeger.log-spans=true`
* For environment variables use `_`. For example `OPENTRACING_JAEGER_LOG_SPANS` 

## Defaults

If no configuration options are changed and the user does not manually provide any of the beans that the 
auto-configuration process provides, the following defaults are used:

* `unknown-spring-boot` Will be used as the service-name if no value has been specified to the property `spring.application.name`. 
* A `CompositeReporter` is provided which does not contain any delegates - effectively functioning as a Noop `Reporter`
* A `ConstSampler` with the value of `true`. This means that every trace will be sampled
* A `NoopMetricsFactory` is used - effectively meaning that no metrics will be collected

## Manual bean provisioning

Any of the following beans can be provided by the application (by adding configuring them as bean with `@Bean` for example)
and will be used to by the Tracer instead of the auto-configured beans.

* `com.uber.jaeger.samplers.Sampler`  
* `com.uber.jaeger.metrics.MetricsFactory`  
* `com.uber.jaeger.metrics.Metrics`  
* `com.uber.jaeger.reporters.Reporter`

## Common cases

### Set service name 

Set `spring.application.name` to the desired name

### Define an HTTP collector

Set `opentracing.jaeger.http-sender.url` to the URL of the Jaeger collector

### Define a UDP collector

Set `opentracing.jaeger.udp-sender.host` to the host of the Jaeger collector
and `opentracing.jaeger.udp-sender.port` to the end of the Jaeger collector

### Enable logging of spans

Set `opentracing.jaeger.log-spans` to `true`

### Use a probabilistic sampler 

Set `opentracing.jaeger.probabilistic-sampler.sampling-rate` to a value between `0.0` (no sampling) and `1.0` (sampling of every request)

### Propagate headers in B3 format (for compatibility with Zipkin collectors)

Set `opentracing.jaeger.enable-b3-propagation` to `true`

## Advanced cases

### com.uber.jaeger.Tracer.Builder customization

Before creating the `Tracer` it is possible to provide arbitrary customizations to `Tracer.Builder` by providing a bean
of type `JaegerTracerCustomizer`

### Add custom reporter while maintaining the ability to autoconfigure standard ones with properties

By supplying a bean of `ReporterAppender` the user can add custom as many custom `Reporter` as needed without
having the forgo the ability to configure the standard reporters via auto-configuration

## Caution

### Beware of the default sampler in production

In a high traffic environment, the default sampler that is configured is very unsafe since it samples every request.
It is therefore highly recommended to explicitly configure on of the other options in a production environment


## Development

### Executing tests

In order for all tests to run correctly, the docker daemon need to be running on the system

Run the tests be executing

`mvn clean test` 
