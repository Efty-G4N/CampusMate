package com.eftyg4n.campusmate.utils;

import android.content.Context;

import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.models.Routine;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ScheduleUtils {

    private ScheduleUtils() {
    }

    // 0 = Sunday ... 6 = Saturday (same numbering as Routine.dayOfWeek)
    public static int todayIndex() {
        return Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;
    }

    // Minutes after midnight, for example 09:40 = 580
    public static int nowMinute() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
    }

    public static String dayName(Context context, int day) {
        return context.getResources().getStringArray(R.array.schedule_days)[day];
    }

    // 580 -> "09:40 AM", 870 -> "02:30 PM"
    public static String formatMinute(Context context, int minuteOfDay) {
        int hour24 = minuteOfDay / 60;
        int minute = minuteOfDay % 60;
        int hour12 = hour24 % 12;
        if (hour12 == 0) {
            hour12 = 12;
        }
        int suffix = hour24 < 12 ? R.string.schedule_am : R.string.schedule_pm;
        return String.format(Locale.getDefault(), "%02d:%02d %s",
                hour12, minute, context.getString(suffix));
    }

    // "SN+MAN" -> "SN + MAN" (both teach), "SH/MAN" -> "SH or MAN" (one of them teaches)
    public static String formatTeachers(Context context, String codes) {
        if (codes == null) {
            return "";
        }
        String or = " " + context.getString(R.string.schedule_teacher_or) + " ";
        return codes.replace("/", or).replace("+", " + ");
    }

    // The week list must be sorted by day, then start time (RoutineRepository does that).
    // Returns the class that is running now or starts next; after the last class of the
    // week it wraps around to the first class of the next week. Cancelled classes are skipped.
    public static Routine findNextClass(List<Routine> week, int today, int nowMinute) {
        Routine firstOfWeek = null;
        for (int i = 0; i < week.size(); i++) {
            Routine routine = week.get(i);
            if (Routine.STATUS_CANCELLED.equals(routine.getStatus())) {
                continue;
            }
            if (firstOfWeek == null) {
                firstOfWeek = routine;
            }
            boolean laterDay = routine.getDayOfWeek() > today;
            boolean laterToday = routine.getDayOfWeek() == today
                    && routine.getEndMinute() > nowMinute;
            if (laterDay || laterToday) {
                return routine;
            }
        }
        return firstOfWeek;
    }
}