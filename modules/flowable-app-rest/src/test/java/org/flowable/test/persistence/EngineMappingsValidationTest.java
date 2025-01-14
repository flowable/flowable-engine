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

import static org.assertj.core.api.Assertions.assertThat;
import static org.flowable.test.persistence.EntityParameterTypesOverview.getColumnType;
import static org.flowable.test.persistence.EntityParameterTypesOverview.getParameterType;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.StringUtils;
import org.flowable.test.persistence.EntityHelperUtil.EntityMappingPackageInformation;
import org.flowable.test.persistence.EntityHelperUtil.EntityPackageTestArgumentsProvider;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Various tests for Mybatis mapping files.
 * This test class is here, in the spring boot rest api module, as this module has access to all engines and services.
 *
 * @author Joram Barrez
 */
public class EngineMappingsValidationTest {
    
    private static final List<String> IMMUTABLE_ENTITIES = Arrays.asList(
        "HistoricIdentityLink",
        "HistoricTaskLogEntry",
        "EntityLink",
        "HistoricEntityLink",
        "EventLogEntry",
        "Privilege",
        "PrivilegeMapping",
        "Membership");

    @ParameterizedTest(name = "Package {0}")
    @ArgumentsSource(EntityPackageTestArgumentsProvider.class)
    public void verifyMappedEntitiesExist(EntityMappingPackageInformation packageInformation) {
        Map<String, Document> mappedResources = EntityHelperUtil.readMappingFile(packageInformation);
        assertFalse(mappedResources.isEmpty());

        for (String mappedResource : mappedResources.keySet()) {
            Document mappingFileContent = mappedResources.get(mappedResource);
            getAndAssertEntityInterfaceClass(mappingFileContent, mappedResource);
            getAndAssertEntityImplClass(mappingFileContent, mappedResource);
        }
    }

    @ParameterizedTest(name = "Package {0}")
    @ArgumentsSource(EntityPackageTestArgumentsProvider.class)
    public void verifyEntitiesInEntityDependencyOrder(EntityMappingPackageInformation packageInformation) {
        Map<String, Document> mappedResources = EntityHelperUtil.readMappingFile(packageInformation);
        for (String mappedResource : mappedResources.keySet()) {
            Document mappingFileContent = mappedResources.get(mappedResource);
            assertTrue(packageInformation.getEntityInsertOrder().contains(getAndAssertEntityImplClass(mappingFileContent, mappedResource)),
                "No insert entry in EntityDependencyOrder for " + mappedResource);
            assertTrue(packageInformation.getEntityDeleteOrder().contains(getAndAssertEntityImplClass(mappingFileContent, mappedResource)),
                "No delete entry in EntityDependencyOrder for " + mappedResource);
        }
    }

    @ParameterizedTest(name = "Package {0}")
    @ArgumentsSource(EntityPackageTestArgumentsProvider.class)
    public void verifyInserts(EntityMappingPackageInformation packageInformation) {
        Map<String, Document> mappedResources = EntityHelperUtil.readMappingFile(packageInformation);
        for (String resource : mappedResources.keySet()) {

            Document mappingFileContent = mappedResources.get(resource);

            Class<?> entityClass = getAndAssertEntityImplClass(mappingFileContent, resource);
            if (Modifier.isAbstract(entityClass.getModifiers())) {
                continue;
            }

            List<Node> entityInsertStatements = findEntityInsertStatements(mappingFileContent, resource);
            assertThat(entityInsertStatements)
                .withFailMessage("There should be 1 insert statement for " + resource)
                .hasSizeGreaterThanOrEqualTo(1); // some entities have more than 1 insert (e.g. specific for some dbs)

            List<Node> entityBulkInsertStatements = findEntityBulkInsertStatements(mappingFileContent, resource);
            assertThat(entityBulkInsertStatements)
                .withFailMessage("There are no two bulk insert statement for " + resource)
                .hasSize(2);

            // The columns between regular bulk insert and the bulk insert for Oracle should be the same

            if (resource.equals("EventLogEntry") || resource.equals("HistoricTaskLogEntry")) { // bulk insert differs to using a seqeuence
                continue;
            }

            String bulkInsert1 = entityBulkInsertStatements.get(0).getTextContent();
            List<String> bulkInsert1Columns = getColumnsForBulkInsert(bulkInsert1);

            String bulkInsert2 = entityBulkInsertStatements.get(1).getTextContent();
            List<String> bulkInsert2Columns = getColumnsForBulkInsert(bulkInsert2);

            assertThat(bulkInsert1Columns).as(resource).containsOnlyOnceElementsOf(bulkInsert2Columns);
            assertThat(bulkInsert2Columns).as(resource).containsOnlyOnceElementsOf(bulkInsert1Columns);
        }
    }

    private static Pattern INSERT_PATTERN = Pattern.compile("((.*)into(.*)\\((.*?)_ *\\))");

    private List<String> getColumnsForBulkInsert(String bulkInsert) {
        List<String> result = new ArrayList<>();

        bulkInsert = bulkInsert.toLowerCase(Locale.ROOT)
            .replace("\n", " ")
            .replace("\r", " ")
            .replace("\t", " ");

        Matcher matcher = INSERT_PATTERN.matcher(bulkInsert);
        assertThat(matcher.find()).withFailMessage("Regex didn't work on " + bulkInsert);
        try {
            String columns = matcher.group(4);
            if (StringUtils.isNotEmpty(columns)) {
                String[] splitted = columns.split(",");
                for (String column : splitted) {
                    result.add(column.toLowerCase(Locale.ROOT).replace(",", "").trim());
                }
            }
        } catch (IllegalStateException e) {
            throw new RuntimeException("Could not apply regex on " + bulkInsert);
        }

        return result;
    }

    @ParameterizedTest(name = "Package {0}")
    @ArgumentsSource(EntityPackageTestArgumentsProvider.class)
    public void verifyUpdateStatements(EntityMappingPackageInformation packageInformation) {
        Map<String, Document> mappedResources = EntityHelperUtil.readMappingFile(packageInformation);
        for (String resource : mappedResources.keySet()) {
            Document mappingFileContent = mappedResources.get(resource);

            Class<?> entityClass = getAndAssertEntityImplClass(mappingFileContent, resource);
            if (Modifier.isAbstract(entityClass.getModifiers())
                    || IMMUTABLE_ENTITIES.contains(resource)) { // these entities are insert only
                continue;
            }

            List<Node> entityUpdateStatements = findEntityUpdateStatements(mappingFileContent, resource);
            assertThat(entityUpdateStatements)
                .withFailMessage("No update statement for " + resource)
                .hasSize(1);

            Node entityUpdateStatement = entityUpdateStatements.get(0);
            Node setNode = findFirstChildNode(entityUpdateStatement, "set");
            List<Node> ifNodes = findChildNodes(setNode, "if");
            if (ifNodes.isEmpty()) {
                assertThat(setNode.getTextContent().trim())
                    .withFailMessage("No <if> or final comma for update statement of " + resource)
                    .endsWith(",");

            } else {
                for (Node ifNode : ifNodes) {
                    assertThat(ifNode.getTextContent().trim())
                        .withFailMessage("Comma missing for update statement of " + resource)
                        .endsWith(",");
                }

            }
        }
    }

    @ParameterizedTest(name = "Package {0}")
    @ArgumentsSource(EntityPackageTestArgumentsProvider.class)
    public void verifyAllParametersAreTyped(EntityMappingPackageInformation packageInformation) throws IOException {
        Map<String, String> mappedResources = EntityHelperUtil.readMappingFileAsString(packageInformation);
        assertFalse(mappedResources.isEmpty());

        for (String mappedResource : mappedResources.keySet()) {
            System.out.println("Checking mapping " + mappedResource);
            String xmlContent = mappedResources.get(mappedResource);

            int lineCounter = 0;
            try (BufferedReader reader = new BufferedReader(new StringReader(xmlContent))) {
                String line;
                while ((line = reader.readLine()) != null) {

                    lineCounter++;

                    // Checking the string replacements (they are passed by ${} )
                    if (line.contains("${")) {

                        int index = -1;
                        while ((index = line.indexOf("${", index + 1)) > 0) {
                            int endIndex = line.indexOf("}", index);
                            assertThat(endIndex).isGreaterThan(0);
                            String content = line.substring(index + 2, endIndex);

                            assertThat(content).isIn("prefix", "queryTablePrefix", "queryTablePrefixSelect",
                                    "limitBefore", "limitBetween", "limitAfter",
                                    "blobType", "orderBy", "outerJoinOrderBy", "wildcardEscapeClause", "sql");
                        }

                    }

                    // ResultMap validation
                    if (line.contains("<result ") || line.contains("<id ")) {
                        int columnBeginIndex = line.indexOf("column=\"");
                        String columnName = line.substring(columnBeginIndex + 8, line.indexOf("\"", columnBeginIndex + 8));
                        assertThat(columnName).withFailMessage("No column set on line " + lineCounter).isNotNull();

                        String expectedColumnType = getColumnType(mappedResource, columnName);
                        assertThat(expectedColumnType)
                                .withFailMessage("No jdbcType configured in " + EntityParameterTypesOverview.class + " for column " + columnName
                                        + " on line " + lineCounter + " for resource " + mappedResource)
                                .isNotNull();

                        if (!line.contains("typeHandler")) {
                            int jdbcTypeBeginIndex = line.indexOf("jdbcType=\"");
                            String columnType = line.substring(jdbcTypeBeginIndex + 10, line.indexOf("\"", jdbcTypeBeginIndex + 10));
                            assertThat(columnType)
                                    .withFailMessage("Wrong jdbcType in " + mappedResource + " on line " + lineCounter + " for resource " + mappedResource
                                            + ", should be " + expectedColumnType + ", but was " + columnType)
                                    .isEqualTo(expectedColumnType);
                        }

                    }

                    // The #{...} are the parameters
                    if (line.contains("#{")) {

                        int index = -1;
                        while ((index = line.indexOf("#{", index + 1)) > 0) {
                            int endIndex = line.indexOf("}", index);
                            String content = line.substring(index + 2, endIndex);

                            if (!content.contains("typeHandler")) { // only checking jdbcTypes, not the typeHandler
                                assertThat(content)
                                        .withFailMessage("Missing 'jdbcType' in " + mappedResource
                                                + " on line " + lineCounter + ": #{" + content + "}" + " for resource " + mappedResource)
                                        .contains(", jdbcType=");

                                int commaIndex = line.indexOf(",", index);
                                String parameterName = line.substring(index + 2, commaIndex).trim();

                                int equalsSignIndex = line.indexOf("=", commaIndex);
                                String jdbcType = line.substring(equalsSignIndex + 1, endIndex).trim();

                                if (jdbcType.contains("blobType")) { // quick-fix for reading ${blobType}
                                    jdbcType = line.substring(equalsSignIndex + 1, endIndex + 1).trim();
                                }

                                // Special handling when it's just passing parameter
                                if ("parameter".equals(parameterName)) {

                                    assertThat(jdbcType)
                                            .withFailMessage("No jdbcType in " + mappedResource + " on line " + lineCounter + " for resource ")
                                            .isNotNull();

                                } else {

                                    String expectedParameterType = getParameterType(mappedResource, parameterName);
                                    assertThat(expectedParameterType)
                                            .withFailMessage("No jdbcType configured in " + EntityParameterTypesOverview.class + " for parameter '" + parameterName
                                                    + "' on line " + lineCounter + " for resource " + mappedResource)
                                            .isNotNull();
                                    assertThat(jdbcType)
                                            .withFailMessage("Wrong jdbcType in " + mappedResource + " on line " + lineCounter + " for resource " + mappedResource
                                                    + ", should be " + expectedParameterType + ", but was " + jdbcType)
                                            .isEqualTo(expectedParameterType);
                                }
                            }

                        }

                    }
                }
            }
        }
    }

    /*
     * Helper Methods
     */

    protected Class<?> getAndAssertEntityInterfaceClass(Document mappingFileContent, String mappedEntity) {
        String entityPackage = getEntityPackageFromMapperElement(mappingFileContent);
        String expectedClass = entityPackage + "." + mappedEntity + "Entity";
        try {
            Class<?> c = Class.forName(expectedClass);
            assertNotNull(c);
            return c;
        } catch (Exception e) {
            fail("Entity interface class " + entityPackage + "." + mappedEntity + " for " + mappedEntity + " not found");
        }
        return null;
    }

    protected Class<?> getAndAssertEntityImplClass(Document mappingFileContent, String mappedEntity) {
        String entityPackage = getEntityPackageFromMapperElement(mappingFileContent);
        String expectedClass = entityPackage + "." + mappedEntity + "EntityImpl";
        try {
            Class<?> c = Class.forName(expectedClass);
            assertNotNull(c);
            return c;
        } catch (Exception e) {
            fail("Entity interface class " + expectedClass + " for " + mappedEntity + " not found");
        }
        return null;
    }

    private String getEntityPackageFromMapperElement(Document mappingFileContent) {
        Node mapperNode = findChildNodes(mappingFileContent, "mapper").get(1); // 1 --> the first element is <!DOCTYPE mapper ..., so we need the second
        String namespace = mapperNode.getAttributes().getNamedItem("namespace").getTextContent();
        return namespace.substring(0, namespace.lastIndexOf("."));
    }

    private List<Node> findEntityInsertStatements(Document mappingFileContent, String entity) {
        return findEntityStatement(mappingFileContent, entity, "insert", "insert" + entity);
    }

    private List<Node> findEntityBulkInsertStatements(Document mappingFileContent, String entity) {
        return findEntityStatement(mappingFileContent, entity, "insert", "bulkInsert" + entity);
    }

    private List<Node> findEntityUpdateStatements(Document mappingFileContent, String entity) {
        return findEntityStatement(mappingFileContent, entity, "update", "update" + entity);
    }

    private List<Node> findEntityStatement(Document mappingFileContent, String entity, String tagName, String id) {
        List<Node> result = new ArrayList<>();

        NodeList matchingNodes = mappingFileContent.getElementsByTagName(tagName);
        for (int i = 0; i < matchingNodes.getLength(); i++) {
            Node matchingNode = matchingNodes.item(i);
            if (matchingNode.getAttributes().getNamedItem("id").getTextContent().equals(id)) {
                result.add(matchingNode);
            }
        }
        return result;
    }

    private Node findFirstChildNode(Node node, String elementName) {
        List<Node> childNodes = findChildNodes(node, elementName);
        assertThat(childNodes)
            .withFailMessage(toString(node) + " does not have child element " + elementName)
            .isNotEmpty();
        return childNodes.get(0);
    }

    private List<Node> findChildNodes(Node node, String elementName) {
        List<Node> result = new ArrayList<>();
        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            if (elementName.equals(childNode.getNodeName())) {
                result.add(childNode);
            }
        }
        return result;
    }

    private String toString(Node node) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            TransformerFactory.newInstance().newTransformer().transform(new DOMSource(node), new StreamResult(byteArrayOutputStream));
            return byteArrayOutputStream.toString();
        } catch (TransformerException te) {
            fail();
        }
        return null;
    }

}
