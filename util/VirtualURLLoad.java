package com.americanexpress.smartserviceengine.common.util;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import com.americanexpress.smartserviceengine.common.util.ThreadLocalManager;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;


public class VirtualURLLoad {
	private static final AmexLogger LOG = AmexLogger.create(VirtualURLLoad.class);
	@Autowired
	private static MessageSource messageSource;
	
	private static HashMap<String, String> apikeyMap;
	
	@Autowired
	private static String environment;
	
	public static MessageSource getMessageSource() {
		return messageSource;
	}

	public static String getEnvironment() {
		return environment;
	}

	public static void setEnvironment(String environment) {
		VirtualURLLoad.environment = environment;
	}

	public static void setMessageSource(MessageSource messageSource) {
		VirtualURLLoad.messageSource = messageSource;
	}
	
	public static void setApikeyMap(HashMap<String, String> apikeyMap) {
		VirtualURLLoad.apikeyMap = apikeyMap;
	}

	public static String getMessage(String name) {
		String message = null;
		Map<String, String> map = new HashMap<String, String>();
		
		try {
			String partnerId = ThreadLocalManager.getPartnerId();
			String partnerAliasName = apikeyMap.get(partnerId).toString();
			LOG.info("Partner Alias Name: " + partnerAliasName);
					
			String dynamicName = partnerAliasName+"_"+name;
			LOG.info("Dynamic Name: " + dynamicName);
			
			message= messageSource.getMessage(dynamicName, new Object[] {}, new Locale("en_US"));

		} catch (Exception ex) {
			LOG.warn(String.format("Property %s does not exist in LISA, Exception Message :%s", name,  ex.getMessage()));
		}
		return message;
	}
	
	public static String getMessagevalue(String name) {
		String message = null;
		Map<String, String> map = new HashMap<String, String>();
		
		try {
			message= messageSource.getMessage(name, new Object[] {}, new Locale("en_US"));

		} catch (Exception ex) {
			LOG.warn(String.format("Property key %s does not exist in configfured property, Exception Message :%s", name,  ex.getMessage()));
		}
		return message;
	}
	
}