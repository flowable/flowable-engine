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
import org.flowable.engine.delegate.event.FlowableConditionalEvent;

/**
 * An {@link FlowableConditionalEvent} implementation.
 */
public class FlowableConditionalEventImpl extends FlowableActivityEventImpl implements FlowableConditionalEvent {

    protected String conditionExpression;
    protected String conditionLanguage;

    public FlowableConditionalEventImpl(FlowableEngineEventType type) {
        super(type);
    }

    @Override
    public String getConditionExpression() {
        return conditionExpression;
    }

    public void setConditionExpression(String conditionExpression) {
        this.conditionExpression = conditionExpression;
    }

    @Override
    public String getConditionLanguage() {
        return conditionLanguage;
    }

    public void setConditionLanguage(String conditionLanguage) {
        this.conditionLanguage = conditionLanguage;
    }
}
