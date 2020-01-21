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
package org.activiti.engine.impl.bpmn.parser.handler;

import org.activiti.engine.impl.bpmn.parser.BpmnParse;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.Transaction;

/**
 * @author Joram Barrez
 */
public class TransactionParseHandler extends AbstractActivityBpmnParseHandler<Transaction> {

    @Override
    public Class<? extends BaseElement> getHandledType() {
        return Transaction.class;
    }

    @Override
    protected void executeParse(BpmnParse bpmnParse, Transaction transaction) {

        ActivityImpl activity = createActivityOnCurrentScope(bpmnParse, transaction, BpmnXMLConstants.ELEMENT_TRANSACTION);

        activity.setAsync(transaction.isAsynchronous());
        activity.setExclusive(!transaction.isNotExclusive());

        activity.setScope(true);
        activity.setActivityBehavior(bpmnParse.getActivityBehaviorFactory().createTransactionActivityBehavior(transaction));

        bpmnParse.setCurrentScope(activity);

        bpmnParse.processFlowElements(transaction.getFlowElements());
        processArtifacts(bpmnParse, transaction.getArtifacts(), activity);

        bpmnParse.removeCurrentScope();
    }

}
