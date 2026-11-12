package com.MessLoc.repository;

import com.MessLoc.entity.Menu;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MenuRepository extends MongoRepository<Menu, String> {

    Optional<Menu> findByMessIdAndMenuDate(String messId, LocalDate menuDate);

    List<Menu> findByMessIdAndMenuDateBetweenOrderByMenuDateAsc(String messId, LocalDate startDate, LocalDate endDate);

    List<Menu> findByMessId(String messId);

    void deleteByMessId(String messId);
}

