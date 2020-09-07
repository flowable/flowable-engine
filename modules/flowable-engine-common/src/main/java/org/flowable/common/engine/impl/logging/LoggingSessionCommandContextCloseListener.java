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
package org.flowable.common.engine.impl.logging;

import java.util.List;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandContextCloseListener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class LoggingSessionCommandContextCloseListener implements CommandContextCloseListener {
    
    protected LoggingSession loggingSession;
    protected LoggingListener loggingListener;
    protected ObjectMapper objectMapper;
    
    protected String engineType;
    
    public LoggingSessionCommandContextCloseListener() {
        
    }
    
    public LoggingSessionCommandContextCloseListener(LoggingSession loggingSession, LoggingListener loggingListener, ObjectMapper objectMapper) {
        this.loggingSession = loggingSession;
        this.loggingListener = loggingListener;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public void closing(CommandContext commandContext) {
        // nothing to do
    }
    
    @Override
    public void closed(CommandContext commandContext) {
        LoggingSessionUtil.addEngineLoggingData(LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE, 
                "Closed command context for " + engineType + " engine", engineType, objectMapper);
        List<ObjectNode> loggingData = loggingSession.getLoggingData();
        loggingListener.loggingGenerated(loggingData);
    }

    @Override
    public void closeFailure(CommandContext commandContext) {
        LoggingSessionUtil.addEngineLoggingData(LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE_FAILURE, 
                "Exception at closing command context for " + engineType + " engine", engineType, objectMapper);
        List<ObjectNode> loggingData = loggingSession.getLoggingData();
        loggingListener.loggingGenerated(loggingData);
    }

    @Override
    public void afterSessionsFlush(CommandContext commandContext) {
        // nothing to do
    }
    
    @Override
    public Integer order() {
        return 500;
    }
    
    @Override
    public boolean multipleAllowed() {
        return false;
    }

    public LoggingSession getLoggingSession() {
        return loggingSession;
    }

    public void setLoggingSession(LoggingSession loggingSession) {
        this.loggingSession = loggingSession;
    }

    public LoggingListener getLoggingListener() {
        return loggingListener;
    }

    public void setLoggingListener(LoggingListener loggingListener) {
        this.loggingListener = loggingListener;
    }

    public String getEngineType() {
        return engineType;
    }

    public void setEngineType(String engineType) {
        this.engineType = engineType;
    }
}
