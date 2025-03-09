package com.impansini.todo.rest;

import com.impansini.todo.domain.Task;
import com.impansini.todo.kafka.KafkaProducer;
import com.impansini.todo.repository.TaskRepository;
import com.impansini.todo.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TaskResourceTest {

    @Mock
    private TaskService taskService;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private KafkaProducer kafkaProducer;

    @InjectMocks
    private TaskResource taskResource;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createTask() throws URISyntaxException {
        Task task = new Task();
        task.setId(null);
        when(taskService.save(any(Task.class))).thenReturn(task);

        ResponseEntity<Task> response = taskResource.createTask(task);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(kafkaProducer, times(1)).sendMessage(anyString());
    }

    @Test
    void createTaskWithExistingId() {
        Task task = new Task();
        task.setId(1L);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            taskResource.createTask(task);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void updateTask() {
        Task task = new Task();
        task.setId(1L);
        when(taskRepository.existsById(task.getId())).thenReturn(true);
        when(taskService.save(any(Task.class))).thenReturn(task);

        ResponseEntity<Task> response = taskResource.updateTask(task);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(task, response.getBody());
    }

    @Test
    void updateTaskWithNoId() {
        Task task = new Task();
        task.setId(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            taskResource.updateTask(task);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void updateTaskNotFound() {
        Task task = new Task();
        task.setId(1L);
        when(taskRepository.existsById(task.getId())).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            taskResource.updateTask(task);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void getAllTasks() {
        List<Task> tasks = new ArrayList<>();
        when(taskService.findAll()).thenReturn(tasks);

        ResponseEntity<List<Task>> response = taskResource.getAllTasks();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(tasks, response.getBody());
    }

    @Test
    void findOneTask() {
        Task task = new Task();
        task.setId(1L);
        when(taskService.findOne(task.getId())).thenReturn(Optional.of(task));

        ResponseEntity<Optional<Task>> response = taskResource.findOneTask(task.getId());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isPresent());
        assertEquals(task, response.getBody().get());
    }

    @Test
    void findOneTaskNotFound() {
        when(taskService.findOne(1L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            taskResource.findOneTask(1L);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void deleteTask() {
        doNothing().when(taskService).delete(1L);

        ResponseEntity<Void> response = taskResource.deleteTask(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(taskService, times(1)).delete(1L);
    }
}