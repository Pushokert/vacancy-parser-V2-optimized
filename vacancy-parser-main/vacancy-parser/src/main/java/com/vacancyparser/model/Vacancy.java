package com.vacancyparser.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "vacancies", indexes = {
    @Index(name = "idx_source", columnList = "source"),
    @Index(name = "idx_city", columnList = "city"),
    @Index(name = "idx_company", columnList = "company"),
    @Index(name = "idx_published_date", columnList = "publishedDate"),
    @Index(name = "idx_source_url", columnList = "sourceUrl", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Vacancy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String company;

    @Column(length = 1000)
    private String salary;

    @Column(length = 5000)
    private String requirements;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private LocalDateTime publishedDate;

    @Column(nullable = false)
    private String sourceUrl;

    @Column(nullable = false)
    private String source; // hh, superjob, habr

    @Column(nullable = false)
    private LocalDateTime parsedAt;

    @PrePersist
    protected void onCreate() {
        parsedAt = LocalDateTime.now();
    }
}
