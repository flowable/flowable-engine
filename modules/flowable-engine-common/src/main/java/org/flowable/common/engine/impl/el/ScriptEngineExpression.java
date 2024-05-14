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

package org.flowable.common.engine.impl.el;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.ScriptingEngineAwareEngineConfiguration;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.common.engine.impl.javax.el.ELContext;
import org.flowable.common.engine.impl.javax.el.MethodNotFoundException;
import org.flowable.common.engine.impl.javax.el.PropertyNotFoundException;
import org.flowable.common.engine.impl.javax.el.ValueExpression;
import org.flowable.common.engine.impl.scripting.ScriptEngineRequest;
import org.flowable.common.engine.impl.scripting.ScriptingEngines;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;

/**
 * Expression implementation backed by the {@link javax.script.ScriptEngineManager}.
 */
public class ScriptEngineExpression implements Expression {

	private static final long serialVersionUID = -6712195382739315692L;

	protected String expressionText;
	protected String expressionLanguage;

	public ScriptEngineExpression(String expressionText, String expressionLanguage) {
		this.expressionText = expressionText;
		this.expressionLanguage = expressionLanguage;
	}

	@Override
	public Object getValue(VariableContainer variableContainer) {
		ScriptEngineRequest.Builder builder = ScriptEngineRequest.builder()
				.script(expressionText)
				.language(expressionLanguage)
				.variableContainer(variableContainer);
		return getScriptingEngines().evaluate(builder.build()).getResult();
	}

	private ScriptingEngines getScriptingEngines() {
		CommandContext commandContext = Context.getCommandContext();
		if (commandContext == null) {
			throw new FlowableException("No CommandContext");
		}
		AbstractEngineConfiguration engineConfiguration = commandContext.getEngineConfigurations().get(EngineConfigurationConstants.KEY_PROCESS_ENGINE_CONFIG);
		if (!(engineConfiguration instanceof ScriptingEngineAwareEngineConfiguration scriptingEngineAware)) {
			throw new FlowableException("Process engine configuration is not scripting aware");
		}
		return scriptingEngineAware.getScriptingEngines();
	}

	@Override
	public void setValue(Object value, VariableContainer variableContainer) {
		throw new FlowableException("Cannot change value with " + expressionLanguage);
	}

	@Override
	public String toString() {
		return expressionText;
	}

	@Override
	public String getExpressionText() {
		return expressionText;
	}

}
