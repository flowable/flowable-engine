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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ibatis.session.SqlSessionFactory;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.Session;
import org.flowable.common.engine.impl.interceptor.SessionFactory;
import org.flowable.common.engine.impl.persistence.cache.EntityCache;
import org.flowable.common.engine.impl.persistence.entity.Entity;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class DbSqlSessionFactory implements SessionFactory {

    protected Map<String, Map<String, String>> databaseSpecificStatements = new HashMap<>();

    protected String databaseType;
    protected String databaseTablePrefix = "";
    protected boolean tablePrefixIsSchema;

    protected String databaseCatalog;
    protected String databaseSchema;
    protected SqlSessionFactory sqlSessionFactory;
    protected Map<String, String> statementMappings;

    protected Map<Class<?>, String> insertStatements = new ConcurrentHashMap<>();
    protected Map<Class<?>, String> updateStatements = new ConcurrentHashMap<>();
    protected Map<Class<?>, String> deleteStatements = new ConcurrentHashMap<>();
    protected Map<Class<?>, String> selectStatements = new ConcurrentHashMap<>();

    protected List<Class<? extends Entity>> insertionOrder = new ArrayList<>();
    protected List<Class<? extends Entity>> deletionOrder = new ArrayList<>();

    protected boolean isDbHistoryUsed = true;

    protected Set<Class<? extends Entity>> bulkInserteableEntityClasses = new HashSet<>();
    protected Map<Class<?>, String> bulkInsertStatements = new ConcurrentHashMap<>();

    protected int maxNrOfStatementsInBulkInsert = 100;
    
    protected Map<String, Class<?>> logicalNameToClassMapping = new ConcurrentHashMap<>();

    @Override
    public Class<?> getSessionType() {
        return DbSqlSession.class;
    }

    @Override
    public Session openSession(CommandContext commandContext) {
        DbSqlSession dbSqlSession = createDbSqlSession();
        if (getDatabaseSchema() != null && getDatabaseSchema().length() > 0) {
            try {
                dbSqlSession.getSqlSession().getConnection().setSchema(getDatabaseSchema());
            } catch (SQLException e) {
                throw new FlowableException("Could not set database schema on connection", e);
            }
        }
        if (getDatabaseCatalog() != null && getDatabaseCatalog().length() > 0) {
            try {
                dbSqlSession.getSqlSession().getConnection().setCatalog(getDatabaseCatalog());
            } catch (SQLException e) {
                throw new FlowableException("Could not set database catalog on connection", e);
            }
        }
        if (dbSqlSession.getSqlSession().getConnection() == null) {
            throw new FlowableException("Invalid dbSqlSession: no active connection found");
        }
        return dbSqlSession;
    }

    protected DbSqlSession createDbSqlSession() {
        return new DbSqlSession(this, Context.getCommandContext().getSession(EntityCache.class));
    }

    // insert, update and delete statements
    // /////////////////////////////////////

    public String getInsertStatement(Entity object) {
        return getStatement(object.getClass(), insertStatements, "insert");
    }

    public String getInsertStatement(Class<? extends Entity> clazz) {
        return getStatement(clazz, insertStatements, "insert");
    }

    public String getUpdateStatement(Entity object) {
        return getStatement(object.getClass(), updateStatements, "update");
    }

    public String getDeleteStatement(Class<?> entityClass) {
        return getStatement(entityClass, deleteStatements, "delete");
    }

    public String getSelectStatement(Class<?> entityClass) {
        return getStatement(entityClass, selectStatements, "select");
    }

    protected String getStatement(Class<?> entityClass, Map<Class<?>, String> cachedStatements, String prefix) {
        String statement = cachedStatements.get(entityClass);
        if (statement != null) {
            return statement;
        }
        statement = prefix + entityClass.getSimpleName();
        if (statement.endsWith("Impl")) {
            statement = statement.substring(0, statement.length() - 10); // removing 'entityImpl'
        } else {
            statement = statement.substring(0, statement.length() - 6); // removing 'entity'
        }
        cachedStatements.put(entityClass, statement);
        return statement;
    }

    // db specific mappings
    // /////////////////////////////////////////////////////

    protected void addDatabaseSpecificStatement(String databaseType, String activitiStatement, String ibatisStatement) {
        Map<String, String> specificStatements = databaseSpecificStatements.get(databaseType);
        if (specificStatements == null) {
            specificStatements = new HashMap<>();
            databaseSpecificStatements.put(databaseType, specificStatements);
        }
        specificStatements.put(activitiStatement, ibatisStatement);
    }

    public String mapStatement(String statement) {
        if (statementMappings == null) {
            return statement;
        }
        String mappedStatement = statementMappings.get(statement);
        return (mappedStatement != null ? mappedStatement : statement);
    }

    // customized getters and setters
    // ///////////////////////////////////////////

    public void setDatabaseType(String databaseType) {
        this.databaseType = databaseType;
        this.statementMappings = databaseSpecificStatements.get(databaseType);
    }

    public boolean isMysql() {
        return getDatabaseType().equals("mysql");
    }

    public boolean isOracle() {
        return getDatabaseType().equals("oracle");
    }

    public Boolean isBulkInsertable(Class<? extends Entity> entityClass) {
        return bulkInserteableEntityClasses != null && bulkInserteableEntityClasses.contains(entityClass);
    }

    @SuppressWarnings("rawtypes")
    public String getBulkInsertStatement(Class clazz) {
        return getStatement(clazz, bulkInsertStatements, "bulkInsert");
    }

    public Set<Class<? extends Entity>> getBulkInserteableEntityClasses() {
        return bulkInserteableEntityClasses;
    }

    public void setBulkInserteableEntityClasses(Set<Class<? extends Entity>> bulkInserteableEntityClasses) {
        this.bulkInserteableEntityClasses = bulkInserteableEntityClasses;
    }

    public int getMaxNrOfStatementsInBulkInsert() {
        return maxNrOfStatementsInBulkInsert;
    }

    public void setMaxNrOfStatementsInBulkInsert(int maxNrOfStatementsInBulkInsert) {
        this.maxNrOfStatementsInBulkInsert = maxNrOfStatementsInBulkInsert;
    }

    public Map<Class<?>, String> getBulkInsertStatements() {
        return bulkInsertStatements;
    }

    public void setBulkInsertStatements(Map<Class<?>, String> bulkInsertStatements) {
        this.bulkInsertStatements = bulkInsertStatements;
    }

    // getters and setters //////////////////////////////////////////////////////

    public SqlSessionFactory getSqlSessionFactory() {
        return sqlSessionFactory;
    }

    public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public Map<String, Map<String, String>> getDatabaseSpecificStatements() {
        return databaseSpecificStatements;
    }

    public void setDatabaseSpecificStatements(Map<String, Map<String, String>> databaseSpecificStatements) {
        this.databaseSpecificStatements = databaseSpecificStatements;
    }

    public Map<String, String> getStatementMappings() {
        return statementMappings;
    }

    public void setStatementMappings(Map<String, String> statementMappings) {
        this.statementMappings = statementMappings;
    }

    public Map<Class<?>, String> getInsertStatements() {
        return insertStatements;
    }

    public void setInsertStatements(Map<Class<?>, String> insertStatements) {
        this.insertStatements = insertStatements;
    }

    public Map<Class<?>, String> getUpdateStatements() {
        return updateStatements;
    }

    public void setUpdateStatements(Map<Class<?>, String> updateStatements) {
        this.updateStatements = updateStatements;
    }

    public Map<Class<?>, String> getDeleteStatements() {
        return deleteStatements;
    }

    public void setDeleteStatements(Map<Class<?>, String> deleteStatements) {
        this.deleteStatements = deleteStatements;
    }

    public Map<Class<?>, String> getSelectStatements() {
        return selectStatements;
    }

    public void setSelectStatements(Map<Class<?>, String> selectStatements) {
        this.selectStatements = selectStatements;
    }

    public boolean isDbHistoryUsed() {
        return isDbHistoryUsed;
    }

    public void setDbHistoryUsed(boolean isDbHistoryUsed) {
        this.isDbHistoryUsed = isDbHistoryUsed;
    }

    public void setDatabaseTablePrefix(String databaseTablePrefix) {
        this.databaseTablePrefix = databaseTablePrefix;
    }

    public String getDatabaseTablePrefix() {
        return databaseTablePrefix;
    }

    public String getDatabaseCatalog() {
        return databaseCatalog;
    }

    public void setDatabaseCatalog(String databaseCatalog) {
        this.databaseCatalog = databaseCatalog;
    }

    public String getDatabaseSchema() {
        return databaseSchema;
    }

    public void setDatabaseSchema(String databaseSchema) {
        this.databaseSchema = databaseSchema;
    }

    public void setTablePrefixIsSchema(boolean tablePrefixIsSchema) {
        this.tablePrefixIsSchema = tablePrefixIsSchema;
    }

    public boolean isTablePrefixIsSchema() {
        return tablePrefixIsSchema;
    }

    public List<Class<? extends Entity>> getInsertionOrder() {
        return insertionOrder;
    }

    public void setInsertionOrder(List<Class<? extends Entity>> insertionOrder) {
        this.insertionOrder = insertionOrder;
    }

    public List<Class<? extends Entity>> getDeletionOrder() {
        return deletionOrder;
    }

    public void setDeletionOrder(List<Class<? extends Entity>> deletionOrder) {
        this.deletionOrder = deletionOrder;
    }
    public void addLogicalEntityClassMapping(String logicalName, Class<?> entityClass) {
        logicalNameToClassMapping.put(logicalName, entityClass);
    }

    public Map<String, Class<?>> getLogicalNameToClassMapping() {
        return logicalNameToClassMapping;
    }

    public void setLogicalNameToClassMapping(Map<String, Class<?>> logicalNameToClassMapping) {
        this.logicalNameToClassMapping = logicalNameToClassMapping;
    }
    

}
