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

package org.flowable.management.jmx;

import static org.assertj.core.api.Assertions.assertThat;

import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanInfo;

import org.flowable.management.jmx.testMbeans.BadAttributeGetterHavinParameter;
import org.flowable.management.jmx.testMbeans.BadAttributeGetterNameNotCapital;
import org.flowable.management.jmx.testMbeans.BadAttributeNameNoGetterSetter;
import org.flowable.management.jmx.testMbeans.BadAttributeSetterHavinReturn;
import org.flowable.management.jmx.testMbeans.BadAttributeSetterNameNotCapital;
import org.flowable.management.jmx.testMbeans.BadAttributeVoid;
import org.flowable.management.jmx.testMbeans.BadInherited;
import org.flowable.management.jmx.testMbeans.NotManagedMBean;
import org.flowable.management.jmx.testMbeans.TestMbean;
import org.junit.Test;

/**
 * @author Saeid Mirzaei
 */

public class MBeanInfoAssemblerTest {

    protected TestMbean testMbean = new TestMbean();
    protected MBeanInfoAssembler mbeanInfoAssembler = new MBeanInfoAssembler();

    @Test
    public void testNullInputs() throws JMException {

        // at least one of the first parameters should be not null
        assertThat(mbeanInfoAssembler.getMBeanInfo(null, null, "")).isNull();

        // mbean should be not null
        assertThat(mbeanInfoAssembler.getMBeanInfo(testMbean, testMbean, null)).isNull();

        // it should return something if at least one of the first parameters
        // are
        // not null
        NotManagedMBean notManagedMbean = new NotManagedMBean();
        assertThat(mbeanInfoAssembler.getMBeanInfo(null, notManagedMbean, "someName")).isNotNull();
        assertThat(mbeanInfoAssembler.getMBeanInfo(notManagedMbean, null, "someName")).isNotNull();

    }

    @Test
    public void testReadAttributeInfoHappyPath() throws JMException {
        ModelMBeanInfo beanInfo = mbeanInfoAssembler.getMBeanInfo(testMbean, null, "someName");
        assertThat(beanInfo).isNotNull();

        assertThat(beanInfo.getDescription()).isEqualTo("test description");
        MBeanAttributeInfo[] testAttributes = beanInfo.getAttributes();
        assertThat(testAttributes).hasSize(2);

        int counter = 0;
        for (MBeanAttributeInfo info : testAttributes) {
            if ("TestAttributeBoolean".equals(info.getName())) {
                counter++;
                assertThat(info.getDescription()).isEqualTo("test attribute Boolean description");
                assertThat(info.getType()).isEqualTo("java.lang.Boolean");
                assertThat(info.isReadable()).isTrue();
                assertThat(info.isWritable()).isFalse();
            } else if ("TestAttributeString".equals(info.getName())) {
                counter++;
                assertThat(info.getDescription()).isEqualTo("test attribute String description");
                assertThat(info.getType()).isEqualTo("java.lang.String");
                assertThat(info.isReadable()).isTrue();
                assertThat(info.isWritable()).isFalse();
            }
        }
        assertThat(counter).isEqualTo(2);

        // check the single operation

        assertThat(beanInfo.getOperations()).hasSize(3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAttributeGetterNameNotCapitial() throws JMException {
        mbeanInfoAssembler.getMBeanInfo(new BadAttributeGetterNameNotCapital(), null, "someName");

    }

    @Test(expected = IllegalArgumentException.class)
    public void testAttributePOJONamingNoGetter() throws JMException {
        mbeanInfoAssembler.getMBeanInfo(new BadAttributeNameNoGetterSetter(), null, "someName");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAttributeSetterNameNotCapitial() throws JMException {
        mbeanInfoAssembler.getMBeanInfo(new BadAttributeSetterNameNotCapital(), null, "someName");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAttributeHavingParameter() throws JMException {
        mbeanInfoAssembler.getMBeanInfo(new BadAttributeGetterHavinParameter(), null, "someName");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAttributeSetterHavingResult() throws JMException {
        mbeanInfoAssembler.getMBeanInfo(new BadAttributeSetterHavinReturn(), null, "someName");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAttributeVoid() throws JMException {
        mbeanInfoAssembler.getMBeanInfo(new BadAttributeVoid(), null, "someName");
    }

    @Test
    public void testInherited() throws JMException {
        ModelMBeanInfo beanInfo = mbeanInfoAssembler.getMBeanInfo(new BadInherited(), null, "someName");
        assertThat(beanInfo).isNotNull();
        assertThat(beanInfo.getAttributes()).hasSize(2);
        assertThat(beanInfo.getOperations()).hasSize(3);

    }

}
