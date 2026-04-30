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
import java.util.List;
import java.util.Objects;

import org.flowable.cmmn.converter.CmmnXmlConstants;
import org.flowable.cmmn.engine.impl.deployer.EventRegistryCaseDefinitionStartLifecycleHandler;
import org.flowable.cmmn.engine.impl.parser.CmmnParseResult;
import org.flowable.cmmn.engine.impl.parser.CmmnParserImpl;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.ExtensionElement;

/**
 * Installs the built-in event-registry case-start lifecycle handler on a {@link Case} when its
 * {@code startEventType} is set. Resolves the manual-correlation flag from the case's extension
 * elements at parse time so the handler carries it as a constructor arg.
 * <p>
 * Custom integrations can install additional {@code CaseDefinitionStartLifecycleHandler}s on the
 * same case via their own parse handlers registered through {@code customCmmnParseHandlers}.
 */
public class EventRegistryCaseStartLifecycleParseHandler extends AbstractCmmnParseHandler<Case> {

    @Override
    public Collection<Class<? extends BaseElement>> getHandledTypes() {
        return Collections.singletonList(Case.class);
    }

    @Override
    protected void executeParse(CmmnParserImpl cmmnParser, CmmnParseResult cmmnParseResult, Case caze) {
        String startEventType = caze.getStartEventType();
        if (startEventType == null) {
            return;
        }
        boolean manualCorrelation = isManualCorrelation(caze);
        caze.addStartLifecycleHandler(new EventRegistryCaseDefinitionStartLifecycleHandler(startEventType, manualCorrelation));
    }

    protected boolean isManualCorrelation(Case caze) {
        List<ExtensionElement> correlationConfig = caze.getExtensionElements()
                .getOrDefault(CmmnXmlConstants.START_EVENT_CORRELATION_CONFIGURATION, Collections.emptyList());
        return !correlationConfig.isEmpty()
                && Objects.equals(correlationConfig.get(0).getElementText(), CmmnXmlConstants.START_EVENT_CORRELATION_MANUAL);
    }
}
