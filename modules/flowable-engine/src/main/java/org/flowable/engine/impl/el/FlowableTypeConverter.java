package org.flowable.engine.impl.el;

import java.util.Date;

import org.flowable.engine.ProcessEngineConfiguration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import de.odysseus.el.misc.TypeConverterImpl;

public class FlowableTypeConverter extends TypeConverterImpl {

  private static final long serialVersionUID = 1L;
  
  protected ProcessEngineConfiguration processEngineConfiguration;
  
  public FlowableTypeConverter(ProcessEngineConfiguration processEngineConfiguration) {
    this.processEngineConfiguration = processEngineConfiguration;
  }

  protected String coerceToString(Object value) {
    String coercedValue = null;
    if (value instanceof Date) {
      Date date = (Date) value;
      DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
      DateTimeZone dateTimeZone = DateTimeZone.forTimeZone(processEngineConfiguration.getClock().getCurrentTimeZone());
      coercedValue = fmt.print(new DateTime(date, dateTimeZone));
      
    } else {
      coercedValue = super.coerceToString(value);
    }
    
    return coercedValue;
  }
}
