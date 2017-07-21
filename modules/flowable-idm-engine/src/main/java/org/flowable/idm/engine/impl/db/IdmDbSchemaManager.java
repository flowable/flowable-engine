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
package org.flowable.idm.engine.impl.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.FlowableWrongDbException;
import org.flowable.engine.common.impl.db.DbSchemaManager;
import org.flowable.engine.common.impl.db.DbSqlSession;
import org.flowable.engine.common.impl.db.DbSqlSessionFactory;
import org.flowable.engine.common.impl.util.IoUtil;
import org.flowable.engine.common.impl.util.ReflectUtil;
import org.flowable.idm.engine.IdmEngine;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.idm.engine.impl.persistence.entity.IdmPropertyEntity;
import org.flowable.idm.engine.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdmDbSchemaManager implements DbSchemaManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(IdmDbSchemaManager.class);
    
    protected static final Pattern CLEAN_VERSION_REGEX = Pattern.compile("\\d\\.\\d*");

    protected static final List<FlowableIdmVersion> FLOWABLE_IDM_VERSIONS = new ArrayList<>();

    static {

        /* Previous */
        FLOWABLE_IDM_VERSIONS.add(new FlowableIdmVersion("0"));
        FLOWABLE_IDM_VERSIONS.add(new FlowableIdmVersion("6.0.0.0"));
        FLOWABLE_IDM_VERSIONS.add(new FlowableIdmVersion("6.0.1.0"));
        FLOWABLE_IDM_VERSIONS.add(new FlowableIdmVersion("6.1.0.0"));
        FLOWABLE_IDM_VERSIONS.add(new FlowableIdmVersion("6.1.1.0"));

        /* Current */
        FLOWABLE_IDM_VERSIONS.add(new FlowableIdmVersion(IdmEngine.VERSION));
    }

    public static String[] JDBC_METADATA_TABLE_TYPES = { "TABLE" };
    
    public void dbSchemaCheckVersion() {
        try {
            String dbVersion = getDbVersion();
            if (!IdmEngine.VERSION.equals(dbVersion)) {
                throw new FlowableWrongDbException(IdmEngine.VERSION, dbVersion);
            }

            String errorMessage = null;
            if (!isIdmPropertyTablePresent()) {
                errorMessage = addMissingComponent(errorMessage, "engine");
            }

            if (errorMessage != null) {
                throw new FlowableException("Flowable IDM database problem: " + errorMessage);
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

        LOGGER.debug("flowable idm db schema check successful");
    }

    protected String addMissingComponent(String missingComponents, String component) {
        if (missingComponents == null) {
            return "Tables missing for component(s) " + component;
        }
        return missingComponents + ", " + component;
    }

    protected String getDbVersion() {
        DbSqlSession dbSqlSession = CommandContextUtil.getDbSqlSession();
        String selectSchemaVersionStatement = dbSqlSession.getDbSqlSessionFactory().mapStatement("selectIdmDbSchemaVersion");
        return (String) dbSqlSession.getSqlSession().selectOne(selectSchemaVersionStatement);
    }

    public void dbSchemaCreate() {
        if (isIdmPropertyTablePresent()) {
            String dbVersion = getDbVersion();
            if (!IdmEngine.VERSION.equals(dbVersion)) {
                throw new FlowableWrongDbException(IdmEngine.VERSION, dbVersion);
            }
        } else {
            // User and Group tables can already have been created by the process engine in an earlier version
            if (isIdmGroupTablePresent()) {
                // Engine upgrade
                dbSchemaUpgrade("engine", 0);

            } else {
                dbSchemaCreateIdmEngine();
            }
        }
    }

    protected void dbSchemaCreateIdmEngine() {
        executeMandatorySchemaResource("create", "identity");
    }

    public void dbSchemaDrop() {
        executeMandatorySchemaResource("drop", "identity");
    }

    public void executeMandatorySchemaResource(String operation, String component) {
        executeSchemaResource(operation, component, getResourceForDbOperation(operation, operation, component), false);
    }

    public String dbSchemaUpdate() {

        String feedback = null;
        boolean isUpgradeNeeded = false;
        int matchingVersionIndex = -1;

        if (isIdmPropertyTablePresent()) {

            DbSqlSession dbSqlSession = CommandContextUtil.getDbSqlSession();
            IdmPropertyEntity dbVersionProperty = dbSqlSession.selectById(IdmPropertyEntity.class, "schema.version");
            String dbVersion = dbVersionProperty.getValue();

            // Determine index in the sequence of Flowable releases
            matchingVersionIndex = findMatchingVersionIndex(dbVersion);

            // Exception when no match was found: unknown/unsupported version
            if (matchingVersionIndex < 0) {
                throw new FlowableException("Could not update Flowable IDM database schema: unknown version from database: '" + dbVersion + "'");
            }

            isUpgradeNeeded = (matchingVersionIndex != (FLOWABLE_IDM_VERSIONS.size() - 1));

            if (isUpgradeNeeded) {
                dbVersionProperty.setValue(IdmEngine.VERSION);

                // Engine upgrade
                dbSchemaUpgrade("engine", matchingVersionIndex);
                feedback = "upgraded Flowable IDM from " + dbVersion + " to " + IdmEngine.VERSION;
            }

        } else {
            // User and Group tables can already have been created by the process engine in an earlier version
            if (isIdmGroupTablePresent()) {
                // Engine upgrade
                dbSchemaUpgrade("engine", 0);
                feedback = "upgraded Flowable IDM to " + IdmEngine.VERSION;

            } else {
                dbSchemaCreate();
            }
        }

        return feedback;
    }

    /**
     * Returns the index in the list of {@link #FLOWABLE_IDM_VERSIONS} matching the provided string version. Returns -1 if no match can be found.
     */
    protected int findMatchingVersionIndex(String dbVersion) {
        int index = 0;
        int matchingVersionIndex = -1;
        while (matchingVersionIndex < 0 && index < FLOWABLE_IDM_VERSIONS.size()) {
            if (FLOWABLE_IDM_VERSIONS.get(index).matches(dbVersion)) {
                matchingVersionIndex = index;
            } else {
                index++;
            }
        }
        return matchingVersionIndex;
    }

    public boolean isIdmPropertyTablePresent() {
        return isTablePresent("ACT_ID_PROPERTY");
    }

    public boolean isIdmGroupTablePresent() {
        return isTablePresent("ACT_ID_GROUP");
    }

    public boolean isTablePresent(String tableName) {
        // ACT-1610: in case the prefix IS the schema itself, we don't add the
        // prefix, since the check is already aware of the schema
        DbSqlSession dbSqlSession = CommandContextUtil.getDbSqlSession();
        DbSqlSessionFactory dbSqlSessionFactory = dbSqlSession.getDbSqlSessionFactory();
        if (!dbSqlSessionFactory.isTablePrefixIsSchema()) {
            tableName = prependDatabaseTablePrefix(tableName);
        }

        Connection connection = null;
        try {
            connection = dbSqlSession.getSqlSession().getConnection();
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            ResultSet tables = null;

            String catalog = dbSqlSession.getConnectionMetadataDefaultCatalog();
            if (dbSqlSessionFactory.getDatabaseCatalog() != null && dbSqlSessionFactory.getDatabaseCatalog().length() > 0) {
                catalog = dbSqlSessionFactory.getDatabaseCatalog();
            }

            String schema = dbSqlSession.getConnectionMetadataDefaultSchema();
            if (dbSqlSessionFactory.getDatabaseSchema() != null && dbSqlSessionFactory.getDatabaseSchema().length() > 0) {
                schema = dbSqlSessionFactory.getDatabaseSchema();
            }

            String databaseType = dbSqlSessionFactory.getDatabaseType();

            if ("postgres".equals(databaseType)) {
                tableName = tableName.toLowerCase();
            }

            if (schema != null && "oracle".equals(databaseType)) {
                schema = schema.toUpperCase();
            }

            if (catalog != null && catalog.length() == 0) {
                catalog = null;
            }

            try {
                tables = databaseMetaData.getTables(catalog, schema, tableName, JDBC_METADATA_TABLE_TYPES);
                return tables.next();
            } finally {
                try {
                    tables.close();
                } catch (Exception e) {
                    LOGGER.error("Error closing meta data tables", e);
                }
            }

        } catch (Exception e) {
            throw new FlowableException("couldn't check if tables are already present using metadata: " + e.getMessage(), e);
        }
    }

    protected boolean isUpgradeNeeded(String versionInDatabase) {
        if (IdmEngine.VERSION.equals(versionInDatabase)) {
            return false;
        }

        String cleanDbVersion = getCleanVersion(versionInDatabase);
        String[] cleanDbVersionSplitted = cleanDbVersion.split("\\.");
        int dbMajorVersion = Integer.valueOf(cleanDbVersionSplitted[0]);
        int dbMinorVersion = Integer.valueOf(cleanDbVersionSplitted[1]);

        String cleanEngineVersion = getCleanVersion(IdmEngine.VERSION);
        String[] cleanEngineVersionSplitted = cleanEngineVersion.split("\\.");
        int engineMajorVersion = Integer.valueOf(cleanEngineVersionSplitted[0]);
        int engineMinorVersion = Integer.valueOf(cleanEngineVersionSplitted[1]);

        if ((dbMajorVersion > engineMajorVersion) || ((dbMajorVersion <= engineMajorVersion) && (dbMinorVersion > engineMinorVersion))) {
            throw new FlowableException("Version of idm database (" + versionInDatabase + ") is more recent than the engine (" + IdmEngine.VERSION + ")");
        } else if (cleanDbVersion.compareTo(cleanEngineVersion) == 0) {
            // Versions don't match exactly, possibly snapshot is being used
            LOGGER.warn("IDM Engine-version is the same, but not an exact match: {} vs. {}. Not performing database-upgrade.", versionInDatabase, IdmEngine.VERSION);
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
            throw new FlowableException("Illegal format for version: " + versionString);
        }
    }

    protected String prependDatabaseTablePrefix(String tableName) {
        DbSqlSession dbSqlSession = CommandContextUtil.getDbSqlSession();
        return dbSqlSession.getDbSqlSessionFactory().getDatabaseTablePrefix() + tableName;
    }

    protected void dbSchemaUpgrade(final String component, final int currentDatabaseVersionsIndex) {
        FlowableIdmVersion flowableVersion = FLOWABLE_IDM_VERSIONS.get(currentDatabaseVersionsIndex);
        String dbVersion = flowableVersion.getMainVersion();
        LOGGER.info("upgrading {} schema from {} to {}", component, dbVersion, IdmEngine.VERSION);

        // Actual execution of schema DDL SQL
        for (int i = currentDatabaseVersionsIndex + 1; i < FLOWABLE_IDM_VERSIONS.size(); i++) {
            String nextVersion = FLOWABLE_IDM_VERSIONS.get(i).getMainVersion();

            // Taking care of -SNAPSHOT version in development
            if (nextVersion.endsWith("-SNAPSHOT")) {
                nextVersion = nextVersion.substring(0, nextVersion.length() - "-SNAPSHOT".length());
            }

            dbVersion = dbVersion.replace(".", "");
            nextVersion = nextVersion.replace(".", "");
            LOGGER.info("Upgrade needed: {} -> {}. Looking for schema update resource for component '{}'", dbVersion, nextVersion, component);
            executeSchemaResource("upgrade", component, getResourceForDbOperation("upgrade", "upgradestep." + dbVersion + ".to." + nextVersion, component), true);
            dbVersion = nextVersion;
        }
    }

    public String getResourceForDbOperation(String directory, String operation, String component) {
        DbSqlSession dbSqlSession = CommandContextUtil.getDbSqlSession();
        String databaseType = dbSqlSession.getDbSqlSessionFactory().getDatabaseType();
        return "org/flowable/idm/db/" + directory + "/flowable." + databaseType + "." + operation + "." + component + ".sql";
    }

    public void executeSchemaResource(String operation, String component, String resourceName, boolean isOptional) {
        InputStream inputStream = null;
        try {
            inputStream = ReflectUtil.getResourceAsStream(resourceName);
            if (inputStream == null) {
                if (isOptional) {
                    LOGGER.info("no schema resource {} for {}", resourceName, operation);
                } else {
                    throw new FlowableException("resource '" + resourceName + "' is not available");
                }
            } else {
                executeSchemaResource(operation, component, resourceName, inputStream);
            }

        } finally {
            IoUtil.closeSilently(inputStream);
        }
    }

    protected void executeSchemaResource(String operation, String component, String resourceName, InputStream inputStream) {
        LOGGER.info("performing {} on {} with resource {}", operation, component, resourceName);
        String sqlStatement = null;
        String exceptionSqlStatement = null;
        try {
            DbSqlSession dbSqlSession = CommandContextUtil.getDbSqlSession();
            Connection connection = dbSqlSession.getSqlSession().getConnection();
            Exception exception = null;
            byte[] bytes = IoUtil.readInputStream(inputStream, resourceName);
            String ddlStatements = new String(bytes);

            // Special DDL handling for certain databases
            try {
                if (dbSqlSession.getDbSqlSessionFactory().isMysql()) {
                    DatabaseMetaData databaseMetaData = connection.getMetaData();
                    int majorVersion = databaseMetaData.getDatabaseMajorVersion();
                    int minorVersion = databaseMetaData.getDatabaseMinorVersion();
                    LOGGER.info("Found MySQL: majorVersion={} minorVersion={}", majorVersion, minorVersion);

                    // Special care for MySQL < 5.6
                    if (majorVersion <= 5 && minorVersion < 6) {
                        ddlStatements = updateDdlForMySqlVersionLowerThan56(ddlStatements);
                    }
                }
            } catch (Exception e) {
                LOGGER.info("Could not get database metadata", e);
            }

            BufferedReader reader = new BufferedReader(new StringReader(ddlStatements));
            String line = readNextTrimmedLine(reader);
            boolean inOraclePlsqlBlock = false;
            while (line != null) {
                if (line.startsWith("# ")) {
                    LOGGER.debug(line.substring(2));

                } else if (line.startsWith("-- ")) {
                    LOGGER.debug(line.substring(3));

                } else if (line.length() > 0) {

                    if (dbSqlSession.getDbSqlSessionFactory().isOracle() && line.startsWith("begin")) {
                        inOraclePlsqlBlock = true;
                        sqlStatement = addSqlStatementPiece(sqlStatement, line);

                    } else if ((line.endsWith(";") && !inOraclePlsqlBlock) || (line.startsWith("/") && inOraclePlsqlBlock)) {

                        if (inOraclePlsqlBlock) {
                            inOraclePlsqlBlock = false;
                        } else {
                            sqlStatement = addSqlStatementPiece(sqlStatement, line.substring(0, line.length() - 1));
                        }

                        Statement jdbcStatement = connection.createStatement();
                        try {
                            // no logging needed as the connection will log it
                            LOGGER.debug("SQL: {}", sqlStatement);
                            jdbcStatement.execute(sqlStatement);
                            jdbcStatement.close();
                        } catch (Exception e) {
                            if (exception == null) {
                                exception = e;
                                exceptionSqlStatement = sqlStatement;
                            }
                            LOGGER.error("problem during schema {}, statement {}", operation, sqlStatement, e);
                        } finally {
                            sqlStatement = null;
                        }
                    } else {
                        sqlStatement = addSqlStatementPiece(sqlStatement, line);
                    }
                }

                line = readNextTrimmedLine(reader);
            }

            if (exception != null) {
                throw exception;
            }

            LOGGER.debug("flowable db schema {} for component {} successful", operation, component);

        } catch (Exception e) {
            throw new FlowableException("couldn't " + operation + " db schema: " + exceptionSqlStatement, e);
        }
    }

    /**
     * MySQL is funny when it comes to timestamps and dates.
     * 
     * More specifically, for a DDL statement like 'MYCOLUMN timestamp(3)': - MySQL 5.6.4+ has support for timestamps/dates with millisecond (or smaller) precision. The DDL above works and the data in
     * the table will have millisecond precision - MySQL < 5.5.3 allows the DDL statement, but ignores it. The DDL above works but the data won't have millisecond precision - MySQL 5.5.3 < [version] <
     * 5.6.4 gives and exception when using the DDL above.
     * 
     * Also, the 5.5 and 5.6 branches of MySQL are both actively developed and patched.
     * 
     * Hence, when doing auto-upgrade/creation of the Flowable tables, the default MySQL DDL file is used and all timestamps/datetimes are converted to not use the millisecond precision by string
     * replacement done in the method below.
     * 
     * If using the DDL files directly (which is a sane choice in production env.), there is a distinction between MySQL version < 5.6.
     */
    protected String updateDdlForMySqlVersionLowerThan56(String ddlStatements) {
        return ddlStatements.replace("timestamp(3)", "timestamp").replace("datetime(3)", "datetime").replace("TIMESTAMP(3)", "TIMESTAMP").replace("DATETIME(3)", "DATETIME");
    }

    protected String addSqlStatementPiece(String sqlStatement, String line) {
        if (sqlStatement == null) {
            return line;
        }
        return sqlStatement + " \n" + line;
    }

    protected String readNextTrimmedLine(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        if (line != null) {
            line = line.trim();
        }
        return line;
    }

    protected boolean isMissingTablesException(Exception e) {
        String exceptionMessage = e.getMessage();
        if (e.getMessage() != null) {
            // Matches message returned from H2
            if ((exceptionMessage.indexOf("Table") != -1) && (exceptionMessage.indexOf("not found") != -1)) {
                return true;
            }

            // Message returned from MySQL and Oracle
            if (((exceptionMessage.indexOf("Table") != -1 || exceptionMessage.indexOf("table") != -1)) && (exceptionMessage.indexOf("doesn't exist") != -1)) {
                return true;
            }

            // Message returned from Postgres
            if (((exceptionMessage.indexOf("relation") != -1 || exceptionMessage.indexOf("table") != -1)) && (exceptionMessage.indexOf("does not exist") != -1)) {
                return true;
            }
        }
        return false;
    }

    public void performSchemaOperationsIdmEngineBuild() {
        String databaseSchemaUpdate = CommandContextUtil.getIdmEngineConfiguration().getDatabaseSchemaUpdate();
        LOGGER.debug("Executing performSchemaOperationsProcessEngineBuild with setting {}", databaseSchemaUpdate);
        if (IdmEngineConfiguration.DB_SCHEMA_UPDATE_DROP_CREATE.equals(databaseSchemaUpdate)) {
            try {
                dbSchemaDrop();
            } catch (RuntimeException e) {
                // ignore
            }
        }
        if (IdmEngineConfiguration.DB_SCHEMA_UPDATE_CREATE_DROP.equals(databaseSchemaUpdate) || IdmEngineConfiguration.DB_SCHEMA_UPDATE_DROP_CREATE.equals(databaseSchemaUpdate)
                || IdmEngineConfiguration.DB_SCHEMA_UPDATE_CREATE.equals(databaseSchemaUpdate)) {
            dbSchemaCreate();

        } else if (IdmEngineConfiguration.DB_SCHEMA_UPDATE_FALSE.equals(databaseSchemaUpdate)) {
            dbSchemaCheckVersion();

        } else if (IdmEngineConfiguration.DB_SCHEMA_UPDATE_TRUE.equals(databaseSchemaUpdate)) {
            dbSchemaUpdate();
        }
    }

    public void performSchemaOperationsProcessEngineClose() {
        String databaseSchemaUpdate = CommandContextUtil.getIdmEngineConfiguration().getDatabaseSchemaUpdate();
        if (IdmEngineConfiguration.DB_SCHEMA_UPDATE_CREATE_DROP.equals(databaseSchemaUpdate)) {
            dbSchemaDrop();
        }
    }
    
}
