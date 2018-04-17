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
package org.flowable.form.engine;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.EngineInfo;
import org.flowable.common.engine.impl.util.ReflectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class FormEngines {

    private static final Logger LOGGER = LoggerFactory.getLogger(FormEngines.class);

    public static final String NAME_DEFAULT = "default";

    protected static boolean isInitialized;
    protected static Map<String, FormEngine> formEngines = new HashMap<>();
    protected static Map<String, EngineInfo> formEngineInfosByName = new HashMap<>();
    protected static Map<String, EngineInfo> formEngineInfosByResourceUrl = new HashMap<>();
    protected static List<EngineInfo> formEngineInfos = new ArrayList<>();

    /**
     * Initializes all form engines that can be found on the classpath for resources <code>flowable.form.cfg.xml</code> and for resources <code>flowable-form-context.xml</code> (Spring style
     * configuration).
     */
    public static synchronized void init() {
        if (!isInitialized()) {
            if (formEngines == null) {
                // Create new map to store form engines if current map is null
                formEngines = new HashMap<>();
            }
            ClassLoader classLoader = FormEngines.class.getClassLoader();
            Enumeration<URL> resources = null;
            try {
                resources = classLoader.getResources("flowable.form.cfg.xml");
            } catch (IOException e) {
                throw new FlowableException("problem retrieving flowable.form.cfg.xml resources on the classpath: " + System.getProperty("java.class.path"), e);
            }

            // Remove duplicated configuration URL's using set. Some
            // classloaders may return identical URL's twice, causing duplicate startups
            Set<URL> configUrls = new HashSet<>();
            while (resources.hasMoreElements()) {
                configUrls.add(resources.nextElement());
            }
            for (Iterator<URL> iterator = configUrls.iterator(); iterator.hasNext();) {
                URL resource = iterator.next();
                LOGGER.info("Initializing form engine using configuration '{}'", resource.toString());
                initFormEngineFromResource(resource);
            }

            try {
                resources = classLoader.getResources("flowable-form-context.xml");
            } catch (IOException e) {
                throw new FlowableException("problem retrieving flowable-form-context.xml resources on the classpath: " + System.getProperty("java.class.path"), e);
            }

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                LOGGER.info("Initializing form engine using Spring configuration '{}'", resource.toString());
                initFormEngineFromSpringResource(resource);
            }

            setInitialized(true);
        } else {
            LOGGER.info("Form engines already initialized");
        }
    }

    protected static void initFormEngineFromSpringResource(URL resource) {
        try {
            Class<?> springConfigurationHelperClass = ReflectUtil.loadClass("org.flowable.form.spring.SpringFormConfigurationHelper");
            Method method = springConfigurationHelperClass.getDeclaredMethod("buildFormEngine", new Class<?>[] { URL.class });
            FormEngine formEngine = (FormEngine) method.invoke(null, new Object[] { resource });

            String formEngineName = formEngine.getName();
            EngineInfo formEngineInfo = new EngineInfo(formEngineName, resource.toString(), null);
            formEngineInfosByName.put(formEngineName, formEngineInfo);
            formEngineInfosByResourceUrl.put(resource.toString(), formEngineInfo);

        } catch (Exception e) {
            throw new FlowableException("couldn't initialize form engine from spring configuration resource " + resource.toString() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Registers the given form engine. No {@link EngineInfo} will be available for this form engine. An engine that is registered will be closed when the {@link FormEngines#destroy()} is called.
     */
    public static void registerFormEngine(FormEngine formEngine) {
        formEngines.put(formEngine.getName(), formEngine);
    }

    /**
     * Unregisters the given form engine.
     */
    public static void unregister(FormEngine formEngine) {
        formEngines.remove(formEngine.getName());
    }

    private static EngineInfo initFormEngineFromResource(URL resourceUrl) {
        EngineInfo formEngineInfo = formEngineInfosByResourceUrl.get(resourceUrl.toString());
        // if there is an existing form engine info
        if (formEngineInfo != null) {
            // remove that form engine from the member fields
            formEngineInfos.remove(formEngineInfo);
            if (formEngineInfo.getException() == null) {
                String formEngineName = formEngineInfo.getName();
                formEngines.remove(formEngineName);
                formEngineInfosByName.remove(formEngineName);
            }
            formEngineInfosByResourceUrl.remove(formEngineInfo.getResourceUrl());
        }

        String resourceUrlString = resourceUrl.toString();
        try {
            LOGGER.info("initializing form engine for resource {}", resourceUrl);
            FormEngine formEngine = buildFormEngine(resourceUrl);
            String formEngineName = formEngine.getName();
            LOGGER.info("initialised form engine {}", formEngineName);
            formEngineInfo = new EngineInfo(formEngineName, resourceUrlString, null);
            formEngines.put(formEngineName, formEngine);
            formEngineInfosByName.put(formEngineName, formEngineInfo);
        } catch (Throwable e) {
            LOGGER.error("Exception while initializing form engine: {}", e.getMessage(), e);
            formEngineInfo = new EngineInfo(null, resourceUrlString, ExceptionUtils.getStackTrace(e));
        }
        formEngineInfosByResourceUrl.put(resourceUrlString, formEngineInfo);
        formEngineInfos.add(formEngineInfo);
        return formEngineInfo;
    }

    protected static FormEngine buildFormEngine(URL resource) {
        InputStream inputStream = null;
        try {
            inputStream = resource.openStream();
            FormEngineConfiguration formEngineConfiguration = FormEngineConfiguration.createFormEngineConfigurationFromInputStream(inputStream);
            return formEngineConfiguration.buildFormEngine();

        } catch (IOException e) {
            throw new FlowableException("couldn't open resource stream: " + e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    /** Get initialization results. */
    public static List<EngineInfo> getFormEngineInfos() {
        return formEngineInfos;
    }

    /**
     * Get initialization results. Only info will we available for form engines which were added in the {@link FormEngines#init()}. No {@link EngineInfo} is available for engines which were registered
     * programmatically.
     */
    public static EngineInfo getFormEngineInfo(String formEngineName) {
        return formEngineInfosByName.get(formEngineName);
    }

    public static FormEngine getDefaultFormEngine() {
        return getFormEngine(NAME_DEFAULT);
    }

    /**
     * Obtain a form engine by name.
     * 
     * @param formEngineName
     *            is the name of the form engine or null for the default form engine.
     */
    public static FormEngine getFormEngine(String formEngineName) {
        if (!isInitialized()) {
            init();
        }
        return formEngines.get(formEngineName);
    }

    /**
     * retries to initialize a form engine that previously failed.
     */
    public static EngineInfo retry(String resourceUrl) {
        LOGGER.debug("retying initializing of resource {}", resourceUrl);
        try {
            return initFormEngineFromResource(new URL(resourceUrl));
        } catch (MalformedURLException e) {
            throw new FlowableException("invalid url: " + resourceUrl, e);
        }
    }

    /**
     * provides access to form engine to application clients in a managed server environment.
     */
    public static Map<String, FormEngine> getFormEngines() {
        return formEngines;
    }

    /**
     * closes all form engines. This method should be called when the server shuts down.
     */
    public static synchronized void destroy() {
        if (isInitialized()) {
            Map<String, FormEngine> engines = new HashMap<>(formEngines);
            formEngines = new HashMap<>();

            for (String formEngineName : engines.keySet()) {
                FormEngine formEngine = engines.get(formEngineName);
                try {
                    formEngine.close();
                } catch (Exception e) {
                    LOGGER.error("exception while closing {}", (formEngineName == null ? "the default form engine" : "form engine " + formEngineName), e);
                }
            }

            formEngineInfosByName.clear();
            formEngineInfosByResourceUrl.clear();
            formEngineInfos.clear();

            setInitialized(false);
        }
    }

    public static boolean isInitialized() {
        return isInitialized;
    }

    public static void setInitialized(boolean isInitialized) {
        FormEngines.isInitialized = isInitialized;
    }
}
