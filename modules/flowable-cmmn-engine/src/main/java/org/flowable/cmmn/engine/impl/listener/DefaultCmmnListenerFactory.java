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
package org.flowable.cmmn.engine.impl.listener;

import org.flowable.cmmn.api.listener.CaseInstanceLifecycleListener;
import org.flowable.cmmn.api.listener.PlanItemInstanceLifecycleListener;
import org.flowable.cmmn.engine.impl.delegate.CmmnClassDelegateFactory;
import org.flowable.cmmn.model.FlowableListener;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.task.service.delegate.TaskListener;

/**
 * @author Joram Barrez
 */
public class DefaultCmmnListenerFactory implements CmmnListenerFactory {

    protected CmmnClassDelegateFactory classDelegateFactory;
    protected ExpressionManager expressionManager;

    public DefaultCmmnListenerFactory(CmmnClassDelegateFactory classDelegateFactory, ExpressionManager expressionManager) {
        this.classDelegateFactory = classDelegateFactory;
        this.expressionManager = expressionManager;
    }

    @Override
    public TaskListener createClassDelegateTaskListener(FlowableListener listener) {
        return classDelegateFactory.createLifeCycleListener(listener.getImplementation(), listener.getSourceState(), listener.getTargetState(), listener.getFieldExtensions());
    }

    @Override
    public TaskListener createExpressionTaskListener(FlowableListener listener) {
        return new ExpressionTaskListener(expressionManager.createExpression(listener.getImplementation()));
    }

    @Override
    public TaskListener createDelegateExpressionTaskListener(FlowableListener listener) {
        return new DelegateExpressionTaskListener(expressionManager.createExpression(listener.getImplementation()),listener.getFieldExtensions());
    }

    @Override
    public PlanItemInstanceLifecycleListener createClassDelegateLifeCycleListener(FlowableListener listener) {
        return classDelegateFactory.create(listener.getImplementation(), listener.getFieldExtensions());
    }

    @Override
    public PlanItemInstanceLifecycleListener createExpressionLifeCycleListener(FlowableListener listener) {
        return new ExpressionPlanItemLifecycleListener(listener.getSourceState(), listener.getTargetState(), expressionManager.createExpression(listener.getImplementation()));
    }

    @Override
    public PlanItemInstanceLifecycleListener createDelegateExpressionLifeCycleListener(FlowableListener listener) {
        return new DelegateExpressionPlanItemLifecycleListener(listener.getSourceState(), listener.getTargetState(),
            expressionManager.createExpression(listener.getImplementation()), listener.getFieldExtensions());
    }

    @Override
    public CaseInstanceLifecycleListener createClassDelegateCaseLifeCycleListener(FlowableListener listener) {
        return classDelegateFactory.create(listener.getImplementation(), listener.getFieldExtensions());
    }

    @Override
    public CaseInstanceLifecycleListener createExpressionCaseLifeCycleListener(FlowableListener listener) {
        return new ExpressionCaseLifecycleListener(listener.getSourceState(), listener.getTargetState(), expressionManager.createExpression(listener.getImplementation()));
    }

    @Override
    public CaseInstanceLifecycleListener createDelegateExpressionCaseLifeCycleListener(FlowableListener listener) {
        return new DelegateExpressionCaseLifecycleListener(listener.getSourceState(), listener.getTargetState(),
            expressionManager.createExpression(listener.getImplementation()), listener.getFieldExtensions());
    }

}
