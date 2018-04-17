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
import org.flowable.cmmn.engine.impl.behavior.CmmnActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.*;
import org.flowable.cmmn.engine.impl.delegate.CmmnClassDelegate;
import org.flowable.cmmn.engine.impl.delegate.CmmnClassDelegateFactory;
import org.flowable.cmmn.model.*;
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
        return new MilestoneActivityBehavior(expressionManager.createExpression(name));
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
        return new CaseTaskActivityBehavior(expressionManager.createExpression(caseTask.getCaseRef()), caseTask);
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
        return new PlanItemExpressionActivityBehavior(task.getImplementation(), task.getResultVariableName());
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
                theClass = Class.forName("org.flowable.http.cmmn.impl.CmmnHttpActivityBehaviorImpl");
            }

            return (CmmnActivityBehavior) classDelegateFactory.defaultInstantiateDelegate(theClass, task);

        } catch (ClassNotFoundException e) {
            throw new FlowableException("Could not find org.flowable.http.HttpActivityBehavior: ", e);
        }
    }

    @Override
    public ScriptTaskActivityBehavior createScriptTaskActivityBehavior(PlanItem planItem, ScriptServiceTask task) {
        return new ScriptTaskActivityBehavior(task);
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
        Expression processRefExpression = null;
        if (StringUtils.isNotEmpty(refExpressionString)) {
            processRefExpression = expressionManager.createExpression(refExpressionString);
        }
        return processRefExpression;
    }

}
