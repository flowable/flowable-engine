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

import org.flowable.common.engine.api.FlowableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Inspired by {@link RetryInterceptor}, but adapted for CRDB.
 *
 * @author Joram Barrez
 */
public class CockroachDbRetryInterceptor extends AbstractCommandInterceptor {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    protected int nrRetries = 3;
    protected int waitTime = 50;
    protected int waitTimeIncrease = 5;

    @Override
    public <T> T execute(CommandConfig config, Command<T> command) {
        long waitTime = this.waitTime;
        int failedAttempts = 0;

        do {
            if (failedAttempts > 0) {
                LOGGER.info("Waiting for {}ms before retrying the command.", waitTime);
                waitBeforeRetry(waitTime);
                waitTime *= waitTimeIncrease;
            }

            try {

                // try to execute the command
                return next.execute(config, command);

            } catch (Exception e) {
                LOGGER.debug("Exception caught. Retrying.", e);
            }

            failedAttempts++;
        } while (failedAttempts <= nrRetries);

        throw new FlowableException(nrRetries + " retries failed. Stopping");
    }

    protected void waitBeforeRetry(long waitTime) {
        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            LOGGER.debug("Interrupted while waiting for a retry.");
        }
    }

}
