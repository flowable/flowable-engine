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

import jakarta.el.FunctionMapper;
import jakarta.el.VariableMapper;
import jakarta.enterprise.inject.spi.BeanManager;

import org.flowable.cdi.impl.util.BeanManagerLookup;
import org.flowable.cdi.impl.util.ProgrammaticBeanLookup;
import org.flowable.common.engine.impl.javax.el.ELContext;
import org.flowable.common.engine.impl.javax.el.ELResolver;

/**
 * Resolver wrapping an instance of jakarta.el.ELResolver obtained from the {@link BeanManager}. Allows flowable-engine to resolve Cdi-Beans.
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

    /**
     * Creates a new context for the wrapped resolver. A {@link jakarta.el.ELContext} must not be shared between threads and the wrapped resolver
     * keeps state in it (Weld keeps its stack of creational contexts there).
     */
    protected jakarta.el.ELContext createELContext() {
        return new jakarta.el.ELContext() {

            @Override
            public VariableMapper getVariableMapper() {
                return null;
            }

            @Override
            public FunctionMapper getFunctionMapper() {
                return null;
            }

            @Override
            public jakarta.el.ELResolver getELResolver() {
                return getWrappedResolver();
            }
        };
    }

    /**
     * Returns the context for the wrapped resolver that belongs to the given Flowable context. It is created on first use and stored on the
     * Flowable context, which is created for an evaluation, so the wrapped resolver does not see the context of another evaluation.
     */
    protected jakarta.el.ELContext getELContext(ELContext context) {
        jakarta.el.ELContext elContext = (jakarta.el.ELContext) context.getContext(jakarta.el.ELContext.class);
        if (elContext == null) {
            elContext = createELContext();
            context.putContext(jakarta.el.ELContext.class, elContext);
        }
        return elContext;
    }

    protected BeanManager getBeanManager() {
        return BeanManagerLookup.getBeanManager();
    }

    protected jakarta.el.ELResolver getWrappedResolver() {
        BeanManager beanManager = getBeanManager();
        jakarta.el.ELResolver resolver = beanManager.getELResolver();
        return resolver;
    }

    @Override
    public Class<?> getCommonPropertyType(ELContext context, Object base) {
        return getWrappedResolver().getCommonPropertyType(getELContext(context), base);
    }

    @Override
    public Class<?> getType(ELContext context, Object base, Object property) {
        return getWrappedResolver().getType(getELContext(context), base, property);
    }

    @Override
    public Object getValue(ELContext context, Object base, Object property) {
        try {
            Object result = getWrappedResolver().getValue(getELContext(context), base, property);
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
        return getWrappedResolver().isReadOnly(getELContext(context), base, property);
    }

    @Override
    public void setValue(ELContext context, Object base, Object property, Object value) {
        getWrappedResolver().setValue(getELContext(context), base, property, value);
    }

    @Override
    public Object invoke(ELContext context, Object base, Object method, java.lang.Class<?>[] paramTypes, Object[] params) {
        Object result = getWrappedResolver().invoke(getELContext(context), base, method, paramTypes, params);
        context.setPropertyResolved(result != null);
        return result;
    }

}
