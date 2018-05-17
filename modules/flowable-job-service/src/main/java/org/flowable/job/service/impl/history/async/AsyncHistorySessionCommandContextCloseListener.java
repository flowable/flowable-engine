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
import java.util.Map;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandContextCloseListener;
import org.flowable.job.service.impl.history.async.transformer.HistoryJsonTransformer;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;
import org.flowable.job.service.impl.util.CommandContextUtil;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A listener for command context lifecycle close events that generates JSON
 * (using Jackson) and corresponding {@link HistoryJobEntity} when the
 * {@link CommandContext} closes and adds them to the list of entities that will
 * be inserted to the database. 
 * 
 * The reason why this is done at the very end, is because that way the historical data 
 * can be optimized (some events cancel others, can be grouped, etc.)
 * 
 * @author Joram Barrez
 */
public class AsyncHistorySessionCommandContextCloseListener implements CommandContextCloseListener {
    
    protected AsyncHistorySession asyncHistorySession;
    protected AsyncHistoryListener asyncHistoryListener;
    
    // The field name under which the type and actual will be stored
    protected String typeFieldName = HistoryJsonTransformer.FIELD_NAME_TYPE;
    protected String dataFieldName = HistoryJsonTransformer.FIELD_NAME_DATA;
    
    public AsyncHistorySessionCommandContextCloseListener() {
        
    }
    
    public AsyncHistorySessionCommandContextCloseListener(AsyncHistorySession asyncHistorySession, AsyncHistoryListener asyncHistoryListener) {
        this.asyncHistorySession = asyncHistorySession;
        this.asyncHistoryListener = asyncHistoryListener;
    }
    
    @Override
    public void closing(CommandContext commandContext) {
        
        // This logic needs to be done before the dbSqlSession is flushed 
        // which means it can't be done in the transaction pre-commit
        
        Map<String, List<Map<String, String>>> jobData = asyncHistorySession.getJobData();
        if (!jobData.isEmpty()) {
            List<ObjectNode> objectNodes = new ArrayList<>();
            
            // First, the registered types
            for (String type : asyncHistorySession.getJobDataTypes()) {
                if (jobData.containsKey(type)) {
                    generateJson(commandContext, jobData, objectNodes, type);
                    jobData.remove(type);
                }
            }
            
            // Additional data for which the type is not registered
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
    }
    
    protected ObjectNode generateJson(CommandContext commandContext, String type, Map<String, String> historicData) {
        ObjectNode elementObjectNode = CommandContextUtil.getJobServiceConfiguration(commandContext).getObjectMapper().createObjectNode();
        elementObjectNode.put(typeFieldName, type);

        ObjectNode dataNode = elementObjectNode.putObject(dataFieldName);
        for (String key : historicData.keySet()) {
            dataNode.put(key, historicData.get(key));
        }
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

    public String getTypeFieldName() {
        return typeFieldName;
    }

    public void setTypeFieldName(String typeFieldName) {
        this.typeFieldName = typeFieldName;
    }

    public String getDataFieldName() {
        return dataFieldName;
    }

    public void setDataFieldName(String dataFieldName) {
        this.dataFieldName = dataFieldName;
    }

}
