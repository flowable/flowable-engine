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
import java.util.function.Supplier;

import org.flowable.common.engine.api.lock.LockManager;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;

/**
 * @author Filip Hrisafov
 */
public class EngineSchemaManagerLockConfiguration implements SchemaManagerLockConfiguration {

    protected final Supplier<AbstractEngineConfiguration> engineConfigurationSupplier;

    public EngineSchemaManagerLockConfiguration(Supplier<AbstractEngineConfiguration> engineConfigurationSupplier) {
        this.engineConfigurationSupplier = engineConfigurationSupplier;
    }

    protected AbstractEngineConfiguration getEngineConfiguration() {
        return engineConfigurationSupplier.get();
    }

    @Override
    public boolean isUseLockForDatabaseSchemaUpdate() {
        return getEngineConfiguration().isUseLockForDatabaseSchemaUpdate();
    }

    @Override
    public LockManager getLockManager(String lockName) {
        return getEngineConfiguration().getLockManager(lockName);
    }

    @Override
    public Duration getSchemaLockWaitTime() {
        return getEngineConfiguration().getSchemaLockWaitTime();
    }
}
