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
package org.flowable.cmmn.engine.impl.deployer;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntity;
import org.flowable.common.engine.api.repository.EngineResource;

/**
 * Static methods for working with CMMN and image resource names.
 */
public class ResourceNameUtil {

    public static final String[] CMMN_RESOURCE_SUFFIXES = new String[] { "cmmn.xml", "cmmn" };
    public static final String[] DIAGRAM_SUFFIXES = new String[] { "png", "jpg", "gif", "svg" };

    public static String stripCmmnFileSuffix(String cmmnFileResource) {
        for (String suffix : CMMN_RESOURCE_SUFFIXES) {
            if (cmmnFileResource.endsWith(suffix)) {
                return cmmnFileResource.substring(0, cmmnFileResource.length() - suffix.length());
            }
        }
        return cmmnFileResource;
    }

    public static String getCaseDiagramResourceName(String cmmnFileResource, String caseKey, String diagramSuffix) {
        String cmmnFileResourceBase = stripCmmnFileSuffix(cmmnFileResource);
        return cmmnFileResourceBase + caseKey + "." + diagramSuffix;
    }

    /**
     * Finds the name of a resource for the diagram for a case definition. Assumes that the case definition's key and (CMMN) resource name are already set.
     *
     * <p>
     * It will first look for an image resource which matches the case specifically, before resorting to an image resource which matches the CMMN 1.1 xml file resource.
     *
     * <p>
     * Example: if the deployment contains a CMMN 1.1 xml resource called 'abc.cmmn.xml' containing only one case with key 'myCase', then this method will look for an image resources
     * called 'abc.myCase.png' (or .jpg, or .gif, etc.) or 'abc.png' if the previous one wasn't found.
     *
     * <p>
     * Example 2: if the deployment contains a CMMN 1.1 xml resource called 'abc.cmmn.xml' containing three cases (with keys a, b and c), then this method will first look for an image resource
     * called 'abc.a.png' before looking for 'abc.png' (likewise for b and c). Note that if abc.a.png, abc.b.png and abc.c.png don't exist, all cases will have the same image: abc.png.
     *
     * @return name of an existing resource, or null if no matching image resource is found in the resources.
     */
    public static String getCaseDiagramResourceNameFromDeployment(
            CaseDefinitionEntity caseDefinition, Map<String, EngineResource> resources) {

        if (StringUtils.isEmpty(caseDefinition.getResourceName())) {
            throw new IllegalStateException("Provided case definition must have its resource name set.");
        }

        String cmmnResourceBase = stripCmmnFileSuffix(caseDefinition.getResourceName());
        String key = caseDefinition.getKey();

        for (String diagramSuffix : DIAGRAM_SUFFIXES) {
            String possibleName = cmmnResourceBase + key + "." + diagramSuffix;
            if (resources.containsKey(possibleName)) {
                return possibleName;
            }

            possibleName = cmmnResourceBase + diagramSuffix;
            if (resources.containsKey(possibleName)) {
                return possibleName;
            }
        }

        return null;
    }

}
