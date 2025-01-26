package com.impansini.todo.rest;

import com.impansini.todo.domain.Task;
import com.impansini.todo.kafka.KafkaProducer;
import com.impansini.todo.repository.TaskRepository;
import com.impansini.todo.service.TaskService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class TaskResource {

    private final Logger log = LoggerFactory.getLogger(TaskResource.class);

    private final TaskService taskService;

    private final TaskRepository taskRepository;

    private final KafkaProducer kafkaProducer;

    public TaskResource(TaskService taskService, TaskRepository taskRepository, KafkaProducer kafkaProducer) {
        this.taskService = taskService;
        this.taskRepository = taskRepository;
        this.kafkaProducer = kafkaProducer;
    }

    @PostMapping("/tasks")
    public ResponseEntity<Task> createTask(@Valid @RequestBody Task task) throws URISyntaxException {
        log.debug("REST request to save a task: {}", task);
        if (task.getId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A new task cannot already have an ID");
        }
        Task result = taskService.save(task);
        kafkaProducer.sendMessage(result.toString());
        return ResponseEntity
                .created(new URI("/api/tasks/" + result.getId()))
                .body(result);
    }

    @PutMapping("/tasks")
    public ResponseEntity<Task> updateTask(@Valid @RequestBody Task task) {
        log.debug("REST request to update a task: {}", task);
        if (task.getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid id");
        }
        if (!taskRepository.existsById(task.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
        }

        Task updatedTask = taskService.save(task);
        return ResponseEntity
                .ok()
                .body(updatedTask);
    }

    @GetMapping("/tasks")
    public ResponseEntity<List<Task>> getAllTasks() {
        log.debug("REST request to get all tasks");
        List<Task> tasks = taskService.findAll();
        return ResponseEntity.ok().body(tasks);
    }

    @GetMapping("/tasks/{id}")
    public ResponseEntity<Optional<Task>> findOneTask(@PathVariable Long id) {
        log.debug("REST request to get a task: {}", id);

        Optional<Task> task = taskService.findOne(id);

        if (task.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
        }

        return ResponseEntity.ok().body(task);
    }

    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        log.debug("REST request to delete a task: {}", id);
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
