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

import java.sql.SQLIntegrityConstraintViolationException;
import java.time.Duration;
import java.util.function.Supplier;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.common.engine.impl.cfg.TransactionPropagation;
import org.flowable.common.engine.impl.cmd.GetLockValueCmd;
import org.flowable.common.engine.impl.cmd.LockCmd;
import org.flowable.common.engine.impl.cmd.ReleaseLockCmd;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Filip Hrisafov
 */
public class LockManagerImpl implements LockManager {

    protected static final Logger LOGGER = LoggerFactory.getLogger(LockManagerImpl.class);

    protected CommandExecutor commandExecutor;
    protected String lockName;
    protected Duration lockPollRate;
    protected String engineType;
    protected CommandConfig lockCommandConfig;
    protected boolean hasAcquiredLock;
    protected Duration lockForceAcquireAfter;

    public LockManagerImpl(CommandExecutor commandExecutor, String lockName, Duration lockPollRate, String engineType) {
        this(commandExecutor, lockName, lockPollRate, null, engineType);
    }

    public LockManagerImpl(CommandExecutor commandExecutor, String lockName, Duration lockPollRate, Duration forceAcquireAfter, String engineType) {
        this.commandExecutor = commandExecutor;
        this.lockName = lockName;
        this.lockPollRate = lockPollRate;
        this.engineType = engineType;
        this.lockCommandConfig = new CommandConfig(false, TransactionPropagation.REQUIRES_NEW);
        this.lockForceAcquireAfter = forceAcquireAfter;
    }

    @Override
    public void waitForLock(Duration waitTime) {
        long timeToGiveUp = System.currentTimeMillis()+ waitTime.toMillis();
        boolean locked = false;
        while (!locked && (System.currentTimeMillis() < timeToGiveUp)) {
            locked = acquireLock();
            if (!locked) {
                try {
                    Thread.sleep(getLockPollRate().toMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        if (!locked) {
            String lockValue = executeCommand(new GetLockValueCmd(lockName, engineType));
            throw new FlowableException("Could not acquire lock " + lockName + ". Current lock value: " + lockValue);
        }
    }

    @Override
    public boolean acquireLock() {
        if (hasAcquiredLock) {
            return true;
        }

        try {
            hasAcquiredLock = executeCommand(new LockCmd(lockName, lockForceAcquireAfter, engineType));
            if (hasAcquiredLock) {
                LOGGER.debug("Successfully acquired lock {}", lockName);
            }
        } catch (FlowableOptimisticLockingException ex) {
            LOGGER.debug("Failed to acquire lock {} due to optimistic locking", lockName, ex);
            hasAcquiredLock = false;
        } catch (FlowableException ex) {
            if (ex.getClass().equals(FlowableException.class)) {
                // If it is a FlowableException then log a warning and wait to try again
                LOGGER.warn("Failed to acquire lock {} due to unknown exception", lockName, ex);
                hasAcquiredLock = false;
            } else {
                // Re-throw any other Flowable specific exception
                throw ex;
            }
        } catch (RuntimeException ex) {
            if (ex.getCause() instanceof SQLIntegrityConstraintViolationException) {
                // This can happen if 2 nodes try to acquire a lock in the exact same time
                LOGGER.debug("Failed to acquire lock {} due to constraint violation", lockName, ex);
            } else {
                LOGGER.info("Failed to acquire lock {} due to unknown exception", lockName, ex);
            }
            hasAcquiredLock = false;
        }
        return hasAcquiredLock;
    }

    @Override
    public void releaseLock() {
        executeCommand(new ReleaseLockCmd(lockName, engineType));
        LOGGER.debug("successfully released lock {}", lockName);
        hasAcquiredLock = false;
    }

    @Override
    public <T> T waitForLockRunAndRelease(Duration waitTime, Supplier<T> supplier) {
        waitForLock(waitTime);
        try {
            return supplier.get();
        } finally {
            releaseLock();
        }
    }

    protected <T> T executeCommand(Command<T> command) {
        return commandExecutor.execute(lockCommandConfig, command);
    }

    protected Duration getLockPollRate() {
        return lockPollRate;
    }

}
