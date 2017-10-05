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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ApplicationContext;

/**
 * @author vishwakumar_c
 * 
 */
public class EnvironmentPropertiesUtil extends PropertyPlaceholderConfigurer {

	private static Map<String, String> properties = new HashMap<String, String>();
	private static ApplicationContext appCntxt;
	/**
	 * This method loads the properties file.
	 */
	@Override
	protected void loadProperties(final Properties props) throws IOException {
		super.loadProperties(props);
		for (final Object key : props.keySet()) {
			properties.put((String) key, props.getProperty((String) key));
		}

	}

	// Loading Lisa URL/Real

	public static String getProperty(final String name) {
		if (!"e3".equalsIgnoreCase(VirtualURLLoad.getEnvironment()) 
				&& StringUtils.contains(name, "SERVICE_URL") 
				&& VirtualURLLoad.getMessageSource() != null
				&& StringUtils.isNotBlank(VirtualURLLoad.getMessage(name))) {
			return VirtualURLLoad.getMessage(name);
		} else {
			return properties.get(name);
		}
	}

	
	// Loading etv/vng Switch indicator 

	public static String getOtherPropertyValues(final String name) {
		//LOG.info("inside getPropertyValues "+VirtualURLLoad.getMessageSource() + " , "+StringUtils.isNotBlank(VirtualURLLoad.getMessage1(name)));
		String propValue = VirtualURLLoad.getMessagevalue(name);
		
			if (VirtualURLLoad.getMessageSource() != null && StringUtils.isNotBlank(propValue)) {
				return propValue;
			} else {
				return properties.get(propValue);
			}
		}
	/**
	 * @param bean
	 * @return java.lang.Object
	 */
	public static Object getBean(final String bean) {
		return getApplicationContext().getBean(bean);
	}

	/**
	 * @return org.springframework.context.ApplicationContext
	 */
	public static ApplicationContext getApplicationContext() {

		return appCntxt;
	}

}