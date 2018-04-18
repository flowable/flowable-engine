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
package org.flowable.engine.impl.history.async;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.cfg.TransactionContext;
import org.flowable.common.engine.impl.cfg.TransactionState;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandContextCloseListener;
import org.flowable.common.engine.impl.interceptor.Session;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncHistorySession implements Session {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncHistorySession.class);

    protected CommandContext commandContext;
    protected AsyncHistoryListener asyncHistoryListener;
    protected CommandContextCloseListener commandContextCloseListener;
    protected AsyncHistoryCommittedTransactionListener asyncHistoryCommittedTransactionListener;

    protected String tenantId;
    protected Map<String, List<Map<String, String>>> jobData;

    public AsyncHistorySession(CommandContext commandContext, AsyncHistoryListener asyncHistoryJobListener) {
        this.commandContext = commandContext;
        this.asyncHistoryListener = asyncHistoryJobListener;
        initCommandContextCloseListener();
        
        if (CommandContextUtil.getProcessEngineConfiguration(commandContext).isAsyncHistoryExecutorIsMessageQueueMode()) {
            intitTransactionListener();
        }
    }

    protected void initCommandContextCloseListener() {
        this.commandContextCloseListener = new AsyncHistorySessionCommandContextCloseListener(this, asyncHistoryListener); 
    }
    
    protected void intitTransactionListener() {
        
        /* 
         * The transaction listener needed to send a message to a message queue on state committed
         * needs to be registered here, as in the MessageBasedJobManager#sendMessage (where it would make more sense),
         * the TransactionContext has already been removed.
         * 
         * The message does need to be sent after commit, otherwise there can be race conditions between
         * the db commit and the message handling. 
         */
        
        TransactionContext transactionContext = Context.getTransactionContext();
        if (transactionContext != null) {
            this.asyncHistoryCommittedTransactionListener = new AsyncHistoryCommittedTransactionListener();
            transactionContext.addTransactionListener(TransactionState.COMMITTED, asyncHistoryCommittedTransactionListener);
        } else {
            LOGGER.warn("No transaction context active, but one is required for proper message queue based async history.");
        }
    }

    public void addHistoricData(String type, Map<String, String> data) {
        addHistoricData(type, data, null);
    }

    public void addHistoricData(String type, Map<String, String> data, String tenantId) {
        
        data.put(HistoryJsonConstants.TIMESTAMP, AsyncHistoryDateUtil.formatDate(CommandContextUtil.getProcessEngineConfiguration(commandContext).getClock().getCurrentTime()));
        
        if (jobData == null) {
            jobData = new LinkedHashMap<>(); // linked: insertion order is important
            commandContext.addCloseListener(commandContextCloseListener);
        }
        if (tenantId != null) {
            this.tenantId = tenantId;
        }
        
        if (!jobData.containsKey(type)) {
            jobData.put(type, new ArrayList<Map<String, String>>(1));
        }
        jobData.get(type).add(data);
    }
    
    @Override
    public void flush() {

    }

    @Override
    public void close() {

    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Map<String, List<Map<String, String>>> getJobData() {
        return jobData;
    }

    public void setJobData(Map<String, List<Map<String, String>>> jobData) {
        this.jobData = jobData;
    }
    
    public void addAsyncHistoryRunnableAfterCommit(Runnable runnable) {
        if (asyncHistoryCommittedTransactionListener != null) {
            asyncHistoryCommittedTransactionListener.addRunnable(runnable);
        } else {
            LOGGER.warn("Cannot register a Runnable instance when no transaction listener is active");
        }
    }
    
}
