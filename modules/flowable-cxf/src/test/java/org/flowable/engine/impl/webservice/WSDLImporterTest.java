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
package org.flowable.engine.impl.webservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.flowable.common.engine.impl.util.ReflectUtil;
import org.flowable.engine.impl.bpmn.data.SimpleStructureDefinition;
import org.flowable.engine.impl.bpmn.data.StructureDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Esteban Robles Luna
 */
public class WSDLImporterTest {

    private CxfWSDLImporter importer;

    @BeforeEach
    public void setUp() {
        importer = new CxfWSDLImporter();
    }

    @Test
    public void testImportCounter() throws Exception {
        URL url = ReflectUtil.getResource("org/flowable/engine/impl/webservice/counter.wsdl");
        importer.importFrom(url.toString());

        List<WSService> services = new ArrayList<>(importer.getServices().values());
        assertThat(services).hasSize(1);
        WSService service = services.get(0);

        assertThat(service.getName()).isEqualTo("Counter");
        assertThat(service.getLocation()).isEqualTo("http://localhost:63081/webservicemock");

        List<StructureDefinition> structures = sortStructures();
        List<WSOperation> operations = sortOperations();

        assertThat(operations).hasSize(7);
        this.assertOperation(operations.get(0), "getCount", service);
        this.assertOperation(operations.get(1), "inc", service);
        this.assertOperation(operations.get(2), "noNameResult", service);
        this.assertOperation(operations.get(3), "prettyPrintCount", service);
        this.assertOperation(operations.get(4), "reservedWordAsName", service);
        this.assertOperation(operations.get(5), "reset", service);
        this.assertOperation(operations.get(6), "setTo", service);

        assertThat(structures).hasSize(14);
        this.assertStructure(structures.get(0), "getCount", new String[] {}, new Class<?>[] {});
        this.assertStructure(structures.get(1), "getCountResponse", new String[] { "count" }, new Class<?>[] { Integer.class });
        this.assertStructure(structures.get(2), "inc", new String[] {}, new Class<?>[] {});
        this.assertStructure(structures.get(3), "incResponse", new String[] {}, new Class<?>[] {});
        this.assertStructure(structures.get(4), "noNameResult", new String[] { "prefix", "suffix" }, new Class<?>[] { String.class, String.class });
        this.assertStructure(structures.get(5), "noNameResultResponse", new String[] { "return" }, new Class<?>[] { String.class });
        this.assertStructure(structures.get(6), "prettyPrintCount", new String[] { "prefix", "suffix" }, new Class<?>[] { String.class, String.class });
        this.assertStructure(structures.get(7), "prettyPrintCountResponse", new String[] { "prettyPrint" }, new Class<?>[] { String.class });
        this.assertStructure(structures.get(8), "reservedWordAsName", new String[] { "prefix", "suffix" }, new Class<?>[] { String.class, String.class });
        this.assertStructure(structures.get(9), "reservedWordAsNameResponse", new String[] { "static" }, new Class<?>[] { String.class });
        this.assertStructure(structures.get(10), "reset", new String[] {}, new Class<?>[] {});
        this.assertStructure(structures.get(11), "resetResponse", new String[] {}, new Class<?>[] {});
        this.assertStructure(structures.get(12), "setTo", new String[] { "value" }, new Class<?>[] { Integer.class });
        this.assertStructure(structures.get(13), "setToResponse", new String[] {}, new Class<?>[] {});
    }

    @Test
    public void testImportCounterWithImport() throws Exception {
        URL url = ReflectUtil.getResource("org/flowable/engine/impl/webservice/counterWithImport.wsdl");
        importer.importFrom(url.toString());

        List<WSService> services = new ArrayList<>(importer.getServices().values());
        assertThat(services)
                .extracting(WSService::getName, WSService::getLocation)
                .containsExactly(tuple("Counter", "http://localhost:63081/webservicemock"));

        List<StructureDefinition> structures = sortStructures();
        List<WSOperation> operations = sortOperations();

        WSService service = services.get(0);
        assertThat(operations).hasSize(7);
        this.assertOperation(operations.get(0), "getCount", service);
        this.assertOperation(operations.get(1), "inc", service);
        this.assertOperation(operations.get(2), "noNameResult", service);
        this.assertOperation(operations.get(3), "prettyPrintCount", service);
        this.assertOperation(operations.get(4), "reservedWordAsName", service);
        this.assertOperation(operations.get(5), "reset", service);
        this.assertOperation(operations.get(6), "setTo", service);

        assertThat(structures).hasSize(14);
        this.assertStructure(structures.get(0), "getCount", new String[] {}, new Class<?>[] {});
        this.assertStructure(structures.get(1), "getCountResponse", new String[] { "count" }, new Class<?>[] { Integer.class });
        this.assertStructure(structures.get(2), "inc", new String[] {}, new Class<?>[] {});
        this.assertStructure(structures.get(3), "incResponse", new String[] {}, new Class<?>[] {});
        this.assertStructure(structures.get(4), "noNameResult", new String[] { "prefix", "suffix" }, new Class<?>[] { String.class, String.class });
        this.assertStructure(structures.get(5), "noNameResultResponse", new String[] { "return" }, new Class<?>[] { String.class });
        this.assertStructure(structures.get(6), "prettyPrintCount", new String[] { "prefix", "suffix" }, new Class<?>[] { String.class, String.class });
        this.assertStructure(structures.get(7), "prettyPrintCountResponse", new String[] { "prettyPrint" }, new Class<?>[] { String.class });
        this.assertStructure(structures.get(8), "reservedWordAsName", new String[] { "prefix", "suffix" }, new Class<?>[] { String.class, String.class });
        this.assertStructure(structures.get(9), "reservedWordAsNameResponse", new String[] { "static" }, new Class<?>[] { String.class });
        this.assertStructure(structures.get(10), "reset", new String[] {}, new Class<?>[] {});
        this.assertStructure(structures.get(11), "resetResponse", new String[] {}, new Class<?>[] {});
        this.assertStructure(structures.get(12), "setTo", new String[] { "value" }, new Class<?>[] { Integer.class });
        this.assertStructure(structures.get(13), "setToResponse", new String[] {}, new Class<?>[] {});
    }

    private List<WSOperation> sortOperations() {
        List<WSOperation> operations = new ArrayList<>(importer.getOperations().values());
        operations.sort(new Comparator<WSOperation>() {

            @Override
            public int compare(WSOperation o1, WSOperation o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return operations;
    }

    private List<StructureDefinition> sortStructures() {
        List<StructureDefinition> structures = new ArrayList<>(importer.getStructures().values());
        structures.sort(new Comparator<StructureDefinition>() {

            @Override
            public int compare(StructureDefinition o1, StructureDefinition o2) {
                return o1.getId().compareTo(o2.getId());
            }
        });
        return structures;
    }

    private void assertOperation(WSOperation wsOperation, String name, WSService service) {
        assertThat(wsOperation.getName()).isEqualTo(name);
        assertThat(wsOperation.getService()).isEqualTo(service);
    }

    private void assertStructure(StructureDefinition structure, String structureId, String[] parameters, Class<?>[] classes) {
        SimpleStructureDefinition simpleStructure = (SimpleStructureDefinition) structure;

        assertThat(simpleStructure.getId()).isEqualTo(structureId);

        for (int i = 0; i < simpleStructure.getFieldSize(); i++) {
            assertThat(simpleStructure.getFieldNameAt(i)).isEqualTo(parameters[i]);
            assertThat(simpleStructure.getFieldTypeAt(i)).isEqualTo(classes[i]);
        }
    }

    @Test
    public void testImportInheritedElement() throws Exception {
        URL url = ReflectUtil.getResource("org/flowable/engine/impl/webservice/inherited-elements-in-types.wsdl");
        assertThat(url).isNotNull();
        importer.importFrom(url.toString());

        List<StructureDefinition> structures = sortStructures();
        assertThat(structures).hasSize(1);
        final Object structureTypeInst = ReflectUtil.instantiate("org.flowable.webservice.counter.StructureType");
        final Class<? extends Object> structureType = structureTypeInst.getClass();
        this.assertStructure(structures.get(0), "inheritedRequest", new String[] { "rootElt", "inheritedElt", "newSimpleElt",
                "newStructuredElt" }, new Class<?>[] { Short.class, Integer.class, String.class, structureType });
        List<Field> declaredFields = filterJacoco(structureType.getDeclaredFields());
        assertThat(declaredFields).hasSize(2);
        assertThat(structureType.getDeclaredField("booleanElt")).isNotNull();
        assertThat(structureType.getDeclaredField("dateElt")).isNotNull();
        assertThat(filterJacoco(structureType.getSuperclass().getDeclaredFields())).hasSize(1);
        assertThat(structureType.getSuperclass().getDeclaredField("rootElt")).isNotNull();
    }

    protected List<Field> filterJacoco(Field[] declaredFields) {
        return Arrays.stream(declaredFields).filter(
            field -> !field.getName().contains("jacoco")
        ).collect(Collectors.toList());
    }

    @Test
    public void testImportBasicElement() throws Exception {
        URL url = ReflectUtil.getResource("org/flowable/engine/impl/webservice/basic-elements-in-types.wsdl");
        assertThat(url).isNotNull();
        importer.importFrom(url.toString());
    }

    @Test
    public void testComplexTypeMixed() throws Exception {
        URL url = ReflectUtil.getResource("org/flowable/engine/impl/webservice/complexType-mixed.wsdl");
        importer.importFrom(url.toString());
    }
}
