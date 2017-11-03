/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.flowable.variable.service.impl.types;

import groovy.json.JsonBuilder;
import groovy.json.JsonSlurper;
import groovy.json.internal.LazyMap;
import org.flowable.variable.api.types.ValueFields;
import org.flowable.variable.api.types.VariableType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ahmedghonim
 */
public class GroovyLazyMapType implements VariableType {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroovyLazyMapType.class);

    @Override
    public String getTypeName() {
        return "GroovyLazyMap";
    }

    @Override
    public boolean isCachable() {
        return false; // TODO optimise?
    }

    @Override
    public boolean isAbleToStore(Object value) {
        return value != null && value instanceof LazyMap;
    }

    @Override
    public void setValue(Object value, ValueFields valueFields) {
        valueFields.setTextValue(new JsonBuilder(value).toString());
    }

    @Override
    public Object getValue(ValueFields valueFields) {
        String str = valueFields.getTextValue();
        if (str == null) {
            return null;
        } else {
            JsonSlurper sl = new JsonSlurper();
            return sl.parseText(str);
        }
    }

}
