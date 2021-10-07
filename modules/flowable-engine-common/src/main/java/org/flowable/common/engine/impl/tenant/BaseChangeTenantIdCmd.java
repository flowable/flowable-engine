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

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.db.DbSqlSession;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Filip Hrisafov
 */
public abstract class BaseChangeTenantIdCmd implements Command<ChangeTenantIdResult> {

    protected final ChangeTenantIdBuilderImpl builder;
    protected String engineScopeType;

    protected BaseChangeTenantIdCmd(ChangeTenantIdBuilderImpl builder, String engineScopeType) {
        this.builder = builder;
        this.engineScopeType = engineScopeType;
    }

    @Override
    public ChangeTenantIdResult execute(CommandContext commandContext) {
        String sourceTenantId = builder.getSourceTenantId();
        String targetTenantId = builder.getTargetTenantId();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("sourceTenantId", sourceTenantId);
        parameters.put("targetTenantId", targetTenantId);
        parameters.put("definitionTenantId", builder.getDefinitionTenantId());

        DbSqlSession dbSqlSession = commandContext.getSession(DbSqlSession.class);

        Map<String, Long> results = executeOperation(dbSqlSession, parameters);
        DefaultChangeTenantIdResult result = new DefaultChangeTenantIdResult(results);
        beforeReturn(commandContext, result);
        return result;
    }

    protected void beforeReturn(CommandContext commandContext, ChangeTenantIdResult result) {
        // Nothing to do
    }

    protected abstract Map<String, Long> executeOperation(DbSqlSession dbSqlSession, Map<String, Object> parameters);

    protected AbstractEngineConfiguration getEngineConfiguration(CommandContext commandContext) {
        AbstractEngineConfiguration configuration = commandContext.getEngineConfigurations().get(engineScopeType);
        if (configuration != null) {
            return configuration;
        }

        throw new FlowableException("There is no engine registered for the scope type " + engineScopeType);
    }

}
