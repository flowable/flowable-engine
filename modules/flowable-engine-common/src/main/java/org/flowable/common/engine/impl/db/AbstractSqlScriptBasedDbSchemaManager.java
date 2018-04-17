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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.FlowableVersion;
import org.flowable.common.engine.impl.FlowableVersions;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.util.IoUtil;
import org.flowable.common.engine.impl.util.ReflectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public abstract class AbstractSqlScriptBasedDbSchemaManager implements DbSchemaManager {
    
    private static Logger LOGGER = LoggerFactory.getLogger(AbstractSqlScriptBasedDbSchemaManager.class);
    
    public static String[] JDBC_METADATA_TABLE_TYPES = { "TABLE" };
    
    protected static final String PROPERTY_TABLE = "ACT_GE_PROPERTY";
    
    protected static final String SCHEMA_VERSION_PROPERTY = "schema.version";
    
    protected void dbSchemaUpgradeUntil6120(final String component, final int currentDatabaseVersionsIndex) {
        FlowableVersion version = FlowableVersions.FLOWABLE_VERSIONS.get(currentDatabaseVersionsIndex);
        String dbVersion = version.getMainVersion();
        LOGGER.info("upgrading flowable {} schema from {} to {}", component, dbVersion, FlowableVersions.LAST_V6_VERSION_BEFORE_SERVICES);
        
        // Actual execution of schema DDL SQL
        for (int i = currentDatabaseVersionsIndex + 1; i < FlowableVersions.getFlowableVersionIndexForDbVersion(FlowableVersions.LAST_V6_VERSION_BEFORE_SERVICES); i++) {
            String nextVersion = FlowableVersions.FLOWABLE_VERSIONS.get(i).getMainVersion();

            // Taking care of -SNAPSHOT version in development
            if (nextVersion.endsWith("-SNAPSHOT")) {
                nextVersion = nextVersion.substring(0, nextVersion.length() - "-SNAPSHOT".length());
            }

            dbVersion = dbVersion.replace(".", "");
            nextVersion = nextVersion.replace(".", "");
            LOGGER.info("Upgrade needed: {} -> {}. Looking for schema update resource for component '{}'", dbVersion, nextVersion, component);
            String databaseType = getDbSqlSession().getDbSqlSessionFactory().getDatabaseType();
            executeSchemaResource("upgrade", component, getResourceForDbOperation("upgrade", "upgradestep." + dbVersion + ".to." + nextVersion, component, databaseType), true);
            
            // To avoid having too much similar scripts, for upgrades the 'all' database is supported and executed for every database type
            executeSchemaResource("upgrade", component, getResourceForDbOperation("upgrade", "upgradestep." + dbVersion + ".to." + nextVersion, component, "all"), true);
            
            dbVersion = nextVersion;
        }
    }
    
    protected void dbSchemaUpgrade(final String component, final int currentDatabaseVersionsIndex) {
        FlowableVersion version = FlowableVersions.FLOWABLE_VERSIONS.get(currentDatabaseVersionsIndex);
        String dbVersion = version.getMainVersion();
        LOGGER.info("upgrading flowable {} schema from {} to {}", component, dbVersion, FlowableVersions.CURRENT_VERSION);

        // Actual execution of schema DDL SQL
        for (int i = currentDatabaseVersionsIndex + 1; i < FlowableVersions.FLOWABLE_VERSIONS.size(); i++) {
            String nextVersion = FlowableVersions.FLOWABLE_VERSIONS.get(i).getMainVersion();

            // Taking care of -SNAPSHOT version in development
            if (nextVersion.endsWith("-SNAPSHOT")) {
                nextVersion = nextVersion.substring(0, nextVersion.length() - "-SNAPSHOT".length());
            }

            dbVersion = dbVersion.replace(".", "");
            nextVersion = nextVersion.replace(".", "");
            LOGGER.info("Upgrade needed: {} -> {}. Looking for schema update resource for component '{}'", dbVersion, nextVersion, component);
            String databaseType = getDbSqlSession().getDbSqlSessionFactory().getDatabaseType();
            executeSchemaResource("upgrade", component, getResourceForDbOperation("upgrade", "upgradestep." + dbVersion + ".to." + nextVersion, component, databaseType), true);
            
            // To avoid having too much similar scripts, for upgrades the 'all' database is supported and executed for every database type
            executeSchemaResource("upgrade", component, getResourceForDbOperation("upgrade", "upgradestep." + dbVersion + ".to." + nextVersion, component, "all"), true);
            
            dbVersion = nextVersion;
        }
    }

    public boolean isTablePresent(String tableName) {
        // ACT-1610: in case the prefix IS the schema itself, we don't add the
        // prefix, since the check is already aware of the schema
        DbSqlSession dbSqlSession = getDbSqlSession();
        DbSqlSessionFactory dbSqlSessionFactory = dbSqlSession.getDbSqlSessionFactory();
        if (!dbSqlSession.getDbSqlSessionFactory().isTablePrefixIsSchema()) {
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
            } else if (dbSqlSessionFactory.isTablePrefixIsSchema() && StringUtils.isNotEmpty(dbSqlSessionFactory.getDatabaseTablePrefix())) {
                schema = dbSqlSessionFactory.getDatabaseTablePrefix();
                if (StringUtils.isNotEmpty(schema) && schema.endsWith(".")) {
                    schema = schema.substring(0, schema.length() - 1);
                }
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
    
    protected String prependDatabaseTablePrefix(String tableName) {
        return getDbSqlSession().getDbSqlSessionFactory().getDatabaseTablePrefix() + tableName;
    }
    
    public DbSqlSession getDbSqlSession() {
        return Context.getCommandContext().getSession(DbSqlSession.class);
    }
    
    public String getProperty(String propertyName) {
        String tableName = getPropertyTable();
       
        if (!isTablePresent(tableName)) { // isTablePresent will add the prefix, so adding it later
            return null;
        }
        
        if (!getDbSqlSession().getDbSqlSessionFactory().isTablePrefixIsSchema()) {
            tableName = prependDatabaseTablePrefix(tableName);
        }
        PreparedStatement statement = null;
        try {
            
            statement = getDbSqlSession().getSqlSession().getConnection()
                    .prepareStatement("select VALUE_ from " + tableName + " where NAME_ = ?");
            statement.setString(1, propertyName);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString(1);
            } else {
                return null;
            }
        } catch (SQLException e) {
            LOGGER.error("Could not get property from table " + tableName, e);
            return null;
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                }
            }
        }
    }
    
    protected String getPropertyTable() {
        return PROPERTY_TABLE;
    }
    
    public String getResourceForDbOperation(String directory, String operation, String component, String databaseType) {
        return getResourcesRootDirectory() + directory + "/flowable." + databaseType + "." + operation + "." + component + ".sql";
    }
    
    protected abstract String getResourcesRootDirectory();
    
    public void executeMandatorySchemaResource(String operation, String component) {
        String databaseType = getDbSqlSession().getDbSqlSessionFactory().getDatabaseType();
        executeSchemaResource(operation, component, getResourceForDbOperation(operation, operation, component, databaseType), false);
    }

    public void executeSchemaResource(String operation, String component, String resourceName, boolean isOptional) {
        InputStream inputStream = null;
        try {
            inputStream = ReflectUtil.getResourceAsStream(resourceName);
            if (inputStream == null) {
                if (!isOptional) {
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
        DbSqlSession dbSqlSession = getDbSqlSession();
        try {
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

                } else if (line.startsWith("execute java ")) {
                    String upgradestepClassName = line.substring(13).trim();
                    DbUpgradeStep dbUpgradeStep = null;
                    try {
                        dbUpgradeStep = (DbUpgradeStep) ReflectUtil.instantiate(upgradestepClassName);
                    } catch (FlowableException e) {
                        throw new FlowableException("database update java class '" + upgradestepClassName + "' can't be instantiated: " + e.getMessage(), e);
                    }
                    try {
                        LOGGER.debug("executing upgrade step java class {}", upgradestepClassName);
                        dbUpgradeStep.execute();
                    } catch (Exception e) {
                        throw new FlowableException("error while executing database update java class '" + upgradestepClassName + "': " + e.getMessage(), e);
                    }

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

}
