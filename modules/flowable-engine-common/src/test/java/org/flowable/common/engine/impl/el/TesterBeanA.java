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
package org.flowable.common.engine.impl.el;

import java.util.List;

public class TesterBeanA {
    private TesterBeanB bean;
    private String name;
    private long valLong;
    private List<?> valList;
    private Object[] valArray;

    public TesterBeanB getBean() {
        return bean;
    }

    public void setBean(TesterBeanB bean) {
        this.bean = bean;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getValLong() {
        return valLong;
    }

    public void setValLong(long valLong) {
        this.valLong = valLong;
    }

    public List<?> getValList() {
        return valList;
    }

    public void setValList(List<?> valList) {
        this.valList = valList;
    }

    public Object[] getValArray() {
        return valArray;
    }

    public void setValArray(Object[] valArray) {
        this.valArray = valArray;
    }

    public CharSequence echo1(CharSequence cs) {
        return "A1" + cs;
    }

    public CharSequence echo2(String s) {
        return "A2" + s;
    }
}
