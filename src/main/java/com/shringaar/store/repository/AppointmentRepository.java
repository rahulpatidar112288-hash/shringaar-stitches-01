package com.shringaar.store.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.shringaar.store.domain.Appointment;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findAllByOrderByDateDescTimeDesc();
    boolean existsByDateAndTime(LocalDate date, LocalTime time);
    List<Appointment> findByDateBetween(LocalDate from, LocalDate to);
}
