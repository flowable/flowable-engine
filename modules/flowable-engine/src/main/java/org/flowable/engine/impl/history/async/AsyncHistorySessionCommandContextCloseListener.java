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
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.interceptor.CommandContextCloseListener;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class AsyncHistorySessionCommandContextCloseListener implements CommandContextCloseListener {
    
    public static List<String> TYPE_ORDER = Arrays.asList(
                HistoryJsonConstants.TYPE_PROCESS_INSTANCE_START,
                HistoryJsonConstants.TYPE_PROCESS_INSTANCE_PROPERTY_CHANGED,
                HistoryJsonConstants.TYPE_ACTIVITY_START,
                HistoryJsonConstants.TYPE_ACTIVITY_END,
                HistoryJsonConstants.TYPE_ACTIVITY_FULL,
                HistoryJsonConstants.TYPE_TASK_CREATED,
                HistoryJsonConstants.TYPE_TASK_ASSIGNEE_CHANGED,
                HistoryJsonConstants.TYPE_TASK_OWNER_CHANGED,
                HistoryJsonConstants.TYPE_TASK_PROPERTY_CHANGED,
                HistoryJsonConstants.TYPE_TASK_ENDED,
                HistoryJsonConstants.TYPE_VARIABLE_CREATED,
                HistoryJsonConstants.TYPE_VARIABLE_UPDATED,
                HistoryJsonConstants.TYPE_VARIABLE_REMOVED,
                HistoryJsonConstants.TYPE_HISTORIC_DETAIL_VARIABLE_UPDATE,
                HistoryJsonConstants.TYPE_FORM_PROPERTIES_SUBMITTED,
                HistoryJsonConstants.TYPE_SET_PROCESS_DEFINITION,
                HistoryJsonConstants.TYPE_SUBPROCESS_INSTANCE_START,
                HistoryJsonConstants.TYPE_IDENTITY_LINK_CREATED,
                HistoryJsonConstants.TYPE_IDENTITY_LINK_DELETED,
                HistoryJsonConstants.TYPE_PROCESS_INSTANCE_DELETED_BY_PROCDEF_ID,
                HistoryJsonConstants.TYPE_PROCESS_INSTANCE_DELETED,
                HistoryJsonConstants.TYPE_PROCESS_INSTANCE_END
            );
    
    protected AsyncHistorySession asyncHistorySession;
    protected AsyncHistoryListener asyncHistoryListener;
    
    public AsyncHistorySessionCommandContextCloseListener() {
        
    }
    
    public AsyncHistorySessionCommandContextCloseListener(AsyncHistorySession asyncHistorySession, AsyncHistoryListener asyncHistoryListener) {
        this.asyncHistorySession = asyncHistorySession;
        this.asyncHistoryListener = asyncHistoryListener;
    }
    
    @Override
    public void closing(CommandContext commandContext) {
        Map<String, List<Map<String, String>>> jobData = asyncHistorySession.getJobData();
        if (!jobData.isEmpty()) {
            List<ObjectNode> objectNodes = new ArrayList<>();
            for (String type : TYPE_ORDER) {
                if (jobData.containsKey(type)) {
                    generateJson(commandContext, jobData, objectNodes, type);
                }
            }
            
            if (!jobData.isEmpty()) {
                for (String type : jobData.keySet()) {
                    generateJson(commandContext, jobData, objectNodes, type);
                }
            }
            asyncHistoryListener.historyDataGenerated(objectNodes);
        }
    }

    protected void generateJson(CommandContext commandContext, Map<String, List<Map<String, String>>> jobData, List<ObjectNode> objectNodes, String type) {
        List<Map<String, String>> historicDataList = jobData.get(type);
        for (Map<String, String> historicData: historicDataList) {
            ObjectNode historyJson = generateJson(commandContext, type, historicData);
            objectNodes.add(historyJson);
        }
        jobData.remove(type);
    }
    
    protected Map<String, List<Map<String, String>>> filterHistoricData(Map<String, List<Map<String, String>>> jobData) {
        if (jobData == null || jobData.isEmpty()) {
            return Collections.emptyMap();
        }
        matchCorrespondingActivityStartAndEndPairs(jobData);
        return jobData;
    }

    protected void matchCorrespondingActivityStartAndEndPairs(Map<String, List<Map<String, String>>> jobData) {

        // Try to match activity-start with corresponding activity-end and make them an activity-full

        List<Map<String, String>> activityFullDataList = null;
        if (jobData.containsKey(HistoryJsonConstants.TYPE_ACTIVITY_START)) {
            List<Map<String, String>> activityStartDataList = jobData.get(HistoryJsonConstants.TYPE_ACTIVITY_START);

            if (jobData.containsKey(HistoryJsonConstants.TYPE_ACTIVITY_END)) {
                Iterator<Map<String, String>> activityStartDataIterator = activityStartDataList.iterator();
                while (activityStartDataIterator.hasNext()) {

                    Map<String, String> activityStartData = activityStartDataIterator.next();
                    String activityStartKey = createActivityKey(activityStartData);

                    List<Map<String, String>> activityEndDataList = jobData.get(HistoryJsonConstants.TYPE_ACTIVITY_END);
                    Iterator<Map<String, String>> activityEndDataIterator = activityEndDataList.iterator();
                    while (activityEndDataIterator.hasNext()) {
                        
                        Map<String, String> activityEndData = activityEndDataIterator.next();
                        String activityEndKey = createActivityKey(activityEndData);
                        if (activityEndKey.equals(activityStartKey)) {
                            if (!jobData.containsKey(HistoryJsonConstants.TYPE_ACTIVITY_FULL)) {
                                jobData.put(HistoryJsonConstants.TYPE_ACTIVITY_FULL, new ArrayList<Map<String, String>>());
                            }
                            activityEndData.put(HistoryJsonConstants.START_TIME, activityStartData.get(HistoryJsonConstants.START_TIME));
                            
                            // Can't add and remove from same map in one loop ... 
                            if (activityFullDataList == null) {
                                activityFullDataList = new ArrayList<>();
                            }
                            activityFullDataList.add(activityEndData);
                            
                            activityStartDataIterator.remove();
                            activityEndDataIterator.remove();
                            
                        }
                        
                    }

                }
            }
        }
        
        if (activityFullDataList != null) {
            if (jobData.containsKey(HistoryJsonConstants.TYPE_ACTIVITY_FULL)) {
                jobData.put(HistoryJsonConstants.TYPE_ACTIVITY_FULL, new ArrayList<Map<String, String>>());
            }
            for (Map<String, String> activityFullData : activityFullDataList) {
                jobData.get(HistoryJsonConstants.TYPE_ACTIVITY_FULL).add(activityFullData);
            }
        }
    }

    protected String createActivityKey(Map<String, String> historicData) {
        return historicData.get(HistoryJsonConstants.EXECUTION_ID) + "_" + historicData.get(HistoryJsonConstants.ACTIVITY_ID);
    }
    
    protected ObjectNode generateJson(CommandContext commandContext, String type, Map<String, String> historicData) {
        ProcessEngineConfigurationImpl processEngineConfiguration = commandContext.getProcessEngineConfiguration();
        ObjectNode elementObjectNode = processEngineConfiguration.getObjectMapper().createObjectNode();
        elementObjectNode.put(HistoryJsonConstants.TYPE, type);

        ObjectNode dataNode = elementObjectNode.putObject(HistoryJsonConstants.DATA);
        for (String key : historicData.keySet()) {
            dataNode.put(key, historicData.get(key));
        }
        dataNode.put(HistoryJsonConstants.TIMESTAMP, AsyncHistoryDateUtil.formatDate(processEngineConfiguration.getClock().getCurrentTime()));
        
        return elementObjectNode;
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
    
    public AsyncHistorySession getAsyncHistorySession() {
        return asyncHistorySession;
    }

    public void setAsyncHistorySession(AsyncHistorySession asyncHistorySession) {
        this.asyncHistorySession = asyncHistorySession;
    }

    public AsyncHistoryListener getAsyncHistoryListener() {
        return asyncHistoryListener;
    }

    public void setAsyncHistoryListener(AsyncHistoryListener asyncHistoryListener) {
        this.asyncHistoryListener = asyncHistoryListener;
    }

}
