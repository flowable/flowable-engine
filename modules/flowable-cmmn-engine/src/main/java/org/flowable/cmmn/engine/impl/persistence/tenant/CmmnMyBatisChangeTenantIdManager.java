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
package org.flowable.cmmn.engine.impl.persistence.tenant;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.flowable.cmmn.api.CmmnChangeTenantIdEntityTypes;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.api.tenant.ChangeTenantIdRequest;
import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.tenant.ChangeTenantIdManager;
import org.flowable.common.engine.impl.tenant.ExecuteChangeTenantIdCmd;
import org.flowable.common.engine.impl.tenant.SimulateChangeTenantIdCmd;

/**
 * @author Filip Hrisafov
 */
public class CmmnMyBatisChangeTenantIdManager implements ChangeTenantIdManager {

    protected static final Set<String> RUNTIME_TYPES = new HashSet<>(Arrays.asList(
            CmmnChangeTenantIdEntityTypes.CASE_INSTANCES,
            CmmnChangeTenantIdEntityTypes.MILESTONE_INSTANCES,
            CmmnChangeTenantIdEntityTypes.PLAN_ITEM_INSTANCES,
            CmmnChangeTenantIdEntityTypes.EVENT_SUBSCRIPTIONS,
            CmmnChangeTenantIdEntityTypes.DEADLETTER_JOBS,
            CmmnChangeTenantIdEntityTypes.EXTERNAL_WORKER_JOBS,
            CmmnChangeTenantIdEntityTypes.JOBS,
            CmmnChangeTenantIdEntityTypes.SUSPENDED_JOBS,
            CmmnChangeTenantIdEntityTypes.TIMER_JOBS,
            CmmnChangeTenantIdEntityTypes.TASKS
    ));

    protected static final Set<String> HISTORIC_TYPES = new HashSet<>(Arrays.asList(
            CmmnChangeTenantIdEntityTypes.HISTORIC_CASE_INSTANCES,
            CmmnChangeTenantIdEntityTypes.HISTORIC_MILESTONE_INSTANCES,
            CmmnChangeTenantIdEntityTypes.HISTORIC_PLAN_ITEM_INSTANCES,
            CmmnChangeTenantIdEntityTypes.HISTORIC_TASK_INSTANCES,
            CmmnChangeTenantIdEntityTypes.HISTORIC_TASK_LOG_ENTRIES
    ));

    protected final CommandExecutor commandExecutor;
    protected final Set<String> entityTypes;

    public CmmnMyBatisChangeTenantIdManager(CommandExecutor commandExecutor, boolean dbHistoryUsed) {
        this.commandExecutor = commandExecutor;
        this.entityTypes = new HashSet<>(RUNTIME_TYPES);
        if (dbHistoryUsed) {
            this.entityTypes.addAll(HISTORIC_TYPES);
        }
    }

    @Override
    public ChangeTenantIdResult simulate(ChangeTenantIdRequest request) {
        return commandExecutor.execute(new SimulateChangeTenantIdCmd(request, ScopeTypes.CMMN, entityTypes));
    }

    @Override
    public ChangeTenantIdResult complete(ChangeTenantIdRequest request) {
        return commandExecutor.execute(new ExecuteChangeTenantIdCmd(request, ScopeTypes.CMMN, entityTypes));
    }

}
