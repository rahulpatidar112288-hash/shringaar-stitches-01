package com.shringaar.store.repository;

import java.util.List;
import java.util.Optional;

import com.shringaar.store.domain.Category;
import com.shringaar.store.domain.Product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySlugAndActiveTrue(String slug);

    Optional<Product> findBySlug(String slug);

    List<Product> findByActiveTrueOrderByCreatedAtDesc();

    List<Product> findByActiveTrueAndFeaturedTrueOrderByCreatedAtDesc();

    List<Product> findByActiveTrueAndCategoryOrderByCreatedAtDesc(Category category);

    List<Product> findTop4ByActiveTrueAndCategoryAndIdNotOrderByCreatedAtDesc(Category category, Long id);

    @Query("""
           select p from Product p
           where p.active = true
             and (lower(p.name) like lower(concat('%', :q, '%'))
               or lower(p.detail) like lower(concat('%', :q, '%'))
               or lower(p.description) like lower(concat('%', :q, '%'))
               or lower(p.sku) like lower(concat('%', :q, '%')))
           order by p.createdAt desc
           """)
    List<Product> search(@Param("q") String query);
}
