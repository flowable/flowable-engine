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
package org.flowable.app.engine.test.persistence;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.flowable.app.engine.AppEngineConfiguration;
import org.flowable.app.engine.impl.db.EntityDependencyOrder;
import org.flowable.app.engine.impl.persistence.entity.data.impl.TableDataManagerImpl;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Joram Barrez
 */
public class EntitiesTest {
    
    @Test
    public void verifyMappedEntitiesExist() {
        Set<String> mappedResources = getMappedResources();
        assertTrue(mappedResources.size() > 0);
        for (String mappedResource : mappedResources) {
            getAndAssertEntityInterfaceClass(mappedResource);
            getAndAssertEntityImplClass(mappedResource);
        }
    }

    @Test
    public void verifyEntitiesInEntityDependencyOrder() {
        Set<String> mappedResources = getMappedResources();
        for (String mappedResource : mappedResources) {
            assertTrue("No insert entry in EntityDependencyOrder for " + mappedResource, EntityDependencyOrder.INSERT_ORDER.contains(getAndAssertEntityImplClass(mappedResource)));
            assertTrue("No delete entry in EntityDependencyOrder for " + mappedResource, EntityDependencyOrder.DELETE_ORDER.contains(getAndAssertEntityImplClass(mappedResource)));
        }
    }
    
    @Test
    public void verifyEntitiesInTableDataManager() {
        Set<String> mappedResources = getMappedResources();
        for (String mappedResource : mappedResources) {
            assertTrue("No entry in TableDataManagerImpl for " + mappedResource, TableDataManagerImpl.entityToTableNameMap.containsKey(getAndAssertEntityInterfaceClass(mappedResource)));
        }
    }
    
    protected Class getAndAssertEntityInterfaceClass(String mappedResource) {
        try {
            Class c = Class.forName("org.flowable.app.engine.impl.persistence.entity." + mappedResource + "Entity");
            assertNotNull(c);
            return c;
        } catch (Exception e) {
            fail("Entity interface class for " + mappedResource + " not found");
        }
        return null;
    }
    
    protected Class getAndAssertEntityImplClass(String mappedResource) {
        try {
            Class c = Class.forName("org.flowable.app.engine.impl.persistence.entity." + mappedResource + "EntityImpl");
            assertNotNull(c);
            return c;
        } catch (Exception e) {
            fail("Entity interface class for " + mappedResource + " not found");
        }
        return null;
    }
    
    private Set<String> getMappedResources() {
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            docBuilderFactory.setValidating(false);
            docBuilderFactory.setNamespaceAware(false);
            docBuilderFactory.setExpandEntityReferences(false);
            docBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            docBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document document = docBuilder.parse(this.getClass().getClassLoader().getResourceAsStream(AppEngineConfiguration.DEFAULT_MYBATIS_MAPPING_FILE));
            Set<String> resources = new HashSet<>();
            NodeList nodeList = document.getElementsByTagName("mapper");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                String resource = node.getAttributes().getNamedItem("resource").getTextContent();
                if (resource.startsWith("org/flowable/app") && !resource.contains("common.xml")) {
                    resource = resource.replaceAll("org/flowable/app/db/mapping/entity/", "");
                    resource = resource.replaceAll(".xml", "");
                    resources.add(resource);
                }
            }
            
            resources.remove("TableData"); // not an entity
            
            assertTrue(resources.size() > 0);
            return resources;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
