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

package org.flowable.engine.impl.context;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.flowable.engine.impl.persistence.deploy.ProcessDefinitionInfoCacheObject;
import org.flowable.engine.impl.util.CommandContextUtil;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class BpmnOverrideContext {

    protected static ThreadLocal<Map<String, ObjectNode>> bpmnOverrideContextThreadLocal = new ThreadLocal<>();

    protected static ResourceBundle.Control resourceBundleControl = new ResourceBundleControl();

    public static ObjectNode getBpmnOverrideElementProperties(String id, String processDefinitionId) {
        ObjectNode definitionInfoNode = getProcessDefinitionInfoNode(processDefinitionId);
        ObjectNode elementProperties = null;
        if (definitionInfoNode != null) {
            elementProperties = CommandContextUtil.getProcessEngineConfiguration().getDynamicBpmnService().getBpmnElementProperties(id, definitionInfoNode);
        }
        return elementProperties;
    }

    public static ObjectNode getLocalizationElementProperties(String language, String id, String processDefinitionId, boolean useFallback) {
        ObjectNode definitionInfoNode = getProcessDefinitionInfoNode(processDefinitionId);
        ObjectNode localizationProperties = null;
        if (definitionInfoNode != null) {
            if (!useFallback) {
                localizationProperties = CommandContextUtil.getProcessEngineConfiguration().getDynamicBpmnService().getLocalizationElementProperties(
                        language, id, definitionInfoNode);

            } else {
                HashSet<Locale> candidateLocales = new LinkedHashSet<>();
                candidateLocales.addAll(resourceBundleControl.getCandidateLocales(id, Locale.forLanguageTag(language)));
                for (Locale locale : candidateLocales) {
                    localizationProperties = CommandContextUtil.getProcessEngineConfiguration().getDynamicBpmnService().getLocalizationElementProperties(
                            locale.toLanguageTag(), id, definitionInfoNode);

                    if (localizationProperties != null) {
                        break;
                    }
                }
            }
        }
        return localizationProperties;
    }

    public static void removeBpmnOverrideContext() {
        bpmnOverrideContextThreadLocal.remove();
    }

    protected static ObjectNode getProcessDefinitionInfoNode(String processDefinitionId) {
        Map<String, ObjectNode> bpmnOverrideMap = getBpmnOverrideContext();
        if (!bpmnOverrideMap.containsKey(processDefinitionId)) {
            ProcessDefinitionInfoCacheObject cacheObject = CommandContextUtil.getProcessEngineConfiguration().getDeploymentManager()
                    .getProcessDefinitionInfoCache()
                    .get(processDefinitionId);

            addBpmnOverrideElement(processDefinitionId, cacheObject.getInfoNode());
        }

        return getBpmnOverrideContext().get(processDefinitionId);
    }

    protected static Map<String, ObjectNode> getBpmnOverrideContext() {
        Map<String, ObjectNode> bpmnOverrideMap = bpmnOverrideContextThreadLocal.get();
        if (bpmnOverrideMap == null) {
            bpmnOverrideMap = new HashMap<>();
        }
        return bpmnOverrideMap;
    }

    protected static void addBpmnOverrideElement(String id, ObjectNode infoNode) {
        Map<String, ObjectNode> bpmnOverrideMap = bpmnOverrideContextThreadLocal.get();
        if (bpmnOverrideMap == null) {
            bpmnOverrideMap = new HashMap<>();
            bpmnOverrideContextThreadLocal.set(bpmnOverrideMap);
        }
        bpmnOverrideMap.put(id, infoNode);
    }

    public static class ResourceBundleControl extends ResourceBundle.Control {
        @Override
        public List<Locale> getCandidateLocales(String baseName, Locale locale) {
            return super.getCandidateLocales(baseName, locale);
        }
    }
}
