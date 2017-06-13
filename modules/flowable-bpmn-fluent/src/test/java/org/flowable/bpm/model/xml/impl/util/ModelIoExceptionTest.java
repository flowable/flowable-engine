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
package org.flowable.bpm.model.xml.impl.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** 
* ModelIoException Tester.
*/ 
public class ModelIoExceptionTest {

    private ModelIoException modelIoxception;

    @Before
    public void before()
        throws Exception {}

    @After
    public void after()
        throws Exception {
        modelIoxception = null;
    }

    @Test
    public void simpleCtr()
        throws Exception {
        modelIoxception = new ModelIoException();
        assertThat(modelIoxception.getMessage()).isNull();
        assertThat(modelIoxception.getCause()).isNull();
    }

    @Test
    public void messageThrowable()
        throws Exception {
        modelIoxception = new ModelIoException("message", new Throwable());
        assertThat(modelIoxception.getMessage()).isEqualTo("message");
        assertThat(modelIoxception.getCause()).isNotNull();
    }

    @Test
    public void message()
        throws Exception {
        modelIoxception = new ModelIoException("message");
        assertThat(modelIoxception.getMessage()).isEqualTo("message");
        assertThat(modelIoxception.getCause()).isNull();
    }

    @Test
    public void cause()
        throws Exception {
        modelIoxception = new ModelIoException(new Throwable());
        assertThat(modelIoxception.getMessage()).isEqualTo("java.lang.Throwable");
        assertThat(modelIoxception.getCause()).isNotNull();
    }
}
