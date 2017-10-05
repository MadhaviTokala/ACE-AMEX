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
package com.americanexpress.smartserviceengine.service.impl;

import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.ApiErrorConstants;
import com.americanexpress.smartserviceengine.common.exception.SSEApplicationException;
import com.americanexpress.smartserviceengine.common.payload.CommonContext;
import com.americanexpress.smartserviceengine.common.payload.ProcessPaymentRequestData;
import com.americanexpress.smartserviceengine.common.payload.ProcessPaymentRequestType;
import com.americanexpress.smartserviceengine.common.payload.ProcessPaymentResponse;
import com.americanexpress.smartserviceengine.common.payload.ProcessPaymentResponseData;
import com.americanexpress.smartserviceengine.common.payload.ProcessPaymentResponseType;
import com.americanexpress.smartserviceengine.common.payload.v2.ErrorDetailsType;
import com.americanexpress.smartserviceengine.common.payload.v2.ErrorType;
import com.americanexpress.smartserviceengine.common.payload.v2.PaymentRequestType;
import com.americanexpress.smartserviceengine.common.payload.v2.PaymentResponseType;
import com.americanexpress.smartserviceengine.common.util.ApiUtil;
import com.americanexpress.smartserviceengine.common.util.DateTimeUtil;
import com.americanexpress.smartserviceengine.common.util.EnvironmentPropertiesUtil;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.validator.PaymentRequestValidator;
import com.americanexpress.smartserviceengine.common.validator.ReconciliationValidator;
import com.americanexpress.smartserviceengine.manager.PartnerValidationManager;
import com.americanexpress.smartserviceengine.manager.ProcessAchPaymentManager;
import com.americanexpress.smartserviceengine.manager.ProcessCardPaymentManager;
import com.americanexpress.smartserviceengine.manager.ProcessPaymentManager;
import com.americanexpress.smartserviceengine.manager.UpdateCardPaymentManager;
import com.americanexpress.smartserviceengine.service.PaymentService;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static AmexLogger logger = AmexLogger.create(PaymentServiceImpl.class);

    @Autowired
    ProcessPaymentManager processPaymentManager;

    @Autowired
    ProcessAchPaymentManager processAchPymtMngr;

    @Autowired
    ProcessCardPaymentManager processCardPymtManager;
    
    @Autowired
    private UpdateCardPaymentManager updateCardPaymentManager;
    
    @Autowired
    PartnerValidationManager partnerValidationManager;

    @Autowired
    private TivoliMonitoring tivoliMonitoring;

    /*
     * (non-Javadoc)
     *
     * @see com.americanexpress.smartserviceengine.api.service.PaymentService#
     * processPayment(com.americanexpress.smartserviceengine.api.payload.pojo.
     * ProcessPaymentRequestType, java.lang.String)
     */
    @Override
    public ProcessPaymentResponseType processPayment(ProcessPaymentRequestType requestType, String apiMsgId) {

        logger.info(apiMsgId, "SmartServiceEngine", "PaymentServiceImpl", "processPayment",
            "Start of  processPayment", AmexLogger.Result.success, "START");

        ProcessPaymentResponseType responseType = new ProcessPaymentResponseType();
        ProcessPaymentResponse response = new ProcessPaymentResponse();
        CommonContext commonResponseContext = new CommonContext();
        String responseCode = null;
        ProcessPaymentRequestData requestData = null;
        try {

            /*
             * Validate the Request Payload for common fields
             */
            response = PaymentRequestValidator.validateCommonPaymentRequest(requestType, apiMsgId);
            if (response.getData() != null
                    && response.getData().getResponseCode() != null) {
                responseType.setProcessPaymentResponse(response);
                logger.debug(apiMsgId, "SmartServiceEngine",
                    "Process Payment API Service",
                    "After Common field validation",
                    "Common Field validation Failed",
                    AmexLogger.Result.failure,
                    "Common Field validation Failed", "responseCode", response.getData().getResponseCode(),
                    "ResponseDesc" , ApiUtil.getErrorDescription(response.getData().getResponseCode()));
                return responseType;
            }

			logger.debug("Validating Partner ID: "+requestType.getProcessPaymentRequest().getData().getCommonRequestContext().getPartnerId());

			/**
			 * Partner Id eh-cache changes-Comparing partner id in request with list of partner ids in cache.
			 * If partner Id is valid PartnerValidationManager returns corresponding partner name.  
			 */
			String partnerName = partnerValidationManager.validatePartnerId(requestType.getProcessPaymentRequest().getData().getCommonRequestContext().getPartnerId(),apiMsgId);

			if (partnerName != null) {
					logger.info(apiMsgId,"SmartServiceEngine", "Manage Enrollment API","End of Validating Partner ID",
							"Validating Partner ID was successful",AmexLogger.Result.success, "", "partnerId", requestType.getProcessPaymentRequest().getData().getCommonRequestContext().getPartnerId());

			}else{
				responseCode = ApiErrorConstants.SSEAPIEN078;
				 //setFailureResponse(apiMsgId,responseCode,commonResponseContext,requestData,response);
				String buyerPymtRefId = null;
				if(requestType.getProcessPaymentRequest().getData().getPaymentDetails()!=null && requestType.getProcessPaymentRequest().getData().getPaymentDetails().getBuyerPymtRefId()!=null){
					buyerPymtRefId = requestType.getProcessPaymentRequest().getData().getPaymentDetails().getBuyerPymtRefId();		
				}
				PaymentRequestValidator.setFailureResponse(requestType.getProcessPaymentRequest().getData().getCommonRequestContext(), responseCode, 
						buyerPymtRefId, response, apiMsgId);				 

			}
            /*
             * If common field validation is failure, return error response
             */

            if (response.getData() != null
                    && response.getData().getResponseCode() != null) {
                responseType.setProcessPaymentResponse(response);
                logger.debug(apiMsgId, "SmartServiceEngine",
                    "Process Payment API Service",
                    "After Common field validation",
                    "Common Field validation Failed",
                    AmexLogger.Result.failure,
                    "Common Field validation Failed", "responseCode", response.getData().getResponseCode(),
                    "ResponseDesc" , ApiUtil.getErrorDescription(response.getData().getResponseCode()));
                return responseType;
            }

            /*
             * If common field validation is successful, continue further
             * processing
             */
            logger.debug(apiMsgId, "SmartServiceEngine",
                "Process Payment API Service",
                "After Common field validation",
                "Common Field validation Successful",
                AmexLogger.Result.success,
                    "Common Field validation Successful");

            commonResponseContext.setPartnerId(requestType
                .getProcessPaymentRequest().getData()
                .getCommonRequestContext().getPartnerId());
            commonResponseContext.setRequestId(requestType
                .getProcessPaymentRequest().getData()
                .getCommonRequestContext().getRequestId());
            commonResponseContext.setPartnerName(requestType
                .getProcessPaymentRequest().getData()
                .getCommonRequestContext().getPartnerName());
            commonResponseContext.setTimestamp(ApiUtil.getCurrentTimeStamp());

            requestData = requestType.getProcessPaymentRequest().getData();

            response = PaymentRequestValidator.validatePaymentRequestFields(requestData, commonResponseContext, apiMsgId);
            if (response.getData() != null && response.getData().getResponseCode() != null) {

                logger.debug(apiMsgId, "SmartServiceEngine",
                    "Process Payment API Service",
                    "After Request Field validation",
                    "Field validation Failed",
                    AmexLogger.Result.failure,
                    "Field validation Failed", "responseCode", response.getData().getResponseCode(),
                    "ResponseDesc" , ApiUtil.getErrorDescription(response.getData().getResponseCode()));
                responseType.setProcessPaymentResponse(response);
                return responseType;
            }

            logger.debug(apiMsgId, "SmartServiceEngine",
                "Process Payment API Service",
                "After processPayment service",
                "Field validation is successful",
                AmexLogger.Result.success,
                "Field validation is successful", "requestId", requestData
                .getCommonRequestContext().getRequestId());

            /*
             * Perform business validation on Payment details and store payment
             * details in SSE
             */
            String pymtMethod = requestData.getPaymentDetails().getPaymentMethod();

            if(null != pymtMethod && pymtMethod.trim().equalsIgnoreCase(
                ApiConstants.PAYMENT_MTD_CHECK)){								// For Check Payments
                response = processPaymentManager.processPayment(requestData, apiMsgId);
            }else if(null != pymtMethod && pymtMethod.trim().equalsIgnoreCase(
                ApiConstants.PAYMENT_MTD_ACH)){									// For ACH Payments
                response = processAchPymtMngr.processPayment(requestData, apiMsgId);
            }else if(null != pymtMethod && pymtMethod.trim().equalsIgnoreCase(ApiConstants.PAYMENT_MTD_CARD)){
            	response = processCardPymtManager.processPayment(requestData, apiMsgId);
            }

            if (response.getData() != null && response.getData().getResponseCode() != null) {
                logger.debug(apiMsgId, "SmartServiceEngine",
                    "Process Payment API Service",
                    "After Process Payment",
                    "Payment Process Failed",
                    AmexLogger.Result.failure,
                    "Payment Process Failed", "responseCode", response.getData().getResponseCode(),
                    "ResponseDesc" , ApiUtil.getErrorDescription(response.getData().getResponseCode()));
                responseType.setProcessPaymentResponse(response);
                return responseType;
            } else {
                buildSuccessResponse(requestType, response, apiMsgId);

            }
        } catch (SSEApplicationException e) {
            responseCode = e.getResponseCode();
            logger.error(
                apiMsgId,
                "SmartServiceEngine",
                "Process Payment API Service",
                "Exception in processPayment service",
                "Exception occured while processing service layer of ProcessPayment Service",
                AmexLogger.Result.failure,
                "Exception in ProcessPayment service layer ", e,
                "resp_code", responseCode, "resp_msg",
                ApiUtil.getErrorDescription(responseCode), "error_msg", e.getErrorMessage());
            setFailureResponse(apiMsgId,responseCode,commonResponseContext,requestData,response);

        } catch (Exception e) {
            responseCode = ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_PY;
            logger.error(
                apiMsgId,
                "SmartServiceEngine",
                "Process Payment API Service",
                "Exception in processPayment service",
                "Exception occured while processing service layer of ProcessPayment Service",
                AmexLogger.Result.failure,
                "Exception in ProcessPayment service layer ", e,
                "resp_code", responseCode, "resp_msg",
                ApiUtil.getErrorDescription(responseCode), "error_msg", e.getMessage());
            setFailureResponse(apiMsgId,responseCode,commonResponseContext,requestData,response);
        }
        responseType.setProcessPaymentResponse(response);
        logger.info(apiMsgId, "SmartServiceEngine", "PaymentServiceImpl", "processPayment",
            "END of  processPayment", AmexLogger.Result.success, "END");
        return responseType;
    }

    private void buildSuccessResponse(ProcessPaymentRequestType requestType,
            ProcessPaymentResponse response, String apiMsgId)
                    throws SSEApplicationException {
        ProcessPaymentResponseData responseData = null;
        if (response.getData() != null) {
            responseData = response.getData();
        } else {
            responseData = new ProcessPaymentResponseData();
        }
        // CommonContext Object
        CommonContext commonResponseContext = new CommonContext();
        commonResponseContext.setPartnerId(requestType
            .getProcessPaymentRequest().getData().getCommonRequestContext()
            .getPartnerId());
        commonResponseContext.setRequestId(requestType
            .getProcessPaymentRequest().getData().getCommonRequestContext()
            .getRequestId());
        commonResponseContext.setPartnerName(requestType
            .getProcessPaymentRequest().getData().getCommonRequestContext()
            .getPartnerName());

        String timestamp = null;
        try {
            TimeZone timeZone = DateTimeUtil.getTimeZone(requestType
                .getProcessPaymentRequest().getData()
                .getCommonRequestContext().getTimestamp());
            timestamp = DateTimeUtil.getSystemTime(timeZone);
        } catch (Exception e) {
            logger.error(apiMsgId, "SmartServiceEngine", "Get Status API",
                "PaymentServiceImpl: buildSuccessResponse",
                "Succesful execution of processPayment service",
                AmexLogger.Result.failure,
                "Exception while getting the system current date", "");

        }
        commonResponseContext.setTimestamp(timestamp);

        // Service Response
        responseData.setCommonResponseContext(commonResponseContext);
        responseData.setBuyerPymtRefId(requestType.getProcessPaymentRequest()
            .getData().getPaymentDetails().getBuyerPymtRefId());
        response.setData(responseData);
        response.setStatus(ApiConstants.SUCCESS);

    }
    
    private void setFailureResponse(String apiMsgId,String responseCode, CommonContext commonResponseContext,ProcessPaymentRequestData requestData, ProcessPaymentResponse response){
        processPaymentManager.setFailureResponse(commonResponseContext,
            responseCode, requestData.getPaymentDetails()
            .getBuyerPymtRefId(), response);
        //Tivoli Monitoring
        if (null != responseCode
                && responseCode.equals(
                    ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_PY)) {
            logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_APPL_ERR_CD, TivoliMonitoring.SSE_APPL_ERR_MSG, apiMsgId));
        }
    }

	@Override
	public PaymentResponseType updateCardPayment(PaymentRequestType paymentRequest, String apiMsgId) {
		 logger.info(apiMsgId, "SmartServiceEngine", "PaymentServiceImpl", "updateCardPayment", "Start of  update Payment", AmexLogger.Result.success, "Start");
		 String responseCode = null;
		 PaymentResponseType paymentResponse = new PaymentResponseType();
        try {
        	String partnerName = partnerValidationManager.validatePartnerId(paymentRequest.getPartnerId(), apiMsgId);
    		logger.info("PartnerName:"+partnerName);
    		if (partnerName != null) {
    			paymentResponse = updateCardPaymentManager.updateCardPayment(paymentRequest, apiMsgId);
    		}else{
    			paymentResponse = buildPymtFailureResponse(ApiErrorConstants.SSEAPIEN078);
    		}
		}catch(SSEApplicationException exception){
            responseCode = exception.getResponseCode();
            logger.error(apiMsgId, "SmartServiceEngine", "Update Payment API Service",  "Exception in Update Payment service",
                "Exception occured while processing service layer of Update Payment Service",
                AmexLogger.Result.failure, "Exception in Update Payment service layer", exception,
                "resp_code", responseCode, "resp_msg", EnvironmentPropertiesUtil.getProperty(responseCode), "error_msg", exception.getErrorMessage());
            logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_APPL_ERR_CD, TivoliMonitoring.SSE_APPL_ERR_MSG, apiMsgId));
            paymentResponse = buildPymtFailureResponse(responseCode);
		} catch (Exception e) {
			responseCode = ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_PY;
            logger.error(apiMsgId, "SmartServiceEngine", "Update Payment API Service", "Exception in Update Payment service",
                "Exception occured while processing service layer of Update Payment Service", AmexLogger.Result.failure,
                "Exception in Update Payment service layer", e, "resp_code", responseCode, "resp_msg",
                EnvironmentPropertiesUtil.getProperty(responseCode), "error_msg", e.getMessage());
            logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_APPL_ERR_CD, TivoliMonitoring.SSE_APPL_ERR_MSG, apiMsgId));
            paymentResponse = buildPymtFailureResponse(responseCode);
		}
        logger.info(apiMsgId, "SmartServiceEngine", "PaymentServiceImpl", "updateCardPayment", "Start of  update Payment", AmexLogger.Result.success, "End");
		return paymentResponse;
	}
	
	public PaymentResponseType buildPymtFailureResponse(String errorCode){
		PaymentResponseType paymentResponse = new PaymentResponseType();
		ErrorType error = new ErrorType();
		ErrorDetailsType errorDetails = new ErrorDetailsType();
		if(StringUtils.isNotBlank(errorCode)){
			errorDetails.setCode(errorCode);
			errorDetails.setMessage(ApiUtil.getErrorDescription(errorCode));
		}else {
			errorDetails.setCode(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_PY);
			errorDetails.setMessage(ApiUtil.getErrorDescription(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_PY));
		}
		error.getErrorDetails().add(errorDetails);
		paymentResponse.setError(error);
		return paymentResponse;
	}
}
