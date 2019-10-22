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
import java.util.Date;

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
    protected CommandConfig lockCommandConfig;
    protected boolean hasAcquiredLock;

    public LockManagerImpl(CommandExecutor commandExecutor, String lockName, Duration lockPollRate) {
        this.commandExecutor = commandExecutor;
        this.lockName = lockName;
        this.lockPollRate = lockPollRate;
        this.lockCommandConfig = new CommandConfig(false, TransactionPropagation.REQUIRES_NEW);
    }

    @Override
    public void waitForLock(Duration waitTime) {
        long timeToGiveUp = new Date().getTime() + waitTime.toMillis();
        boolean locked = false;
        while (!locked && (new Date().getTime() < timeToGiveUp)) {
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
            String lockValue = executeCommand(new GetLockValueCmd(lockName));
            throw new FlowableException("Could not acquire lock " + lockName + ". Current lock value: " + lockValue);
        }
    }

    @Override
    public boolean acquireLock() {
        if (hasAcquiredLock) {
            return true;
        }

        try {
            hasAcquiredLock = executeCommand(new LockCmd(lockName));
            LOGGER.debug("successfully acquired lock {}", lockName);
        } catch (FlowableOptimisticLockingException ex) {
            LOGGER.debug("failed to acquire lock {} due to optimistic locking", lockName, ex);
            hasAcquiredLock = false;
        }
        return hasAcquiredLock;
    }

    @Override
    public void releaseLock() {
        executeCommand(new ReleaseLockCmd(lockName));
        LOGGER.debug("successfully released lock {}", lockName);
        hasAcquiredLock = false;
    }

    protected <T> T executeCommand(Command<T> command) {
        return commandExecutor.execute(lockCommandConfig, command);
    }

    protected Duration getLockPollRate() {
        return lockPollRate;
    }

}
