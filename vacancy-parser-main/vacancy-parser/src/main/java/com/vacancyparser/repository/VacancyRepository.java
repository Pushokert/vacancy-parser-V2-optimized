package com.vacancyparser.repository;

import com.vacancyparser.model.Vacancy;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VacancyRepository extends JpaRepository<Vacancy, Long> {
    List<Vacancy> findBySource(String source);
    List<Vacancy> findByCity(String city);
    List<Vacancy> findByCompany(String company);
    
    @Query("SELECT v FROM Vacancy v WHERE v.publishedDate >= :fromDate")
    List<Vacancy> findRecentVacancies(LocalDateTime fromDate);
    
    @Query("SELECT DISTINCT v.city FROM Vacancy v")
    List<String> findAllCities();
    
    @Query("SELECT DISTINCT v.source FROM Vacancy v")
    List<String> findAllSources();
    
    // Оптимизированные запросы для фильтрации (избегаем N+1)
    @Query("SELECT v FROM Vacancy v WHERE " +
           "(:source IS NULL OR v.source = :source) AND " +
           "(:city IS NULL OR v.city = :city) AND " +
           "(:company IS NULL OR v.company LIKE %:company%)")
    List<Vacancy> findFiltered(
            @Param("source") String source,
            @Param("city") String city,
            @Param("company") String company
    );
    
    // Оптимизированные запросы для сортировки (выполняется на уровне БД)
    List<Vacancy> findAll(Sort sort);
    
    @Query("SELECT v FROM Vacancy v ORDER BY v.publishedDate ASC")
    List<Vacancy> findAllOrderByPublishedDateAsc();
    
    @Query("SELECT v FROM Vacancy v ORDER BY v.publishedDate DESC")
    List<Vacancy> findAllOrderByPublishedDateDesc();
    
    @Query("SELECT v FROM Vacancy v ORDER BY v.title ASC")
    List<Vacancy> findAllOrderByTitleAsc();
    
    @Query("SELECT v FROM Vacancy v ORDER BY v.title DESC")
    List<Vacancy> findAllOrderByTitleDesc();
    
    @Query("SELECT v FROM Vacancy v ORDER BY v.company ASC")
    List<Vacancy> findAllOrderByCompanyAsc();
    
    @Query("SELECT v FROM Vacancy v ORDER BY v.company DESC")
    List<Vacancy> findAllOrderByCompanyDesc();
    
    @Query("SELECT v FROM Vacancy v ORDER BY v.city ASC")
    List<Vacancy> findAllOrderByCityAsc();
    
    @Query("SELECT v FROM Vacancy v ORDER BY v.city DESC")
    List<Vacancy> findAllOrderByCityDesc();
}
