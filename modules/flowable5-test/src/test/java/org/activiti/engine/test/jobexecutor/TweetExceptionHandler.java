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
package org.activiti.engine.test.jobexecutor;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tom Baeyens
 */
public class TweetExceptionHandler implements JobHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TweetExceptionHandler.class);

    protected int exceptionsRemaining = 2;

    public String getType() {
        return "tweet-exception";
    }

    public void execute(JobEntity job, String configuration, VariableScope variableScope, CommandContext commandContext) {
        if (exceptionsRemaining > 0) {
            exceptionsRemaining--;
            throw new RuntimeException("exception remaining: " + exceptionsRemaining);
        }
        LOGGER.info("no more exceptions to throw.");
    }

    public int getExceptionsRemaining() {
        return exceptionsRemaining;
    }

    public void setExceptionsRemaining(int exceptionsRemaining) {
        this.exceptionsRemaining = exceptionsRemaining;
    }
}
