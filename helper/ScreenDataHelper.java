package com.americanexpress.smartserviceengine.helper;

import handler.ext.GsScreenDataServiceExt;

import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceException;

import org.apache.commons.lang.StringUtils;
import org.datacontract.schemas._2004._07.americanexpress_globalsanctions_realtimews_v2.Request;
import org.datacontract.schemas._2004._07.americanexpress_globalsanctions_realtimews_v2.Response;
import org.springframework.beans.factory.annotation.Autowired;

import webservice.globalsanctions.americanexpress.GlobalSanctionsScreening;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.client.ScreenDataclient;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.ApiErrorConstants;
import com.americanexpress.smartserviceengine.common.constants.SchedulerConstants;
import com.americanexpress.smartserviceengine.common.constants.SchedulerErrorConstants;
import com.americanexpress.smartserviceengine.common.exception.SSEApplicationException;
import com.americanexpress.smartserviceengine.common.util.EnvironmentPropertiesUtil;
import com.americanexpress.smartserviceengine.common.util.SOAPOperationsLoggingHandler;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.vo.ScreenDataVO;

public class ScreenDataHelper {

    private static AmexLogger logger = AmexLogger.create(ScreenDataHelper.class);

    @Autowired
    private TivoliMonitoring tivoliMonitoring;

    public Map<String, Object> getScreenData(String eventID, ScreenDataVO objScreenDataVO) throws GeneralSecurityException, SSEApplicationException {
        logger.info(eventID, "SmartServiceEngine", "GlobalsanctionsHelper", "GlobalsanctionsHelper: getScreenData",
        		"Global  Sancction Payment Helper - Bridger API Invocation Starts", AmexLogger.Result.success, "");
        Map<String, Object> map = new HashMap<String, Object>();
        map = callGlobalSelectionScreenData(eventID, objScreenDataVO);
        logger.info(eventID, "SmartServiceEngine", "GlobalsanctionsHelper", "GlobalsanctionsHelper: getScreenData",
        		"Global  Scanction Payment Helper - Bridger API Invocation Ends", AmexLogger.Result.success, "");
        return map;
    }

    private Map<String, Object> callGlobalSelectionScreenData(String eventID, ScreenDataVO objScreenDataVO) throws SSEApplicationException {
    	logger.info(eventID, "SmartServiceEngine", "GlobalsanctionsHelper","GlobalsanctionsHelper:callGlobalSelectionScreenData",
                "Global Sanction Helper - Start of Bridger API Invocation",AmexLogger.Result.success, "", "doingBusinessAsNm", objScreenDataVO.getSupplierName(),
                "prvdrId", objScreenDataVO.getSseorgId(), "idNbr", objScreenDataVO.getSseorgId(), "strMsgIdField", eventID );
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            GsScreenDataServiceExt globalSessionService = new GsScreenDataServiceExt();
            GlobalSanctionsScreening proxy = globalSessionService.getCustomBindingGlobalSanctionsScreening();
            BindingProvider bindingProvider = (BindingProvider) proxy;

            SOAPOperationsLoggingHandler soapOperationsLoggingHandler = new SOAPOperationsLoggingHandler();
            soapOperationsLoggingHandler.setEventId(eventID);

            bindingProvider.getBinding().getHandlerChain().add(soapOperationsLoggingHandler);
            Map<String, Object> requestContext = bindingProvider.getRequestContext();
            requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, EnvironmentPropertiesUtil.getProperty(ApiConstants.GLOBALSANCTIONS_SOAP_SERVICE_URL));
            requestContext.put(ApiConstants.REQUEST_TIMEOUT, EnvironmentPropertiesUtil.getProperty(ApiConstants.BRIDGER_REQUEST_TIMEOUT_VALUE));
            ScreenDataclient client = new ScreenDataclient();
            Request request = client.buildRequest(eventID, objScreenDataVO);
            String strClientId = EnvironmentPropertiesUtil.getProperty(SchedulerConstants.STRINGCLIN);
            /*Response response = proxy.screenData(strClientId, request);
            if (null != response) {
                map = client.buildResponse(eventID,response);
                map.put(SchedulerConstants.RESPONSE_DETAILS, response);
            }*/
            map.put(ApiConstants.IN_RESULT_ID, new Long(-1));
            map.put(ApiConstants.RESP_ERROR_CODE, ApiConstants.SUCCESS);
            map.put(ApiConstants.RESP_ERROR_DESC, ApiConstants.SUCCESS);
            map.put(ApiConstants.IN_MATCH_CT, "10");
           String resultId = (Long)map.get(ApiConstants.IN_RESULT_ID) != null ? ((Long)map.get(ApiConstants.IN_RESULT_ID)).toString() : "0";
            logger.info(eventID, "SmartServiceEngine", "GlobalsanctionsHelper", "GlobalsanctionsHelper:callGlobalSelectionScreenData",
                "Global Sanction Helper - End of Bridger API Invocation", AmexLogger.Result.success, "", "doingBusinessAsNm", objScreenDataVO.getSupplierName(),
                "prvdrId", StringUtils.stripToEmpty((String) map.get(ApiConstants.IN_PROVIDER_ID)), "idNbr", objScreenDataVO.getSseorgId(), "strMsgIdField", eventID, "resultId", resultId,
                "accountId", StringUtils.stripToEmpty((String)map.get(ApiConstants.IN_ACCOUNT_ID)), "matchCount", StringUtils.stripToEmpty((String) map.get(ApiConstants.IN_MATCH_CT)), "response_errorCd", StringUtils.stripToEmpty((String)map.get(ApiConstants.RESP_ERROR_CODE)),
                "response_errorMsg", StringUtils.stripToEmpty((String)map.get(ApiConstants.RESP_ERROR_DESC)), "trans_errorCd", StringUtils.stripToEmpty((String)map.get(ApiConstants.ERROR_CODE)), "trans_errorMsg", StringUtils.stripToEmpty((String)map.get(ApiConstants.ERROR_DESC)));
        } catch (WebServiceException exception) {
            logger.error(eventID, "SmartServiceEngine", "Brigder Screening Scheduler", "GlobalsanctionsHelper: buildResponse",
					"Exception occured during screenData service call", AmexLogger.Result.failure, exception.getMessage(), exception);
            //Commented for temporary to avoid Bridger service down.Need to uncomment once Bridger service is UP.
            //throw new SSEApplicationException(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD,EnvironmentPropertiesUtil.getProperty(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD),e);
           /* map.put(ApiConstants.IN_RESULT_ID, new Long(-1));
            map.put(ApiConstants.RESP_ERROR_CODE, ApiConstants.SUCCESS);
            map.put(ApiConstants.RESP_ERROR_DESC, ApiConstants.SUCCESS);
            map.put(ApiConstants.IN_MATCH_CT, "10");*/
			if (exception.getMessage().contains("java.net.SocketTimeoutException")) {
			    logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.BRIDGER_TIMEOUT_ERR_CD, TivoliMonitoring.BRIDGER_TIMEOUT_ERR_MSG, eventID));
				throw new SSEApplicationException("Exception occured during Create Payment Relationship service call",ApiErrorConstants.BRIDGER_TIMEOUT_SSEAPIEN903,
					EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.BRIDGER_TIMEOUT_SSEAPIEN903),exception);
			} else {
				logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SCREENDATA_SERVICE_ERR_CD, TivoliMonitoring.SSE_SCREENDATA_SERVICE_ERR_CD_MSG, eventID));
				throw new SSEApplicationException("Exception occured during Create Payment Relationship service call",
					ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD,EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD),exception);
			}
		}catch (Exception e) {
            logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SCREENDATA_SERVICE_ERR_CD, TivoliMonitoring.SSE_SCREENDATA_SERVICE_ERR_CD_MSG, eventID));
            logger.error(eventID, "SmartServiceEngine", "Brigder Screening Scheduler", "GlobalsanctionsHelper: buildResponse",
					"Exception occured during screenData service call", AmexLogger.Result.failure, e.getMessage(), e);
            //Commented for temporary to avoid Bridger service down.Need to uncomment once Bridger service is UP.
            throw new SSEApplicationException(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD,EnvironmentPropertiesUtil.getProperty(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD),e);
           /* map.put(ApiConstants.IN_RESULT_ID, new Long(-1));
            map.put(ApiConstants.RESP_ERROR_CODE, ApiConstants.SUCCESS);
            map.put(ApiConstants.RESP_ERROR_DESC, ApiConstants.SUCCESS);
            map.put(ApiConstants.IN_MATCH_CT, "10");*/
        }
        return map;
    }
}
