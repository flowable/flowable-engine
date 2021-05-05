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
package org.flowable.cmmn.engine.impl.parser;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.cmmn.engine.impl.behavior.CmmnActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.CasePageTaskActivityBehaviour;
import org.flowable.cmmn.engine.impl.behavior.impl.CaseTaskActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.DecisionTaskActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.EventRegistryEventListenerActivityBehaviour;
import org.flowable.cmmn.engine.impl.behavior.impl.ExternalWorkerTaskActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.GenericEventListenerActivityBehaviour;
import org.flowable.cmmn.engine.impl.behavior.impl.HumanTaskActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.MailActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.MilestoneActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.PlanItemDelegateExpressionActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.PlanItemExpressionActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.ProcessTaskActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.ScriptTaskActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.SendEventActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.SignalEventListenerActivityBehaviour;
import org.flowable.cmmn.engine.impl.behavior.impl.StageActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.TaskActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.TimerEventListenerActivityBehaviour;
import org.flowable.cmmn.engine.impl.behavior.impl.UserEventListenerActivityBehaviour;
import org.flowable.cmmn.engine.impl.behavior.impl.VariableEventListenerActivityBehaviour;
import org.flowable.cmmn.engine.impl.behavior.impl.http.DefaultCmmnHttpActivityDelegate;
import org.flowable.cmmn.engine.impl.delegate.CmmnClassDelegate;
import org.flowable.cmmn.engine.impl.delegate.CmmnClassDelegateFactory;
import org.flowable.cmmn.model.CasePageTask;
import org.flowable.cmmn.model.CaseTask;
import org.flowable.cmmn.model.DecisionTask;
import org.flowable.cmmn.model.ExternalWorkerServiceTask;
import org.flowable.cmmn.model.FieldExtension;
import org.flowable.cmmn.model.GenericEventListener;
import org.flowable.cmmn.model.HumanTask;
import org.flowable.cmmn.model.Milestone;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.ProcessTask;
import org.flowable.cmmn.model.ScriptServiceTask;
import org.flowable.cmmn.model.SendEventServiceTask;
import org.flowable.cmmn.model.ServiceTask;
import org.flowable.cmmn.model.SignalEventListener;
import org.flowable.cmmn.model.Stage;
import org.flowable.cmmn.model.Task;
import org.flowable.cmmn.model.TimerEventListener;
import org.flowable.cmmn.model.UserEventListener;
import org.flowable.cmmn.model.VariableEventListener;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.el.ExpressionManager;

/**
 * @author Joram Barrez
 */
public class DefaultCmmnActivityBehaviorFactory implements CmmnActivityBehaviorFactory {

    protected CmmnClassDelegateFactory classDelegateFactory;
    protected ExpressionManager expressionManager;

    @Override
    public StageActivityBehavior createStageActivityBehavior(PlanItem planItem, Stage stage) {
        return new StageActivityBehavior(stage);
    }

    @Override
    public MilestoneActivityBehavior createMilestoneActivityBehavior(PlanItem planItem, Milestone milestone) {
        String name = null;
        if (!StringUtils.isEmpty(planItem.getName())) {
            name = planItem.getName();
        } else if (StringUtils.isNotEmpty(milestone.getName())) {
            name = milestone.getName();
        }
        return new MilestoneActivityBehavior(expressionManager.createExpression(name), milestone.getMilestoneVariable());
    }

    @Override
    public TaskActivityBehavior createTaskActivityBehavior(PlanItem planItem, Task task) {
        return new TaskActivityBehavior(task.isBlocking(), task.getBlockingExpression());
    }

    @Override
    public HumanTaskActivityBehavior createHumanTaskActivityBehavior(PlanItem planItem, HumanTask humanTask) {
        return new HumanTaskActivityBehavior(humanTask);
    }

    @Override
    public CaseTaskActivityBehavior createCaseTaskActivityBehavior(PlanItem planItem, CaseTask caseTask) {
        Expression caseRefExpression = createExpression(caseTask.getCaseRefExpression());
        return new CaseTaskActivityBehavior(caseRefExpression, caseTask);
    }

    @Override
    public ProcessTaskActivityBehavior createProcessTaskActivityBehavior(PlanItem planItem, ProcessTask processTask) {
        Expression processRefExpression = createExpression(processTask.getProcessRefExpression());
        return new ProcessTaskActivityBehavior(processTask.getProcess(), processRefExpression, processTask);
    }

    @Override
    public CmmnClassDelegate createCmmnClassDelegate(PlanItem planItem, ServiceTask task) {
        return classDelegateFactory.create(task.getImplementation(), task.getFieldExtensions());
    }

    @Override
    public PlanItemExpressionActivityBehavior createPlanItemExpressionActivityBehavior(PlanItem planItem, ServiceTask task) {
        return new PlanItemExpressionActivityBehavior(task.getImplementation(), task.getResultVariableName(), task.isStoreResultVariableAsTransient());
    }

    @Override
    public PlanItemDelegateExpressionActivityBehavior createPlanItemDelegateExpressionActivityBehavior(PlanItem planItem, ServiceTask task) {
        return new PlanItemDelegateExpressionActivityBehavior(task.getImplementation(), task.getFieldExtensions());
    }

    @Override
    public TimerEventListenerActivityBehaviour createTimerEventListenerActivityBehavior(PlanItem planItem, TimerEventListener timerEventListener) {
        return new TimerEventListenerActivityBehaviour(timerEventListener);
    }

    @Override
    public UserEventListenerActivityBehaviour createUserEventListenerActivityBehavior(PlanItem planItem, UserEventListener userEventListener) {
        return new UserEventListenerActivityBehaviour(userEventListener);
    }
    
    @Override
    public SignalEventListenerActivityBehaviour createSignalEventListenerActivityBehavior(PlanItem planItem, SignalEventListener signalEventListener) {
        return new SignalEventListenerActivityBehaviour(signalEventListener);
    }
    
    @Override
    public GenericEventListenerActivityBehaviour createGenericEventListenerActivityBehavior(PlanItem planItem, GenericEventListener genericEventListener) {
        return new GenericEventListenerActivityBehaviour();
    }

    @Override
    public EventRegistryEventListenerActivityBehaviour createEventRegistryEventListenerActivityBehaviour(PlanItem planItem, GenericEventListener genericEventListener) {
        return new EventRegistryEventListenerActivityBehaviour(createExpression(genericEventListener.getEventType()));
    }
    
    @Override
    public VariableEventListenerActivityBehaviour createVariableEventListenerActivityBehaviour(PlanItem planItem, VariableEventListener variableEventListener) {
        return new VariableEventListenerActivityBehaviour(variableEventListener);
    }

    @Override
    public DecisionTaskActivityBehavior createDecisionTaskActivityBehavior(PlanItem planItem, DecisionTask decisionTask) {
        return new DecisionTaskActivityBehavior(createExpression(decisionTask.getDecisionRefExpression()), decisionTask);
    }

    @Override
    public CmmnActivityBehavior createHttpActivityBehavior(PlanItem planItem, ServiceTask task) {
        try {
            Class<?> theClass = null;
            FieldExtension behaviorExtension = null;
            for (FieldExtension fieldExtension : task.getFieldExtensions()) {
                if ("httpActivityBehaviorClass".equals(fieldExtension.getFieldName()) && StringUtils.isNotEmpty(fieldExtension.getStringValue())) {
                    theClass = Class.forName(fieldExtension.getStringValue());
                    behaviorExtension = fieldExtension;
                    break;
                }
            }

            if (behaviorExtension != null) {
                task.getFieldExtensions().remove(behaviorExtension);
            }

            // Default Http behavior class
            if (theClass == null) {
                return createDefaultHttpActivityBehaviour(planItem, task);
            }

            return classDelegateFactory.create(theClass.getName(), task.getFieldExtensions());

        } catch (ClassNotFoundException e) {
            throw new FlowableException("Could not find org.flowable.http.HttpActivityBehavior: ", e);
        }
    }

    protected CmmnActivityBehavior createDefaultHttpActivityBehaviour(PlanItem planItem, ServiceTask serviceTask) {
        if (ImplementationType.IMPLEMENTATION_TYPE_CLASS.equals(serviceTask.getImplementationType())) {
            return createCmmnClassDelegate(planItem, serviceTask);
        } else if (ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION.equals(serviceTask.getImplementationType())) {
            return createPlanItemDelegateExpressionActivityBehavior(planItem, serviceTask);
        } else {
            return classDelegateFactory.create(DefaultCmmnHttpActivityDelegate.class.getName(), serviceTask.getFieldExtensions());
        }
    }

    @Override
    public MailActivityBehavior createEmailActivityBehavior(PlanItem planItem, ServiceTask task) {
        return (MailActivityBehavior) classDelegateFactory.defaultInstantiateDelegate(MailActivityBehavior.class, task, true); // MailActivityBehavior only has expression fields
    }

    @Override
    public SendEventActivityBehavior createSendEventActivityBehavior(PlanItem planItem, SendEventServiceTask sendEventServiceTask) {
        return new SendEventActivityBehavior(sendEventServiceTask);
    }

    @Override
    public ExternalWorkerTaskActivityBehavior createExternalWorkerActivityBehaviour(PlanItem planItem, ExternalWorkerServiceTask externalWorkerServiceTask) {
        return new ExternalWorkerTaskActivityBehavior(externalWorkerServiceTask);
    }

    @Override
    public ScriptTaskActivityBehavior createScriptTaskActivityBehavior(PlanItem planItem, ScriptServiceTask task) {
        return new ScriptTaskActivityBehavior(task);
    }

    @Override
    public CasePageTaskActivityBehaviour createCasePageTaskActivityBehaviour(PlanItem planItem, CasePageTask task) {
        return new CasePageTaskActivityBehaviour(task);
    }

    public void setClassDelegateFactory(CmmnClassDelegateFactory classDelegateFactory) {
        this.classDelegateFactory = classDelegateFactory;
    }

    public ExpressionManager getExpressionManager() {
        return expressionManager;
    }

    public void setExpressionManager(ExpressionManager expressionManager) {
        this.expressionManager = expressionManager;
    }

    protected Expression createExpression(String refExpressionString) {
        Expression expression = null;
        if (StringUtils.isNotEmpty(refExpressionString)) {
            expression = expressionManager.createExpression(refExpressionString);
        }
        return expression;
    }

}
