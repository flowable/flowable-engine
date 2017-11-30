package org.flowable.variable.service.impl.types;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.xml.bind.DatatypeConverter;

import org.flowable.variable.api.types.ValueFields;
import org.flowable.variable.api.types.VariableType;
import org.flowable.variable.service.impl.persistence.entity.AdvancedList;

public class AdvancedListType implements VariableType{

	@Override
	public String getTypeName() {
		return "AdvancedList<T>";
	}

	@Override
	public boolean isCachable() {
		return false;
	}

	@Override
	public boolean isAbleToStore(Object value) {
		return value == null || value instanceof AdvancedList<?>;
	}

	@Override
	public void setValue(Object value, ValueFields valueFields) {
		ByteArrayOutputStream byteArrayOutputStream;
		ObjectOutputStream objectOutputStream;
		byte[] data;
		String encodedData;
		try {
			byteArrayOutputStream = new ByteArrayOutputStream();
			objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
			objectOutputStream.writeObject(value);
			
			data = byteArrayOutputStream.toString().getBytes("UTF-8");
			encodedData = DatatypeConverter.printBase64Binary(data);
			valueFields.setTextValue(encodedData);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public Object getValue(ValueFields valueFields) {
		String str = valueFields.getTextValue();
		if(str == null)
			return null;
		else {
			byte[] decodedData = DatatypeConverter.parseBase64Binary(str);
			ByteArrayInputStream byteArrayInputStream= new ByteArrayInputStream(decodedData);
			ObjectInputStream objectInputStream = null;
			AdvancedList<?> advancedList = null;
			try {
				objectInputStream = new ObjectInputStream(byteArrayInputStream);
				advancedList = (AdvancedList<?>)objectInputStream.readObject();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return advancedList;
		}
	}

}
