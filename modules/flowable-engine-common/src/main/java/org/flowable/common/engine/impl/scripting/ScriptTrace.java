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
import java.util.Map;

/**
 * Captures meta information about a script invocation, like the start time,
 * the duration of the script execution, tags, whether it ended with an exception, etc.
 */
public interface ScriptTrace {

    ScriptEngineRequest getRequest();

    Throwable getException();

    Map<String, String> getTraceTags();

    Duration getDuration();

    default boolean hasException() {
        return getException() != null;
    }

    default boolean isSuccess() {
        return !hasException();
    }
}
