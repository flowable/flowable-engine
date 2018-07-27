package com.dj.flowable;

import org.flowable.engine.common.api.FlowableException;
import org.flowable.variable.api.types.ValueFields;
import org.flowable.variable.service.impl.types.SerializableType;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Alberto D.
 *         
 */
public class JsonVariableType extends SerializableType {
	
	public static final String TYPE_NAME = "dj_json";
	
    
    private static final ObjectMapper jsonMapper = new ObjectMapper();
    

    @Override
    public String getTypeName() {
        return TYPE_NAME;
    }

    public JsonVariableType() {
    	super();
    }

    public JsonVariableType(boolean trackDeserializedObjects) {
    	super(trackDeserializedObjects);
    }


    public byte[] serialize(Object value, ValueFields valueFields) {
        if (value == null) {
            return null;
        }
        try {
        	return jsonMapper.writeValueAsBytes(value);
        } catch (Exception e) {
            throw new FlowableException("Couldn't serialize value '" + value + "' in variable '" + valueFields.getName() + "'", e);
        } 
    }

    @Override
    public Object deserialize(byte[] bytes, ValueFields valueFields) {
        try {
            Object deserializedObject = jsonMapper.readValue(bytes, Object.class);
            return deserializedObject;
        } catch (Exception e) {
            throw new FlowableException("Couldn't deserialize object in variable '" + valueFields.getName() + "'", e);
        } 
    }

	
	
}