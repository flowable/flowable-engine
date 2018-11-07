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
import org.flowable.engine.delegate.event.FlowableSequenceFlowTakenEvent;

/**
 * @author Joram Barrez
 */
public class FlowableSequenceFlowTakenEventImpl extends FlowableProcessEventImpl implements FlowableSequenceFlowTakenEvent {

    protected String id;
    protected String sourceActivityId;
    protected String sourceActivityName;
    protected String sourceActivityType;
    protected String targetActivityId;
    protected String targetActivityName;
    protected String targetActivityType;
    protected String sourceActivityBehaviorClass;
    protected String targetActivityBehaviorClass;

    public FlowableSequenceFlowTakenEventImpl(FlowableEngineEventType type) {
        super(type);
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getSourceActivityId() {
        return sourceActivityId;
    }

    public void setSourceActivityId(String sourceActivityId) {
        this.sourceActivityId = sourceActivityId;
    }

    @Override
    public String getSourceActivityName() {
        return sourceActivityName;
    }

    public void setSourceActivityName(String sourceActivityName) {
        this.sourceActivityName = sourceActivityName;
    }

    @Override
    public String getSourceActivityType() {
        return sourceActivityType;
    }

    public void setSourceActivityType(String sourceActivityType) {
        this.sourceActivityType = sourceActivityType;
    }

    @Override
    public String getTargetActivityId() {
        return targetActivityId;
    }

    public void setTargetActivityId(String targetActivityId) {
        this.targetActivityId = targetActivityId;
    }

    @Override
    public String getTargetActivityName() {
        return targetActivityName;
    }

    public void setTargetActivityName(String targetActivityName) {
        this.targetActivityName = targetActivityName;
    }

    @Override
    public String getTargetActivityType() {
        return targetActivityType;
    }

    public void setTargetActivityType(String targetActivityType) {
        this.targetActivityType = targetActivityType;
    }

    @Override
    public String getSourceActivityBehaviorClass() {
        return sourceActivityBehaviorClass;
    }

    public void setSourceActivityBehaviorClass(String sourceActivityBehaviorClass) {
        this.sourceActivityBehaviorClass = sourceActivityBehaviorClass;
    }

    @Override
    public String getTargetActivityBehaviorClass() {
        return targetActivityBehaviorClass;
    }

    public void setTargetActivityBehaviorClass(String targetActivityBehaviorClass) {
        this.targetActivityBehaviorClass = targetActivityBehaviorClass;
    }

}
