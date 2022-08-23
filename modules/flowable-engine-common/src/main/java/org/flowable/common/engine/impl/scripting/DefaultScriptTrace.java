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
import java.util.LinkedHashMap;
import java.util.Map;

public class DefaultScriptTrace implements ScriptTrace, ScriptTraceEnhancer.ScriptTraceContext {

    protected Duration duration;
    protected ScriptEngineRequest request;
    protected Throwable exception;
    protected Map<String, String> traceTags = new LinkedHashMap<>();

    public DefaultScriptTrace(Duration duration, ScriptEngineRequest request, Throwable caughtException) {
        this.duration = duration;
        this.request = request;
        this.exception = caughtException;
    }

    public static DefaultScriptTrace successTrace(Duration duration, ScriptEngineRequest request) {
        return new DefaultScriptTrace(duration, request, null);
    }

    public static DefaultScriptTrace errorTrace(Duration duration, ScriptEngineRequest request, Throwable caughtException) {
        return new DefaultScriptTrace(duration, request, caughtException);
    }

    @Override
    public ScriptTraceEnhancer.ScriptTraceContext addTraceTag(String tag, String value) {
        this.traceTags.put(tag, value);
        return this;
    }

    @Override
    public ScriptEngineRequest getRequest() {
        return request;
    }

    @Override
    public Throwable getException() {
        return exception;
    }

    @Override
    public Map<String, String> getTraceTags() {
        return traceTags;
    }

    @Override
    public Duration getDuration() {
        return duration;
    }
}
