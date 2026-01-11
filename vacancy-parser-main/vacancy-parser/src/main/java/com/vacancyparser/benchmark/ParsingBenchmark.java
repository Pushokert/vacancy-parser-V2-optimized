package com.vacancyparser.benchmark;

import com.vacancyparser.model.Vacancy;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * JMH бенчмарк для сравнения производительности разных реализаций парсинга
 * Сравнивает: обычный for, Stream API, parallelStream
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class ParsingBenchmark {

    private List<Vacancy> vacancies;

    @Setup
    public void setup() {
        // Создаём тестовые данные
        vacancies = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            Vacancy vacancy = new Vacancy();
            vacancy.setId((long) i);
            vacancy.setTitle("Vacancy " + i);
            vacancy.setCompany("Company " + (i % 100));
            vacancy.setCity("City " + (i % 50));
            vacancy.setSource("hh");
            vacancy.setSourceUrl("https://hh.ru/vacancy/" + i);
            vacancies.add(vacancy);
        }
    }

    /**
     * Бенчмарк для обычного цикла for
     */
    @Benchmark
    public List<String> filterWithForLoop() {
        List<String> result = new ArrayList<>();
        for (Vacancy vacancy : vacancies) {
            if (vacancy.getCity().contains("City 1")) {
                result.add(vacancy.getTitle());
            }
        }
        return result;
    }

    /**
     * Бенчмарк для Stream API
     */
    @Benchmark
    public List<String> filterWithStream() {
        return vacancies.stream()
                .filter(v -> v.getCity().contains("City 1"))
                .map(Vacancy::getTitle)
                .collect(Collectors.toList());
    }

    /**
     * Бенчмарк для parallelStream
     */
    @Benchmark
    public List<String> filterWithParallelStream() {
        return vacancies.parallelStream()
                .filter(v -> v.getCity().contains("City 1"))
                .map(Vacancy::getTitle)
                .collect(Collectors.toList());
    }

    /**
     * Бенчмарк для сортировки с обычным циклом
     */
    @Benchmark
    public List<Vacancy> sortWithForLoop() {
        List<Vacancy> result = new ArrayList<>(vacancies);
        // Простая пузырьковая сортировка (O(n^2))
        for (int i = 0; i < result.size() - 1; i++) {
            for (int j = 0; j < result.size() - i - 1; j++) {
                if (result.get(j).getTitle().compareTo(result.get(j + 1).getTitle()) > 0) {
                    Vacancy temp = result.get(j);
                    result.set(j, result.get(j + 1));
                    result.set(j + 1, temp);
                }
            }
        }
        return result;
    }

    /**
     * Бенчмарк для сортировки с Stream API (использует TimSort - O(n log n))
     */
    @Benchmark
    public List<Vacancy> sortWithStream() {
        return vacancies.stream()
                .sorted((v1, v2) -> v1.getTitle().compareTo(v2.getTitle()))
                .collect(Collectors.toList());
    }

    /**
     * Бенчмарк для сортировки с parallelStream
     */
    @Benchmark
    public List<Vacancy> sortWithParallelStream() {
        return vacancies.parallelStream()
                .sorted((v1, v2) -> v1.getTitle().compareTo(v2.getTitle()))
                .collect(Collectors.toList());
    }

    /**
     * Бенчмарк для группировки с обычным циклом
     */
    @Benchmark
    public int groupWithForLoop() {
        int count = 0;
        for (Vacancy vacancy : vacancies) {
            if (vacancy.getSource().equals("hh")) {
                count++;
            }
        }
        return count;
    }

    /**
     * Бенчмарк для группировки с Stream API
     */
    @Benchmark
    public long groupWithStream() {
        return vacancies.stream()
                .filter(v -> v.getSource().equals("hh"))
                .count();
    }

    /**
     * Бенчмарк для группировки с parallelStream
     */
    @Benchmark
    public long groupWithParallelStream() {
        return vacancies.parallelStream()
                .filter(v -> v.getSource().equals("hh"))
                .count();
    }

    /**
     * Запуск бенчмарков
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(ParsingBenchmark.class.getSimpleName())
                .result("jmh-results.txt")  // Сохранение результатов в файл
                .resultFormat(org.openjdk.jmh.results.format.ResultFormatType.TEXT)  // Формат вывода
                .build();

        new Runner(opt).run();
    }
}
