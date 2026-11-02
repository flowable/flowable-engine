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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableWrongDbException;
import org.flowable.common.engine.api.lock.LockManager;
import org.flowable.common.engine.impl.FlowableVersions;

public abstract class EngineSqlScriptBasedDbSchemaManager extends AbstractSqlScriptBasedDbSchemaManager {

    protected final String context;
    protected final SchemaManagerLockConfiguration lockConfiguration;

    protected EngineSqlScriptBasedDbSchemaManager(String context, SchemaManagerLockConfiguration lockConfiguration) {
        this.context = context;
        this.lockConfiguration = lockConfiguration;
    }

    protected abstract String getEngineVersion();

    protected abstract String getSchemaVersionPropertyName();

    protected abstract String getDbSchemaLockName();

    protected abstract String getEngineTableName();

    protected abstract String getChangeLogTableName();

    protected abstract String getDbVersionForChangelogVersion(String changeLogVersion);

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

        if (lockConfiguration.isUseLockForDatabaseSchemaUpdate()) {
            LockManager lockManager = lockConfiguration.getLockManager(getDbSchemaLockName());
            lockManager.waitForLockRunAndRelease(lockConfiguration.getSchemaLockWaitTime(), () -> {
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
        if (lockConfiguration.isUseLockForDatabaseSchemaUpdate()) {
            LockManager lockManager = lockConfiguration.getLockManager(getDbSchemaLockName());
            return lockManager.waitForLockRunAndRelease(lockConfiguration.getSchemaLockWaitTime(), this::schemaUpdateInLock);
        } else {
            return schemaUpdateInLock();
        }
    }

    protected String schemaUpdateInLock() {
        String feedback = null;
        boolean isUpgradeNeeded = false;
        int matchingVersionIndex = -1;

        boolean isEngineTablePresent = isEngineTablePresent();

        String dbVersion = null;
        if (isEngineTablePresent) {
            dbVersion = getDbVersion();
            if (dbVersion == null) {
                dbVersion = getChangeLogVersion().dbVersion();
            }
        }

        if (isEngineTablePresent) {
            matchingVersionIndex = FlowableVersions.getFlowableVersionIndexForDbVersion(dbVersion);
            isUpgradeNeeded = (matchingVersionIndex != (FlowableVersions.FLOWABLE_VERSIONS.size() - 1));
        }

        if (isUpgradeNeeded) {
            // Engine upgrade
            dbSchemaUpgrade(context, matchingVersionIndex, dbVersion);
            feedback = "upgraded Flowable from " + dbVersion + " to " + getEngineVersion();

        } else if (!isEngineTablePresent) {
            dbSchemaCreateEngine();
        }

        return feedback;
    }

    @Override
    public String getContext() {
        return context;
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
        return getProperty(getSchemaVersionPropertyName(), false);
    }

    protected int getChangeLogVersionOrder(String changeLogVersion) {
        return Integer.parseInt(changeLogVersion);
    }

    protected ChangeLogVersion getChangeLogVersion() {
        String changeLogTableName = getChangeLogTableName();
        if (changeLogTableName != null && isTablePresent(changeLogTableName)) {
            SchemaManagerDatabaseConfiguration databaseConfiguration = getDatabaseConfiguration();
            if (!databaseConfiguration.isTablePrefixIsSchema()) {
                changeLogTableName = prependDatabaseTablePrefix(changeLogTableName);
            }
            try (PreparedStatement statement = databaseConfiguration.getConnection()
                    .prepareStatement("select ID from " + changeLogTableName + " order by DATEEXECUTED")) {
                int latestChangeLogVersionOrder = 0;
                String changeLogVersion = null;
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        String changeLogVersionId = resultSet.getString(1);
                        int changeLogVersionOrder = getChangeLogVersionOrder(changeLogVersionId);
                        if (changeLogVersionOrder > latestChangeLogVersionOrder) {
                            // Even though we are ordering by DATEEXECUTED, and the last ID should be the last executed one.
                            // It is still possible that there are multiple entries with the same DATEEXECUTED value and the order might not be correct.
                            // e.g. MySQL 8.0 sometimes does not return the correct order.
                            changeLogVersion = changeLogVersionId;
                            latestChangeLogVersionOrder = changeLogVersionOrder;
                        }
                    }
                }
                if (changeLogVersion != null) {
                    return new ChangeLogVersion(changeLogVersion, getDbVersionForChangelogVersion(changeLogVersion));
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to get change log version from " + changeLogTableName, e);
            }
        }

        return new ChangeLogVersion(null, getDbVersionForChangelogVersion(null));
    }

    public record ChangeLogVersion(String version, String dbVersion) {
    }

}
