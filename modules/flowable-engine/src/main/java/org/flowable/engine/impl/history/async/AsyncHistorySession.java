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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.flowable.engine.common.impl.interceptor.Session;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.interceptor.CommandContextCloseListener;

public class AsyncHistorySession implements Session {

    protected CommandContext commandContext;
    protected AsyncHistoryJobProducer asyncHistoryJobProducer;
    protected CommandContextCloseListener commandContextCloseListener;

    protected String tenantId;
    protected List<Pair<String, Map<String, String>>> jobData;

    public AsyncHistorySession(CommandContext commandContext, AsyncHistoryJobProducer asyncHistoryJobProducer) {
        this.commandContext = commandContext;
        this.asyncHistoryJobProducer = asyncHistoryJobProducer;
        initCommandContextCloseListener();
    }

    protected void initCommandContextCloseListener() {
        this.commandContextCloseListener = new CommandContextCloseListener() {

            @Override
            public void closing(CommandContext commandContext) {
                asyncHistoryJobProducer.createAsyncHistoryJobs(commandContext);
            }

            @Override
            public void closed(CommandContext commandContext) {
            }

            @Override
            public void closeFailure(CommandContext commandContext) {
            }

            @Override
            public void afterSessionsFlush(CommandContext commandContext) {
            }

        };
    }

    public void addHistoricData(String type, Map<String, String> data) {
        addHistoricData(type, data, null);
    }

    public void addHistoricData(String type, Map<String, String> data, String tenantId) {
        if (jobData == null) {
            jobData = new ArrayList<Pair<String, Map<String, String>>>();
            commandContext.addCloseListener(commandContextCloseListener);
        }
        if (tenantId != null) {
            this.tenantId = tenantId;
        }
        jobData.add(Pair.of(type, data));
    }
    
    public boolean isActivityStartPresentWithoutEnd(String activityId, String executionId) {
        boolean foundActivityStartWithoutEnd = false;
        if (jobData != null) {
            Map<String, List<Pair<String, Map<String, String>>>> activityStartMap = new HashMap<>();
            for (Pair<String, Map<String, String>> historicData : jobData) {
                if ("activity-start".equals(historicData.getKey())) {
                    
                    String activityKey = historicData.getValue().get(HistoryJsonConstants.EXECUTION_ID) + "_" + 
                                    historicData.getValue().get(HistoryJsonConstants.ACTIVITY_ID);
                    
                    List<Pair<String, Map<String, String>>> activityHistoricData = null;
                    if (activityStartMap.containsKey(activityKey)) {
                        activityHistoricData = activityStartMap.get(activityKey);
                    } else {
                        activityHistoricData = new ArrayList<>();
                    }
                    
                    activityHistoricData.add(historicData);
                    activityStartMap.put(activityKey, activityHistoricData);
                }
            }
            
            for (Pair<String, Map<String, String>> historicData : jobData) {
                if ("activity-end".equals(historicData.getKey())) {
                    
                    String activityKey = historicData.getValue().get(HistoryJsonConstants.EXECUTION_ID) + "_" + 
                                    historicData.getValue().get(HistoryJsonConstants.ACTIVITY_ID);
                    
                    if (activityStartMap.containsKey(activityKey)) {
                        List<Pair<String, Map<String, String>>> activityHistoricData = activityStartMap.get(activityKey);
                        activityHistoricData.remove(0);
                        if (activityHistoricData.size() == 0) {
                            activityStartMap.remove(activityKey);
                        } else {
                            activityStartMap.put(activityKey, activityHistoricData); 
                        }
                    }
                }
            }
            
            if (activityStartMap.containsKey(executionId + "_" + activityId)) {
                foundActivityStartWithoutEnd = true;
            }
        }
        
        return foundActivityStartWithoutEnd;
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

    public List<Pair<String, Map<String, String>>> getJobData() {
        return jobData;
    }

    public void setJobData(List<Pair<String, Map<String, String>>> jobData) {
        this.jobData = jobData;
    }

}
