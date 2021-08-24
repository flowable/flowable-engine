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
package org.flowable.cdi.impl.el;

import java.beans.FeatureDescriptor;
import java.util.Iterator;

import javax.el.FunctionMapper;
import javax.el.VariableMapper;
import javax.enterprise.inject.spi.BeanManager;

import org.flowable.cdi.impl.util.BeanManagerLookup;
import org.flowable.cdi.impl.util.ProgrammaticBeanLookup;
import org.flowable.common.engine.impl.javax.el.ELContext;
import org.flowable.common.engine.impl.javax.el.ELResolver;

/**
 * Resolver wrapping an instance of javax.el.ELResolver obtained from the {@link BeanManager}. Allows flowable-engine to resolve Cdi-Beans.
 *
 * This {@link ELResolver} implementation performs lazy lookup of the Cdi-BeanManager and can thus be configured using the spring-based configuration of the process engine:
 *
 * <pre>
 * &lt;property name="customElResolvers"&gt;
 *      &lt;list&gt;
 *          &lt;bean class="org.flowable.cdi.impl.el.CdiResolver" /&gt;
 *      &lt;/list&gt;
 * &lt;/property&gt;
 * </pre>
 *
 * <b>IMPORTANT:</b> If you are using the {@link org.flowable.cdi.CdiStandaloneProcessEngineConfiguration} or {@link org.flowable.cdi.CdiJtaProcessEngineConfiguration}
 * then you don't need to configure this.
 * 
 * @author Daniel Meyer
 */
public class CdiResolver extends ELResolver {

    protected javax.el.ELContext context;

    public CdiResolver() {
        context = new javax.el.ELContext() {

            @Override
            public VariableMapper getVariableMapper() {
                return null;
            }

            @Override
            public FunctionMapper getFunctionMapper() {
                return null;
            }

            @Override
            public javax.el.ELResolver getELResolver() {
                return getWrappedResolver();
            }
        };
    }

    protected BeanManager getBeanManager() {
        return BeanManagerLookup.getBeanManager();
    }

    protected javax.el.ELResolver getWrappedResolver() {
        BeanManager beanManager = getBeanManager();
        javax.el.ELResolver resolver = beanManager.getELResolver();
        return resolver;
    }

    @Override
    public Class<?> getCommonPropertyType(ELContext context, Object base) {
        return getWrappedResolver().getCommonPropertyType(this.context, base);
    }

    @Override
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object base) {
        return getWrappedResolver().getFeatureDescriptors(this.context, base);
    }

    @Override
    public Class<?> getType(ELContext context, Object base, Object property) {
        return getWrappedResolver().getType(this.context, base, property);
    }

    @Override
    public Object getValue(ELContext context, Object base, Object property) {
        try {
            Object result = getWrappedResolver().getValue(this.context, base, property);
            context.setPropertyResolved(result != null);
            return result;
        } catch (IllegalStateException e) {
            // dependent scoped / EJBs
            Object result = ProgrammaticBeanLookup.lookup(property.toString(), getBeanManager());
            context.setPropertyResolved(result != null);
            return result;
        }
    }

    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property) {
        return getWrappedResolver().isReadOnly(this.context, base, property);
    }

    @Override
    public void setValue(ELContext context, Object base, Object property, Object value) {
        getWrappedResolver().setValue(this.context, base, property, value);
    }

    @Override
    public Object invoke(ELContext context, Object base, Object method, java.lang.Class<?>[] paramTypes, Object[] params) {
        Object result = getWrappedResolver().invoke(this.context, base, method, paramTypes, params);
        context.setPropertyResolved(result != null);
        return result;
    }

}
