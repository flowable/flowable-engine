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
package org.flowable.engine.impl.bpmn.behavior;

import org.flowable.bpmn.model.Escalation;
import org.flowable.bpmn.model.EscalationEventDefinition;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.bpmn.helper.EscalationPropagation;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;

/**
 * @author Tijs Rademakers
 */
public class EscalationEndEventActivityBehavior extends FlowNodeActivityBehavior {

    private static final long serialVersionUID = 1L;

    protected EscalationEventDefinition escalationEventDefinition;
    protected Escalation escalation;

    public EscalationEndEventActivityBehavior(EscalationEventDefinition escalationEventDefinition, Escalation escalation) {
        this.escalationEventDefinition = escalationEventDefinition;
        this.escalation = escalation;
    }

    @Override
    public void execute(DelegateExecution execution) {
        if (escalation != null) {
            EscalationPropagation.propagateEscalation(escalation, execution);
        } else {
            EscalationPropagation.propagateEscalation(escalationEventDefinition.getEscalationCode(), 
                            escalationEventDefinition.getEscalationCode(), execution);
        }
        
        CommandContextUtil.getAgenda().planTakeOutgoingSequenceFlowsOperation((ExecutionEntity) execution, true);
    }

    public EscalationEventDefinition getEscalationEventDefinition() {
        return escalationEventDefinition;
    }

    public void setEscalationEventDefinition(EscalationEventDefinition escalationEventDefinition) {
        this.escalationEventDefinition = escalationEventDefinition;
    }

    public Escalation getEscalation() {
        return escalation;
    }

    public void setEscalation(Escalation escalation) {
        this.escalation = escalation;
    }

}
