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
package org.flowable.app.engine.impl.db;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.flowable.app.engine.AppEngineConfiguration;
import org.flowable.app.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableWrongDbException;
import org.flowable.common.engine.api.lock.LockManager;
import org.flowable.common.engine.impl.FlowableVersions;
import org.flowable.common.engine.impl.db.AbstractSqlScriptBasedDbSchemaManager;
import org.flowable.common.engine.impl.db.DbSqlSession;
import org.flowable.common.engine.impl.db.SchemaManager;
import org.flowable.common.engine.impl.persistence.entity.PropertyEntity;
import org.flowable.common.engine.impl.persistence.entity.PropertyEntityImpl;

public class AppDbSchemaManager extends AbstractSqlScriptBasedDbSchemaManager {
    
    protected static final Pattern CLEAN_VERSION_REGEX = Pattern.compile("\\d\\.\\d*");

    protected static final String APP_DB_SCHEMA_LOCK_NAME = "appDbSchemaLock";
    
    @Override
    public void schemaCheckVersion() {
        try {
            String dbVersion = getDbVersion();
            if (!FlowableVersions.CURRENT_VERSION.equals(dbVersion)) {
                throw new FlowableWrongDbException(FlowableVersions.CURRENT_VERSION, dbVersion);
            }

            String errorMessage = null;
            if (!isTablePresent()) {
                errorMessage = addMissingComponent(errorMessage, "app");
            }

            if (errorMessage != null) {
                throw new FlowableException("Flowable app database problem: " + errorMessage);
            }

        } catch (Exception e) {
            if (isMissingTablesException(e)) {
                throw new FlowableException(
                        "no flowable tables in db. set <property name=\"databaseSchemaUpdate\" to value=\"true\" or value=\"create-drop\" (use create-drop for testing only!) in bean appEngineConfiguration in flowable.app.cfg.xml for automatic schema creation",
                        e);
            } else {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new FlowableException("couldn't get app db schema version", e);
                }
            }
        }

        logger.debug("flowable app db schema check successful");
    }

    protected String addMissingComponent(String missingComponents, String component) {
        if (missingComponents == null) {
            return "Tables missing for component(s) " + component;
        }
        return missingComponents + ", " + component;
    }

    protected String getDbVersion() {
        DbSqlSession dbSqlSession = CommandContextUtil.getDbSqlSession();
        String selectSchemaVersionStatement = dbSqlSession.getDbSqlSessionFactory().mapStatement("org.flowable.common.engine.impl.persistence.entity.PropertyEntityImpl.selectPropertyValue");
        return (String) dbSqlSession.getSqlSession().selectOne(selectSchemaVersionStatement, "app.schema.version");
    }
    
    @Override
    public void schemaCreate() {
        
        // The common schema manager is special and would handle its own locking mechanism
        getCommonSchemaManager().schemaCreate();

        AppEngineConfiguration appEngineConfiguration = getAppEngineConfiguration();
        if (appEngineConfiguration.isUseLockForDatabaseSchemaUpdate()) {
            LockManager lockManager = appEngineConfiguration.getAppManagementService().getLockManager(APP_DB_SCHEMA_LOCK_NAME);
            lockManager.waitForLockRunAndRelease(appEngineConfiguration.getSchemaLockWaitTime(), () -> {
                schemaCreateInLock();
                return null;
            });
        } else {
            schemaCreateInLock();
        }
    }

    protected void schemaCreateInLock() {
        if (isTablePresent()) {
            String dbVersion = getDbVersion();
            if (!FlowableVersions.CURRENT_VERSION.equals(dbVersion)) {
                throw new FlowableWrongDbException(FlowableVersions.CURRENT_VERSION, dbVersion);
            }
        } else {
            dbSchemaCreateAppEngine();
        }
    }

    protected void dbSchemaCreateAppEngine() {
        executeMandatorySchemaResource("create", "app");
    }

    @Override
    public void schemaDrop() {
        
        try {
            executeMandatorySchemaResource("drop", "app");
            
        } catch (Exception e) {
            logger.info("Error dropping app engine tables", e);
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
        String dbVersion = null;
        String feedback = null;
        boolean isUpgradeNeeded = false;
        int matchingVersionIndex = -1;
        int version6120Index = FlowableVersions.getFlowableVersionIndexForDbVersion(FlowableVersions.LAST_V6_VERSION_BEFORE_SERVICES);

        DbSqlSession dbSqlSession = CommandContextUtil.getDbSqlSession();
        boolean isTablePresent = isTablePresent();
        if (isTablePresent) {
            dbVersionProperty = dbSqlSession.selectById(PropertyEntityImpl.class, "app.schema.version");
            dbVersion = dbVersionProperty.getValue();
        }
        
        // The common schema manager is special and would handle its own locking mechanism
        getCommonSchemaManager().schemaUpdate(dbVersion);

        AppEngineConfiguration appEngineConfiguration = getAppEngineConfiguration();
        LockManager lockManager;
        if (appEngineConfiguration.isUseLockForDatabaseSchemaUpdate()) {
            lockManager = appEngineConfiguration.getAppManagementService().getLockManager(APP_DB_SCHEMA_LOCK_NAME);
            lockManager.waitForLock(appEngineConfiguration.getSchemaLockWaitTime());
        } else {
            lockManager = null;
        }

        try {
            if (isTablePresent) {
                dbVersionProperty = dbSqlSession.selectById(PropertyEntityImpl.class, "app.schema.version");
                dbVersion = dbVersionProperty.getValue();

                matchingVersionIndex = FlowableVersions.getFlowableVersionIndexForDbVersion(dbVersion);
                isUpgradeNeeded = (matchingVersionIndex != (FlowableVersions.FLOWABLE_VERSIONS.size() - 1));
            }

            if (isUpgradeNeeded && matchingVersionIndex < version6120Index) {
                dbSchemaUpgradeUntil6120("engine", matchingVersionIndex, dbVersion);
            }

            if (isUpgradeNeeded) {
                dbVersionProperty.setValue(FlowableVersions.CURRENT_VERSION);

                
                // App engine upgrade
                if (version6120Index > matchingVersionIndex) {
                    dbSchemaUpgrade("engine", version6120Index, dbVersion);
                } else {
                    dbSchemaUpgrade("engine", matchingVersionIndex, dbVersion);
                }

                feedback = "upgraded Flowable from " + dbVersion + " to " + FlowableVersions.CURRENT_VERSION;

            } else if (!isTablePresent) {
                dbSchemaCreateAppEngine();
            }

            return feedback;
            
        } finally {
            if (lockManager != null) {
                lockManager.releaseLock();
            }
        }

    }

    public boolean isTablePresent() {
        return isTablePresent("ACT_APP_DEPLOYMENT");
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
        String databaseSchemaUpdate = CommandContextUtil.getAppEngineConfiguration().getDatabaseSchemaUpdate();
        if (AppEngineConfiguration.DB_SCHEMA_UPDATE_CREATE_DROP.equals(databaseSchemaUpdate)) {
            schemaDrop();
        }
    }
    
    protected SchemaManager getCommonSchemaManager() {
        return CommandContextUtil.getAppEngineConfiguration().getCommonSchemaManager();
    }
    
    protected AppEngineConfiguration getAppEngineConfiguration() {
        return CommandContextUtil.getAppEngineConfiguration();
    }

    @Override
    protected String getResourcesRootDirectory() {
        return "org/flowable/app/db/";
    }
}
