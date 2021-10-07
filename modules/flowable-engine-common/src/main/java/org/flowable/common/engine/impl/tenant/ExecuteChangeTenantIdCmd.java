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
package org.flowable.common.engine.impl.tenant;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.common.engine.impl.db.DbSqlSession;
import org.flowable.common.engine.impl.event.FlowableChangeTenantIdEventImpl;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Filip Hrisafov
 */
public class ExecuteChangeTenantIdCmd extends BaseChangeTenantIdCmd {

    protected static final Logger LOGGER = LoggerFactory.getLogger(ExecuteChangeTenantIdCmd.class);

    protected final Set<String> entityTypes;
    protected final boolean dispatchEvent;

    public ExecuteChangeTenantIdCmd(ChangeTenantIdBuilderImpl builder, String engineScopeType, Set<String> entityTypes) {
        this(builder, engineScopeType, entityTypes, true);
    }

    public ExecuteChangeTenantIdCmd(ChangeTenantIdBuilderImpl builder, String engineScopeType, Set<String> entityTypes, boolean dispatchEvent) {
        super(builder, engineScopeType);
        this.entityTypes = entityTypes;
        this.dispatchEvent = dispatchEvent;
    }

    @Override
    protected Map<String, Long> executeOperation(DbSqlSession dbSqlSession, Map<String, Object> parameters) {
        if (LOGGER.isDebugEnabled()) {
            String definitionTenantId = builder.getDefinitionTenantId();
            String option = definitionTenantId != null
                    ? " but only for instances from the '" + definitionTenantId + "' tenant definitions"
                    : "";
            LOGGER.debug("Executing instance migration from '{}' to '{}'{}.",
                    parameters.get("sourceTenantId"), parameters.get("targetTenantId"), option);
        }

        Map<String, Long> results = new HashMap<>();
        for (String entityType : entityTypes) {
            results.put(entityType, (long) dbSqlSession.update("changeTenantId" + entityType, parameters));
        }

        return results;
    }

    @Override
    protected void beforeReturn(CommandContext commandContext, ChangeTenantIdResult result) {
        FlowableEventDispatcher eventDispatcher = getEngineConfiguration(commandContext)
                .getEventDispatcher();

        if (dispatchEvent && eventDispatcher != null && eventDispatcher.isEnabled()) {
            String sourceTenantId = builder.getSourceTenantId();
            String targetTenantId = builder.getTargetTenantId();
            String definitionTenantId = builder.getDefinitionTenantId();
            eventDispatcher.dispatchEvent(new FlowableChangeTenantIdEventImpl(engineScopeType, sourceTenantId, targetTenantId, definitionTenantId),
                    engineScopeType);
        }
    }
}
