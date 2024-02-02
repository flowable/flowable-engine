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

package org.flowable.spring.test.autodeployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.InputStream;

import org.flowable.cmmn.spring.autodeployment.SingleResourceAutoDeploymentStrategy;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.Resource;

/**
 * @author Tiese Barrell
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class SingleResourceAutoDeploymentStrategyTest extends AbstractAutoDeploymentStrategyTest {

    private SingleResourceAutoDeploymentStrategy classUnderTest;

    @Before
    @Override
    public void before() throws Exception {
        super.before();
        classUnderTest = new SingleResourceAutoDeploymentStrategy();
        assertThat(classUnderTest).isNotNull();
    }

    @Test
    public void testHandlesMode() {
        assertThat(classUnderTest.handlesMode(SingleResourceAutoDeploymentStrategy.DEPLOYMENT_MODE)).isTrue();
        assertThat(classUnderTest.handlesMode("other-mode")).isFalse();
        assertThat(classUnderTest.handlesMode(null)).isFalse();
    }

    @Test
    public void testDeployResources() {
        final Resource[] resources = new Resource[] { resourceMock1, resourceMock2, resourceMock3 };
        classUnderTest.deployResources(deploymentNameHint, resources, cmmnEngineMock);

        verify(repositoryServiceMock, times(3)).createDeployment();
        verify(deploymentBuilderMock, times(1)).name(resourceName1);
        verify(deploymentBuilderMock, times(1)).name(resourceName2);
        verify(deploymentBuilderMock, times(1)).name(resourceName3);
        verify(deploymentBuilderMock, times(1)).addInputStream(eq(resourceName1), isA(InputStream.class));
        verify(deploymentBuilderMock, times(1)).addInputStream(eq(resourceName2), isA(InputStream.class));
        verify(deploymentBuilderMock, times(3)).deploy();
    }

    @Test
    public void testDeployResourcesNoResources() {
        final Resource[] resources = new Resource[] {};
        classUnderTest.deployResources(deploymentNameHint, resources, cmmnEngineMock);

        verify(repositoryServiceMock, never()).createDeployment();
        verify(deploymentBuilderMock, never()).name(deploymentNameHint);
        verify(deploymentBuilderMock, never()).addInputStream(isA(String.class), isA(InputStream.class));
        verify(deploymentBuilderMock, never()).addInputStream(eq(resourceName2), isA(InputStream.class));
        verify(deploymentBuilderMock, never()).deploy();
    }

}
