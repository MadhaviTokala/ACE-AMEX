package com.americanexpress.smartserviceengine.common.util;

import org.springframework.context.ApplicationContext;

/**
 * <p>
 * Copyright © 2013 AMERICAN EXPRESS. All Rights Reserved.
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
public class BeanFactory {
	
	/** holds the singleton object reference **/
	private static BeanFactory oBeanFactory = new BeanFactory();
	/** holds the application context **/
	private ApplicationContext oApplicationContext;
	
	/**
	 * private default constructor
	 */
	private BeanFactory() {
	}
	
	/**
	 * returns the Spring Bean Object 
	 */
	public Object getBean(String beanName) {
		if (oApplicationContext != null && oApplicationContext.getBean(beanName) != null) {
			return oApplicationContext.getBean(beanName);
		} else {
			return null;
		}
	}
	
	/**
	 * returns the singleton object reference 
	 */
	public static BeanFactory getInstance(){
		return oBeanFactory;
	}
	/**
	 * sets application context
	 */
	public void setApplicationContext(ApplicationContext oApplicationContext){
		this.oApplicationContext = oApplicationContext;
	}
	
}
