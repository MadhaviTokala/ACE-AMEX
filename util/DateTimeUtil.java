package com.americanexpress.smartserviceengine.common.util;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang.time.FastDateFormat;
import org.apache.log4j.Logger;

import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.exception.InvalidTimeStampException;
import com.americanexpress.smartserviceengine.common.splunk.SplunkAction;
import com.americanexpress.smartserviceengine.common.splunk.SplunkLogger;



/**
 * <p>
 * Copyright © 2013 AMERICAN EXPRESS. All Rights Reserved.
 * </p>
 * <p>
 * AMERICAN EXPRESS CONFIDENTIAL. All information, copyrights, trade secrets<br>
 * and other intellectual property rights, contained herein are the property<br>
 * of AMERICAN EXPRESS. This document is strictly confidential and must not be<br>
 * copied, accessed, disclosed or used in any manner, in whole or in part,<br>
 * without Amex's express written authorization.
 * </p>
 */
public class DateTimeUtil {

	private static final Logger LOG = Logger.getLogger(DateTimeUtil.class);
	public static final String numericRegexPattern = "^[0-9]+$";

	private DateTimeUtil() {
		super();
	}
	public static String getSystemTime(TimeZone timezone)
			throws DatatypeConfigurationException {

		String output = null;
		if (timezone != null) {
			GregorianCalendar gc = new GregorianCalendar();
			gc.setTime(new Date());
			gc.setTimeZone(timezone);
			output = DatatypeFactory.newInstance().newXMLGregorianCalendar(gc)
					.toString();
		}
		return output;
	}

	public static String convertTimeZone(XMLGregorianCalendar xmlTimeStamp,
			TimeZone timeZone) throws DatatypeConfigurationException {

		String output = null;

		if (xmlTimeStamp != null && timeZone != null) {
			GregorianCalendar gc = xmlTimeStamp.toGregorianCalendar();
			gc.setTime(gc.getTime());
			gc.setTimeZone(timeZone);
			output = DatatypeFactory.newInstance().newXMLGregorianCalendar(gc)
					.toString();
		}

		return output;

	}

	public static TimeZone getTimeZone(String timestamp) throws ParseException {

		TimeZone timeZone = null;
		if (timestamp != null && !ApiConstants.CHAR_BLANKSPACE.equals(timestamp)) {
			String tzString = "-07:00"; // Default to MST
			Pattern patern = Pattern.compile("[-+]\\d\\d[:]{0,1}\\d\\d$");
			Matcher matcher = patern.matcher(timestamp);
			if (matcher.find()) {
				tzString = matcher.group();
			}
			String timeZoneId = "GMT" + tzString;
			timeZone = TimeZone.getTimeZone(timeZoneId);
		}

		return timeZone;
	}

	public static XMLGregorianCalendar toXMLGregorianCalendar(
			String inputTimestamp) throws ParseException,
			DatatypeConfigurationException, InvalidTimeStampException {
		XMLGregorianCalendar xmlCalendar = null;
		if (inputTimestamp != null && !ApiConstants.CHAR_BLANKSPACE.equals(inputTimestamp)) {
			// Replacing last ':' for WAS-7
			String formatTimestamp = null;
			formatTimestamp = inputTimestamp.replaceAll("\\:(?=[^.]*$)", "");
			SimpleDateFormat sdf = new SimpleDateFormat(
					"yyyy-MM-dd'T'HH:mm:ss.SSSZ");
			Date date = sdf.parse(formatTimestamp);
			GregorianCalendar gCalendar = new GregorianCalendar();
			gCalendar.setTime(date);
			xmlCalendar = DatatypeFactory.newInstance()
					.newXMLGregorianCalendar(gCalendar);
		} else {
			throw new InvalidTimeStampException();
		}
		return xmlCalendar;
	}

	public static String getExpirationXMLGregorianCal(String inputDate,
			TimeZone timeZone) throws ParseException,DatatypeConfigurationException {

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date date = sdf.parse(inputDate);
		GregorianCalendar gc = new GregorianCalendar();
		gc.setTime(date);
		gc.setTimeZone(timeZone);
		String output = DatatypeFactory.newInstance()
				.newXMLGregorianCalendar(gc).toString();
		return output;

	}

	public static String getDBFormattedTimeStamp(String inputTimestamp) {
		// This is to convert incoming timestamp into equivalent
		// 2013-10-05-01.51.29.218000 format with MST timezone
		// 1. Convert incoming time into XMLGregorianCalendar object
		// 2. Covert into MST String timestamp
		// 3. Regular expression to chnage it to required DN accepted format
		String toDate = "";
		try {
			if (inputTimestamp != null) {
				String formatTimestamp = null;
				formatTimestamp = inputTimestamp.replaceAll("Z", "+00:00");
				XMLGregorianCalendar xmlCalendar = toXMLGregorianCalendar(formatTimestamp);
				String outString = convertTimeZone(xmlCalendar,
						TimeZone.getTimeZone("MST"));
				toDate = outString.substring(0, 10) + "-"
						+ outString.substring(11, 13) + "."
						+ outString.substring(14, 16) + "."
						+ outString.substring(17, 23) + "000";
			}
		} catch (Exception e) {
			SplunkLogger.logSplunkPing(LOG, "getDBFormattedTimeStamp", "",
					"getDBFormattedTimeStamp",
					"In getDBFormattedTimeStamp method", SplunkAction.FAIL,
					"Error in parsing Date String: " + inputTimestamp, e);
		}
		return toDate;
	}

	public static XMLGregorianCalendar getXMLGregorianCalendar() {
		GregorianCalendar gcal = new GregorianCalendar();
		XMLGregorianCalendar xgcal = null;
		try {
			xgcal = DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal);
		} catch (DatatypeConfigurationException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}
		return xgcal;
	}

	public static String getDateMMMMMd(String inputTimestamp) {
		// This is to convert incoming timestamp into MMMMM d e.g. October 25 or
		// November 8 for GCS email

		String toDate = "";
		try {
			if (inputTimestamp != null) {
				String formatTimestamp = null;
				formatTimestamp = inputTimestamp.replaceAll("\\:(?=[^.]*$)", "");
				SimpleDateFormat sdf = new SimpleDateFormat(
						"yyyy-MM-dd-HH.mm.ss.SSS");
				Date date = sdf.parse(formatTimestamp);
				sdf = new SimpleDateFormat("MMMMM d");
				toDate = sdf.format(date);
			}
		} catch (Exception e) {
			SplunkLogger.logSplunkPing(LOG, "getDateMMMMMd", "",
					"getDateMMMMMd", "In getDateMMMMMd method",
					SplunkAction.FAIL,
					"Error in parsing Date String into MMMMM d "
							+ inputTimestamp, e);
		}
		return toDate.trim();
	}

	public static String getCurDateTime() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss.SSS");

		// get current date time with Calendar()
		Calendar cal = Calendar.getInstance();
		return dateFormat.format(cal.getTime());

	}

	public static String getCurDateyyyyMMdd() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

		// get current date time with Calendar()
		Calendar cal = Calendar.getInstance();
		return dateFormat.format(cal.getTime());

	}
	public static java.sql.Date getCurrentDateyyyyMMdd() throws ParseException {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		dateFormat.setLenient(false);
		// get current date time with Calendar()
		Calendar cal = Calendar.getInstance();
		String parsed = dateFormat.format(cal.getTime());
		java.sql.Date sqlDate = java.sql.Date.valueOf(parsed);
		return sqlDate;
	}

	public static String getCurDateTimeString(String inputTimestamp) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss.SSS");
		String dtString = "";

		try {
			if (inputTimestamp != null) {
				String formatTimestamp = null;
				formatTimestamp = inputTimestamp.replaceAll("\\:(?=[^.]*$)", "");
				Date date = dateFormat.parse(formatTimestamp);
				dateFormat = new SimpleDateFormat("yyMMddHHmmssSSS");
				dtString = dateFormat.format(date);
			}

		} catch (Exception e) {
			SplunkLogger.logSplunkPing(LOG, "getCurDateTimeString", "",
					"getCurDateTimeString", "In getCurDateTimeString method",
					SplunkAction.FAIL,
					"Error in parsing Date String into yyMMddHHmmssZ "
							+ inputTimestamp, e);
		}

		return dtString.trim();
	}

	public static String getRequestTimestamp(String currentTime)
			throws ParseException, DatatypeConfigurationException {

		String timestamp = null;

		TimeZone timeZone = DateTimeUtil.getTimeZone(currentTime);
		timestamp = DateTimeUtil.getSystemTime(timeZone);

		return timestamp;
	}

	public static String getDb2Date(String inputDate) throws ParseException {
		SimpleDateFormat in = new SimpleDateFormat("dd/MM/yyyy");
		SimpleDateFormat out = new SimpleDateFormat("yyyy-MM-dd");
		String dateStringOut = "";

		Date date = in.parse(inputDate);
		dateStringOut = out.format(date); // YYYY-MM-DD

		return dateStringOut;
	}

	public static String getDb2DateValue(String inputDate)
			throws ParseException {
		SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat out = new SimpleDateFormat("yyyy-MM-dd");
		String dateStringOut = "";

		Date date = in.parse(inputDate);
		dateStringOut = out.format(date); // YYYY-MM-DD

		return dateStringOut;
	}

	public static java.sql.Date getDB2SqlDate(String strDate)
			throws ParseException {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		Date parsed = format.parse(strDate);
		java.sql.Date sqlDate = new java.sql.Date(parsed.getTime());
		return sqlDate;
	}
	
	public static java.sql.Date getDB2SqlDateYYYYMMDD(String strDate)
			throws ParseException {
		SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
		format.setLenient(false);
		Date parsed = format.parse(strDate);
		java.sql.Date sqlDate = new java.sql.Date(parsed.getTime());
		return sqlDate;
	}


	public static String getFormattedDB2Dt1(String strDate)throws ParseException{
		SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat out = new SimpleDateFormat("yyyy/MM/dd");

		String outDate = "";

		Date date = in.parse(strDate);
		outDate = out.format(date);

		return outDate;


	}

	public static String getFormattedDB2Dt2(String strDate)throws ParseException{
		SimpleDateFormat in = new SimpleDateFormat("dd/MM/yyyy");
		SimpleDateFormat out = new SimpleDateFormat("yyyy/MM/dd");

		String outDate = "";

		Date date = in.parse(strDate);
		outDate = out.format(date);

		return outDate;


	}

	public static String getYYYYMMDDDb2DateValue(String inputDate)
			throws ParseException {
		SimpleDateFormat in = new SimpleDateFormat("yyyyMMdd");
		SimpleDateFormat out = new SimpleDateFormat("yyyy-MM-dd");
		String dateStringOut = "";

		Date date = in.parse(inputDate);
		dateStringOut = out.format(date); // YYYY-MM-DD

		return dateStringOut;
	}


	public static String getMMDDYYYYhhmmssDb2DateValue(String inputDate)
			throws ParseException {
		SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		SimpleDateFormat out = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		out.setLenient(false);
		String dateStringOut = "";
		if(null != inputDate && !"".equalsIgnoreCase(inputDate.trim())){
			Date date = in.parse(inputDate);
			dateStringOut = out.format(date); // MM/DD/YYYY HH:mm:ss
		}
		return dateStringOut;
	}

	public static String getMMDDYYYYhhmmssDb2DateValuePymt(String inputDate)
			throws ParseException {
		SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss");
		SimpleDateFormat out = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		String dateStringOut = "";
		if(null != inputDate && !"".equalsIgnoreCase(inputDate.trim())){
			/*
			 * To fix the mismatch in DB timestamp and converted timestamp values
			 * (approx 5min difference), the milliseconds are ignored in the Conversion process
			 *
			 */
			inputDate =inputDate.substring(0,inputDate.lastIndexOf("."));
			Date date = in.parse(inputDate);
			dateStringOut = out.format(date); // MM/DD/YYYY HH:mm:ss
		}
		return dateStringOut;
	}

	public static java.sql.Date getStringToDB2SqlDate(String strDate)
			throws ParseException {
		SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
		format.setLenient(false);
		Date parsed = format.parse(strDate);
		java.sql.Date sqlDate = new java.sql.Date(parsed.getTime());
		return sqlDate;
	}

	public static java.sql.Timestamp getStringToDB2SqlTimeStamp(String strDate)
			throws ParseException {
		SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		Date parsed = format.parse(strDate);
		Timestamp sqlTimestamp = new java.sql.Timestamp(parsed.getTime());
		return sqlTimestamp;
	}

	public static String convertDb2DateValueToYYYYMMDD(String inputDate)
			throws ParseException {
		String dateStringOut = "";
		if(null != inputDate && !"".equals(inputDate)){
		SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
		SimpleDateFormat out = new SimpleDateFormat("yyyyMMdd");


		Date date = in.parse(inputDate);
		dateStringOut = out.format(date); // YYYYMMDD
		}
		return dateStringOut;
	}


	public static String convertCurrentDttoDB2Format(String inputDate){

		String dateStringOut = "";


		if(null != inputDate && !"".equals(inputDate)){
			SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss.SSS");

			SimpleDateFormat out = new SimpleDateFormat("yyyyMMdd");



			Date date;
			try {
				date = in.parse(inputDate);

				dateStringOut = out.format(date); // YYYYMMDD

			}catch (Exception e) {
				SplunkLogger.logSplunkPing(LOG, "convertCurrentDttoDB2Format", "",
						"convertCurrentDttoDB2Format",
						"In convertCurrentDttoDB2Format method", SplunkAction.FAIL,
						"Error in parsing Date String: " + inputDate, e);
			}
		}



		return dateStringOut;

	}
	public static String convertYYYYMMDDtoMMddyyy(String inputDate) throws ParseException {
		if(null==inputDate || "".equalsIgnoreCase(inputDate)) {
			return "";
		}

		SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat out = new SimpleDateFormat("MM/dd/yyyy");
		String dateStringOut = "";

		Date date = in.parse(inputDate);
		dateStringOut = out.format(date);

		return dateStringOut;
	}
	
	public static String convertYYYYMMDDtoMMddyyyTS(String inputDate) throws ParseException {
		if(null==inputDate || "".equalsIgnoreCase(inputDate)) {
			return "";
		}
		//input: 2016-07-11 00:23:45.056471
		SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		//in.setLenient(false);
		//output: 01/01/2016 13:01
		SimpleDateFormat out = new SimpleDateFormat("MM/dd/yyyy HH:mm");
		out.setLenient(false);
		String dateStringOut = "";

		Date date = in.parse(inputDate);
		dateStringOut = out.format(date);

		return dateStringOut;
	}

	public static boolean checkPaymentSubmissionTime(String pymtSubmissionDate, int pymtSubmissionPeriod) {
		Timestamp pymtSubsmissionTS = Timestamp.valueOf(pymtSubmissionDate);
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(pymtSubsmissionTS.getTime());
		cal.add(Calendar.HOUR_OF_DAY, pymtSubmissionPeriod);
		long pymtTime = cal.getTime().getTime();
		long currentTime = System.currentTimeMillis();
		Timestamp newTimestamp = new Timestamp(pymtTime);
		LOG.info("Pymt Threshhold Time: " + newTimestamp);
		LOG.info("Current Time: " +new Timestamp(currentTime));
		if (currentTime > pymtTime) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean checkPaymentProcessTime(String pymtSubmissionDate, int pymtProcessPeriod) {
		Timestamp currentTime = Timestamp.valueOf(pymtSubmissionDate);
		Timestamp oldTime = new Timestamp(new Date().getTime());
		long milliseconds1 = oldTime.getTime();
		long milliseconds2 = currentTime.getTime();
		long diff = milliseconds1 - milliseconds2;
		/*long diffSeconds = diff / 1000;
		long diffMinutes = diff / (60 * 1000);
		long diffHours = diff / (60 * 60 * 1000);*/
		long diffDays = diff / (24 * 60 * 60 * 1000);
		if(diffDays > pymtProcessPeriod){
			return true;
		}else {
			return false;
		}
	}

	public static String getDateYYMMDD(){
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyMMdd");
		simpleDateFormat.setLenient(false);
		String date = simpleDateFormat.format(new Date());
		return date;
	}


	public static String getDate(String strDate) throws ParseException {
	    SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	    format.setLenient(false);
	    Date parsed = format.parse(strDate);
	    SimpleDateFormat format1 = new SimpleDateFormat("MM/dd/yyyy");
	    String date = format1.format(parsed);
	    return date;
	    }
	
	public static String getDBFormattedTimeStampforLastRunTS(String inputTimestamp) {
            // This is to convert incoming timestamp into equivalent
            // 2013-10-05-01.51.29.218000 format with MST timezone
            // 1. Convert incoming time into XMLGregorianCalendar object
            // 2. Covert into MST String timestamp
            // 3. Regular expression to chnage it to required DN accepted format
            String toDate = "";
            try {
                    if (inputTimestamp != null) {
                            String formatTimestamp = null;
                            formatTimestamp = inputTimestamp.replaceAll("Z", "+00:00");
                            XMLGregorianCalendar xmlCalendar = toXMLGregorianCalendar(formatTimestamp);
                            String outString = convertTimeZone(xmlCalendar,
                                            TimeZone.getTimeZone("MST"));
                            toDate = outString.substring(0, 10) + " "
                                            + outString.substring(11, 13) + ":"
                                            + outString.substring(14, 16) + ":"
                                            + outString.substring(17, 23) + "000";
                    }
            } catch (Exception e) {
                    SplunkLogger.logSplunkPing(LOG, "getDBFormattedTimeStamp", "",
                                    "getDBFormattedTimeStamp",
                                    "In getDBFormattedTimeStamp method", SplunkAction.FAIL,
                                    "Error in parsing Date String: " + inputTimestamp, e);
            }
            return toDate;
    }
	
	public static boolean calculateDateDiff(String emailTriggerDate) throws ParseException {
	        // TODO Auto-generated method stub
	        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	        Date date1=new Date();
	        Date date2=sdf.parse(emailTriggerDate);
	        long diffDays =0;
	        long milliseconds1 = date1.getTime();
	        long milliseconds2 = date2.getTime();
	        long diff = milliseconds1 - milliseconds2;
	        diffDays = diff / (24 * 60 * 60 * 1000);

	        if(diffDays>0){
	            return true;
	        }
	        return false;
	    }

	
	public static Boolean isFutureTimeStamp(String termsCondAcceptTime) {
        
    	//String termsCondAcceptTime = "2015-10-29T07:06:12.051-07:00";
		String serverTermsCondAcceptTime=getServerFormattedTimeStamp(termsCondAcceptTime);
		String currentTime = getServerFormattedTimeStamp(ApiUtil.getCurrentTimeStamp());
		Timestamp termsCondAcceptTimeStamp = Timestamp.valueOf(serverTermsCondAcceptTime);
		Timestamp currentTimeStamp = Timestamp.valueOf(currentTime);
		return termsCondAcceptTimeStamp.after(currentTimeStamp);
    }
	
	public static String getServerFormattedTimeStamp(String inputTimestamp) {
		// This is to convert incoming timestamp into equivalent
		// 2013-10-05-01.51.29.218000 format with MST timezone
		// 1. Convert incoming time into XMLGregorianCalendar object
		// 2. Covert into MST String timestamp
		// 3. Regular expression to chnage it to required DN accepted format
		String toDate = "";
		try {
			if (inputTimestamp != null) {
				String formatTimestamp = null;
				formatTimestamp = inputTimestamp.replaceAll("Z", "+00:00");
				XMLGregorianCalendar xmlCalendar = toXMLGregorianCalendar(formatTimestamp);
				String outString = convertTimeZone(xmlCalendar,TimeZone.getTimeZone("MST"));
				toDate = outString.replace("T", " ") ;
				toDate = toDate.substring(0,23) + "000";
			}
		} catch (Exception e) {
			SplunkLogger.logSplunkPing(LOG, "getServerFormattedTimeStamp", "",
					"getServerFormattedTimeStamp",
					"In getServerFormattedTimeStamp method", SplunkAction.FAIL,
					"Error in parsing Date String: " + inputTimestamp, e);				
		}
		return toDate;
	}

	
	public static String getUTCDateString(long utcTime) {
		final String format = ApiConstants.DATEFRMT_MMDDYYYY;
		final TimeZone TIME_ZONE = TimeZone.getTimeZone(ApiConstants.TIMEZONE_MST);		
		final Calendar CALENDAR = Calendar.getInstance(TIME_ZONE);
		
		SimpleDateFormat dtFormat = new SimpleDateFormat(format);
		dtFormat.setCalendar(CALENDAR);
		Date localDate = new Date(utcTime);
		return dtFormat.format(localDate);
	}
	
	public static String getDBFormattedTimeStampforCurrentTS() {
        String toDate = "";
        String inputTimestamp=ApiUtil.getCurrentTimeStamp();
        try {
                if (inputTimestamp != null) {
                        String formatTimestamp = null;
                        formatTimestamp = inputTimestamp.replaceAll("Z", "+00:00");
                        XMLGregorianCalendar xmlCalendar = toXMLGregorianCalendar(formatTimestamp);
                        String outString = convertTimeZone(xmlCalendar,
                                        TimeZone.getTimeZone("MST"));
                        toDate = outString.substring(0, 10) + "-"
                                        + outString.substring(11, 13) + "."
                                        + outString.substring(14, 16) + "."
                                        + outString.substring(17, 23) + "000";
                }
        } catch (Exception e) {
                SplunkLogger.logSplunkPing(LOG, "getDBFormattedTimeStamp", "",
                                "getDBFormattedTimeStamp",
                                "In getDBFormattedTimeStamp method", SplunkAction.FAIL,
                                "Error in parsing Date String: " + inputTimestamp, e);
        }
        return toDate;
}
	
	public static String getDateToString(String strDate) throws ParseException {
	    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss");
	    format.setLenient(false);
	    Date parsed = format.parse(strDate);
	    SimpleDateFormat format1 = new SimpleDateFormat("MM/dd/yyyy");
	    String date = format1.format(parsed);
	    return date;
	}
	
	public static java.sql.Date formYYYYMMDDSqlDateValue(String inputDate,String inputFormat)
			throws ParseException {
		SimpleDateFormat utilDateFrmt = new SimpleDateFormat(inputFormat);
		Date utildate = utilDateFrmt.parse(inputDate);
		SimpleDateFormat sqlDateFrmt = new SimpleDateFormat("yyyy-MM-dd");
		String sqlDt = sqlDateFrmt.format(utildate);
		java.sql.Date sqlDate = java.sql.Date.valueOf(sqlDt);
		return sqlDate;
	}
	public static Date getSpecificDateyyyyMMdd(String dateFrmt,String date1) {
		DateFormat dateFormat = new SimpleDateFormat(dateFrmt);
		Date specificDate = null;
		try {
			specificDate = dateFormat.parse(date1);
		} catch (ParseException e) {
			//e.printStackTrace();
		}
		return specificDate;
	}
	
	public static Date getCurrntDateyyyyMMdd(String dateFrmt) {
		DateFormat dateFormat = new SimpleDateFormat(dateFrmt);

		// get current date time with Calendar()
		Calendar cal = Calendar.getInstance();
		Date date = null;
		try {
			date = dateFormat.parse(dateFormat.format(cal.getTime()));
		} catch (ParseException e) {
			//e.printStackTrace();
		}
		return date;

	}
	
    public static boolean isDateLessThanCurrDate(String dateToCompareWithCurrDate,String dateFrmt) {    	
    	
    	Date currentdate = getCurrntDateyyyyMMdd(dateFrmt);
    	return compareDate(currentdate, dateToCompareWithCurrDate, dateFrmt);
    }
    
    public static boolean isEndDateB4StrtDate(String startDate , String endDate,String dateFrmt) {    	
    	
    	Date startDt = getSpecificDateyyyyMMdd(dateFrmt,startDate);
    	return compareDate(startDt, endDate, dateFrmt);
        
    }
    
    public static boolean compareDate(Date startDt, String endDate,String dateFrmt) {    	
    	  SimpleDateFormat sdf = new SimpleDateFormat(dateFrmt);
          sdf.setLenient(false);
          boolean flag = true;
          try {
          	Date endDt = sdf.parse(endDate);
              if(endDt.before(startDt)){
              	flag = false;
              }
          } catch (Exception e) {
          	return false;
          }
          return flag;
    }
  

	public static String convertYYYYMMDDtoYYYYMMDDTHHMMSSSSS(String inputDate) throws ParseException {
		if(null==inputDate || "".equalsIgnoreCase(inputDate)) {
			return "";
		}
		//input: 2016-07-11 00:23:45.056471
		SimpleDateFormat in = new SimpleDateFormat("yyyyMMdd");
		//in.setLenient(false);
		//output: 01/01/2016 13:01
		SimpleDateFormat out = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		out.setLenient(false);
		String dateStringOut = "";

		Date date = in.parse(inputDate);
		dateStringOut = out.format(date);

		return dateStringOut;
	}
	
	public static Timestamp convertYYYYMMDDtoTimestamp(String inputDate) throws ParseException {
		SimpleDateFormat in = new SimpleDateFormat("yyyyMMdd");
		in.setLenient(false);
		Date date = in.parse(inputDate);
		Timestamp timestamp =  new Timestamp(date.getTime());
		return timestamp;
	}
	
	public static String convertYYYYMMDDtoYYYYMMDDHHMMSSSSSEOD(String inputDate) throws ParseException {
		if(null==inputDate || "".equalsIgnoreCase(inputDate)) {
			return "";
		}
		//input: 2016-07-11 00:23:45.056471
		SimpleDateFormat in = new SimpleDateFormat("yyyyMMdd");
		//in.setLenient(false);
		//output: 01/01/2016 13:01
		SimpleDateFormat out = new SimpleDateFormat("yyyy-MM-dd'T'");
		out.setLenient(false);
		String dateStringOut = "";

		Date date = in.parse(inputDate);
		dateStringOut = out.format(date);
		dateStringOut = dateStringOut+ApiConstants.TOKEN_ENDTIMESTAMP_HHMMSSSSS;
		return dateStringOut;
	}
	
	public static String getCurrentDateyyyyMMdd(String dateStr) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		Date date1 = new Date();
		
		try {
			date1 = dateFormat.parse(dateStr);
		} catch (ParseException e) {

		}
		
		dateFormat = new SimpleDateFormat("yyyyMMdd");
		return dateFormat.format(date1.getTime());
	}
	
	public static long getCurrentDateyyyyMMddTHHMMSSSSSinMilliSec(String dateStr) {
		//DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		Date date1 = new Date();
		
		try {
			date1 = dateFormat.parse(dateStr);
		} catch (ParseException e) {

		}		
		return date1.getTime();
	}
	
	
	public static String getCurrentDateyyyyMMddTHHMMSSSSS(String dateStr) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		DateFormat dateFormat1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		Date date1 = new Date();
		
		try {
			date1 = dateFormat.parse(dateStr);
		} catch (ParseException e) {
		}		
		return dateFormat1.format(date1.getTime());
	}
	
	public static String getDateyyyyMMddHHMMSSSSS(String dateStr, int hours, int minutes,int seconds) throws ParseException {
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		DateFormat dateFormat1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		Date date = dateFormat.parse(dateStr);
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.HOUR, hours);
		cal.add(Calendar.MINUTE, minutes);
		cal.add(Calendar.SECOND, seconds);
		return dateFormat1.format(cal.getTime());
	}


	public static String addDateyyyyMMdd(String dateStr,int noOfDays) {
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		Date date1 = new Date();
		
		try {
			date1 = dateFormat.parse(dateStr);
		} catch (ParseException e) {

		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(date1);
		cal.add(Calendar.DATE,noOfDays);
		dateFormat = new SimpleDateFormat("yyyyMMdd");
		return dateFormat.format(cal.getTime());
	}
	
	public static Timestamp getTimestampFromString(String string) throws ParseException{
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		dateFormat.setLenient(Boolean.FALSE);
		Date date1 = dateFormat.parse(string);
		Timestamp timestamp =  new Timestamp(date1.getTime());
		return timestamp;
	}
	
	public static Timestamp getCurrentTimestampWithYYYYMMDD(){
		Date date = new Date();                      // timestamp now
		Calendar cal = Calendar.getInstance();       // get calendar instance
		cal.setTime(date);                           // set cal to date
		cal.set(Calendar.HOUR_OF_DAY, 0);            // set hour to midnight
		cal.set(Calendar.MINUTE, 0);                 // set minute in hour
		cal.set(Calendar.SECOND, 0);                 // set second in minute
		cal.set(Calendar.MILLISECOND, 0);            // set millis in second
		Date zeroedDate = cal.getTime(); 
		Timestamp timestamp = new Timestamp(zeroedDate.getTime());
		return timestamp;
	}
	
	public static java.sql.Date getStringWithTimeZoneToDB2SqlDate(String strDate)
			throws ParseException {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S");
		format.setLenient(false);
		Date parsed = format.parse(strDate);
		java.sql.Date sqlDate = new java.sql.Date(parsed.getTime());
		return sqlDate;
	}
	public static String forMessagePostTimeStamp(){
		String tzString="-0700";
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		String strDate = sdfDate.format(new Date());
		strDate=strDate.concat(tzString);
		return strDate;
	}
	

	public static String getFormattedEffectiveDate(String inputDate) throws ParseException {
		SimpleDateFormat in = new SimpleDateFormat("MM/dd/yyyy");
		SimpleDateFormat out = new SimpleDateFormat("yyyy-MM-dd");
		String dateStringOut = "";

		Date date = in.parse(inputDate);
		dateStringOut = out.format(date); // YYYY-MM-DD

		return dateStringOut;
	}
	
	public static String convertDb2DateValue(String inputDate)
			throws ParseException {
		String dateStringOut = "";
		if(null != inputDate && !"".equals(inputDate)){
		SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		SimpleDateFormat out = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss.SSSSSS");
		Date date = in.parse(inputDate);
		dateStringOut = out.format(date); // YYYYMMDD
		}
		return dateStringOut;
	}

}
