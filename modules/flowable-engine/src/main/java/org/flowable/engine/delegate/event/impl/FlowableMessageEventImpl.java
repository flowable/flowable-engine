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
package org.flowable.engine.delegate.event.impl;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.engine.delegate.event.FlowableMessageEvent;
import org.flowable.engine.delegate.event.FlowableSignalEvent;

/**
 * An {@link FlowableSignalEvent} implementation.
 * 
 * @author Frederik Heremans
 */
public class FlowableMessageEventImpl extends FlowableActivityEventImpl implements FlowableMessageEvent {

    protected String messageName;
    protected Object messageData;

    public FlowableMessageEventImpl(FlowableEngineEventType type) {
        super(type);
    }

    public void setMessageName(String messageName) {
        this.messageName = messageName;
    }

    @Override
    public String getMessageName() {
        return messageName;
    }

    public void setMessageData(Object messageData) {
        this.messageData = messageData;
    }

    @Override
    public Object getMessageData() {
        return messageData;
    }
}
