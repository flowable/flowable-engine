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
package org.flowable.idm.engine;

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
import org.flowable.common.engine.impl.util.IoUtil;
import org.flowable.common.engine.impl.util.ReflectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class IdmEngines {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdmEngines.class);

    public static final String NAME_DEFAULT = "default";

    protected static boolean isInitialized;
    protected static Map<String, IdmEngine> idmEngines = new HashMap<>();
    protected static Map<String, EngineInfo> idmEngineInfosByName = new HashMap<>();
    protected static Map<String, EngineInfo> idmEngineInfosByResourceUrl = new HashMap<>();
    protected static List<EngineInfo> idmEngineInfos = new ArrayList<>();

    /**
     * Initializes all idm engines that can be found on the classpath for resources <code>flowable.idm.cfg.xml</code> and for resources <code>flowable-idm-context.xml</code> (Spring style
     * configuration).
     */
    public static synchronized void init() {
        if (!isInitialized()) {
            if (idmEngines == null) {
                // Create new map to store idm engines if current map is null
                idmEngines = new HashMap<>();
            }
            ClassLoader classLoader = IdmEngines.class.getClassLoader();
            Enumeration<URL> resources = null;
            try {
                resources = classLoader.getResources("flowable.idm.cfg.xml");
            } catch (IOException e) {
                throw new FlowableException("problem retrieving flowable.idm.cfg.xml resources on the classpath: " + System.getProperty("java.class.path"), e);
            }

            // Remove duplicated configuration URL's using set. Some
            // classloaders may return identical URL's twice, causing duplicate startups
            Set<URL> configUrls = new HashSet<>();
            while (resources.hasMoreElements()) {
                configUrls.add(resources.nextElement());
            }
            for (Iterator<URL> iterator = configUrls.iterator(); iterator.hasNext(); ) {
                URL resource = iterator.next();
                LOGGER.info("Initializing idm engine using configuration '{}'", resource.toString());
                initIdmEngineFromResource(resource);
            }

            try {
                resources = classLoader.getResources("flowable-idm-context.xml");
            } catch (IOException e) {
                throw new FlowableException("problem retrieving flowable-idm-context.xml resources on the classpath: " + System.getProperty("java.class.path"), e);
            }

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                LOGGER.info("Initializing idm engine using Spring configuration '{}'", resource.toString());
                initIdmEngineFromSpringResource(resource);
            }

            setInitialized(true);
        } else {
            LOGGER.info("Idm engines already initialized");
        }
    }

    protected static void initIdmEngineFromSpringResource(URL resource) {
        try {
            Class<?> springConfigurationHelperClass = ReflectUtil.loadClass("org.flowable.idm.spring.SpringIdmConfigurationHelper");
            Method method = springConfigurationHelperClass.getDeclaredMethod("buildIdmEngine", new Class<?>[]{URL.class});
            IdmEngine idmEngine = (IdmEngine) method.invoke(null, new Object[]{resource});

            String idmEngineName = idmEngine.getName();
            EngineInfo idmEngineInfo = new EngineInfo(idmEngineName, resource.toString(), null);
            idmEngineInfosByName.put(idmEngineName, idmEngineInfo);
            idmEngineInfosByResourceUrl.put(resource.toString(), idmEngineInfo);

        } catch (Exception e) {
            throw new FlowableException("couldn't initialize idm engine from spring configuration resource " + resource.toString() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Registers the given idm engine. No {@link EngineInfo} will be available for this idm engine. An engine that is registered will be closed when the {@link IdmEngines#destroy()} is called.
     */
    public static void registerIdmEngine(IdmEngine idmEngine) {
        idmEngines.put(idmEngine.getName(), idmEngine);
    }

    /**
     * Unregisters the given idm engine.
     */
    public static void unregister(IdmEngine idmEngine) {
        idmEngines.remove(idmEngine.getName());
    }

    private static EngineInfo initIdmEngineFromResource(URL resourceUrl) {
        EngineInfo idmEngineInfo = idmEngineInfosByResourceUrl.get(resourceUrl.toString());
        // if there is an existing idm engine info
        if (idmEngineInfo != null) {
            // remove that idm engine from the member fields
            idmEngineInfos.remove(idmEngineInfo);
            if (idmEngineInfo.getException() == null) {
                String idmEngineName = idmEngineInfo.getName();
                idmEngines.remove(idmEngineName);
                idmEngineInfosByName.remove(idmEngineName);
            }
            idmEngineInfosByResourceUrl.remove(idmEngineInfo.getResourceUrl());
        }

        String resourceUrlString = resourceUrl.toString();
        try {
            LOGGER.info("initializing idm engine for resource {}", resourceUrl);
            IdmEngine idmEngine = buildIdmEngine(resourceUrl);
            String idmEngineName = idmEngine.getName();
            LOGGER.info("initialised idm engine {}", idmEngineName);
            idmEngineInfo = new EngineInfo(idmEngineName, resourceUrlString, null);
            idmEngines.put(idmEngineName, idmEngine);
            idmEngineInfosByName.put(idmEngineName, idmEngineInfo);
        } catch (Throwable e) {
            LOGGER.error("Exception while initializing idm engine: {}", e.getMessage(), e);
            idmEngineInfo = new EngineInfo(null, resourceUrlString, ExceptionUtils.getStackTrace(e));
        }
        idmEngineInfosByResourceUrl.put(resourceUrlString, idmEngineInfo);
        idmEngineInfos.add(idmEngineInfo);
        return idmEngineInfo;
    }

    protected static IdmEngine buildIdmEngine(URL resource) {
        InputStream inputStream = null;
        try {
            inputStream = resource.openStream();
            IdmEngineConfiguration idmEngineConfiguration = IdmEngineConfiguration.createIdmEngineConfigurationFromInputStream(inputStream);
            return idmEngineConfiguration.buildIdmEngine();

        } catch (IOException e) {
            throw new FlowableException("couldn't open resource stream: " + e.getMessage(), e);
        } finally {
            IoUtil.closeSilently(inputStream);
        }
    }

    /**
     * Get initialization results.
     */
    public static List<EngineInfo> getIdmEngineInfos() {
        return idmEngineInfos;
    }

    /**
     * Get initialization results. Only info will we available for form engines which were added in the {@link IdmEngines#init()}. No {@link EngineInfo} is available for engines which were registered
     * programmatically.
     */
    public static EngineInfo getIdmEngineInfo(String idmEngineName) {
        return idmEngineInfosByName.get(idmEngineName);
    }

    public static IdmEngine getDefaultIdmEngine() {
        return getIdmEngine(NAME_DEFAULT);
    }

    /**
     * obtain a idm engine by name.
     *
     * @param idmEngineName is the name of the idm engine or null for the default idm engine.
     */
    public static IdmEngine getIdmEngine(String idmEngineName) {
        if (!isInitialized()) {
            init();
        }
        return idmEngines.get(idmEngineName);
    }

    /**
     * retries to initialize a idm engine that previously failed.
     */
    public static EngineInfo retry(String resourceUrl) {
        LOGGER.debug("retying initializing of resource {}", resourceUrl);
        try {
            return initIdmEngineFromResource(new URL(resourceUrl));
        } catch (MalformedURLException e) {
            throw new FlowableException("invalid url: " + resourceUrl, e);
        }
    }

    /**
     * provides access to idm engine to application clients in a managed server environment.
     */
    public static Map<String, IdmEngine> getIdmEngines() {
        return idmEngines;
    }

    /**
     * closes all idm engines. This method should be called when the server shuts down.
     */
    public static synchronized void destroy() {
        if (isInitialized()) {
            Map<String, IdmEngine> engines = new HashMap<>(idmEngines);
            idmEngines = new HashMap<>();

            for (String idmEngineName : engines.keySet()) {
                IdmEngine idmEngine = engines.get(idmEngineName);
                try {
                    idmEngine.close();
                } catch (Exception e) {
                    LOGGER.error("exception while closing {}", (idmEngineName == null ? "the default idm engine" : "idm engine " + idmEngineName), e);
                }
            }

            idmEngineInfosByName.clear();
            idmEngineInfosByResourceUrl.clear();
            idmEngineInfos.clear();

            setInitialized(false);
        }
    }

    public static boolean isInitialized() {
        return isInitialized;
    }

    public static void setInitialized(boolean isInitialized) {
        IdmEngines.isInitialized = isInitialized;
    }
}
