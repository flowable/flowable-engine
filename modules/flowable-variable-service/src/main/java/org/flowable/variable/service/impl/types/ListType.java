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
		byte[] data;
		try {
			ObjectOutputStream out = new ObjectOutputStream(bos);
			out.writeObject(o);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			data = bos.toString().getBytes("UTF-8");
			String encodedData = DatatypeConverter.printBase64Binary(data);
			valueFields.setTextValue(encodedData);
		} catch (UnsupportedEncodingException e) {
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
			ArrayList<Object> list = new ArrayList<Object>();
			ByteArrayInputStream bis = new ByteArrayInputStream(decodedData);
			try {
				ObjectInputStream ois = new ObjectInputStream(bis);
				list = (ArrayList) ois.readObject();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			return list;
		}

	}

}
