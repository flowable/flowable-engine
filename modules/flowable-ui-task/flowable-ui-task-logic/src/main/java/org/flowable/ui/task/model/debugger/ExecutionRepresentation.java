package org.flowable.ui.task.model.debugger;

/**
 * @author martin.grofcik
 */
public class ExecutionRepresentation {
    
    protected String id;
    protected String parentId;
    protected String processInstanceId;
    protected String superExecutionId;
    protected String activityId;
    protected Boolean suspended;
    protected String tenantId;

    public ExecutionRepresentation(String id, String parentId, String processInstanceId, String superExecutionId, 
                                   String activityId, boolean suspended, String tenantId) {
        this.id = id;
        this.parentId = parentId;
        this.processInstanceId = processInstanceId;
        this.superExecutionId = superExecutionId;
        this.activityId = activityId;
        this.suspended = suspended;
        this.tenantId = tenantId;
    }

    public String getId() {
        return id;
    }

    public String getParentId() {
        return parentId;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public String getSuperExecutionId() {
        return superExecutionId;
    }

    public String getActivityId() {
        return activityId;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public String getTenantId() {
        return tenantId;
    }
}
