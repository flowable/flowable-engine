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
import org.flowable.cmmn.model.GenericEventListener;
import org.flowable.cmmn.model.PlanItem;

/**
 * @author Tijs Rademakers
 */
public class GenericEventListenerParseHandler extends AbstractPlanItemParseHandler<GenericEventListener> {

    @Override
    public Collection<Class<? extends BaseElement>> getHandledTypes() {
        return Collections.singletonList(GenericEventListener.class);
    }

    @Override
    protected void executePlanItemParse(CmmnParserImpl cmmnParser, CmmnParseResult cmmnParseResult, PlanItem planItem, GenericEventListener genericEventListener) {
        if (StringUtils.isEmpty(genericEventListener.getEventType())) {
            planItem.setBehavior(cmmnParser.getActivityBehaviorFactory().createGenericEventListenerActivityBehavior(planItem, genericEventListener));
        } else {
            planItem.setBehavior(cmmnParser.getActivityBehaviorFactory().createEventRegistryEventListenerActivityBehaviour(planItem, genericEventListener));
        }
    }

}
