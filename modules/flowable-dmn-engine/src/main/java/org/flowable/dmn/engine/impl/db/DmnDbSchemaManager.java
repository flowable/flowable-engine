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

package org.flowable.dmn.engine.impl.db;

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
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.util.CommandContextUtil;

public class DmnDbSchemaManager extends AbstractSqlScriptBasedDbSchemaManager {

    protected static final Pattern CLEAN_VERSION_REGEX = Pattern.compile("\\d\\.\\d*");

    protected static final String DMN_DB_SCHEMA_LOCK_NAME = "dmnDbSchemaLock";
    
    protected static Map<String, String> changeLogVersionMap = new HashMap<>();
    
    static {
        changeLogVersionMap.put("1", "6.0.0.5");
        changeLogVersionMap.put("2", "6.1.1.0");
        changeLogVersionMap.put("3", "6.3.0.0");
        changeLogVersionMap.put("4", "6.3.1.0");
        changeLogVersionMap.put("5", "6.4.0.0");
        changeLogVersionMap.put("6", "6.4.1.3");
        changeLogVersionMap.put("7", "6.6.0.0");
        changeLogVersionMap.put("8", "6.6.0.0");
        changeLogVersionMap.put("9", "6.8.0.0");
        changeLogVersionMap.put("10", "7.1.0.0");
    }
    
    @Override
    public void schemaCheckVersion() {
        try {
            String dbVersion = getDbVersion();
            if (!DmnEngine.VERSION.equals(dbVersion)) {
                throw new FlowableWrongDbException(DmnEngine.VERSION, dbVersion);
            }

            String errorMessage = null;
            if (!isDmnTablePresent()) {
                errorMessage = addMissingComponent(errorMessage, "dmn");
            }

            if (errorMessage != null) {
                throw new FlowableException("Flowable database problem: " + errorMessage);
            }

        } catch (Exception e) {
            if (isMissingTablesException(e)) {
                throw new FlowableException(
                        "no flowable tables in db. set <property name=\"databaseSchemaUpdate\" to value=\"true\" or value=\"create-drop\" (use create-drop for testing only!) in bean dmnEngineConfiguration in flowable.dmn.cfg.xml for automatic schema creation",
                        e);
            } else {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new FlowableException("couldn't get dmn db schema version", e);
                }
            }
        }

        logger.debug("flowable dmn db schema check successful");
    }
    
    @Override
    public void schemaCreate() {
        
        // The common schema manager is special and would handle its own locking mechanism
        getCommonSchemaManager().schemaCreate();

        DmnEngineConfiguration dmnEngineConfiguration = getDmnEngineConfiguration();
        if (dmnEngineConfiguration.isUseLockForDatabaseSchemaUpdate()) {
            LockManager lockManager = dmnEngineConfiguration.getDmnManagementService().getLockManager(DMN_DB_SCHEMA_LOCK_NAME);
            lockManager.waitForLockRunAndRelease(dmnEngineConfiguration.getSchemaLockWaitTime(), () -> {
                schemaCreateInLock();
                return null;
            });
        } else {
            schemaCreateInLock();
        }
    }

    protected void schemaCreateInLock() {
        if (isDmnTablePresent()) {
            String dbVersion = getDbVersion();
            if (!DmnEngine.VERSION.equals(dbVersion)) {
                throw new FlowableWrongDbException(DmnEngine.VERSION, dbVersion);
            }
        } else {
            dbSchemaCreateDmnEngine();
        }
    }

    protected void dbSchemaCreateDmnEngine() {
        executeMandatorySchemaResource("create", "dmn");
    }

    @Override
    public void schemaDrop() {
        
        try {
            executeMandatorySchemaResource("drop", "dmn");
            
        } catch (Exception e) {
            logger.info("Error dropping dmn tables", e);
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
        boolean isDmnTablePresent = isDmnTablePresent();
        
        String mappedChangeLogVersion = null;
        if (isDmnTablePresent) {
            dbVersionProperty = dbSqlSession.selectById(PropertyEntityImpl.class, "dmn.schema.version");
            if (dbVersionProperty != null) {
                mappedChangeLogVersion = dbVersionProperty.getValue();
            } else {
                String changeLogVersion = getChangeLogVersion();
                if (StringUtils.isNotEmpty(changeLogVersion) && changeLogVersionMap.containsKey(changeLogVersion)) {
                    mappedChangeLogVersion = changeLogVersionMap.get(changeLogVersion);
                } else {
                    mappedChangeLogVersion = "5.99.0.0";
                }
            }
        }
        
        // The common schema manager is special and would handle its own locking mechanism
        getCommonSchemaManager().schemaUpdate(mappedChangeLogVersion);

        DmnEngineConfiguration dmnEngineConfiguration = getDmnEngineConfiguration();
        LockManager lockManager;
        if (dmnEngineConfiguration.isUseLockForDatabaseSchemaUpdate()) {
            lockManager = dmnEngineConfiguration.getDmnManagementService().getLockManager(DMN_DB_SCHEMA_LOCK_NAME);
            lockManager.waitForLock(dmnEngineConfiguration.getSchemaLockWaitTime());
        } else {
            lockManager = null;
        }

        try {
            if (isDmnTablePresent) {
                matchingVersionIndex = FlowableVersions.getFlowableVersionIndexForDbVersion(mappedChangeLogVersion);
                isUpgradeNeeded = (matchingVersionIndex != (FlowableVersions.FLOWABLE_VERSIONS.size() - 1));
            }

            if (isUpgradeNeeded) {
                // Engine upgrade
                dbSchemaUpgrade("dmn", matchingVersionIndex, mappedChangeLogVersion);

                feedback = "upgraded Flowable from " + mappedChangeLogVersion + " to " + DmnEngine.VERSION;

            } else if (!isDmnTablePresent) {
                dbSchemaCreateDmnEngine();
            }

            return feedback;
        } finally {
            if (lockManager != null) {
                lockManager.releaseLock();
            }
        }

    }

    public boolean isDmnTablePresent() {
        return isTablePresent("ACT_DMN_DECISION");
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
    
    protected String getChangeLogVersion() {
        if (isTablePresent("ACT_DMN_DATABASECHANGELOG")) {
            DbSqlSession dbSqlSession = CommandContextUtil.getDbSqlSession();
            String selectChangeLogVersionsStatement = dbSqlSession.getDbSqlSessionFactory().mapStatement("org.flowable.common.engine.impl.persistence.entity.ChangeLogEntityImpl.selectDmnChangeLogVersions");
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
        String databaseSchemaUpdate = CommandContextUtil.getDmnEngineConfiguration().getDatabaseSchemaUpdate();
        if (DmnEngineConfiguration.DB_SCHEMA_UPDATE_CREATE_DROP.equals(databaseSchemaUpdate)) {
            schemaDrop();
        }
    }
    
    protected SchemaManager getCommonSchemaManager() {
        return CommandContextUtil.getDmnEngineConfiguration().getCommonSchemaManager();
    }
    
    protected DmnEngineConfiguration getDmnEngineConfiguration() {
        return CommandContextUtil.getDmnEngineConfiguration();
    }

    @Override
    protected String getResourcesRootDirectory() {
        return "org/flowable/dmn/db/";
    }
}
