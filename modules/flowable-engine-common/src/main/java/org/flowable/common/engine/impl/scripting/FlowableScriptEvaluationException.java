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

import java.util.Map;
import java.util.stream.Collectors;

import org.flowable.common.engine.api.FlowableException;

/**
 * Thrown by ScriptingEngines in case script evaluation failed.
 * <p>
 * Provides access to the {@link ScriptTrace} for diagnostic purposes.
 * </p>
 */
public class FlowableScriptEvaluationException extends FlowableException {

    protected ScriptTrace errorTrace;

    public FlowableScriptEvaluationException(ScriptTrace errorTrace, Throwable cause) {
        super(createErrorMessage(errorTrace), cause);
        this.errorTrace = errorTrace;
    }

    protected static String createErrorMessage(ScriptTrace trace) {
        StringBuilder b = new StringBuilder();
        Map<String, String> traceTags = trace.getTraceTags();
        b.append(trace.getRequest().getLanguage());
        b.append(" script evaluation failed: ");
        if (trace.getException() != null && trace.getException().getMessage() != null) {
            String message = trace.getException().getMessage();
            b.append("'").append(message).append("'");
        }

        if (!traceTags.isEmpty()) {
            b.append(" Trace: ");
            b.append(traceTags.entrySet().stream()
                    .map(k -> k.getKey() + "=" + k.getValue())
                    .collect(Collectors.joining(", ")));
        }
        return b.toString();
    }

    public ScriptTrace getErrorTrace() {
        return errorTrace;
    }
}
