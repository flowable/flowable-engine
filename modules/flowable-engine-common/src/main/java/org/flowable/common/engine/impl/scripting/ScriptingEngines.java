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

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages and provides access to JSR-223 {@link ScriptEngine ScriptEngines}.
 *
 * <p>
 * ScriptEngines are attempted to be cached by default, if the ScriptEngines
 * factory {@link ScriptEngineFactory#getParameter(String) THREADING parameter}
 * indicates thread safe read access.
 * </p>
 *
 * @author Tom Baeyens
 * @author Joram Barrez
 * @author Frederik Heremans
 * @author Arthur Hupka-Merle
 * @see ScriptEngineManager
 */
public class ScriptingEngines {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptingEngines.class);

    public static final String DEFAULT_SCRIPTING_LANGUAGE = "juel";
    public static final String GROOVY_SCRIPTING_LANGUAGE = "groovy";

    private final ScriptEngineManager scriptEngineManager;
    protected ScriptBindingsFactory scriptBindingsFactory;

    protected boolean cacheScriptingEngines = true;
    protected Map<String, ScriptEngine> cachedEngines;

    protected ScriptTraceEnhancer defaultTraceEnhancer;

    protected ScriptTraceListener scriptErrorListener = null;
    protected ScriptTraceListener scriptSuccessListener = null;

    public ScriptingEngines(ScriptBindingsFactory scriptBindingsFactory) {
        this(new ScriptEngineManager());
        this.scriptBindingsFactory = scriptBindingsFactory;
    }

    public ScriptingEngines(ScriptEngineManager scriptEngineManager) {
        this.scriptEngineManager = scriptEngineManager;
        cachedEngines = new HashMap<>();
    }

    public ScriptEvaluation evaluate(ScriptEngineRequest request) {
        Bindings bindings = createBindings(request);
        Object result = evaluate(request, bindings);
        return new ScriptEvaluationImpl(bindings, result);
    }

    /**
     * @deprecated since 6.8.0 use {@link #evaluate(ScriptEngineRequest)}.getResult()
     */
    @Deprecated
    public Object evaluate(String script, String language, VariableContainer variableContainer) {
        return evaluate(script, language, variableContainer, false);
    }

    /**
     * @deprecated since 6.8.0 use {@link #evaluate(ScriptEngineRequest)}.getResult()
     */
    @Deprecated
    public Object evaluate(String script, String language, VariableContainer variableContainer, boolean storeScriptVariables) {
        ScriptEngineRequest.Builder builder = ScriptEngineRequest.builder()
                .script(script)
                .language(language)
                .variableContainer(variableContainer);
        builder = storeScriptVariables ? builder.storeScriptVariables() : builder;
        return evaluate(builder.build()).getResult();
    }

    protected Object evaluate(ScriptEngineRequest request, Bindings bindings) {
        ScriptEngine scriptEngine = getEngineByName(request.getLanguage());
        return evaluate(scriptEngine, request, bindings);
    }

    protected Object evaluate(ScriptEngine scriptEngine, ScriptEngineRequest request, Bindings bindings) {
        long startNanos = System.nanoTime();
        try {
            Object scriptResult = scriptEngine.eval(request.getScript(), bindings);
            if (scriptSuccessListener != null) {
                DefaultScriptTrace scriptTrace = DefaultScriptTrace.successTrace(Duration.ofNanos(System.nanoTime() - startNanos), request);
                enhanceScriptTrace(request, scriptTrace);
                notifyScriptTraceListener(scriptSuccessListener, scriptTrace);
            }
            return scriptResult;
        } catch (ScriptException e) {
            DefaultScriptTrace scriptTrace = DefaultScriptTrace.errorTrace(Duration.ofNanos(System.nanoTime() - startNanos), request, e);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Caught exception evaluating script. {}{}{}", request.getLanguage(), System.lineSeparator(),
                        request.getScript());
            }
            enhanceScriptTrace(request, scriptTrace);
            if (scriptErrorListener != null) {
                notifyScriptTraceListener(scriptErrorListener, scriptTrace);
            }
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            if (rootCause instanceof FlowableException) {
                throw (FlowableException) rootCause;
            }
            throw new FlowableScriptEvaluationException(scriptTrace, e);
        }
    }

    protected void notifyScriptTraceListener(ScriptTraceListener listener, ScriptTrace scriptTrace) {
        try {
            listener.onScriptTrace(scriptTrace);
        } catch (Exception e) {
            LOGGER.warn("Exception while executing scriptTraceListener: {}", e.getMessage(), e);
        }
    }

    protected void enhanceScriptTrace(ScriptEngineRequest request, DefaultScriptTrace scriptTrace) {
        if (defaultTraceEnhancer != null) {
            defaultTraceEnhancer.enhanceScriptTrace(scriptTrace);
        }
        if (request.getTraceEnhancer() != null) {
            request.getTraceEnhancer().enhanceScriptTrace(scriptTrace);
        }
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
                        // ACT-1858: Special handling for groovy engine regarding GC
                        if (GROOVY_SCRIPTING_LANGUAGE.equals(language)) {
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

    /**
     * override to build a spring aware ScriptingEngines
     */
    protected Bindings createBindings(ScriptEngineRequest request) {
        return scriptBindingsFactory.createBindings(request);
    }

    public ScriptBindingsFactory getScriptBindingsFactory() {
        return scriptBindingsFactory;
    }

    public void setScriptBindingsFactory(ScriptBindingsFactory scriptBindingsFactory) {
        this.scriptBindingsFactory = scriptBindingsFactory;
    }

    public void setScriptEngineFactories(List<ScriptEngineFactory> scriptEngineFactories) {
        if (scriptEngineFactories != null) {
            for (ScriptEngineFactory scriptEngineFactory : scriptEngineFactories) {
                scriptEngineManager.registerEngineName(scriptEngineFactory.getEngineName(), scriptEngineFactory);
            }
        }
    }

    public ScriptingEngines addScriptEngineFactory(ScriptEngineFactory scriptEngineFactory) {
        scriptEngineManager.registerEngineName(scriptEngineFactory.getEngineName(), scriptEngineFactory);
        return this;
    }

    public void setCacheScriptingEngines(boolean cacheScriptingEngines) {
        this.cacheScriptingEngines = cacheScriptingEngines;
    }

    public boolean isCacheScriptingEngines() {
        return cacheScriptingEngines;
    }

    public ScriptTraceEnhancer getDefaultTraceEnhancer() {
        return defaultTraceEnhancer;
    }

    public void setDefaultTraceEnhancer(ScriptTraceEnhancer defaultTraceEnhancer) {
        this.defaultTraceEnhancer = defaultTraceEnhancer;
    }

    public ScriptTraceListener getScriptErrorListener() {
        return scriptErrorListener;
    }

    public void setScriptErrorListener(ScriptTraceListener scriptErrorListener) {
        this.scriptErrorListener = scriptErrorListener;
    }

    public ScriptTraceListener getScriptSuccessListener() {
        return scriptSuccessListener;
    }

    public void setScriptSuccessListener(ScriptTraceListener scriptSuccessListener) {
        this.scriptSuccessListener = scriptSuccessListener;
    }

    public ScriptEngineManager getScriptEngineManager() {
        return scriptEngineManager;
    }
}
