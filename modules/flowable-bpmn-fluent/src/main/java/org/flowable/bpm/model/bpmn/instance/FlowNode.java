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
package org.flowable.bpm.model.bpmn.instance;

import org.flowable.bpm.model.bpmn.Query;
import org.flowable.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;

import java.util.Collection;

/**
 * The BPMN flowNode element.
 */
public interface FlowNode
        extends FlowElement {

    @SuppressWarnings("rawtypes")
    AbstractFlowNodeBuilder builder();

    Collection<SequenceFlow> getIncoming();

    Collection<SequenceFlow> getOutgoing();

    Query<FlowNode> getPreviousNodes();

    Query<FlowNode> getSucceedingNodes();

    boolean isFlowableAsync();

    void setFlowableAsync(boolean isFlowableAsync);

    boolean isFlowableExclusive();

    void setFlowableExclusive(boolean isFlowableExclusive);
}
