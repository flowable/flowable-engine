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
package org.flowable.eventregistry.impl;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.EngineInfo;
import org.flowable.common.engine.impl.util.ReflectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class EventRegistryEngines {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventRegistryEngines.class);

    public static final String NAME_DEFAULT = "default";

    protected static boolean isInitialized;
    protected static Map<String, EventRegistryEngine> eventRegistryEngines = new HashMap<>();
    protected static Map<String, EngineInfo> eventRegistryEngineInfosByName = new HashMap<>();
    protected static Map<String, EngineInfo> eventRegistryEngineInfosByResourceUrl = new HashMap<>();
    protected static List<EngineInfo> eventRegistryEngineInfos = new ArrayList<>();

    /**
     * Initializes all event registry engines that can be found on the classpath for resources <code>flowable.eventregistry.cfg.xml</code> 
     * and for resources <code>flowable-eventregistry-context.xml</code> (Spring style configuration).
     */
    public static synchronized void init() {
        if (!isInitialized()) {
            if (eventRegistryEngines == null) {
                // Create new map to store event registry engines if current map is null
                eventRegistryEngines = new HashMap<>();
            }
            ClassLoader classLoader = EventRegistryEngines.class.getClassLoader();
            Enumeration<URL> resources = null;
            try {
                resources = classLoader.getResources("flowable.eventregistry.cfg.xml");
            } catch (IOException e) {
                throw new FlowableException("problem retrieving flowable.registry.cfg.xml resources on the classpath: " + System.getProperty("java.class.path"), e);
            }

            // Remove duplicated configuration URL's using set. Some
            // classloaders may return identical URL's twice, causing duplicate startups
            Set<URL> configUrls = new HashSet<>();
            while (resources.hasMoreElements()) {
                configUrls.add(resources.nextElement());
            }
            for (URL resource : configUrls) {
                LOGGER.info("Initializing event registry engine using configuration '{}'", resource);
                initEventRegistryEngineFromResource(resource);
            }

            try {
                resources = classLoader.getResources("flowable-eventregistry-context.xml");
            } catch (IOException e) {
                throw new FlowableException("problem retrieving flowable-registry-context.xml resources on the classpath: " + System.getProperty("java.class.path"), e);
            }

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                LOGGER.info("Initializing event registry engine using Spring configuration '{}'", resource);
                initEventRegistryEngineFromSpringResource(resource);
            }

            setInitialized(true);
        } else {
            LOGGER.info("Event registry engines already initialized");
        }
    }

    protected static void initEventRegistryEngineFromSpringResource(URL resource) {
        try {
            Class<?> springConfigurationHelperClass = ReflectUtil.loadClass("org.flowable.eventregistry.impl.spring.SpringEventRegistryConfigurationHelper");
            Method method = springConfigurationHelperClass.getDeclaredMethod("buildEventRegistryEngine", new Class<?>[] { URL.class });
            EventRegistryEngine eventRegistryEngine = (EventRegistryEngine) method.invoke(null, new Object[] { resource });

            String eventRegistryEngineName = eventRegistryEngine.getName();
            EngineInfo eventRegistryEngineInfo = new EngineInfo(eventRegistryEngineName, resource.toString(), null);
            eventRegistryEngineInfosByName.put(eventRegistryEngineName, eventRegistryEngineInfo);
            eventRegistryEngineInfosByResourceUrl.put(resource.toString(), eventRegistryEngineInfo);

        } catch (Exception e) {
            throw new FlowableException("couldn't initialize event registry engine from spring configuration resource " + resource + ": " + e.getMessage(), e);
        }
    }

    /**
     * Registers the given event registry engine. No {@link EngineInfo} will be available for this event registry engine. An engine that is registered will be closed when the {@link EventRegistryEngines#destroy()} is called.
     */
    public static void registerEventRegistryEngine(EventRegistryEngine eventRegistryEngine) {
        eventRegistryEngines.put(eventRegistryEngine.getName(), eventRegistryEngine);
    }

    /**
     * Unregisters the given event registry engine.
     */
    public static void unregister(EventRegistryEngine eventRegistryEngine) {
        eventRegistryEngines.remove(eventRegistryEngine.getName());
    }

    private static EngineInfo initEventRegistryEngineFromResource(URL resourceUrl) {
        EngineInfo eventRegistryEngineInfo = eventRegistryEngineInfosByResourceUrl.get(resourceUrl.toString());
        // if there is an existing event registry engine info
        if (eventRegistryEngineInfo != null) {
            // remove that event registry engine from the member fields
            eventRegistryEngineInfos.remove(eventRegistryEngineInfo);
            if (eventRegistryEngineInfo.getException() == null) {
                String eventRegistryEngineName = eventRegistryEngineInfo.getName();
                eventRegistryEngines.remove(eventRegistryEngineName);
                eventRegistryEngineInfosByName.remove(eventRegistryEngineName);
            }
            eventRegistryEngineInfosByResourceUrl.remove(eventRegistryEngineInfo.getResourceUrl());
        }

        String resourceUrlString = resourceUrl.toString();
        try {
            LOGGER.info("initializing event registry engine for resource {}", resourceUrl);
            EventRegistryEngine eventRegistryEngine = buildEventRegistryEngine(resourceUrl);
            String eventRegistryEngineName = eventRegistryEngine.getName();
            LOGGER.info("initialised event registry engine {}", eventRegistryEngineName);
            eventRegistryEngineInfo = new EngineInfo(eventRegistryEngineName, resourceUrlString, null);
            eventRegistryEngines.put(eventRegistryEngineName, eventRegistryEngine);
            eventRegistryEngineInfosByName.put(eventRegistryEngineName, eventRegistryEngineInfo);
        } catch (Throwable e) {
            LOGGER.error("Exception while initializing event registry engine: {}", e.getMessage(), e);
            eventRegistryEngineInfo = new EngineInfo(null, resourceUrlString, ExceptionUtils.getStackTrace(e));
        }
        eventRegistryEngineInfosByResourceUrl.put(resourceUrlString, eventRegistryEngineInfo);
        eventRegistryEngineInfos.add(eventRegistryEngineInfo);
        return eventRegistryEngineInfo;
    }

    protected static EventRegistryEngine buildEventRegistryEngine(URL resource) {
        try (InputStream inputStream = resource.openStream()) {
            EventRegistryEngineConfiguration eventRegistryEngineConfiguration = EventRegistryEngineConfiguration.createEventRegistryEngineConfigurationFromInputStream(inputStream);
            return eventRegistryEngineConfiguration.buildEventRegistryEngine();

        } catch (IOException e) {
            throw new FlowableException("couldn't open resource stream: " + e.getMessage(), e);
        }
    }

    /** Get initialization results. */
    public static List<EngineInfo> getEventRegistryEngineInfos() {
        return eventRegistryEngineInfos;
    }

    /**
     * Get initialization results. Only info will we available for event registry engines which were added in the 
     * {@link EventRegistryEngines#init()}. No {@link EngineInfo} is available for engines which were registered
     * programmatically.
     */
    public static EngineInfo getEventRegistryEngineInfo(String eventRegistryEngineName) {
        return eventRegistryEngineInfosByName.get(eventRegistryEngineName);
    }

    public static EventRegistryEngine getDefaultEventRegistryEngine() {
        return getEventRegistryEngine(NAME_DEFAULT);
    }

    /**
     * Obtain an event registry engine by name.
     * 
     * @param eventRegistryEngineName
     *            is the name of the event registry engine or null for the default event registry engine.
     */
    public static EventRegistryEngine getEventRegistryEngine(String eventRegistryEngineName) {
        if (!isInitialized()) {
            init();
        }
        return eventRegistryEngines.get(eventRegistryEngineName);
    }

    /**
     * retries to initialize an event registry engine that previously failed.
     */
    public static EngineInfo retry(String resourceUrl) {
        LOGGER.debug("retying initializing of resource {}", resourceUrl);
        try {
            return initEventRegistryEngineFromResource(new URL(resourceUrl));
        } catch (MalformedURLException e) {
            throw new FlowableException("invalid url: " + resourceUrl, e);
        }
    }

    /**
     * provides access to event registry engine to application clients in a managed server environment.
     */
    public static Map<String, EventRegistryEngine> getEventRegistryEngines() {
        return eventRegistryEngines;
    }

    /**
     * closes all event registry engines. This method should be called when the server shuts down.
     */
    public static synchronized void destroy() {
        if (isInitialized()) {
            Map<String, EventRegistryEngine> engines = new HashMap<>(eventRegistryEngines);
            eventRegistryEngines = new HashMap<>();

            for (String eventRegistryEngineName : engines.keySet()) {
                EventRegistryEngine eventRegistryEngine = engines.get(eventRegistryEngineName);
                try {
                    eventRegistryEngine.close();
                } catch (Exception e) {
                    LOGGER.error("exception while closing {}", (eventRegistryEngineName == null ? "the default event registry engine" : 
                        "event registry engine " + eventRegistryEngineName), e);
                }
            }

            eventRegistryEngineInfosByName.clear();
            eventRegistryEngineInfosByResourceUrl.clear();
            eventRegistryEngineInfos.clear();

            setInitialized(false);
        }
    }

    public static boolean isInitialized() {
        return isInitialized;
    }

    public static void setInitialized(boolean isInitialized) {
        EventRegistryEngines.isInitialized = isInitialized;
    }
}
