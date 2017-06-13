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
* ModelParseException Tester.
*/ 
public class ModelParseExceptionTest {

    private ModelParseException modelParseException;

    @Before
    public void before()
        throws Exception {}

    @After
    public void after()
        throws Exception {
        modelParseException = null;
    }

    @Test
    public void simpleCtr()
        throws Exception {
        modelParseException = new ModelParseException();
        assertThat(modelParseException.getMessage()).isNull();
        assertThat(modelParseException.getCause()).isNull();
    }

    @Test
    public void messageThrowable()
        throws Exception {
        modelParseException = new ModelParseException("message", new Throwable());
        assertThat(modelParseException.getMessage()).isEqualTo("message");
        assertThat(modelParseException.getCause()).isNotNull();
    }

    @Test
    public void message()
        throws Exception {
        modelParseException = new ModelParseException("message");
        assertThat(modelParseException.getMessage()).isEqualTo("message");
        assertThat(modelParseException.getCause()).isNull();
    }

    @Test
    public void cause()
        throws Exception {
        modelParseException = new ModelParseException(new Throwable());
        assertThat(modelParseException.getMessage()).isEqualTo("java.lang.Throwable");
        assertThat(modelParseException.getCause()).isNotNull();
    }
}
