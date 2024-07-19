package com.privatesecuredata.arch.tools;

import java.util.Calendar;

/**
 * Created by kenan on 6/10/16.
 */
public class TimeAndDate {

    /**
     * Calculate the difference in days between two dates
     *
     * @param d1 Calendar representing the start-date
     * @param d2 Calendar representing the end-date
     * @return The time difference in days
     */
    public static int daysBetween(Calendar d1, Calendar d2){
        Calendar start = (Calendar) d1.clone();
        Calendar end = (Calendar) d2.clone();

        // If cals share the same year it is easy...
        if (start.get(Calendar.YEAR) == end.get(Calendar.YEAR)) {
            return Math.abs(start.get(Calendar.DAY_OF_YEAR) - end.get(Calendar.DAY_OF_YEAR));
        } else {
            if (end.get(Calendar.YEAR) < start.get(Calendar.YEAR)) {
                //swap them
                Calendar tmp = start;
                start = end;
                end = tmp;
            }

            int extraDays = end.get(Calendar.DAY_OF_YEAR);
            end.add(Calendar.YEAR, -1);

            while (end.get(Calendar.YEAR) > start.get(Calendar.YEAR)) {
                //use getActualMaximum() because of leap years
                extraDays += end.getActualMaximum(Calendar.DAY_OF_YEAR);
                end.add(Calendar.YEAR, -1);
            }

            return extraDays + ( start.getActualMaximum(Calendar.DAY_OF_YEAR) - start.get(Calendar.DAY_OF_YEAR));
        }
    }
}
