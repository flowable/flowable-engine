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
package org.flowable.engine.common.impl.db;

import java.sql.Connection;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.ibatis.session.SqlSession;
import org.flowable.engine.common.impl.Page;
import org.flowable.engine.common.impl.interceptor.Session;
import org.flowable.engine.common.impl.persistence.entity.Entity;

public abstract class AbstractDbSqlSession implements Session {
    
    protected static final Pattern CLEAN_VERSION_REGEX = Pattern.compile("\\d\\.\\d*");
    
    public static String[] JDBC_METADATA_TABLE_TYPES = { "TABLE" };
    
    protected SqlSession sqlSession;
    protected AbstractDbSqlSessionFactory dbSqlSessionFactory;
    protected String connectionMetadataDefaultCatalog;
    protected String connectionMetadataDefaultSchema;
    
    public AbstractDbSqlSession(AbstractDbSqlSessionFactory dbSqlSessionFactory) {
        this.dbSqlSessionFactory = dbSqlSessionFactory;
        this.sqlSession = dbSqlSessionFactory.getSqlSessionFactory().openSession();
    }

    public AbstractDbSqlSession(AbstractDbSqlSessionFactory dbSqlSessionFactory, Connection connection, String catalog, String schema) {
        this.dbSqlSessionFactory = dbSqlSessionFactory;
        this.sqlSession = dbSqlSessionFactory.getSqlSessionFactory().openSession(connection); // Note the use of connection param here, different from other constructor
        this.connectionMetadataDefaultCatalog = catalog;
        this.connectionMetadataDefaultSchema = schema;
    }
    
    @SuppressWarnings({ "rawtypes" })
    public List selectList(String statement) {
        return selectList(statement, null, -1, -1);
    }

    @SuppressWarnings("rawtypes")
    public List selectList(String statement, Object parameter) {
        return selectList(statement, parameter, -1, -1);
    }
    
    @SuppressWarnings("rawtypes")
    public List selectList(String statement, Object parameter, Page page) {
        if (page != null) {
            return selectList(statement, parameter, page.getFirstResult(), page.getMaxResults());
        } else {
            return selectList(statement, parameter, -1, -1);
        }
    }
    
    @SuppressWarnings("rawtypes")
    public List selectList(String statement, ListQueryParameterObject parameter) {
        parameter.setDatabaseType(dbSqlSessionFactory.getDatabaseType());
        return selectListWithRawParameter(statement, parameter);
    }
    
    @SuppressWarnings("rawtypes")
    public List selectList(String statement, Object parameter, int firstResult, int maxResults) {
        return selectList(statement, new ListQueryParameterObject(parameter, firstResult, maxResults));
    }
    
    @SuppressWarnings("rawtypes")
    public List selectListWithRawParameter(String statement, Object parameter) {
        statement = dbSqlSessionFactory.mapStatement(statement);
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
    
    public abstract void insert(Entity entity);

    public abstract void update(Entity entity);
        
    public abstract int update(String statement, Object parameters);

    public abstract void delete(String statement, Object parameter);
        
    public abstract void delete(Entity entity);
    
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
    
    public <T> T getCustomMapper(Class<T> type) {
        return sqlSession.getMapper(type);
    }

    // getters and setters
    // //////////////////////////////////////////////////////

    public SqlSession getSqlSession() {
        return sqlSession;
    }
    
    public AbstractDbSqlSessionFactory getDbSqlSessionFactory() {
        return dbSqlSessionFactory;
    }

    public String getConnectionMetadataDefaultCatalog() {
        return connectionMetadataDefaultCatalog;
    }

    public void setConnectionMetadataDefaultCatalog(String connectionMetadataDefaultCatalog) {
        this.connectionMetadataDefaultCatalog = connectionMetadataDefaultCatalog;
    }

    public String getConnectionMetadataDefaultSchema() {
        return connectionMetadataDefaultSchema;
    }

    public void setConnectionMetadataDefaultSchema(String connectionMetadataDefaultSchema) {
        this.connectionMetadataDefaultSchema = connectionMetadataDefaultSchema;
    }
    
}
