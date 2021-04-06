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
package org.flowable.cmmn.engine.impl.cmd;

import java.io.Serializable;

import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.reactivation.CaseReactivationBuilderImpl;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableIllegalStateException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * This command reactivates a history case instance by putting it back to the runtime and triggering the reactivation event on its CMMN model. If there is no
 * reactivation event explicitly available, an exception is thrown.
 *
 * @author Micha Kiener
 */
public class ReactivateHistoricCaseInstanceCmd implements Command<CaseInstance>, Serializable {

    private static final long serialVersionUID = 1L;
    protected final CaseReactivationBuilderImpl reactivationBuilder;

    public ReactivateHistoricCaseInstanceCmd(CaseReactivationBuilderImpl reactivationBuilder) {
        this.reactivationBuilder = reactivationBuilder;
    }

    @Override
    public CaseInstance execute(CommandContext commandContext) {
        if (reactivationBuilder.getCaseInstanceId() == null) {
            throw new FlowableIllegalArgumentException("No historic case instance id provided");
        }
        // Check if the historic case instance is found and if it is no longer running
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        HistoricCaseInstance instance = cmmnEngineConfiguration.getHistoricCaseInstanceEntityManager().createHistoricCaseInstanceQuery()
            .caseInstanceId(reactivationBuilder.getCaseInstanceId())
            .singleResult();

        if (instance == null) {
            throw new FlowableObjectNotFoundException("No historic case instance to be reactivated found with id: " + reactivationBuilder.getCaseInstanceId(), HistoricCaseInstance.class);
        }
        if (instance.getEndTime() == null) {
            throw new FlowableIllegalStateException("Case instance is still running, cannot reactivate historic case instance: " + reactivationBuilder.getCaseInstanceId());
        }

        CaseInstanceEntity caseInstanceEntity = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getCaseInstanceHelper()
            .copyHistoricCaseInstanceToRuntime(instance);

        // set case variables, if the builder contains any
        if (reactivationBuilder.hasVariables()) {
            caseInstanceEntity.setVariables(reactivationBuilder.getVariables());
        }

        // set transient case variables, if the builder contains any
        if (reactivationBuilder.hasTransientVariables()) {
            caseInstanceEntity.setTransientVariables(reactivationBuilder.getTransientVariables());
        }

        // the reactivate operation will take care of triggering the reactivation event and re-initialize all necessary plan items according the model
        CommandContextUtil.getAgenda(commandContext).planReactivateCaseInstanceOperation(caseInstanceEntity);

        return caseInstanceEntity;
    }

}
