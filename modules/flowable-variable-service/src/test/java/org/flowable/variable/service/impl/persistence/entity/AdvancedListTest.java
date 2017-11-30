package org.flowable.variable.service.impl.persistence.entity;

import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;

public class AdvancedListTest {

	@Test
	public void containsStringShouldReturnTrueIfValueEqualsTrue() {
		AdvancedList<SealMetadata> list = new AdvancedList<>();
		
		SealMetadata obj1 = new SealMetadata();
		obj1.setType("string");
		obj1.setValue("Ahmed");
		obj1.setOrigin("BPM");
		
		list.append(obj1);
		
		assertTrue(list.containsString("Ahmed"));
		
	}
	
	@Test
	public void containsNumberShouldReturnTrueIfTheNumberFound() {
		AdvancedList<SealMetadata> list = new AdvancedList<>();
		
		SealMetadata obj1 = new SealMetadata();
		obj1.setType("string");
		obj1.setValue("Ahmed");
		obj1.setOrigin("BPM");
		
		list.append(obj1);
		
		SealMetadata obj2 = new SealMetadata();
		obj2.setType("string");
		obj2.setValue("2");
		obj2.setOrigin("BPM");
		
		list.append(obj2);
		
		assertTrue(list.containsNumber(2.0));
	}
	
	@Test
	public void containsNumberNotEqualsShouldReturnTrueIfTheNumberNOTFound() {
		AdvancedList<SealMetadata> list = new AdvancedList<>();
		
		SealMetadata obj1 = new SealMetadata();
		obj1.setType("string");
		obj1.setValue("Ahmed");
		obj1.setOrigin("BPM");
		
		list.append(obj1);
		
		SealMetadata obj2 = new SealMetadata();
		obj2.setType("string");
		obj2.setValue("2");
		obj2.setOrigin("BPM");
		
		list.append(obj2);
		assertTrue(list.containsNumberNotEquals(3.0));
	}
	
	@Test
	public void containsDateEqualsShouldReturnTrueIfListContainsEquivalentDate() {
		
		AdvancedList<SealMetadata> list = new AdvancedList<>();

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		SealMetadata obj1 = new SealMetadata();
		obj1.setType("string");
		obj1.setValue("Ahmed");
		obj1.setOrigin("BPM");
		
		list.append(obj1);
		
		SealMetadata obj2 = new SealMetadata();
		obj2.setType("string");
		obj2.setValue("2");
		obj2.setOrigin("BPM");
		
		list.append(obj2);
		
		SealMetadata obj3 = new SealMetadata();
		obj3.setValue(simpleDateFormat.format(new Date()));
		obj3.setOrigin("BPM");
		
		list.add(obj3);
		
		assertTrue(list.containsDateEquals(new Date()));
	}
	
	@Test
	public void containsDateEqualsShouldReturnTrueIfListDoesnotContainEquivalentDates() {
		
		AdvancedList<SealMetadata> list = new AdvancedList<>();

		SealMetadata obj1 = new SealMetadata();
		obj1.setType("string");
		obj1.setValue("Ahmed");
		obj1.setOrigin("BPM");
		
		list.append(obj1);
		
		SealMetadata obj2 = new SealMetadata();
		obj2.setType("string");
		obj2.setValue("2");
		obj2.setOrigin("BPM");
		
		list.append(obj2);
		
		SealMetadata obj3 = new SealMetadata();
		obj3.setValue("2017-11-29");
		obj3.setOrigin("BPM");
		
		list.add(obj3);
		
		assertTrue(list.containsDateNotEquals(new Date()));
		
	}
	
	@Test
	public void containsDateLessThanShouldReturnTrueIfListContainsDateBeforeGivenDate() {
		
		AdvancedList<SealMetadata> list = new AdvancedList<>();

		SealMetadata obj1 = new SealMetadata();
		obj1.setType("string");
		obj1.setValue("Ahmed");
		obj1.setOrigin("BPM");
		
		list.append(obj1);
		
		SealMetadata obj2 = new SealMetadata();
		obj2.setType("string");
		obj2.setValue("2");
		obj2.setOrigin("BPM");
		
		list.append(obj2);
		
		SealMetadata obj3 = new SealMetadata();
		obj3.setValue("2017-11-29");
		obj3.setOrigin("BPM");
		
		list.add(obj3);
		
		assertTrue(list.containsDateLessThan(new Date()));
		
	}
	
	@Test
	public void containsDateGreaterThanShouldReturnTrueIfListContainsDateAfterGivenDate() {
		
		AdvancedList<SealMetadata> list = new AdvancedList<>();

		SealMetadata obj1 = new SealMetadata();
		obj1.setType("string");
		obj1.setValue("Ahmed");
		obj1.setOrigin("BPM");
		
		list.append(obj1);
		
		SealMetadata obj2 = new SealMetadata();
		obj2.setType("string");
		obj2.setValue("2");
		obj2.setOrigin("BPM");
		
		list.append(obj2);
		
		SealMetadata obj3 = new SealMetadata();
		obj3.setValue("2019-11-29");
		obj3.setOrigin("BPM");
		
		list.add(obj3);
		
		assertTrue(list.containsDateGreaterThan(new Date()));
		
	}
	
	@Test
	public void containsDateLessThanOrEqualsShouldReturnTrueIfListContainsDateBeforeOrLessGivenDate() {
		
		AdvancedList<SealMetadata> list = new AdvancedList<>();

		SealMetadata obj1 = new SealMetadata();
		obj1.setType("string");
		obj1.setValue("Ahmed");
		obj1.setOrigin("BPM");
		
		list.append(obj1);
		
		SealMetadata obj2 = new SealMetadata();
		obj2.setType("string");
		obj2.setValue("2");
		obj2.setOrigin("BPM");
		
		list.append(obj2);
		
		SealMetadata obj3 = new SealMetadata();
		obj3.setValue("2017-11-29");
		obj3.setOrigin("BPM");
		
		list.add(obj3);
		
		assertTrue(list.containsDateLessThanOrEquals(new Date()));
		
	}
	
	@Test
	public void containsDateGreaterThanOrEqualsShouldReturnTrueIfListContainsDateAfterOrGreaterThanGivenDate() {
		
		AdvancedList<SealMetadata> list = new AdvancedList<>();

		SealMetadata obj1 = new SealMetadata();
		obj1.setType("string");
		obj1.setValue("Ahmed");
		obj1.setOrigin("BPM");
		
		list.append(obj1);
		
		SealMetadata obj2 = new SealMetadata();
		obj2.setType("string");
		obj2.setValue("2");
		obj2.setOrigin("BPM");
		
		list.append(obj2);
		
		SealMetadata obj3 = new SealMetadata();
		obj3.setValue("2019-11-29");
		obj3.setOrigin("BPM");
		
		list.append(obj3);
		
		assertTrue(list.containsDateGreaterThanOrEquals(new Date()));
		
	}
	

}
