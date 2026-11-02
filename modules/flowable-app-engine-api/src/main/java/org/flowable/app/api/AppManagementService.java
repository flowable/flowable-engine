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
package org.flowable.app.api;

import java.util.Collection;
import java.util.Map;

import org.flowable.common.engine.api.lock.LockManager;

/**
 * @author Tijs Rademakers
 */
public interface AppManagementService {

    /**
     * Returns a map containing {tableName, rowCount} values.
     */
    Map<String, Long> getTableCounts();
    
    /**
     * Returns all relational database tables of the engine. 
     */
    Collection<String> getTableNames();

    /**
     * Acquire a lock manager for the requested lock.
     * This is a stateless call, this means that every time a lock manager
     * is requested a new one would be created. Make sure that you release the lock
     * once you are done.
     *
     * @param lockName the name of the lock that is being requested
     *
     * @return the lock manager for the given lock
     */
    LockManager getLockManager(String lockName);
}
