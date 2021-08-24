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
package org.flowable.cmmn.engine.impl.parser.handler;

import java.util.Collection;
import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.engine.impl.parser.CmmnParseResult;
import org.flowable.cmmn.engine.impl.parser.CmmnParserImpl;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.ParentCompletionRule;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemControl;
import org.flowable.cmmn.model.ReactivateEventListener;

/**
 * The parse handler for the reactivation event listener, adding specific properties to the listener like to be ignored by the parent for completion as well
 * as an availability condition on the state of the case.
 *
 * @author Micha Kiener
 */
public class ReactivateEventListenerParseHandler extends AbstractPlanItemParseHandler<ReactivateEventListener> {

    @Override
    public Collection<Class<? extends BaseElement>> getHandledTypes() {
        return Collections.singletonList(ReactivateEventListener.class);
    }

    @Override
    protected void executePlanItemParse(CmmnParserImpl cmmnParser, CmmnParseResult cmmnParseResult, PlanItem planItem, ReactivateEventListener reactivateEventListener) {
        // the behavior is the same as with the user event listener
        planItem.setBehavior(cmmnParser.getActivityBehaviorFactory().createUserEventListenerActivityBehavior(planItem, reactivateEventListener));

        // if we are parsing a reactivation listener, we automatically set the parent completion rule to ignore as the listener does not have an impact on
        // parent completion at all as it is used when the case is completed to only mark the case eligible for reactivation
        ParentCompletionRule parentCompletionRule = new ParentCompletionRule();
        parentCompletionRule.setName("listenerIgnoredForCompletion");
        parentCompletionRule.setType(ParentCompletionRule.IGNORE);
        if (planItem.getItemControl() == null) {
            PlanItemControl planItemControl = new PlanItemControl();
            planItem.setItemControl(planItemControl);
        }
        planItem.getItemControl().setParentCompletionRule(parentCompletionRule);

        // check, if there is an available condition set on the listener and set it on the reactivation listener as the reactivate condition expression
        // explicitly as we need the default one to be a predefined one making the listener unavailable at runtime
        if (StringUtils.isNotEmpty(reactivateEventListener.getAvailableConditionExpression())) {
            reactivateEventListener.setReactivationAvailableConditionExpression(reactivateEventListener.getAvailableConditionExpression());
        }

        // additionally, we only set the listener to be available once the case is not active anymore (which in fact will make the listener unavailable at
        // all at runtime as it is used only in history to reactivate the case again)
        reactivateEventListener.setAvailableConditionExpression("${caseInstance.getState() != 'active'}");
    }
}
