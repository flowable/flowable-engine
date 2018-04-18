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
package org.flowable.engine.impl.history.async.json.transformer;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.history.async.AsyncHistoryDateUtil;
import org.flowable.engine.impl.history.async.HistoryJsonConstants;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntity;
import org.flowable.engine.impl.util.CommandContextUtil;

import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class AbstractHistoryJsonTransformer implements HistoryJsonTransformer {

    protected String getStringFromJson(ObjectNode objectNode, String fieldName) {
        if (objectNode.has(fieldName)) {
            return objectNode.get(fieldName).asText();
        }
        return null;
    }

    protected Date getDateFromJson(ObjectNode objectNode, String fieldName) {
        String s = getStringFromJson(objectNode, fieldName);
        return AsyncHistoryDateUtil.parseDate(s);
    }

    protected Integer getIntegerFromJson(ObjectNode objectNode, String fieldName) {
        String s = getStringFromJson(objectNode, fieldName);
        if (StringUtils.isNotEmpty(s)) {
            return Integer.valueOf(s);
        }
        return null;
    }
    
    protected Double getDoubleFromJson(ObjectNode objectNode, String fieldName) {
        String s = getStringFromJson(objectNode, fieldName);
        if (StringUtils.isNotEmpty(s)) {
            return Double.valueOf(s);
        }
        return null;
    }
    
    protected Long getLongFromJson(ObjectNode objectNode, String fieldName) {
        String s = getStringFromJson(objectNode, fieldName);
        if (StringUtils.isNotEmpty(s)) {
            return Long.valueOf(s);
        }
        return null;
    }
    
    protected Boolean getBooleanFromJson(ObjectNode objectNode, String fieldName, Boolean defaultValue) {
        Boolean value = getBooleanFromJson(objectNode, fieldName);
        return value != null ? value : defaultValue;
    }
    
    protected Boolean getBooleanFromJson(ObjectNode objectNode, String fieldName) {
        String s = getStringFromJson(objectNode, fieldName);
        if ((StringUtils.isNotEmpty(s))) {
            return Boolean.valueOf(s);
        }
        return null;
    }

    protected void dispatchEvent(CommandContext commandContext, FlowableEvent event) {
        FlowableEventDispatcher eventDispatcher = CommandContextUtil.getProcessEngineConfiguration(commandContext).getEventDispatcher();
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(event);
        }
    }

    public boolean historicActivityInstanceExistsForData(ObjectNode historicalData, CommandContext commandContext) {
        String executionId = getStringFromJson(historicalData, HistoryJsonConstants.EXECUTION_ID);
        if (StringUtils.isNotEmpty(executionId)) {
            String activityId = getStringFromJson(historicalData, HistoryJsonConstants.ACTIVITY_ID);
            
            if (StringUtils.isNotEmpty(activityId)) {
                HistoricActivityInstanceEntity historicActivityInstanceEntity = findUnfinishedHistoricActivityInstance(commandContext, executionId, activityId);
                return historicActivityInstanceEntity != null;
            }
        }
        return false;
    }
    
    public boolean historicActivityInstanceExistsForDataIncludingFinished(ObjectNode historicalData, CommandContext commandContext) {
        String executionId = getStringFromJson(historicalData, HistoryJsonConstants.EXECUTION_ID);
        if (StringUtils.isNotEmpty(executionId)) {
            String activityId = getStringFromJson(historicalData, HistoryJsonConstants.ACTIVITY_ID);
            
            if (StringUtils.isNotEmpty(activityId)) {
                HistoricActivityInstanceEntity historicActivityInstanceEntity = findHistoricActivityInstance(commandContext, executionId, activityId);
                return historicActivityInstanceEntity != null;
            }
        }
        return false;
    }

    protected HistoricActivityInstanceEntity findUnfinishedHistoricActivityInstance(CommandContext commandContext, String executionId, String activityId) {
        if (executionId == null || activityId == null) {
            return null;
        }

        HistoricActivityInstanceEntity historicActivityInstanceEntity = getUnfinishedHistoricActivityInstanceFromCache(commandContext, executionId, activityId);
        if (historicActivityInstanceEntity == null) {
            List<HistoricActivityInstanceEntity> historicActivityInstances = CommandContextUtil.getHistoricActivityInstanceEntityManager(commandContext)
                            .findUnfinishedHistoricActivityInstancesByExecutionAndActivityId(executionId, activityId);
            if (!historicActivityInstances.isEmpty()) {
                historicActivityInstanceEntity = historicActivityInstances.get(0);
            }
        }
        return historicActivityInstanceEntity;
    }

    protected HistoricActivityInstanceEntity getUnfinishedHistoricActivityInstanceFromCache(CommandContext commandContext,
                    String executionId, String activityId) {
        
        List<HistoricActivityInstanceEntity> cachedHistoricActivityInstances = CommandContextUtil.getEntityCache(commandContext).findInCache(HistoricActivityInstanceEntity.class);
        for (HistoricActivityInstanceEntity cachedHistoricActivityInstance : cachedHistoricActivityInstances) {
            if (activityId != null
                            && activityId.equals(cachedHistoricActivityInstance.getActivityId())
                            && cachedHistoricActivityInstance.getEndTime() == null
                            && executionId.equals(cachedHistoricActivityInstance.getExecutionId())) {
                
                return cachedHistoricActivityInstance;
            }
        }
        return null;
    }
    
    protected HistoricActivityInstanceEntity findHistoricActivityInstance(CommandContext commandContext, String executionId, String activityId) {
        if (executionId == null || activityId == null) {
            return null;
        }

        HistoricActivityInstanceEntity historicActivityInstanceEntity = getHistoricActivityInstanceFromCache(commandContext, executionId, activityId);
        if (historicActivityInstanceEntity == null) {
            List<HistoricActivityInstanceEntity> historicActivityInstances = CommandContextUtil.getHistoricActivityInstanceEntityManager(commandContext)
                            .findHistoricActivityInstancesByExecutionAndActivityId(executionId, activityId);
            if (!historicActivityInstances.isEmpty()) {
                historicActivityInstanceEntity = historicActivityInstances.get(0);
            }
        }
        return historicActivityInstanceEntity;
    }
    
    protected HistoricActivityInstanceEntity getHistoricActivityInstanceFromCache(CommandContext commandContext,
                    String executionId, String activityId) {
        
        List<HistoricActivityInstanceEntity> cachedHistoricActivityInstances = CommandContextUtil.getEntityCache(commandContext).findInCache(HistoricActivityInstanceEntity.class);
        for (HistoricActivityInstanceEntity cachedHistoricActivityInstance : cachedHistoricActivityInstances) {
            if (activityId != null
                            && activityId.equals(cachedHistoricActivityInstance.getActivityId())
                            && executionId.equals(cachedHistoricActivityInstance.getExecutionId())) {
                
                return cachedHistoricActivityInstance;
            }
        }
        return null;
    }

}
