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
package org.flowable.cmmn.engine.impl.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.PlanItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class CmmnParseHandlers {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmmnParseHandlers.class);

    protected Map<Class<? extends BaseElement>, List<CmmnParseHandler>> parseHandlers = new HashMap<>();

    public CmmnParseHandlers() {
    }

    public CmmnParseHandlers(List<CmmnParseHandler> cmmnParseHandlers) {
        cmmnParseHandlers.forEach(this::addHandler);
    }

    public void addHandlers(List<CmmnParseHandler> cmmnParseHandlers) {
        cmmnParseHandlers.forEach(this::addHandler);
    }

    public void addHandler(CmmnParseHandler cmmnParseHandler) {
        for (Class<? extends BaseElement> type : cmmnParseHandler.getHandledTypes()) {
            parseHandlers
                .computeIfAbsent(type, key -> new ArrayList<>())
                .add(cmmnParseHandler);
        }
    }

    public void parseElement(CmmnParser cmmnParser, CmmnParseResult cmmnParseResult, BaseElement baseElement) {

        List<CmmnParseHandler> handlers = null;
        if (baseElement instanceof PlanItem planItem) {
            // The plan item definition defines the actual behavior
            handlers = parseHandlers.get(planItem.getPlanItemDefinition().getClass());

            if (handlers == null) {
                LOGGER.warn("Could not find matching parse handler for planItem '{}' with planItemDefinition '{}'. This is likely a bug.",
                    baseElement.getId(), planItem.getPlanItemDefinition());

            } else {
                handlers.forEach(handler -> handler.parse(cmmnParser, cmmnParseResult, planItem)); // Note: passing plan item, NOT plan item definition

            }

        } else {
            handlers = parseHandlers.get(baseElement.getClass());
            if (handlers == null) {
                LOGGER.warn("Could not find matching parse handler for '{}' this is likely a bug.", baseElement.getId());

            } else {
                handlers.forEach(handler -> handler.parse(cmmnParser, cmmnParseResult, baseElement));

            }

        }
    }

}
