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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;
import javax.script.SimpleScriptContext;

import org.apache.commons.lang3.tuple.Pair;
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
    protected VariableContainer scopeContainer;
    protected VariableContainer inputVariableContainer;
    protected Bindings defaultBindings;
    protected boolean storeScriptVariables = true; // By default everything is stored (backwards compatibility)

    public ScriptBindings(List<Resolver> scriptResolvers, VariableContainer scopeContainer, VariableContainer inputVariableContainer) {
        this.scriptResolvers = scriptResolvers;
        this.scopeContainer = scopeContainer;
        this.inputVariableContainer = inputVariableContainer;
        this.defaultBindings = new SimpleScriptContext().getBindings(SimpleScriptContext.ENGINE_SCOPE);
    }

    public ScriptBindings(List<Resolver> scriptResolvers, VariableContainer scopeContainer, VariableContainer inputVariableContainer,
            boolean storeScriptVariables) {
        this(scriptResolvers, scopeContainer, inputVariableContainer);
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
                oldValue = scopeContainer.getVariable(name);
                scopeContainer.setVariable(name, value);
                return oldValue;
            }
        }
        return defaultBindings.put(name, value);
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        Set<Map.Entry<String, Object>> entries = new HashSet<>();
        for (String key : inputVariableContainer.getVariableNames()) {
            entries.add(Pair.of(key, inputVariableContainer.getVariable(key)));
        }
        return entries;
    }

    @Override
    public Set<String> keySet() {
        return inputVariableContainer.getVariableNames();
    }

    @Override
    public int size() {
        return inputVariableContainer.getVariableNames().size();
    }

    @Override
    public Collection<Object> values() {
        Set<String> variableNames = inputVariableContainer.getVariableNames();
        List<Object> values = new ArrayList<>(variableNames.size());
        for (String key : variableNames) {
            values.add(inputVariableContainer.getVariable(key));
        }
        return values;
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
        if (this.scopeContainer instanceof VariableScope) {
            return ((VariableScope) this.scopeContainer).getVariables();
        }
        return Collections.emptyMap();
    }
}
