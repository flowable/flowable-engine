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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.variable.VariableContainer;

/**
 * Manages and provides access to JSR-223 {@link ScriptEngine ScriptEngines}.
 *
 * <p>
 * ScriptEngines are attempted to be cached by default, if the ScriptEngines
 * factory {@link ScriptEngineFactory#getParameter(String) THREADING parameter}
 * indicates thread safe read access.
 * </p>
 *
 * @see ScriptEngineManager
 */
public class JSR223FlowableScriptEngine implements FlowableScriptEngine {

    private final ScriptEngineManager scriptEngineManager;

    protected boolean cacheScriptingEngines = true;
    protected Map<String, ScriptEngine> cachedEngines;

    public JSR223FlowableScriptEngine() {
        this(new ScriptEngineManager());
    }

    public JSR223FlowableScriptEngine(ScriptEngineManager scriptEngineManager) {
        this.scriptEngineManager = scriptEngineManager;
        cachedEngines = new HashMap<>();
    }

    @Override
    public FlowableScriptEvaluationRequest createEvaluationRequest() {
        return new JavaScriptingFlowableScriptEvaluationRequest();
    }

    protected ScriptEngine getEngineByName(String language) {
        ScriptEngine scriptEngine = null;

        if (cacheScriptingEngines) {
            scriptEngine = cachedEngines.get(language);
            if (scriptEngine == null) {
                synchronized (scriptEngineManager) {
                    // Get the cached engine again in case a different thread already created it
                    scriptEngine = cachedEngines.get(language);

                    if (scriptEngine != null) {
                        return scriptEngine;
                    }

                    scriptEngine = scriptEngineManager.getEngineByName(language);

                    if (scriptEngine != null) {
                        if (ScriptingEngines.GROOVY_SCRIPTING_LANGUAGE.equals(language)) {
                            try {
                                scriptEngine.getContext().setAttribute("#jsr223.groovy.engine.keep.globals", "weak", ScriptContext.ENGINE_SCOPE);
                            } catch (Exception ignore) {
                                // ignore this, in case engine doesn't support the
                                // passed attribute
                            }
                        }

                        // Check if script-engine allows caching, using "THREADING"
                        // parameter as defined in spec
                        Object threadingParameter = scriptEngine.getFactory().getParameter("THREADING");
                        if (threadingParameter != null) {
                            // Add engine to cache as any non-null result from the
                            // threading-parameter indicates at least MT-access
                            cachedEngines.put(language, scriptEngine);
                        }
                    }
                }
            }
        } else {
            scriptEngine = scriptEngineManager.getEngineByName(language);
        }

        if (scriptEngine == null) {
            throw new FlowableException("Can't find scripting engine for '" + language + "'");
        }
        return scriptEngine;
    }

    public void setScriptEngineFactories(List<ScriptEngineFactory> scriptEngineFactories) {
        if (scriptEngineFactories != null) {
            for (ScriptEngineFactory scriptEngineFactory : scriptEngineFactories) {
                scriptEngineManager.registerEngineName(scriptEngineFactory.getEngineName(), scriptEngineFactory);
            }
        }
    }

    public void addScriptEngineFactory(ScriptEngineFactory scriptEngineFactory) {
        scriptEngineManager.registerEngineName(scriptEngineFactory.getEngineName(), scriptEngineFactory);
    }

    public void setCacheScriptingEngines(boolean cacheScriptingEngines) {
        this.cacheScriptingEngines = cacheScriptingEngines;
    }

    public boolean isCacheScriptingEngines() {
        return cacheScriptingEngines;
    }

    public ScriptEngineManager getScriptEngineManager() {
        return scriptEngineManager;
    }


    protected class JavaScriptingFlowableScriptEvaluationRequest implements FlowableScriptEvaluationRequest {

        protected String language;
        protected String script;
        protected Resolver resolver;
        protected VariableContainer scopeContainer;
        protected VariableContainer inputVariableContainer;
        protected boolean storeScriptVariables;

        @Override
        public FlowableScriptEvaluationRequest language(String language) {
            if (StringUtils.isEmpty(language)) {
                throw new FlowableIllegalArgumentException("language is empty");
            }
            this.language = language;
            return this;
        }

        @Override
        public FlowableScriptEvaluationRequest script(String script) {
            if (StringUtils.isEmpty(script)) {
                throw new FlowableIllegalArgumentException("script is empty");
            }
            this.script = script;
            return this;
        }

        @Override
        public FlowableScriptEvaluationRequest resolver(Resolver resolver) {
            this.resolver = resolver;
            return this;
        }

        @Override
        public FlowableScriptEvaluationRequest scopeContainer(VariableContainer scopeContainer) {
            this.scopeContainer = scopeContainer;
            return this;
        }

        @Override
        public FlowableScriptEvaluationRequest inputVariableContainer(VariableContainer inputVariableContainer) {
            this.inputVariableContainer = inputVariableContainer;
            return this;
        }

        @Override
        public FlowableScriptEvaluationRequest storeScriptVariables() {
            this.storeScriptVariables = true;
            return this;
        }

        @Override
        public ScriptEvaluation evaluate() throws FlowableScriptException {
            if (StringUtils.isEmpty(language)) {
                throw new FlowableIllegalArgumentException("language is required");
            }

            if (StringUtils.isEmpty(script)) {
                throw new FlowableIllegalArgumentException("script is required");
            }

            ScriptEngine scriptEngine = getEngineByName(language);
            Bindings bindings = createBindings();
            try {
                Object result = scriptEngine.eval(script, bindings);
                return new ScriptEvaluationImpl(resolver, result);
            } catch (ScriptException e) {
                throw new FlowableScriptException(e.getMessage(), e);
            }
        }

        protected Bindings createBindings() {
            return new ScriptBindings(Collections.singletonList(resolver), scopeContainer, inputVariableContainer, storeScriptVariables);
        }
    }
}
