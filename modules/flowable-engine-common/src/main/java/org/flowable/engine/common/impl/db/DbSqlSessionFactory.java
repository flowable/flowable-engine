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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.flowable.engine.common.impl.context.Context;
import org.flowable.engine.common.impl.persistence.cache.EntityCache;
import org.flowable.engine.common.impl.persistence.entity.Entity;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class DbSqlSessionFactory extends AbstractDbSqlSessionFactory {
    
    protected Set<Class<? extends Entity>> bulkInserteableEntityClasses = new HashSet<>();
    protected Map<Class<?>, String> bulkInsertStatements = new ConcurrentHashMap<Class<?>, String>();
    
    protected int maxNrOfStatementsInBulkInsert = 100;

    public Class<?> getSessionType() {
        return DbSqlSession.class;
    }
    
    @Override
    protected AbstractDbSqlSession createDbSqlSession() {
        return new DbSqlSession(this, Context.getCommandContext().getSession(EntityCache.class));
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
    
}
