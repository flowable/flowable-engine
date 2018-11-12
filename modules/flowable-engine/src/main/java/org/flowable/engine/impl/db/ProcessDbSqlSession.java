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

import static java.util.stream.Collectors.toSet;

import java.sql.Connection;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.flowable.common.engine.impl.db.BulkDeleteOperation;
import org.flowable.common.engine.impl.db.DbSqlSession;
import org.flowable.common.engine.impl.db.DbSqlSessionFactory;
import org.flowable.common.engine.impl.persistence.cache.EntityCache;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.engine.impl.persistence.entity.ActivityInstanceEntityImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;

/**
 * @author martin.grofcik
 */
public class ProcessDbSqlSession extends DbSqlSession {

    public ProcessDbSqlSession(DbSqlSessionFactory processDbSqlSessionFactory, EntityCache session) {
        super(processDbSqlSessionFactory, session);
    }

    public ProcessDbSqlSession(DbSqlSessionFactory dbSqlSessionFactory, EntityCache entityCache, Connection connection, String catalog, String schema) {
        super(dbSqlSessionFactory, entityCache, connection, catalog, schema);
    }

    protected void removeUnnecessaryOperations() {
        removeUnnecessaryActivityInstanceBulkDelete();
        removeUnnecessaryActivityInstanceInserts();
        super.removeUnnecessaryOperations();
    }

    protected void removeUnnecessaryActivityInstanceInserts() {
        // in the case when process instance was deleted there is no need to insert its ActivityInstances
        if (deletedObjects.containsKey(ExecutionEntityImpl.class) && insertedObjects.containsKey(ActivityInstanceEntityImpl.class)) {
            Collection<Entity> deletedExecutions = deletedObjects.get(ExecutionEntityImpl.class).values();
            Collection<Entity> insertedActivities = new HashSet<>(insertedObjects.get(ActivityInstanceEntityImpl.class).values());
            for (Entity deletedEntity : deletedExecutions) {
                ExecutionEntityImpl deletedExecution = (ExecutionEntityImpl) deletedEntity;
                if (deletedExecution.isProcessInstanceType()) {
                    for (Entity insertedEntity : insertedActivities) {
                        ActivityInstanceEntityImpl insertedActivityInstance = (ActivityInstanceEntityImpl) insertedEntity;
                        if (deletedExecution.getId().equals(insertedActivityInstance.getProcessInstanceId())) {
                            insertedObjects.get(ActivityInstanceEntityImpl.class).remove(insertedActivityInstance.getId());
                        }
                    }
                }
            }
        }
    }

    protected void removeUnnecessaryActivityInstanceBulkDelete() {
        // when process instance was just created and deleted right after that, there is no need to perform ActivityInstanceEntityImpl bulk delete
        if (insertedObjects.containsKey(ExecutionEntityImpl.class) && bulkDeleteOperations.containsKey(ActivityInstanceEntityImpl.class)) {
            Collection<Entity> insertedExecutions = insertedObjects.get(ExecutionEntityImpl.class).values();
            Set<BulkDeleteOperation> deleteActivityInstancesByProcessInstanceIds = this.bulkDeleteOperations.get(ActivityInstanceEntityImpl.class).stream().
                filter(bulkDeleteOperation -> bulkDeleteOperation.getStatement().equals("deleteActivityInstancesByProcessInstanceId")).
                collect(toSet());

            for (Entity insertedExecution : insertedExecutions) {
                if (((ExecutionEntity) insertedExecution).isProcessInstanceType()) {
                    deleteActivityInstancesByProcessInstanceIds.stream().
                        filter(bulkDeleteOperation -> insertedExecution.getId().equals(bulkDeleteOperation.getParameter())).
                        findFirst().
                        ifPresent(
                            unnecessaryBulkDeleteOperation -> this.bulkDeleteOperations.get(ActivityInstanceEntityImpl.class).remove(unnecessaryBulkDeleteOperation)
                        );
                }
            }
        }
    }

}
