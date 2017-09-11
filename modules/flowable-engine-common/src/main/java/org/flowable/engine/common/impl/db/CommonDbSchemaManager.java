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
public class CommonDbSchemaManager extends AbstractSqlScriptBasedDbSchemaManager {
    
    private static final String COMMON_VERSION_PROPERTY = "common.schema.version";
    
    private static final String SCHEMA_FILE_COMPONENT = "common";
    
    @Override
    public void dbSchemaCreate() {
        if (isTablePresent(PROPERTY_TABLE)) {
            String dbVersion = getCommonSchemaVersion();
            if (!FlowableVersions.CURRENT_VERSION.equals(dbVersion)) {
                throw new FlowableWrongDbException(FlowableVersions.CURRENT_VERSION, dbVersion);
            }
        } else {
            executeMandatorySchemaResource("create", SCHEMA_FILE_COMPONENT);
        }
    }

    @Override
    public void dbSchemaDrop() {
        executeMandatorySchemaResource("drop", SCHEMA_FILE_COMPONENT);
    }

    @Override
    public String dbSchemaUpdate() {
        String feedback = null;
        if (isTablePresent(PROPERTY_TABLE)) {
            String dbVersion = getCommonSchemaVersion();
            int matchingVersionIndex = FlowableVersions.getFlowableVersionForDbVersion(dbVersion);
            boolean isUpgradeNeeded = (matchingVersionIndex != (FlowableVersions.FLOWABLE_VERSIONS.size() - 1));
            if (isUpgradeNeeded) {
                dbSchemaUpgrade(SCHEMA_FILE_COMPONENT, matchingVersionIndex);
                setProperty(COMMON_VERSION_PROPERTY, FlowableVersions.CURRENT_VERSION);
            }
            
            feedback = "upgraded from " + dbVersion + " to " + FlowableVersions.CURRENT_VERSION;
        } else {
            dbSchemaCreate();
        }
        return feedback;
    }

    protected String getCommonSchemaVersion() {
        // The 'common.schema.version' was introduced in 6.2.0.
        String dbVersion = getProperty(COMMON_VERSION_PROPERTY);
        if (dbVersion == null) {
            return "6.1.2.0"; // last version before common.schema.version was added. Start upgrading from this point.
        }
        return dbVersion;
    }
    
    @Override
    protected String getResourcesRootDirectory() {
        return "org/flowable/db/";
    }
    
}
