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
package org.flowable.dmn.engine.impl.deployer;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.repository.EngineResource;
import org.flowable.dmn.engine.impl.persistence.entity.DecisionEntity;

/**
 * Static methods for working with DMN and image resource names.
 */
public class ResourceNameUtil {

    public static final String[] DMN_RESOURCE_SUFFIXES = new String[] { "dmn.xml", "dmn" };
    public static final String[] DIAGRAM_SUFFIXES = new String[] { "png", "jpg", "gif", "svg" };

    public static String stripDmnFileSuffix(String dmnFileResource) {
        for (String suffix : DMN_RESOURCE_SUFFIXES) {
            if (dmnFileResource.endsWith(suffix)) {
                return dmnFileResource.substring(0, dmnFileResource.length() - suffix.length());
            }
        }
        return dmnFileResource;
    }

    public static String getDecisionRequirementsDiagramResourceName(String dmnFileResource, String decisionKey, String diagramSuffix) {
        String dmnFileResourceBase = stripDmnFileSuffix(dmnFileResource);
        return dmnFileResourceBase + decisionKey + "." + diagramSuffix;
    }

    /**
     * Finds the name of a resource for the diagram for a decision definition. Assumes that the decision definition's key and (DMN) resource name are already set.
     *
     * @return name of an existing resource, or null if no matching image resource is found in the resources.
     */
    public static String getDecisionRequirementsDiagramResourceNameFromDeployment(
            DecisionEntity decisionDefinition, Map<String, EngineResource> resources) {

        if (StringUtils.isEmpty(decisionDefinition.getResourceName())) {
            throw new IllegalStateException("Provided decision definition must have its resource name set.");
        }

        String dmnResourceBase = stripDmnFileSuffix(decisionDefinition.getResourceName());
        String key = decisionDefinition.getKey();

        for (String diagramSuffix : DIAGRAM_SUFFIXES) {
            String possibleName = dmnResourceBase + key + "." + diagramSuffix;
            if (resources.containsKey(possibleName)) {
                return possibleName;
            }

            possibleName = dmnResourceBase + diagramSuffix;
            if (resources.containsKey(possibleName)) {
                return possibleName;
            }
        }

        return null;
    }

}
