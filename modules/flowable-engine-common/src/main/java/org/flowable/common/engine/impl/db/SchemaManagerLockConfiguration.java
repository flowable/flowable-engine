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

import java.time.Duration;

import org.flowable.common.engine.api.lock.LockManager;

/**
 * @author Filip Hrisafov
 */
public interface SchemaManagerLockConfiguration {

    boolean isUseLockForDatabaseSchemaUpdate();

    LockManager getLockManager(String lockName);

    /**
     * @param reuseCurrentCommandContext when {@code true}, the returned lock manager acquires/releases the lock using the
     *         currently active command context (same connection/transaction), instead of a brand new one. This must be used
     *         when locking around schema creation/update, to avoid a self-deadlock on databases with transactional DDL
     *         (SQL Server, PostgreSQL, ...) - see {@code LockManagerImpl} for details.
     */
    default LockManager getLockManager(String lockName, boolean reuseCurrentCommandContext) {
        return getLockManager(lockName);
    }

    Duration getSchemaLockWaitTime();
}
