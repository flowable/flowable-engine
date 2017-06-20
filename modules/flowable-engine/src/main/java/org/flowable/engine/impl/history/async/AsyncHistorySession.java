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

import org.flowable.engine.common.impl.interceptor.Session;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.interceptor.CommandContextCloseListener;

public class AsyncHistorySession implements Session {

    protected CommandContext commandContext;
    protected AsyncHistoryListener asyncHistoryListener;
    protected CommandContextCloseListener commandContextCloseListener;

    protected String tenantId;
    protected Map<String, List<Map<String, String>>> jobData;

    public AsyncHistorySession(CommandContext commandContext, AsyncHistoryListener asyncHistoryJobListener) {
        this.commandContext = commandContext;
        this.asyncHistoryListener = asyncHistoryJobListener;
        initCommandContextCloseListener();
    }

    protected void initCommandContextCloseListener() {
        this.commandContextCloseListener = new AsyncHistorySessionCommandContextCloseListener(this, asyncHistoryListener); 
    }

    public void addHistoricData(String type, Map<String, String> data) {
        addHistoricData(type, data, null);
    }

    public void addHistoricData(String type, Map<String, String> data, String tenantId) {
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
    
}
