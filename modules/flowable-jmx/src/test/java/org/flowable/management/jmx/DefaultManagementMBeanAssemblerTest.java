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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.modelmbean.ModelMBean;

import org.flowable.management.jmx.annotations.NotificationSenderAware;
import org.flowable.management.jmx.testMbeans.TestMbean;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Saeid Mirzaei
 */

public class DefaultManagementMBeanAssemblerTest {

    DefaultManagementMBeanAssembler defaultManagementMBeanAssembler = new DefaultManagementMBeanAssembler();

    @Test
    public void testHappyPath() throws MalformedObjectNameException, JMException {
        TestMbean testMbean = new TestMbean();
        ModelMBean mbean = defaultManagementMBeanAssembler.assemble(testMbean, new ObjectName("org.flowable.jmx.Mbeans:type=something"));
        assertThat(mbean).isNotNull();
        assertThat(mbean.getMBeanInfo()).isNotNull();
        assertThat(mbean.getMBeanInfo().getAttributes()).isNotNull();
        MBeanAttributeInfo[] attributes = mbean.getMBeanInfo().getAttributes();
        assertThat(attributes).hasSize(2);
        assertThat(("TestAttributeString".equals(attributes[0].getName()) && "TestAttributeBoolean".equals(attributes[1].getName()) || (
		        "TestAttributeString".equals(attributes[1].getName()) && "TestAttributeBoolean".equals(attributes[0]
		                .getName())))).isTrue();
        assertThat(mbean.getMBeanInfo().getOperations()).isNotNull();
        MBeanOperationInfo[] operations = mbean.getMBeanInfo().getOperations();
        assertThat(operations).hasSize(3);

    }

    @Test
    public void testNotificationAware() throws MalformedObjectNameException, JMException {
        NotificationSenderAware mockedNotificationAwareMbean = mock(NotificationSenderAware.class);
        ModelMBean modelBean = defaultManagementMBeanAssembler.assemble(mockedNotificationAwareMbean, new ObjectName("org.flowable.jmx.Mbeans:type=something"));
        assertThat(modelBean).isNotNull();
        ArgumentCaptor<NotificationSender> argument = ArgumentCaptor.forClass(NotificationSender.class);
        verify(mockedNotificationAwareMbean).setNotificationSender(argument.capture());
        assertThat(argument).isNotNull();
        assertThat(argument.getValue()).isNotNull();

    }

}
