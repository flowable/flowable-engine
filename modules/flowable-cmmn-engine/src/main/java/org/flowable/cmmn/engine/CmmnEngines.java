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
package org.flowable.cmmn.engine;

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

public abstract class CmmnEngines {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmmnEngines.class);

    public static final String NAME_DEFAULT = "default";

    protected static boolean isInitialized;
    protected static Map<String, CmmnEngine> cmmnEngines = new HashMap<>();
    protected static Map<String, EngineInfo> cmmnEngineInfosByName = new HashMap<>();
    protected static Map<String, EngineInfo> cmmnEngineInfosByResourceUrl = new HashMap<>();
    protected static List<EngineInfo> cmmnEngineInfos = new ArrayList<>();

    /**
     * Initializes all CMMN engines that can be found on the classpath for resources <code>flowable.cmmn.cfg.xml</code> and for resources <code>flowable-cmmn-context.xml</code> (Spring style
     * configuration).
     */
    public static synchronized void init() {
        if (!isInitialized()) {
            if (cmmnEngines == null) {
                // Create new map to store CMMN engines if current map is null
                cmmnEngines = new HashMap<>();
            }
            ClassLoader classLoader = CmmnEngines.class.getClassLoader();
            Enumeration<URL> resources = null;
            try {
                resources = classLoader.getResources("flowable.cmmn.cfg.xml");
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
                LOGGER.info("Initializing cmmn engine using configuration '{}'", resource.toString());
                initCmmnEngineFromResource(resource);
            }

            try {
                resources = classLoader.getResources("flowable-cmmn-context.xml");
            } catch (IOException e) {
                throw new FlowableException("problem retrieving flowable-cmmn-context.xml resources on the classpath: " + System.getProperty("java.class.path"), e);
            }

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                LOGGER.info("Initializing cmmn engine using Spring configuration '{}'", resource.toString());
                initCmmnEngineFromSpringResource(resource);
            }

            setInitialized(true);
        } else {
            LOGGER.info("Cmmn engines already initialized");
        }
    }

    protected static void initCmmnEngineFromSpringResource(URL resource) {
        try {
            Class<?> springConfigurationHelperClass = ReflectUtil.loadClass("org.flowable.cmmn.spring.SpringCmmnConfigurationHelper");
            Method method = springConfigurationHelperClass.getDeclaredMethod("buildCmmnEngine", new Class<?>[] { URL.class });
            CmmnEngine cmmnEngine = (CmmnEngine) method.invoke(null, new Object[] { resource });

            String cmmnEngineName = cmmnEngine.getName();
            EngineInfo cmmnEngineInfo = new EngineInfo(cmmnEngineName, resource.toString(), null);
            cmmnEngineInfosByName.put(cmmnEngineName, cmmnEngineInfo);
            cmmnEngineInfosByResourceUrl.put(resource.toString(), cmmnEngineInfo);

        } catch (Exception e) {
            throw new FlowableException("couldn't initialize cmmn engine from spring configuration resource " + resource.toString() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Registers the given cmmn engine. No {@link EngineInfo} will be available for this cmmn engine. An engine that is registered will be closed when the {@link CmmnEngines#destroy()} is called.
     */
    public static void registerCmmnEngine(CmmnEngine cmmnEngine) {
        cmmnEngines.put(cmmnEngine.getName(), cmmnEngine);
    }

    /**
     * Unregisters the given cmmn engine.
     */
    public static void unregister(CmmnEngine cmmnEngine) {
        cmmnEngines.remove(cmmnEngine.getName());
    }

    private static EngineInfo initCmmnEngineFromResource(URL resourceUrl) {
        EngineInfo cmmnEngineInfo = cmmnEngineInfosByResourceUrl.get(resourceUrl.toString());
        // if there is an existing cmmn engine info
        if (cmmnEngineInfo != null) {
            // remove that cmmn engine from the member fields
            cmmnEngineInfos.remove(cmmnEngineInfo);
            if (cmmnEngineInfo.getException() == null) {
                String cmmnEngineName = cmmnEngineInfo.getName();
                cmmnEngines.remove(cmmnEngineName);
                cmmnEngineInfosByName.remove(cmmnEngineName);
            }
            cmmnEngineInfosByResourceUrl.remove(cmmnEngineInfo.getResourceUrl());
        }

        String resourceUrlString = resourceUrl.toString();
        try {
            LOGGER.info("initializing cmmn engine for resource {}", resourceUrl);
            CmmnEngine cmmnEngine = buildCmmnEngine(resourceUrl);
            String cmmnEngineName = cmmnEngine.getName();
            LOGGER.info("initialised cmmn engine {}", cmmnEngineName);
            cmmnEngineInfo = new EngineInfo(cmmnEngineName, resourceUrlString, null);
            cmmnEngines.put(cmmnEngineName, cmmnEngine);
            cmmnEngineInfosByName.put(cmmnEngineName, cmmnEngineInfo);
        } catch (Throwable e) {
            LOGGER.error("Exception while initializing cmmn engine: {}", e.getMessage(), e);
            cmmnEngineInfo = new EngineInfo(null, resourceUrlString, ExceptionUtils.getStackTrace(e));
        }
        cmmnEngineInfosByResourceUrl.put(resourceUrlString, cmmnEngineInfo);
        cmmnEngineInfos.add(cmmnEngineInfo);
        return cmmnEngineInfo;
    }

    protected static CmmnEngine buildCmmnEngine(URL resource) {
        try (InputStream inputStream = resource.openStream()) {
            CmmnEngineConfiguration cmmnEngineConfiguration = CmmnEngineConfiguration.createCmmnEngineConfigurationFromInputStream(inputStream);
            return cmmnEngineConfiguration.buildCmmnEngine();

        } catch (IOException e) {
            throw new FlowableException("couldn't open resource stream: " + e.getMessage(), e);
        }
    }

    /** Get initialization results. */
    public static List<EngineInfo> getCmmnEngineInfos() {
        return cmmnEngineInfos;
    }

    /**
     * Get initialization results. Only info will we available for cmmn engines which were added in the {@link CmmnEngines#init()}. No {@link EngineInfo} is available for engines which were registered
     * programmatically.
     */
    public static EngineInfo getCmmnEngineInfo(String cmmnEngineName) {
        return cmmnEngineInfosByName.get(cmmnEngineName);
    }

    public static CmmnEngine getDefaultCmmnEngine() {
        return getCmmnEngine(NAME_DEFAULT);
    }

    /**
     * Obtain a cmmn engine by name.
     * 
     * @param cmmnEngineName
     *            is the name of the cmmn engine or null for the default cmmn engine.
     */
    public static CmmnEngine getCmmnEngine(String cmmnEngineName) {
        if (!isInitialized()) {
            init();
        }
        return cmmnEngines.get(cmmnEngineName);
    }

    /**
     * retries to initialize a cmmn engine that previously failed.
     */
    public static EngineInfo retry(String resourceUrl) {
        LOGGER.debug("retying initializing of resource {}", resourceUrl);
        try {
            return initCmmnEngineFromResource(new URL(resourceUrl));
        } catch (MalformedURLException e) {
            throw new FlowableException("invalid url: " + resourceUrl, e);
        }
    }

    /**
     * provides access to cmmn engine to application clients in a managed server environment.
     */
    public static Map<String, CmmnEngine> getCmmnEngines() {
        return cmmnEngines;
    }

    /**
     * closes all cmmn engines. This method should be called when the server shuts down.
     */
    public static synchronized void destroy() {
        if (isInitialized()) {
            Map<String, CmmnEngine> engines = new HashMap<>(cmmnEngines);
            cmmnEngines = new HashMap<>();

            for (String cmmnEngineName : engines.keySet()) {
                CmmnEngine cmmnEngine = engines.get(cmmnEngineName);
                try {
                    cmmnEngine.close();
                } catch (Exception e) {
                    LOGGER.error("exception while closing {}", (cmmnEngineName == null ? "the default cmmn engine" : "cmmn engine " + cmmnEngineName), e);
                }
            }

            cmmnEngineInfosByName.clear();
            cmmnEngineInfosByResourceUrl.clear();
            cmmnEngineInfos.clear();

            setInitialized(false);
        }
    }

    public static boolean isInitialized() {
        return isInitialized;
    }

    public static void setInitialized(boolean isInitialized) {
        CmmnEngines.isInitialized = isInitialized;
    }
}
