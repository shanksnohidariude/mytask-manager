package com.example.demo.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.Task;
import com.example.demo.repository.TaskRepository;

@Service
public class TaskServiceImpl implements TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Override
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    @Override
    public List<Task> getTasksByDate(LocalDate date) {
        return taskRepository.findByDueDate(date);
    }

    @Override
    public List<Task> getTasksOrderByDueDate() {
        return taskRepository.findAllByOrderByDueDateAsc();
    }
    
    @Override
    public Optional<Task> findById(Long id) {
        return taskRepository.findById(id);
    }

    
    public Task getTaskById(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid task Id:" + id));
    }
    

    @Override
    public void deleteById(Long id) {
        taskRepository.deleteById(id);
    }


    @Override
    public void saveTask(Task task) {
        taskRepository.save(task);
    }
    
    @Override
    public Map<LocalDate, List<Task>> getTaskCountByMonth(Integer year, Integer month) {

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);

        List<Task> tasks = taskRepository.findByDueDateBetween(start, end);

        // 日付ごとに Task の List をまとめる
        return tasks.stream()
                .collect(Collectors.groupingBy(Task::getDueDate));
    }

}
