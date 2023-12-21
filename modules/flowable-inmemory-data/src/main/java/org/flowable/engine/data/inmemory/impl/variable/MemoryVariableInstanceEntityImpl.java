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

package org.flowable.engine.data.inmemory.impl.variable;

import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntityImpl;

/**
 * An extension of VariableInstanceEntityImpl that stores byte data as-is, without using references to ByteArray data as the memory implementation
 * does not have database level limits on what variables can and cannot be stored.
 * 
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class MemoryVariableInstanceEntityImpl extends VariableInstanceEntityImpl {

    private static final long serialVersionUID = -8915628524923165552L;

    private byte[] bytes;

    @Override
    public byte[] getBytes() {
        return bytes;
    }

    @Override
    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

}
