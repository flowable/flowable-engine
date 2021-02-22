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
package org.flowable.cmmn.engine.impl.runtime;

import org.flowable.cmmn.api.runtime.InjectedPlanItemInstanceBuilder;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.impl.cmd.CreateInjectedPlanItemInstanceCmd;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;

/**
 * Implementation of the dynamically created and injected plan item into a running stage instance.
 *
 * @author Micha Kiener
 */
public class InjectedPlanItemInstanceBuilderImpl implements InjectedPlanItemInstanceBuilder {

    protected final CommandExecutor commandExecutor;
    
    protected String stagePlanItemInstanceId;
    protected String caseInstanceId;
    protected String caseDefinitionId;
    protected String elementId;
    protected String name;

    public InjectedPlanItemInstanceBuilderImpl(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    @Override
    public InjectedPlanItemInstanceBuilder name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public InjectedPlanItemInstanceBuilder caseDefinitionId(String caseDefinitionId) {
        this.caseDefinitionId = caseDefinitionId;
        return this;
    }

    @Override
    public InjectedPlanItemInstanceBuilder elementId(String elementId) {
        this.elementId = elementId;
        return this;
    }

    @Override
    public PlanItemInstance createInStage(String stagePlanItemInstanceId) {
        validateData();
        this.stagePlanItemInstanceId = stagePlanItemInstanceId;
        return commandExecutor.execute(new CreateInjectedPlanItemInstanceCmd(this));
    }

    @Override
    public PlanItemInstance createInCase(String caseInstanceId) {
        validateData();
        this.caseInstanceId = caseInstanceId;
        return commandExecutor.execute(new CreateInjectedPlanItemInstanceCmd(this));
    }

    protected void validateData() {
        if (caseDefinitionId == null) {
            throw new FlowableIllegalArgumentException("The case definition id must be provided for the plan item instance");
        }
        if (elementId == null) {
            throw new FlowableIllegalArgumentException("The element id must be provided for the plan item instance");
        }
    }

    public boolean injectInStage() {
        return stagePlanItemInstanceId != null;
    }
    public boolean injectInCase() {
        return caseInstanceId != null;
    }
    public String getStagePlanItemInstanceId() {
        return stagePlanItemInstanceId;
    }
    public String getCaseInstanceId() {
        return caseInstanceId;
    }
    public String getName() {
        return name;
    }
    public String getCaseDefinitionId() {
        return caseDefinitionId;
    }
    public String getElementId() {
        return elementId;
    }
}
