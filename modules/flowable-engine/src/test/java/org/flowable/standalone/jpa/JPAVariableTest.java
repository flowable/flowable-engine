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

package org.flowable.standalone.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.variable.service.impl.types.EntityManagerSession;
import org.flowable.variable.service.impl.types.EntityManagerSessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * @author Frederik Heremans
 */
@Tag("jpa")
public class JPAVariableTest extends ResourceFlowableTestCase {

    private FieldAccessJPAEntity simpleEntityFieldAccess;
    private PropertyAccessJPAEntity simpleEntityPropertyAccess;
    private SubclassFieldAccessJPAEntity subclassFieldAccess;
    private SubclassPropertyAccessJPAEntity subclassPropertyAccess;

    private ByteIdJPAEntity byteIdJPAEntity;
    private ShortIdJPAEntity shortIdJPAEntity;
    private IntegerIdJPAEntity integerIdJPAEntity;
    private LongIdJPAEntity longIdJPAEntity;
    private FloatIdJPAEntity floatIdJPAEntity;
    private DoubleIdJPAEntity doubleIdJPAEntity;
    private CharIdJPAEntity charIdJPAEntity;
    private StringIdJPAEntity stringIdJPAEntity;
    private DateIdJPAEntity dateIdJPAEntity;
    private SQLDateIdJPAEntity sqlDateIdJPAEntity;
    private BigDecimalIdJPAEntity bigDecimalIdJPAEntity;
    private BigIntegerIdJPAEntity bigIntegerIdJPAEntity;
    private CompoundIdJPAEntity compoundIdJPAEntity;

    private FieldAccessJPAEntity entityToQuery;
    private FieldAccessJPAEntity entityToUpdate;

    private EntityManagerFactory entityManagerFactory;

    public JPAVariableTest() {
        super("org/flowable/standalone/jpa/flowable.cfg.xml");
    }

    @BeforeEach
    protected void setUp() {
        entityManagerFactory = ((EntityManagerSessionFactory) processEngineConfiguration.getSessionFactories().get(EntityManagerSession.class))
                .getEntityManagerFactory();
        setupJPAEntities();
    }

    public void setupJPAEntities() {

        EntityManager manager = entityManagerFactory.createEntityManager();
        manager.getTransaction().begin();

        // Simple test data
        simpleEntityFieldAccess = new FieldAccessJPAEntity();
        simpleEntityFieldAccess.setId(1L);
        simpleEntityFieldAccess.setValue("value1");
        manager.persist(simpleEntityFieldAccess);

        simpleEntityPropertyAccess = new PropertyAccessJPAEntity();
        simpleEntityPropertyAccess.setId(1L);
        simpleEntityPropertyAccess.setValue("value2");
        manager.persist(simpleEntityPropertyAccess);

        subclassFieldAccess = new SubclassFieldAccessJPAEntity();
        subclassFieldAccess.setId(1L);
        subclassFieldAccess.setValue("value3");
        manager.persist(subclassFieldAccess);

        subclassPropertyAccess = new SubclassPropertyAccessJPAEntity();
        subclassPropertyAccess.setId(1L);
        subclassPropertyAccess.setValue("value4");
        manager.persist(subclassPropertyAccess);

        // Test entities with all possible ID types
        byteIdJPAEntity = new ByteIdJPAEntity();
        byteIdJPAEntity.setByteId((byte) 1);
        manager.persist(byteIdJPAEntity);

        shortIdJPAEntity = new ShortIdJPAEntity();
        shortIdJPAEntity.setShortId((short) 123);
        manager.persist(shortIdJPAEntity);

        integerIdJPAEntity = new IntegerIdJPAEntity();
        integerIdJPAEntity.setIntId(123);
        manager.persist(integerIdJPAEntity);

        longIdJPAEntity = new LongIdJPAEntity();
        longIdJPAEntity.setLongId(123456789L);
        manager.persist(longIdJPAEntity);

        floatIdJPAEntity = new FloatIdJPAEntity();
        floatIdJPAEntity.setFloatId((float) 123.45678);
        manager.persist(floatIdJPAEntity);

        doubleIdJPAEntity = new DoubleIdJPAEntity();
        doubleIdJPAEntity.setDoubleId(12345678.987654);
        manager.persist(doubleIdJPAEntity);

        charIdJPAEntity = new CharIdJPAEntity();
        charIdJPAEntity.setCharId('g');
        manager.persist(charIdJPAEntity);

        dateIdJPAEntity = new DateIdJPAEntity();
        dateIdJPAEntity.setDateId(new java.util.Date());
        manager.persist(dateIdJPAEntity);

        sqlDateIdJPAEntity = new SQLDateIdJPAEntity();
        sqlDateIdJPAEntity.setDateId(new java.sql.Date(Calendar.getInstance().getTimeInMillis()));
        manager.persist(sqlDateIdJPAEntity);

        stringIdJPAEntity = new StringIdJPAEntity();
        stringIdJPAEntity.setStringId("azertyuiop");
        manager.persist(stringIdJPAEntity);

        bigDecimalIdJPAEntity = new BigDecimalIdJPAEntity();
        bigDecimalIdJPAEntity.setBigDecimalId(new BigDecimal("12345678912345678900000.123456789123456789"));
        manager.persist(bigDecimalIdJPAEntity);

        bigIntegerIdJPAEntity = new BigIntegerIdJPAEntity();
        bigIntegerIdJPAEntity.setBigIntegerId(new BigInteger("12345678912345678912345678900000"));
        manager.persist(bigIntegerIdJPAEntity);

        manager.flush();
        manager.getTransaction().commit();
        manager.close();

    }

    public void setupIllegalJPAEntities() {
        EntityManager manager = entityManagerFactory.createEntityManager();
        manager.getTransaction().begin();

        compoundIdJPAEntity = new CompoundIdJPAEntity();
        EmbeddableCompoundId id = new EmbeddableCompoundId();
        id.setIdPart1(123L);
        id.setIdPart2("part2");
        compoundIdJPAEntity.setId(id);
        manager.persist(compoundIdJPAEntity);

        manager.flush();
        manager.getTransaction().commit();
        manager.close();
    }

    public void setupQueryJPAEntity() {
        EntityManager manager = entityManagerFactory.createEntityManager();
        manager.getTransaction().begin();

        entityToQuery = new FieldAccessJPAEntity();
        entityToQuery.setId(2L);
        manager.persist(entityToQuery);

        manager.flush();
        manager.getTransaction().commit();
        manager.close();
    }

    public void setupJPAEntityToUpdate() {
        EntityManager manager = entityManagerFactory.createEntityManager();
        manager.getTransaction().begin();

        entityToUpdate = new FieldAccessJPAEntity();
        entityToUpdate.setId(3L);
        manager.persist(entityToUpdate);
        manager.flush();
        manager.getTransaction().commit();
        manager.close();
    }

    @Test
    @Deployment
    public void testStoreJPAEntityAsVariable() {
        // -----------------------------------------------------------------------------
        // Simple test, Start process with JPA entities as variables
        // -----------------------------------------------------------------------------
        Map<String, Object> variables = new HashMap<>();
        variables.put("simpleEntityFieldAccess", simpleEntityFieldAccess);
        variables.put("simpleEntityPropertyAccess", simpleEntityPropertyAccess);
        variables.put("subclassFieldAccess", subclassFieldAccess);
        variables.put("subclassPropertyAccess", subclassPropertyAccess);

        // Start the process with the JPA-entities as variables. They will be
        // stored in the DB.
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("JPAVariableProcess", variables);

        // Read entity with @Id on field
        Object fieldAccessResult = runtimeService.getVariable(processInstance.getId(), "simpleEntityFieldAccess");
        assertThat(fieldAccessResult).isInstanceOf(FieldAccessJPAEntity.class);
        assertThat(((FieldAccessJPAEntity) fieldAccessResult).getId().longValue()).isEqualTo(1L);
        assertThat(((FieldAccessJPAEntity) fieldAccessResult).getValue()).isEqualTo("value1");

        // Read entity with @Id on property
        Object propertyAccessResult = runtimeService.getVariable(processInstance.getId(), "simpleEntityPropertyAccess");
        assertThat(propertyAccessResult).isInstanceOf(PropertyAccessJPAEntity.class);
        assertThat(((PropertyAccessJPAEntity) propertyAccessResult).getId().longValue()).isEqualTo(1L);
        assertThat(((PropertyAccessJPAEntity) propertyAccessResult).getValue()).isEqualTo("value2");

        // Read entity with @Id on field of mapped superclass
        Object subclassFieldResult = runtimeService.getVariable(processInstance.getId(), "subclassFieldAccess");
        assertThat(subclassFieldResult).isInstanceOf(SubclassFieldAccessJPAEntity.class);
        assertThat(((SubclassFieldAccessJPAEntity) subclassFieldResult).getId().longValue()).isEqualTo(1L);
        assertThat(((SubclassFieldAccessJPAEntity) subclassFieldResult).getValue()).isEqualTo("value3");

        // Read entity with @Id on property of mapped superclass
        Object subclassPropertyResult = runtimeService.getVariable(processInstance.getId(), "subclassPropertyAccess");
        assertThat(subclassPropertyResult).isInstanceOf(SubclassPropertyAccessJPAEntity.class);
        assertThat(((SubclassPropertyAccessJPAEntity) subclassPropertyResult).getId().longValue()).isEqualTo(1L);
        assertThat(((SubclassPropertyAccessJPAEntity) subclassPropertyResult).getValue()).isEqualTo("value4");

        // -----------------------------------------------------------------------------
        // Test updating JPA-entity to null-value and back again
        // -----------------------------------------------------------------------------
        Object currentValue = runtimeService.getVariable(processInstance.getId(), "simpleEntityFieldAccess");
        assertThat(currentValue).isNotNull();
        // Set to null
        runtimeService.setVariable(processInstance.getId(), "simpleEntityFieldAccess", null);
        currentValue = runtimeService.getVariable(processInstance.getId(), "simpleEntityFieldAccess");
        assertThat(currentValue).isNull();
        // Set to JPA-entity again
        runtimeService.setVariable(processInstance.getId(), "simpleEntityFieldAccess", simpleEntityFieldAccess);
        currentValue = runtimeService.getVariable(processInstance.getId(), "simpleEntityFieldAccess");
        assertThat(currentValue).isInstanceOf(FieldAccessJPAEntity.class);
        assertThat(((FieldAccessJPAEntity) currentValue).getId().longValue()).isEqualTo(1L);

        // -----------------------------------------------------------------------------
        // Test all allowed types of ID values
        // -----------------------------------------------------------------------------

        variables = new HashMap<>();
        variables.put("byteIdJPAEntity", byteIdJPAEntity);
        variables.put("shortIdJPAEntity", shortIdJPAEntity);
        variables.put("integerIdJPAEntity", integerIdJPAEntity);
        variables.put("longIdJPAEntity", longIdJPAEntity);
        variables.put("floatIdJPAEntity", floatIdJPAEntity);
        variables.put("doubleIdJPAEntity", doubleIdJPAEntity);
        variables.put("charIdJPAEntity", charIdJPAEntity);
        variables.put("stringIdJPAEntity", stringIdJPAEntity);
        variables.put("dateIdJPAEntity", dateIdJPAEntity);
        variables.put("sqlDateIdJPAEntity", sqlDateIdJPAEntity);
        variables.put("bigDecimalIdJPAEntity", bigDecimalIdJPAEntity);
        variables.put("bigIntegerIdJPAEntity", bigIntegerIdJPAEntity);

        // Start the process with the JPA-entities as variables. They will be
        // stored in the DB.
        ProcessInstance processInstanceAllTypes = runtimeService.startProcessInstanceByKey("JPAVariableProcess", variables);
        Object byteIdResult = runtimeService.getVariable(processInstanceAllTypes.getId(), "byteIdJPAEntity");
        assertThat(byteIdResult).isInstanceOf(ByteIdJPAEntity.class);
        assertThat(((ByteIdJPAEntity) byteIdResult).getByteId()).isEqualTo(byteIdJPAEntity.getByteId());

        Object shortIdResult = runtimeService.getVariable(processInstanceAllTypes.getId(), "shortIdJPAEntity");
        assertThat(shortIdResult).isInstanceOf(ShortIdJPAEntity.class);
        assertThat(((ShortIdJPAEntity) shortIdResult).getShortId()).isEqualTo(shortIdJPAEntity.getShortId());

        Object integerIdResult = runtimeService.getVariable(processInstanceAllTypes.getId(), "integerIdJPAEntity");
        assertThat(integerIdResult).isInstanceOf(IntegerIdJPAEntity.class);
        assertThat(((IntegerIdJPAEntity) integerIdResult).getIntId()).isEqualTo(integerIdJPAEntity.getIntId());

        Object longIdResult = runtimeService.getVariable(processInstanceAllTypes.getId(), "longIdJPAEntity");
        assertThat(longIdResult).isInstanceOf(LongIdJPAEntity.class);
        assertThat(((LongIdJPAEntity) longIdResult).getLongId()).isEqualTo(longIdJPAEntity.getLongId());

        Object floatIdResult = runtimeService.getVariable(processInstanceAllTypes.getId(), "floatIdJPAEntity");
        assertThat(floatIdResult).isInstanceOf(FloatIdJPAEntity.class);
        assertThat(((FloatIdJPAEntity) floatIdResult).getFloatId()).isEqualTo(floatIdJPAEntity.getFloatId());

        Object doubleIdResult = runtimeService.getVariable(processInstanceAllTypes.getId(), "doubleIdJPAEntity");
        assertThat(doubleIdResult).isInstanceOf(DoubleIdJPAEntity.class);
        assertThat(((DoubleIdJPAEntity) doubleIdResult).getDoubleId()).isEqualTo(doubleIdJPAEntity.getDoubleId());

        Object charIdResult = runtimeService.getVariable(processInstanceAllTypes.getId(), "charIdJPAEntity");
        assertThat(charIdResult).isInstanceOf(CharIdJPAEntity.class);
        assertThat(((CharIdJPAEntity) charIdResult).getCharId()).isEqualTo(charIdJPAEntity.getCharId());

        Object stringIdResult = runtimeService.getVariable(processInstanceAllTypes.getId(), "stringIdJPAEntity");
        assertThat(stringIdResult).isInstanceOf(StringIdJPAEntity.class);
        assertThat(((StringIdJPAEntity) stringIdResult).getStringId()).isEqualTo(stringIdJPAEntity.getStringId());

        Object dateIdResult = runtimeService.getVariable(processInstanceAllTypes.getId(), "dateIdJPAEntity");
        assertThat(dateIdResult).isInstanceOf(DateIdJPAEntity.class);
        assertThat(((DateIdJPAEntity) dateIdResult).getDateId()).isEqualTo(dateIdJPAEntity.getDateId());

        Object sqlDateIdResult = runtimeService.getVariable(processInstanceAllTypes.getId(), "sqlDateIdJPAEntity");
        assertThat(sqlDateIdResult).isInstanceOf(SQLDateIdJPAEntity.class);
        assertThat(((SQLDateIdJPAEntity) sqlDateIdResult).getDateId()).isEqualTo(sqlDateIdJPAEntity.getDateId());

        Object bigDecimalIdResult = runtimeService.getVariable(processInstanceAllTypes.getId(), "bigDecimalIdJPAEntity");
        assertThat(bigDecimalIdResult).isInstanceOf(BigDecimalIdJPAEntity.class);
        assertThat(((BigDecimalIdJPAEntity) bigDecimalIdResult).getBigDecimalId()).isEqualTo(bigDecimalIdJPAEntity.getBigDecimalId());

        Object bigIntegerIdResult = runtimeService.getVariable(processInstanceAllTypes.getId(), "bigIntegerIdJPAEntity");
        assertThat(bigIntegerIdResult).isInstanceOf(BigIntegerIdJPAEntity.class);
        assertThat(((BigIntegerIdJPAEntity) bigIntegerIdResult).getBigIntegerId()).isEqualTo(bigIntegerIdJPAEntity.getBigIntegerId());
    }

    @Test
    @Deployment(resources = { "org/flowable/standalone/jpa/JPAVariableTest.testStoreJPAEntityAsVariable.bpmn20.xml" })
    public void testStoreJPAEntityListAsVariable() {
        // -----------------------------------------------------------------------------
        // Simple test, Start process with lists of JPA entities as variables
        // -----------------------------------------------------------------------------
        Map<String, Object> variables = new HashMap<>();
        variables.put("simpleEntityFieldAccess", Arrays.asList(simpleEntityFieldAccess, simpleEntityFieldAccess, simpleEntityFieldAccess));
        variables.put("simpleEntityPropertyAccess", Arrays.asList(simpleEntityPropertyAccess, simpleEntityPropertyAccess, simpleEntityPropertyAccess));
        variables.put("subclassFieldAccess", Arrays.asList(subclassFieldAccess, subclassFieldAccess, subclassFieldAccess));
        variables.put("subclassPropertyAccess", Arrays.asList(subclassPropertyAccess, subclassPropertyAccess, subclassPropertyAccess));

        // Start the process with the JPA-entities as variables. They will be
        // stored in the DB.
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("JPAVariableProcess", variables);

        // Read entity with @Id on field
        Object fieldAccessResult = runtimeService.getVariable(processInstance.getId(), "simpleEntityFieldAccess");
        assertThat(fieldAccessResult).isInstanceOf(List.class);
        List<?> list = (List<?>) fieldAccessResult;
        assertThat(list).hasSize(3);
        assertThat(list.get(0)).isInstanceOf(FieldAccessJPAEntity.class);
        assertThat(simpleEntityFieldAccess.getId()).isEqualTo(((FieldAccessJPAEntity) list.get(0)).getId());

        // Read entity with @Id on property
        Object propertyAccessResult = runtimeService.getVariable(processInstance.getId(), "simpleEntityPropertyAccess");
        assertThat(propertyAccessResult).isInstanceOf(List.class);
        list = (List<?>) propertyAccessResult;
        assertThat(list).hasSize(3);
        assertThat(list.get(0)).isInstanceOf(PropertyAccessJPAEntity.class);
        assertThat(simpleEntityPropertyAccess.getId()).isEqualTo(((PropertyAccessJPAEntity) list.get(0)).getId());

        // Read entity with @Id on field of mapped superclass
        Object subclassFieldResult = runtimeService.getVariable(processInstance.getId(), "subclassFieldAccess");
        assertThat(subclassFieldResult).isInstanceOf(List.class);
        list = (List<?>) subclassFieldResult;
        assertThat(list).hasSize(3);
        assertThat(list.get(0)).isInstanceOf(SubclassFieldAccessJPAEntity.class);
        assertThat(simpleEntityPropertyAccess.getId()).isEqualTo(((SubclassFieldAccessJPAEntity) list.get(0)).getId());

        // Read entity with @Id on property of mapped superclass
        Object subclassPropertyResult = runtimeService.getVariable(processInstance.getId(), "subclassPropertyAccess");
        assertThat(subclassPropertyResult).isInstanceOf(List.class);
        list = (List<?>) subclassPropertyResult;
        assertThat(list).hasSize(3);
        assertThat(list.get(0)).isInstanceOf(SubclassPropertyAccessJPAEntity.class);
        assertThat(simpleEntityPropertyAccess.getId()).isEqualTo(((SubclassPropertyAccessJPAEntity) list.get(0)).getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/standalone/jpa/JPAVariableTest.testStoreJPAEntityAsVariable.bpmn20.xml" })
    public void testStoreJPAEntityListAsVariableEdgeCases() {

        // Test using mixed JPA-entities which are not serializable, should not
        // be picked up by JPA list type and therefore fail due to serialization error
        assertThatThrownBy(() ->
        {
            Map<String, Object> variables = new HashMap<>();
            variables.put("simpleEntityFieldAccess", Arrays.asList(simpleEntityFieldAccess, simpleEntityPropertyAccess));
            runtimeService.startProcessInstanceByKey("JPAVariableProcess", variables);
        })
                .isExactlyInstanceOf(FlowableException.class);

        // Test updating value to an empty list and back
        Map<String, Object> variables = new HashMap<>();
        variables.put("list", Arrays.asList(simpleEntityFieldAccess, simpleEntityFieldAccess));

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("JPAVariableProcess", variables);

        runtimeService.setVariable(processInstance.getId(), "list", new ArrayList<String>());
        assertThat(((List<?>) runtimeService.getVariable(processInstance.getId(), "list"))).isEmpty();

        runtimeService.setVariable(processInstance.getId(), "list", Arrays.asList(simpleEntityFieldAccess, simpleEntityFieldAccess));
        assertThat(((List<?>) runtimeService.getVariable(processInstance.getId(), "list"))).hasSize(2);
        assertThat(((List<?>) runtimeService.getVariable(processInstance.getId(), "list")).get(0)).isInstanceOf(FieldAccessJPAEntity.class);

        // Test updating to list of Strings
        runtimeService.setVariable(processInstance.getId(), "list", Arrays.asList("TEST", "TESTING"));
        assertThat(((List<?>) runtimeService.getVariable(processInstance.getId(), "list"))).hasSize(2);
        assertThat(((List<?>) runtimeService.getVariable(processInstance.getId(), "list")).get(0)).isInstanceOf(String.class);

        runtimeService.setVariable(processInstance.getId(), "list", Arrays.asList(simpleEntityFieldAccess, simpleEntityFieldAccess));
        assertThat(((List<?>) runtimeService.getVariable(processInstance.getId(), "list"))).hasSize(2);
        assertThat(((List<?>) runtimeService.getVariable(processInstance.getId(), "list")).get(0)).isInstanceOf(FieldAccessJPAEntity.class);

        // Test updating to null
        runtimeService.setVariable(processInstance.getId(), "list", null);
        assertThat(runtimeService.getVariable(processInstance.getId(), "list")).isNull();

        runtimeService.setVariable(processInstance.getId(), "list", Arrays.asList(simpleEntityFieldAccess, simpleEntityFieldAccess));
        assertThat(((List<?>) runtimeService.getVariable(processInstance.getId(), "list"))).hasSize(2);
        assertThat(((List<?>) runtimeService.getVariable(processInstance.getId(), "list")).get(0)).isInstanceOf(FieldAccessJPAEntity.class);
    }

    // https://activiti.atlassian.net/browse/ACT-995
    @Test
    @Deployment(resources = "org/flowable/standalone/jpa/JPAVariableTest.testQueryJPAVariable.bpmn20.xml")
    public void testReplaceExistingJPAEntityWithAnotherOfSameType() {
        EntityManager manager = entityManagerFactory.createEntityManager();
        manager.getTransaction().begin();

        // Old variable that gets replaced
        FieldAccessJPAEntity oldVariable = new FieldAccessJPAEntity();
        oldVariable.setId(11L);
        oldVariable.setValue("value1");
        manager.persist(oldVariable);

        // New variable
        FieldAccessJPAEntity newVariable = new FieldAccessJPAEntity();
        newVariable.setId(12L);
        newVariable.setValue("value2");
        manager.persist(newVariable);

        manager.flush();
        manager.getTransaction().commit();
        manager.close();

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("JPAVariableProcess");

        String executionId = processInstance.getId();
        String variableName = "testVariable";
        runtimeService.setVariable(executionId, variableName, oldVariable);

        runtimeService.setVariable(executionId, variableName, newVariable);

        Object variable = runtimeService.getVariable(executionId, variableName);
        assertThat(((FieldAccessJPAEntity) variable).getId()).isEqualTo(newVariable.getId());
    }

    @Test
    @Deployment
    public void testIllegalEntities() {
        setupIllegalJPAEntities();
        // Starting process instance with a variable that has a compound primary
        // key, which is not supported.
        assertThatThrownBy(() -> {
            Map<String, Object> variables = new HashMap<>();
            variables.put("compoundIdJPAEntity", compoundIdJPAEntity);
            runtimeService.startProcessInstanceByKey("JPAVariableProcessExceptions", variables);
        })
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessageContaining("Cannot find field or method with annotation @Id on class")
                .hasMessageContaining("only single-valued primary keys are supported on JPA-entities");

        // Starting process instance with a variable that has null as ID-value
        assertThatThrownBy(() -> {
            Map<String, Object> variables = new HashMap<>();
            variables.put("nullValueEntity", new FieldAccessJPAEntity());
            runtimeService.startProcessInstanceByKey("JPAVariableProcessExceptions", variables);
        })
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("Value of primary key for JPA-Entity cannot be null");

        // Starting process instance with an invalid type of ID
        // Under normal circumstances, JPA will throw an exception for this of
        // the class is present in the PU when creating EntityManagerFactory, but we test it
        // *just in case*
        assertThatThrownBy(() -> {
            Map<String, Object> variables = new HashMap<>();
            IllegalIdClassJPAEntity illegalIdTypeEntity = new IllegalIdClassJPAEntity();
            illegalIdTypeEntity.setId(Calendar.getInstance());
            variables.put("illegalTypeId", illegalIdTypeEntity);
            runtimeService.startProcessInstanceByKey("JPAVariableProcessExceptions", variables);
        })
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("Unsupported Primary key type for JPA-Entity");

        // Start process instance with JPA-entity which has an ID but isn't
        // persisted. When reading the variable we should get an exception.
        assertThatThrownBy(() -> {
            Map<String, Object> variables = new HashMap<>();
            FieldAccessJPAEntity nonPersistentEntity = new FieldAccessJPAEntity();
            nonPersistentEntity.setId(9999L);
            variables.put("nonPersistentEntity", nonPersistentEntity);

            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("JPAVariableProcessExceptions", variables);
            runtimeService.getVariable(processInstance.getId(), "nonPersistentEntity");
        })
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessageContaining("Entity does not exist: " + FieldAccessJPAEntity.class.getName() + " - 9999");
    }

    @Test
    @Deployment
    public void testQueryJPAVariable() {
        setupQueryJPAEntity();

        Map<String, Object> variables = new HashMap<>();
        variables.put("entityToQuery", entityToQuery);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("JPAVariableProcess", variables);

        // Query the processInstance
        ProcessInstance result = runtimeService.createProcessInstanceQuery().variableValueEquals("entityToQuery", entityToQuery).singleResult();
        assertThat(result).isNotNull();
        assertThat(processInstance.getId()).isEqualTo(result.getId());

        // Query with the same entity-type but with different ID should have no
        // result
        FieldAccessJPAEntity unexistingEntity = new FieldAccessJPAEntity();
        unexistingEntity.setId(8888L);

        result = runtimeService.createProcessInstanceQuery().variableValueEquals("entityToQuery", unexistingEntity).singleResult();
        assertThat(result).isNull();

        // All other operators are unsupported
        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().variableValueNotEquals("entityToQuery", entityToQuery).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("JPA entity variables can only be used in 'variableValueEquals'");
        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().variableValueGreaterThan("entityToQuery", entityToQuery).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("JPA entity variables can only be used in 'variableValueEquals'");
        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("entityToQuery", entityToQuery).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("JPA entity variables can only be used in 'variableValueEquals'");
        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().variableValueLessThan("entityToQuery", entityToQuery).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("JPA entity variables can only be used in 'variableValueEquals'");
        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("entityToQuery", entityToQuery).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("JPA entity variables can only be used in 'variableValueEquals'");
    }

    @Test
    @Deployment
    public void testUpdateJPAEntityValues() {
        setupJPAEntityToUpdate();
        Map<String, Object> variables = new HashMap<>();
        variables.put("entityToUpdate", entityToUpdate);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("UpdateJPAValuesProcess", variables);

        // Servicetask in process 'UpdateJPAValuesProcess' should have set value
        // on entityToUpdate.
        Object updatedEntity = runtimeService.getVariable(processInstance.getId(), "entityToUpdate");
        assertThat(updatedEntity).isInstanceOf(FieldAccessJPAEntity.class);
        assertThat(((FieldAccessJPAEntity) updatedEntity).getValue()).isEqualTo("updatedValue");
    }
}
