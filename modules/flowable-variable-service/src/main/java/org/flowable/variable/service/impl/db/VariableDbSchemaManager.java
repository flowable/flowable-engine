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
package org.flowable.variable.service.impl.db;

import org.flowable.engine.common.api.FlowableWrongDbException;
import org.flowable.engine.common.impl.FlowableVersions;
import org.flowable.engine.common.impl.db.AbstractSqlScriptBasedDbSchemaManager;

/**
 * @author Joram Barrez
 */
public class VariableDbSchemaManager extends AbstractSqlScriptBasedDbSchemaManager {
    
    private static final String VARIABLE_TABLE = "ACT_RU_VARIABLE";

    private static final String VARIABLE_VERSION_PROPERTY = "variable.schema.version";

    private static final String SCHEMA_COMPONENT = "variable";

    private static final String SCHEMA_COMPONENT_HISTORY = "variable.history";

    @Override
    public void dbSchemaCreate() {
        if (isTablePresent(VARIABLE_TABLE)) {
            String dbVersion = getVariableSchemaVersion();
            if (!FlowableVersions.CURRENT_VERSION.equals(dbVersion)) {
                throw new FlowableWrongDbException(FlowableVersions.CURRENT_VERSION, dbVersion);
            }
        } else {
            executeMandatorySchemaResource("create", SCHEMA_COMPONENT);
            if (getDbSqlSession().getDbSqlSessionFactory().isDbHistoryUsed()) {
                executeMandatorySchemaResource("create", SCHEMA_COMPONENT_HISTORY);
            }
        }
    }

    @Override
    public void dbSchemaDrop() {
        executeMandatorySchemaResource("drop", SCHEMA_COMPONENT);
        if (getDbSqlSession().getDbSqlSessionFactory().isDbHistoryUsed()) {
            executeMandatorySchemaResource("drop", SCHEMA_COMPONENT_HISTORY);
        }
    }

    @Override
    public String dbSchemaUpdate() {
        String feedback = null;
        if (isTablePresent(VARIABLE_TABLE)) {
            String dbVersion = getVariableSchemaVersion();
            int matchingVersionIndex = FlowableVersions.getFlowableVersionForDbVersion(dbVersion);
            boolean isUpgradeNeeded = (matchingVersionIndex != (FlowableVersions.FLOWABLE_VERSIONS.size() - 1));
            if (isUpgradeNeeded) {
                dbSchemaUpgrade(SCHEMA_COMPONENT, matchingVersionIndex);
                if (getDbSqlSession().getDbSqlSessionFactory().isDbHistoryUsed()) {
                    dbSchemaUpgrade(SCHEMA_COMPONENT_HISTORY, matchingVersionIndex);
                }
                setProperty(VARIABLE_VERSION_PROPERTY, FlowableVersions.CURRENT_VERSION);
            }
            
            feedback = "upgraded from " + dbVersion + " to " + FlowableVersions.CURRENT_VERSION;
        } else {
            dbSchemaCreate();
        }
        return feedback;
    }

    protected String getVariableSchemaVersion() {
        // The 'variable.schema.version' was introduced in 6.2.0.
        String dbVersion = getProperty(VARIABLE_VERSION_PROPERTY);
        if (dbVersion == null) {
            return "6.1.2.0"; // last version before common.schema.version was added. Start upgrading from this point.
        }
        return dbVersion;
    }
    
    @Override
    protected String getResourcesRootDirectory() {
        return "org/flowable/variable/db/";
    }
    
}
