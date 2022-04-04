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
package org.flowable.cmmn.engine.impl.agenda.operation;

import java.util.Objects;

import org.flowable.cmmn.engine.impl.listener.CaseInstanceLifeCycleListenerUtil;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public abstract class AbstractChangeCaseInstanceStateOperation extends AbstractCaseInstanceOperation {

    public AbstractChangeCaseInstanceStateOperation(CommandContext commandContext, String caseInstanceId) {
        super(commandContext, caseInstanceId, null);
    }

    public AbstractChangeCaseInstanceStateOperation(CommandContext commandContext, CaseInstanceEntity caseInstanceEntity) {
        super(commandContext, null, caseInstanceEntity);
    }

    @Override
    public void run() {
        preRunCheck();

        super.run();

        String oldState = caseInstanceEntity.getState();
        String newState = getNewState();
        if (!Objects.equals(oldState, newState)) {
            invokePreLifecycleListeners();
            CaseInstanceLifeCycleListenerUtil.callLifecycleListeners(commandContext, caseInstanceEntity, caseInstanceEntity.getState(), newState);
            invokePostLifecycleListeners();
            caseInstanceEntity.setState(newState);

            internalExecute();
        }
    }

    /**
     * Internal hook to be implemented to invoke any listeners BEFORE the lifecycle listeners are being invoked and before the new state is set
     * on the case instance.
     */
    protected void invokePreLifecycleListeners() {

    }

    /**
     * Internal hook to be implemented to invoke any listeners AFTER the lifecycle listeners are being invoked and before the new state is set
     * on the case instance.
     */
    protected void invokePostLifecycleListeners() {

    }

    public void preRunCheck() {
        // Meant to be overridden
    }
    
    public abstract String getNewState();

    public abstract void internalExecute();

    public abstract void changeStateForChildPlanItemInstance(PlanItemInstanceEntity planItemInstanceEntity);

}
