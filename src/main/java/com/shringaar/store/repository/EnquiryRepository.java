package com.shringaar.store.repository;

import java.util.List;

import com.shringaar.store.domain.Enquiry;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EnquiryRepository extends JpaRepository<Enquiry, Long> {
    List<Enquiry> findTop200ByOrderByCreatedAtDesc();
}
