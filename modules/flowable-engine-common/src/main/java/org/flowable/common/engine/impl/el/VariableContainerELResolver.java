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
package org.flowable.common.engine.impl.el;

import java.beans.FeatureDescriptor;
import java.util.Iterator;

import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.common.engine.impl.javax.el.ELContext;
import org.flowable.common.engine.impl.javax.el.ELResolver;
import org.flowable.variable.api.delegate.VariableScope;
import org.flowable.variable.api.persistence.entity.VariableInstance;

/**
 * @author Joram Barrez
 */
public class VariableContainerELResolver extends ELResolver {

    public static final String VARIABLE_CONTAINER_KEY = "variableContainer";
    public static final String LOGGED_IN_USER_KEY = "authenticatedUserId";

    protected VariableContainer variableContainer;

    public VariableContainerELResolver(VariableContainer variableContainer) {
        this.variableContainer = variableContainer;
    }
    
    @Override
    public Object getValue(ELContext context, Object base, Object property) {
        if (base == null) {
            String variable = (String) property; // according to javadoc, can only be a String
            if (LOGGED_IN_USER_KEY.equals(property)) {
                context.setPropertyResolved(true);
                return Authentication.getAuthenticatedUserId();

            } else if (variableContainer.hasVariable(variable)) {
                Object value = null;
                if (context.getContext(EvaluationState.class) == EvaluationState.WRITE) {
                    if (variableContainer instanceof VariableScope) {
                        VariableInstance variableInstance = ((VariableScope) variableContainer).getVariableInstance(variable);
                        if (!variableInstance.isReadOnly()) {
                            // When we are in a write evaluation context we can only access the variable if it is not read only
                            // e.g. this can happen when using multi instance variable aggregation and someone wants to write to
                            // reviews[0].score, reviews is an aggregated variable which is read only
                            value = variableInstance.getValue();
                        }
                    }
                } else {
                    value = variableContainer.getVariable(variable);
                }
                context.setPropertyResolved(true); // if not set, the next elResolver in the CompositeElResolver will be called
                return value;
            } else if (VARIABLE_CONTAINER_KEY.equals(property)) {
                context.setPropertyResolved(true); // if not set, the next elResolver in the CompositeElResolver will be called
                return variableContainer;
            }
        }
        return null;
    }

    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property) {
        if (base == null) {
            String variable = (String) property;
            return !variableContainer.hasVariable(variable);
        }
        return true;
    }

    @Override
    public void setValue(ELContext context, Object base, Object property, Object value) {
        if (base == null) {
            String variable = (String) property;
            if (variableContainer.hasVariable(variable)) {
                context.setPropertyResolved(true);
                if (variableContainer instanceof VariableScope) {
                    VariableInstance variableInstance = ((VariableScope) variableContainer).getVariableInstance(variable);
                    if (variableInstance == null || !variableInstance.isReadOnly()) {
                        variableContainer.setVariable(variable, value);
                    }
                } else {
                    variableContainer.setVariable(variable, value);
                }
            }
        }
    }

    @Override
    public Class<?> getCommonPropertyType(ELContext arg0, Object arg1) {
        return Object.class;
    }

    @Override
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext arg0, Object arg1) {
        return null;
    }

    @Override
    public Class<?> getType(ELContext arg0, Object arg1, Object arg2) {
        return Object.class;
    }

}