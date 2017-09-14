package io.opentracing.contrib.spring.cloud.zuul;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapInjectAdapter;
import io.opentracing.tag.Tags;

class TracePreZuulFilter extends ZuulFilter {
    static final String COMPONENT_NAME = "zuul";

    private final Tracer tracer;

    TracePreZuulFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public String filterType() {
        return "pre";
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

        Span span = tracer.buildSpan(ctx.getRequest().getMethod())
                .withTag(Tags.COMPONENT.getKey(), COMPONENT_NAME)
                .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER)
                .withTag(Tags.HTTP_METHOD.getKey(), ctx.getRequest().getMethod())
                .withTag(Tags.HTTP_URL.getKey(), ctx.getRequest().getRequestURL().toString())
                .startManual();

        tracer.inject(span.context(), Format.Builtin.HTTP_HEADERS,
                new TextMapInjectAdapter(ctx.getZuulRequestHeaders()));

        ctx.set("span", span);

        return null;
    }


}
