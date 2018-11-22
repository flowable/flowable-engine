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
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.impl.persistence.entity.PropertyEntity;
import org.flowable.engine.impl.persistence.entity.PropertyEntityImpl;
import org.flowable.engine.impl.util.CommandContextUtil;

public class ProcessDbSchemaManager extends AbstractSqlScriptBasedDbSchemaManager {
    
    protected static final Pattern CLEAN_VERSION_REGEX = Pattern.compile("\\d\\.\\d*");
    
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
        String selectSchemaVersionStatement = dbSqlSession.getDbSqlSessionFactory().mapStatement("org.flowable.engine.impl.persistence.entity.PropertyEntityImpl.selectDbSchemaVersion");
        return (String) dbSqlSession.getSqlSession().selectOne(selectSchemaVersionStatement);
    }
    
    @Override
    public void schemaCreate() {
        
        getCommonSchemaManager().schemaCreate();
        getIdentityLinkSchemaManager().schemaCreate();
        getEntityLinkSchemaManager().schemaCreate();
        getTaskSchemaManager().schemaCreate();
        getVariableSchemaManager().schemaCreate();
        getJobSchemaManager().schemaCreate();
        
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
            getIdentityLinkSchemaManager().schemaDrop();
        } catch (Exception e) {
            logger.info("Error dropping identity link tables", e);
        }
        
        try {
            getEntityLinkSchemaManager().schemaDrop();
        } catch (Exception e) {
            logger.info("Error dropping entity link tables", e);
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
        
        getCommonSchemaManager().schemaUpdate();
        getIdentityLinkSchemaManager().schemaUpdate();
        getEntityLinkSchemaManager().schemaUpdate();
        getTaskSchemaManager().schemaUpdate();
        getVariableSchemaManager().schemaUpdate();
        getJobSchemaManager().schemaUpdate();

        if (isUpgradeNeeded) {
            dbVersionProperty.setValue(ProcessEngine.VERSION);

            PropertyEntity dbHistoryProperty;
            if ("5.0".equals(dbVersion)) {
                dbHistoryProperty = CommandContextUtil.getPropertyEntityManager().create();
                dbHistoryProperty.setName("schema.history");
                dbHistoryProperty.setValue("create(5.0)");
                dbSqlSession.insert(dbHistoryProperty);
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
    }

    public boolean isEngineTablePresent() {
        return isTablePresent("ACT_RU_EXECUTION");
    }

    public boolean isHistoryTablePresent() {
        return isTablePresent("ACT_HI_PROCINST");
    }

    protected boolean isUpgradeNeeded(String versionInDatabase) {
        if (ProcessEngine.VERSION.equals(versionInDatabase)) {
            return false;
        }

        String cleanDbVersion = getCleanVersion(versionInDatabase);
        String[] cleanDbVersionSplitted = cleanDbVersion.split("\\.");
        int dbMajorVersion = Integer.valueOf(cleanDbVersionSplitted[0]);
        int dbMinorVersion = Integer.valueOf(cleanDbVersionSplitted[1]);

        String cleanEngineVersion = getCleanVersion(ProcessEngine.VERSION);
        String[] cleanEngineVersionSplitted = cleanEngineVersion.split("\\.");
        int engineMajorVersion = Integer.valueOf(cleanEngineVersionSplitted[0]);
        int engineMinorVersion = Integer.valueOf(cleanEngineVersionSplitted[1]);

        if ((dbMajorVersion > engineMajorVersion) || ((dbMajorVersion <= engineMajorVersion) && (dbMinorVersion > engineMinorVersion))) {
            throw new FlowableException("Version of flowable database (" + versionInDatabase + ") is more recent than the engine (" + ProcessEngine.VERSION + ")");
        } else if (cleanDbVersion.compareTo(cleanEngineVersion) == 0) {
            // Versions don't match exactly, possibly snapshot is being used
            logger.warn("Engine-version is the same, but not an exact match: {} vs. {}. Not performing database-upgrade.", versionInDatabase, ProcessEngine.VERSION);
            return false;
        }
        return true;
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
            if ((exceptionMessage.indexOf("Table") != -1) && (exceptionMessage.indexOf("not found") != -1)) {
                return true;
            }

            // Message returned from MySQL and Oracle
            if ((exceptionMessage.indexOf("Table") != -1 || exceptionMessage.indexOf("table") != -1) && (exceptionMessage.indexOf("doesn't exist") != -1)) {
                return true;
            }

            // Message returned from Postgres
            if ((exceptionMessage.indexOf("relation") != -1 || exceptionMessage.indexOf("table") != -1) && (exceptionMessage.indexOf("does not exist") != -1)) {
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
    
    protected SchemaManager getVariableSchemaManager() {
        return CommandContextUtil.getProcessEngineConfiguration().getVariableSchemaManager();
    }
    
    protected SchemaManager getTaskSchemaManager() {
        return CommandContextUtil.getProcessEngineConfiguration().getTaskSchemaManager();
    }
    
    protected SchemaManager getJobSchemaManager() {
        return CommandContextUtil.getProcessEngineConfiguration().getJobSchemaManager();
    }
    
    @Override
    protected String getResourcesRootDirectory() {
        return "org/flowable/db/";
    }

}
