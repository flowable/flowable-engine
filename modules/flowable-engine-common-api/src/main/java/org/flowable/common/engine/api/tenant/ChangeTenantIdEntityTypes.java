package org.flowable.common.engine.api.tenant;

public interface ChangeTenantIdEntityTypes {

    String ACTIVITY_INSTANCES = "ActivityInstances";
    String EXECUTIONS = "Executions";
    String CASE_INSTANCES = "CaseInstances";
    String PLAN_ITEM_INSTANCES = "PlanItemInstances";
    String MILESTONE_INSTANCES = "MilestoneInstances";
    String EVENT_SUBSCRIPTIONS = "EventSubscriptions";
    String TASKS = "Tasks";
    String FORM_INSTANCES = "FormInstances";
    String EXTERNAL_WORKER_JOBS = "ExternalWorkerJobs";
    String CONTENT_ITEM_INSTANCES = "ContentItemInstances";
    String HISTORIC_ACTIVITY_INSTANCES = "HistoricActivityInstances";
    String HISTORIC_CASE_INSTANCES = "HistoricCaseInstances";
    String HISTORIC_DECISION_EXECUTIONS = "HistoricDecisionExecutions";
    String HISTORIC_MILESTONE_INSTANCES = "HistoricMilestoneInstances";
    String HISTORIC_PLAN_ITEM_INSTANCES = "HistoricPlanItemInstances";
    String HISTORIC_PROCESS_INSTANCES = "HistoricProcessInstances";
    String HISTORIC_TASK_LOG_ENTRIES = "HistoricTaskLogEntries";
    String HISTORIC_TASK_INSTANCES = "HistoricTaskInstances";
    String HISTORY_JOBS = "HistoryJobs";
    String JOBS = "Jobs";
    String SUSPENDED_JOBS = "SuspendedJobs";
    String TIMER_JOBS = "TimerJobs";
    String DEADLETTER_JOBS = "DeadLetterJobs";
    
}