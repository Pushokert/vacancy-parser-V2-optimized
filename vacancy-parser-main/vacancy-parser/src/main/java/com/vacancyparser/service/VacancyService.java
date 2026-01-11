package com.vacancyparser.service;

import com.vacancyparser.model.Vacancy;
import com.vacancyparser.parser.VacancyParser;
import com.vacancyparser.repository.VacancyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;
import io.micrometer.core.instrument.Timer;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;

@Service
@RequiredArgsConstructor
@Slf4j
public class VacancyService {

    private final VacancyRepository vacancyRepository;
    private final VacancyParser vacancyParser;
    private final LoggingService loggingService;
    private final MetricsService metricsService;
    
    @Value("${parser.thread.pool.size:10}")
    private int threadPoolSize;
    
    private ExecutorService executorService;
    private final Set<String> processedUrls = ConcurrentHashMap.newKeySet();
    private final BlockingQueue<Vacancy> vacancyQueue = new LinkedBlockingQueue<>();
    
    @PostConstruct
    public void init() {
        executorService = Executors.newFixedThreadPool(threadPoolSize);
    }

    @Transactional
    public void parseVacancies(List<String> urls, Integer maxPages) {
        log.info("Starting parsing for {} URLs with max {} pages", urls.size(), maxPages);
        
        // Измеряем общее время выполнения парсинга
        metricsService.recordParsingTime(() -> {
            List<Future<List<Vacancy>>> futures = new ArrayList<>();
            
            for (String url : urls) {
                Future<List<Vacancy>> future = executorService.submit(() -> {
                    String source = vacancyParser.detectSource(url);
                    List<Vacancy> vacancies = new ArrayList<>();
                    
                    try {
                        // Измеряем время парсинга для каждого источника
                        Timer sourceTimer = metricsService.getParsingTimerForSource(source);
                        
                        switch (source) {
                            case "hh":
                                vacancies = sourceTimer.recordCallable(() -> vacancyParser.parseHhRu(url));
                                break;
                            case "superjob":
                                vacancies = sourceTimer.recordCallable(() -> vacancyParser.parseSuperJob(url));
                                break;
                            case "habr":
                                vacancies = sourceTimer.recordCallable(() -> vacancyParser.parseHabrCareer(url));
                                break;
                            default:
                                log.warn("Unknown source for URL: {}", url);
                        }
                        
                        // Filter duplicates and save
                        List<Vacancy> newVacancies = vacancies.stream()
                                .filter(v -> !processedUrls.contains(v.getSourceUrl()))
                                .collect(Collectors.toList());
                        
                        if (!newVacancies.isEmpty()) {
                            vacancyRepository.saveAll(newVacancies);
                            newVacancies.forEach(v -> processedUrls.add(v.getSourceUrl()));
                            vacancyQueue.addAll(newVacancies);
                            
                            // Обновляем метрики
                            metricsService.incrementVacanciesSaved(newVacancies.size());
                            metricsService.incrementParsingSuccess();
                            
                            log.info("Saved {} new vacancies from {}", newVacancies.size(), url);
                            loggingService.log(String.format("Saved %d new vacancies from %s", newVacancies.size(), url));
                        } else {
                            log.warn("No new vacancies found from {} (found {} total, but all duplicates)", url, vacancies.size());
                            metricsService.incrementParsingSuccess(); // Успешный парсинг, но без новых вакансий
                        }
                        
                    } catch (Exception e) {
                        metricsService.incrementParsingError();
                        log.error("Error parsing URL {}: {}", url, e.getMessage(), e);
                    }
                    
                    return vacancies;
                });
                
                futures.add(future);
            }
            
            // Wait for all tasks to complete
            for (Future<List<Vacancy>> future : futures) {
                try {
                    future.get(30, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.error("Error waiting for parsing task: {}", e.getMessage());
                }
            }
        });
        
        // Обновляем общее количество вакансий в БД
        long totalCount = vacancyRepository.count();
        metricsService.updateTotalVacanciesInDb(totalCount);
        
        log.info("Parsing completed");
    }

    @Transactional(readOnly = true)
    public List<Vacancy> getAllVacancies() {
        return vacancyRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Vacancy> getVacanciesBySource(String source) {
        return vacancyRepository.findBySource(source);
    }

    @Transactional(readOnly = true)
    public List<Vacancy> getVacanciesByCity(String city) {
        return vacancyRepository.findByCity(city);
    }

    @Transactional(readOnly = true)
    public List<Vacancy> getVacanciesSorted(String sortBy, String order) {
        // Используем оптимизированные запросы БД вместо загрузки всех данных в память
        boolean isDesc = "desc".equalsIgnoreCase(order);
        
        return switch (sortBy.toLowerCase()) {
            case "date" -> isDesc 
                ? vacancyRepository.findAllOrderByPublishedDateDesc()
                : vacancyRepository.findAllOrderByPublishedDateAsc();
            case "title" -> isDesc
                ? vacancyRepository.findAllOrderByTitleDesc()
                : vacancyRepository.findAllOrderByTitleAsc();
            case "company" -> isDesc
                ? vacancyRepository.findAllOrderByCompanyDesc()
                : vacancyRepository.findAllOrderByCompanyAsc();
            case "city" -> isDesc
                ? vacancyRepository.findAllOrderByCityDesc()
                : vacancyRepository.findAllOrderByCityAsc();
            default -> {
                Sort sort = isDesc 
                    ? Sort.by(Sort.Direction.DESC, "id")
                    : Sort.by(Sort.Direction.ASC, "id");
                yield vacancyRepository.findAll(sort);
            }
        };
    }

    @Transactional(readOnly = true)
    public List<Vacancy> getVacanciesFiltered(String source, String city, String company) {
        // Используем оптимизированный запрос БД вместо загрузки всех данных в память
        return vacancyRepository.findFiltered(source, city, company);
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
