package com.americanexpress.smartserviceengine.common.util;

import java.io.ByteArrayOutputStream;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.ws.soap.SOAPFaultException;

import org.apache.commons.lang.StringUtils;

import com.americanexpress.amexlogger.AmexLogger;


/**
 * @author vishwakumar_c
 *
 */
public class SOAPOperationsLoggingHandler implements SOAPHandler<SOAPMessageContext> {

	/** Log Object reference **/
    private static AmexLogger LOG = AmexLogger.create(SOAPOperationsLoggingHandler.class);

    private String eventId = new String();

    /**
     * This method is default getHeaders for SOAPHandler
     */
    @Override
    public Set<QName> getHeaders()
    {
        return null;
    }

    /**
     * This method is default handleMessage for SOAPHandler which calls the
     * Logging Method.
     */
    @Override
    public boolean handleMessage(SOAPMessageContext oSOAPMessageContext)
    {

        Boolean outbound = (Boolean)oSOAPMessageContext.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        String authReqId=ThreadLocalManager.getApiMsgId();
    	if(StringUtils.isBlank(authReqId)){
    		authReqId = eventId;
    	}

        if (outbound)
        {
            SOAPEnvelope envelope;
            try
            {

            	envelope = oSOAPMessageContext.getMessage().getSOAPPart()
            			.getEnvelope();

            	SOAPHeader header = envelope.getHeader();

            	SOAPElement trackingHdr = header.addChildElement("TrackingHdr", "ns4", "http://www.americanexpress.com/PAYVE/ServiceHeader/V4");
            	SOAPElement requestorInfo = trackingHdr.addChildElement("RequestorInfo", "ns4", "http://www.americanexpress.com/PAYVE/ServiceHeader/V4");
            	SOAPElement msgPostTime = requestorInfo.addChildElement("MsgPostTime", "ns4", "http://www.americanexpress.com/PAYVE/ServiceHeader/V4").addTextNode(CommonUtils.currentRequestTime());
            	SOAPElement messageID = requestorInfo.addChildElement("MessageID", "ns4", "http://www.americanexpress.com/PAYVE/ServiceHeader/V4").addTextNode(CommonUtils.getMessageId(40));


            	LOG.debug(authReqId, "SmartServiceEngine", "SOAPOperationsLoggingHandler",
                        "Added Header parameters to SOAP Header",
                        "Added Header parameters to SOAP Header for Payve service",
                        AmexLogger.Result.success, "",
                        "msgPostTime",msgPostTime.getAttribute("MsgPostTime"),
                        "messageID",messageID.getAttribute("MessageID"));

            }
            catch (SOAPException e)
            {
            	LOG.error(authReqId, "SmartServiceEngine", "SOAPOperationsLoggingHandler",
                        "Error while adding header parameters to SOAP Header",
                        "Error while adding header parameters to SOAP Header for Payve service",
                        AmexLogger.Result.failure, "Error while adding header parameters to SOAP Header",e);
            }
        logToSystemOut(oSOAPMessageContext);
    }else{
        logToSystemOut(oSOAPMessageContext);
    }
    return true;
    }

    /**
     * This method is default handleFault for SOAPHandler which calls the
     * Logging Method.
     */
    @Override
    public boolean handleFault(SOAPMessageContext oSOAPMessageContext) throws SOAPFaultException
    {

        logToSystemOut(oSOAPMessageContext);
        return true;
    }

    /**
     * This method is default Close for SOAPHandler
     */
    @Override
    public void close(MessageContext messageContext)
    {

    }


    /**
     * This method will log all the Request and Response XMLs for an SOAP Web
     * service.
     *
     * @param smcSOAPMessageContext
     */
    private void logToSystemOut(SOAPMessageContext oSOAPMessageContext)
    {
    	String authReqId=ThreadLocalManager.getApiMsgId();
if(StringUtils.isBlank(authReqId)){
    		authReqId = eventId;
    	}        Boolean outboundProperty = (Boolean)oSOAPMessageContext
            .get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        if (!outboundProperty.booleanValue())
        {
        	 LOG.debug(authReqId, "SmartServiceEngine", "SOAPOperationsLoggingHandler",
                     "About to log Response XML from Payve Service",
                     "About to log Response XML from Payve Service",
                     AmexLogger.Result.success, "",
                     "outbound_property",outboundProperty+"");

        }
        else
        {
            LOG.debug(authReqId, "SmartServiceEngine", "SOAPOperationsLoggingHandler",
                    "About to log Request XML from Payve Service",
                    "About to log Request XML from Payve Service",
                    AmexLogger.Result.success, "",
                    "outbound_property",outboundProperty+"");
        }

        SOAPMessage oSOAPMessage = oSOAPMessageContext.getMessage();
        try
        {
            ByteArrayOutputStream oMessageOutputStream = new ByteArrayOutputStream();
            oSOAPMessage.writeTo(oMessageOutputStream);
            String sMessage = new String(oMessageOutputStream.toByteArray());

            LOG.debug(authReqId, "SmartServiceEngine", "SOAPOperationsLoggingHandler",
                    "Logging SOAP XML",
                    "SOAP XML from or to Payve Service",
                    AmexLogger.Result.success, "",
                    "outbound_property",outboundProperty+"",
                    "soap_xml",sMessage);
        }
        catch (Exception e)
        {
        	LOG.error(authReqId, "SmartServiceEngine", "SOAPOperationsLoggingHandler",
                    "Error occured while logging SOAP XML",
                    "Error occured while logging SOAP XML from or to Payve Service",
                    AmexLogger.Result.failure, "Error occured while logging SOAP XML",e);
        }
    }

    public void setEventId(String eventId){
    	this.eventId = eventId;
    }
}
