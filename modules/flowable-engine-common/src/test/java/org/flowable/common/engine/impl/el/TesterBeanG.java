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

/**
 * Test cases based on https://bz.apache.org/bugzilla/show_bug.cgi?id=65358 BeanG is BeanF with all the methods that use
 * Enum parameters removed so that the EL caller always has to coerce the Enum to String.
 */
public class TesterBeanG {

    @SuppressWarnings("unused")
    public String doTest(String param1) {
        return "String";
    }


    @SuppressWarnings("unused")
    public String doTest(String param1, String param2) {
        return "String-String";
    }


    @SuppressWarnings("unused")
    public String doTest(String param1, String... param2) {
        return "String-VString";
    }
}
