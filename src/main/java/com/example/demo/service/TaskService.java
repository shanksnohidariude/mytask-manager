package com.example.demo.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.example.demo.model.Task;

public interface TaskService {
    List<Task> getAllTasks();
    List<Task> getTasksByDate(LocalDate date);
    List<Task> getTasksOrderByDueDate();
    Map<LocalDate, List<Task>> getTaskCountByMonth(Integer year,Integer month);
    public Optional<Task> findById(Long id);
    Task getTaskById(Long id);
    void saveTask(Task task);
    void deleteById(Long id);
    
}


