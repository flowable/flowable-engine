package org.flowable.task.service.impl.util;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.task.api.TaskInfo;
import org.flowable.variable.api.types.ValueFields;

public class TaskVariableUtils {

    public static boolean isCaseRelated(ValueFields valueField) {
        return isCaseRelated(valueField.getScopeId(), valueField.getScopeType());
    }

    public static boolean isCaseRelated(TaskInfo task) {
        return isCaseRelated(task.getScopeId(), task.getScopeType());
    }

    private static boolean isCaseRelated(String scopeId, String scopeType) {
        return scopeId != null && ScopeTypes.CMMN.equals(scopeType);
    }

    public static boolean isProcessRelated(ValueFields valueField) {
        return isCaseRelated(valueField.getScopeId(), valueField.getScopeType());
    }

    public static boolean isProcessRelated(TaskInfo task) {
        return task.getProcessInstanceId() != null;
    }

    private static boolean isProcessRelated(String scopeId) {
        return scopeId != null;
    }

    public static boolean isProcessRelatedAndProcessIdEquals(TaskInfo task, String processInstanceId) {
        return isProcessRelated(task) && task.getProcessInstanceId()
                .equals(processInstanceId);
    }

    public static boolean isCaseRelatedAndScopeIdEquals(TaskInfo task, String scopeId) {
        return isCaseRelated(task) && task.getScopeId()
                .equals(scopeId);
    }
}
