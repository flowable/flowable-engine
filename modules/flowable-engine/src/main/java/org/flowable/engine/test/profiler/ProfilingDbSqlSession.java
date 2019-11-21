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
package org.flowable.engine.test.profiler;

import java.sql.Connection;
import java.util.Collection;
import java.util.List;

import org.flowable.common.engine.impl.db.BulkDeleteOperation;
import org.flowable.common.engine.impl.db.DbSqlSession;
import org.flowable.common.engine.impl.db.DbSqlSessionFactory;
import org.flowable.common.engine.impl.persistence.cache.EntityCache;
import org.flowable.common.engine.impl.persistence.entity.Entity;

/**
 * @author Joram Barrez
 */
public class ProfilingDbSqlSession extends DbSqlSession {

    protected CommandExecutionResult commandExecutionResult;

    public ProfilingDbSqlSession(DbSqlSessionFactory dbSqlSessionFactory, EntityCache entityCache) {
        super(dbSqlSessionFactory, entityCache);
    }

    public ProfilingDbSqlSession(DbSqlSessionFactory dbSqlSessionFactory, EntityCache entityCache, Connection connection, String catalog, String schema) {
        super(dbSqlSessionFactory, entityCache, connection, catalog, schema);
    }

    @Override
    public void flush() {
        long startTime = System.currentTimeMillis();
        super.flush();
        long endTime = System.currentTimeMillis();

        CommandExecutionResult commandExecutionResult = getCurrentCommandExecution();
        if (commandExecutionResult != null) {
            commandExecutionResult.addDatabaseTime(endTime - startTime);
        }
    }

    @Override
    public void commit() {

        long startTime = System.currentTimeMillis();
        super.commit();
        long endTime = System.currentTimeMillis();

        CommandExecutionResult commandExecutionResult = getCurrentCommandExecution();
        if (commandExecutionResult != null) {
            commandExecutionResult.addDatabaseTime(endTime - startTime);
        }
    }

    // SELECT ONE

    @Override
    public Object selectOne(String statement, Object parameter) {
        if (getCurrentCommandExecution() != null) {
            getCurrentCommandExecution().addDbSelect(statement);
        }
        return super.selectOne(statement, parameter);
    }

    @Override
    public <T extends Entity> T selectById(Class<T> entityClass, String id, boolean useCache) {
        if (getCurrentCommandExecution() != null) {
            getCurrentCommandExecution().addDbSelect("selectById " + entityClass.getName());
        }
        return super.selectById(entityClass, id, useCache);
    }

    // SELECT LIST

    @Override
    public List selectListWithRawParameter(String statement, Object parameter, boolean useCache) {
        if (getCurrentCommandExecution() != null) {
            getCurrentCommandExecution().addDbSelect(statement);
        }
        return super.selectListWithRawParameter(statement, parameter, useCache);
    }

    @Override
    public List selectListWithRawParameterNoCacheLoadAndStore(String statement, Object parameter) {
        if (getCurrentCommandExecution() != null) {
            getCurrentCommandExecution().addDbSelect(statement);
        }
        return super.selectListWithRawParameterNoCacheLoadAndStore(statement, parameter);
    }

    // INSERTS

    @Override
    protected void flushRegularInsert(Entity entity, Class<? extends Entity> clazz) {
        super.flushRegularInsert(entity, clazz);
        if (getCurrentCommandExecution() != null) {
            getCurrentCommandExecution().addDbInsert(clazz.getName());
        }
    }

    @Override
    protected void flushBulkInsert(Collection<Entity> entities, Class<? extends Entity> clazz) {
        if (getCurrentCommandExecution() != null && entities.size() > 0) {
            getCurrentCommandExecution().addDbInsert(clazz.getName() + "-bulk-with-" + entities.size());
        }
        super.flushBulkInsert(entities, clazz);
    }

    // UPDATES

    @Override
    protected void flushUpdates() {
        if (getCurrentCommandExecution() != null) {
            for (Entity persistentObject : updatedObjects) {
                getCurrentCommandExecution().addDbUpdate(persistentObject.getClass().getName());
            }
        }

        super.flushUpdates();
    }

    // DELETES

    @Override
    protected void flushDeleteEntities(Class<? extends Entity> entityClass, Collection<Entity> entitiesToDelete) {
        super.flushDeleteEntities(entityClass, entitiesToDelete);
        if (getCurrentCommandExecution() != null) {
            for (Entity entity : entitiesToDelete) {
                getCurrentCommandExecution().addDbDelete(entity.getClass().getName());
            }
        }
    }

    @Override
    protected void flushBulkDeletes(Class<? extends Entity> entityClass, List<BulkDeleteOperation> deleteOperations) {
        // Bulk deletes
        if (getCurrentCommandExecution() != null && deleteOperations != null) {
            for (BulkDeleteOperation bulkDeleteOperation : deleteOperations) {
                getCurrentCommandExecution().addDbDelete("Bulk-delete-" + bulkDeleteOperation.getStatement());
            }
        }
        super.flushBulkDeletes(entityClass, deleteOperations);
    }

    public CommandExecutionResult getCurrentCommandExecution() {
        if (commandExecutionResult == null) {
            ProfileSession profileSession = FlowableProfiler.getInstance().getCurrentProfileSession();
            if (profileSession != null) {
                this.commandExecutionResult = profileSession.getCurrentCommandExecution();
            }
        }
        return commandExecutionResult;
    }
}
