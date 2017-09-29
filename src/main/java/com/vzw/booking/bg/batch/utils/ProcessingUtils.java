package com.vzw.booking.bg.batch.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <h1>DateTimeUtils</h1> DateTimeUtils is a utility class that provides
 * convenience routines for date handling.
 * <p>
 */
public class ProcessingUtils {

    public static final String STANDARD_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSZ";
    public static final String SHORT_DATE_FORMAT = "yyyy-MM-dd";
    public static final String FILE_DATE_FORMAT = "MM/dd/yyyy";
    public static final String REALY_SHORT_FORMAT = "yyyyMM";
    public static final String SHORT_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String SHORT_DATETIME_FORMAT_NOSPACE = "yyyy-MM-dd-HHmmss";
    public static final String MAINFRAME_FORMAT = "yyyy-MM-dd-HH:mm:ss.SSSZ";
    
    private static final String LBRACKET = "[";
    
    /**
     * formats zone date according to given format
     * @param zdt
     * @param format
     * @return 
     */
    public static String zdtToString(ZonedDateTime zdt, String format) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format);
        return dtf.format(zdt);
    }

    /**
     * formats regular date according to given format
     * @param date
     * @param format
     * @return 
     */
    public static String dateToString(Date date, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(date);
    }
    
    /**
     * Formats a ZonedDateTime into a standard string.
     *
     * @param zdt
     * @return
     */
    public static String toStringWithoutText(ZonedDateTime zdt) {
        String zdtOut = zdtToString(zdt, STANDARD_FORMAT);
        int bracketIndex = zdtOut.indexOf(LBRACKET);
        if (bracketIndex != -1) {
            zdtOut = zdtOut.substring(0, bracketIndex);
        }
        return zdtOut;
    }
    
    public static String getYearAndMonthFromStrDate(String strDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(FILE_DATE_FORMAT);
            Date date = sdf.parse(strDate);
            SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMM");
            return sdf2.format(date);
        } catch (ParseException ex) {
            Logger.getLogger(ProcessingUtils.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    public static String dateTimeToStringWithourSpaces(Date date) {
        String strDate = dateToString(date, SHORT_DATETIME_FORMAT);
        return strDate.replace(" ", ".");
    }
    
    /**
     * analyzing the line of CSV input file to determine delimiter of the fields
     * @param line
     * @return 
     */
    public static String decodeDelimiter(String line) {
        String[] delimiters = {",", ";", "|", "¦"};
        for (String delimiter : delimiters) {
            if (line.contains(delimiter))
                return delimiter;
        }
        return "None of those was found "+Arrays.toString(delimiters);
    }
    
}
