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
package org.flowable.content.engine.test;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.flowable.content.api.ContentManagementService;
import org.flowable.content.api.ContentService;
import org.flowable.content.engine.ContentEngine;
import org.flowable.content.engine.ContentEngineConfiguration;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class FlowableContentExtension implements ParameterResolver, BeforeEachCallback, AfterEachCallback {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String DEFAULT_CONFIGURATION_RESOURCE = "flowable.content.cfg.xml";

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(FlowableContentExtension.class);

    private static final Set<Class<?>> SUPPORTED_PARAMETERS = new HashSet<>(Arrays.asList(
            ContentEngineConfiguration.class,
            ContentEngine.class,
            ContentService.class,
            ContentManagementService.class
    ));

    protected String deploymentId;

    protected ContentEngineConfiguration contentEngineConfiguration;
    protected ContentEngine contentEngine;
    protected ContentService contentService;

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        createContentEngine(extensionContext);
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        ContentEngine contentEngine = ContentTestHelper.getContentEngine(getConfigurationResource(extensionContext));
        contentEngine.getContentEngineConfiguration().getClock().reset();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext context) {
        Class<?> parameterType = parameterContext.getParameter().getType();
        return SUPPORTED_PARAMETERS.contains(parameterType);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context) {
        Class<?> parameterType = parameterContext.getParameter().getType();
        ContentEngine contentEngine = getContentEngine(context);
        if (parameterType.isInstance(contentEngine)) {
            return contentEngine;
        }

        try {
            return ContentEngine.class.getDeclaredMethod("get" + parameterType.getSimpleName()).invoke(contentEngine);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
            throw new ParameterResolutionException("Could not find service " + parameterType, ex);
        }
    }

    protected ContentEngine getContentEngine(ExtensionContext context) {
        return ContentTestHelper.getContentEngine(getConfigurationResource(context));
    }

    protected ContentEngine createContentEngine(ExtensionContext extensionContext) {
        return ContentTestHelper.getContentEngine(getConfigurationResource(extensionContext));
    }

    protected String getConfigurationResource(ExtensionContext context) {
        return DEFAULT_CONFIGURATION_RESOURCE;
    }

    protected ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getRoot().getStore(NAMESPACE);
    }

}
