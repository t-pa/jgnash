/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Static methods to make working with dates a bit easier
 *
 * @author Craig Cavanaugh
 * @author Vincent Frison
 */

@SuppressWarnings("MagicConstant")
public class DateUtils {

    private static final int MILLISECONDS_PER_SECOND = 1000;

    private static final Pattern MONTH_PATTERN = Pattern.compile("M{1,2}");

    private static final Pattern DAY_PATTERN = Pattern.compile("d{1,2}");

    private static final DateTimeFormatter shortDateTimeFormatter;

    /**
     * Pattern for a {@code java.time.format.DateTimeFormatter} to parse and format to the default.  DateTimeFormatter
     * does not like use of the 'z' literal, so force the UTC zone which is the XStream default.
     * <p>
     * {@code com.thoughtworks.xstream.converters.basic.DateConverter} format.
     */
    public static final String DEFAULT_XSTREAM_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS 'UTC'";

    static {
        final DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);

        if (df instanceof SimpleDateFormat) {
            String pattern = ((SimpleDateFormat) df).toPattern();
            pattern = DAY_PATTERN.matcher(MONTH_PATTERN.matcher(pattern).replaceAll("MM")).replaceAll("dd");

            shortDateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
        } else {
            shortDateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
        }
    }

    /**
     * ThreadLocal for a {@code GregorianCalendar}
     */
    private static final ThreadLocal<GregorianCalendar> gregorianCalendarThreadLocal = new ThreadLocal<GregorianCalendar>() {
        @Override
        protected GregorianCalendar initialValue() {
            return new GregorianCalendar();
        }
    };

    /**
     * ThreadLocal for a short {@code DateFormat}
     */
    private static final ThreadLocal<DateFormat> shortDateFormatHolder = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            final DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);

            if (df instanceof SimpleDateFormat) {
                String pattern = ((SimpleDateFormat) df).toPattern();

                pattern = DAY_PATTERN.matcher(MONTH_PATTERN.matcher(pattern).replaceAll("MM")).replaceAll("dd");
                ((SimpleDateFormat) df).applyPattern(pattern);
            }
            return df;
        }
    };

    private DateUtils() {
    }

    /**
     * Converts a {@code LocalDate} into a {@code Date} using the default timezone
     *
     * @param localDate {@code LocalDate} to convert
     * @return an equivalent {@code Date}
     */
    public static Date asDate(final LocalDate localDate) {
        return Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * Converts a {@code LocalDateTime} into a {@code Date} using the default timezone
     *
     * @param localDate {@code LocalDateTime} to convert
     * @return an equivalent {@code Date}
     */
    public static Date asDate(final LocalDateTime localDate) {
        return Date.from(localDate.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * Converts a {@code Date} into a {@code LocalDate} using the default timezone.
     *
     * @param date {@code Date} to convert
     * @return an equivalent {@code LocalDate} or {@code null} if the supplied date was {@code null}
     */
    public static LocalDate asLocalDate(final Date date) {
        if (date != null) {
            return asLocalDate(date.getTime());
        }

        return null;
    }

    /**
     * Converts milliseconds from the epoch of 1970-01-01T00:00:00Z into a {@code LocalDate} using the default timezone.
     *
     * @param milli milliseconds from the epoch of 1970-01-01T00:00:00Z.
     * @return an equivalent {@code LocalDate} or {@code null} if the supplied date was {@code null}
     */
    public static LocalDate asLocalDate(final long milli) {
        return Instant.ofEpochMilli(milli).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * Converts a LocaleDate into milliseconds from the epoch of 1970-01-01T00:00:00Z
     *
     * @param localDate {@code LocalDate} to convert
     * @return and equivalent milliseconds from the epoch of 1970-01-01T00:00:00Z
     */
    public static long asEpochMilli(final LocalDate localDate) {
        return localDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * MILLISECONDS_PER_SECOND;
    }

    /**
     * Converts a {@code Date} into a {@code LocalDateTime} using the UTC timezone.
     *
     * @param date {@code Date} to convert
     * @return an equivalent {@code LocalDateTime} or {@code null} if the supplied date was {@code null}
     */
    public static LocalDateTime asLocalDateTime(final Date date) {
        if (date != null) {
            return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.of("UTC")).toLocalDateTime();
        }

        return null;
    }

    /**
     * Determines is {@code LocalDate} d1 occurs after {@code LocalDate} d2. The specified dates are
     * inclusive.
     *
     * @param d1 date 1
     * @param d2 date 2
     * @return true if d1 is after d2
     */
    public static boolean after(final LocalDate d1, final LocalDate d2) {
        return before(d2, d1, true);
    }

    /**
     * Determines if {@code LocalDate} d1 occurs before {@code LocalDate} d2. The specified dates are
     * inclusive
     *
     * @param d1 date 1
     * @param d2 date 2
     * @return true if d1 is before d2 or the same date
     */
    public static boolean before(final LocalDate d1, final LocalDate d2) {
        return before(d1, d2, true);
    }

    /**
     * Determines if {@code LocalDate} d1 occurs before {@code LocalDate} d2.
     *
     * @param d1        {@code LocalDate} 1
     * @param d2        {@code LocalDate} 2
     * @param inclusive {@code true} is comparison is inclusive
     * @return {@code true} if d1 occurs before d2
     */
    public static boolean before(final LocalDate d1, final LocalDate d2, final boolean inclusive) {
        if (inclusive) {
            return d1.isEqual(d2) || d1.isBefore(d2);
        }

        return d1.isBefore(d2);
    }

    /**
     * Returns the number of days in the year
     *
     * @param year calendar year
     * @return the number of days in the year
     */
    private static int getDaysInYear(final int year) {
        return LocalDate.ofYearDay(year, 1).lengthOfYear();
    }

    /**
     * @param year The year to generate the array for
     * @return The array of dates
     */
    public static LocalDate[] getFirstDayBiWeekly(final int year) {
        List<LocalDate> dates = new ArrayList<>();

        LocalDate[] allWeeks = getFirstDayWeekly(year);

        for (int i = 0; i < allWeeks.length; i += 2) {
            dates.add(allWeeks[i]);
        }

        return dates.toArray(new LocalDate[dates.size()]);
    }

    /**
     * Generates an array of dates starting on the first day of every month in
     * the specified year
     *
     * @param year The year to generate the array for
     * @return The array of dates
     */
    public static LocalDate[] getFirstDayMonthly(final int year) {
        LocalDate[] list = new LocalDate[12];
        for (int i = 0; i < 12; i++) {
            list[i] = getFirstDayOfTheMonth(i, year);
        }
        return list;
    }

    /**
     * Returns a leveled date representing the first day of the month based on a
     * specified date.
     *
     * @param date the base date to work from
     * @return The last day of the month and year specified
     */
    public static LocalDate getFirstDayOfTheMonth(final LocalDate date) {
        return date.with(TemporalAdjusters.firstDayOfMonth());
    }

    /**
     * Returns a leveled date representing the first day of the month
     *
     * @param month The month (index starts at 0)
     * @param year  The year (index starts at 1)
     * @return The last day of the month and year specified
     */
    private static LocalDate getFirstDayOfTheMonth(final int month, final int year) {
        assert month >= 0 && month <= 11;

        final GregorianCalendar c = gregorianCalendarThreadLocal.get();

        c.set(year, month, 15);
        c.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.getActualMinimum(Calendar.DAY_OF_MONTH));

        return asLocalDate(c.getTime());
    }

    /**
     * Returns an array of the starting date of each quarter in a year
     *
     * @param year The year to generate the array for
     * @return The array of quarter bound dates
     */
    public static LocalDate[] getFirstDayQuarterly(final int year) {
        LocalDate[] bounds = new LocalDate[4];

        bounds[0] = LocalDate.of(year, Month.JANUARY, 1);
        bounds[1] = LocalDate.of(year, Month.APRIL, 1);
        bounds[2] = LocalDate.of(year, Month.JULY, 1);
        bounds[3] = LocalDate.of(year, Month.OCTOBER, 1);

        return bounds;
    }

    /**
     * Returns an array of Dates starting with the first day of each week of the
     * year.
     *
     * @param year The year to generate the array for
     * @return The array of dates
     * @see <a href="http://en.wikipedia.org/wiki/ISO_8601">ISO_8601</a>
     */
    public static LocalDate[] getFirstDayWeekly(final int year) {
        final GregorianCalendar cal = gregorianCalendarThreadLocal.get();
        final GregorianCalendar testCal = new GregorianCalendar();

        List<LocalDate> dates = new ArrayList<>();

        // level the date
        cal.setTime(trimDate(cal.getTime()));

        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.WEEK_OF_YEAR, 1);
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());

        for (int i = 0; i < 53; i++) {
            testCal.setTime(cal.getTime());
            testCal.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY);

            if (testCal.get(Calendar.YEAR) == year) {
                dates.add(asLocalDate(cal.getTime()));
            }

            cal.add(Calendar.DATE, 7); // add 7 days
        }

        return dates.toArray(new LocalDate[dates.size()]);
    }

    /**
     * Returns an array of every day in a given year
     *
     * @param year The year to generate the array for
     * @return The array of dates
     */
    public static LocalDate[] getAllDays(final int year) {

        final List<LocalDate> dates = new ArrayList<>();

        for (int i = 1; i <= getDaysInYear(year); i++) {
            dates.add(LocalDate.ofYearDay(year, i));
        }

        return dates.toArray(new LocalDate[dates.size()]);
    }

    /**
     * Returns date representing the last day of the month given a specified date.
     *
     * @param date the base date to work from
     * @return The last day of the month of the supplied date
     */
    public static LocalDate getLastDayOfTheMonth(@NotNull final LocalDate date) {
        Objects.requireNonNull(date);

        return date.with(TemporalAdjusters.lastDayOfMonth());
    }

    /**
     * Generates an array of dates ending on the last day of every month between
     * the start and stop dates.
     *
     * @param startDate The date to start at
     * @param endDate   The data to stop at
     * @return The array of dates
     */
    public static List<LocalDate> getLastDayOfTheMonths(final LocalDate startDate, final LocalDate endDate) {
        final ArrayList<LocalDate> list = new ArrayList<>();

        final LocalDate end = DateUtils.getLastDayOfTheMonth(endDate);
        LocalDate t = DateUtils.getLastDayOfTheMonth(startDate);

        /*
         * add a month at a time to the previous date until all of the months
         * have been captured
         */
        while (before(t, end)) {
            list.add(t);

            t = t.plusMonths(1);
            t = t.with(TemporalAdjusters.lastDayOfMonth());
        }
        return list;
    }

    /**
     * Generates an array of dates starting on the first day of every month
     * between the start and stop dates.
     *
     * @param startDate The date to start at
     * @param endDate   The data to stop at
     * @return The array of dates
     */
    public static List<LocalDate> getFirstDayOfTheMonths(final LocalDate startDate, final LocalDate endDate) {
        final ArrayList<LocalDate> list = new ArrayList<>();

        final LocalDate end = DateUtils.getFirstDayOfTheMonth(endDate);
        LocalDate t = DateUtils.getFirstDayOfTheMonth(startDate);

        /*
         * add a month at a time to the previous date until all of the months
         * have been captured
         */
        while (before(t, end)) {
            list.add(t);
            t = t.with(TemporalAdjusters.firstDayOfNextMonth());
        }
        return list;
    }

    /**
     * Returns a {@code LocalDate} date representing the last day of the quarter based on
     * a specified date.
     *
     * @param date the base date to work from
     * @return The last day of the quarter specified
     */
    public static LocalDate getLastDayOfTheQuarter(final LocalDate date) {
        Objects.requireNonNull(date);

        LocalDate result;

        LocalDate[] bounds = getQuarterBounds(date);

        if (date.compareTo(bounds[2]) < 0) {
            result = bounds[1];
        } else if (date.compareTo(bounds[4]) < 0) {
            result = bounds[3];
        } else if (date.compareTo(bounds[6]) < 0) {
            result = bounds[5];
        } else {
            result = bounds[7];
        }

        return result;
    }

    /**
     * Returns a {@code LocalDate} representing the last day of the year based on a
     * specified date.
     *
     * @param date the base date to work from
     * @return The last day of the year specified
     */
    public static LocalDate getLastDayOfTheYear(@NotNull final LocalDate date) {
        Objects.requireNonNull(date);

        return date.with(TemporalAdjusters.lastDayOfYear());
    }

    /**
     * Returns an array of quarter bound dates of the year based on a specified
     * date. The order is q1s, q1e, q2s, q2e, q3s, q3e, q4s, q4e.
     *
     * @param date the base date to work from
     * @return The array of quarter bound dates
     */
    private static LocalDate[] getQuarterBounds(final LocalDate date) {
        Objects.requireNonNull(date);

        final LocalDate[] bounds = new LocalDate[8];

        bounds[0] = date.with(TemporalAdjusters.firstDayOfYear());
        bounds[1] = date.withMonth(Month.MARCH.getValue()).with(TemporalAdjusters.lastDayOfMonth());
        bounds[2] = date.withMonth(Month.APRIL.getValue()).with(TemporalAdjusters.firstDayOfMonth());
        bounds[3] = date.withMonth(Month.JUNE.getValue()).with(TemporalAdjusters.lastDayOfMonth());
        bounds[4] = date.withMonth(Month.JULY.getValue()).with(TemporalAdjusters.firstDayOfMonth());
        bounds[5] = date.withMonth(Month.SEPTEMBER.getValue()).with(TemporalAdjusters.lastDayOfMonth());
        bounds[6] = date.withMonth(Month.OCTOBER.getValue()).with(TemporalAdjusters.firstDayOfMonth());
        bounds[7] = date.with(TemporalAdjusters.lastDayOfYear());

        return bounds;
    }

    /**
     * Returns the number of the quarter (i.e. 1, 2, 3 or 4) based on a
     * specified date.
     *
     * @param date the base date to work from
     * @return The number of the quarter specified
     */
    public static int getQuarterNumber(final LocalDate date) {
        Objects.requireNonNull(date);

        int result;

        LocalDate[] bounds = getQuarterBounds(date);

        if (date.compareTo(bounds[2]) < 0) {
            result = 1;
        } else if (date.compareTo(bounds[4]) < 0) {
            result = 2;
        } else if (date.compareTo(bounds[6]) < 0) {
            result = 3;
        } else {
            result = 4;
        }

        return result;
    }

    /**
     * Generates a customized DateFormat with constant width for all dates. A new
     * instance is created each time
     *
     * @return a short DateFormat
     */
    public static DateFormat getShortDateFormat() {
        return shortDateFormatHolder.get();
    }

    public static DateTimeFormatter getShortDateTimeFormat() {
        return shortDateTimeFormatter;
    }

    /**
     * Returns the numerical week of the year given a date.
     * <p>
     * Minimal days of week is set to 4 to comply with ISO 8601
     *
     * @param dateOfYear the base date to work from
     * @return the week of the year
     */
    public static int getWeekOfTheYear(final LocalDate dateOfYear) {
        return dateOfYear.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear());
    }

    /**
     * Trims the date so that only the day, month, and year are significant.
     *
     * @param date date to trim
     * @return leveled date
     */
    public static Date trimDate(final Date date) {
        final GregorianCalendar c = gregorianCalendarThreadLocal.get();

        c.setTime(date);

        c.set(Calendar.MILLISECOND, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.HOUR_OF_DAY, 0);

        return c.getTime();
    }
}
