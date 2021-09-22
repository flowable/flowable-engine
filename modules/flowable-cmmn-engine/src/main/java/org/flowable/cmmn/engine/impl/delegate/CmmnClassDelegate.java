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
package org.flowable.cmmn.engine.impl.delegate;

import java.util.List;

import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.delegate.PlanItemFutureJavaDelegate;
import org.flowable.cmmn.api.delegate.PlanItemJavaDelegate;
import org.flowable.cmmn.api.delegate.PlanItemVariableAggregator;
import org.flowable.cmmn.api.delegate.PlanItemVariableAggregatorContext;
import org.flowable.cmmn.api.listener.CaseInstanceLifecycleListener;
import org.flowable.cmmn.api.listener.PlanItemInstanceLifecycleListener;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.impl.behavior.CmmnActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.CmmnTriggerableActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.PlanItemFutureJavaDelegateActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.PlanItemJavaDelegateActivityBehavior;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.FieldExtension;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.el.FixedValue;
import org.flowable.common.engine.impl.util.ReflectUtil;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.flowable.variable.api.persistence.entity.VariableInstance;

/**
 * @author Joram Barrez
 */
public class CmmnClassDelegate implements CmmnTriggerableActivityBehavior, TaskListener, PlanItemInstanceLifecycleListener, CaseInstanceLifecycleListener,
        PlanItemVariableAggregator {

    protected String sourceState;
    protected String targetState;
    protected String className;
    protected List<FieldExtension> fieldExtensions;
    protected CmmnActivityBehavior activityBehaviorInstance;

    public CmmnClassDelegate(String className, List<FieldExtension> fieldExtensions) {
        this.className = className;
        this.fieldExtensions = fieldExtensions;
    }

    @Override
    public void execute(DelegatePlanItemInstance planItemInstance) {
        if (activityBehaviorInstance == null) {
            activityBehaviorInstance = getCmmnActivityBehavior(className);
        }
        activityBehaviorInstance.execute(planItemInstance);
    }

    @Override
    public void trigger(DelegatePlanItemInstance planItemInstance) {
        if (activityBehaviorInstance == null) {
            activityBehaviorInstance = getCmmnActivityBehavior(className);
        }

        if (!(activityBehaviorInstance instanceof CmmnTriggerableActivityBehavior)) {
            throw new FlowableIllegalArgumentException(className + " does not implement the "
                + CmmnTriggerableActivityBehavior.class + " interface");
        }

        ((CmmnTriggerableActivityBehavior) activityBehaviorInstance).trigger(planItemInstance);
    }

    protected CmmnActivityBehavior getCmmnActivityBehavior(String className) {
        Object instance = instantiate(className);
        applyFieldExtensions(fieldExtensions, instance, false);

        if (instance instanceof PlanItemJavaDelegate) {
            return new PlanItemJavaDelegateActivityBehavior((PlanItemJavaDelegate) instance);

        } else if (instance instanceof PlanItemFutureJavaDelegate) {
            return new PlanItemFutureJavaDelegateActivityBehavior((PlanItemFutureJavaDelegate) instance);

        } else if (instance instanceof CmmnTriggerableActivityBehavior) {
            return (CmmnTriggerableActivityBehavior) instance;

        } else if (instance instanceof CmmnActivityBehavior) {
            return (CmmnActivityBehavior) instance;

        } else {
            throw new FlowableIllegalArgumentException(className + " does not implement the "
                    + CmmnActivityBehavior.class + " nor the " + PlanItemJavaDelegate.class + " interface");

        }
    }

    @Override
    public void notify(DelegateTask delegateTask) {
        TaskListener taskListenerInstance = getTaskListenerInstance(delegateTask);
        taskListenerInstance.notify(delegateTask);
    }

    protected TaskListener getTaskListenerInstance(DelegateTask delegateTask) {
        Object delegateInstance = instantiate(className);
        applyFieldExtensions(fieldExtensions, delegateInstance, false);

        if (delegateInstance instanceof TaskListener) {
            return (TaskListener) delegateInstance;
        } else {
            throw new FlowableIllegalArgumentException(delegateInstance.getClass().getName() + " doesn't implement " + TaskListener.class);
        }
    }

    @Override
    public void stateChanged(DelegatePlanItemInstance planItemInstance, String oldState, String newState) {
        PlanItemInstanceLifecycleListener planItemLifeCycleListenerInstance = getPlanItemLifeCycleListenerInstance();
        planItemLifeCycleListenerInstance.stateChanged(planItemInstance, oldState, newState);
    }

    @Override
    public void stateChanged(CaseInstance caseInstance, String oldState, String newState) {
        CaseInstanceLifecycleListener caseLifeCycleListenerInstance = getCaseLifeCycleListenerInstance();
        caseLifeCycleListenerInstance.stateChanged(caseInstance, oldState, newState);
    }

    protected PlanItemInstanceLifecycleListener getPlanItemLifeCycleListenerInstance() {
        Object delegateInstance = instantiate(className);
        applyFieldExtensions(fieldExtensions, delegateInstance, false);
        if (delegateInstance instanceof PlanItemInstanceLifecycleListener) {
            return (PlanItemInstanceLifecycleListener) delegateInstance;
        } else {
            throw new FlowableIllegalArgumentException(delegateInstance.getClass().getName() + " doesn't implement " + PlanItemInstanceLifecycleListener.class);
        }
    }

    protected CaseInstanceLifecycleListener getCaseLifeCycleListenerInstance() {
        Object delegateInstance = instantiate(className);
        applyFieldExtensions(fieldExtensions, delegateInstance, false);
        if (delegateInstance instanceof CaseInstanceLifecycleListener) {
            return (CaseInstanceLifecycleListener) delegateInstance;
        } else {
            throw new FlowableIllegalArgumentException(delegateInstance.getClass().getName() + " doesn't implement " + CaseInstanceLifecycleListener.class);
        }
    }

    @Override
    public Object aggregateSingleVariable(DelegatePlanItemInstance planItemInstance, PlanItemVariableAggregatorContext context) {
        return getPlanItemVariableAggregator().aggregateSingleVariable(planItemInstance, context);
    }

    @Override
    public Object aggregateMultiVariables(DelegatePlanItemInstance planItemInstance, List<? extends VariableInstance> instances, PlanItemVariableAggregatorContext context) {
        return getPlanItemVariableAggregator().aggregateMultiVariables(planItemInstance, instances, context);
    }

    protected PlanItemVariableAggregator getPlanItemVariableAggregator() {
        Object delegateInstance = instantiate(className);
        applyFieldExtensions(fieldExtensions, delegateInstance, false);
        if (delegateInstance instanceof PlanItemVariableAggregator) {
            return (PlanItemVariableAggregator) delegateInstance;
        } else {
            throw new FlowableIllegalArgumentException(delegateInstance.getClass().getName() + " doesn't implement " + PlanItemVariableAggregator.class);
        }
    }

    protected Object instantiate(String className) {
        return ReflectUtil.instantiate(className);
    }

    public static void applyFieldExtensions(List<FieldExtension> fieldExtensions, Object target, boolean throwExceptionOnMissingField) {
        if (fieldExtensions != null) {
            for (FieldExtension fieldExtension : fieldExtensions) {
                applyFieldExtension(fieldExtension, target, throwExceptionOnMissingField);
            }
        }
    }

    protected static void applyFieldExtension(FieldExtension fieldExtension, Object target, boolean throwExceptionOnMissingField) {
        Object value = null;
        if (fieldExtension.getExpression() != null) {
            ExpressionManager expressionManager = CommandContextUtil.getCmmnEngineConfiguration().getExpressionManager();
            value = expressionManager.createExpression(fieldExtension.getExpression());
        } else {
            value = new FixedValue(fieldExtension.getStringValue());
        }

        ReflectUtil.invokeSetterOrField(target, fieldExtension.getFieldName(), value, throwExceptionOnMissingField);
    }

    @Override
    public String getSourceState() {
        return sourceState;
    }
    public void setSourceState(String sourceState) {
        this.sourceState = sourceState;
    }
    @Override
    public String getTargetState() {
        return targetState;
    }
    public void setTargetState(String targetState) {
        this.targetState = targetState;
    }
    public String getClassName() {
        return className;
    }
    public void setClassName(String className) {
        this.className = className;
    }
    public List<FieldExtension> getFieldExtensions() {
        return fieldExtensions;
    }
    public void setFieldExtensions(List<FieldExtension> fieldExtensions) {
        this.fieldExtensions = fieldExtensions;
    }
    public CmmnActivityBehavior getActivityBehaviorInstance() {
        return activityBehaviorInstance;
    }
    public void setActivityBehaviorInstance(CmmnActivityBehavior activityBehaviorInstance) {
        this.activityBehaviorInstance = activityBehaviorInstance;
    }
}
