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
package org.flowable.job.service.impl.history.async;

import java.util.ArrayList;
import java.util.List;

import org.flowable.common.engine.impl.cfg.TransactionListener;
import org.flowable.common.engine.impl.cfg.TransactionPropagation;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;
import org.flowable.job.service.impl.util.CommandContextUtil;

/**
 * A {@link TransactionListener} that will, typically on post-commit, trigger 
 * the async history executor to execute the provided list of {@link HistoryJobEntity} instances. 
 * 
 * @author Joram Barrez
 */
public class TriggerAsyncExecutorTransactionListener implements TransactionListener {
    
    protected List<HistoryJobEntity> historyJobEntities;
    
    protected  JobServiceConfiguration jobServiceConfiguration;
    
    public TriggerAsyncExecutorTransactionListener(CommandContext commandContext) {
        this(commandContext,null);
    }
    
    public TriggerAsyncExecutorTransactionListener(CommandContext commandContext, HistoryJobEntity historyJobEntity) {
        // The execution of this listener will reference components that might 
        // not be available when the command context is closing (when typically 
        // the history jobs are created and scheduled), so they are already referenced here.
        this.jobServiceConfiguration = CommandContextUtil.getJobServiceConfiguration(commandContext);
        
        if (historyJobEntity != null) {
            this.historyJobEntities = new ArrayList<>(1);
            this.historyJobEntities.add(historyJobEntity);
        }
    }
    
    public void addHistoryJobEntity(HistoryJobEntity historyJobEntity) {
        if (this.historyJobEntities == null) {
            this.historyJobEntities = new ArrayList<>(1);
        }
        this.historyJobEntities.add(historyJobEntity);
    }
    
    public void addHistoryJobEntities(List<HistoryJobEntity> historyJobEntities) {
        if (this.historyJobEntities == null) {
            this.historyJobEntities = new ArrayList<>(historyJobEntities.size());
        }
        this.historyJobEntities.addAll(historyJobEntities);
    }
    
    @Override
    public void execute(CommandContext commandContext) {
        // Each needs to run in a separate transaction, as the trigger could fail independently.
        for (HistoryJobEntity historyJobEntity : historyJobEntities) {
            jobServiceConfiguration.getCommandExecutor().execute(new CommandConfig(false, TransactionPropagation.REQUIRES_NEW), new Command<Void>() {
                @Override
                public Void execute(CommandContext commandContext) {
                    jobServiceConfiguration.getAsyncHistoryExecutor().executeAsyncJob(historyJobEntity);
                    return null;
                }
            });
        }
    }

}
