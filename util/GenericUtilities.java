package com.americanexpress.smartserviceengine.common.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.soap.SOAPMessage;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.w3c.dom.NodeList;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.SchedulerConstants;
import com.americanexpress.smartserviceengine.common.constants.ServiceConstants;

import com.fasterxml.jackson.databind.ObjectMapper;

public class GenericUtilities {
	
	private static AmexLogger LOGGER = AmexLogger.create(GenericUtilities.class);

	private static final String YYYY_MM_DD_HH_MM_SS_SSSSSS = "yyyy-MM-dd' 'HH:mm:ss.SSSSSS";
	private static final String YYYY_MM_DD_T_HH_MM_SS_SSSZ = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";	//2014-01-10T15:27:18.060-0700
	private static final String YYYY_MM_DD_T_HH_MM_SS = "yyyy-MM-dd'T'HH:mm:ss";		// 2012-12-17T09:30:47
	private static final String YYYY_MM_DD_T_HH_MM_SS_SSS = "yyyy-MM-dd'T'HH:mm:ss.SSS";
	private static final String ACE_DB_FORMAT_TIMESTAMP = "yyyy-MM-dd-HH.mm.ss.SSS";
	private static final String MM_DD_YYYY = "MM/dd/yyyy";
	private static final String MM_YY = "MMyy";
	private static final String MMMM_DD_YYYY = "MMMM dd, yyyy";
	
	public static String getMessageId(int len) {
        String randomStr = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(randomStr.charAt(rnd.nextInt(randomStr.length())));
        }
        return new SimpleDateFormat("ddHHmmss").format(System.currentTimeMillis()) + sb.toString();
    }
	
	public static String getCurrentTimestamp() {
		return new SimpleDateFormat(YYYY_MM_DD_HH_MM_SS_SSSSSS).format(System.currentTimeMillis());
	}
	
	public static String getCurrentTimeAS_YYYY_MM_DD_T_HH_MM_SS() {
		return new SimpleDateFormat(YYYY_MM_DD_T_HH_MM_SS).format(System.currentTimeMillis());
	}
	
	public static String javaToJson(Object obj) throws Exception{
		String jsonStr = null;
		ObjectMapper mapper = new ObjectMapper();
		jsonStr = mapper.writeValueAsString(obj);
		return jsonStr;
	}
	
	public static <T> T jsonToJava(String json, Class<T> value) throws Exception{
		T javaObj = null;
		ObjectMapper mapper = new ObjectMapper();
		javaObj = mapper.readValue(json, value);
		return javaObj;
	}

	public static String getCurrentTimestampAsISO8601Format() {
	    String result = new SimpleDateFormat(YYYY_MM_DD_T_HH_MM_SS_SSSZ).format(System.currentTimeMillis());
	    result = result.substring(0, result.length() - 2) + ":" + result.substring(result.length() - 2); // 2014-01-10T15:27:18.060-07:00
	    return result;
	}
	
	public static <T> boolean contains(final T[] array, final T value) {
	    for(final T element : array){
	        if(element == value || value != null && value.equals(element)){
	            return true;
	        }
	    }
	    return false;
	}
	
	public static boolean isEmpty(Collection<?> collection){
		return collection == null || collection.isEmpty();
	}
	
	public static boolean isNullOrEmpty(Collection<?> collection){
		return collection == null || collection.isEmpty();
	}
	
	public static boolean isNullOrEmpty(Object[] objects){
		return objects == null || objects.length == 0;
	}
	
	public static boolean isEmpty(Map<?, ?> map){
		return map==null || map.keySet().isEmpty();
	}
	
	public static boolean isNullOrEmpty(Map<?, ?> map){
		return map==null || map.isEmpty();
	}
	
	public static boolean isEmpty(String string){
		return string==null || string.trim().length()==0;
	}
		
	public static boolean isEquals(Object o1, Object o2){
		return o1 == null ? o2 == null : o1.equals(o2);
	}
	
	public static boolean isNullOrEmpty(String string){
		return string==null || string.trim().length()==0;
	}
	
	public static boolean isAllNotNulls(Object... objects){
		boolean isAllNotNulls = true;
		if(!isNullOrEmpty(objects)){
			for(Object obj : objects){
				if(obj == null){
					isAllNotNulls = false;
					break;
				}
			}
		}
		return isAllNotNulls;
	}
	
	public static String appendCommaSeperatedValue(String... values){
		StringBuilder builder = new StringBuilder();
		for(String value : values){
			if(!GenericUtilities.isNullOrEmpty(value)){
				if(builder.length() > 0){
					builder.append(ServiceConstants.COMMA);
				}
				builder.append(value);
			}
		}
		return builder.toString();
	}
	
	/**
	 * Returns results in UTC Format (YYYY_MM_DD_T_HH_MM_SS_SSS). Time will be end of nth Day.
	 * @param nthDay
	 * @param date
	 * @return
	 */
	public static String getEndOfNthDayInUTCZone(int nthDay, long timeInMilli) {
	    Calendar calendar = Calendar.getInstance();
	    calendar.setTimeInMillis(timeInMilli);
	    calendar.add(Calendar.DATE, nthDay);
	    calendar.set(Calendar.HOUR_OF_DAY, calendar.getMaximum(Calendar.HOUR_OF_DAY));
	    calendar.set(Calendar.MINUTE, calendar.getMaximum(Calendar.MINUTE));
	    calendar.set(Calendar.SECOND, calendar.getMaximum(Calendar.SECOND));
	    calendar.set(Calendar.MILLISECOND, calendar.getMaximum(Calendar.MILLISECOND));
	    FastDateFormat fastDateFormat = FastDateFormat.getInstance(YYYY_MM_DD_T_HH_MM_SS_SSS, TimeZone.getTimeZone("UTC"));
	    return fastDateFormat.format(calendar.getTimeInMillis());
	}
	
	/**
	 * Returns results in UTC Format (YYYY_MM_DD_T_HH_MM_SS_SSS). Time will be current.
	 * @param nthDay - use 0 if u need current date time otherwise use no of days you want to add to current time.
	 * @return
	 */
	public static String getCurrentTimeOfNthDayInUTCZone(int nthDay, long timeInMilli){
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(timeInMilli);
		calendar.add(Calendar.DATE, nthDay);
		FastDateFormat fastDateFormat = FastDateFormat.getInstance(YYYY_MM_DD_T_HH_MM_SS_SSS, TimeZone.getTimeZone("UTC"));
		return fastDateFormat.format(calendar.getTimeInMillis());
	}
	
	public static Date convertStringToDate(String timeStamp, String format) {
		Date utcTime = null;
		SimpleDateFormat fromDateFormat = new SimpleDateFormat(format);
		try {
			utcTime = fromDateFormat.parse(timeStamp);
		} catch (ParseException ex) {
			LOGGER.error("Exception while converting " + timeStamp + " to Format " + format + ": " + ex.getMessage());
		}
		return utcTime;
	}
	
	public static Date parseFromACEdbFormatTime(String timeStamp) {
		return convertStringToDate(timeStamp, ACE_DB_FORMAT_TIMESTAMP);
	}
	
	public static Date parseFromStdTime(String timeStamp) {
		return convertStringToDate(timeStamp, YYYY_MM_DD_T_HH_MM_SS_SSS);
	}
	
	public static Date convertFromUTCtoMST(String utcTimeString, String format) {
		Date utcTime = null;
		SimpleDateFormat fromDateFormat = new SimpleDateFormat(format);
		fromDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		try {
			utcTime = fromDateFormat.parse(utcTimeString);
		} catch (ParseException ex) {
			LOGGER.error("Exception while converting " + utcTimeString + " to Format " + format + ": " + ex.getMessage());
		}
		return utcTime;
	}
	
	public static Date convertFromUTCtoMSTStdFormat(String utcTimeString) {
		return convertFromUTCtoMST(utcTimeString, YYYY_MM_DD_T_HH_MM_SS_SSS);
	}
	
	public static String getTodaysDateMMDDYYYY() {
		return new SimpleDateFormat(MM_DD_YYYY).format(System.currentTimeMillis());
	}
	
	public static String convertToMMDDYYYYFormat(Date date) {
		return new SimpleDateFormat(MM_DD_YYYY).format(date.getTime());
	}
	
	public static String convertToMMYYFormat(Date date) {
		return new SimpleDateFormat(MM_YY).format(date.getTime());
	}
	
	public static String convertToMMMMDDYYYYFormat(Date date) {
		return new SimpleDateFormat(MMMM_DD_YYYY).format(date.getTime());
	}

	/**
	 * This method converts UTC Date (yyyy-MM-dd'T'HH:mm:ss.SSS)
	 * string to the MST date in MM/dd/yyyy format
	 * @param dateToConvert
	 * @return
	 */
	public static String convertFromUTCToMSTMMDDYYYYFormat(String dateToConvert) {
		String convertedDate = null;
		if(dateToConvert != null){
			Date startDate = GenericUtilities.convertFromUTCtoMSTStdFormat(dateToConvert);
			if(startDate != null){
				convertedDate = GenericUtilities.convertToMMDDYYYYFormat(startDate);
			}
		}
		return convertedDate;
	}
	
	/**
	 * This method converts UTC Date (yyyy-MM-dd'T'HH:mm:ss.SSS)
	 * string to the MST date in MMMM dd, yyyy format
	 * @param dateToConvert
	 * @return
	 */
	public static String convertFromUTCToMST_MMMMDDYYYYFormat(String dateToConvert) {
		String convertedDate = null;
		if(dateToConvert != null){
			Date startDate = GenericUtilities.convertFromUTCtoMSTStdFormat(dateToConvert);
			if(startDate != null){
				convertedDate = GenericUtilities.convertToMMMMDDYYYYFormat(startDate);
			}
		}
		return convertedDate;
	}
	

	public static String maskXmlPayload(SOAPMessage oSOAPMessage,String xmlMessageString, String serviceIdentifierStr, int nonMaskLength ,String apiMsgId){
				
		 NodeList nodes = null;
		 String maskXmlString="";
		 String xmlKey = "";
		 try{
			 nodes = oSOAPMessage.getSOAPBody().getElementsByTagName(serviceIdentifierStr);
	         if(nodes.getLength()>0)
	         {
                           
	         	//String compareStr="AcctNbr:"; 
	    	 	xmlKey = EnvironmentPropertiesUtil.getProperty(serviceIdentifierStr);
	    		String[] formStr =xmlKey.split(ApiConstants.C3_XML_SPLIT_CHAR);
	    		String completeRegEx="";		
    		
           		for(String compareStrng: formStr){
          			    //completeRegEx ="<(.*):"+compareStrng+">(.*?)</(.*):"+compareStrng+">(.*)";
           				completeRegEx =ApiConstants.C3_XML_GROUP1+compareStrng+ApiConstants.C3_XML_GROUP2+compareStrng+ApiConstants.C3_XML_GROUP3;
          			    //System.out.println(completeRegEx);
		           		Pattern pattern = Pattern.compile(completeRegEx);
		           		Matcher matcher = pattern.matcher(xmlMessageString);
		           		String nonMaskXmlValue=null;
		           		int i=2;
		           		if (matcher.find())
		           		{
		           			nonMaskXmlValue=matcher.group(i);
		           			maskXmlString = xmlMessageString.replace(nonMaskXmlValue, 
			           				GenericUtilities.maskField(nonMaskXmlValue,(nonMaskXmlValue.length()-nonMaskLength), nonMaskXmlValue.length(), 
			           				ApiConstants.C3_MASK_CHAR,"",ApiConstants.TRUEBOOLEAN));
	           				
		           		}else{
		           			LOGGER.error(apiMsgId, "SmartServiceEngine", "Mask xml PII Data", "GenericUtils:maskXmlPayload",
		          	                "unable to find the xmlkey in the soap Msg", AmexLogger.Result.failure, 
		          	                "unable to find the xmlkey in soap_Msg","compareStrng",compareStrng);
		           		}
		           		
		           	/*	if(StringUtils.isNotBlank(nonMaskXmlValue)){
		           			maskXmlString = maskXmlString.replace(nonMaskXmlValue, 
		           				GenericUtilities.maskField(nonMaskXmlValue,(nonMaskXmlValue.length()-nonMaskLength), nonMaskXmlValue.length(), 
		           				ApiConstants.C3_MASK_CHAR,"",ApiConstants.TRUEBOOLEAN));		           			
		           		}*/
           		}
           		//System.out.println("maskXmlString "+maskXmlString);
        
	         }else{
        			LOGGER.error(apiMsgId, "SmartServiceEngine", "Mask xml PII Data", "GenericUtils:maskXmlPayload",
          	                "unable to Identify the SOAP service Type", AmexLogger.Result.failure, 
          	                "unable to Identify the SOAP_MSG","serviceIdentifierStr",serviceIdentifierStr);
           	 }
	         }catch(Exception e){
	        	 LOGGER.error(apiMsgId, "SmartServiceEngine", "Mask xml PII Data", "GenericUtils:maskXmlPayload",
      	                "Exception while masking maskXmlPayload_in logs", AmexLogger.Result.failure, "Exception while masking maskXmlPayload in logs");
	   		
			}
         
         return maskXmlString;
    }
	 
	public static String maskField(String number, int startIndex, int endIndex,char replaceCharacter, String apiMsgId, boolean isFpan) {
		String maskedNum = null;
		try {
			if (StringUtils.isNotBlank(number)) {
				maskedNum = number.trim();
				String b = maskedNum.substring(startIndex, endIndex);
				if (isFpan) {
					maskedNum = StringUtils.leftPad(b, number.length(),replaceCharacter);
				} else {
					String startKey = number.substring(0,startIndex);
			        String endKey = number.substring(endIndex , number.length());
			        String padded = StringUtils.rightPad(startKey, endIndex,replaceCharacter); 
			        maskedNum = padded.concat(endKey);	
				}
			}
		} catch (Exception e) {
			LOGGER.error(apiMsgId, "SmartServiceEngine", "Mask PII Data",
					"GenericUtils:maskField",
					"Exception while masking Field in logs",
					AmexLogger.Result.failure,
					"Exception while masking Field in logs");
			maskedNum = number;
		}
		return maskedNum;
	}
	 	 
	 public static String maskJsonPayload(String jsonPayloadString, String maskValue,int nonMaskLength ,String apiMsgId,boolean isFPAN){
	    String maskedJsonPayloadStr="";
	    try{
	    	if(maskValue!=null && !maskValue.isEmpty()){	    		
	    		if(isFPAN){
	    			maskedJsonPayloadStr=jsonPayloadString.replace(maskValue,maskField(maskValue,(maskValue.length()-nonMaskLength), maskValue.length(),ApiConstants.C3_MASK_CHAR,apiMsgId,isFPAN));
	    		}else{
	    			maskedJsonPayloadStr=jsonPayloadString.replace(maskValue,maskField(maskValue,SchedulerConstants.START_MASK_TOKEN_INDEX,SchedulerConstants.END_MASK_TOKEN_INDEX,ApiConstants.C3_MASK_CHAR,apiMsgId,isFPAN));
	    		}
	    		}
	    }catch(Exception e){
	   		LOGGER.error(apiMsgId, "SmartServiceEngine", "Mask PII Data", "GenericUtils:maskJsonPayload",
	                "Exception while masking jsonPayload_in logs", AmexLogger.Result.failure, "Exception while masking jsonPayload in logs");
	   		
	   	  }
		return maskedJsonPayloadStr;
	}
	 
	public static Map<String, Object> maskFieldInMap(Map<String, Object> inMap,String maskFieldKey, int nonMaskLength, String apiMsgId,boolean isFPAN) {
		Map<String, Object> inMaskMap = new HashMap<String, Object>(inMap);
		String maskFieldValue = "";
		try {
			maskFieldValue = ((String) inMap.get(maskFieldKey));
			if (StringUtils.isNotBlank(maskFieldValue)) {
				maskFieldValue = maskFieldValue.trim();
				if (isFPAN) {
					inMaskMap.put(maskFieldKey,	maskField(maskFieldValue,(maskFieldValue.length() - nonMaskLength),	maskFieldValue.length(),ApiConstants.C3_MASK_CHAR,apiMsgId, isFPAN));
				} else {
					inMaskMap.put(maskFieldKey,	maskField(maskFieldValue,SchedulerConstants.START_MASK_TOKEN_INDEX,SchedulerConstants.END_MASK_TOKEN_INDEX,ApiConstants.C3_MASK_CHAR,apiMsgId, isFPAN));
				}

			}
		} catch (Exception e) {
			LOGGER.error(apiMsgId, "SmartServiceEngine", "Mask PII Data",
					"GenericUtils:maskFieldInMap",
					"Exception while masking HMAP Field in logs",
					AmexLogger.Result.failure,
					"Exception while masking DB Field in logs");

		}
		return inMaskMap;
	}
	
}