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
package org.flowable.common.engine.impl.scripting;

import javax.script.Bindings;

/**
 * @author Filip Hrisafov
 */
public class ScriptEvaluationImpl implements ScriptEvaluation {

    protected final Bindings bindings;
    protected final Object result;

    public ScriptEvaluationImpl(Bindings bindings, Object result) {
        this.bindings = bindings;
        this.result = result;
    }

    @Override
    public Object getEvaluationAttribute(String key) {
        return bindings.get(key);
    }

    @Override
    public Object getResult() {
        return result;
    }
}
