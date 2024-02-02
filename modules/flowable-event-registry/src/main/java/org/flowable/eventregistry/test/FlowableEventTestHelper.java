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

import java.time.Instant;
import java.util.Date;

import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.impl.EventRegistryEngine;

/**
 * A Helper for the Flowable {@link FlowableEventExtension} that can be used within the JUnit Jupiter context store
 * and users can use it in the tests for easy modifying of the {@link EventRegistryEngine} time and easy access for waiting on the job executor.
 *
 * @author Filip Hrisafov
 */
public class FlowableEventTestHelper {

    protected final EventRegistryEngine eventRegistryEngine;
    protected String deploymentIdFromDeploymentAnnotation;

    public FlowableEventTestHelper(EventRegistryEngine eventRegistryEngine) {
        this.eventRegistryEngine = eventRegistryEngine;
    }

    public EventRegistryEngine getEventRegistryEngine() {
        return eventRegistryEngine;
    }
    
    public EventRepositoryService getEventRepositoryService() {
        return eventRegistryEngine.getEventRepositoryService();
    }

    public String getDeploymentIdFromDeploymentAnnotation() {
        return deploymentIdFromDeploymentAnnotation;
    }

    public void setDeploymentIdFromDeploymentAnnotation(String deploymentIdFromDeploymentAnnotation) {
        this.deploymentIdFromDeploymentAnnotation = deploymentIdFromDeploymentAnnotation;
    }

    public void setCurrentTime(Date date) {
        eventRegistryEngine.getEventRegistryEngineConfiguration().getClock().setCurrentTime(date);
    }

    public void setCurrentTime(Instant instant) {
        eventRegistryEngine.getEventRegistryEngineConfiguration().getClock().setCurrentTime(instant == null ? null : Date.from(instant));
    }

}
