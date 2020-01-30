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
package org.flowable.common.engine.impl.lock;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * @author Filip Hrisafov
 */
public interface LockManager {

    /**
     * Wait for the given {@code waitTime} to acquire the lock
     *
     * @param waitTime the duration to wait before throwing an exception
     */
    void waitForLock(Duration waitTime);

    /**
     * Acquire the lock.
     *
     * @return {@code true} if the lock was acquired, {@code false} otherwise
     */
    boolean acquireLock();

    /**
     * Release the lock.
     */
    void releaseLock();

    /**
     * Wait to acquire a lock, once a lock is acquired execute the supplier and release finally the lock.
     *
     * @param waitTime the duration to wait before throwing an exception
     * @param supplier the supplier to be executed once the lock is acquired
     * @return the result from the {@code supplier}
     */
    <T> T waitForLockRunAndRelease(Duration waitTime, Supplier<T> supplier);

}
