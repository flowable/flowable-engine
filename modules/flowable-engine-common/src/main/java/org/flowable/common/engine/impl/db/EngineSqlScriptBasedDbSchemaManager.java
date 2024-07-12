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
package org.flowable.common.engine.impl.db;

import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableWrongDbException;
import org.flowable.common.engine.api.lock.LockManager;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.FlowableVersions;
import org.flowable.common.engine.impl.persistence.entity.PropertyEntity;
import org.flowable.common.engine.impl.persistence.entity.PropertyEntityImpl;

public abstract class EngineSqlScriptBasedDbSchemaManager extends AbstractSqlScriptBasedDbSchemaManager {

    protected final String context;

    protected EngineSqlScriptBasedDbSchemaManager(String context) {
        this.context = context;
    }

    protected abstract String getEngineVersion();

    protected abstract String getSchemaVersionPropertyName();

    protected abstract String getDbSchemaLockName();

    protected abstract String getEngineTableName();

    protected abstract String getChangeLogTableName();
    
    protected abstract String getChangeLogTablePrefixName();

    protected abstract String getDbVersionForChangelogVersion(String changeLogVersion);

    protected abstract AbstractEngineConfiguration getEngineConfiguration();

    @Override
    public void schemaCheckVersion() {
        try {
            String dbVersion = getDbVersion();
            String currentVersion = getEngineVersion();
            if (!currentVersion.equals(dbVersion)) {
                throw new FlowableWrongDbException(currentVersion, dbVersion);
            }

            String errorMessage = null;
            if (!isEngineTablePresent()) {
                errorMessage = addMissingComponent(errorMessage, context);
            }

            if (errorMessage != null) {
                throw new FlowableException("Flowable database problem: " + errorMessage);
            }

        } catch (Exception e) {
            if (isMissingTablesException(e)) {
                throw new FlowableException(
                        "No flowable tables in DB. Set property \"databaseSchemaUpdate\" \"true\" or value=\"create-drop\" (use create-drop for testing only!) for automatic schema creation",
                        e);
            } else {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new FlowableException("couldn't get " + context + " db schema version", e);
                }
            }
        }

        logger.debug("flowable {} db schema check successful", context);
    }

    @Override
    public void schemaCreate() {

        AbstractEngineConfiguration engineConfiguration = getEngineConfiguration();
        if (engineConfiguration.isUseLockForDatabaseSchemaUpdate()) {
            LockManager lockManager = engineConfiguration.getLockManager(getDbSchemaLockName());
            lockManager.waitForLockRunAndRelease(engineConfiguration.getSchemaLockWaitTime(), () -> {
                schemaCreateInLock();
                return null;
            });
        } else {
            schemaCreateInLock();
        }
    }

    protected void schemaCreateInLock() {
        if (isEngineTablePresent()) {
            String dbVersion = getDbVersion();
            String engineVersion = getEngineVersion();
            if (!engineVersion.equals(dbVersion)) {
                throw new FlowableWrongDbException(engineVersion, dbVersion);
            }
        } else {
            dbSchemaCreateEngine();
        }
    }

    protected void dbSchemaCreateEngine() {
        executeMandatorySchemaResource("create", context);
    }

    @Override
    public void schemaDrop() {

        try {
            executeMandatorySchemaResource("drop", context);

        } catch (Exception e) {
            logger.info("Error dropping {} tables", context, e);
        }

    }

    @Override
    public String schemaUpdate() {

        PropertyEntity dbVersionProperty = null;
        String feedback = null;
        boolean isUpgradeNeeded = false;
        int matchingVersionIndex = -1;

        DbSqlSession dbSqlSession = getDbSqlSession();
        boolean isEngineTablePresent = isEngineTablePresent();

        ChangeLogVersion changeLogVersion = null;
        String dbVersion = null;
        if (isEngineTablePresent) {
            dbVersionProperty = dbSqlSession.selectById(PropertyEntityImpl.class, getSchemaVersionPropertyName());
            if (dbVersionProperty != null) {
                dbVersion = dbVersionProperty.getValue();
            } else {
                changeLogVersion = getChangeLogVersion();
                dbVersion = changeLogVersion.dbVersion();
            }
        }

        AbstractEngineConfiguration engineConfiguration = getEngineConfiguration();
        LockManager lockManager;
        if (engineConfiguration.isUseLockForDatabaseSchemaUpdate()) {
            lockManager = engineConfiguration.getLockManager(getDbSchemaLockName());
            lockManager.waitForLock(engineConfiguration.getSchemaLockWaitTime());
        } else {
            lockManager = null;
        }

        try {
            if (isEngineTablePresent) {
                matchingVersionIndex = FlowableVersions.getFlowableVersionIndexForDbVersion(dbVersion);
                isUpgradeNeeded = (matchingVersionIndex != (FlowableVersions.FLOWABLE_VERSIONS.size() - 1));
            }

            if (isUpgradeNeeded) {
                // Engine upgrade
                dbSchemaUpgrade(context, matchingVersionIndex, dbVersion);
                dbSchemaUpgraded(changeLogVersion);

                feedback = "upgraded Flowable from " + dbVersion + " to " + getEngineVersion();

            } else if (!isEngineTablePresent) {
                dbSchemaCreateEngine();
            }

            return feedback;
            
        } finally {
            if (lockManager != null) {
                lockManager.releaseLock();
            }
        }
    }

    @Override
    public String getContext() {
        return context;
    }

    protected void dbSchemaUpgraded(ChangeLogVersion changeLogVersion) {

    }

    public boolean isEngineTablePresent() {
        return isTablePresent(getEngineTableName());
    }

    protected String addMissingComponent(String missingComponents, String component) {
        if (missingComponents == null) {
            return "Tables missing for component(s) " + component;
        }
        return missingComponents + ", " + component;
    }

    protected String getDbVersion() {
        DbSqlSession dbSqlSession = getDbSqlSession();
        String selectSchemaVersionStatement = dbSqlSession.getDbSqlSessionFactory()
                .mapStatement("org.flowable.common.engine.impl.persistence.entity.PropertyEntityImpl.selectPropertyValue");
        return dbSqlSession.getSqlSession().selectOne(selectSchemaVersionStatement, getSchemaVersionPropertyName());
    }

    protected ChangeLogVersion getChangeLogVersion() {
        String changeLogTableName = getChangeLogTableName();
        if (changeLogTableName != null && isTablePresent(changeLogTableName)) {
            DbSqlSession dbSqlSession = getDbSqlSession();
            String selectChangeLogVersionsStatement = dbSqlSession.getDbSqlSessionFactory().mapStatement("org.flowable.common.engine.impl.persistence.change.ChangeLog.selectFlowableChangeLogVersions");
            List<String> changeLogIds = dbSqlSession.getSqlSession().selectList(selectChangeLogVersionsStatement, getChangeLogTablePrefixName());
            if (changeLogIds != null && !changeLogIds.isEmpty()) {
                String changeLogVersion = changeLogIds.get(changeLogIds.size() - 1);
                return new ChangeLogVersion(changeLogVersion, getDbVersionForChangelogVersion(changeLogVersion));
            }
        }

        return new ChangeLogVersion(null, getDbVersionForChangelogVersion(null));
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

    public record ChangeLogVersion(String version, String dbVersion) {
    }

}
