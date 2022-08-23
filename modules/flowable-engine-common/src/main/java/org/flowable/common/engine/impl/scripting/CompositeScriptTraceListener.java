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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Composite implementation of {@link ScriptTraceListener}.
 *
 * @author Arthur Hupka-Merle
 */
public class CompositeScriptTraceListener implements ScriptTraceListener {

    protected final List<ScriptTraceListener> listeners;

    public CompositeScriptTraceListener(Collection<ScriptTraceListener> listeners) {
        this.listeners = listeners != null ? new ArrayList<>(listeners) : Collections.emptyList();
    }

    @Override
    public void onScriptTrace(ScriptTrace scriptTrace) {
        for (ScriptTraceListener l : listeners) {
            l.onScriptTrace(scriptTrace);
        }
    }

    public List<ScriptTraceListener> getListeners() {
        return listeners;
    }
}
