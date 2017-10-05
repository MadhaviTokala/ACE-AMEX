package com.americanexpress.smartserviceengine.common.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class ApplicationConfiguration implements ApplicationContextAware {

	public void setApplicationContext(ApplicationContext ctx)
			throws BeansException {
		BeanFactory.getInstance().setApplicationContext(ctx);
	}
}
