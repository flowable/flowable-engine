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

import org.flowable.cmmn.engine.impl.parser.CmmnParseResult;
import org.flowable.cmmn.engine.impl.parser.CmmnParserImpl;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.common.engine.api.FlowableException;

/**
 * @author Joram Barrez
 */
public abstract class AbstractPlanItemParseHandler<T extends BaseElement> extends AbstractCmmnParseHandler<T> {

    @Override
    protected void executeParse(CmmnParserImpl cmmnParser, CmmnParseResult cmmnParseResult, T element) {
        if (!(element instanceof PlanItem planItem)) {
            throw new FlowableException("Programmatic error: passed element is not a PlanItem instance, but an instance of " + element.getClass());
        }

        executePlanItemParse(cmmnParser, cmmnParseResult, planItem, (T) planItem.getPlanItemDefinition());
    }

    protected abstract void executePlanItemParse(CmmnParserImpl cmmnParser, CmmnParseResult cmmnParseResult, PlanItem planItem, T planItemDefinition);

}
