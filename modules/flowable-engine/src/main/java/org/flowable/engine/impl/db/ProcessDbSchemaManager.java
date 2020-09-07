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
package org.flowable.engine.impl.db;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableWrongDbException;
import org.flowable.common.engine.impl.FlowableVersions;
import org.flowable.common.engine.impl.db.AbstractSqlScriptBasedDbSchemaManager;
import org.flowable.common.engine.impl.db.DbSqlSession;
import org.flowable.common.engine.impl.db.SchemaManager;
import org.flowable.common.engine.impl.lock.LockManager;
import org.flowable.common.engine.impl.persistence.entity.PropertyEntity;
import org.flowable.common.engine.impl.persistence.entity.PropertyEntityImpl;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;

public class ProcessDbSchemaManager extends AbstractSqlScriptBasedDbSchemaManager {
    
    protected static final Pattern CLEAN_VERSION_REGEX = Pattern.compile("\\d\\.\\d*");

    protected static final String PROCESS_DB_SCHEMA_LOCK_NAME = "processDbSchemaLock";
    
    @Override
    public void schemaCheckVersion() {
        try {
            String dbVersion = getDbVersion();
            if (!ProcessEngine.VERSION.equals(dbVersion)) {
                throw new FlowableWrongDbException(ProcessEngine.VERSION, dbVersion);
            }

            String errorMessage = null;
            if (!isEngineTablePresent()) {
                errorMessage = addMissingComponent(errorMessage, "engine");
            }
            if (CommandContextUtil.getDbSqlSession().getDbSqlSessionFactory().isDbHistoryUsed() && !isHistoryTablePresent()) {
                errorMessage = addMissingComponent(errorMessage, "history");
            }

            if (errorMessage != null) {
                throw new FlowableException("Flowable database problem: " + errorMessage);
            }

        } catch (Exception e) {
            if (isMissingTablesException(e)) {
                throw new FlowableException(
                        "no flowable tables in db. set <property name=\"databaseSchemaUpdate\" to value=\"true\" or value=\"create-drop\" (use create-drop for testing only!) in bean processEngineConfiguration in flowable.cfg.xml for automatic schema creation",
                        e);
            } else {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new FlowableException("couldn't get db schema version", e);
                }
            }
        }

        logger.debug("flowable db schema check successful");
    }

    protected String addMissingComponent(String missingComponents, String component) {
        if (missingComponents == null) {
            return "Tables missing for component(s) " + component;
        }
        return missingComponents + ", " + component;
    }

    protected String getDbVersion() {
        DbSqlSession dbSqlSession = CommandContextUtil.getDbSqlSession();
        String selectSchemaVersionStatement = dbSqlSession.getDbSqlSessionFactory().mapStatement("org.flowable.common.engine.impl.persistence.entity.PropertyEntityImpl.selectDbSchemaVersion");
        return (String) dbSqlSession.getSqlSession().selectOne(selectSchemaVersionStatement);
    }
    
    @Override
    public void schemaCreate() {
        
        // The common schema manager is special and would handle its own locking mechanism
        getCommonSchemaManager().schemaCreate();

        ProcessEngineConfigurationImpl processEngineConfiguration = getProcessEngineConfiguration();
        if (processEngineConfiguration.isUseLockForDatabaseSchemaUpdate()) {
            LockManager lockManager = processEngineConfiguration.getManagementService().getLockManager(PROCESS_DB_SCHEMA_LOCK_NAME);
            lockManager.waitForLockRunAndRelease(processEngineConfiguration.getSchemaLockWaitTime(), () -> {
                schemaCreateInLock();
                return null;
            });
        } else {
            schemaCreateInLock();
        }
    }

    protected void schemaCreateInLock() {
        getIdentityLinkSchemaManager().schemaCreate();
        getEntityLinkSchemaManager().schemaCreate();
        getEventSubscriptionSchemaManager().schemaCreate();
        getTaskSchemaManager().schemaCreate();
        getVariableSchemaManager().schemaCreate();
        getJobSchemaManager().schemaCreate();
        getBatchSchemaManager().schemaCreate();
        
        if (isEngineTablePresent()) {
            String dbVersion = getDbVersion();
            if (!ProcessEngine.VERSION.equals(dbVersion)) {
                throw new FlowableWrongDbException(ProcessEngine.VERSION, dbVersion);
            }
        } else {
            dbSchemaCreateEngine();
        }
        
        if (CommandContextUtil.getDbSqlSession().getDbSqlSessionFactory().isDbHistoryUsed()) {
            dbSchemaCreateHistory();
        }
    }

    protected void dbSchemaCreateHistory() {
        executeMandatorySchemaResource("create", "history");
    }

    protected void dbSchemaCreateEngine() {
        executeMandatorySchemaResource("create", "engine");
    }

    @Override
    public void schemaDrop() {
        
        try {
            executeMandatorySchemaResource("drop", "engine");
            if (CommandContextUtil.getDbSqlSession().getDbSqlSessionFactory().isDbHistoryUsed()) {
                executeMandatorySchemaResource("drop", "history");
            }
            
        } catch (Exception e) {
            logger.info("Error dropping engine tables", e);
        }
        
        try {
            getBatchSchemaManager().schemaDrop();
        } catch (Exception e) {
            logger.info("Error dropping batch tables", e);
        }
        
        try {
            getJobSchemaManager().schemaDrop();
        } catch (Exception e) {
            logger.info("Error dropping job tables", e);
        }
     
        try {
            getVariableSchemaManager().schemaDrop();
        } catch (Exception e) {
            logger.info("Error dropping variable tables", e);
        }
        
        try {
            getTaskSchemaManager().schemaDrop();
        } catch (Exception e) {
            logger.info("Error dropping task tables", e);
        }
        
        try {
            getEventSubscriptionSchemaManager().schemaDrop();
        } catch (Exception e) {
            logger.info("Error dropping event subscription tables", e);
        }
        
        try {
            getEntityLinkSchemaManager().schemaDrop();
        } catch (Exception e) {
            logger.info("Error dropping entity link tables", e);
        }
        
        try {
            getIdentityLinkSchemaManager().schemaDrop();
        } catch (Exception e) {
            logger.info("Error dropping identity link tables", e);
        }
        
        try {
            getCommonSchemaManager().schemaDrop();
        } catch (Exception e) {
            logger.info("Error dropping common tables", e);
        }
    }

    public void dbSchemaPrune() {
        if (isHistoryTablePresent() && !CommandContextUtil.getDbSqlSession().getDbSqlSessionFactory().isDbHistoryUsed()) {
            executeMandatorySchemaResource("drop", "history");
        }
    }

    @Override
    public String schemaUpdate() {
        
        PropertyEntity dbVersionProperty = null;
        String dbVersion = null;
        String feedback = null;
        boolean isUpgradeNeeded = false;
        int matchingVersionIndex = -1;
        int version6120Index = FlowableVersions.getFlowableVersionIndexForDbVersion(FlowableVersions.LAST_V6_VERSION_BEFORE_SERVICES);

        // The common schema manager is special and would handle its own locking mechanism
        getCommonSchemaManager().schemaUpdate();

        ProcessEngineConfigurationImpl processEngineConfiguration = getProcessEngineConfiguration();
        LockManager lockManager;
        if (processEngineConfiguration.isUseLockForDatabaseSchemaUpdate()) {
            lockManager = processEngineConfiguration.getManagementService().getLockManager(PROCESS_DB_SCHEMA_LOCK_NAME);
            lockManager.waitForLock(processEngineConfiguration.getSchemaLockWaitTime());
        } else {
            lockManager = null;
        }

        try {
            DbSqlSession dbSqlSession = CommandContextUtil.getDbSqlSession();
            boolean isEngineTablePresent = isEngineTablePresent();
            if (isEngineTablePresent) {

                dbVersionProperty = dbSqlSession.selectById(PropertyEntityImpl.class, "schema.version");
                dbVersion = dbVersionProperty.getValue();

                matchingVersionIndex = FlowableVersions.getFlowableVersionIndexForDbVersion(dbVersion);
                isUpgradeNeeded = (matchingVersionIndex != (FlowableVersions.FLOWABLE_VERSIONS.size() - 1));
            }

            boolean isHistoryTablePresent = isHistoryTablePresent();
            if (isUpgradeNeeded && matchingVersionIndex < version6120Index) {
                dbSchemaUpgradeUntil6120("engine", matchingVersionIndex);

                if (isHistoryTablePresent) {
                    dbSchemaUpgradeUntil6120("history", matchingVersionIndex);
                }
            }

            getIdentityLinkSchemaManager().schemaUpdate();
            getEntityLinkSchemaManager().schemaUpdate();
            getEventSubscriptionSchemaManager().schemaUpdate();
            getTaskSchemaManager().schemaUpdate();
            getVariableSchemaManager().schemaUpdate();
            getJobSchemaManager().schemaUpdate();
            getBatchSchemaManager().schemaUpdate();

            if (isUpgradeNeeded) {
                dbVersionProperty.setValue(ProcessEngine.VERSION);

                PropertyEntity dbHistoryProperty;
                if ("5.0".equals(dbVersion)) {
                    dbHistoryProperty = CommandContextUtil.getPropertyEntityManager().create();
                    dbHistoryProperty.setName("schema.history");
                    dbHistoryProperty.setValue("create(5.0)");
                    dbSqlSession.insert(dbHistoryProperty, processEngineConfiguration.getIdGenerator());
                } else {
                    dbHistoryProperty = dbSqlSession.selectById(PropertyEntity.class, "schema.history");
                }

                // Set upgrade history
                String dbHistoryValue = "upgrade(" + dbVersion + "->" + ProcessEngine.VERSION + ")";
                dbHistoryProperty.setValue(dbHistoryValue);

                // Engine upgrade
                if (version6120Index > matchingVersionIndex) {
                    dbSchemaUpgrade("engine", version6120Index);
                } else {
                    dbSchemaUpgrade("engine", matchingVersionIndex);
                }

                feedback = "upgraded Flowable from " + dbVersion + " to " + ProcessEngine.VERSION;

            } else if (!isEngineTablePresent) {
                dbSchemaCreateEngine();
            }

            if (isHistoryTablePresent) {
                if (isUpgradeNeeded) {
                    if (version6120Index > matchingVersionIndex) {
                        dbSchemaUpgrade("history", version6120Index);
                    } else {
                        dbSchemaUpgrade("history", matchingVersionIndex);
                    }
                }

            } else if (dbSqlSession.getDbSqlSessionFactory().isDbHistoryUsed()) {
                dbSchemaCreateHistory();
            }

            return feedback;
        } finally {
            if (lockManager != null) {
                lockManager.releaseLock();
            }
        }

    }

    public boolean isEngineTablePresent() {
        return isTablePresent("ACT_RU_EXECUTION");
    }

    public boolean isHistoryTablePresent() {
        return isTablePresent("ACT_HI_PROCINST");
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
        String databaseSchemaUpdate = CommandContextUtil.getProcessEngineConfiguration().getDatabaseSchemaUpdate();
        if (org.flowable.engine.ProcessEngineConfiguration.DB_SCHEMA_UPDATE_CREATE_DROP.equals(databaseSchemaUpdate)) {
            schemaDrop();
        }
    }
    
    protected SchemaManager getCommonSchemaManager() {
        return CommandContextUtil.getProcessEngineConfiguration().getCommonSchemaManager();
    }
    
    protected SchemaManager getIdentityLinkSchemaManager() {
        return CommandContextUtil.getProcessEngineConfiguration().getIdentityLinkSchemaManager();
    }
    
    protected SchemaManager getEntityLinkSchemaManager() {
        return CommandContextUtil.getProcessEngineConfiguration().getEntityLinkSchemaManager();
    }
    
    protected SchemaManager getEventSubscriptionSchemaManager() {
        return CommandContextUtil.getProcessEngineConfiguration().getEventSubscriptionSchemaManager();
    }
    
    protected SchemaManager getVariableSchemaManager() {
        return CommandContextUtil.getProcessEngineConfiguration().getVariableSchemaManager();
    }
    
    protected SchemaManager getTaskSchemaManager() {
        return CommandContextUtil.getProcessEngineConfiguration().getTaskSchemaManager();
    }
    
    protected SchemaManager getJobSchemaManager() {
        return CommandContextUtil.getProcessEngineConfiguration().getJobSchemaManager();
    }
    
    protected SchemaManager getBatchSchemaManager() {
        return CommandContextUtil.getProcessEngineConfiguration().getBatchSchemaManager();
    }
    
    protected ProcessEngineConfigurationImpl getProcessEngineConfiguration() {
        return CommandContextUtil.getProcessEngineConfiguration();
    }

    @Override
    protected String getResourcesRootDirectory() {
        return "org/flowable/db/";
    }

}
