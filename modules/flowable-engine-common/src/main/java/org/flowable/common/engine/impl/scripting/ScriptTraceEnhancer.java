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

import org.flowable.common.engine.api.variable.VariableContainer;

/**
 * Functional interface to enhance {@link ScriptTraceContext} information
 * with metadata
 *
 * @author Arthur Hupka-Merle
 */
@FunctionalInterface
public interface ScriptTraceEnhancer {

    /**
     * Allows to add information to script invocations by
     * adding metadata like which can be used to trace the origin of a script invocation.
     *
     * @param scriptTrace the trace object to add information to
     */
    void enhanceScriptTrace(ScriptTraceContext scriptTrace);

    /**
     * Allows enhancing of {@link ScriptTrace ScriptTraces} with additional meta information.
     *
     * @author Arthur Hupka-Merle
     */
    interface ScriptTraceContext {

        /**
         * Adds a tracing tag to this script trace.
         * Tags are used to identify the origin of a script invocation and can also
         * be used to classify script invocations e.g. to distinguish different use-cases
         * etc.
         */
        ScriptTraceContext addTraceTag(String key, String value);

        /**
         * @return the variable container which shall be used to extract trace tags from
         */
        default VariableContainer getVariableContainer() {
            return getRequest().getVariableContainer();
        }

        /**
         * @return the processed request which lead to this script trace.
         */
        ScriptEngineRequest getRequest();

        /**
         * @return the exception (if the request ended in an error). Or <code>null</code> if the
         * request was successful.
         */
        Throwable getException();
    }

}
