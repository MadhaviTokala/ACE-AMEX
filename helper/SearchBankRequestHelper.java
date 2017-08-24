package com.americanexpress.smartserviceengine.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.ws.BindingProvider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.gtt.fxip.handler.ext.FXIPPaymentWebServiceVs1Ext;
import com.americanexpress.gtt.fxip.ws.IPaymentWebService;
import com.americanexpress.gtt.fxip.ws.PaymentWebserviceFault;
import com.americanexpress.gtt.fxip.ws.pwsfault.PWSError;
import com.americanexpress.gtt.fxip.ws.pwsfault.PWSErrors;
import com.americanexpress.gtt.fxip.ws.searchbanks.BanksReq;
import com.americanexpress.gtt.fxip.ws.searchbanks.BanksRes;
import com.americanexpress.gtt.fxip.ws.searchbanks.SearchBanksRequestObj;
import com.americanexpress.smartserviceengine.client.SearchBankServiceClient;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.ApiErrorConstants;
import com.americanexpress.smartserviceengine.common.exception.SSEApplicationException;
import com.americanexpress.smartserviceengine.common.payload.SearchBankRequestData;
import com.americanexpress.smartserviceengine.common.util.EnvironmentPropertiesUtil;
import com.americanexpress.smartserviceengine.common.util.SOAPOperationsLoggingHandler;
import com.americanexpress.smartserviceengine.common.util.ThreadLocalManager;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;

@Service
public class SearchBankRequestHelper {
	
    private static AmexLogger logger = AmexLogger.create(SearchBankRequestHelper.class);

    @Autowired
    private TivoliMonitoring tivoliMonitoring;
 
    public Map<String,Object> searchBank(SearchBankRequestData requestWrapper, String apiMsgId) throws SSEApplicationException  {
    	 Map<String, Object> map = new HashMap<String, Object>();
    	  logger.info(apiMsgId,"SmartServiceEngine","SearchBankRequestHelper","SearchBankRequestHelper:searchBank",
    	            "SearchBank service - Start",AmexLogger.Result.success, "");
         map = callSearchBankService(requestWrapper, apiMsgId);// Calling the Search Bank service
         logger.info(apiMsgId,"SmartServiceEngine","SearchBankRequestHelper","SearchBankRequestHelper:searchBank",
 	            "SearchBank service - End",AmexLogger.Result.success, "");
    	return map;
    }


	private Map<String, Object> callSearchBankService(SearchBankRequestData requestWrapper, String apiMsgId) throws SSEApplicationException {
        Map<String, Object> map = new HashMap<String, Object>();
        final long startTimeStamp = System.currentTimeMillis();
        boolean isError = false;
        logger.info(apiMsgId,"SmartServiceEngine","Search Bank API","SearchBankRequestHelper: callSearchBankService: Start",
                "Invoking Search Bank service", AmexLogger.Result.success, "");
		try{
        FXIPPaymentWebServiceVs1Ext fxipPaymentService=new FXIPPaymentWebServiceVs1Ext();
        IPaymentWebService proxy=fxipPaymentService.getPaymentWebServicePort();
        BindingProvider bindingProvider = (BindingProvider) proxy;
        bindingProvider.getBinding().getHandlerChain().add(new SOAPOperationsLoggingHandler());
        Map<String, Object> requestContext = bindingProvider.getRequestContext();
        requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, EnvironmentPropertiesUtil.getProperty(ApiConstants.FXP_SERVICE_URL));
        logger.info(apiMsgId,"SmartServiceEngine","search bank", "SearchBankRequestHelper: callSearchBankService",
                "Before calling search bank service",AmexLogger.Result.success, "FXP soap Endpoint URL",EnvironmentPropertiesUtil.getProperty(ApiConstants.FXP_SERVICE_URL));

        SearchBankServiceClient client=new SearchBankServiceClient();
        SearchBanksRequestObj reqType = client.buildRequestType(requestWrapper);
        BanksReq searchBankInfoType = client.buildSearchBankInfoType(reqType);
        BanksRes respType = proxy.searchBanksOperation(searchBankInfoType);
        if (null == respType) {
        	 logger.error(apiMsgId,"SmartServiceEngine","Search Bank","SearchBankRequestHelper: callSearchBankService",
                     "After invoking callSearchBankService",AmexLogger.Result.failure,"Null response from SearchBankService API", "");
                 logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SEARCH_BANK_ERR_CD, TivoliMonitoring.SEARCH_BANK_ERR_MSG, apiMsgId));
                 throw new SSEApplicationException("Null response from searchBank API", ApiErrorConstants.SEARCH_BANK_INTERNAL_SERVER_ERR_CD,
                     EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.SEARCH_BANK_INTERNAL_SERVER_ERR_CD));
            
        }
        logger.info(apiMsgId,"SmartServiceEngine","Search bank","SearchBankRequestHelper: callSearchBankService",
                "After calling callSearchBankService ",AmexLogger.Result.success, "", "respTypeHolder: ",respType.getBanksResObj().getMsgResponse().getMsgResponseCode());
        map.put(ApiConstants.RESPONSE_DETAILS, respType);
		}catch(PaymentWebserviceFault ex){
			isError=true;
			String sseRespCode = null;
            String sseRespDesc = null;
			PWSErrors errors=ex.getFaultInfo();
			List<PWSError> errorsList=new ArrayList<PWSError>();
			errorsList=errors.getErrorList();
			PWSError error=new PWSError();
			error=errorsList.get(0);
			String errorCode=error.getErrorCode();
			String description=error.getDescription();
			if(errorCode!=null){
				 sseRespCode = EnvironmentPropertiesUtil.getProperty(errorCode);
				 if(sseRespCode!=null){
	                    sseRespDesc = EnvironmentPropertiesUtil.getProperty(sseRespCode);
				 }else{
					sseRespCode = ApiErrorConstants.SEARCH_BANK_INTERNAL_SERVER_ERR_CD;
                    sseRespDesc = EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.SEARCH_BANK_INTERNAL_SERVER_ERR_CD)+ ": " + description;
                    logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SEARCH_BANK_ERR_CD, TivoliMonitoring.SEARCH_BANK_ERR_MSG, apiMsgId));
				 }
			} else {
                sseRespCode = ApiErrorConstants.SEARCH_BANK_INTERNAL_SERVER_ERR_CD;
                sseRespDesc = EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.SEARCH_BANK_INTERNAL_SERVER_ERR_CD);
                logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SEARCH_BANK_ERR_CD, TivoliMonitoring.SEARCH_BANK_ERR_MSG, apiMsgId));
            }
			
			logger.error(apiMsgId,"SmartServiceEngine","Process FXP Payment API", "SearchBankRequestHelper: callSearchBankService",
	                "SOAP Fault Error occured during Search Bank service invocation", AmexLogger.Result.failure, "", ex, 
	                "SSE Response Code", sseRespCode, "SSE Response Desc", sseRespDesc);
			throw new SSEApplicationException("SOAP Fault Error occured during Search Bank service invocation", sseRespCode, sseRespDesc, ex);
		}
		 catch (SSEApplicationException ex) {
				isError=true;
	            throw ex;
        }catch (Exception ex) {
			isError=true;
            logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SEARCH_BANK_ERR_CD, TivoliMonitoring.SEARCH_BANK_ERR_MSG, apiMsgId));
            logger.error(apiMsgId, "SmartServiceEngine", "search bank", "SearchBankRequestHelper: callSearchBankService",
                "Exception occured during Search Bank service invocation", AmexLogger.Result.failure, ex.getMessage(), ex);
            throw new SSEApplicationException("Exception occured during Search Bank service invocation", ApiErrorConstants.SEARCH_BANK_INTERNAL_SERVER_ERR_CD,
                EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.SEARCH_BANK_INTERNAL_SERVER_ERR_CD),ex);
        } finally {
            final long endTimeStamp = System.currentTimeMillis();
            ThreadLocalManager.getRequestStatistics().put("Search Bank service", (isError?"F:":"S:")+(endTimeStamp-startTimeStamp) +" ms");
            logger.info(apiMsgId,"SmartServiceEngine","search bank","SearchBankRequestHelper: callSearchBankService",
                "After Search Bank service invocation", AmexLogger.Result.success, "", "Total Time Taken to get response from search bank SOAP service",
                (endTimeStamp-startTimeStamp)+ " milliseconds("+(endTimeStamp-startTimeStamp)/1000.00+" seconds)");
        }
		 logger.info(apiMsgId,"SmartServiceEngine","Search Bank API","SearchBankRequestHelper: callSearchBankService: End",
	                "Invoking Search Bank service", AmexLogger.Result.success, "");
		return map;
	}
}
