package com.americanexpress.smartserviceengine.common.util;

import java.lang.reflect.Method;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.common.enums.Environment;

/**
 *  
 *
 */
public class AceEnvironment {
	private static final AmexLogger LOG = AmexLogger.create(AceEnvironment.class);
	private static final String CLUSTER_KEY = "jvm.cluster.name";
	
	private Environment environment;
	private String clusterName;
	private String jvmName;

	private AceEnvironment(final Environment environment) {
		this.environment = environment;
	}

	private AceEnvironment() {
		if (EnvironmentInitializer.INSTANCE != null) {
			throw new IllegalStateException(
					"Environmento already instantiated.");
		}
		initializeEnvironment();
	}

	private void initializeEnvironment() {
		String env = System.getProperty("spring.profiles.active");
		if (StringUtils.equalsIgnoreCase("e0", env)) {
			this.environment = Environment.E0;
		} else if (StringUtils.equalsIgnoreCase("e1", env)) {
			this.environment = Environment.E1;
		} else if (StringUtils.equalsIgnoreCase("e2", env)) {
			this.environment = Environment.E2;
		} else if (StringUtils.equalsIgnoreCase("e3", env)) {
			this.environment = Environment.E3;
		}
		
		// Cluster Name
		clusterName = StringUtils.stripToEmpty(System.getProperty(CLUSTER_KEY));
		
		// JVM Name
		initializeJvmName();
	}

	private void initializeJvmName() {
		try {
			Object obj = null;
			Class<?> clazz = Class.forName("com.ibm.ejs.ras.RasHelper");
			if (clazz != null) {
				Method method = clazz.getMethod("getServerName", new Class<?>[]{});
				if ( method != null) {
					obj = method.invoke(null, null);
				}
			}

			if (obj instanceof String) {
				String cloneId = (String) obj;
				jvmName = StringUtils.substringAfterLast(cloneId, "\\");
			}
			
		} catch (Exception exp) {
			LOG.warn(String.format("Exception occured while getting the server name:[%s]", ExceptionUtils.getStackTrace(exp)));
		}
		
	}

	public static AceEnvironment getInstance() {
		return EnvironmentInitializer.INSTANCE;
	}

	private static class EnvironmentInitializer {
		private static final AceEnvironment INSTANCE = new AceEnvironment();
	}
	
	public boolean isEnvironment(Environment environment) {
		return this.environment == environment;
	}

	public String getClusterName() {
		return clusterName;
	}

	public String getJvmName() {
		return this.jvmName;
	}

	public Environment getEnvironment() {
		return environment;
	}
	
}
