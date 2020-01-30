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
package org.flowable.eventregistry.test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.api.EventDeploymentBuilder;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.impl.EventRegistryEngine;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.impl.deployer.ParsedDeploymentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public abstract class EventTestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventTestHelper.class);

    public static final String EMPTY_LINE = "\n";

    static Map<String, EventRegistryEngine> eventRegistryEngines = new HashMap<>();

    // Test annotation support /////////////////////////////////////////////

    public static String annotationDeploymentSetUp(EventRepositoryService eventRepositoryService, Class<?> testClass, String methodName) {
        Method method = null;
        try {
            method = testClass.getMethod(methodName, (Class<?>[]) null);
        } catch (Exception e) {
            LOGGER.warn("Could not get method by reflection. This could happen if you are using @Parameters in combination with annotations.", e);
            return null;
        }
        
        EventDeploymentAnnotation eventDeploymentAnnotation = method.getAnnotation(EventDeploymentAnnotation.class);
        ChannelDeploymentAnnotation channelDeploymentAnnotation = method.getAnnotation(ChannelDeploymentAnnotation.class);
        return annotationDeploymentSetUp(eventRepositoryService, testClass, method, eventDeploymentAnnotation, channelDeploymentAnnotation);
    }

    public static String annotationDeploymentSetUp(EventRepositoryService eventRepositoryService, Class<?> testClass, Method method, 
                    EventDeploymentAnnotation eventDeploymentAnnotation, ChannelDeploymentAnnotation channelDeploymentAnnotation) {
        
        String deploymentId = null;
        String tenantId = null;
        String[] resources = null;
        if (eventDeploymentAnnotation != null) {
            String methodName = method.getName();
            LOGGER.debug("annotation @EventDeployment creates deployment for {}.{}", testClass.getSimpleName(), methodName);
            resources = eventDeploymentAnnotation.resources();
            tenantId = eventDeploymentAnnotation.tenantId();
            if (resources.length == 0) {
                String resource = getEventResource(testClass, methodName);
                resources = new String[] { resource };
            }
        }
        
        if (channelDeploymentAnnotation != null) {
            String methodName = method.getName();
            LOGGER.debug("annotation @ChannelDeployment creates deployment for {}.{}", testClass.getSimpleName(), methodName);
            String[] channelResources = channelDeploymentAnnotation.resources();
            tenantId = channelDeploymentAnnotation.tenantId();
            if (channelResources.length == 0) {
                String resource = getChannelResource(testClass, methodName);
                channelResources = new String[] { resource };
            }
            
            if (resources != null) {
                List<String> resourceList = new ArrayList<>(Arrays.asList(resources));
                resourceList.addAll(new ArrayList<>(Arrays.asList(channelResources)));
                resources = resourceList.toArray(new String[channelResources.length]);
            } else {
                resources = channelResources;
            }
        }
        
        if (resources != null && resources.length > 0) {
            EventDeploymentBuilder deploymentBuilder = eventRepositoryService.createDeployment().name(testClass.getSimpleName() + "." + method.getName());
    
            for (String resource : resources) {
                deploymentBuilder.addClasspathResource(resource);
            }
            
            if (StringUtils.isNotEmpty(tenantId)) {
                deploymentBuilder.tenantId(tenantId);
            }
    
            deploymentId = deploymentBuilder.deploy().getId();
        }

        return deploymentId;
    }

    public static void annotationDeploymentTearDown(EventRepositoryService eventRepositoryService, String deploymentId, Class<?> testClass, String methodName) {
        LOGGER.debug("annotation @Deployment deletes deployment for {}.{}", testClass.getSimpleName(), methodName);
        if (deploymentId != null) {
            try {
                eventRepositoryService.deleteDeployment(deploymentId);
            } catch (FlowableObjectNotFoundException e) {
                // Deployment was already deleted by the test case. Ignore.
            }
        }
    }

    /**
     * get a resource location by convention based on a class (type) and a relative resource name. The return value will be the full classpath location of the type, plus a suffix built from the name
     * parameter: <code>EventDeployer.EVENT_RESOURCE_SUFFIXES</code>. The first resource matching a suffix will be returned.
     */
    public static String getEventResource(Class<?> type, String name) {
        for (String suffix : ParsedDeploymentBuilder.EVENT_RESOURCE_SUFFIXES) {
            String resource = type.getName().replace('.', '/') + "." + name + "." + suffix;
            InputStream inputStream = EventTestHelper.class.getClassLoader().getResourceAsStream(resource);
            if (inputStream == null) {
                continue;
            } else {
                return resource;
            }
        }
        return type.getName().replace('.', '/') + "." + name + "." + ParsedDeploymentBuilder.EVENT_RESOURCE_SUFFIXES[0];
    }
    
    /**
     * get a resource location by convention based on a class (type) and a relative resource name. The return value will be the full classpath location of the type, plus a suffix built from the name
     * parameter: <code>EventDeployer.CHANNEL_RESOURCE_SUFFIXES</code>. The first resource matching a suffix will be returned.
     */
    public static String getChannelResource(Class<?> type, String name) {
        for (String suffix : ParsedDeploymentBuilder.CHANNEL_RESOURCE_SUFFIXES) {
            String resource = type.getName().replace('.', '/') + "." + name + "." + suffix;
            InputStream inputStream = EventTestHelper.class.getClassLoader().getResourceAsStream(resource);
            if (inputStream == null) {
                continue;
            } else {
                return resource;
            }
        }
        return type.getName().replace('.', '/') + "." + name + "." + ParsedDeploymentBuilder.CHANNEL_RESOURCE_SUFFIXES[0];
    }

    // Engine startup and shutdown helpers
    // ///////////////////////////////////////////////////

    public static EventRegistryEngine getEventRegistryEngine(String configurationResource) {
        EventRegistryEngine eventRegistryEngine = eventRegistryEngines.get(configurationResource);
        if (eventRegistryEngine == null) {
            LOGGER.debug("==== BUILDING EVENT REGISTRY ENGINE ========================================================================");
            eventRegistryEngine = ((EventRegistryEngineConfiguration) EventRegistryEngineConfiguration.createEventRegistryEngineConfigurationFromResource(configurationResource)
                    .setDatabaseSchemaUpdate(EventRegistryEngineConfiguration.DB_SCHEMA_UPDATE_DROP_CREATE))
                    .buildEventRegistryEngine();
            LOGGER.debug("==== EVENT REGISTRY ENGINE CREATED =========================================================================");
            eventRegistryEngines.put(configurationResource, eventRegistryEngine);
        }
        return eventRegistryEngine;
    }

    public static void closeEventRegistryEngines() {
        for (EventRegistryEngine eventRegistryEngine : eventRegistryEngines.values()) {
            eventRegistryEngine.close();
        }
        eventRegistryEngines.clear();
    }

    /**
     * Each test is assumed to clean up all DB content it entered. After a test method executed, this method scans all tables to see if the DB is completely clean. It throws AssertionFailed in case
     * the DB is not clean. If the DB is not clean, it is cleaned by performing a create a drop.
     */
    public static void assertAndEnsureCleanDb(EventRegistryEngine eventRegistryEngine) {
        LOGGER.debug("verifying that db is clean after test");
        EventRepositoryService repositoryService = eventRegistryEngine.getEventRegistryEngineConfiguration().getEventRepositoryService();
        List<EventDeployment> deployments = repositoryService.createDeploymentQuery().list();
        if (deployments != null && !deployments.isEmpty()) {
            throw new AssertionError("EventDeployments is not empty");
        }
    }

}
