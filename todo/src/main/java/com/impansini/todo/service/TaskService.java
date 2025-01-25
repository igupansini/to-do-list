package com.impansini.todo.service;

import com.impansini.todo.domain.Task;
import com.impansini.todo.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TaskService {

    private final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional
    public Task save(Task task) {
        log.debug("Request to save a task: {}", task);
        return taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public List<Task> findAll() {
        log.debug("Request to get all tasks");
        return taskRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Task> findOne(Long id) {
        log.debug("Request to get a task: {}", id);
        return taskRepository.findById(id);
    }

    @Transactional
    public void delete(Long id) {
        taskRepository
                .findById(id)
                .ifPresent(task -> {
                    taskRepository.delete(task);
                    log.debug("Deleted task: {}", task);
                });
    }
}
