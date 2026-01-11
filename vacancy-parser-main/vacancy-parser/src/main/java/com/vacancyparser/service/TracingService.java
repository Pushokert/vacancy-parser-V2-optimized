package com.vacancyparser.service;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.context.Scope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Сервис для распределенного трейсинга с использованием OpenTelemetry
 */
@Service
@Slf4j
public class TracingService {

    private final OpenTelemetry openTelemetry;
    private final Tracer tracer;

    public TracingService(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
        Tracer tempTracer = null;
        try {
            TracerProvider tracerProvider = openTelemetry.getTracerProvider();
            if (tracerProvider != null) {
                tempTracer = tracerProvider.get("vacancy-parser", "1.0.0");
            }
        } catch (Exception e) {
            log.warn("Failed to initialize tracer, tracing will be disabled", e);
        }
        this.tracer = tempTracer;
    }

    /**
     * Создаёт span для парсинга вакансий
     * @param source источник парсинга
     * @param url URL для парсинга
     * @return Span для парсинга
     */
    public Span startParsingSpan(String source, String url) {
        Span span = tracer.spanBuilder("parse_vacancies")
                .setAttribute("source", source)
                .setAttribute("url", url)
                .startSpan();
        
        log.info("Started parsing span for source: {}, url: {}", source, url);
        return span;
    }

    /**
     * Создаёт span для этапа парсинга
     * @param parentSpan родительский span
     * @param stageName название этапа
     * @return Span для этапа
     */
    public Span startStageSpan(Span parentSpan, String stageName) {
        Span span = tracer.spanBuilder(stageName)
                .setParent(io.opentelemetry.context.Context.current().with(parentSpan))
                .startSpan();
        
        log.debug("Started stage span: {}", stageName);
        return span;
    }

    /**
     * Завершает span с успешным результатом
     * @param span span для завершения
     * @param vacanciesCount количество найденных вакансий
     */
    public void endSpanSuccess(Span span, int vacanciesCount) {
        span.setAttribute("vacancies.count", vacanciesCount);
        span.setStatus(StatusCode.OK);
        span.end();
        log.debug("Ended span successfully with {} vacancies", vacanciesCount);
    }

    /**
     * Завершает span с ошибкой
     * @param span span для завершения
     * @param error ошибка
     */
    public void endSpanError(Span span, Throwable error) {
        span.setStatus(StatusCode.ERROR, error.getMessage());
        span.recordException(error);
        span.end();
        log.error("Ended span with error: {}", error.getMessage());
    }

    /**
     * Выполняет операцию с трейсингом
     * @param operationName название операции
     * @param operation операция для выполнения
     * @param <T> тип результата
     * @return результат операции
     */
    public <T> T traceOperation(String operationName, java.util.function.Supplier<T> operation) {
        if (tracer == null) {
            return operation.get();
        }
        Span span = tracer.spanBuilder(operationName).startSpan();
        try (Scope scope = span.makeCurrent()) {
            T result = operation.get();
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Выполняет операцию без результата с трейсингом
     * @param operationName название операции
     * @param operation операция для выполнения
     */
    public void traceOperation(String operationName, Runnable operation) {
        if (tracer == null) {
            operation.run();
            return;
        }
        Span span = tracer.spanBuilder(operationName).startSpan();
        try (Scope scope = span.makeCurrent()) {
            operation.run();
            span.setStatus(StatusCode.OK);
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }
}
