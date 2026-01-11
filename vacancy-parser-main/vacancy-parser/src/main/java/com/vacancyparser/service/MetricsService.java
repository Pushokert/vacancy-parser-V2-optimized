package com.vacancyparser.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Gauge;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class MetricsService {

    private final MeterRegistry meterRegistry;
    
    // Счётчики для парсинга
    private Counter parsingSuccessCounter;
    private Counter parsingErrorCounter;
    private Counter vacanciesSavedCounter;
    
    // Таймеры для измерения времени выполнения
    private Timer parsingTimer;
    
    // Счётчик общего количества вакансий в БД
    private final AtomicLong totalVacanciesInDb = new AtomicLong(0);

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    @PostConstruct
    public void init() {
        // Инициализация счётчиков
        this.parsingSuccessCounter = Counter.builder("vacancy.parsing.success")
                .description("Количество успешных парсингов")
                .tag("status", "success")
                .register(meterRegistry);
        
        this.parsingErrorCounter = Counter.builder("vacancy.parsing.error")
                .description("Количество ошибочных парсингов")
                .tag("status", "error")
                .register(meterRegistry);
        
        this.vacanciesSavedCounter = Counter.builder("vacancy.saved")
                .description("Количество сохранённых вакансий в БД")
                .register(meterRegistry);
        
        // Инициализация таймера
        this.parsingTimer = Timer.builder("vacancy.parsing.duration")
                .description("Время выполнения парсинга")
                .register(meterRegistry);
        
        // Gauge для отслеживания общего количества вакансий в БД
        Gauge.builder("vacancy.database.total", totalVacanciesInDb, AtomicLong::get)
                .description("Общее количество вакансий в базе данных")
                .register(meterRegistry);
    }

    /**
     * Увеличивает счётчик успешных парсингов
     */
    public void incrementParsingSuccess() {
        parsingSuccessCounter.increment();
    }

    /**
     * Увеличивает счётчик ошибочных парсингов
     */
    public void incrementParsingError() {
        parsingErrorCounter.increment();
    }

    /**
     * Увеличивает счётчик сохранённых вакансий
     * @param count количество сохранённых вакансий
     */
    public void incrementVacanciesSaved(long count) {
        vacanciesSavedCounter.increment(count);
        totalVacanciesInDb.addAndGet(count);
    }

    /**
     * Обновляет общее количество вакансий в БД
     * @param count новое количество
     */
    public void updateTotalVacanciesInDb(long count) {
        totalVacanciesInDb.set(count);
    }

    /**
     * Возвращает таймер для измерения времени выполнения парсинга
     * @return Timer для измерения времени выполнения
     */
    public Timer getParsingTimer() {
        return parsingTimer;
    }

    /**
     * Измеряет время выполнения операции парсинга
     * @param runnable операция для измерения
     */
    public void recordParsingTime(Runnable runnable) {
        parsingTimer.record(runnable);
    }

    /**
     * Возвращает MeterRegistry для создания дополнительных метрик
     * @return MeterRegistry
     */
    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }

    /**
     * Создаёт таймер с тегами для конкретного источника
     * @param source источник парсинга
     * @return Timer с тегами
     */
    public Timer getParsingTimerForSource(String source) {
        return Timer.builder("vacancy.parsing.duration")
                .tag("source", source)
                .description("Время выполнения парсинга по источникам")
                .register(meterRegistry);
    }
}
