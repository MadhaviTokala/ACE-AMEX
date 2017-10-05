package com.americanexpress.smartserviceengine.common.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.activation.MimetypesFileTypeMap;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.common.constants.ApiErrorConstants;
import com.americanexpress.smartserviceengine.common.exception.SSEApplicationException;
import com.americanexpress.smartserviceengine.common.vo.EmailAttachment;



/**
 * Abstract implementation of a simple mail utility. The unimplemented
 * methods are usually defined in a system setting or hard coded, so left
 * to the implementing application to fulfill.
 */
@Service
public class SendMail {

	private static AmexLogger LOGGER = AmexLogger.create(SendMail.class);
	
	@Value("${SMTP_HOST_NAME}")
	private String smtpHostName;
	
	@Value("${SMTP_PORT}")
	private String smtpPort;

	@Value("${ICM_TEMPLATE_PATH}")
	private String icmTemplatePath;
	
	@Value("${ICM_IMAGE_PATH}")
	private String icmImagePath;
	
	@Value("${ICM_PROXY_REQUIRED}")
	private String proxyReq;
				
	@Value("${ICM_PROXY_HOST}")
    private String proxyHost;
	
	@Value("${ICM_PROXY_PORT}")
    private String proxyPort;
    
	/*private static SendMail instance = null;
	   
	    A private Constructor prevents any other 
	    * class from instantiating.
	    
	private SendMail(){ }
	   
    Static 'instance' method 
   public SendMail getInstance() {
	   if(instance == null) {
		   instance = new SendMail();
		   smtpHostName = EnvironmentPropertiesUtil.getProperty("SMTP_HOST_NAME");
		   System.out.println(smtpHostName);
	   	  smtpPort = EnvironmentPropertiesUtil.getProperty("SMTP_PORT");
	   	  icmTemplatePath = EnvironmentPropertiesUtil.getProperty("ICM_TEMPLATE_PATH");
	   	  icmImagePath = EnvironmentPropertiesUtil.getProperty("ICM_IMAGE_PATH");
	   	  proxyReq = EnvironmentPropertiesUtil.getProperty("ICM_PROXY_REQUIRED");
	   	  proxyHost = EnvironmentPropertiesUtil.getProperty("ICM_PROXY_HOST");
	   	  proxyPort = EnvironmentPropertiesUtil.getProperty("ICM_PROXY_PORT");
      }	 
      return instance;
   }*/
	   

	public boolean sendMail(String to, String cc,String from, String subject, String format, String body, String template, HashMap<String,String> tokens, 
			HashMap<String,String> images, List<EmailAttachment> attachments, boolean voltage) throws SSEApplicationException {
		LOGGER.debug("", "Generic Utility", "Email Utility", "sendMail", "SENDING EMAIL MESSAGE", AmexLogger.Result.success, "", 
				"SUBJECT", subject,"Template", template, "TO EMAIL ADDRESS", to);
		boolean success = false;
		try {
			// connect to host with a new session
			if(smtpHostName == null || smtpPort == null){
				return false;
			}
			Properties props = new Properties();
			props.put("mail.smtp.host", smtpHostName);
			props.put("mail.debug", Boolean.TRUE);
			props.put("mail.smtp.port", smtpPort);

			Session session = Session.getDefaultInstance(props);
			LOGGER.info("", "Generic Utility", "Email Utility", "sendMail", "STARTED SENDING EMAIL MESSAGE", AmexLogger.Result.success, "", 
					"HOST", smtpHostName, "PORT", smtpPort);
			Message emailMessage = constructMessage(session, to, cc, from, subject, format, body, template, tokens, images, attachments, voltage);
			if(emailMessage != null){
				Transport.send(emailMessage);
				success = true;
				LOGGER.info("", "Generic Utility", "Email Utility", "sendMail", "SEND EMAIL SUCCESSFUL", AmexLogger.Result.success, "", 
						"SUBJECT", subject,"TO_Email_Address", to, "CC_Email_Address", cc);
			}
			
		}catch(MessagingException msgEx){
			LOGGER.error("", "Generic Utility", "Email Utility", "sendMail", "SEND EMAIL FAILURE", AmexLogger.Result.failure, 
					"Messaging Exception while Sending Email Message.", msgEx);
			/*throw new SSEApplicationException(
					"Exception in sending email for supplier remittance", ApiErrorConstants.EMAIL_INTERNAL_SERVER_ERR_CD,
					msgEx.getMessage(),
					msgEx);*/
		}catch(Exception ex){
			LOGGER.error("", "Generic Utility", "Email Utility", "sendMail", "SEND EMAIL FAILURE", AmexLogger.Result.failure, 
					"Other Exception while Sending Email Message.", ex);
			/*throw new SSEApplicationException(
					"Exception in sending email for supplier remittance", ApiErrorConstants.EMAIL_INTERNAL_SERVER_ERR_CD,
					msgEx.getMessage(),
					msgEx);*/
		}
		return success;
	}

	private Message constructMessage(Session session, String to, String cc, String from, String subject, String format, 
			String body, String template, HashMap<String, String> tokens, HashMap<String, String> images, List<EmailAttachment> attachments, boolean voltage) 
					throws MessagingException {
		LOGGER.debug("", "Generic Utility", "Email Utility", "constructMessage", "CONSTRUCTING EMAIL MESSAGE", AmexLogger.Result.success, "", 
				"SUBJECT", subject, "TO_Email_Address", to, "CC_Email_Address", cc);
		// Create a new message
		Message msg = new MimeMessage(session);
		if(from != null && !from.trim().isEmpty()){
			msg.setFrom(new InternetAddress(from));
		}else{
			return null;
		}
		
		
		if(to != null && !to.trim().isEmpty()){
			msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
		}else{
			return null;
		}
		
		// get message body from input or icm
		String msgBody = "";
		if(body != null && !body.trim().isEmpty()){
			 msgBody = body;
			 /*LOGGER.info("", "Generic Utility", "Email Utility", "constructMessage", "CONSTRUCTING EMAIL MESSAGE",
					 AmexLogger.Result.success, "");*/
		}
		else if(template != null && !template.trim().isEmpty()){
			try {
				LOGGER.info("", "Generic Utility", "Email Utility", "constructMessage", "CONSTRUCTING EMAIL MESSAGE", AmexLogger.Result.success, "", 
						"TEMPLATE", template);
				msgBody = getTemplateFromICM(template);
			} catch (MalformedURLException e) {
				LOGGER.error("", "Generic Utility", "Email Utility", "sendMail", "SEND EMAIL FAILURE",
						AmexLogger.Result.failure, 
						"Not able to connect to ICM.", e);
				return null;
			} catch (IOException e) {
				LOGGER.error("", "Generic Utility", "Email Utility", "sendMail", "SEND EMAIL FAILURE", AmexLogger.Result.failure, 
						"Not able to connect to ICM.", e);
				return null;
			}
		}else{
			 LOGGER.info("", "Generic Utility", "Email Utility", "constructMessage", "CONSTRUCTING EMAIL MESSAGE", AmexLogger.Result.success, "", 
						"No message body received. So mail not sent.");
			 return null;
		}
		
		// set voltage header
		if(voltage){
			msg.addHeader("Sensitivity", "company-confidential");
		}
		
		if(cc != null && !cc.trim().isEmpty()){
			msg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(cc));
		}
		msg.setSubject(subject);
		
		// replace tokens with values
		//if(tokens != null){
			Pattern pattern = Pattern.compile("\\{(.*?)\\}");
			Matcher matcher = pattern.matcher(msgBody);
			while (matcher.find()) {	
			    String token = matcher.group();		    
			    if(token != null && !token.trim().isEmpty()){
					 String tokenKey = token.substring(1, token.lastIndexOf("}")).trim();
					 if(tokenKey != null && !tokenKey.trim().isEmpty()){						 
						 String[] splitStr = tokenKey.split(":", 2);						 
						 if(splitStr != null && splitStr.length > 1){
							 tokenKey = splitStr[0];
							 if("m".equalsIgnoreCase(splitStr[1]) && (tokens == null || tokens.get(tokenKey) == null)){
								 LOGGER.error("", "Generic Utility", "Email Utility", "sendMail", "SEND EMAIL FAILURE", AmexLogger.Result.failure, 
											"Token value not passed for one of the token", tokenKey);
								 return null;
							 }
						 }
						 if(splitStr != null && tokens != null && tokens.get(splitStr[0]) != null)
							 msgBody = msgBody.replace(token, tokens.get(splitStr[0]));
						 else
							 msgBody = msgBody.replace(token, "");
						
					 }
						 
					 
				}
			    
			//}

		}
		
		// set email format
		MimeBodyPart messageBodyPart = new MimeBodyPart();
		if(format != null)
			messageBodyPart.setContent(msgBody, format);
		else
			messageBodyPart.setContent(msgBody, "text/html; charset=utf-8");
		Multipart multipart = new MimeMultipart();
		multipart.addBodyPart(messageBodyPart);
		
		// add email attachments
		if(attachments != null){
			Iterator<EmailAttachment> iterator = attachments.iterator();
			while (iterator.hasNext()) {
				EmailAttachment attachment = iterator.next();
				ByteArrayDataSource baDS = new ByteArrayDataSource(attachment.getFile(),attachment.getContentType());     
				MimeBodyPart attachmentPart = new MimeBodyPart();
				attachmentPart.setDataHandler(new DataHandler(baDS));
				attachmentPart.setFileName(attachment.getFileName());
				multipart.addBodyPart(attachmentPart);
			}
		}
		
		// attach inline images
		pattern = Pattern.compile("\"(cid:[^\"]*)\"");
		matcher = pattern.matcher(msgBody);
		
		while (matcher.find()) {
		    String imageToken = matcher.group();
		    if(imageToken != null && !imageToken.trim().isEmpty()){
		    	 String imageName = imageToken.substring(imageToken.indexOf(":") + 1, imageToken.lastIndexOf("\"")).trim();			
		    	 //LOGGER.info("imageName: " +imageName);
		    	 if(images != null){
					String imagePath = images.get(imageName);
					MimeBodyPart attachMentPart = new MimeBodyPart(); //Part two is attachment
					String finalPath = Thread.currentThread().getContextClassLoader().getResource(imagePath).getPath();
					File f = new File(finalPath);
					f.exists();	
					DataSource source = new FileDataSource(finalPath);
					attachMentPart.setDataHandler(new DataHandler(source));
					attachMentPart.setHeader("Content-ID", "<" + imageName + ">");
					attachMentPart.setDisposition(MimeBodyPart.INLINE);
					multipart.addBodyPart(attachMentPart);
				 }else{
					MimeBodyPart attachMentPart = new MimeBodyPart(); //Part two is attachment
					//String finalPath = Thread.currentThread().getContextClassLoader().getResource(imagePath).getPath();
					URL url = null;
					try {
						url = new URL(icmImagePath + imageName);
						//LOGGER.info("Image URL: " + url.toString()+ " :proxyReq" + proxyReq);
						//f.exists();
						InputStream is = null;
						if(Boolean.parseBoolean(proxyReq)){						
							Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort)));
							HttpURLConnection connection = (HttpURLConnection)url.openConnection(proxy);
							connection.connect();
							is = connection.getInputStream();											
						} else {
							
								is = url.openStream();							
						}
						attachMentPart.setDataHandler(new DataHandler(new InputStreamDataSource(is, imageName)));
						attachMentPart.setHeader("Content-ID", "<" + imageName + ">");
						attachMentPart.setDisposition(MimeBodyPart.INLINE);
						multipart.addBodyPart(attachMentPart);
					} catch (MalformedURLException e) {
						LOGGER.error("", "Generic Utility", "Email Utility", "sendMail", "IMAGE ATTACHMENT", AmexLogger.Result.failure, 
								"Not able to get image from ICM.", e);
					}catch (IOException e) {
						LOGGER.error("", "Generic Utility", "Email Utility", "sendMail", "IMAGE ATTACHMENT", AmexLogger.Result.failure, 
								"Not able to get image from ICM.", e);
					}
				 }
			}
		}
		
		
			
		
		msg.setContent(multipart);
		msg.setSentDate(new Date());
		LOGGER.info("", "Generic Utility", "Email Utility", "constructMessage", "CONSTRUCTED EMAIL MESSAGE", AmexLogger.Result.success, "");
		return msg;
	}

	private String getTemplateFromICM(String template) throws IOException, MalformedURLException{
		String str = "";
		StringBuilder contentBuilder = new StringBuilder();
		BufferedReader in = null;
		URL url = new URL(icmTemplatePath + template);
		InputStream is;
		if(Boolean.parseBoolean(proxyReq)){
			Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort)));
			HttpURLConnection connection = (HttpURLConnection)url.openConnection(proxy);
			connection.connect();
			is = connection.getInputStream();
		} else {
			is = url.openStream();
		}
		
		in = new BufferedReader(new InputStreamReader(is));
		while ((str = in.readLine()) != null) {
            contentBuilder.append(str);
        }
		in.close();
		
	    String content = contentBuilder.toString();
		return content;
	}
	
   private class InputStreamDataSource implements DataSource {

	    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	    private final String name;

	    public InputStreamDataSource(InputStream inputStream, String name) {
	        this.name = name;
	        try {
	            int nRead;
	            byte[] data = new byte[16384];
	            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
	                buffer.write(data, 0, nRead);
	            }

	            buffer.flush();
	            inputStream.close();
	        } catch (IOException e) {
	           
	        }

	    }

	    @Override
	    public String getContentType() {
	        return new MimetypesFileTypeMap().getContentType(name);
	    }

	    @Override
	    public InputStream getInputStream() throws IOException {
	        return new ByteArrayInputStream(buffer.toByteArray());
	    }

	    @Override
	    public String getName() {
	        return name;
	    }

	    @Override
	    public OutputStream getOutputStream() throws IOException {
	        throw new IOException("Read-only data");
	    }

	}

}