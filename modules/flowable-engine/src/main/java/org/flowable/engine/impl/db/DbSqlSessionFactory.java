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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.flowable.engine.common.impl.db.AbstractDbSqlSession;
import org.flowable.engine.common.impl.db.AbstractDbSqlSessionFactory;
import org.flowable.engine.common.impl.persistence.entity.Entity;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.persistence.entity.EventLogEntryEntityImpl;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class DbSqlSessionFactory extends AbstractDbSqlSessionFactory {
    
    /**
     * A map {class, boolean}, to indicate whether or not a certain {@link Entity} class can be bulk inserted.
     */
    protected Map<Class<? extends Entity>, Boolean> bulkInsertableMap;

    protected Map<Class<?>, String> bulkInsertStatements = new ConcurrentHashMap<Class<?>, String>();
    protected Map<Class<?>, String> bulkDeleteStatements = new ConcurrentHashMap<Class<?>, String>();
    
    protected int maxNrOfStatementsInBulkInsert = 100;

    public Class<?> getSessionType() {
        return DbSqlSession.class;
    }
    
    @Override
    protected AbstractDbSqlSession createDbSqlSession() {
        return new DbSqlSession(this, Context.getCommandContext().getEntityCache());
    }

    public void setBulkInsertEnabled(boolean isBulkInsertEnabled, String databaseType) {
        // If false, just keep don't initialize the map. Memory saved.
        if (isBulkInsertEnabled) {
            initBulkInsertEnabledMap(databaseType);
        }
    }

    protected void initBulkInsertEnabledMap(String databaseType) {
        bulkInsertableMap = new HashMap<Class<? extends Entity>, Boolean>();

        for (Class<? extends Entity> clazz : EntityDependencyOrder.INSERT_ORDER) {
            bulkInsertableMap.put(clazz, Boolean.TRUE);
        }

        // Only Oracle is making a fuss in one specific case right now
        if ("oracle".equals(databaseType)) {
            bulkInsertableMap.put(EventLogEntryEntityImpl.class, Boolean.FALSE);
        }
    }

    public Boolean isBulkInsertable(Class<? extends Entity> entityClass) {
        return bulkInsertableMap != null && bulkInsertableMap.containsKey(entityClass) && bulkInsertableMap.get(entityClass);
    }

    public Map<Class<? extends Entity>, Boolean> getBulkInsertableMap() {
        return bulkInsertableMap;
    }
    
    @SuppressWarnings("rawtypes")
    public String getBulkInsertStatement(Class clazz) {
        return getStatement(clazz, bulkInsertStatements, "bulkInsert");
    }
    
    public String getBulkDeleteStatement(Class<?> entityClass) {
        return getStatement(entityClass, bulkDeleteStatements, "bulkDelete");
    }

    public void setBulkInsertableMap(Map<Class<? extends Entity>, Boolean> bulkInsertableMap) {
        this.bulkInsertableMap = bulkInsertableMap;
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
    
    public Map<Class<?>, String> getBulkDeleteStatements() {
        return bulkDeleteStatements;
    }

    public void setBulkDeleteStatements(Map<Class<?>, String> bulkDeleteStatements) {
        this.bulkDeleteStatements = bulkDeleteStatements;
    }

}
