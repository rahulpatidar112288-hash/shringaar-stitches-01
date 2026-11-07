package com.shringaar.store.repository;

import java.util.List;
import java.util.Optional;

import com.shringaar.store.domain.Category;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findBySlug(String slug);
    List<Category> findAllByOrderBySortOrderAscNameAsc();
}
