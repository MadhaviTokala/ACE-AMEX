package com.americanexpress.smartserviceengine.service.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.americanexpress.ace.security.helper.SecurityHelper;
import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.payve.paymentmanagementservice.v1.getpaymentdetails.GetPaymentRespWrapperVo;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.ApiErrorConstants;
import com.americanexpress.smartserviceengine.common.constants.SchedulerConstants;
import com.americanexpress.smartserviceengine.common.constants.SchedulerErrorConstants;
import com.americanexpress.smartserviceengine.common.exception.SSEApplicationException;
import com.americanexpress.smartserviceengine.common.util.DateTimeUtil;
import com.americanexpress.smartserviceengine.common.util.EnvironmentPropertiesUtil;
import com.americanexpress.smartserviceengine.common.util.GenericUtilities;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.vo.PayRequestVO;
import com.americanexpress.smartserviceengine.common.vo.PayResponseVO;
import com.americanexpress.smartserviceengine.dao.PaymentDAO;
import com.americanexpress.smartserviceengine.dao.UpdatePaymentDAO;
import com.americanexpress.smartserviceengine.helper.PaymentInfoRequestHelper;
import com.americanexpress.smartserviceengine.service.PaymentStatusService;

/**
 * @author vishwakumar_c
 *
 */
public class PaymentStatusServiceImpl implements PaymentStatusService {

    AmexLogger logger = AmexLogger.create(PaymentStatusServiceImpl.class);

    @Autowired
    private PaymentDAO paymentDAO;
    @Autowired
    private UpdatePaymentDAO updatePaymentDAO;
    @Autowired
    private TivoliMonitoring tivoliMonitoring;
    @Autowired
    private PaymentInfoRequestHelper pymtInfoRequestHelper;

    @Override
    public void getPaymentData(String eventId) throws SSEApplicationException {
        try {
            int failedPaymentCount = 0;
            int inProgressPaymentCount = 0;
            logger.info(eventId, "SmartServiceEngine", "PaymentStatusServiceImpl", "getPaymentData",
                "Start of  PaymentStatusService", AmexLogger.Result.success, "");

            List<PayRequestVO> payDataResultSet = null;
            Map<String, String> inMap = new HashMap<String, String>();
            logger.debug(eventId, "SmartServiceEngine", "PaymentStatusServiceImpl", "getPaymentData",
                "Start of Get Payment Details SP E3GMR027", AmexLogger.Result.success, "");
            Map<String, Object> outMap = paymentDAO.execute((HashMap) inMap, eventId);

            if (outMap != null
                    && !outMap.isEmpty()
                    && outMap.get(SchedulerConstants.RESP_CD) != null
                    && SchedulerConstants.RESPONSE_CODE_SUCCESS.equalsIgnoreCase(outMap.get(SchedulerConstants.RESP_CD)
                        .toString().trim())) {

                payDataResultSet = (List<PayRequestVO>) outMap.get(SchedulerConstants.RESULT_SET);
                String resultCode = outMap.get("RESP_CD").toString().trim();
                String resultMessage = outMap.get("RESP_MSG").toString().trim();
                String resultParam = outMap.get("SQLCODE_PARM").toString().trim();

                logger.info(eventId, "SmartServiceEngine", "PaymentStatusServiceImpl", "getPaymentData",
                    "End of Get Payment Details SP E3GMR027", AmexLogger.Result.success, "Success Response from SP",
                    SchedulerConstants.RESP_CD, resultCode, SchedulerConstants.RESP_MSG, resultMessage,
                    SchedulerConstants.SQLCODE_PARM, resultParam);



            } else {
                logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_027_ERR_CD, TivoliMonitoring.SSE_SP_027_ERR_MSG, eventId));
                logger.info(eventId, "SmartServiceEngine", "PaymentStatusServiceImpl", "getPaymentData",
                    "End of Get Payment Details SP E3GMR027", AmexLogger.Result.failure,
                        "outMap is null or error resp_code from SP");
            }

            if (payDataResultSet != null && !payDataResultSet.isEmpty()) {
                logger.info(eventId, "SmartServiceEngine", "PaymentStatusServiceImpl", "getPaymentData",
                    "Check resultset map for SP E3GMR027", AmexLogger.Result.success, "", "Payment ResultSet Size:",
                    new Integer(payDataResultSet.size()).toString(), "Payment ResultSet:", payDataResultSet.toString());
                Iterator<PayRequestVO> iterator = payDataResultSet.iterator();




                while (iterator.hasNext()) {
                    PayRequestVO payDataDetails = iterator.next();


                    logger.info(eventId, "SmartServiceEngine", "PaymentStatusServiceImpl", "getPaymentData",
                        "logging payment info for splunk", AmexLogger.Result.success, "Checking for payment status",
                        "buyerPymtRefId", payDataDetails.getBipPaymentReferenceId(), "bigOrgId", payDataDetails.getBuyerId(),
                        "PaymentSubmissionDate", payDataDetails.getPaymentSubmissionDate()," getPartnerPaymentId ",payDataDetails.getPartnerPaymentId());

if(StringUtils.isNotBlank(payDataDetails.getPartnerPaymentId()))
{
	try
	{
		logger.debug(eventId,"SmartServiceEngine", "PaymentStatusServiceImpl", "getPaymentData", "Partner Payment Id: "+payDataDetails.getPartnerPaymentId(),
				AmexLogger.Result.success, " BIP rest call for partner payment id started");

		String serviceURI=EnvironmentPropertiesUtil.getProperty(ApiConstants.BIP_REST_SERVICE_GET_PAYMENT_URI)+"/"+payDataDetails.getPartnerPaymentId();
		String serviceURL=EnvironmentPropertiesUtil.getProperty(ApiConstants.BIP_REST_SERVICE_GET_PAYMENT_URL)+""+payDataDetails.getPartnerPaymentId();
		
		ResponseEntity<String> responseWrapperVO = generateRestTemplate(ApiConstants.HTTPMETHODGET,serviceURI,serviceURL,eventId);
		
		GetPaymentRespWrapperVo respWrapperObj=null;
		if(null != responseWrapperVO)
		{
			String responseEntity=responseWrapperVO.getBody();
			respWrapperObj = GenericUtilities.jsonToJava(responseEntity, GetPaymentRespWrapperVo.class);
			String paymentStatusDescription="";
			
			if(responseWrapperVO.getStatusCode()==HttpStatus.OK)
			{
				paymentStatusDescription = getPaymentStatusDescription(respWrapperObj.getPayment_status_cd());	
				
				logger.debug(eventId, "SmartServiceEngine", "PaymentStatusServiceImpl", "getPaymentData",
	                        "logging payment info for splunk", AmexLogger.Result.success, " BIP rest call got success for ", " partnerPaymentId: "+payDataDetails.getPartnerPaymentId(),
	                        "buyerPymtRefId", payDataDetails.getBipPaymentReferenceId(), "bigOrgId", payDataDetails.getBuyerId(),
	                        "PaymentSubmissionDate", payDataDetails.getPaymentSubmissionDate()," getPartnerPaymentId ",payDataDetails.getPartnerPaymentId()," paymentStatusDescription ", paymentStatusDescription);
				 
				if(SchedulerConstants.STATUSDESC_INPROGRESS.equals(paymentStatusDescription))
				{				 
				    String paymentProcessPeriod = EnvironmentPropertiesUtil.getProperty(SchedulerConstants.PAYMENT_PROCESS_PERIOD);
                    int pymtProcessPeriod = Integer.parseInt(paymentProcessPeriod);
                    String pymtSubmissionDate = payDataDetails.getPaymentSubmissionDate();

                    if (DateTimeUtil.checkPaymentProcessTime(pymtSubmissionDate, pymtProcessPeriod)) {
                        //INPR status from the BIP - Exceeded time period - Raise Tivoli alert
                        logger.info(eventId, "SmartServiceEngine", "PaymentStatusServiceImpl", "getPaymentData", "Failure Response from BIP Payment Status Service.",
                            AmexLogger.Result.failure, "In-progress status from BIP Payment Status Service for buyer payment id: "+ respWrapperObj.getBuyer_payment_id(),
                            "payment entity id: ",respWrapperObj.getPayment_entity_id());
                        inProgressPaymentCount = inProgressPaymentCount + 1;
                    }
                }
			
			
	            Map<String, Object> inResMap = new HashMap<String, Object>();
	            inResMap.put(SchedulerConstants.IN_PRTR_PYMT_ID,payDataDetails.getPartnerPaymentId());
	            inResMap.put(SchedulerConstants.IN_PYMT_SYS_RESP_DS, paymentStatusDescription);//respWrapperObj.getVendor_id());
	            inResMap.put(SchedulerConstants.IN_BUY_PYMT_STA , paymentStatusDescription);
	            inResMap.put(SchedulerConstants.IN_PYMT_SYS_RESP_CD, respWrapperObj.getPayment_status_cd());
	            inResMap.put(SchedulerConstants.IN_SRCE_ORG_ID, SchedulerConstants.CHAR_SPACE);
	            inResMap.put(SchedulerConstants.IN_BUY_PYMT_ID,  SchedulerConstants.CHAR_SPACE);

	            String  respCode = invokeProcedureE3GMR028(inResMap, eventId);
	
	            if(SchedulerConstants.SUC_SSEIN000.equalsIgnoreCase(respCode))
	            {
	            		logger.info(eventId, "SmartServiceEngine", "PaymentStatusServiceImpl", "getPaymentData",
	                        " E3GMR028 status logging for splunk", AmexLogger.Result.success, "updated DB", "partnerPaymentId", payDataDetails.getPartnerPaymentId(),
	                        "buyerPymtRefId", respWrapperObj.getBuyer_payment_id(), "bipOrgId", payDataDetails.getBuyerId(), "buyerPaymentStatus", paymentStatusDescription,
	                        "paymentSystemResponseCode",respWrapperObj.getPayment_status_cd(),"checkNbr",respWrapperObj.getCheck_att().getDelivery_ind(),
	                        "pymtAmt",respWrapperObj.getPayment_amt()+"","paymentMethod",respWrapperObj.getPayment_cmnt());
	            }
	            else
	            {
            		logger.info(eventId, "SmartServiceEngine", "PaymentStatusServiceImpl", "getPaymentData",
                        " E3GMR028 status logging for splunk", AmexLogger.Result.failure, "updated DB", "partnerPaymentId", payDataDetails.getPartnerPaymentId(),
                        "buyerPymtRefId", respWrapperObj.getBuyer_payment_id(), "bipOrgId", payDataDetails.getBuyerId(), "buyerPaymentStatus", paymentStatusDescription,
                        "paymentSystemResponseCode",respWrapperObj.getPayment_status_cd(),"checkNbr",respWrapperObj.getCheck_att().getDelivery_ind(),
	                        "pymtAmt",respWrapperObj.getPayment_amt()+"","paymentMethod",respWrapperObj.getPayment_cmnt());
	            }
		    }
			else
			{
			 
	             String sseRespCode = SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD;
	             String sseRespDesc = EnvironmentPropertiesUtil.getProperty(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD);
	
	             logger.error(eventId, "SmartServiceEngine", "PaymentStatusServiceImpl", "getPaymentData",
	                 "Failure Response from Payment Status rest service. Update Payment SP was not invoked. ",
	                 AmexLogger.Result.failure, "Got Failure Response from Payment Status Service",
	                 "responseCode", sseRespCode, "ResponseDesc", sseRespDesc, "buyer Pymt Id",respWrapperObj.getBuyer_payment_id(),
	                 "bipOrgId", payDataDetails.getBuyerId(), "buyerPaymentStatus", "fail");
	             
	             // failedPaymentCount logic
	             
	             if (responseWrapperVO.getStatusCode()==HttpStatus.NO_CONTENT) 
	             {
                     String paymentSubmissionPeriod = EnvironmentPropertiesUtil.getProperty(SchedulerConstants.PAYMENT_SUBMISSION_PERIOD);
                     if (paymentSubmissionPeriod == null || paymentSubmissionPeriod.isEmpty()) {
                         paymentSubmissionPeriod = "15";
                     }
                     int pymtSubmissionPeriod = Integer.parseInt(paymentSubmissionPeriod);
                     String pymtSubmissionDate = payDataDetails.getPaymentSubmissionDate();

                     if (DateTimeUtil.checkPaymentSubmissionTime(pymtSubmissionDate, pymtSubmissionPeriod)) 
                     {
                         //No Record Found - Exceeded timeperiod - Raise Tivoli alert
                         logger.info(eventId, "SmartServiceEngine", "PaymentStatusServiceImpl", "getPaymentData", "no record found [204] response from BIP Payment Status Service.",
                             AmexLogger.Result.failure, "Got no record found [204] response from BIP Payment Status Service for PymtRefId:","buyerPymtRefId ",payDataDetails.getBipPaymentReferenceId(),
                             "bipOrgId ", payDataDetails.getBuyerId()," partnerPaymentId "+payDataDetails.getPartnerPaymentId()+" paymentSubmissionDate "+payDataDetails.getPaymentSubmissionDate());
                         failedPaymentCount = failedPaymentCount + 1;
                     }
                 }
			}
		}
		else
		{
			logger.debug(eventId,"SmartServiceEngine", "PaymentStatusServiceImpl", "getPaymentData", " partnerPaymentId: "+payDataDetails.getPartnerPaymentId(),
					AmexLogger.Result.failure, " BIP rest call for partner payment id returned no data");			
		}
	
	} catch(SSEApplicationException ex){
		if(ex.getMessage().contains("java.net.SocketTimeoutException")){
			logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.BIP_TIMEOUT_ERR_CD, TivoliMonitoring.BIP_TIMEOUT_ERR_MSG, eventId));                
			logger.error(eventId,"SmartServiceEngine","CheckSupplierRestClientHelper","createCustomer","Time out exception occured in Bip service",
					AmexLogger.Result.failure,"Exception",ex);
       	}else{
		logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_BIP_GET_CHECK_PAYMENT_STA_CD, TivoliMonitoring.SSE_BIP_GET_CHECK_PAYMENT_STA_MSG, eventId));
		logger.error(eventId, "SmartServieEngine", "CheckSupplierRestClientHelper", "createCustomer", "Initiating BIP Enrollment Request Failed with Exception", AmexLogger.Result.failure, 
				"Unexpected Exception while intiating supplier enrollment service", ex);
       	}
	} catch (Exception e) 
	{
	    logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SCHED_ERR_CD, TivoliMonitoring.SSE_SCHED_ERR_MSG, eventId));
	    logger.error(eventId, "SmartServiceEngine", "PaymentStatusServiceImpl", "getPaymentData",
	        "Exception while Calling Payment Status Service - invoke BIP API",
	        AmexLogger.Result.failure, "Failure in getPaymentData", e, "ErrorMsg", e.getMessage());
	}
	
}
else
{
                    try {
                        Map<String, Object> statusMap = new HashMap<String, Object>();
                        statusMap = pymtInfoRequestHelper.getPaymentInfo(payDataDetails, eventId);
                        PayResponseVO response = (PayResponseVO) statusMap.get(SchedulerConstants.RESPONSE_DETAILS);

                        String responseCode = response.getPaymentSystemResponseCode();
                        String responseDesc = response.getPaymentSystemResponseDesc();
                        String explCd = response.getPaymentSystemExplCd();
                        String explDesc =  response.getPaymentSystemExplDesc();

                        logger.info(eventId, "SmartServiceEngine", "PaymentStatusServiceImpl", "getPaymentData", "Response from BIP",
                            AmexLogger.Result.success, "","buyerPymtRefId",payDataDetails.getBipPaymentReferenceId(), "bipOrgId", payDataDetails.getBuyerId(),
                            "responseCode" , responseCode,"responseDesc", responseDesc, "explCode" , explCd, "explDesc", explDesc);
                        if (null != responseCode && SchedulerConstants.SUCCESS_RESP_CD.equals(responseCode)) {

                            if(SchedulerConstants.BIP_STATUS_INPR.equals(response.getBuyerPaymentStatus())){
                                String paymentProcessPeriod = EnvironmentPropertiesUtil.getProperty(SchedulerConstants.PAYMENT_PROCESS_PERIOD);
                                int pymtProcessPeriod = Integer.parseInt(paymentProcessPeriod);
                                String pymtSubmissionDate = payDataDetails.getPaymentSubmissionDate();

                                if (DateTimeUtil.checkPaymentProcessTime(pymtSubmissionDate, pymtProcessPeriod)) {
                                    //INPR status from the BIP - Exceeded time period - Raise Tivoli alert
                                    logger.info(eventId, "SmartServiceEngine", "PaymentStatusServiceImpl", "getPaymentData", "Failure Response from BIP Payment Status Service.",
                                        AmexLogger.Result.failure, "In-progress status from BIP Payment Status Service for PymtRefId:"+ response.getBuyerPaymentRefId(),
                                        "paymentMethod",response.getPymtMthd(), "responseCode",responseCode, "ResponseDesc", responseDesc);
                                    inProgressPaymentCount = inProgressPaymentCount + 1;
                                }
                            }

                            Map<String, Object> inResMap = new HashMap<String, Object>();
                            inResMap.put(SchedulerConstants.IN_BUY_PYMT_ID     , response.getBuyerPaymentRefId());
                            inResMap.put(SchedulerConstants.IN_SRCE_ORG_ID     , response.getBipOrganizationId());                          
                            inResMap.put(SchedulerConstants.IN_BUY_PYMT_STA        , response.getBuyerPaymentStatus());
                            inResMap.put(SchedulerConstants.IN_PYMT_SYS_RESP_CD, response.getPaymentSystemResponseCode());
                            inResMap.put(SchedulerConstants.IN_PYMT_SYS_RESP_DS  , SchedulerConstants.CHAR_SPACE);
                            inResMap.put(SchedulerConstants.IN_PRTR_PYMT_ID      , SchedulerConstants.CHAR_SPACE);

                            String  respCode = invokeProcedureE3GMR028(inResMap, eventId);
                        	
            	            if(SchedulerConstants.SUC_SSEIN000.equalsIgnoreCase(respCode))
            	            {
            	            	 logger.info(eventId, "SmartServiceEngine", "PaymentStatusServiceImpl", "getPaymentData",
                                         "payment status logging for splunk", AmexLogger.Result.success, "updated DB",
                                         "buyerPymtRefId", response.getBuyerPaymentRefId(), "bipOrgId", response.getBipOrganizationId(), "buyerPaymentStatus", response.getBuyerPaymentStatus(),
                                         "paymentSystemResponseCode",response.getPaymentSystemResponseCode(),"pymtStatDt",response.getPymtStatDt(),"checkNbr",response.getCheckNbr(),
                                         "pymtAmt",response.getPymtAmt(),"paymentMethod",response.getPymtMthd());
            	            }
                          
                        }else {

                            String sseRespCode = null;
                            String sseRespDesc = null;

                            // Business Error response from BIP getPaymentDetails SOAP API
                            if (explCd != null) {
                                /*
                                 * Get the corresponding SSE response code and response desc
                                 * from Properties files
                                 */
                                sseRespCode = EnvironmentPropertiesUtil.getProperty(explCd);
                                if (sseRespCode != null) {
                                    sseRespDesc = EnvironmentPropertiesUtil
                                            .getProperty(sseRespCode);

                                } else {

                                    sseRespCode = SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD;
                                    sseRespDesc = EnvironmentPropertiesUtil
                                            .getProperty(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD);

                                }
                            } else {
                                sseRespCode = SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD;
                                sseRespDesc = EnvironmentPropertiesUtil
                                        .getProperty(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD);
                            }


                            logger.error(eventId, "SmartServiceEngine", "PaymentStatusServiceImpl", "getPaymentData",
                                "Failure Response from Payment Status Service. Update Payment SP was not invoked. ",
                                AmexLogger.Result.failure, "Got Failure Response from Payment Status Service",
                                "responseCode", sseRespCode, "ResponseDesc", sseRespDesc, "ExplCode" ,explCd, "ExplDesc", explDesc,"buyerPymtRefId",payDataDetails.getBipPaymentReferenceId(),
                                "bipOrgId", payDataDetails.getBuyerId(), "buyerPaymentStatus", "fail");
                            /*
                             * When BIP Payment inquiry service returns "No Record Found", if it exceeds the payment submission timestamp
                             * and the processing time, raise an Tivoli alert
                             *
                             */
                            if (SchedulerConstants.ERROR_RESP_CD.equals(responseCode) && explCd.contains(SchedulerConstants.EXPL_CD_NO_REC_FOUND)) {
                                String paymentSubmissionPeriod = EnvironmentPropertiesUtil.getProperty(SchedulerConstants.PAYMENT_SUBMISSION_PERIOD);
                                if (paymentSubmissionPeriod == null || paymentSubmissionPeriod.isEmpty()) {
                                    paymentSubmissionPeriod = "15";
                                }
                                int pymtSubmissionPeriod = Integer.parseInt(paymentSubmissionPeriod);
                                String pymtSubmissionDate = payDataDetails.getPaymentSubmissionDate();

                                if (DateTimeUtil.checkPaymentSubmissionTime(pymtSubmissionDate, pymtSubmissionPeriod)) {
                                    //No Record Found - Exceeded timeperiod - Raise Tivoli alert
                                    logger.info(eventId, "SmartServiceEngine", "PaymentStatusServiceImpl", "getPaymentData", "Failure Response from BIP Payment Status Service.",
                                        AmexLogger.Result.failure, "Got Failure Response from BIP Payment Status Service for PymtRefId:","buyerPymtRefId",payDataDetails.getBipPaymentReferenceId(),
                                        "bipOrgId", payDataDetails.getBuyerId(),"responseCode", sseRespCode, "ResponseDesc", sseRespDesc);
                                    failedPaymentCount = failedPaymentCount + 1;
                                }
                            }
                        }
                    } catch (SSEApplicationException ex) {
                        String responseCode = ex.getResponseCode();
                        logger.error(eventId, "SmartServiceEngine", "PaymentStatusServiceImpl", "getPaymentData",
                            "SSE Application Exception while Calling Payment Status Service - invoke BIP API",
                            AmexLogger.Result.failure, "SSEApplicationException in Payment Status Service", ex,
                            "ErrorMsg", ex.getMessage(), "responseCode", responseCode, "responseDesc", ex.getResponseDescription());
                    } catch (Exception e) {
                        logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SCHED_ERR_CD, TivoliMonitoring.SSE_SCHED_ERR_MSG, eventId));
                        logger.error(eventId, "SmartServiceEngine", "PaymentStatusServiceImpl", "getPaymentData",
                            "Exception while Calling Payment Status Service - invoke BIP API",
                            AmexLogger.Result.failure, "Failure in getPaymentData", e, "ErrorMsg", e.getMessage());
                    }
}// end of else condition
                }
                if(failedPaymentCount > 0){
                    logger.error(tivoliMonitoring.logStatement(String.valueOf(failedPaymentCount), TivoliMonitoring.GET_PYMT_DTLS_NOT_FOUND_ERR_CD,TivoliMonitoring.GET_PYMT_DTLS_NOT_FOUND_ERR_MSG,eventId));
                }
                if(inProgressPaymentCount > 0){
                    logger.error(tivoliMonitoring.logStatement(String.valueOf(inProgressPaymentCount), TivoliMonitoring.GET_PYMT_DTLS_NOT_PROCESSED_ERR_CD,TivoliMonitoring.GET_PYMT_DTLS_NOT_PROCESSED_ERR_MSG,eventId));
                }
            } else {
                logger.info(eventId, "SmartServiceEngine", "PaymentStatusServiceImpl", "getPaymentData",
                    "Check resultset map for SP E3GMR027", AmexLogger.Result.failure, "Result set is null");
            }
            logger.info(eventId, "SmartServiceEngine", "PaymentStatusServiceImpl", "getPaymentData",
                "End of  PaymentStatusService", AmexLogger.Result.success, "");
        } catch (Exception e) {
            logger.error(eventId, "SmartServiceEngine", "PaymentStatusServiceImpl", "getPaymentData",
                "Exception while processing Payment Status Service call", AmexLogger.Result.failure,
                "Failure in getPaymentData", e, "ErrorMsg", e.getMessage());
            throw new SSEApplicationException(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD,
                EnvironmentPropertiesUtil.getProperty(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD), e);
        }
    }

	@javax.annotation.Resource
	private SecurityHelper securityHelper;
	
	private ResponseEntity<String> generateRestTemplate(String httpMethod, String serviceURI,
			String serviceURL,String apiMsgID) throws Exception {
		logger.info(apiMsgID, "SmartServiceEngine", "PaymentStatusServiceImpl", "generateRestTemplate", " invoking BIP payment call start", 
				 AmexLogger.Result.success, "");
		ResponseEntity<String> result = null;
		try{
			String port=EnvironmentPropertiesUtil.getProperty(ApiConstants.BIP_REST_SERVICE_PORT);
			String host=EnvironmentPropertiesUtil.getProperty(ApiConstants.BIP_REST_SERVICE_HOST);
			String clientId=EnvironmentPropertiesUtil.getProperty(ApiConstants.BIP_REST_SERVICE_CLIENT_ID);
			String secretKey=EnvironmentPropertiesUtil.getProperty(ApiConstants.BIP_REST_SERVICE_SECRET_KEY);
			String jsonPayload="";
			String	authenticationHeader = securityHelper.getAuthenticationHeader(serviceURL, serviceURI, jsonPayload, 
				secretKey, clientId, host, port,httpMethod);
			//System.setProperty("https.protocols", "TLSv1.1,TLSv1.2");
				HttpHeaders headers = new HttpHeaders();
				headers.set(ApiConstants.AUTHORIZATION_HEADER, authenticationHeader);
				headers.set(ApiConstants.AMEX_API_KEY,clientId);
				headers.setAccept(Arrays.asList(org.springframework.http.MediaType.APPLICATION_JSON));
				RestTemplate rest=new RestTemplate();
				HttpEntity<String> entity = new HttpEntity(jsonPayload, headers);
				logger.debug(apiMsgID, "SmartServiceEngine", "PaymentStatusServiceImpl", "generateRestTemplate", "Request ", AmexLogger.Result.success, 
							"Rest HTTP GET request Completed Successfully", "Json request", jsonPayload," for url ",serviceURL);
				((SimpleClientHttpRequestFactory)rest.getRequestFactory()).setReadTimeout(Integer.parseInt(EnvironmentPropertiesUtil.getProperty(ApiConstants.BIP_REQUEST_TIMEOUT_VALUE))*1000);
	                        ((SimpleClientHttpRequestFactory)rest.getRequestFactory()).setConnectTimeout(Integer.parseInt(EnvironmentPropertiesUtil.getProperty(ApiConstants.BIP_REQUEST_TIMEOUT_VALUE))*1000);
	                        
				result = rest.exchange(serviceURL, HttpMethod.GET, entity, String.class);
				
				logger.debug(apiMsgID, "SmartServiceEngine", "PaymentStatusServiceImpl", "generateRestTemplate", "request_Completed", AmexLogger.Result.success, 
                                    "Rest HTTP Post request Completed Successfully", "Json Response", result.toString());
				
		}catch ( HttpClientErrorException e) {
	        String responseBody=e.getResponseBodyAsString();
	        logger.debug(apiMsgID,"SmartServiceEngine","PaymentStatusServiceImpl","generateRestTemplate","Failure from BIP Get Status for payment enrollment",AmexLogger.Result.failure, "","Response from BIP",responseBody);
	        GetPaymentRespWrapperVo getPaymentRespWrapperVo=null;
	        if(StringUtils.isNotBlank(responseBody)){
	        	getPaymentRespWrapperVo = GenericUtilities.jsonToJava(responseBody, GetPaymentRespWrapperVo.class);
		    }
			throw new SSEApplicationException(e.getMessage(),getPaymentRespWrapperVo.getError_cd(),getPaymentRespWrapperVo.getError_desc(),e);
	    }catch(Exception e){
	    	if(e.getMessage().contains("java.net.SocketTimeoutException")){
				throw new SSEApplicationException(e.getMessage(),ApiErrorConstants.BIP_TIMEOUT_SSEAPIEN901,ApiErrorConstants.BIP_TIMEOUT_SSEAPIEN901,e);
		       	}
	    	logger.error(apiMsgID, "SmartServiceEngine", "PaymentStatusServiceImpl", "generateRestTemplate",
	                "Exception occured during Payment status inquiry API service call", AmexLogger.Result.failure, e.getMessage(), e);
	    }
		logger.debug(apiMsgID, "SmartServiceEngine", "PaymentStatusServiceImpl", "generateRestTemplate", "Request Completed", AmexLogger.Result.success, 
				"Rest HTTP GET request Completed Successfully", "Json Response", result.toString());
			return result;
	}

	private String getPaymentStatusDescription(String status)
	{
		String paymentStatusDescription="";
		/*
		payment_status_cd and payment status description
		0-In Progress
		1-Errors found
		2-Failed
		3-Cancelled
		4-Scheduled
		5-Settled
		6-Reversed
		7-Mailed
		8-Rejected
		9-Print Complete
		10-Held
		11-Print in Progress
		12-Unknown status
		13-Verification Hold
		14-Verification Processing
		*/
		if(StringUtils.isNotBlank(status))
		{
		switch(Integer.parseInt(status))
	      {
	         case 0 :
	        	 	paymentStatusDescription="In Progress"; 
	        	 	break;
	         case 1 :
        	 		paymentStatusDescription="Errors Found"; 
        	 		break;
	         case 2 :
        	 		paymentStatusDescription="Failed"; 
        	 		break;
	         case 3 :
        	 		paymentStatusDescription="Cancelled"; 
        	 		break;
	         case 4 :
        	 		paymentStatusDescription="Scheduled"; 
        	 		break;
	         case 5 :
        	 		paymentStatusDescription="Settled"; 
        	 		break;
	         case 6 :
        	 		paymentStatusDescription="Reversed"; 
        	 		break;
	         case 7 :
        	 		paymentStatusDescription="Mailed"; 
        	 		break;
	         case 8 :
        	 		paymentStatusDescription="Rejected"; 
        	 		break;
	         case 9 :
        	 		paymentStatusDescription="Print Complete"; 
        	 		break;
	         case 10 :
        	 		paymentStatusDescription="Held"; 
        	 		break;
	         case 11 :
        	 		paymentStatusDescription="Print In Progress"; 
        	 		break;
	         case 12 :
        	 		paymentStatusDescription="Unknown status"; 
        	 		break;
	         case 13 :
        	 		paymentStatusDescription="Verification Hold"; 
        	 		break;
	         case 14 :
        	 		paymentStatusDescription="Verification Processing"; 
        	 		break;
	         
	      }
		}
		return paymentStatusDescription;
	}
	
	private String invokeProcedureE3GMR028(Map<String, Object> inResMap, String eventId )
	{		
         Map<String, Object> outResMap = updatePaymentDAO.execute(inResMap, eventId);
         
         String respCd= null;
         if(outResMap != null && !outResMap.isEmpty()){
              respCd = StringUtils.stripToEmpty((String) outResMap.get(SchedulerConstants.RES_CODE));
             String sqlCd = StringUtils.stripToEmpty((String) outResMap.get(SchedulerConstants.SQL_CODE));
             String respDesc = StringUtils.stripToEmpty((String) outResMap.get(SchedulerConstants.RES_MSSG));

             if(SchedulerConstants.SUC_SSEIN000.equalsIgnoreCase(respCd)){
                 logger.info(eventId, "SmartServiceEngine", "PaymentStatusServiceImpl", "getPaymentData",
                     "UpdatePaymentDAO - SP E3GMR028 is Successful", AmexLogger.Result.success, "",
                     SchedulerConstants.SQLCODE_PARM, sqlCd, "resp_code", respCd, "resp_msg", respDesc);

                 /*logger.info(eventId, "SmartServiceEngine", "PaymentStatusServiceImpl", "getPaymentData",
                     "payment status logging for splunk", AmexLogger.Result.success, "updated DB",
                     "buyerPymtRefId", respWrapperObj.getBuyer_payment_id(), "bipOrgId", respWrapperObj.getVendor_id(), "buyerPaymentStatus", respWrapperObj.getBuyer_account_ind(),
                     "paymentSystemResponseCode",respWrapperObj.getPayment_status_cd(),"pymtStatDt",respWrapperObj.getPayment_status_cd(),"checkNbr",respWrapperObj.getCheck_att().getDelivery_ind(),
                     "pymtAmt",respWrapperObj.getPayment_amt()+"","paymentMethod",respWrapperObj.getPayment_cmnt());*/
             }else if(SchedulerErrorConstants.RNF_SSEIN100.equalsIgnoreCase(respCd)
                     || SchedulerErrorConstants.RNF_SSEIN200.equalsIgnoreCase(respCd)
                     || SchedulerErrorConstants.RNF_SSEIN300.equalsIgnoreCase(respCd)
                     || SchedulerErrorConstants.RNF_SSEIN400.equalsIgnoreCase(respCd)){
                 logger.info(eventId, "SmartServiceEngine", "PaymentStatusServiceImpl", "getPaymentData","UpdatePaymentDAO - SP E3GMR028 is not Successful",
                     AmexLogger.Result.failure, "RECORD NOT FOUND", SchedulerConstants.SQLCODE_PARM,sqlCd, "resp_code", respCd, "resp_msg", respDesc);
             }else {
                 // Update Organization SP execution failed
                 logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_028_ERR_CD, TivoliMonitoring.SSE_SP_028_ERR_MSG, eventId));
                 logger.error(eventId, "SmartServiceEngine", "PaymentStatusServiceImpl", "getPaymentData", "UpdatePaymentDAO - SP E3GMR028 is not Successful",
                     AmexLogger.Result.failure, "EXCEPTION OCCURRED WITH CODE", SchedulerConstants.SQLCODE_PARM, sqlCd,
                     "resp_code", respCd, "resp_msg", respDesc);
             }
         }else {
             // Update Organization SP execution failed
             logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_028_ERR_CD, TivoliMonitoring.SSE_SP_028_ERR_MSG, eventId));
             logger.error(eventId, "SmartServiceEngine", "PaymentStatusServiceImpl", "getPaymentData", "UpdatePaymentDAO - Empty OutMap from SP E3GMR028",
                 AmexLogger.Result.failure, "");
         }
         
         return respCd;
	}
}
