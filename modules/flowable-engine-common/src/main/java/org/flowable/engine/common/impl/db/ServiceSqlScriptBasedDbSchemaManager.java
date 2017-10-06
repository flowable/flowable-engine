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
package org.flowable.engine.common.impl.db;

import org.flowable.engine.common.api.FlowableWrongDbException;
import org.flowable.engine.common.impl.FlowableVersions;

/**
 * @author Joram Barrez
 */
public abstract class ServiceSqlScriptBasedDbSchemaManager extends AbstractSqlScriptBasedDbSchemaManager {
    
    protected String table;
    protected String schemaComponent;
    protected String schemaComponentHistory;
    protected String schemaVersionProperty;
    
    public ServiceSqlScriptBasedDbSchemaManager(String table, String schemaComponent, String schemaComponentHistory, String schemaVersionProperty) {
        this.table = table;
        this.schemaComponent = schemaComponent;
        this.schemaComponentHistory = schemaComponentHistory;
        this.schemaVersionProperty = schemaVersionProperty;
    }
    
    @Override
    public void dbSchemaCreate() {
        if (isTablePresent(table)) {
            String dbVersion = getSchemaVersion();
            if (!FlowableVersions.CURRENT_VERSION.equals(dbVersion)) {
                throw new FlowableWrongDbException(FlowableVersions.CURRENT_VERSION, dbVersion);
            }
        } else {
            executeMandatorySchemaResource("create", schemaComponent);
            if (isHistoryUsed()) {
                executeMandatorySchemaResource("create", schemaComponentHistory);
            }
        }
    }

    @Override
    public void dbSchemaDrop() {
        executeMandatorySchemaResource("drop", schemaComponent);
        if (isHistoryUsed()) {
            executeMandatorySchemaResource("drop", schemaComponentHistory);
        }
    }

    @Override
    public String dbSchemaUpdate() {
        String feedback = null;
        if (isTablePresent(table)) {
            String dbVersion = getSchemaVersion();
            int matchingVersionIndex = FlowableVersions.getFlowableVersionForDbVersion(dbVersion);
            boolean isUpgradeNeeded = (matchingVersionIndex != (FlowableVersions.FLOWABLE_VERSIONS.size() - 1));
            if (isUpgradeNeeded) {
                dbSchemaUpgrade(schemaComponent, matchingVersionIndex);
                if (isHistoryUsed()) {
                    dbSchemaUpgrade(schemaComponentHistory, matchingVersionIndex);
                }
                setProperty(schemaVersionProperty, FlowableVersions.CURRENT_VERSION);
            }
            
            feedback = "upgraded from " + dbVersion + " to " + FlowableVersions.CURRENT_VERSION;
        } else {
            dbSchemaCreate();
        }
        return feedback;
    }
    
    protected boolean isHistoryUsed() {
        return getDbSqlSession().getDbSqlSessionFactory().isDbHistoryUsed() && schemaComponentHistory != null;
    }

    protected String getSchemaVersion() {
        // The service schema version properties were introduced in 6.2.0.
        String dbVersion = getProperty(schemaVersionProperty);
        if (dbVersion == null) {
            return "6.1.2.0"; // last version before services were separated. Start upgrading from this point.
        }
        return dbVersion;
    }

}
