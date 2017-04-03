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

package org.flowable.engine.common.impl.interceptor;

import org.flowable.engine.common.impl.cfg.BaseTransactionContext;

/**
 * @author Joram Barrez
 */
public class TransactionCommandContextCloseListener implements BaseCommandContextCloseListener<AbstractCommandContext> {

    protected BaseTransactionContext transactionContext;

    public TransactionCommandContextCloseListener(BaseTransactionContext transactionContext) {
        this.transactionContext = transactionContext;
    }

    @Override
    public void closing(AbstractCommandContext commandContext) {

    }

    @Override
    public void afterSessionsFlush(AbstractCommandContext commandContext) {
        transactionContext.commit();
    }

    @Override
    public void closed(AbstractCommandContext commandContext) {

    }

    @Override
    public void closeFailure(AbstractCommandContext commandContext) {
        transactionContext.rollback();
    }

}
