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
package org.flowable.engine.impl.webservice.wrappers;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "outParam1", "outParam2", "outParam3"
})
@XmlRootElement(name = "returnsSeveralParamsResponse")
public class ReturnsSeveralParamsResponse {

    @XmlElement(name = "out-param-1")
    protected String outParam1;

    @XmlElement(name = "out-param-2")
    protected Integer outParam2;

    @XmlElement(name = "out-param-3")
    protected String outParam3;

    public String getOutParam1() {
        return outParam1;
    }

    public void setOutParam1(String value) {
        this.outParam1 = value;
    }

    public Integer getOutParam2() {
        return outParam2;
    }

    public void setOutParam2(Integer value) {
        this.outParam2 = value;
    }

    public String getOutParam3() {
        return outParam3;
    }

    public void setOutParam3(String value) {
        this.outParam3 = value;
    }

}
