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
package org.flowable.cmmn.rest.service.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.common.rest.variable.EngineRestVariable;
import org.flowable.common.rest.variable.RestVariableConverter;
import org.flowable.variable.service.impl.types.JPAEntityListVariableType;
import org.flowable.variable.service.impl.types.JPAEntityVariableType;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import tools.jackson.databind.node.ObjectNode;

/**
 * The REST type of a variable whose value cannot be resolved is determined from the variable type of the engine. It must be the same type as
 * the type of a variable whose value can be resolved, which is determined from the value.
 */
public class RestVariableTypeNameTest extends BaseSpringRestTestCase {

    @Autowired
    protected CmmnRestResponseFactory restResponseFactory;

    @Test
    public void restVariableTypeNameOfVariableTypes() {
        assertRestVariableTypeName("Kermit", "string", "string");
        assertRestVariableTypeName("a".repeat(5000), "longString", "string");
        assertRestVariableTypeName(1, "integer", "integer");
        assertRestVariableTypeName(1L, "long", "long");
        assertRestVariableTypeName((short) 1, "short", "short");
        assertRestVariableTypeName(1.5d, "double", "double");
        assertRestVariableTypeName(true, "boolean", "boolean");
        assertRestVariableTypeName(new Date(), "date", "date");
        assertRestVariableTypeName(Instant.now(), "instant", "instant");
        assertRestVariableTypeName(LocalDate.now(), "localdate", "localDate");
        assertRestVariableTypeName(LocalDateTime.now(), "localdatetime", "localDateTime");
        assertRestVariableTypeName(UUID.randomUUID(), "uuid", "uuid");
        assertRestVariableTypeName(new BigDecimal("1.5"), "bigdecimal", "bigDecimal");
        assertRestVariableTypeName(new BigInteger("15"), "biginteger", "bigInteger");
        assertRestVariableTypeName(objectMapper.createObjectNode().put("name", "Kermit"), "json", "json");
        assertRestVariableTypeName("Kermit".getBytes(StandardCharsets.UTF_8), "bytes", "binary");
        assertRestVariableTypeName(new TestSerializableVariable(), "serializable", "serializable");
        assertRestVariableTypeName(new org.joda.time.LocalDate(), "jodadate", "serializable");
        assertRestVariableTypeName(new DateTime(), "jodadatetime", "serializable");
    }

    @Test
    public void restVariableTypeNameOfLongJsonVariableType() {
        // The long json type is kept for existing variables, a new json value that is too long for a text value is stored in a byte array
        assertThat(cmmnEngineConfiguration.getVariableTypes().getVariableType("longJson")).isNotNull();
        assertThat(restResponseFactory.getRestVariableTypeName("longJson")).isEqualTo("json");
    }

    @Test
    public void restVariableTypeNameOfEmptyCollectionVariableType() {
        // An empty collection has no converter
        assertThat(restResponseFactory.getRestVariableTypeName("emptyCollection")).isEqualTo("serializable");
        assertThat(createRestVariableType(new ArrayList<>())).isEqualTo("serializable");
    }

    @Test
    public void restVariableTypeNameOfJpaEntityVariableTypes() {
        // The JPA variable types are only registered when JPA is configured, a JPA entity value has no converter
        assertThat(restResponseFactory.getRestVariableTypeName(JPAEntityVariableType.TYPE_NAME)).isEqualTo("serializable");
        assertThat(createRestVariableType(new TestEntity())).isEqualTo("serializable");

        assertThat(restResponseFactory.getRestVariableTypeName(JPAEntityListVariableType.TYPE_NAME)).isEqualTo("serializable");
        assertThat(createRestVariableType(Collections.singletonList(new TestEntity()))).isEqualTo("serializable");
    }

    @Test
    public void restVariableTypeNameOfCustomVariableType() {
        assertThat(restResponseFactory.getRestVariableTypeName("customType")).isEqualTo("serializable");

        CmmnRestResponseFactory customRestResponseFactory = new CmmnRestResponseFactory(objectMapper);
        customRestResponseFactory.getVariableConverters().add(new CustomRestVariableConverter());
        assertThat(customRestResponseFactory.getRestVariableTypeName("customType")).isEqualTo("custom");
    }

    @Test
    public void restVariableTypeNameWithoutVariableType() {
        assertThat(restResponseFactory.getRestVariableTypeName(null)).isNull();
    }

    protected void assertRestVariableTypeName(Object value, String expectedVariableTypeName, String expectedRestVariableTypeName) {
        assertThat(cmmnEngineConfiguration.getVariableTypes().findVariableType(value).getTypeName()).isEqualTo(expectedVariableTypeName);
        assertThat(createRestVariableType(value)).isEqualTo(expectedRestVariableTypeName);
        assertThat(restResponseFactory.getRestVariableTypeName(expectedVariableTypeName)).isEqualTo(expectedRestVariableTypeName);
    }

    protected String createRestVariableType(Object value) {
        return restResponseFactory.createRestVariable("variable", value, null, "id", CmmnRestResponseFactory.VARIABLE_CASE, false).getType();
    }

    public static class TestSerializableVariable implements Serializable {

        private static final long serialVersionUID = 1L;
    }

    // Stands in for a JPA entity (JPA is not on the classpath of this module), the value of an entity has no converter
    public static class TestEntity {

        protected String id;
    }

    protected static class CustomRestVariableConverter implements RestVariableConverter {

        @Override
        public String getRestTypeName() {
            return "custom";
        }

        @Override
        public boolean isVariableTypeSupported(String variableTypeName) {
            return "customType".equals(variableTypeName);
        }

        @Override
        public Class<?> getVariableType() {
            return ObjectNode.class;
        }

        @Override
        public Object getVariableValue(EngineRestVariable result) {
            return result.getValue();
        }

        @Override
        public void convertVariableValue(Object variableValue, EngineRestVariable result) {
            result.setValue(variableValue);
        }
    }
}
