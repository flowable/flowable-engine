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

import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.FlowableOptimisticLockingException;
import org.flowable.engine.common.impl.persistence.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractNonCachingDbSqlSession extends AbstractDbSqlSession {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractNonCachingDbSqlSession.class);

    public AbstractNonCachingDbSqlSession(AbstractDbSqlSessionFactory dbSqlSessionFactory) {
        super(dbSqlSessionFactory);
    }
    
    public AbstractNonCachingDbSqlSession(AbstractDbSqlSessionFactory dbSqlSessionFactory, Connection connection, String catalog, String schema) {
        super(dbSqlSessionFactory, connection, catalog, schema);
    }
    
    // insert ///////////////////////////////////////////////////////////////////

    public void insert(Entity entity) {
        if (entity.getId() == null) {
            String id = dbSqlSessionFactory.getIdGenerator().getNextId();
            entity.setId(id);
        }

        String insertStatement = dbSqlSessionFactory.getInsertStatement(entity);
        insertStatement = dbSqlSessionFactory.mapStatement(insertStatement);

        if (insertStatement == null) {
            throw new FlowableException("no insert statement for " + entity.getClass() + " in the ibatis mapping files");
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("inserting: {}", entity);
        }
        sqlSession.insert(insertStatement, entity);
    }

    // update
    // ///////////////////////////////////////////////////////////////////

    public void update(Entity entity) {
        String updateStatement = dbSqlSessionFactory.getUpdateStatement(entity);
        updateStatement = dbSqlSessionFactory.mapStatement(updateStatement);

        if (updateStatement == null) {
            throw new FlowableException("no update statement for " + entity.getClass() + " in the ibatis mapping files");
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("updating: {}", entity);
        }
        int updatedRecords = sqlSession.update(updateStatement, entity);
        if (updatedRecords == 0) {
            throw new FlowableOptimisticLockingException(entity + " was updated by another transaction concurrently");
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
            throw new FlowableException("no delete statement for " + entity.getClass() + " in the ibatis mapping files");
        }

        sqlSession.delete(deleteStatement, entity);
    }

}
