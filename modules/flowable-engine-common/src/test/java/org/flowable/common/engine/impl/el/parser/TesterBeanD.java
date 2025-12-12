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

import java.util.List;
import java.util.Map;

/*
 * Bean class used to evaluate EL expressions with two or more complex types.
 * Provides 10 methods.
 */
public class TesterBeanD {

    private TesterBeanE bean;
    private String name;
    private Map<String,String> stringMap;
    private List<?> valList;
    private long valLong;

    public TesterBeanE getBean() {
        return bean;
    }

    public String getName() {
        return name;
    }

    public Map<String,String> getStringMap() {
        return stringMap;
    }

    public List<?> getValList() {
        return valList;
    }

    public long getValLong() {
        return valLong;
    }

    public void setBean(TesterBeanE bean) {
        this.bean = bean;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStringMap(Map<String,String> stringMap) {
        this.stringMap = stringMap;
    }

    public void setValList(List<?> valList) {
        this.valList = valList;
    }

    public void setValLong(long valLong) {
        this.valLong = valLong;
    }
}
