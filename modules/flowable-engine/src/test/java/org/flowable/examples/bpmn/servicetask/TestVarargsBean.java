package org.flowable.examples.bpmn.servicetask;

import java.io.Serializable;

public class TestVarargsBean implements Serializable {

    public String myMethod(String...strings) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String s : strings) {
            stringBuilder.append(s);
        }
        return stringBuilder.toString();
    }

}
