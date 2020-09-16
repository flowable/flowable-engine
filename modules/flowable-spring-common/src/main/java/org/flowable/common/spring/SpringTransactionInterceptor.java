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
package org.flowable.common.spring;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.AbstractCommandInterceptor;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Dave Syer
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class SpringTransactionInterceptor extends AbstractCommandInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpringTransactionInterceptor.class);

    protected PlatformTransactionManager transactionManager;

    public SpringTransactionInterceptor(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public <T> T execute(final CommandConfig config, final Command<T> command, CommandExecutor commandExecutor) {
        LOGGER.debug("Running command with propagation {}", config.getTransactionPropagation());


        // If the transaction is required (the other two options always need to go through the transactionTemplate),
        // the transactionTemplate is not used when the transaction is already active.
        // The reason for this is that the transactionTemplate try-catches exceptions and marks it as rollback.
        // Which will break nested service calls that go through the same stack of interceptors.

        int transactionPropagation = getPropagation(config);
        if (transactionPropagation == TransactionTemplate.PROPAGATION_REQUIRED && TransactionSynchronizationManager.isActualTransactionActive()) {
            return next.execute(config, command, commandExecutor);

        } else {
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
            transactionTemplate.setPropagationBehavior(transactionPropagation);
            return transactionTemplate.execute(status -> next.execute(config, command, commandExecutor));

        }

    }

    private int getPropagation(CommandConfig config) {
        switch (config.getTransactionPropagation()) {
        case NOT_SUPPORTED:
            return TransactionTemplate.PROPAGATION_NOT_SUPPORTED;
        case REQUIRED:
            return TransactionTemplate.PROPAGATION_REQUIRED;
        case REQUIRES_NEW:
            return TransactionTemplate.PROPAGATION_REQUIRES_NEW;
        default:
            throw new FlowableIllegalArgumentException("Unsupported transaction propagation: " + config.getTransactionPropagation());
        }
    }
}