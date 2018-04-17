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
package org.flowable.content.engine;

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

public abstract class ContentEngines {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentEngines.class);

    public static final String NAME_DEFAULT = "default";

    protected static boolean isInitialized;
    protected static Map<String, ContentEngine> contentEngines = new HashMap<>();
    protected static Map<String, EngineInfo> contentEngineInfosByName = new HashMap<>();
    protected static Map<String, EngineInfo> contentEngineInfosByResourceUrl = new HashMap<>();
    protected static List<EngineInfo> contentEngineInfos = new ArrayList<>();

    /**
     * Initializes all content engines that can be found on the classpath for resources <code>flowable.content.cfg.xml</code> and for resources <code>flowable-context.xml</code> (Spring style
     * configuration).
     */
    public static synchronized void init() {
        if (!isInitialized()) {
            if (contentEngines == null) {
                // Create new map to store content engines if current map is null
                contentEngines = new HashMap<>();
            }
            ClassLoader classLoader = ContentEngines.class.getClassLoader();
            Enumeration<URL> resources = null;
            try {
                resources = classLoader.getResources("flowable.content.cfg.xml");
            } catch (IOException e) {
                throw new FlowableException("problem retrieving flowable.content.cfg.xml resources on the classpath: " + System.getProperty("java.class.path"), e);
            }

            // Remove duplicated configuration URL's using set. Some
            // classloaders may return identical URL's twice, causing duplicate startups
            Set<URL> configUrls = new HashSet<>();
            while (resources.hasMoreElements()) {
                configUrls.add(resources.nextElement());
            }
            for (Iterator<URL> iterator = configUrls.iterator(); iterator.hasNext();) {
                URL resource = iterator.next();
                LOGGER.info("Initializing content engine using configuration '{}'", resource.toString());
                initContentEngineFromResource(resource);
            }

            try {
                resources = classLoader.getResources("flowable-content-context.xml");
            } catch (IOException e) {
                throw new FlowableException("problem retrieving flowable-content-context.xml resources on the classpath: " + System.getProperty("java.class.path"), e);
            }

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                LOGGER.info("Initializing content engine using Spring configuration '{}'", resource.toString());
                initContentEngineFromSpringResource(resource);
            }

            setInitialized(true);
        } else {
            LOGGER.info("Content engines already initialized");
        }
    }

    protected static void initContentEngineFromSpringResource(URL resource) {
        try {
            Class<?> springConfigurationHelperClass = ReflectUtil.loadClass("org.flowable.content.spring.SpringContentConfigurationHelper");
            Method method = springConfigurationHelperClass.getDeclaredMethod("buildContentEngine", new Class<?>[] { URL.class });
            ContentEngine contentEngine = (ContentEngine) method.invoke(null, new Object[] { resource });

            String contentEngineName = contentEngine.getName();
            EngineInfo contentEngineInfo = new EngineInfo(contentEngineName, resource.toString(), null);
            contentEngineInfosByName.put(contentEngineName, contentEngineInfo);
            contentEngineInfosByResourceUrl.put(resource.toString(), contentEngineInfo);

        } catch (Exception e) {
            throw new FlowableException("couldn't initialize content engine from spring configuration resource " + resource.toString() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Registers the given content engine. No {@link ContentEngine} will be available for this content engine. An engine that is registered will be closed when the {@link ContentEngines#destroy()}
     * is called.
     */
    public static void registerContentEngine(ContentEngine contentEngine) {
        contentEngines.put(contentEngine.getName(), contentEngine);
    }

    /**
     * Unregisters the given content engine.
     */
    public static void unregister(ContentEngine contentEngine) {
        contentEngines.remove(contentEngine.getName());
    }

    private static EngineInfo initContentEngineFromResource(URL resourceUrl) {
        EngineInfo contentEngineInfo = contentEngineInfosByResourceUrl.get(resourceUrl.toString());
        // if there is an existing content engine info
        if (contentEngineInfo != null) {
            // remove that content engine from the member fields
            contentEngineInfos.remove(contentEngineInfo);
            if (contentEngineInfo.getException() == null) {
                String contentEngineName = contentEngineInfo.getName();
                contentEngines.remove(contentEngineName);
                contentEngineInfosByName.remove(contentEngineName);
            }
            contentEngineInfosByResourceUrl.remove(contentEngineInfo.getResourceUrl());
        }

        String resourceUrlString = resourceUrl.toString();
        try {
            LOGGER.info("initializing content engine for resource {}", resourceUrl);
            ContentEngine contentEngine = buildContentEngine(resourceUrl);
            String contentEngineName = contentEngine.getName();
            LOGGER.info("initialised content engine {}", contentEngineName);
            contentEngineInfo = new EngineInfo(contentEngineName, resourceUrlString, null);
            contentEngines.put(contentEngineName, contentEngine);
            contentEngineInfosByName.put(contentEngineName, contentEngineInfo);

        } catch (Throwable e) {
            LOGGER.error("Exception while initializing content engine: {}", e.getMessage(), e);
            contentEngineInfo = new EngineInfo(null, resourceUrlString, ExceptionUtils.getStackTrace(e));
        }
        contentEngineInfosByResourceUrl.put(resourceUrlString, contentEngineInfo);
        contentEngineInfos.add(contentEngineInfo);
        return contentEngineInfo;
    }

    protected static ContentEngine buildContentEngine(URL resource) {
        InputStream inputStream = null;
        try {
            inputStream = resource.openStream();
            ContentEngineConfiguration contentEngineConfiguration = ContentEngineConfiguration.createContentEngineConfigurationFromInputStream(inputStream);
            return contentEngineConfiguration.buildContentEngine();

        } catch (IOException e) {
            throw new FlowableException("couldn't open resource stream: " + e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    /** Get initialization results. */
    public static List<EngineInfo> getContentEngineInfos() {
        return contentEngineInfos;
    }

    /**
     * Get initialization results. Only info will we available for content engines which were added in the {@link ContentEngines#init()}. No
     * {@link EngineInfo} is available for engines which were registered programmatically.
     */
    public static EngineInfo getContentEngineInfo(String contentEngineName) {
        return contentEngineInfosByName.get(contentEngineName);
    }

    public static ContentEngine getDefaultContentEngine() {
        return getContentEngine(NAME_DEFAULT);
    }

    /**
     * Obtain a content engine by name.
     * 
     * @param contentEngineName
     *            is the name of the content engine or null for the default content engine.
     */
    public static ContentEngine getContentEngine(String contentEngineName) {
        if (!isInitialized()) {
            init();
        }
        return contentEngines.get(contentEngineName);
    }

    /**
     * Retries to initialize a content engine that previously failed.
     */
    public static EngineInfo retry(String resourceUrl) {
        LOGGER.debug("retying initializing of resource {}", resourceUrl);
        try {
            return initContentEngineFromResource(new URL(resourceUrl));
        } catch (MalformedURLException e) {
            throw new FlowableException("invalid url: " + resourceUrl, e);
        }
    }

    /**
     * Provides access to content engine to application clients in a managed server environment.
     */
    public static Map<String, ContentEngine> getContentEngines() {
        return contentEngines;
    }

    /**
     * Closes all content engines. This method should be called when the server shuts down.
     */
    public static synchronized void destroy() {
        if (isInitialized()) {
            Map<String, ContentEngine> engines = new HashMap<>(contentEngines);
            contentEngines = new HashMap<>();

            for (String contentEngineName : engines.keySet()) {
                ContentEngine contentEngine = engines.get(contentEngineName);
                try {
                    contentEngine.close();
                } catch (Exception e) {
                    LOGGER.error("exception while closing {}", (contentEngineName == null ? "the default content engine" : "content engine " + contentEngineName), e);
                }
            }

            contentEngineInfosByName.clear();
            contentEngineInfosByResourceUrl.clear();
            contentEngineInfos.clear();

            setInitialized(false);
        }
    }

    public static boolean isInitialized() {
        return isInitialized;
    }

    public static void setInitialized(boolean isInitialized) {
        ContentEngines.isInitialized = isInitialized;
    }
}
