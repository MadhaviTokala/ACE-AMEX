/**
 * <p>
 * Copyright © 2014 AMERICAN EXPRESS. All Rights Reserved.
 * </p>
 * <p>
 * AMERICAN EXPRESS CONFIDENTIAL. All information, copyrights, trade secrets<br>
 * and other intellectual property rights, contained herein are the property<br>
 * of AMERICAN EXPRESS. This document is strictly confidential and must not be
 * <br>
 * copied, accessed, disclosed or used in any manner, in whole or in part,<br>
 * without Amex's express written authorization.
 * </p>
 */
package com.americanexpress.smartserviceengine.common.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig.Feature;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;



public class ApiUtil {
	
	private ApiUtil() {
		super();
		// TODO Auto-generated constructor stub
	}

	public static String getErrorDescription(String responseCode) { 
		return EnvironmentPropertiesUtil.getProperty(responseCode); 
	} 
	
	public static String jsonToString(Object request)
            throws JsonGenerationException,
            JsonMappingException,
            IOException
            {
        ObjectMapper mapper = new ObjectMapper();
        mapper.getSerializationConfig().setSerializationInclusion(Inclusion.NON_NULL);
        mapper.getSerializationConfig().set(Feature.INDENT_OUTPUT, true);
        String result = mapper.writeValueAsString(request);
        return result;
            }

    public static String pojoToJSONString(Object obj)
            throws JsonGenerationException,
            JsonMappingException,
            IOException
            {
        String jsonText="";
        ObjectMapper om = new ObjectMapper();
        om.configure(Feature.INDENT_OUTPUT, true);
        om.getSerializationConfig().setSerializationInclusion(Inclusion.NON_NULL);
        om.getSerializationConfig().set(Feature.INDENT_OUTPUT, true);
        jsonText = om.writeValueAsString(obj);
        return jsonText;

            }


    /**
     * String to JSON object utility method for ActPassResponse.
     * 
     * @param inputString
     * @return
     * @throws JsonGenerationException
     * @throws JsonMappingException
     * @throws IOException
     */
    public static Object stringToDemoResJSON(String respString, Class class1)
            throws JsonGenerationException,
            JsonMappingException,
            IOException
            {
        ObjectMapper om = new ObjectMapper();
        om.configure(
            DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return om.readValue(respString,class1);

            }



	/**
	 * Utility method to get the hostname
	 * @return hostName
	 * @throws UnknownHostException
	 */
	public static String getHostName() throws UnknownHostException{
		String hostName=InetAddress.getLocalHost().getHostName().toUpperCase();
		if(hostName.indexOf(".")>0){
			hostName = hostName.substring(0,hostName.indexOf("."));
			
		}
		// TODO Temporary Fix - Increase JVM column length from 10 in Database
					if(hostName.length() >10) {
						hostName = hostName.substring(0, 9);
					}
		return hostName;
	}
	
	/**
	 * Utility method to return the timestamp in yyyy-MM-dd'T'HH:mm:ss.SSSZ format
	 * @return
	 */
	public static String getCurrentTimeStamp() {
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		String strDate = sdfDate.format(new Date());
		strDate=strDate.replace("GMT", "");
		return strDate;

		/*
		 * TODO Sent the response timestamp in same EST timezone
		 */
		/*SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    	Date date = new Date(); 
    	DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    	// Set the formatter to use a EST timezone  
    	formatter.setTimeZone(TimeZone.getTimeZone("EST"));  
    	String strDate = null;
        try {
			strDate = sdfDate.format(formatter.parse((formatter.format(date)))).toString();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return strDate;*/

	}

	public static String getFiveDigitCardNbr(String cardNumber) {
		return cardNumber == null || cardNumber.trim().length() < 10 ? cardNumber : cardNumber.substring(cardNumber.length()-5, cardNumber.length());
	}
	
	public static String fetchCountryCodeOrName(String countryCode, String countryName)
    {
        String finalValue=" ";
        finalValue= (countryCode!=null && countryCode.trim().length()>0)?((countryCode.trim().length()==2?countryCode.trim():finalValue)):((countryName!=null&&countryName.trim().length()>0?countryName.trim():finalValue));
        return finalValue;
    }
	
	public static String fetchString1OrString2(String String1, String String2)
    {
        String finalValue=" ";
        //finalValue= (OrgDBAName!=null && OrgDBAName.trim().length()>0)?(OrgDBAName):((orgName!=null&&orgName.trim().length()>0?orgName.trim():finalValue));
        finalValue= ((StringUtils.isNotBlank(String1)==true)?String1.trim():(StringUtils.isNotBlank(String2)==true)?String2:finalValue);
        return finalValue;
    }
    
	public static Map<String, String> readPropertiesMap(final String propertyKey){
		Map<String, String> propertiesMap = new HashMap<String, String>();
		String propertyValue = EnvironmentPropertiesUtil.getProperty(propertyKey);
		String[] partnerIdKeyValuePairs = propertyValue.split(ApiConstants.CHAR_SEMICOLON);
		for(String pair : partnerIdKeyValuePairs){
			String[] pidInfoArray = pair.split(ApiConstants.CHAR_COLON);
			propertiesMap.put(pidInfoArray[0], pidInfoArray[1]);
		}
		return propertiesMap;
	}
		

}
