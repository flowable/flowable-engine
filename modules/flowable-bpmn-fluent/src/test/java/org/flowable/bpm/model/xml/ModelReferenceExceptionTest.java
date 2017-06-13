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
package org.flowable.bpm.model.xml;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** 
* ModelReferenceException Tester.
*/ 
public class ModelReferenceExceptionTest {

    private ModelReferenceException modelReferenceException;

    @Before
    public void before()
        throws Exception {}

    @After
    public void after()
        throws Exception {
        modelReferenceException = null;
    }

    @Test
    public void simpleCtr()
        throws Exception {
        modelReferenceException = new ModelReferenceException();
        assertThat(modelReferenceException.getMessage()).isNull();
        assertThat(modelReferenceException.getCause()).isNull();
    }

    @Test
    public void messageThrowable()
        throws Exception {
        modelReferenceException = new ModelReferenceException("message", new Throwable());
        assertThat(modelReferenceException.getMessage()).isEqualTo("message");
        assertThat(modelReferenceException.getCause()).isNotNull();
    }

    @Test
    public void message()
        throws Exception {
        modelReferenceException = new ModelReferenceException("message");
        assertThat(modelReferenceException.getMessage()).isEqualTo("message");
        assertThat(modelReferenceException.getCause()).isNull();
    }

    @Test
    public void cause()
        throws Exception {
        modelReferenceException = new ModelReferenceException(new Throwable());
        assertThat(modelReferenceException.getMessage()).isEqualTo("java.lang.Throwable");
        assertThat(modelReferenceException.getCause()).isNotNull();
    }
}
