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

import java.util.Date;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.ws.Holder;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

/**
 * A simple WS for unit test purpose
 *
 * @author Esteban Robles Luna
 */
@WebService
public interface WebServiceMock {

    /**
     * Increase the counter in 1
     */
    void inc() throws MaxValueReachedFault;

    /**
     * Add the given value to the counter
     */
    void add(@WebParam(name = "value") int value);

    /**
     * Add the given values to the counter
     */
    void addMulti(@WebParam(name = "values") int[] values);

    /**
     * Add the given values and add result to the counter
     */
    void addition(@WebParam(name = "value1") int value1, @WebParam(name = "value2") int value2);

    /**
     * Add the given values and add result to the counter
     */
    void addOperands(@WebParam(name = "args") Operands args);

    /**
     * Returns the current count
     *
     * @return the count
     */
    @WebResult(name = "count")
    int getCount();

    /**
     * Resets the counter to 0
     */
    void reset();

    /**
     * Sets the counter to value
     *
     * @param value
     *            the value of the new counter
     */
    void setTo(@WebParam(name = "value") int value);

    /**
     * Returns a formatted string composed of prefix + current count + suffix
     *
     * @param prefix
     *            the prefix
     * @param suffix
     *            the suffix
     * @return the formatted string
     */
    @WebResult(name = "prettyPrint")
    String prettyPrintCount(@WebParam(name = "prefix") String prefix, @WebParam(name = "suffix") String suffix);

    /**
     * Sets the current data structure
     *
     * @param str
     *            the new string of data structure
     * @param date
     *            the new date of data structure
     */
    void setDataStructure(@WebParam(name = "eltStr") String str, @WebParam(name = "eltDate") Date date);

    /**
     * Returns the current data structure
     *
     * @return the current data structure
     */
    @WebResult(name = "currentStructure")
    WebServiceDataStructure getDataStructure();

    @WebResult
    String noNameResult(@WebParam(name = "prefix") String prefix, @WebParam(name = "suffix") String suffix);

    @WebResult(name = "static")
    String reservedWordAsName(@WebParam(name = "prefix") String prefix, @WebParam(name = "suffix") String suffix);

    @WebMethod(action = "http://flowable.org/test/unit/returnsSeveralParams")
    @RequestWrapper(localName = "returnsSeveralParams", className = "org.flowable.engine.impl.webservice.wrappers.ReturnsSeveralParams")
    @ResponseWrapper(localName = "returnsSeveralParamsResponse", className = "org.flowable.engine.impl.webservice.wrappers.ReturnsSeveralParamsResponse")
    public void returnsSeveralParams(

            @WebParam(name = "in-param-1", targetNamespace = "") final String inParam,
            @WebParam(mode = WebParam.Mode.OUT, name = "out-param-1", targetNamespace = "") final Holder<String> outParam1,
            @WebParam(mode = WebParam.Mode.OUT, name = "out-param-2", targetNamespace = "") final Holder<Integer> outParam2,
            @WebParam(mode = WebParam.Mode.OUT, name = "out-param-3", targetNamespace = "") final Holder<String> outParam3);
}
