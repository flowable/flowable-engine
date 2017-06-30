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
package org.flowable.scripting.secure.behavior;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.ScriptTask;
import org.flowable.engine.impl.bpmn.parser.BpmnParse;
import org.flowable.engine.impl.bpmn.parser.handler.ScriptTaskParseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class SecureJavascriptTaskParseHandler extends ScriptTaskParseHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecureJavascriptTaskParseHandler.class);

    public static final String LANGUAGE_JAVASCRIPT = "javascript";

    @Override
    protected void executeParse(BpmnParse bpmnParse, ScriptTask scriptTask) {
        String language = scriptTask.getScriptFormat();
        if (LANGUAGE_JAVASCRIPT.equalsIgnoreCase(language)) {
            createSecureJavascriptTaskBehavior(bpmnParse, scriptTask, language);
        } else {
            super.executeParse(bpmnParse, scriptTask);
        }
    }

    protected void createSecureJavascriptTaskBehavior(BpmnParse bpmnParse, ScriptTask scriptTask, String language) {
        if (StringUtils.isEmpty(scriptTask.getScript())) {
            LOGGER.warn("No script provided for scriptTask {}", scriptTask.getId());
        }

        scriptTask.setBehavior(new SecureJavascriptTaskActivityBehavior(scriptTask.getId(),
                scriptTask.getScript(), language, scriptTask.getResultVariable(), scriptTask.isAutoStoreVariables()));
    }

}
