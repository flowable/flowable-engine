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

import java.util.ArrayList;
import java.util.List;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.Session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class LoggingSession implements Session {
    
    protected CommandContext commandContext;
    protected LoggingSessionCommandContextCloseListener commandContextCloseListener;
    
    protected LoggingListener loggingListener;
    protected ObjectMapper objectMapper;
    
    protected List<ObjectNode> loggingData;

    public LoggingSession(CommandContext commandContext, LoggingListener loggingListener, ObjectMapper objectMapper) {
        this.commandContext = commandContext;
        this.loggingListener = loggingListener;
        this.objectMapper = objectMapper;
        
        initCommandContextCloseListener();
    }

    protected void initCommandContextCloseListener() {
        this.commandContextCloseListener = new LoggingSessionCommandContextCloseListener(this, loggingListener, objectMapper); 
    }
    
    public void addLoggingData(String type, ObjectNode data, String engineType) {
        if (loggingData == null) {
            loggingData = new ArrayList<>();
            commandContextCloseListener.setEngineType(engineType);
            commandContext.addCloseListener(commandContextCloseListener);
        }
        
        String transactionId = null;
        if (loggingData.size() > 0) {
            transactionId = loggingData.get(0).get(LoggingSessionUtil.TRANSACTION_ID).asText();
        } else {
            transactionId = data.get(LoggingSessionUtil.ID).asText();
        }
        
        data.put(LoggingSessionUtil.TRANSACTION_ID, transactionId);
        data.put(LoggingSessionUtil.LOG_NUMBER, loggingData.size() + 1);
        loggingData.add(data);
    }
    
    @Override
    public void flush() {

    }

    @Override
    public void close() {

    }

    public List<ObjectNode> getLoggingData() {
        return loggingData;
    }

    public void setLoggingData(List<ObjectNode> loggingData) {
        this.loggingData = loggingData;
    }
}
