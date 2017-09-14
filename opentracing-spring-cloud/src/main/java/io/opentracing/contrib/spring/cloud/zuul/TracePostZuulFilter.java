package io.opentracing.contrib.spring.cloud.zuul;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class TracePostZuulFilter extends ZuulFilter {
    private final Tracer tracer;

    public TracePostZuulFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public String filterType() {
        return "post";
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();

        Object spanObject = ctx.get("span");
        if (spanObject instanceof Span) {
            Span span = (Span) spanObject;
            span.setTag(Tags.HTTP_STATUS.getKey(), ctx.getResponseStatusCode());

            if (ctx.getThrowable() != null) {
                onError(ctx.getThrowable(), span);
            }

            if (ctx.getRouteHost() != null) {
                span.setTag("route.host", ctx.getRouteHost().toString());
            }

            span.finish();
        }

        return null;
    }

    private static void onError(Throwable throwable, Span span) {
        Tags.ERROR.set(span, Boolean.TRUE);

        if (throwable != null) {
            span.log(errorLogs(throwable));
        }
    }

    private static Map<String, Object> errorLogs(Throwable throwable) {
        Map<String, Object> errorLogs = new HashMap<>();
        errorLogs.put("event", Tags.ERROR.getKey());
        errorLogs.put("error.kind", throwable.getClass().getName());
        errorLogs.put("error.object", throwable);

        errorLogs.put("message", throwable.getMessage());

        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        errorLogs.put("stack", sw.toString());

        return errorLogs;
    }
}
