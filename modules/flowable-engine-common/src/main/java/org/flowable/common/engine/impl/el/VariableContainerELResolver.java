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
import org.flowable.common.engine.impl.javax.el.ELContext;
import org.flowable.common.engine.impl.javax.el.ELResolver;

/**
 * @author Joram Barrez
 */
public class VariableContainerELResolver extends ELResolver {

    protected VariableContainer variableContainer;

    public VariableContainerELResolver(VariableContainer variableContainer) {
        this.variableContainer = variableContainer;
    }
    
    @Override
    public Object getValue(ELContext context, Object base, Object property) {
        if (base == null) {
            String variable = (String) property; // according to javadoc, can only be a String
            if (variableContainer.hasVariable(variable)) {
                context.setPropertyResolved(true); // if not set, the next elResolver in the CompositeElResolver will be called
                return variableContainer.getVariable(variable);
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
                variableContainer.setVariable(variable, value);
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