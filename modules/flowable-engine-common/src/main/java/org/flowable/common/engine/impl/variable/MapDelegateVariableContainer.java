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
package org.flowable.common.engine.impl.variable;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.variable.VariableContainer;

/**
 * A simple VariableContainer implementation with the ability to
 * add transient variables in addition to a VariableContainer.
 * <p/>
 * The {@link #getVariable(String)} method first looks if there is a
 * local transient variable and returns it, if not, the delegate method is called.
 * In case there is also a variable with the same key here as well as in the delegate,
 * the delegate is ignored, effectively shadowing the variable of the delegate.
 *
 * @author Arthur Hupka-Merle
 */
public class MapDelegateVariableContainer implements VariableContainer {

    protected Map<String, Object> transientVariables;
    protected VariableContainer delegate;

    public MapDelegateVariableContainer(Map<String, Object> transientVariables, VariableContainer delegate) {
        this.transientVariables = transientVariables;
        this.delegate = delegate;
    }

    public MapDelegateVariableContainer(VariableContainer delegate) {
        this(new HashMap<>(), delegate);
    }

    public MapDelegateVariableContainer() {
        this(new HashMap<>(), VariableContainer.empty());
    }

    /**
     * Checks whether the given key can be resolved by this variable container.
     *
     * @param key the name of the variable
     * @return true in case this variable container defines a variable with the given key.
     * @see #getVariable(String)
     */
    @Override
    public boolean hasVariable(String key) {
        return this.transientVariables.containsKey(key) || this.delegate.hasVariable(key);
    }

    /**
     * The method first looks if there is a local transient variable and returns it. If not, the delegate method is called.
     * In case there is also a variable with the same key here as well as in the delegate,
     * the delegate is ignored, effectively shadowing the variable of the delegate.
     *
     * @return the variable of this variable container, the delegate container if not defined here or null
     * when not found in either.
     */
    @Override
    public Object getVariable(String s) {
        Object o = this.transientVariables.get(s);
        if (o == null) {
            o = this.delegate.getVariable(s);
        }
        return o;
    }

    /**
     * Sets the variable to the delegate.
     * <p/>
     * <b>NOTE</b>: this does not add the variable to this variable container,
     * but to the delegate. In case delegate is {@link VariableContainer#empty()}
     * it is be set as transient variable for this container.
     * Use {@link #addTransientVariable(String, Object)} to add
     * variables local to this variable container only.
     *
     * @param key the variable name
     * @param value the variable value to set to the delegate
     */
    @Override
    public void setVariable(String key, Object value) {
        if (delegate != VariableContainer.empty()) {
            this.delegate.setVariable(key, value);
        } else {
            setTransientVariable(key, value);
        }
    }

    /**
     * Sets a transient variable, which is local to this variable container.
     * Transient variables take precedence over variables
     * for the delegate VariableContainer.
     * Therefore, transient variables do shadow potentially available variables with
     * the same name in the delegate.
     *
     * @param key the variable name
     * @param value the variable value
     * @see #addTransientVariable(String, Object)
     */
    @Override
    public void setTransientVariable(String key, Object value) {
        this.transientVariables.put(key, value);
    }

    /**
     * Convenience method which returns <code>this</code> for method concatenation.
     * Same as {@link #setTransientVariable(String, Object)}
     */
    public MapDelegateVariableContainer addTransientVariable(String s, Object o) {
        setTransientVariable(s, o);
        return this;
    }

    /**
     * Clears all transient variables of this variable container (not touching the delegate).
     */
    public void clearTransientVariables() {
        this.transientVariables.clear();
    }

    @Override
    public String getTenantId() {
        return this.delegate.getTenantId();
    }
}
