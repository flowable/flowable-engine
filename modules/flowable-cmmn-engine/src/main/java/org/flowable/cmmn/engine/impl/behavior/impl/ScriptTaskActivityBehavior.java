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
package org.flowable.cmmn.engine.impl.behavior.impl;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.ScriptServiceTask;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.scripting.ScriptingEngines;

/**
 * Implementation of ScriptActivity CMMN 2.0 PlanItem.
 */
public class ScriptTaskActivityBehavior extends TaskActivityBehavior {

    protected ScriptServiceTask scriptTask;

    public ScriptTaskActivityBehavior(ScriptServiceTask scriptTask) {
        super(scriptTask.isBlocking(), scriptTask.getBlockingExpression());
        this.scriptTask = scriptTask;
    }

    @Override
    public void execute(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        ScriptingEngines scriptingEngines = CommandContextUtil.getCmmnEngineConfiguration().getScriptingEngines();
        if (scriptingEngines == null) {
            throw new FlowableException("Could not execute script task instance: no scripting engines found.");
        }
        String scriptFormat = scriptTask.getScriptFormat() != null ? scriptTask.getScriptFormat() : ScriptingEngines.DEFAULT_SCRIPTING_LANGUAGE;
        Object result = scriptingEngines.evaluate(scriptTask.getScript(), scriptFormat, planItemInstanceEntity, scriptTask.isAutoStoreVariables());
        String resultVariableName = scriptTask.getResultVariableName();
        if (StringUtils.isNotBlank(scriptTask.getResultVariableName())) {
            planItemInstanceEntity.setVariable(resultVariableName.trim(), result);
        }
        CommandContextUtil.getAgenda(commandContext).planCompletePlanItemInstanceOperation(planItemInstanceEntity);
    }
}
