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

import org.flowable.dmn.spring.autodeployment.DefaultAutoDeploymentStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.Resource;

/**
 * @author Tiese Barrell
 */
@MockitoSettings(strictness = Strictness.LENIENT)
public class DefaultAutoDeploymentStrategyTest extends AbstractAutoDeploymentStrategyTest {

    private DefaultAutoDeploymentStrategy classUnderTest;

    @BeforeEach
    @Override
    public void before() throws Exception {
        super.before();
        classUnderTest = new DefaultAutoDeploymentStrategy();
        assertThat(classUnderTest).isNotNull();
    }

    @Test
    public void testHandlesMode() {
        assertThat(classUnderTest.handlesMode(DefaultAutoDeploymentStrategy.DEPLOYMENT_MODE)).isTrue();
        assertThat(classUnderTest.handlesMode("other-mode")).isFalse();
        assertThat(classUnderTest.handlesMode(null)).isFalse();
    }

    @Test
    public void testDeployResources() {
        final Resource[] resources = new Resource[] { resourceMock1, resourceMock2 };
        classUnderTest.deployResources(deploymentNameHint, resources, dmnEngineMock);

        verify(repositoryServiceMock, times(1)).createDeployment();
        verify(deploymentBuilderMock, times(1)).enableDuplicateFiltering();
        verify(deploymentBuilderMock, times(1)).name(deploymentNameHint);
        verify(deploymentBuilderMock, times(1)).addInputStream(eq(resourceName1), isA(InputStream.class));
        verify(deploymentBuilderMock, times(1)).addInputStream(eq(resourceName2), isA(InputStream.class));
        verify(deploymentBuilderMock, times(1)).deploy();
    }

    @Test
    public void testDeployResourcesNoResources() {
        final Resource[] resources = new Resource[] {};
        classUnderTest.deployResources(deploymentNameHint, resources, dmnEngineMock);

        verify(repositoryServiceMock, times(1)).createDeployment();
        verify(deploymentBuilderMock, times(1)).enableDuplicateFiltering();
        verify(deploymentBuilderMock, times(1)).name(deploymentNameHint);
        verify(deploymentBuilderMock, never()).addInputStream(isA(String.class), isA(InputStream.class));
        verify(deploymentBuilderMock, never()).addInputStream(eq(resourceName2), isA(InputStream.class));
        verify(deploymentBuilderMock, times(1)).deploy();
    }

}
