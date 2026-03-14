package com.hkbuyer.api;

import com.hkbuyer.api.dto.AcceptTaskRequest;
import com.hkbuyer.api.dto.SubmitProofRequest;
import com.hkbuyer.service.TaskService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/v1/buyer/tasks")
public class BuyerTaskController {

    private final TaskService taskService;

    public BuyerTaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<Map<String, Object>> listPublishedTasks(@RequestParam(value = "buyerId", required = false) Long buyerId) {
        return taskService.listPublishedTasks(buyerId);
    }

    @PostMapping("/{taskId}/accept")
    public Map<String, Object> acceptTask(@PathVariable("taskId") Long taskId,
                                          @Valid @RequestBody AcceptTaskRequest request) {
        return taskService.acceptTask(taskId, request.getBuyerId());
    }

    @PostMapping("/{taskId}/proofs")
    public Map<String, Object> submitProof(@PathVariable("taskId") Long taskId,
                                           @Valid @RequestBody SubmitProofRequest request) {
        return taskService.submitProof(taskId, request);
    }
}
