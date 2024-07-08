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
package org.flowable.eventregistry.impl.db;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableWrongDbException;
import org.flowable.common.engine.api.lock.LockManager;
import org.flowable.common.engine.impl.FlowableVersions;
import org.flowable.common.engine.impl.db.AbstractSqlScriptBasedDbSchemaManager;
import org.flowable.common.engine.impl.db.DbSqlSession;
import org.flowable.common.engine.impl.db.SchemaManager;
import org.flowable.common.engine.impl.persistence.entity.ChangeLogEntity;
import org.flowable.common.engine.impl.persistence.entity.PropertyEntity;
import org.flowable.common.engine.impl.persistence.entity.PropertyEntityImpl;
import org.flowable.eventregistry.impl.EventRegistryEngine;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.impl.cmd.UpdateChannelDefinitionTypeAndImplementationForAllChannelDefinitionsCmd;
import org.flowable.eventregistry.impl.util.CommandContextUtil;

public class EventDbSchemaManager extends AbstractSqlScriptBasedDbSchemaManager {

    protected static final Pattern CLEAN_VERSION_REGEX = Pattern.compile("\\d\\.\\d*");

    protected static final String EVENTREGISTRY_DB_SCHEMA_LOCK_NAME = "eventRegistryDbSchemaLock";
    
    protected static Map<String, String> changeLogVersionMap = new HashMap<>();
    
    static {
        changeLogVersionMap.put("1", "6.5.0.6");
        changeLogVersionMap.put("2", "6.7.2.0");
        changeLogVersionMap.put("3", "6.7.2.0");
        changeLogVersionMap.put("4", "7.1.0.0");
        changeLogVersionMap.put("5", "7.1.0.0");
    }
    
    @Override
    public void schemaCheckVersion() {
        try {
            String dbVersion = getDbVersion();
            if (!EventRegistryEngine.VERSION.equals(dbVersion)) {
                throw new FlowableWrongDbException(EventRegistryEngine.VERSION, dbVersion);
            }

            String errorMessage = null;
            if (!isEventRegistryTablePresent()) {
                errorMessage = addMissingComponent(errorMessage, "eventregistry");
            }

            if (errorMessage != null) {
                throw new FlowableException("Flowable database problem: " + errorMessage);
            }

        } catch (Exception e) {
            if (isMissingTablesException(e)) {
                throw new FlowableException(
                        "no flowable tables in db. set <property name=\"databaseSchemaUpdate\" to value=\"true\" or value=\"create-drop\" (use create-drop for testing only!) in bean dmnEngineConfiguration in flowable.eventregistry.cfg.xml for automatic schema creation",
                        e);
            } else {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new FlowableException("couldn't get eventregistry db schema version", e);
                }
            }
        }

        logger.debug("flowable eventregistry db schema check successful");
    }
    
    @Override
    public void schemaCreate() {
        
        // The common schema manager is special and would handle its own locking mechanism
        getCommonSchemaManager().schemaCreate();

        EventRegistryEngineConfiguration eventRegistryConfiguration = getEventRegistryConfiguration();
        if (eventRegistryConfiguration.isUseLockForDatabaseSchemaUpdate()) {
            LockManager lockManager = eventRegistryConfiguration.getEventManagementService().getLockManager(EVENTREGISTRY_DB_SCHEMA_LOCK_NAME);
            lockManager.waitForLockRunAndRelease(eventRegistryConfiguration.getSchemaLockWaitTime(), () -> {
                schemaCreateInLock();
                return null;
            });
        } else {
            schemaCreateInLock();
        }
    }

    protected void schemaCreateInLock() {
        if (isEventRegistryTablePresent()) {
            String dbVersion = getDbVersion();
            if (!EventRegistryEngine.VERSION.equals(dbVersion)) {
                throw new FlowableWrongDbException(EventRegistryEngine.VERSION, dbVersion);
            }
        } else {
            dbSchemaCreateDmnEngine();
        }
    }

    protected void dbSchemaCreateDmnEngine() {
        executeMandatorySchemaResource("create", "eventregistry");
    }

    @Override
    public void schemaDrop() {
        
        try {
            executeMandatorySchemaResource("drop", "eventregistry");
            
        } catch (Exception e) {
            logger.info("Error dropping eventregistry tables", e);
        }
        
        try {
            getCommonSchemaManager().schemaDrop();
        } catch (Exception e) {
            logger.info("Error dropping common tables", e);
        }
    }

    @Override
    public String schemaUpdate() {
        
        PropertyEntity dbVersionProperty = null;
        String feedback = null;
        boolean isUpgradeNeeded = false;
        int matchingVersionIndex = -1;
        
        DbSqlSession dbSqlSession = CommandContextUtil.getDbSqlSession();
        boolean isEventRegistryTablePresent = isEventRegistryTablePresent();
        
        String mappedChangeLogVersion = null;
        String changeLogVersion = null;
        if (isEventRegistryTablePresent) {
            dbVersionProperty = dbSqlSession.selectById(PropertyEntityImpl.class, "eventregistry.schema.version");
            if (dbVersionProperty != null) {
                mappedChangeLogVersion = dbVersionProperty.getValue();
            } else {
                changeLogVersion = getChangeLogVersion();
                if (StringUtils.isNotEmpty(changeLogVersion) && changeLogVersionMap.containsKey(changeLogVersion)) {
                    mappedChangeLogVersion = changeLogVersionMap.get(changeLogVersion);
                } else {
                    mappedChangeLogVersion = "6.5.0.0";
                }
            }
        }
        
        // The common schema manager is special and would handle its own locking mechanism
        getCommonSchemaManager().schemaUpdate(mappedChangeLogVersion);

        EventRegistryEngineConfiguration eventRegistryConfiguration = getEventRegistryConfiguration();
        LockManager lockManager;
        if (eventRegistryConfiguration.isUseLockForDatabaseSchemaUpdate()) {
            lockManager = eventRegistryConfiguration.getEventManagementService().getLockManager(EVENTREGISTRY_DB_SCHEMA_LOCK_NAME);
            lockManager.waitForLock(eventRegistryConfiguration.getSchemaLockWaitTime());
        } else {
            lockManager = null;
        }

        try {
            if (isEventRegistryTablePresent) {
                matchingVersionIndex = FlowableVersions.getFlowableVersionIndexForDbVersion(mappedChangeLogVersion);
                isUpgradeNeeded = (matchingVersionIndex != (FlowableVersions.FLOWABLE_VERSIONS.size() - 1));
            }

            if (isUpgradeNeeded) {
                // Engine upgrade
                dbSchemaUpgrade("eventregistry", matchingVersionIndex, mappedChangeLogVersion);
                
                if (changeLogVersion != null && ("1".equals(changeLogVersion) || "2".equals(changeLogVersion))) {
                    eventRegistryConfiguration.getCommandExecutor().execute(new UpdateChannelDefinitionTypeAndImplementationForAllChannelDefinitionsCmd());
                }

                feedback = "upgraded Flowable from " + mappedChangeLogVersion + " to " + EventRegistryEngine.VERSION;

            } else if (!isEventRegistryTablePresent) {
                dbSchemaCreateDmnEngine();
            }

            return feedback;
        } finally {
            if (lockManager != null) {
                lockManager.releaseLock();
            }
        }

    }

    public boolean isEventRegistryTablePresent() {
        return isTablePresent("FLW_EVENT_DEFINITION");
    }
    
    protected String addMissingComponent(String missingComponents, String component) {
        if (missingComponents == null) {
            return "Tables missing for component(s) " + component;
        }
        return missingComponents + ", " + component;
    }

    protected String getDbVersion() {
        DbSqlSession dbSqlSession = CommandContextUtil.getDbSqlSession();
        String selectSchemaVersionStatement = dbSqlSession.getDbSqlSessionFactory().mapStatement("org.flowable.common.engine.impl.persistence.entity.PropertyEntityImpl.selectEventRegistryDbSchemaVersion");
        return (String) dbSqlSession.getSqlSession().selectOne(selectSchemaVersionStatement);
    }
    
    protected String getChangeLogVersion() {
        if (isTablePresent("FLW_EV_DATABASECHANGELOG")) {
            DbSqlSession dbSqlSession = CommandContextUtil.getDbSqlSession();
            String selectChangeLogVersionsStatement = dbSqlSession.getDbSqlSessionFactory().mapStatement("org.flowable.common.engine.impl.persistence.entity.ChangeLogEntityImpl.selectEventRegistryChangeLogVersions");
            List<ChangeLogEntity> changeLogItems = dbSqlSession.getSqlSession().selectList(selectChangeLogVersionsStatement);
            if (changeLogItems != null && !changeLogItems.isEmpty()) {
                ChangeLogEntity lastExecutedItem = changeLogItems.get(changeLogItems.size() - 1);
                return lastExecutedItem.getId();
            }
        }
        
        return null;
    }

    protected String getCleanVersion(String versionString) {
        Matcher matcher = CLEAN_VERSION_REGEX.matcher(versionString);
        if (!matcher.find()) {
            throw new FlowableException("Illegal format for version: " + versionString);
        }

        String cleanString = matcher.group();
        try {
            Double.parseDouble(cleanString); // try to parse it, to see if it is
                                             // really a number
            return cleanString;
        } catch (NumberFormatException nfe) {
            throw new FlowableException("Illegal format for version: " + versionString, nfe);
        }
    }

    protected boolean isMissingTablesException(Exception e) {
        String exceptionMessage = e.getMessage();
        if (e.getMessage() != null) {
            // Matches message returned from H2
            if ((exceptionMessage.contains("Table")) && (exceptionMessage.contains("not found"))) {
                return true;
            }

            // Message returned from MySQL and Oracle
            if ((exceptionMessage.contains("Table") || exceptionMessage.contains("table")) && (exceptionMessage.contains("doesn't exist"))) {
                return true;
            }

            // Message returned from Postgres
            if ((exceptionMessage.contains("relation") || exceptionMessage.contains("table")) && (exceptionMessage.contains("does not exist"))) {
                return true;
            }
        }
        return false;
    }

    public void performSchemaOperationsProcessEngineClose() {
        String databaseSchemaUpdate = CommandContextUtil.getEventRegistryConfiguration().getDatabaseSchemaUpdate();
        if (EventRegistryEngineConfiguration.DB_SCHEMA_UPDATE_CREATE_DROP.equals(databaseSchemaUpdate)) {
            schemaDrop();
        }
    }
    
    protected SchemaManager getCommonSchemaManager() {
        return CommandContextUtil.getEventRegistryConfiguration().getCommonSchemaManager();
    }
    
    protected EventRegistryEngineConfiguration getEventRegistryConfiguration() {
        return CommandContextUtil.getEventRegistryConfiguration();
    }

    @Override
    protected String getResourcesRootDirectory() {
        return "org/flowable/eventregistry/db/";
    }

}
