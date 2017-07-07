package org.flowable.engine.impl.el;

import java.util.Date;

import org.flowable.engine.impl.util.CommandContextUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class DateUtil {

    public static String format(Object value) {
        String formattedString = null;
        if (value instanceof Date) {
            Date date = (Date) value;
            DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
            DateTimeZone dateTimeZone = DateTimeZone.forTimeZone(CommandContextUtil.getProcessEngineConfiguration().getClock().getCurrentTimeZone());
            formattedString = fmt.print(new DateTime(date, dateTimeZone));
        } else {
            formattedString = value.toString();
        }

        return formattedString;
    }
}
