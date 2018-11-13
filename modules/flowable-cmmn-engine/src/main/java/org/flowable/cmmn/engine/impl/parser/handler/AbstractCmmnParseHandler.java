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

import org.flowable.cmmn.engine.impl.parser.CmmnParseHandler;
import org.flowable.cmmn.engine.impl.parser.CmmnParseResult;
import org.flowable.cmmn.engine.impl.parser.CmmnParser;
import org.flowable.cmmn.engine.impl.parser.CmmnParserImpl;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.PlanFragment;
import org.flowable.cmmn.model.PlanItem;

/**
 * @author Joram Barrez
 */
public abstract class AbstractCmmnParseHandler<T extends BaseElement> implements CmmnParseHandler {

    @Override
    public void parse(CmmnParser cmmnParser, CmmnParseResult cmmnParseResult, BaseElement element) {
        executeParse((CmmnParserImpl) cmmnParser, cmmnParseResult, (T) element);
    }

    protected abstract void executeParse(CmmnParserImpl cmmnParser, CmmnParseResult cmmnParseResult, T element);

    protected void processPlanFragment(CmmnParserImpl cmmnParser, CmmnParseResult cmmnParseResult, PlanFragment planFragment) {
        for (PlanItem planItem : planFragment.getPlanItems()) {
            cmmnParser.getCmmnParseHandlers().parseElement(cmmnParser, cmmnParseResult, planItem);
        }
    }

}
