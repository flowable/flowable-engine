/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.common.engine.impl.el.parser;

/*
 * Bean class used to evaluate EL expressions with two or more complex types. Provides 10 methods, plus those on the
 * superclass.
 */
public class TesterBeanE extends TesterBeanD {

    private String four;
    private String one;
    private String three;
    private String two;
    private String five;

    public String getFive() {
        return five;
    }

    public void setFive(String five) {
        this.five = five;
    }

    public String getFour() {
        return four;
    }

    public String getOne() {
        return one;
    }

    public String getThree() {
        return three;
    }

    public String getTwo() {
        return two;
    }

    public void setFour(String four) {
        this.four = four;
    }

    public void setOne(String one) {
        this.one = one;
    }

    public void setThree(String three) {
        this.three = three;
    }

    public void setTwo(String two) {
        this.two = two;
    }
}
