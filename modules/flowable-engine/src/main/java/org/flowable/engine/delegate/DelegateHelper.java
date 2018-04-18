/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.engine.delegate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FieldExtension;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowableListener;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.TaskWithFieldExtensions;
import org.flowable.bpmn.model.UserTask;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.engine.impl.delegate.ActivityBehavior;
import org.flowable.engine.impl.el.FixedValue;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.task.service.delegate.DelegateTask;

/**
 * Class that provides helper operations for use in the {@link JavaDelegate}, {@link ActivityBehavior}, {@link ExecutionListener} and {@link TaskListener} interfaces.
 * 
 * @author Joram Barrez
 */
public class DelegateHelper {

    /**
     * To be used in an {@link ActivityBehavior} or {@link JavaDelegate}: leaves according to the default BPMN 2.0 rules: all sequenceflow with a condition that evaluates to true are followed.
     */
    public static void leaveDelegate(DelegateExecution delegateExecution) {
        CommandContextUtil.getAgenda().planTakeOutgoingSequenceFlowsOperation((ExecutionEntity) delegateExecution, true);
    }

    /**
     * To be used in an {@link ActivityBehavior} or {@link JavaDelegate}: leaves the current activity via one specific sequenceflow.
     */
    public static void leaveDelegate(DelegateExecution delegateExecution, String sequenceFlowId) {
        String processDefinitionId = delegateExecution.getProcessDefinitionId();
        org.flowable.bpmn.model.Process process = ProcessDefinitionUtil.getProcess(processDefinitionId);
        FlowElement flowElement = process.getFlowElement(sequenceFlowId);
        if (flowElement instanceof SequenceFlow) {
            delegateExecution.setCurrentFlowElement(flowElement);
            CommandContextUtil.getAgenda().planTakeOutgoingSequenceFlowsOperation((ExecutionEntity) delegateExecution, false);
        } else {
            throw new FlowableException(sequenceFlowId + " does not match a sequence flow");
        }
    }

    /**
     * Returns the {@link BpmnModel} matching the process definition bpmn model for the process definition of the passed {@link DelegateExecution}.
     */
    public static BpmnModel getBpmnModel(DelegateExecution execution) {
        if (execution == null) {
            throw new FlowableException("Null execution passed");
        }
        return ProcessDefinitionUtil.getBpmnModel(execution.getProcessDefinitionId());
    }

    /**
     * Returns the current {@link FlowElement} where the {@link DelegateExecution} is currently at.
     */
    public static FlowElement getFlowElement(DelegateExecution execution) {
        BpmnModel bpmnModel = getBpmnModel(execution);
        FlowElement flowElement = bpmnModel.getFlowElement(execution.getCurrentActivityId());
        if (flowElement == null) {
            throw new FlowableException("Could not find a FlowElement for activityId " + execution.getCurrentActivityId());
        }
        return flowElement;
    }

    /**
     * Returns whether or not the provided execution is being use for executing an {@link ExecutionListener}.
     */
    public static boolean isExecutingExecutionListener(DelegateExecution execution) {
        return execution.getCurrentFlowableListener() != null;
    }

    /**
     * Returns for the activityId of the passed {@link DelegateExecution} the {@link Map} of {@link ExtensionElement} instances. These represent the extension elements defined in the BPMN 2.0 XML as
     * part of that particular activity.
     * 
     * If the execution is currently being used for executing an {@link ExecutionListener}, the extension elements of the listener will be used. Use the
     * {@link #getFlowElementExtensionElements(DelegateExecution)} or {@link #getListenerExtensionElements(DelegateExecution)} instead to specifically get the extension elements of either the flow
     * element or the listener.
     */
    public static Map<String, List<ExtensionElement>> getExtensionElements(DelegateExecution execution) {
        if (isExecutingExecutionListener(execution)) {
            return getListenerExtensionElements(execution);
        } else {
            return getFlowElementExtensionElements(execution);
        }
    }

    public static Map<String, List<ExtensionElement>> getFlowElementExtensionElements(DelegateExecution execution) {
        return getFlowElement(execution).getExtensionElements();
    }

    public static Map<String, List<ExtensionElement>> getListenerExtensionElements(DelegateExecution execution) {
        return execution.getCurrentFlowableListener().getExtensionElements();
    }

    /**
     * Returns the list of field extensions, represented as instances of {@link FieldExtension}, for the current activity of the passed {@link DelegateExecution}.
     * 
     * If the execution is currently being used for executing an {@link ExecutionListener}, the fields of the listener will be returned. Use {@link #getFlowElementFields(DelegateExecution)} or
     * {@link #getListenerFields(DelegateExecution)} if needing the flow element of listener fields specifically.
     */
    public static List<FieldExtension> getFields(DelegateExecution execution) {
        if (isExecutingExecutionListener(execution)) {
            return getListenerFields(execution);
        } else {
            return getFlowElementFields(execution);
        }
    }

    public static List<FieldExtension> getFlowElementFields(DelegateExecution execution) {
        FlowElement flowElement = getFlowElement(execution);
        if (flowElement instanceof TaskWithFieldExtensions) {
            return ((TaskWithFieldExtensions) flowElement).getFieldExtensions();
        }
        return new ArrayList<>();
    }

    public static List<FieldExtension> getListenerFields(DelegateExecution execution) {
        return execution.getCurrentFlowableListener().getFieldExtensions();
    }

    /**
     * Returns the {@link FieldExtension} matching the provided 'fieldName' which is defined for the current activity of the provided {@link DelegateExecution}.
     * 
     * Returns null if no such {@link FieldExtension} can be found.
     * 
     * If the execution is currently being used for executing an {@link ExecutionListener}, the field of the listener will be returned. Use {@link #getFlowElementField(DelegateExecution, String)} or
     * {@link #getListenerField(DelegateExecution, String)} for specifically getting the field from either the flow element or the listener.
     */
    public static FieldExtension getField(DelegateExecution execution, String fieldName) {
        if (isExecutingExecutionListener(execution)) {
            return getListenerField(execution, fieldName);
        } else {
            return getFlowElementField(execution, fieldName);
        }
    }

    public static FieldExtension getFlowElementField(DelegateExecution execution, String fieldName) {
        List<FieldExtension> fieldExtensions = getFlowElementFields(execution);
        if (fieldExtensions == null || fieldExtensions.size() == 0) {
            return null;
        }
        for (FieldExtension fieldExtension : fieldExtensions) {
            if (fieldExtension.getFieldName() != null && fieldExtension.getFieldName().equals(fieldName)) {
                return fieldExtension;
            }
        }
        return null;
    }

    public static FieldExtension getListenerField(DelegateExecution execution, String fieldName) {
        List<FieldExtension> fieldExtensions = getListenerFields(execution);
        if (fieldExtensions == null || fieldExtensions.size() == 0) {
            return null;
        }
        for (FieldExtension fieldExtension : fieldExtensions) {
            if (fieldExtension.getFieldName() != null && fieldExtension.getFieldName().equals(fieldName)) {
                return fieldExtension;
            }
        }
        return null;
    }

    /**
     * Creates an {@link Expression} for the {@link FieldExtension}.
     */
    public static Expression createExpressionForField(FieldExtension fieldExtension) {
        if (StringUtils.isNotEmpty(fieldExtension.getExpression())) {
            ExpressionManager expressionManager = CommandContextUtil.getProcessEngineConfiguration().getExpressionManager();
            return expressionManager.createExpression(fieldExtension.getExpression());
        } else {
            return new FixedValue(fieldExtension.getStringValue());
        }
    }

    /**
     * Returns the {@link Expression} for the field defined for the current activity of the provided {@link DelegateExecution}.
     * 
     * Returns null if no such field was found in the process definition xml.
     * 
     * If the execution is currently being used for executing an {@link ExecutionListener}, it will return the field expression for the listener. Use
     * {@link #getFlowElementFieldExpression(DelegateExecution, String)} or {@link #getListenerFieldExpression(DelegateExecution, String)} for specifically getting the flow element or listener field
     * expression.
     */
    public static Expression getFieldExpression(DelegateExecution execution, String fieldName) {
        if (isExecutingExecutionListener(execution)) {
            return getListenerFieldExpression(execution, fieldName);
        } else {
            return getFlowElementFieldExpression(execution, fieldName);
        }
    }

    /**
     * Similar to {@link #getFieldExpression(DelegateExecution, String)}, but for use within a {@link TaskListener}.
     */
    public static Expression getFieldExpression(DelegateTask task, String fieldName) {
        String eventHandlerId = task.getEventHandlerId();
        if (eventHandlerId != null && task.getProcessDefinitionId() != null) {
            org.flowable.bpmn.model.Process process = ProcessDefinitionUtil.getProcess(task.getProcessDefinitionId());
            UserTask userTask = (UserTask) process.getFlowElementMap().get(task.getTaskDefinitionKey());
            
            FlowableListener flowableListener = null;
            for (FlowableListener f : userTask.getTaskListeners()) {
                if (f.getId() != null && f.getId().equals(eventHandlerId)) {
                    flowableListener = f;
                }
            }
            
            if (flowableListener != null) {
                List<FieldExtension> fieldExtensions = flowableListener.getFieldExtensions();
                if (fieldExtensions != null && fieldExtensions.size() > 0) {
                    for (FieldExtension fieldExtension : fieldExtensions) {
                        if (fieldName.equals(fieldExtension.getFieldName())) {
                            return createExpressionForField(fieldExtension);
                        }
                    }
                }
            }
        }
        return null;
    }

    public static Expression getFlowElementFieldExpression(DelegateExecution execution, String fieldName) {
        FieldExtension fieldExtension = getFlowElementField(execution, fieldName);
        if (fieldExtension != null) {
            return createExpressionForField(fieldExtension);
        }
        return null;
    }

    public static Expression getListenerFieldExpression(DelegateExecution execution, String fieldName) {
        FieldExtension fieldExtension = getListenerField(execution, fieldName);
        if (fieldExtension != null) {
            return createExpressionForField(fieldExtension);
        }
        return null;
    }

}
