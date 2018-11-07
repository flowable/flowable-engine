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
package org.flowable.app.engine;

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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.EngineInfo;
import org.flowable.common.engine.impl.util.ReflectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AppEngines {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppEngines.class);

    public static final String NAME_DEFAULT = "default";

    protected static boolean isInitialized;
    protected static Map<String, AppEngine> appEngines = new HashMap<>();
    protected static Map<String, EngineInfo> appEngineInfosByName = new HashMap<>();
    protected static Map<String, EngineInfo> appEngineInfosByResourceUrl = new HashMap<>();
    protected static List<EngineInfo> appEngineInfos = new ArrayList<>();

    /**
     * Initializes all App engines that can be found on the classpath for resources <code>flowable.app.cfg.xml</code> and for resources <code>flowable-app-context.xml</code> (Spring style
     * configuration).
     */
    public static synchronized void init() {
        if (!isInitialized()) {
            if (appEngines == null) {
                // Create new map to store CMMN engines if current map is null
                appEngines = new HashMap<>();
            }
            ClassLoader classLoader = AppEngines.class.getClassLoader();
            Enumeration<URL> resources = null;
            try {
                resources = classLoader.getResources("flowable.app.cfg.xml");
            } catch (IOException e) {
                throw new FlowableException("problem retrieving flowable.cmmn.cfg.xml resources on the classpath: " + System.getProperty("java.class.path"), e);
            }

            // Remove duplicated configuration URL's using set. Some
            // classloaders may return identical URL's twice, causing duplicate startups
            Set<URL> configUrls = new HashSet<>();
            while (resources.hasMoreElements()) {
                configUrls.add(resources.nextElement());
            }
            for (Iterator<URL> iterator = configUrls.iterator(); iterator.hasNext();) {
                URL resource = iterator.next();
                LOGGER.info("Initializing app engine using configuration '{}'", resource.toString());
                initAppEngineFromResource(resource);
            }

            try {
                resources = classLoader.getResources("flowable-app-context.xml");
            } catch (IOException e) {
                throw new FlowableException("problem retrieving flowable-app-context.xml resources on the classpath: " + System.getProperty("java.class.path"), e);
            }

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                LOGGER.info("Initializing app engine using Spring configuration '{}'", resource.toString());
                initAppEngineFromSpringResource(resource);
            }

            setInitialized(true);
        } else {
            LOGGER.info("App engines already initialized");
        }
    }

    protected static void initAppEngineFromSpringResource(URL resource) {
        try {
            Class<?> springConfigurationHelperClass = ReflectUtil.loadClass("org.flowable.app.spring.SpringAppConfigurationHelper");
            Method method = springConfigurationHelperClass.getDeclaredMethod("buildAppEngine", new Class<?>[] { URL.class });
            AppEngine appEngine = (AppEngine) method.invoke(null, new Object[] { resource });

            String appEngineName = appEngine.getName();
            EngineInfo appEngineInfo = new EngineInfo(appEngineName, resource.toString(), null);
            appEngineInfosByName.put(appEngineName, appEngineInfo);
            appEngineInfosByResourceUrl.put(resource.toString(), appEngineInfo);

        } catch (Exception e) {
            throw new FlowableException("couldn't initialize app engine from spring configuration resource " + resource.toString() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Registers the given app engine. No {@link EngineInfo} will be available for this app engine. An engine that is registered will be closed when the {@link AppEngines#destroy()} is called.
     */
    public static void registerAppEngine(AppEngine appEngine) {
        appEngines.put(appEngine.getName(), appEngine);
    }

    /**
     * Unregisters the given app engine.
     */
    public static void unregister(AppEngine appEngine) {
        appEngines.remove(appEngine.getName());
    }

    private static EngineInfo initAppEngineFromResource(URL resourceUrl) {
        EngineInfo appEngineInfo = appEngineInfosByResourceUrl.get(resourceUrl.toString());
        // if there is an existing app engine info
        if (appEngineInfo != null) {
            // remove that app engine from the member fields
            appEngineInfos.remove(appEngineInfo);
            if (appEngineInfo.getException() == null) {
                String appEngineName = appEngineInfo.getName();
                appEngines.remove(appEngineName);
                appEngineInfosByName.remove(appEngineName);
            }
            appEngineInfosByResourceUrl.remove(appEngineInfo.getResourceUrl());
        }

        String resourceUrlString = resourceUrl.toString();
        try {
            LOGGER.info("initializing app engine for resource {}", resourceUrl);
            AppEngine appEngine = buildAppEngine(resourceUrl);
            String appEngineName = appEngine.getName();
            LOGGER.info("initialised app engine {}", appEngineName);
            appEngineInfo = new EngineInfo(appEngineName, resourceUrlString, null);
            appEngines.put(appEngineName, appEngine);
            appEngineInfosByName.put(appEngineName, appEngineInfo);
        } catch (Throwable e) {
            LOGGER.error("Exception while initializing app engine: {}", e.getMessage(), e);
            appEngineInfo = new EngineInfo(null, resourceUrlString, ExceptionUtils.getStackTrace(e));
        }
        appEngineInfosByResourceUrl.put(resourceUrlString, appEngineInfo);
        appEngineInfos.add(appEngineInfo);
        return appEngineInfo;
    }

    protected static AppEngine buildAppEngine(URL resource) {
        try (InputStream inputStream = resource.openStream()) {
            AppEngineConfiguration appEngineConfiguration = AppEngineConfiguration.createAppEngineConfigurationFromInputStream(inputStream);
            return appEngineConfiguration.buildAppEngine();

        } catch (IOException e) {
            throw new FlowableException("couldn't open resource stream: " + e.getMessage(), e);
        }
    }

    /** Get initialization results. */
    public static List<EngineInfo> getAppEngineInfos() {
        return appEngineInfos;
    }

    /**
     * Get initialization results. Only info will we available for app engines which were added in the {@link AppEngines#init()}. No {@link EngineInfo} is available for engines which were registered
     * programmatically.
     */
    public static EngineInfo getAppEngineInfo(String appEngineName) {
        return appEngineInfosByName.get(appEngineName);
    }

    public static AppEngine getDefaultAppEngine() {
        return getAppEngine(NAME_DEFAULT);
    }

    /**
     * Obtain an app engine by name.
     * 
     * @param appEngineName
     *            is the name of the app engine or null for the default app engine.
     */
    public static AppEngine getAppEngine(String appEngineName) {
        if (!isInitialized()) {
            init();
        }
        return appEngines.get(appEngineName);
    }

    /**
     * retries to initialize an app engine that previously failed.
     */
    public static EngineInfo retry(String resourceUrl) {
        LOGGER.debug("retying initializing of resource {}", resourceUrl);
        try {
            return initAppEngineFromResource(new URL(resourceUrl));
        } catch (MalformedURLException e) {
            throw new FlowableException("invalid url: " + resourceUrl, e);
        }
    }

    /**
     * provides access to app engine to application clients in a managed server environment.
     */
    public static Map<String, AppEngine> getAppEngines() {
        return appEngines;
    }

    /**
     * closes all app engines. This method should be called when the server shuts down.
     */
    public static synchronized void destroy() {
        if (isInitialized()) {
            Map<String, AppEngine> engines = new HashMap<>(appEngines);
            appEngines = new HashMap<>();

            for (String appEngineName : engines.keySet()) {
                AppEngine appEngine = engines.get(appEngineName);
                try {
                    appEngine.close();
                } catch (Exception e) {
                    LOGGER.error("exception while closing {}", (appEngineName == null ? "the default app engine" : "app engine " + appEngineName), e);
                }
            }

            appEngineInfosByName.clear();
            appEngineInfosByResourceUrl.clear();
            appEngineInfos.clear();

            setInitialized(false);
        }
    }

    public static boolean isInitialized() {
        return isInitialized;
    }

    public static void setInitialized(boolean isInitialized) {
        AppEngines.isInitialized = isInitialized;
    }
}
