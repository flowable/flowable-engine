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
package org.flowable.bpm.model.bpmn.instance;

import java.util.Collection;

/**
 * The BPMN definitions element.
 */
public interface Definitions
        extends BpmnModelElementInstance {

    String getId();

    void setId(String id);

    String getName();

    void setName(String name);

    String getTargetNamespace();

    void setTargetNamespace(String namespace);

    String getExpressionLanguage();

    void setExpressionLanguage(String expressionLanguage);

    String getTypeLanguage();

    void setTypeLanguage(String typeLanguage);

    String getExporter();

    void setExporter(String exporter);

    String getExporterVersion();

    void setExporterVersion(String exporterVersion);

    Collection<Import> getImports();

    Collection<Extension> getExtensions();

    Collection<RootElement> getRootElements();

    Collection<Relationship> getRelationships();
}
