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
package org.flowable.cmmn.engine.impl.behavior;

import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.process.ProcessInstanceService;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.runtime.DelegatePlanItemInstance;
import org.flowable.cmmn.model.Process;
import org.flowable.engine.common.api.FlowableException;

import liquibase.util.StringUtils;

/**
 * @author Joram Barrez
 */
public class ProcessTaskActivityBehavior extends TaskActivityBehavior {
    
    protected Process process;
    
    public ProcessTaskActivityBehavior(Process process, boolean isBlocking) {
        super(isBlocking);
        this.process = process;
    }

    @Override
    public void execute(DelegatePlanItemInstance planItemInstance) {
        ProcessInstanceService processInstanceService = CommandContextUtil.getCmmnEngineConfiguration().getProcessInstanceService();
        if (processInstanceService == null) {
            throw new FlowableException("Could not start process instance: no " + ProcessInstanceService.class + " implementation found");
        }

        if (process != null) {
            String externalRef = process.getExternalRef();
            if (StringUtils.isEmpty(externalRef)) {
                throw new FlowableException("Could not start process instance: no externalRef defined");
            }
            processInstanceService.startProcessInstanceByKey(externalRef, planItemInstance.getId());
            
        } else {
            // TODO (expression support etc)
            
        }
        
        if (!isBlocking) {
            CommandContextUtil.getAgenda().planCompletePlanItem((PlanItemInstanceEntity) planItemInstance);
        }
    }
    
}
