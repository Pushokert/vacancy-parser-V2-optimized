package com.vacancyparser.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration  // Временно отключено для запуска без Jaeger
public class OpenTelemetryConfig {

    @Value("${otel.service.name:vacancy-parser}")
    private String serviceName;

    @Value("${otel.exporter.jaeger.endpoint:http://localhost:14250}")
    private String jaegerEndpoint;

    @Bean
    public OpenTelemetry openTelemetry() {
        try {
            Resource resource = Resource.getDefault()
                    .merge(Resource.create(Attributes.builder()
                            .put("service.name", serviceName)
                            .build()));

            SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                    .addSpanProcessor(BatchSpanProcessor.builder(
                            JaegerGrpcSpanExporter.builder()
                                    .setEndpoint(jaegerEndpoint)
                                    .build())
                            .build())
                    .setResource(resource)
                    .build();

            return OpenTelemetrySdk.builder()
                    .setTracerProvider(sdkTracerProvider)
                    .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                    .buildAndRegisterGlobal();
        } catch (Exception e) {
            // Если Jaeger недоступен, возвращаем no-op OpenTelemetry
            return OpenTelemetry.noop();
        }
    }
}
