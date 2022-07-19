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

package org.flowable.engine.impl.bpmn.listener;

import org.flowable.common.engine.impl.scripting.AbstractScriptEvaluator;
import org.flowable.common.engine.impl.scripting.ScriptingEngines;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.task.service.delegate.DelegateTask;

/**
 * @author Rich Kroll
 * @author Joram Barrez
 */
public class ScriptTaskListener extends AbstractScriptEvaluator implements TaskListener {

    private static final long serialVersionUID = -8915149072830499057L;

    public ScriptTaskListener() {
        autoStoreVariables = true;
    }

    @Override
    protected ScriptingEngines getScriptingEngines() {
        return CommandContextUtil.getProcessEngineConfiguration().getScriptingEngines();
    }

    @Override
    public void notify(DelegateTask delegateTask) {
        validateParametersAndEvaluteScript(delegateTask);
    }
}
