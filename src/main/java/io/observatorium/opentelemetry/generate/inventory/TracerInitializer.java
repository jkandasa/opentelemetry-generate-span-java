package io.observatorium.opentelemetry.generate.inventory;

import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class TracerInitializer {
    OpenTelemetrySdk sdkProvider;

    @Produces
    @Inventory
    Tracer tracer;

    void onStart(@Observes StartupEvent ev) {
		JaegerGrpcSpanExporter jaegerExporter = JaegerGrpcSpanExporter.builder()
				.setEndpoint("http://" + System.getenv().getOrDefault("OTEL_OTLP_ENDPOINT", "localhost:14250")).build();
        BatchSpanProcessor spanProcessor = BatchSpanProcessor.builder(jaegerExporter)
                .setScheduleDelay(100, TimeUnit.MILLISECONDS).build();

        sdkProvider = OpenTelemetrySdk.builder()
                .setTracerProvider(SdkTracerProvider.builder().addSpanProcessor(spanProcessor)
                        .setResource(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "inventory")))
                        .build())
                .build();

        tracer = sdkProvider.getTracer("inventory");
    }

    void onStop(@Observes ShutdownEvent ev) {
        // shutdown for processors and exporters should be called as a result of
        // shutting down the tracing provider
        sdkProvider.getSdkTracerProvider().shutdown();
    }
}
