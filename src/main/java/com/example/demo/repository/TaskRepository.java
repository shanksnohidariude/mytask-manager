package com.example.demo.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByDueDate(LocalDate date);
    List<Task> findAllByOrderByDueDateAsc();
    List<Task> findByDueDateBetween(LocalDate start, LocalDate end);
}
