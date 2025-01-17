package com.onepunchcrafts.util;

import java.time.DateTimeException;
import java.time.Duration;

public class TickUtil {

    public static int convertTimeInTicks(Duration duration){
        if (duration.isNegative() || duration.isZero())
            throw new DateTimeException("Duration is negative or zero");
        long millisDuration = duration.toMillis();
        if (millisDuration < 50)
            throw new DateTimeException("Duration less than 50 milliseconds");
        return Math.round((float) millisDuration / 50);
    }
}
