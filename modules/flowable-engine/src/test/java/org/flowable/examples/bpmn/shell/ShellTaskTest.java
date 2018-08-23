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
package org.flowable.examples.bpmn.shell;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

public class ShellTaskTest extends PluggableFlowableTestCase {

    enum OsType {
        LINUX, WINDOWS, MAC, SOLARIS, UNKOWN
    }

    OsType osType;

    OsType getSystemOsType() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win"))
            return OsType.WINDOWS;
        else if (osName.contains("mac"))
            return OsType.MAC;
        else if ((osName.contains("nix")) || (osName.contains("nux")))
            return OsType.LINUX;
        else if (osName.contains("sunos"))
            return OsType.SOLARIS;
        else
            return OsType.UNKOWN;
    }

    @BeforeEach
    protected void setUp() throws Exception {
        osType = getSystemOsType();
    }

    @Test
    public void testOsDetection() throws Exception {
        assertNotSame(OsType.UNKOWN, osType);
    }

    @Test
    @Deployment
    @EnabledOnOs(OS.WINDOWS)
    public void testEchoShellWindows() {
        if (osType == OsType.WINDOWS) {

            ProcessInstance pi = runtimeService.startProcessInstanceByKey("echoShellWindows");

            String st = (String) runtimeService.getVariable(pi.getId(), "resultVar");
            assertNotNull(st);
            assertTrue(st.startsWith("EchoTest"));
        }
    }

    @Test
    @Deployment
    @EnabledOnOs(OS.LINUX)
    public void testEchoShellLinux() {
        if (osType == OsType.LINUX) {

            ProcessInstance pi = runtimeService.startProcessInstanceByKey("echoShellLinux");

            String st = (String) runtimeService.getVariable(pi.getId(), "resultVar");
            assertNotNull(st);
            assertTrue(st.startsWith("EchoTest"));
        }
    }

    @Test
    @Deployment
    @EnabledOnOs(OS.MAC)
    public void testEchoShellMac() {
        if (osType == OsType.MAC) {

            ProcessInstance pi = runtimeService.startProcessInstanceByKey("echoShellMac");

            String st = (String) runtimeService.getVariable(pi.getId(), "resultVar");
            assertNotNull(st);
            assertTrue(st.startsWith("EchoTest"));
        }
    }
}
