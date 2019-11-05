import io.jaegertracing.internal.JaegerTracer;
import io.opentelemetry.opentracingshim.*;
import io.opentracing.util.GlobalTracer;
import io.opentracing.contrib.tracerresolver.TracerFactory;
import io.opentracing.Tracer;

public class OpenTelemetryShim {
	
	
	/*
	 *class to Add OpenTracing to OpenTelemetry shim:
	 *In order for OpenTracing users to migrate to OpenTelemetry 
	 *we should provide a shim that allows existing
	 *OpenTracing instrumentation to report directly to OpenTelemetry.
	 */
	public Tracer tracer;
	public TracerFactory tFacory;
	public GlobalTracer gTracer;
	
	/*
     * TODO
	 * const tracer = new opentelemetry.BasicTracer(myExporters);
	 * opentracing.initGlobalTracer(new opentelemtry.OpenTracingShim(tracer));
	 * 
	 */
	
	public void reportToOpenTelemetry(Tracer t, JaegerTracer jTracer) {
	
		
	}

}
