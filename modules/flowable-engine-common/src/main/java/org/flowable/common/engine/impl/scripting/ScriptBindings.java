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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;
import javax.script.SimpleScriptContext;

import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.variable.api.delegate.VariableScope;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class ScriptBindings implements Bindings {

    /**
     * The script engine implementations put some key/value pairs into the binding. This list contains those keys, such that they wouldn't be stored as process variable.
     * 
     * This list contains the keywords for JUEL, Javascript and Groovy.
     */
    protected static final Set<String> UNSTORED_KEYS = new HashSet<>(Arrays.asList("out", "out:print", "lang:import", "context", "elcontext", "print", "println", "nashorn.global"));

    protected List<Resolver> scriptResolvers;
    protected VariableContainer variableContainer;
    protected Bindings defaultBindings;
    protected boolean storeScriptVariables = true; // By default everything is stored (backwards compatibility)

    public ScriptBindings(List<Resolver> scriptResolvers, VariableContainer variableContainer) {
        this.scriptResolvers = scriptResolvers;
        this.variableContainer = variableContainer;
        this.defaultBindings = new SimpleScriptContext().getBindings(SimpleScriptContext.ENGINE_SCOPE);
    }

    public ScriptBindings(List<Resolver> scriptResolvers, VariableContainer variableContainer, boolean storeScriptVariables) {
        this(scriptResolvers, variableContainer);
        this.storeScriptVariables = storeScriptVariables;
    }

    @Override
    public boolean containsKey(Object key) {
        for (Resolver scriptResolver : scriptResolvers) {
            if (scriptResolver.containsKey(key)) {
                return true;
            }
        }
        return defaultBindings.containsKey(key);
    }

    @Override
    public Object get(Object key) {
        for (Resolver scriptResolver : scriptResolvers) {
            if (scriptResolver.containsKey(key)) {
                return scriptResolver.get(key);
            }
        }
        return defaultBindings.get(key);
    }

    @Override
    public Object put(String name, Object value) {
        if (storeScriptVariables) {
            Object oldValue = null;
            if (!UNSTORED_KEYS.contains(name)) {
                oldValue = variableContainer.getVariable(name);
                variableContainer.setVariable(name, value);
                return oldValue;
            }
        }
        return defaultBindings.put(name, value);
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        return getVariables().entrySet();
    }

    @Override
    public Set<String> keySet() {
        return getVariables().keySet();
    }

    @Override
    public int size() {
        return getVariables().size();
    }

    @Override
    public Collection<Object> values() {
        return getVariables().values();
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> toMerge) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object remove(Object key) {
        if (UNSTORED_KEYS.contains(key)) {
            return null;
        }
        return defaultBindings.remove(key);
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException();
    }

    public void addUnstoredKey(String unstoredKey) {
        UNSTORED_KEYS.add(unstoredKey);
    }

    protected Map<String, Object> getVariables() {
        if (this.variableContainer instanceof VariableScope) {
            return ((VariableScope) this.variableContainer).getVariables();
        }
        return Collections.emptyMap();
    }
}
