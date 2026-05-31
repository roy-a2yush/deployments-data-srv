package com.zephyr.deployments_data_srv.interceptor;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Spring MVC Handler Interceptor to seamlessly manage OpenTelemetry tracing contexts
 * and automatically inject tracing properties (trace_id and span_id) into SLF4J MDC.
 */
@Slf4j
@Component
public class TelemetryInterceptor implements HandlerInterceptor {

    private final Tracer tracer;
    private static final String SPAN_KEY = "otel.span";
    private static final String SCOPE_KEY = "otel.scope";

    public TelemetryInterceptor() {
        this.tracer = GlobalOpenTelemetry.getTracer("deployments-data-srv");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Span currentSpan = Span.current();
        boolean createdNewSpan = false;
        Scope scope = null;

        // If no active trace context is present, start a new telemetry span for the request
        if (!currentSpan.getSpanContext().isValid()) {
            currentSpan = tracer.spanBuilder("http.request")
                    .setAttribute("http.method", request.getMethod())
                    .setAttribute("http.url", request.getRequestURI())
                    .startSpan();
            scope = currentSpan.makeCurrent();
            createdNewSpan = true;
            log.debug("No active OTel telemetry context detected. Initialized a new trace span context.");
        }

        String traceId = currentSpan.getSpanContext().getTraceId();
        String spanId = currentSpan.getSpanContext().getSpanId();

        // Automatically inject tracing fields into SLF4J MDC for unified log formatting
        MDC.put("trace_id", traceId);
        MDC.put("span_id", spanId);

        if (createdNewSpan) {
            request.setAttribute(SPAN_KEY, currentSpan);
            request.setAttribute(SCOPE_KEY, scope);
        }

        log.info("Incoming HTTP request. Method: {}, URI: {}, trace_id: {}, span_id: {}", 
                request.getMethod(), request.getRequestURI(), traceId, spanId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Span span = (Span) request.getAttribute(SPAN_KEY);
        Scope scope = (Scope) request.getAttribute(SCOPE_KEY);

        if (span != null) {
            if (ex != null) {
                span.recordException(ex);
                log.error("HTTP Request failed with exception. [trace_id: {}]", span.getSpanContext().getTraceId(), ex);
            }
            span.end();
            log.debug("OTel request span context finalized and ended.");
        }

        if (scope != null) {
            scope.close();
        }

        // Clean up MDC context to prevent thread-pool memory pollution
        MDC.remove("trace_id");
        MDC.remove("span_id");
    }
}
