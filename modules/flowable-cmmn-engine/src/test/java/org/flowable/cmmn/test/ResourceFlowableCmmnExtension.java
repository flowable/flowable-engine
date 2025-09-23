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
package org.flowable.cmmn.test;

import static org.flowable.cmmn.engine.test.FlowableCmmnExtension.DEFAULT_CONFIGURATION_RESOURCE;

import java.lang.reflect.Method;
import java.util.List;

import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.CmmnEngines;
import org.flowable.cmmn.engine.impl.test.InternalFlowableCmmnExtension;
import org.flowable.cmmn.engine.test.CmmnConfigurationResource;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.HierarchyTraversalMode;

/**
 * This extension will initialize a Flowable CMMN Engine configuration and use methods annotated with {@link EngineConfigurer} to configure it.
 * The engine will be closed after all tests in the class are executed.
 *
 * @author Filip Hrisafov
 */
public class ResourceFlowableCmmnExtension extends InternalFlowableCmmnExtension implements BeforeAllCallback {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(ResourceFlowableCmmnExtension.class);
    private static final String CONFIGURATION_KEY = "configuration";
    private static final String ENGINE_KEY = "configuration";

    @Override
    public void beforeAll(ExtensionContext context) {
        Class<?> testClass = context.getRequiredTestClass();
        List<Method> methodConfigurers = AnnotationSupport.findAnnotatedMethods(
                context.getRequiredTestClass(),
                EngineConfigurer.class,
                HierarchyTraversalMode.BOTTOM_UP
        );
        if (methodConfigurers.isEmpty()) {
            throw new IllegalStateException("No method annotated with @EngineConfigurer found in " + testClass.getName());
        }

        ExtensionContext.Store store = getStore(context);

        CmmnEngineConfiguration configuration = initializeCmmnEngineConfiguration(context);
        store.put(CONFIGURATION_KEY, configuration);

        for (Method configurerMethod : methodConfigurers) {
            context.getExecutableInvoker().invoke(configurerMethod);
        }

        store.put(ENGINE_KEY, new CloseableEngine(initializeCmmnEngine(configuration)));
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Class<?> parameterType = parameterContext.getParameter().getType();
        if (CmmnEngineConfiguration.class.isAssignableFrom(parameterType)) {
            return AnnotationSupport.isAnnotated(parameterContext.getDeclaringExecutable(), EngineConfigurer.class);
        }

        return super.supportsParameter(parameterContext, extensionContext);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        if (AnnotationSupport.isAnnotated(parameterContext.getDeclaringExecutable(), EngineConfigurer.class)) {
            return getCmmnEngineConfiguration(extensionContext);
        }
        return super.resolveParameter(parameterContext, extensionContext);
    }

    protected String getConfigurationResource(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getTestClass(), CmmnConfigurationResource.class)
                .map(CmmnConfigurationResource::value)
                .orElse(DEFAULT_CONFIGURATION_RESOURCE);
    }

    @Override
    protected CmmnEngine getCmmnEngine(ExtensionContext context) {
        return getStore(context).get(ENGINE_KEY, CloseableEngine.class).cmmnEngine;
    }

    protected CmmnEngineConfiguration getCmmnEngineConfiguration(ExtensionContext context) {
        return getStore(context).get(CONFIGURATION_KEY, CmmnEngineConfiguration.class);
    }

    protected CmmnEngine initializeCmmnEngine(CmmnEngineConfiguration configuration) {
        logger.info("Initializing CmmnEngine with name: {}", configuration.getCmmnEngineName());
        CmmnEngine cmmnEngine = configuration.buildEngine();
        CmmnEngines.setInitialized(true);
        return cmmnEngine;
    }

    protected CmmnEngineConfiguration initializeCmmnEngineConfiguration(ExtensionContext context) {
        String configurationResource = getConfigurationResource(context);
        CmmnEngineConfiguration config = CmmnEngineConfiguration.createCmmnEngineConfigurationFromResource(configurationResource);
        config.setCmmnEngineName(context.getRequiredTestClass().getName());
        return config;
    }

    @Override
    protected ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(NAMESPACE);
    }

    protected static class CloseableEngine implements AutoCloseable {

        protected final CmmnEngine cmmnEngine;

        public CloseableEngine(CmmnEngine cmmnEngine) {
            this.cmmnEngine = cmmnEngine;
        }

        @Override
        public void close() {
            if (cmmnEngine != null) {
                cmmnEngine.close();
            }
        }
    }

}
