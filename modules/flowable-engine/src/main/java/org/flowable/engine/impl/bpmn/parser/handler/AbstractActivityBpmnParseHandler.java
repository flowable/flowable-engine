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
package org.flowable.engine.impl.bpmn.parser.handler;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.MultiInstanceLoopCharacteristics;
import org.flowable.engine.impl.bpmn.behavior.AbstractBpmnActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.MultiInstanceActivityBehavior;
import org.flowable.engine.impl.bpmn.parser.BpmnParse;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.variable.service.impl.el.ExpressionManager;

/**
 * @author Joram Barrez
 */
public abstract class AbstractActivityBpmnParseHandler<T extends FlowNode> extends AbstractFlowNodeBpmnParseHandler<T> {

    @Override
    public void parse(BpmnParse bpmnParse, BaseElement element) {
        super.parse(bpmnParse, element);

        if (element instanceof Activity && ((Activity) element).getLoopCharacteristics() != null) {
            createMultiInstanceLoopCharacteristics(bpmnParse, (Activity) element);
        }
    }

    protected void createMultiInstanceLoopCharacteristics(BpmnParse bpmnParse, Activity modelActivity) {

        MultiInstanceLoopCharacteristics loopCharacteristics = modelActivity.getLoopCharacteristics();

        // Activity Behavior
        MultiInstanceActivityBehavior miActivityBehavior = null;

        if (loopCharacteristics.isSequential()) {
            miActivityBehavior = bpmnParse.getActivityBehaviorFactory().createSequentialMultiInstanceBehavior(modelActivity, (AbstractBpmnActivityBehavior) modelActivity.getBehavior());
        } else {
            miActivityBehavior = bpmnParse.getActivityBehaviorFactory().createParallelMultiInstanceBehavior(modelActivity, (AbstractBpmnActivityBehavior) modelActivity.getBehavior());
        }

        modelActivity.setBehavior(miActivityBehavior);

        ExpressionManager expressionManager = CommandContextUtil.getProcessEngineConfiguration().getExpressionManager();

        // loop cardinality
        if (StringUtils.isNotEmpty(loopCharacteristics.getLoopCardinality())) {
            miActivityBehavior.setLoopCardinalityExpression(expressionManager.createExpression(loopCharacteristics.getLoopCardinality()));
        }

        // completion condition
        if (StringUtils.isNotEmpty(loopCharacteristics.getCompletionCondition())) {
            miActivityBehavior.setCompletionCondition(loopCharacteristics.getCompletionCondition());
        }

        // flowable:collection
        if (StringUtils.isNotEmpty(loopCharacteristics.getInputDataItem())) {
            miActivityBehavior.setCollectionExpression(expressionManager.createExpression(loopCharacteristics.getInputDataItem()));
        }

        // flowable:collectionString
        if (StringUtils.isNotEmpty(loopCharacteristics.getCollectionString())) {
            miActivityBehavior.setCollectionString(loopCharacteristics.getCollectionString());
        }

        // flowable:elementVariable
        if (StringUtils.isNotEmpty(loopCharacteristics.getElementVariable())) {
            miActivityBehavior.setCollectionElementVariable(loopCharacteristics.getElementVariable());
        }

        // flowable:elementIndexVariable
        if (StringUtils.isNotEmpty(loopCharacteristics.getElementIndexVariable())) {
            miActivityBehavior.setCollectionElementIndexVariable(loopCharacteristics.getElementIndexVariable());
        }

        // flowable:collectionParser
        if (loopCharacteristics.getHandler() != null) {
            miActivityBehavior.setHandler(loopCharacteristics.getHandler().clone());
        }
    }
}
