package org.flowable.variable.service.impl.types;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import javax.xml.bind.DatatypeConverter;

import org.flowable.variable.api.types.ValueFields;
import org.flowable.variable.api.types.VariableType;

public class ListType implements VariableType {

	@Override
	public String getTypeName() {
		return "ArrayList<Object>";
	}

	@Override
	public boolean isCachable() {
		return false;
	}

	@Override
	public boolean isAbleToStore(Object value) {
		return value == null || value instanceof ArrayList<?>;
	}

	@Override
	public void setValue(Object o, ValueFields valueFields) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream out;
		byte[] data;
		try {
			out = new ObjectOutputStream(bos);
			out.writeObject(o);
			data = bos.toString().getBytes("UTF-8");
			String encodedData = DatatypeConverter.printBase64Binary(data);
			valueFields.setTextValue(encodedData);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public Object getValue(ValueFields valueFields) {
		String str = valueFields.getTextValue();
		if (str == null) {
			return null;
		} else {
			byte[] decodedData = DatatypeConverter.parseBase64Binary(str);
			ByteArrayInputStream bis = new ByteArrayInputStream(decodedData);
			ObjectInputStream ois;
			try {
				ois = new ObjectInputStream(bis);
				return (ArrayList<?>) ois.readObject();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
			
		}

	}

}
