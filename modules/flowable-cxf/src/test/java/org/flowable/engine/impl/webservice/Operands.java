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
package org.flowable.engine.impl.webservice;

public class Operands {

    private int arg1 = 0;

    private int arg2 = 0;

    public Operands() {
    }

    public Operands(final int arg1, final int arg2) {
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    public int getArg1() {
        return this.arg1;
    }

    public void setArg1(final int arg1) {
        this.arg1 = arg1;
    }

    public int getArg2() {
        return this.arg2;
    }

    public void setArg2(final int arg2) {
        this.arg2 = arg2;
    }

}
