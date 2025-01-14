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
package org.flowable.test.persistence;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.engine.impl.db.EntityDependencyOrder;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class EntityHelperUtil {

    private static final Pattern ENTITY_RESOURCE_PATTERN = Pattern.compile(".*/entity/(.*)\\.xml");

    static class EntityPackageTestArgumentsProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(

                    Arguments.of(new EntityMappingPackageInformation("org.flowable",
                            EntityDependencyOrder.DELETE_ORDER,
                            EntityDependencyOrder.INSERT_ORDER)),

                    Arguments.of(new EntityMappingPackageInformation("org.flowable.cmmn",
                            org.flowable.cmmn.engine.impl.db.EntityDependencyOrder.DELETE_ORDER,
                            org.flowable.cmmn.engine.impl.db.EntityDependencyOrder.INSERT_ORDER)),

                    Arguments.of(new EntityMappingPackageInformation("org.flowable.dmn",
                            org.flowable.dmn.engine.impl.db.EntityDependencyOrder.DELETE_ORDER,
                            org.flowable.dmn.engine.impl.db.EntityDependencyOrder.INSERT_ORDER)),

                    Arguments.of(new EntityMappingPackageInformation("org.flowable.app",
                            org.flowable.app.engine.impl.db.EntityDependencyOrder.DELETE_ORDER,
                            org.flowable.app.engine.impl.db.EntityDependencyOrder.INSERT_ORDER)),

                    Arguments.of(new EntityMappingPackageInformation("org.flowable.idm",
                            org.flowable.idm.engine.impl.db.EntityDependencyOrder.DELETE_ORDER,
                            org.flowable.idm.engine.impl.db.EntityDependencyOrder.INSERT_ORDER)),

                    Arguments.of(new EntityMappingPackageInformation("org.flowable.eventregistry",
                            org.flowable.eventregistry.impl.db.EntityDependencyOrder.DELETE_ORDER,
                            org.flowable.eventregistry.impl.db.EntityDependencyOrder.INSERT_ORDER))

            );
        }

    }

    /**
     * Returns a map {entityName, xmlMappingFileContent as Document}
     */
    public static Map<String, Document> readMappingFile(EntityMappingPackageInformation packageInformation) {
        try {
            Document mappingsFile = readXmlDocument(packageInformation.getBasePackageFolder() + "/db/mapping/mappings.xml");
            Map<String, Document> resources = new HashMap<>();
            NodeList nodeList = mappingsFile.getElementsByTagName("mapper");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                String resource = node.getAttributes().getNamedItem("resource").getTextContent();
                Matcher resourceMatcher = ENTITY_RESOURCE_PATTERN.matcher(resource);
                if (resourceMatcher.matches()) {
                    String entity = resourceMatcher.group(1);
                    Document entityMappingXmlContent = readXmlDocument(resource);
                    resources.put(entity, entityMappingXmlContent);
                }
            }

            resources.remove("TableData"); // not an entity

            assertFalse(resources.isEmpty());

            return resources;
        } catch (Exception e) {
            throw new FlowableException("Error getting mapped resources", e);
        }
    }

    public static Document readXmlDocument(String resourceLocation) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setValidating(false);
        docBuilderFactory.setNamespaceAware(false);
        docBuilderFactory.setExpandEntityReferences(false);
        docBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        docBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        return docBuilder.parse(EntityHelperUtil.class.getClassLoader().getResourceAsStream(resourceLocation));
    }

    /**
     * Returns a map {entityName, xmlMappingFileContent as Document}
     */
    public static Map<String, String> readMappingFileAsString(EntityMappingPackageInformation packageInformation) {
        try {
            Document mappingsFile = readXmlDocument(packageInformation.getBasePackageFolder() + "/db/mapping/mappings.xml");
            Map<String, String> resources = new HashMap<>();
            NodeList nodeList = mappingsFile.getElementsByTagName("mapper");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                String resource = node.getAttributes().getNamedItem("resource").getTextContent();
                Matcher resourceMatcher = ENTITY_RESOURCE_PATTERN.matcher(resource);
                if (resourceMatcher.matches()) {
                    String entity = resourceMatcher.group(1);
                    String entityMappingXmlContent = IOUtils.toString(EntityHelperUtil.class.getClassLoader().getResourceAsStream(resource), StandardCharsets.UTF_8);
                    resources.put(entity, entityMappingXmlContent);
                }
            }

            resources.remove("TableData"); // not an entity

            assertFalse(resources.isEmpty());

            return resources;
        } catch (Exception e) {
            throw new FlowableException("Error getting mapped resources", e);
        }
    }

    public static class EntityMappingPackageInformation {

        private String basePackage;
        private String basePackageFolder;

        private List<Class<? extends Entity>> entityDeleteOrder;
        private List<Class<? extends Entity>> entityInsertOrder;

        public EntityMappingPackageInformation(String basePackage,
                List<Class<? extends Entity>> entityDeleteOrder, List<Class<? extends Entity>> entityInsertOrder) {

            this.basePackage = basePackage;
            this.basePackageFolder = basePackage.replace(".", "/");

            this.entityDeleteOrder = entityDeleteOrder;
            this.entityInsertOrder = entityInsertOrder;
        }

        public String getBasePackage() {
            return basePackage;
        }
        public String getBasePackageFolder() {
            return basePackageFolder;
        }
        public List<Class<? extends Entity>> getEntityDeleteOrder() {
            return entityDeleteOrder;
        }
        public List<Class<? extends Entity>> getEntityInsertOrder() {
            return entityInsertOrder;
        }

        @Override
        public String toString() {
            return basePackage; // used in name for ParameterizedTest
        }
    }

}
