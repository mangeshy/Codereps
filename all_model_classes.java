// ==================== ScheduleActionRequest.java ====================
package com.example.workflow.model;

import jakarta.validation.constraints.NotBlank;

public class ScheduleActionRequest {
    
    @NotBlank(message = "Action is required")
    private String action;
    
    private String reason;

    public ScheduleActionRequest() {
    }

    public ScheduleActionRequest(String action, String reason) {
        this.action = action;
        this.reason = reason;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

// ==================== WorkflowCancellationRequest.java ====================
package com.example.workflow.model;

public class WorkflowCancellationRequest {
    
    private String runId;
    private String reason;

    public WorkflowCancellationRequest() {
    }

    public WorkflowCancellationRequest(String runId, String reason) {
        this.runId = runId;
        this.reason = reason;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

// ==================== ApiResponse.java ====================
package com.example.workflow.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    private boolean success;
    private String message;
    private T data;
    private String error;

    public ApiResponse() {
    }

    public ApiResponse(boolean success, String message, T data, String error) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.error = error;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    public static <T> ApiResponse<T> error(String error) {
        return new ApiResponse<>(false, null, null, error);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}

// ==================== WorkflowDetailsResponse.java ====================
package com.example.workflow.model;

import java.util.ArrayList;
import java.util.List;

public class WorkflowDetailsResponse {
    
    private String workflowId;
    private String runId;
    private String workflowType;
    private long startTime;
    private String status;
    private int activityCount;
    private List<ActivityInfo> activitiesInProgress;

    public WorkflowDetailsResponse() {
        this.activitiesInProgress = new ArrayList<>();
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getWorkflowType() {
        return workflowType;
    }

    public void setWorkflowType(String workflowType) {
        this.workflowType = workflowType;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getActivityCount() {
        return activityCount;
    }

    public void setActivityCount(int activityCount) {
        this.activityCount = activityCount;
    }

    public List<ActivityInfo> getActivitiesInProgress() {
        return activitiesInProgress;
    }

    public void setActivitiesInProgress(List<ActivityInfo> activitiesInProgress) {
        this.activitiesInProgress = activitiesInProgress;
    }

    // Inner class for Activity Information
    public static class ActivityInfo {
        private String activityId;
        private String activityType;
        private long startedTime;

        public ActivityInfo() {
        }

        public ActivityInfo(String activityId, String activityType, long startedTime) {
            this.activityId = activityId;
            this.activityType = activityType;
            this.startedTime = startedTime;
        }

        public String getActivityId() {
            return activityId;
        }

        public void setActivityId(String activityId) {
            this.activityId = activityId;
        }

        public String getActivityType() {
            return activityType;
        }

        public void setActivityType(String activityType) {
            this.activityType = activityType;
        }

        public long getStartedTime() {
            return startedTime;
        }

        public void setStartedTime(long startedTime) {
            this.startedTime = startedTime;
        }
    }
}