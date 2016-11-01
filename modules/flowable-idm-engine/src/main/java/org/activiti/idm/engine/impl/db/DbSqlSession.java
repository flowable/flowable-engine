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

package org.activiti.idm.engine.impl.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.ActivitiOptimisticLockingException;
import org.activiti.engine.ActivitiWrongDbException;
import org.activiti.idm.engine.IdmEngine;
import org.activiti.idm.engine.IdmEngineConfiguration;
import org.activiti.idm.engine.impl.Page;
import org.activiti.idm.engine.impl.context.Context;
import org.activiti.idm.engine.impl.db.upgrade.DbUpgradeStep;
import org.activiti.idm.engine.impl.interceptor.Session;
import org.activiti.idm.engine.impl.persistence.entity.PropertyEntity;
import org.activiti.idm.engine.impl.util.IoUtil;
import org.activiti.idm.engine.impl.util.ReflectUtil;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class DbSqlSession implements Session {

  private static final Logger log = LoggerFactory.getLogger(DbSqlSession.class);
  
  protected static final Pattern CLEAN_VERSION_REGEX = Pattern.compile("\\d\\.\\d*");
  
  protected static final List<ActivitiIdmVersion> ACTIVITI_IDM_VERSIONS = new ArrayList<ActivitiIdmVersion>();
  
  static {

    /* Previous */

    
    /* Current */
    ACTIVITI_IDM_VERSIONS.add(new ActivitiIdmVersion(IdmEngine.VERSION));
  }
  
  public static String[] JDBC_METADATA_TABLE_TYPES = { "TABLE" };

  protected SqlSession sqlSession;
  protected DbSqlSessionFactory dbSqlSessionFactory;
  protected String connectionMetadataDefaultCatalog;
  protected String connectionMetadataDefaultSchema;

  public DbSqlSession(DbSqlSessionFactory dbSqlSessionFactory) {
    this.dbSqlSessionFactory = dbSqlSessionFactory;
    this.sqlSession = dbSqlSessionFactory.getSqlSessionFactory().openSession();
  }

  public DbSqlSession(DbSqlSessionFactory dbSqlSessionFactory, Connection connection, String catalog, String schema) {
    this.dbSqlSessionFactory = dbSqlSessionFactory;
    this.sqlSession = dbSqlSessionFactory.getSqlSessionFactory().openSession(connection); // Note the use of connection param here, different from other constructor
    this.connectionMetadataDefaultCatalog = catalog;
    this.connectionMetadataDefaultSchema = schema;
  }
  
  // insert ///////////////////////////////////////////////////////////////////
  
  
  public void insert(Entity entity) {
    if (entity.getId() == null) {
      String id = dbSqlSessionFactory.getIdGenerator().getNextId();
      entity.setId(id);
    }
    
    String insertStatement = dbSqlSessionFactory.getInsertStatement(entity);
    insertStatement = dbSqlSessionFactory.mapStatement(insertStatement);

    if (insertStatement==null) {
      throw new ActivitiException("no insert statement for " + entity.getClass() + " in the ibatis mapping files");
    }
    
    log.debug("inserting: {}", entity);
    sqlSession.insert(insertStatement, entity);
  }

  // update
  // ///////////////////////////////////////////////////////////////////

  public void update(Entity entity) {
    String updateStatement = dbSqlSessionFactory.getUpdateStatement(entity);
    updateStatement = dbSqlSessionFactory.mapStatement(updateStatement);

    if (updateStatement == null) {
      throw new ActivitiException("no update statement for " + entity.getClass() + " in the ibatis mapping files");
    }

    log.debug("updating: {}", entity);
    int updatedRecords = sqlSession.update(updateStatement, entity);
    if (updatedRecords == 0) {
      throw new ActivitiOptimisticLockingException(entity + " was updated by another transaction concurrently");
    }
  }

  public int update(String statement, Object parameters) {
    String updateStatement = dbSqlSessionFactory.mapStatement(statement);
    return sqlSession.update(updateStatement, parameters);
  }

  // delete
  // ///////////////////////////////////////////////////////////////////

  public void delete(String statement, Object parameter) {
    sqlSession.delete(statement, parameter);
  }

  public void delete(Entity entity) {
    String deleteStatement = dbSqlSessionFactory.getDeleteStatement(entity.getClass());
    deleteStatement = dbSqlSessionFactory.mapStatement(deleteStatement);
    if (deleteStatement == null) {
      throw new ActivitiException("no delete statement for " + entity.getClass() + " in the ibatis mapping files");
    }

    sqlSession.delete(deleteStatement, entity);
  }

  // select
  // ///////////////////////////////////////////////////////////////////

  @SuppressWarnings({ "rawtypes" })
  public List selectList(String statement) {
    return selectList(statement, null, 0, Integer.MAX_VALUE);
  }

  @SuppressWarnings("rawtypes")
  public List selectList(String statement, Object parameter) {
    return selectList(statement, parameter, 0, Integer.MAX_VALUE);
  }

  @SuppressWarnings("rawtypes")
  public List selectList(String statement, Object parameter, Page page) {
    if (page != null) {
      return selectList(statement, parameter, page.getFirstResult(), page.getMaxResults());
    } else {
      return selectList(statement, parameter, 0, Integer.MAX_VALUE);
    }
  }

  @SuppressWarnings("rawtypes")
  public List selectList(String statement, ListQueryParameterObject parameter, Page page) {
    if (page != null) {
      parameter.setFirstResult(page.getFirstResult());
      parameter.setMaxResults(page.getMaxResults());
    }
    return selectList(statement, parameter);
  }

  @SuppressWarnings("rawtypes")
  public List selectList(String statement, Object parameter, int firstResult, int maxResults) {
    return selectList(statement, new ListQueryParameterObject(parameter, firstResult, maxResults));
  }

  @SuppressWarnings("rawtypes")
  public List selectList(String statement, ListQueryParameterObject parameter) {
    return selectListWithRawParameter(statement, parameter, parameter.getFirstResult(), parameter.getMaxResults());
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public List selectListWithRawParameter(String statement, Object parameter, int firstResult, int maxResults) {
    statement = dbSqlSessionFactory.mapStatement(statement);
    if (firstResult == -1 || maxResults == -1) {
      return Collections.EMPTY_LIST;
    }
    List loadedObjects = sqlSession.selectList(statement, parameter);
    return loadedObjects;
  }

  @SuppressWarnings({ "rawtypes" })
  public List selectListWithRawParameterWithoutFilter(String statement, Object parameter, int firstResult, int maxResults) {
    statement = dbSqlSessionFactory.mapStatement(statement);
    if (firstResult == -1 || maxResults == -1) {
      return Collections.EMPTY_LIST;
    }
    return sqlSession.selectList(statement, parameter);
  }

  public Object selectOne(String statement, Object parameter) {
    statement = dbSqlSessionFactory.mapStatement(statement);
    Object result = sqlSession.selectOne(statement, parameter);
    return result;
  }
  
  @SuppressWarnings("unchecked")
  public <T extends Entity> T selectById(Class<T> entityClass, String id) {
    T entity = null;
    
    String selectStatement = dbSqlSessionFactory.getSelectStatement(entityClass);
    selectStatement = dbSqlSessionFactory.mapStatement(selectStatement);
    entity = (T) sqlSession.selectOne(selectStatement, id);
    if (entity == null) {
      return null;
    }
    
    return entity;
  }
  
  public void flush() {
    sqlSession.flushStatements();
  }

  public void close() {
    sqlSession.close();
  }

  public void commit() {
    sqlSession.commit();
  }

  public void rollback() {
    sqlSession.rollback();
  }

  // schema operations
  // ////////////////////////////////////////////////////////

  public void dbSchemaCheckVersion() {
    try {
      String dbVersion = getDbVersion();
      if (!IdmEngine.VERSION.equals(dbVersion)) {
        throw new ActivitiWrongDbException(IdmEngine.VERSION, dbVersion);
      }

      String errorMessage = null;
      if (!isIdmTablePresent()) {
        errorMessage = addMissingComponent(errorMessage, "engine");
      }

      if (errorMessage != null) {
        throw new ActivitiException("Activiti IDM database problem: " + errorMessage);
      }

    } catch (Exception e) {
      if (isMissingTablesException(e)) {
        throw new ActivitiException(
            "no activiti tables in db. set <property name=\"databaseSchemaUpdate\" to value=\"true\" or value=\"create-drop\" (use create-drop for testing only!) in bean processEngineConfiguration in activiti.cfg.xml for automatic schema creation", e);
      } else {
        if (e instanceof RuntimeException) {
          throw (RuntimeException) e;
        } else {
          throw new ActivitiException("couldn't get db schema version", e);
        }
      }
    }

    log.debug("activiti idm db schema check successful");
  }

  protected String addMissingComponent(String missingComponents, String component) {
    if (missingComponents == null) {
      return "Tables missing for component(s) " + component;
    }
    return missingComponents + ", " + component;
  }

  protected String getDbVersion() {
    String selectSchemaVersionStatement = dbSqlSessionFactory.mapStatement("selectDbSchemaVersion");
    return (String) sqlSession.selectOne(selectSchemaVersionStatement);
  }

  public void dbSchemaCreate() {
    if (isIdmTablePresent()) {
      String dbVersion = getDbVersion();
      if (!IdmEngine.VERSION.equals(dbVersion)) {
        throw new ActivitiWrongDbException(IdmEngine.VERSION, dbVersion);
      }
    } else {
      dbSchemaCreateIdmEngine();
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

    if (isIdmTablePresent()) {

      PropertyEntity dbVersionProperty = selectById(PropertyEntity.class, "schema.version");
      String dbVersion = dbVersionProperty.getValue();

      // Determine index in the sequence of Activiti releases
      matchingVersionIndex = findMatchingVersionIndex(dbVersion);

      // Exception when no match was found: unknown/unsupported version
      if (matchingVersionIndex < 0) {
        throw new ActivitiException("Could not update Activiti database schema: unknown version from database: '" + dbVersion + "'");
      }

      isUpgradeNeeded = (matchingVersionIndex != (ACTIVITI_IDM_VERSIONS.size() - 1));

      if (isUpgradeNeeded) {
        dbVersionProperty.setValue(IdmEngine.VERSION);

        PropertyEntity dbHistoryProperty;
        if ("5.0".equals(dbVersion)) {
          dbHistoryProperty = Context.getCommandContext().getPropertyEntityManager().create();
          dbHistoryProperty.setName("schema.history");
          dbHistoryProperty.setValue("create(5.0)");
          insert(dbHistoryProperty);
        } else {
          dbHistoryProperty = selectById(PropertyEntity.class, "schema.history");
        }

        // Set upgrade history
        String dbHistoryValue = dbHistoryProperty.getValue() + " upgrade(" + dbVersion + "->" + IdmEngine.VERSION + ")";
        dbHistoryProperty.setValue(dbHistoryValue);

        // Engine upgrade
        dbSchemaUpgrade("engine", matchingVersionIndex);
        feedback = "upgraded Activiti IDM from " + dbVersion + " to " + IdmEngine.VERSION;
      }

    } else {
      dbSchemaCreate();
    }

    return feedback;
  }

  /**
   * Returns the index in the list of {@link #ACTIVITI_VERSIONS} matching the
   * provided string version. Returns -1 if no match can be found.
   */
  protected int findMatchingVersionIndex(String dbVersion) {
    int index = 0;
    int matchingVersionIndex = -1;
    while (matchingVersionIndex < 0 && index < ACTIVITI_IDM_VERSIONS.size()) {
      if (ACTIVITI_IDM_VERSIONS.get(index).matches(dbVersion)) {
        matchingVersionIndex = index;
      } else {
        index++;
      }
    }
    return matchingVersionIndex;
  }

  public boolean isIdmTablePresent() {
    return isTablePresent("ACT_ID_PROPERTY");
  }

  public boolean isTablePresent(String tableName) {
    // ACT-1610: in case the prefix IS the schema itself, we don't add the
    // prefix, since the check is already aware of the schema
    if (!dbSqlSessionFactory.isTablePrefixIsSchema()) {
      tableName = prependDatabaseTablePrefix(tableName);
    }

    Connection connection = null;
    try {
      connection = sqlSession.getConnection();
      DatabaseMetaData databaseMetaData = connection.getMetaData();
      ResultSet tables = null;

      String catalog = this.connectionMetadataDefaultCatalog;
      if (dbSqlSessionFactory.getDatabaseCatalog() != null && dbSqlSessionFactory.getDatabaseCatalog().length() > 0) {
        catalog = dbSqlSessionFactory.getDatabaseCatalog();
      }

      String schema = this.connectionMetadataDefaultSchema;
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
          log.error("Error closing meta data tables", e);
        }
      }

    } catch (Exception e) {
      throw new ActivitiException("couldn't check if tables are already present using metadata: " + e.getMessage(), e);
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
      throw new ActivitiException("Version of activiti idm database (" + versionInDatabase + ") is more recent than the engine (" + IdmEngine.VERSION + ")");
    } else if (cleanDbVersion.compareTo(cleanEngineVersion) == 0) {
      // Versions don't match exactly, possibly snapshot is being used
      log.warn("IDM Engine-version is the same, but not an exact match: {} vs. {}. Not performing database-upgrade.", versionInDatabase, IdmEngine.VERSION);
      return false;
    }
    return true;
  }

  protected String getCleanVersion(String versionString) {
    Matcher matcher = CLEAN_VERSION_REGEX.matcher(versionString);
    if (!matcher.find()) {
      throw new ActivitiException("Illegal format for version: " + versionString);
    }

    String cleanString = matcher.group();
    try {
      Double.parseDouble(cleanString); // try to parse it, to see if it is
                                       // really a number
      return cleanString;
    } catch (NumberFormatException nfe) {
      throw new ActivitiException("Illegal format for version: " + versionString);
    }
  }

  protected String prependDatabaseTablePrefix(String tableName) {
    return dbSqlSessionFactory.getDatabaseTablePrefix() + tableName;
  }

  protected void dbSchemaUpgrade(final String component, final int currentDatabaseVersionsIndex) {
    ActivitiIdmVersion activitiVersion = ACTIVITI_IDM_VERSIONS.get(currentDatabaseVersionsIndex);
    String dbVersion = activitiVersion.getMainVersion();
    log.info("upgrading activiti {} schema from {} to {}", component, dbVersion, IdmEngine.VERSION);

    // Actual execution of schema DDL SQL
    for (int i = currentDatabaseVersionsIndex + 1; i < ACTIVITI_IDM_VERSIONS.size(); i++) {
      String nextVersion = ACTIVITI_IDM_VERSIONS.get(i).getMainVersion();

      // Taking care of -SNAPSHOT version in development
      if (nextVersion.endsWith("-SNAPSHOT")) {
        nextVersion = nextVersion.substring(0, nextVersion.length() - "-SNAPSHOT".length());
      }

      dbVersion = dbVersion.replace(".", "");
      nextVersion = nextVersion.replace(".", "");
      log.info("Upgrade needed: {} -> {}. Looking for schema update resource for component '{}'", dbVersion, nextVersion, component);
      executeSchemaResource("upgrade", component, getResourceForDbOperation("upgrade", "upgradestep." + dbVersion + ".to." + nextVersion, component), true);
      dbVersion = nextVersion;
    }
  }

  public String getResourceForDbOperation(String directory, String operation, String component) {
    String databaseType = dbSqlSessionFactory.getDatabaseType();
    return "org/activiti/idm/db/" + directory + "/activiti." + databaseType + "." + operation + "." + component + ".sql";
  }

  public void executeSchemaResource(String operation, String component, String resourceName, boolean isOptional) {
    InputStream inputStream = null;
    try {
      inputStream = ReflectUtil.getResourceAsStream(resourceName);
      if (inputStream == null) {
        if (isOptional) {
          log.info("no schema resource {} for {}", resourceName, operation);
        } else {
          throw new ActivitiException("resource '" + resourceName + "' is not available");
        }
      } else {
        executeSchemaResource(operation, component, resourceName, inputStream);
      }

    } finally {
      IoUtil.closeSilently(inputStream);
    }
  }

  private void executeSchemaResource(String operation, String component, String resourceName, InputStream inputStream) {
    log.info("performing {} on {} with resource {}", operation, component, resourceName);
    String sqlStatement = null;
    String exceptionSqlStatement = null;
    try {
      Connection connection = sqlSession.getConnection();
      Exception exception = null;
      byte[] bytes = IoUtil.readInputStream(inputStream, resourceName);
      String ddlStatements = new String(bytes);

      // Special DDL handling for certain databases
      try {
        if (isMysql()) {
          DatabaseMetaData databaseMetaData = connection.getMetaData();
          int majorVersion = databaseMetaData.getDatabaseMajorVersion();
          int minorVersion = databaseMetaData.getDatabaseMinorVersion();
          log.info("Found MySQL: majorVersion=" + majorVersion + " minorVersion=" + minorVersion);

          // Special care for MySQL < 5.6
          if (majorVersion <= 5 && minorVersion < 6) {
            ddlStatements = updateDdlForMySqlVersionLowerThan56(ddlStatements);
          }
        }
      } catch (Exception e) {
        log.info("Could not get database metadata", e);
      }

      BufferedReader reader = new BufferedReader(new StringReader(ddlStatements));
      String line = readNextTrimmedLine(reader);
      boolean inOraclePlsqlBlock = false;
      while (line != null) {
        if (line.startsWith("# ")) {
          log.debug(line.substring(2));

        } else if (line.startsWith("-- ")) {
          log.debug(line.substring(3));

        } else if (line.startsWith("execute java ")) {
          String upgradestepClassName = line.substring(13).trim();
          DbUpgradeStep dbUpgradeStep = null;
          try {
            dbUpgradeStep = (DbUpgradeStep) ReflectUtil.instantiate(upgradestepClassName);
          } catch (ActivitiException e) {
            throw new ActivitiException("database update java class '" + upgradestepClassName + "' can't be instantiated: " + e.getMessage(), e);
          }
          try {
            log.debug("executing upgrade step java class {}", upgradestepClassName);
            dbUpgradeStep.execute(this);
          } catch (Exception e) {
            throw new ActivitiException("error while executing database update java class '" + upgradestepClassName + "': " + e.getMessage(), e);
          }

        } else if (line.length() > 0) {

          if (isOracle() && line.startsWith("begin")) {
            inOraclePlsqlBlock = true;
            sqlStatement = addSqlStatementPiece(sqlStatement, line);

          } else if ((line.endsWith(";") && inOraclePlsqlBlock == false) || (line.startsWith("/") && inOraclePlsqlBlock == true)) {

            if (inOraclePlsqlBlock) {
              inOraclePlsqlBlock = false;
            } else {
              sqlStatement = addSqlStatementPiece(sqlStatement, line.substring(0, line.length() - 1));
            }

            Statement jdbcStatement = connection.createStatement();
            try {
              // no logging needed as the connection will log it
              log.debug("SQL: {}", sqlStatement);
              jdbcStatement.execute(sqlStatement);
              jdbcStatement.close();
            } catch (Exception e) {
              if (exception == null) {
                exception = e;
                exceptionSqlStatement = sqlStatement;
              }
              log.error("problem during schema {}, statement {}", operation, sqlStatement, e);
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

      log.debug("activiti db schema {} for component {} successful", operation, component);

    } catch (Exception e) {
      throw new ActivitiException("couldn't " + operation + " db schema: " + exceptionSqlStatement, e);
    }
  }

  /**
   * MySQL is funny when it comes to timestamps and dates.
   * 
   * More specifically, for a DDL statement like 'MYCOLUMN timestamp(3)': -
   * MySQL 5.6.4+ has support for timestamps/dates with millisecond (or smaller)
   * precision. The DDL above works and the data in the table will have
   * millisecond precision - MySQL < 5.5.3 allows the DDL statement, but ignores
   * it. The DDL above works but the data won't have millisecond precision -
   * MySQL 5.5.3 < [version] < 5.6.4 gives and exception when using the DDL
   * above.
   * 
   * Also, the 5.5 and 5.6 branches of MySQL are both actively developed and
   * patched.
   * 
   * Hence, when doing auto-upgrade/creation of the Activiti tables, the default
   * MySQL DDL file is used and all timestamps/datetimes are converted to not
   * use the millisecond precision by string replacement done in the method
   * below.
   * 
   * If using the DDL files directly (which is a sane choice in production
   * env.), there is a distinction between MySQL version < 5.6.
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
    String databaseSchemaUpdate = Context.getIdmEngineConfiguration().getDatabaseSchemaUpdate();
    log.debug("Executing performSchemaOperationsProcessEngineBuild with setting " + databaseSchemaUpdate);
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
    String databaseSchemaUpdate = Context.getIdmEngineConfiguration().getDatabaseSchemaUpdate();
    if (IdmEngineConfiguration.DB_SCHEMA_UPDATE_CREATE_DROP.equals(databaseSchemaUpdate)) {
      dbSchemaDrop();
    }
  }

  public <T> T getCustomMapper(Class<T> type) {
    return sqlSession.getMapper(type);
  }
  
  public boolean isMysql() {
    return dbSqlSessionFactory.getDatabaseType().equals("mysql");
  }
  
  public boolean isOracle() {
    return dbSqlSessionFactory.getDatabaseType().equals("oracle");
  }

  // query factory methods
  // ////////////////////////////////////////////////////


  // getters and setters
  // //////////////////////////////////////////////////////

  public SqlSession getSqlSession() {
    return sqlSession;
  }

  public DbSqlSessionFactory getDbSqlSessionFactory() {
    return dbSqlSessionFactory;
  }

}
