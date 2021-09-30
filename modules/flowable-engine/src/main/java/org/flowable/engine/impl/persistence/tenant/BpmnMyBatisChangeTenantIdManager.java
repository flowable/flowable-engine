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
package org.flowable.engine.impl.persistence.tenant;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.api.tenant.ChangeTenantIdRequest;
import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.tenant.ChangeTenantIdManager;
import org.flowable.common.engine.impl.tenant.ExecuteChangeTenantIdCmd;
import org.flowable.common.engine.impl.tenant.SimulateChangeTenantIdCmd;
import org.flowable.engine.BpmnChangeTenantIdEntityTypes;

/**
 * @author Filip Hrisafov
 */
public class BpmnMyBatisChangeTenantIdManager implements ChangeTenantIdManager {

    protected static final Set<String> RUNTIME_TYPES = new HashSet<>(Arrays.asList(
            BpmnChangeTenantIdEntityTypes.EXECUTIONS,
            BpmnChangeTenantIdEntityTypes.ACTIVITY_INSTANCES,
            BpmnChangeTenantIdEntityTypes.EVENT_SUBSCRIPTIONS,
            BpmnChangeTenantIdEntityTypes.DEADLETTER_JOBS,
            BpmnChangeTenantIdEntityTypes.EXTERNAL_WORKER_JOBS,
            BpmnChangeTenantIdEntityTypes.JOBS,
            BpmnChangeTenantIdEntityTypes.SUSPENDED_JOBS,
            BpmnChangeTenantIdEntityTypes.TIMER_JOBS,
            BpmnChangeTenantIdEntityTypes.TASKS
    ));

    protected static final Set<String> HISTORIC_TYPES = new HashSet<>(Arrays.asList(
            BpmnChangeTenantIdEntityTypes.HISTORIC_PROCESS_INSTANCES,
            BpmnChangeTenantIdEntityTypes.HISTORIC_ACTIVITY_INSTANCES,
            BpmnChangeTenantIdEntityTypes.HISTORIC_TASK_INSTANCES,
            BpmnChangeTenantIdEntityTypes.HISTORIC_TASK_LOG_ENTRIES
    ));

    protected final CommandExecutor commandExecutor;
    protected final Set<String> entityTypes;

    public BpmnMyBatisChangeTenantIdManager(CommandExecutor commandExecutor, boolean dbHistoryUsed) {
        this.commandExecutor = commandExecutor;
        this.entityTypes = new HashSet<>(RUNTIME_TYPES);
        if (dbHistoryUsed) {
            this.entityTypes.addAll(HISTORIC_TYPES);
        }
    }

    @Override
    public ChangeTenantIdResult simulate(ChangeTenantIdRequest request) {
        return commandExecutor.execute(new SimulateChangeTenantIdCmd(request, ScopeTypes.BPMN, entityTypes));
    }

    @Override
    public ChangeTenantIdResult complete(ChangeTenantIdRequest request) {
        return commandExecutor.execute(new ExecuteChangeTenantIdCmd(request, ScopeTypes.BPMN, entityTypes));
    }

}
