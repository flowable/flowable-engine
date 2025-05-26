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
package org.flowable.job.service.impl.nontx;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;

/**
 * Internal interface for non-transactional job handler implementations.
 * This interface extends from the regular {@link JobHandler}, however the 'normal' execute method should not be called,
 * because there's no {@link org.flowable.variable.api.delegate.VariableScope} or {@link org.flowable.common.engine.impl.interceptor.CommandContext}
 * available as no transaction is currently ongoing when the job is executed.
 *
 * @author Joram Barrez
 */
public interface NonTransactionalJobHandler extends JobHandler {

    default void execute(JobEntity job, String configuration, VariableScope variableScope, CommandContext commandContext) {
        throw new UnsupportedOperationException();
    }

    void executeNonTransactionally(JobEntity job, String configuration);

}
