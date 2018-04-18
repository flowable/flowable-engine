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
package org.flowable.dmn.engine;

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

public abstract class DmnEngines {

    private static final Logger LOGGER = LoggerFactory.getLogger(DmnEngines.class);

    public static final String NAME_DEFAULT = "default";

    protected static boolean isInitialized;
    protected static Map<String, DmnEngine> dmnEngines = new HashMap<>();
    protected static Map<String, EngineInfo> dmnEngineInfosByName = new HashMap<>();
    protected static Map<String, EngineInfo> dmnEngineInfosByResourceUrl = new HashMap<>();
    protected static List<EngineInfo> dmnEngineInfos = new ArrayList<>();

    /**
     * Initializes all dmn engines that can be found on the classpath for resources <code>flowable.dmn.cfg.xml</code> and for resources <code>flowable-dmn-context.xml</code> (Spring style
     * configuration).
     */
    public static synchronized void init() {
        if (!isInitialized()) {
            if (dmnEngines == null) {
                // Create new map to store dmn engines if current map is null
                dmnEngines = new HashMap<>();
            }
            ClassLoader classLoader = DmnEngines.class.getClassLoader();
            Enumeration<URL> resources = null;
            try {
                resources = classLoader.getResources("flowable.dmn.cfg.xml");
            } catch (IOException e) {
                throw new FlowableException("problem retrieving flowable.dmn.cfg.xml resources on the classpath: " + System.getProperty("java.class.path"), e);
            }

            // Remove duplicated configuration URL's using set. Some
            // classloaders may return identical URL's twice, causing duplicate startups
            Set<URL> configUrls = new HashSet<>();
            while (resources.hasMoreElements()) {
                configUrls.add(resources.nextElement());
            }
            for (Iterator<URL> iterator = configUrls.iterator(); iterator.hasNext();) {
                URL resource = iterator.next();
                LOGGER.info("Initializing dmn engine using configuration '{}'", resource.toString());
                initDmnEngineFromResource(resource);
            }

            try {
                resources = classLoader.getResources("flowable-dmn-context.xml");
            } catch (IOException e) {
                throw new FlowableException("problem retrieving flowable-dmn-context.xml resources on the classpath: " + System.getProperty("java.class.path"), e);
            }

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                LOGGER.info("Initializing dmn engine using Spring configuration '{}'", resource.toString());
                initDmnEngineFromSpringResource(resource);
            }

            setInitialized(true);
        } else {
            LOGGER.info("DMN engines already initialized");
        }
    }

    protected static void initDmnEngineFromSpringResource(URL resource) {
        try {
            Class<?> springConfigurationHelperClass = ReflectUtil.loadClass("org.flowable.dmn.spring.SpringDmnConfigurationHelper");
            Method method = springConfigurationHelperClass.getDeclaredMethod("buildDmnEngine", new Class<?>[] { URL.class });
            DmnEngine dmnEngine = (DmnEngine) method.invoke(null, new Object[] { resource });

            String dmnEngineName = dmnEngine.getName();
            EngineInfo dmnEngineInfo = new EngineInfo(dmnEngineName, resource.toString(), null);
            dmnEngineInfosByName.put(dmnEngineName, dmnEngineInfo);
            dmnEngineInfosByResourceUrl.put(resource.toString(), dmnEngineInfo);

        } catch (Exception e) {
            throw new FlowableException("couldn't initialize dmn engine from spring configuration resource " + resource.toString() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Registers the given dmn engine. No {@link EngineInfo} will be available for this dmn engine. An engine that is registered will be closed when the {@link DmnEngines#destroy()} is called.
     */
    public static void registerDmnEngine(DmnEngine dmnEngine) {
        dmnEngines.put(dmnEngine.getName(), dmnEngine);
    }

    /**
     * Unregisters the given dmn engine.
     */
    public static void unregister(DmnEngine dmnEngine) {
        dmnEngines.remove(dmnEngine.getName());
    }

    private static EngineInfo initDmnEngineFromResource(URL resourceUrl) {
        EngineInfo dmnEngineInfo = dmnEngineInfosByResourceUrl.get(resourceUrl.toString());
        // if there is an existing dmn engine info
        if (dmnEngineInfo != null) {
            // remove that dmn engine from the member fields
            dmnEngineInfos.remove(dmnEngineInfo);
            if (dmnEngineInfo.getException() == null) {
                String dmnEngineName = dmnEngineInfo.getName();
                dmnEngines.remove(dmnEngineName);
                dmnEngineInfosByName.remove(dmnEngineName);
            }
            dmnEngineInfosByResourceUrl.remove(dmnEngineInfo.getResourceUrl());
        }

        String resourceUrlString = resourceUrl.toString();
        try {
            LOGGER.info("initializing dmn engine for resource {}", resourceUrl);
            DmnEngine dmnEngine = buildDmnEngine(resourceUrl);
            String dmnEngineName = dmnEngine.getName();
            LOGGER.info("initialised dmn engine {}", dmnEngineName);
            dmnEngineInfo = new EngineInfo(dmnEngineName, resourceUrlString, null);
            dmnEngines.put(dmnEngineName, dmnEngine);
            dmnEngineInfosByName.put(dmnEngineName, dmnEngineInfo);
        } catch (Throwable e) {
            LOGGER.error("Exception while initializing dmn engine: {}", e.getMessage(), e);
            dmnEngineInfo = new EngineInfo(null, resourceUrlString, ExceptionUtils.getStackTrace(e));
        }
        dmnEngineInfosByResourceUrl.put(resourceUrlString, dmnEngineInfo);
        dmnEngineInfos.add(dmnEngineInfo);
        return dmnEngineInfo;
    }

    protected static DmnEngine buildDmnEngine(URL resource) {
        InputStream inputStream = null;
        try {
            inputStream = resource.openStream();
            DmnEngineConfiguration dmnEngineConfiguration = DmnEngineConfiguration.createDmnEngineConfigurationFromInputStream(inputStream);
            return dmnEngineConfiguration.buildDmnEngine();

        } catch (IOException e) {
            throw new FlowableException("couldn't open resource stream: " + e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    /** Get initialization results. */
    public static List<EngineInfo> getDmnEngineInfos() {
        return dmnEngineInfos;
    }

    /**
     * Get initialization results. Only info will we available for dmn engines which were added in the {@link DmnEngines#init()}. No {@link EngineInfo} is available for engines which were registered
     * programmatically.
     */
    public static EngineInfo getDmnEngineInfo(String dmnEngineName) {
        return dmnEngineInfosByName.get(dmnEngineName);
    }

    public static DmnEngine getDefaultDmnEngine() {
        return getDmnEngine(NAME_DEFAULT);
    }

    /**
     * obtain a dmn engine by name.
     * 
     * @param dmnEngineName
     *            is the name of the dmn engine or null for the default dmn engine.
     */
    public static DmnEngine getDmnEngine(String dmnEngineName) {
        if (!isInitialized()) {
            init();
        }
        return dmnEngines.get(dmnEngineName);
    }

    /**
     * retries to initialize a dmn engine that previously failed.
     */
    public static EngineInfo retry(String resourceUrl) {
        LOGGER.debug("retying initializing of resource {}", resourceUrl);
        try {
            return initDmnEngineFromResource(new URL(resourceUrl));
        } catch (MalformedURLException e) {
            throw new FlowableException("invalid url: " + resourceUrl, e);
        }
    }

    /**
     * provides access to dmn engine to application clients in a managed server environment.
     */
    public static Map<String, DmnEngine> getDmnEngines() {
        return dmnEngines;
    }

    /**
     * closes all dmn engines. This method should be called when the server shuts down.
     */
    public static synchronized void destroy() {
        if (isInitialized()) {
            Map<String, DmnEngine> engines = new HashMap<>(dmnEngines);
            dmnEngines = new HashMap<>();

            for (String dmnEngineName : engines.keySet()) {
                DmnEngine dmnEngine = engines.get(dmnEngineName);
                try {
                    dmnEngine.close();
                } catch (Exception e) {
                    LOGGER.error("exception while closing {}", (dmnEngineName == null ? "the default dmn engine" : "dmn engine " + dmnEngineName), e);
                }
            }

            dmnEngineInfosByName.clear();
            dmnEngineInfosByResourceUrl.clear();
            dmnEngineInfos.clear();

            setInitialized(false);
        }
    }

    public static boolean isInitialized() {
        return isInitialized;
    }

    public static void setInitialized(boolean isInitialized) {
        DmnEngines.isInitialized = isInitialized;
    }
}
