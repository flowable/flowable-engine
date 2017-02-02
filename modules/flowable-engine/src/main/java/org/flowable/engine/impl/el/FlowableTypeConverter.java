package org.flowable.engine.impl.el;

import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import de.odysseus.el.misc.TypeConverterImpl;

public class FlowableTypeConverter extends TypeConverterImpl {

    public FlowableTypeConverter() {}

    private static final long serialVersionUID = 1L;

    protected String coerceToString(Object value) {
        String coercedValue = null;
        if (value instanceof Date) {
            Date date = (Date)value;
            DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
            coercedValue =  fmt.print(new DateTime(date, DateTimeZone.UTC));
        } else {
            coercedValue = super.coerceToString(value);
        }
        return coercedValue;
    }
}
