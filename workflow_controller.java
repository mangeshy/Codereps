package com.example.workflow.controller;

import com.example.workflow.model.ScheduleActionRequest;
import com.example.workflow.model.WorkflowCancellationRequest;
import com.example.workflow.model.ApiResponse;
import com.example.workflow.model.WorkflowDetailsResponse;
import io.temporal.client.ScheduleClient;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.workflow.v1.WorkflowExecutionInfo;
import io.temporal.api.workflowservice.v1.ListOpenWorkflowExecutionsRequest;
import io.temporal.api.workflowservice.v1.ListOpenWorkflowExecutionsResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController {

    private final WorkflowClient workflowClient;
    private final ScheduleClient scheduleClient;

    public WorkflowController(WorkflowClient workflowClient, ScheduleClient scheduleClient) {
        this.workflowClient = workflowClient;
        this.scheduleClient = scheduleClient;
    }

    @PostMapping("/schedules/{scheduleId}/action")
    public ResponseEntity<ApiResponse<String>> handleScheduleAction(
            @PathVariable String scheduleId,
            @RequestBody ScheduleActionRequest request) {
        
        try {
            var scheduleHandle = scheduleClient.getHandle(scheduleId);
            
            switch (request.getAction().toUpperCase()) {
                case "CANCEL":
                    scheduleHandle.delete();
                    return ResponseEntity.ok(
                        ApiResponse.success("Schedule cancelled successfully", scheduleId)
                    );
                case "PAUSE":
                    scheduleHandle.pause(request.getReason());
                    return ResponseEntity.ok(
                        ApiResponse.success("Schedule paused successfully", scheduleId)
                    );
                case "UNPAUSE":
                    scheduleHandle.unpause(request.getReason());
                    return ResponseEntity.ok(
                        ApiResponse.success("Schedule unpaused successfully", scheduleId)
                    );
                default:
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error("Invalid action. Supported: CANCEL, PAUSE, UNPAUSE")
                    );
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error("Failed to perform action: " + e.getMessage())
            );
        }
    }

    @DeleteMapping("/workflows/{workflowId}")
    public ResponseEntity<ApiResponse<String>> cancelWorkflow(
            @PathVariable String workflowId,
            @RequestBody(required = false) WorkflowCancellationRequest request) {
        
        try {
            String runId = request != null ? request.getRunId() : null;
            WorkflowStub workflowStub;
            
            if (runId != null && !runId.isEmpty()) {
                workflowStub = workflowClient.newUntypedWorkflowStub(workflowId, runId);
            } else {
                workflowStub = workflowClient.newUntypedWorkflowStub(workflowId);
            }
            
            String reason = request != null ? request.getReason() : "Cancelled via API";
            workflowStub.cancel();
            
            return ResponseEntity.ok(
                ApiResponse.success("Workflow cancelled successfully", workflowId)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error("Failed to cancel workflow: " + e.getMessage())
            );
        }
    }

    @GetMapping("/workflows/running")
    public ResponseEntity<ApiResponse<List<WorkflowDetailsResponse>>> getRunningWorkflows(
            @RequestParam(defaultValue = "100") int maxResults) {
        
        try {
            var service = workflowClient.getWorkflowServiceStubs().blockingStub();
            
            ListOpenWorkflowExecutionsRequest listRequest = 
                ListOpenWorkflowExecutionsRequest.newBuilder()
                    .setNamespace(workflowClient.getOptions().getNamespace())
                    .setMaximumPageSize(maxResults)
                    .build();
            
            ListOpenWorkflowExecutionsResponse response = 
                service.listOpenWorkflowExecutions(listRequest);
            
            List<WorkflowDetailsResponse> workflows = response.getExecutionsList().stream()
                .map(this::mapToWorkflowDetails)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(
                ApiResponse.success("Retrieved running workflows", workflows)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error("Failed to retrieve workflows: " + e.getMessage())
            );
        }
    }

    private WorkflowDetailsResponse mapToWorkflowDetails(WorkflowExecutionInfo info) {
        WorkflowDetailsResponse details = new WorkflowDetailsResponse();
        details.setWorkflowId(info.getExecution().getWorkflowId());
        details.setRunId(info.getExecution().getRunId());
        details.setWorkflowType(info.getType().getName());
        details.setStartTime(info.getStartTime().getSeconds());
        details.setStatus(info.getStatus().name());
        
        // Note: Activity details would require additional API calls per workflow
        // This is a simplified version
        details.setActivityCount(0);
        
        return details;
    }
}