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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.flowable.common.engine.api.FlowableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages and provides access to Flowable Scripting.
 *
 * @author Tom Baeyens
 * @author Joram Barrez
 * @author Frederik Heremans
 * @author Arthur Hupka-Merle
 * @see FlowableScriptEngine
 * @see JSR223FlowableScriptEngine
 */
public class ScriptingEngines {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptingEngines.class);

    public static final String DEFAULT_SCRIPTING_LANGUAGE = "juel";
    public static final String GROOVY_SCRIPTING_LANGUAGE = "groovy";

    protected final FlowableScriptEngine scriptEngine;
    protected ScriptBindingsFactory scriptBindingsFactory;

    protected ScriptTraceEnhancer defaultTraceEnhancer;

    protected ScriptTraceListener scriptErrorListener = null;
    protected ScriptTraceListener scriptSuccessListener = null;

    public ScriptingEngines(FlowableScriptEngine scriptEngine, ScriptBindingsFactory scriptBindingsFactory) {
        this.scriptEngine = scriptEngine;
        this.scriptBindingsFactory = scriptBindingsFactory;
    }

    public ScriptEvaluation evaluate(ScriptEngineRequest request) {
        FlowableScriptEvaluationRequest evaluationRequest = scriptEngine.createEvaluationRequest()
                .language(request.getLanguage())
                .script(request.getScript())
                .scopeContainer(request.getScopeContainer())
                .inputVariableContainer(request.getInputVariableContainer())
                .resolver(getScriptBindingsFactory().createResolver(request));
        if (request.isStoreScriptVariables()) {
            evaluationRequest.storeScriptVariables();
        }

        long startNanos = System.nanoTime();
        try {
            ScriptEvaluation evaluationResult = evaluationRequest.evaluate();
            if (scriptSuccessListener != null) {
                DefaultScriptTrace scriptTrace = DefaultScriptTrace.successTrace(Duration.ofNanos(System.nanoTime() - startNanos), request);
                enhanceScriptTrace(request, scriptTrace);
                notifyScriptTraceListener(scriptSuccessListener, scriptTrace);
            }
            return evaluationResult;
        } catch (FlowableScriptException e) {
            DefaultScriptTrace scriptTrace = DefaultScriptTrace.errorTrace(Duration.ofNanos(System.nanoTime() - startNanos), request, e);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Caught exception evaluating script for {}. {}{}{}", request.getScopeContainer(), request.getLanguage(), System.lineSeparator(),
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
            LOGGER.warn("Exception while executing scriptTraceListener: {} with {}", listener, scriptTrace, e);
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

    public ScriptBindingsFactory getScriptBindingsFactory() {
        return scriptBindingsFactory;
    }

    public void setScriptBindingsFactory(ScriptBindingsFactory scriptBindingsFactory) {
        this.scriptBindingsFactory = scriptBindingsFactory;
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

}
