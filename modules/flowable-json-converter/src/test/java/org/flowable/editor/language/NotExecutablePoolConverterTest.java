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
package org.flowable.editor.language;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Pool;
import org.flowable.bpmn.model.Process;
import org.junit.Test;

public class NotExecutablePoolConverterTest extends AbstractConverterTest {

    @Test
    public void convertJsonToModel() throws Exception {
        BpmnModel bpmnModel = readJsonFile();
        validateModel(bpmnModel);
    }

    @Test
    public void doubleConversionValidation() throws Exception {
        BpmnModel bpmnModel = readJsonFile();
        bpmnModel = convertToJsonAndBack(bpmnModel);
        validateModel(bpmnModel);
    }

    @Override
    protected String getResource() {
        return "test.notexecutablepoolmodel.json";
    }

    private void validateModel(BpmnModel model) {

        String idPool = "idPool";
        String idProcess = "poolProcess";

        assertEquals(1, model.getPools().size());

        Pool pool = model.getPool(idPool);
        assertEquals(idPool, pool.getId());
        assertEquals(idProcess, pool.getProcessRef());
        assertFalse(pool.isExecutable());

        Process process = model.getProcess(idPool);
        assertEquals(idProcess, process.getId());
        assertFalse(process.isExecutable());
        assertEquals(3, process.getLanes().size());

    }
}
