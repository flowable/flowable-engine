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

import org.flowable.common.engine.impl.db.DbSqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Filip Hrisafov
 */
public class SimulateChangeTenantIdCmd extends BaseChangeTenantIdCmd {

    protected static final Logger LOGGER = LoggerFactory.getLogger(SimulateChangeTenantIdCmd.class);

    protected final Set<String> entityTypes;

    public SimulateChangeTenantIdCmd(ChangeTenantIdBuilderImpl builder, String engineScopeType, Set<String> entityTypes) {
        super(builder, engineScopeType);
        this.entityTypes = entityTypes;
    }

    @Override
    protected Map<String, Long> executeOperation(DbSqlSession dbSqlSession, Map<String, Object> parameters) {
        if (LOGGER.isDebugEnabled()) {
            String definitionTenantId = builder.getDefinitionTenantId();
            String option = definitionTenantId != null
                    ? " but only for instances from the '" + definitionTenantId + "' tenant definitions"
                    : "";
            LOGGER.debug("Simulating instance migration from '{}' to '{}'{}.",
                    parameters.get("sourceTenantId"), parameters.get("targetTenantId"), option);
        }

        Map<String, Long> results = new HashMap<>();
        for (String entityType : entityTypes) {
            results.put(entityType, (long) dbSqlSession.selectOne("countChangeTenantId" + entityType, parameters));
        }

        return results;
    }

}
