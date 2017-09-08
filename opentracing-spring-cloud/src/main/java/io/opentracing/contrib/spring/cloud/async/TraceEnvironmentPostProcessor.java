package io.opentracing.contrib.spring.cloud.async;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

/**
 * Adds default properties for the application:
 * <ul>
 *     <li>logging pattern level that prints trace information (e.g. trace ids)</li>
 *     <li>enables usage of subclass-based (CGLIB) proxies are to be created as opposed
 * to standard Java interface-based proxies</li>
 *     It's required for the tracing aspects like
 *     {@link io.opentracing.contrib.spring.cloud.async.TraceAsyncAspect}
 * </ul>
 *
 * @author Dave Syer
 * @author Pavol Loffay
 * @since 1.0.0
 */
public class TraceEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "defaultProperties";
    private static final String SPRING_AOP_PROXY_TARGET_CLASS = "spring.aop.proxyTargetClass";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> map = new HashMap<>();

        if (!environment.containsProperty(SPRING_AOP_PROXY_TARGET_CLASS)) {
            map.put(SPRING_AOP_PROXY_TARGET_CLASS, "true");
        }

        addOrReplace(environment.getPropertySources(), map);
    }

    private void addOrReplace(MutablePropertySources propertySources,
                              Map<String, Object> map) {
        MapPropertySource target = null;
        if (propertySources.contains(PROPERTY_SOURCE_NAME)) {
            PropertySource<?> source = propertySources.get(PROPERTY_SOURCE_NAME);
            if (source instanceof MapPropertySource) {
                target = (MapPropertySource) source;
                for (String key : map.keySet()) {
                    if (!target.containsProperty(key)) {
                        target.getSource().put(key, map.get(key));
                    }
                }
            }
        }
        if (target == null) {
            target = new MapPropertySource(PROPERTY_SOURCE_NAME, map);
        }
        if (!propertySources.contains(PROPERTY_SOURCE_NAME)) {
            propertySources.addLast(target);
        }
    }
}
