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
package org.flowable.common.engine.impl.interceptor;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command interceptor that intercepts Cloud Spanner transaction abort exceptions and retries the command
 * with exponential backoff.
 *
 * @author Flowable
 */
public class SpannerRetryInterceptor extends AbstractCommandInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpannerRetryInterceptor.class);

    protected int nrRetries = 3;
    protected int waitTime = 50;
    protected int waitTimeIncrease = 2;

    @Override
    public <T> T execute(CommandConfig config, Command<T> command, CommandExecutor commandExecutor) {
        long currentWaitTime = this.waitTime;
        int failedAttempts = 0;

        do {
            if (failedAttempts > 0) {
                LOGGER.info("Waiting for {}ms before retrying the Spanner command.", currentWaitTime);
                waitBeforeRetry(currentWaitTime);
                currentWaitTime *= waitTimeIncrease;
            }

            try {
                // try to execute the command
                return next.execute(config, command, commandExecutor);

            } catch (Exception e) {
                if (isTransactionRetryException(e)) {
                    LOGGER.debug("Spanner transaction abort exception caught. Retrying.", e);

                    if (failedAttempts >= nrRetries) {
                        throw e;
                    }
                    failedAttempts++;

                } else {
                    throw e;
                }
            }

        } while (failedAttempts <= nrRetries);

        return null;
    }

    protected void waitBeforeRetry(long waitTime) {
        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            LOGGER.debug("Interrupted while waiting for a retry.");
            Thread.currentThread().interrupt();
        }
    }

    public boolean isTransactionRetryException(Throwable exception) {
        return isTransactionRetryException(exception, new HashSet<>());
    }

    protected boolean isTransactionRetryException(Throwable exception, Set<Throwable> visited) {
        if (exception == null || !visited.add(exception)) {
            return false;
        }

        String className = exception.getClass().getSimpleName();
        String fullClassName = exception.getClass().getName();
        if ("AbortedException".equals(className)
                || "AbortedDueToConcurrentModificationException".equals(className)
                || fullClassName.contains("AbortedException")
                || fullClassName.contains("AbortedDueToConcurrentModificationException")) {
            return true;
        }

        if (exception instanceof SQLException sqlException) {
            if (sqlException.getErrorCode() == 10) {
                return true;
            }
            if (sqlException.getNextException() != null && isTransactionRetryException(sqlException.getNextException(), visited)) {
                return true;
            }
        }

        String message = exception.getMessage();
        if (message != null && message.contains("ABORTED")) {
            return true;
        }

        if (exception.getCause() != null && isTransactionRetryException(exception.getCause(), visited)) {
            return true;
        }

        return false;
    }

    public int getNrRetries() {
        return nrRetries;
    }

    public void setNrRetries(int nrRetries) {
        this.nrRetries = nrRetries;
    }

    public int getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(int waitTime) {
        this.waitTime = waitTime;
    }

    public int getWaitTimeIncrease() {
        return waitTimeIncrease;
    }

    public void setWaitTimeIncrease(int waitTimeIncrease) {
        this.waitTimeIncrease = waitTimeIncrease;
    }
}
