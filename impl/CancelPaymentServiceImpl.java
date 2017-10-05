package com.americanexpress.smartserviceengine.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.payve.paymentmanagementservice.v1.paymentInfoservice.PaymentResponseVO;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.ApiErrorConstants;
import com.americanexpress.smartserviceengine.common.constants.SchedulerConstants;
import com.americanexpress.smartserviceengine.common.exception.SSEApplicationException;
import com.americanexpress.smartserviceengine.common.payload.v2.ErrorDetailsType;
import com.americanexpress.smartserviceengine.common.payload.v2.ErrorType;
import com.americanexpress.smartserviceengine.common.payload.v2.PaymentResponseType;
import com.americanexpress.smartserviceengine.common.util.ApiUtil;
import com.americanexpress.smartserviceengine.common.util.CommonUtils;
import com.americanexpress.smartserviceengine.common.util.GenericUtilities;
import com.americanexpress.smartserviceengine.common.util.ThreadLocalManager;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.validator.v2.PaymentRequestValidator;
import com.americanexpress.smartserviceengine.common.vo.PayRequestVO;
import com.americanexpress.smartserviceengine.dao.CancelPaymentDAO;
import com.americanexpress.smartserviceengine.helper.CheckPaymentRestClientHelper;
import com.americanexpress.smartserviceengine.helper.StatusUpdateHelper;
import com.americanexpress.smartserviceengine.manager.PartnerValidationManager;
import com.americanexpress.smartserviceengine.service.CancelPaymentService;

@Service
public class CancelPaymentServiceImpl implements CancelPaymentService {
	
	private static AmexLogger logger=AmexLogger.create(CancelPaymentServiceImpl.class);
	
	 @Autowired
	 private CancelPaymentDAO cancelPaymentDAO;
	
	 @Autowired
	 private TivoliMonitoring tivoliMonitoring;
	 
	 @Autowired
	 private PartnerValidationManager partnerValidationManager;
	 
	/* @Autowired
	 private VNGTokenEnrollmentHelper vngTokenEnrollmentHelper;
	 
	 @Autowired
	 private StatusUpdateHelper statusUpdateHelper;*/
	 
	 @Autowired
	 private CheckPaymentRestClientHelper cancelPaymentHelper;

	 @SuppressWarnings("unchecked")
	 @Override
	 public PaymentResponseType cancelPaymentsProcess(String orgId,String paymentId, String requestOriginator, String apiMsgId)  {
		  logger.info(apiMsgId, "SmartServiceEngine", "CancelPaymentServiceImpl", "cancelPayment","Start of  cancelPayment", AmexLogger.Result.success, "START");
		 PaymentResponseType paymentResponse=new PaymentResponseType();
		String errorCode= PaymentRequestValidator.validateCancelPymt(orgId,paymentId, requestOriginator);
		if(errorCode!=null){			
			paymentResponse=createFailureResponse(errorCode,ApiConstants.VALIDATION_ERROR);
			return paymentResponse;
		}
		String partnerId=ThreadLocalManager.getPartnerId();
		String partnerName = partnerValidationManager.validatePartnerId(partnerId, apiMsgId);
		if(StringUtils.isBlank(partnerName)){
			logger.info(apiMsgId,"SmartServiceEngine", "Cancel Payment API","End of Validating Partner ID",
					"Validating Partner ID failed",AmexLogger.Result.success, "", "partnerId",ThreadLocalManager.getPartnerId());

			paymentResponse=createFailureResponse(ApiErrorConstants.SSEAPIPY012,ApiConstants.VALIDATION_ERROR);

		}
		 Map<String,Object> outMap = validateCancelPaymentDaoInvocation(apiMsgId,paymentId,partnerId,orgId);
		 if(!CollectionUtils.isEmpty(outMap)){
			 String respCode = StringUtils.stripToEmpty((String) outMap.get(SchedulerConstants.SP_RESP_CODE));
			 String respDesc = StringUtils.stripToEmpty((String) outMap.get(SchedulerConstants.SP_RESP_MSG));
			 String sqlCode = StringUtils.stripToEmpty((String) outMap.get(SchedulerConstants.SQLCODE_PARM));
			 if(ApiErrorConstants.SSEAAB00.equalsIgnoreCase(respCode)){
				 logger.info(apiMsgId,"SmartServiceEngine","Manage Enrollment API","CancelPaymentServiceImpl - cancelPaymentsProcess",
                         "Validate Cancel Card/Check Payment Request: SP E3GCM100 is successful",AmexLogger.Result.success,"",
                         ApiConstants.SQLCODE_PARM, sqlCode,"resp_code", respCode,"resp_msg", respDesc);
				 List<PayRequestVO> cardPaymentList = (List<PayRequestVO>)outMap.get(ApiConstants.RESULT_SET);
				 List<PayRequestVO> checkPaymentList = (List<PayRequestVO>)outMap.get(ApiConstants.RESULT_SET1);
				 if(!CollectionUtils.isEmpty(checkPaymentList)){
					 PayRequestVO payRequestVO = checkPaymentList.get(0);
					 if(payRequestVO != null){
						 paymentResponse = processCheckCancelPayment(orgId, paymentId, apiMsgId, paymentResponse, partnerId, payRequestVO);
					 }else {
						 paymentResponse=createFailureResponse(ApiErrorConstants.SSEAPIPY262,ApiConstants.BUSINESS_ERROR); 
					}
				 }
				 //Code commented for VNG
				 /*else if(!CollectionUtils.isEmpty(cardPaymentList)){
					 PayRequestVO payRequestVO = cardPaymentList.get(0);
					 if(payRequestVO != null){
						 paymentResponse = processCancelCardPayment(orgId, paymentId, partnerId, payRequestVO, requestOriginator, apiMsgId);
					 }else {
						 paymentResponse=createFailureResponse(ApiErrorConstants.SSEAPIPY262,ApiConstants.BUSINESS_ERROR); 
					}
				}*/else {
					paymentResponse=createFailureResponse(ApiErrorConstants.SSEAPIPY262,ApiConstants.BUSINESS_ERROR);
				}
			 }else if (ApiErrorConstants.SSEAAB01.equalsIgnoreCase(respCode)) {
				 paymentResponse=createFailureResponse(ApiErrorConstants.SSEAPIPY262,ApiConstants.BUSINESS_ERROR);
				 logger.info(apiMsgId,"SmartServiceEngine","Manage Enrollment API","CancelPaymentServiceImpl - cancelPaymentsProcess",
                         "Validate Cancel Card/Check Payment Request: SP E3GCM100 is not successful",AmexLogger.Result.failure,"",
                         ApiConstants.SQLCODE_PARM, sqlCode,"resp_code", respCode,"resp_msg", respDesc);
			}else if (ApiErrorConstants.SSEAAB02.equalsIgnoreCase(respCode)) {
				paymentResponse=createFailureResponse(ApiErrorConstants.SSEAPIPY262,ApiConstants.BUSINESS_ERROR);
				logger.info(apiMsgId,"SmartServiceEngine","Manage Enrollment API","CancelPaymentServiceImpl - cancelPaymentsProcess",
                        "Validate Cancel Card/Check Payment Request: SP E3GCM100 is not successful",AmexLogger.Result.failure,"",
                        ApiConstants.SQLCODE_PARM, sqlCode,"resp_code", respCode,"resp_msg", respDesc);
			}else {
				paymentResponse=createFailureResponse(ApiErrorConstants.SSEAPIPY001,ApiConstants.SYSTEM_ERROR);
				logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_GCM100_ERR_CD,TivoliMonitoring.SSE_SP_GCM100_ERR_MSG, apiMsgId));
				logger.info(apiMsgId,"SmartServiceEngine","Manage Enrollment API","CancelPaymentServiceImpl - cancelPaymentsProcess",
                        "Validate Cancel Card/Check Payment Request: SP E3GCM100 is not successful",AmexLogger.Result.failure,"",
                        ApiConstants.SQLCODE_PARM, sqlCode,"resp_code", respCode,"resp_msg", respDesc);
			}
		 }else {
			 paymentResponse=createFailureResponse(ApiErrorConstants.SSEAPIPY001,ApiConstants.SYSTEM_ERROR);
			 logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_GCM100_ERR_CD,TivoliMonitoring.SSE_SP_GCM100_ERR_MSG, apiMsgId));
			 logger.info(apiMsgId,"SmartServiceEngine","Manage Enrollment API","CancelPaymentServiceImpl - cancelPaymentsProcess",
                     "Validate Cancel Card/Check Payment Request: SP E3GCM100 is not successful",AmexLogger.Result.failure,"");
		}
		return paymentResponse;
	 }

	private PaymentResponseType processCheckCancelPayment(String orgId, String paymentId, String apiMsgId,PaymentResponseType paymentResponse, String partnerId,
			PayRequestVO payRequestVO) {
		try{
			 String paymentStatus=payRequestVO.getTransactionStatusCode();
			 if(paymentStatus.equalsIgnoreCase(ApiConstants.IC_CANCEL)){
					paymentResponse=createFailureResponse(ApiErrorConstants.SSEAPIPY268,ApiConstants.SYSTEM_ERROR);
			 } else{
			String paymentSystemId=payRequestVO.getPartnerPaymentId();
			float paymentAmount = 0;
			if(StringUtils.isNotBlank(payRequestVO.getPaymentLocAmount())){
				paymentAmount = Float.parseFloat(payRequestVO.getPaymentLocAmount());
			}
			String payveEntityId=CommonUtils.getPayvePartnerEntityId(partnerId);

			ResponseEntity<String> paymentResponseEntity=cancelPaymentHelper.cancelPayment(paymentSystemId,paymentAmount,payveEntityId);
			if(paymentResponseEntity!=null && paymentResponseEntity.getStatusCode()!=null){
				int statusCode=Integer.parseInt(paymentResponseEntity.getStatusCode().toString());
			if(statusCode != HttpStatus.SC_INTERNAL_SERVER_ERROR && statusCode != HttpStatus.SC_BAD_REQUEST && statusCode != HttpStatus.SC_GATEWAY_TIMEOUT){
				if(paymentResponseEntity!=null && paymentResponseEntity.getBody()!=null){
				String responseEntity=paymentResponseEntity.getBody();
				PaymentResponseVO paymentResponseVO = GenericUtilities.jsonToJava(responseEntity, PaymentResponseVO.class);

				if(paymentResponseVO!=null && StringUtils.isNotBlank(paymentResponseVO.getPaymentStatusCd())){
					Map<String,Object> outUpdatePymtMap = cancelPaymentDaoInvocation(apiMsgId,paymentId,ApiConstants.CHAR_Y,partnerId,orgId,paymentResponseVO);
					paymentResponse.setPaymentId(paymentId);
					if(outUpdatePymtMap!= null && outUpdatePymtMap.get(ApiConstants.RES_CODE)!=null && ApiConstants.SUC_SSEIN000.equalsIgnoreCase(outUpdatePymtMap.get(ApiConstants.RES_CODE).toString().trim())){
						logger.info(apiMsgId,"SmartServiceEngine","Cancel payment API","CancelPaymentServiceImpl - cancelPaymentsProcess",
								"Cancel payment API BIP call success",AmexLogger.Result.success, "", "Payment id",paymentId);	
					} else{
						//raising tivoli alert if update sp fails
						logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_028_ERR_CD, TivoliMonitoring.SSE_SP_028_ERR_MSG, apiMsgId));
						logger.info(apiMsgId,"SmartServiceEngine","Cancel payment API","CancelPaymentServiceImpl - cancelPaymentsProcess",
								"Cancel payment API BIP call success but update SP failed",AmexLogger.Result.failure, "", "Payment id",paymentId);	
					}		
				}else{
					
					if(paymentResponseVO!=null && StringUtils.isNotBlank(paymentResponseVO.getErrorCd()) && paymentResponseVO.getErrorCd().equals(ApiConstants.RESP_BR_1011)){
						paymentResponse=createFailureResponse(ApiErrorConstants.SSEAPIPY265,ApiConstants.SYSTEM_ERROR);
					} else {
						logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_BIP_CANCEL_PAYMENT_CD, TivoliMonitoring.SSE_BIP_CANCEL_PAYMENT_MSG, apiMsgId));

						paymentResponse=createFailureResponse(ApiErrorConstants.SSEAPIPY001,ApiConstants.SYSTEM_ERROR);
					}
					logger.info(apiMsgId,"SmartServiceEngine","Cancel payment API","CancelPaymentServiceImpl - cancelPaymentsProcess",
							"Cancel payment API BIP call failed",AmexLogger.Result.failure, "", "Payment id",paymentId);	
				}
			}else{
				paymentResponse=createFailureResponse(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_PY,ApiConstants.SYSTEM_ERROR);

			}  }else {
				logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_BIP_CANCEL_PAYMENT_CD, TivoliMonitoring.SSE_BIP_CANCEL_PAYMENT_MSG, apiMsgId));
				if(statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR || statusCode == HttpStatus.SC_BAD_REQUEST){
					paymentResponse=createFailureResponse(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_PY,ApiConstants.SYSTEM_ERROR);
				} 
				if(statusCode == HttpStatus.SC_GATEWAY_TIMEOUT){
					paymentResponse=createFailureResponse(ApiErrorConstants.BIP_TIMEOUT_SSEAPIEN901,ApiConstants.SYSTEM_ERROR);
				}
				logger.error(apiMsgId,"SmartServiceEngine","Cancel payment API","CancelPaymentServiceImpl - cancelPaymentsProcess","Failure from BIP for cancel payment",AmexLogger.Result.failure, "");
			}
			}else{
				paymentResponse=createFailureResponse(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_PY,ApiConstants.SYSTEM_ERROR);

			}
			 }
		}catch (SSEApplicationException e) {
			
			 /* DAO Call to execute Payment Invalidation SP */
			 
			if(e.getResponseCode().equals(ApiConstants.RESP_BR_1011)){
				paymentResponse=createFailureResponse(ApiErrorConstants.SSEAPIPY265,ApiConstants.SYSTEM_ERROR);
			} else {
				logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_BIP_CANCEL_PAYMENT_CD, TivoliMonitoring.SSE_BIP_CANCEL_PAYMENT_MSG, apiMsgId));

				paymentResponse=createFailureResponse(ApiErrorConstants.SSEAPIPY001,ApiConstants.SYSTEM_ERROR);
			}	
		}  catch (Exception e) {
            String responseCode = ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_PY;
            logger.error(
                apiMsgId,
                "SmartServiceEngine",
                "Cancel Payment API Service",
                "Exception in cancelPayment service",
                "Exception occured while processing service layer of cancelPayment Service",
                AmexLogger.Result.failure,
                "Exception in ProcessPayment service layer ", e,
                "resp_code", responseCode, "resp_msg",
                ApiUtil.getErrorDescription(responseCode), "error_msg", e.getMessage());
			paymentResponse=createFailureResponse(ApiErrorConstants.SSEAPIPY001,ApiConstants.SYSTEM_ERROR);
    }
		return paymentResponse;
	}

	/*private PaymentResponseType processCancelCardPayment(String orgId, String paymentId, String partnerId, PayRequestVO payRequestVO, String requestOriginator, String apiMsgId) {
		PaymentResponseType paymentResponse = null;
		boolean success = false;
		VNGTokenEnrollmentVO vngTokenEnrollmentVO = new VNGTokenEnrollmentVO();
		vngTokenEnrollmentVO.setTokenNumber(payRequestVO.getTokenNumber());
		
		if(MultiTokenPaymentStatus.USED.getDbStatusCode().equalsIgnoreCase(payRequestVO.getTransactionStatusCode()) ||
				MultiTokenPaymentStatus.EXPR.getDbStatusCode().equalsIgnoreCase(payRequestVO.getTransactionStatusCode()) ||
				MultiTokenPaymentStatus.FAILED.getDbStatusCode().equalsIgnoreCase(payRequestVO.getTransactionStatusCode())){
			paymentResponse=createFailureResponse(ApiErrorConstants.SSEAPIPY132,ApiConstants.BUSINESS_ERROR);
		}
		
		if(paymentResponse == null){
			try {
				if(!(ApiConstants.INTR.equalsIgnoreCase(payRequestVO.getTransactionStatusCode()) || ApiConstants.PYMT_STAT_INPR.equalsIgnoreCase(payRequestVO.getTransactionStatusCode()))){
					vngTokenEnrollmentHelper.vngCancelTokenEnrollment(vngTokenEnrollmentVO, apiMsgId);
					success = !vngTokenEnrollmentVO.isError();
				}else {
					success = true;
				}
			} catch (GeneralSecurityException e) {
				success = Boolean.FALSE;
				logger.error(apiMsgId, APPLICATION_NAME, "CancelPaymentServiceImpl", "CancelPaymentServiceImpl - processCancelCardPayment", "CANCEL TOKEN FAILED WITH EXCEPTION", AmexLogger.Result.failure, 
						"Unexpected Exception while cancelling the token.", e);
			}catch(Exception exception){
				success = Boolean.FALSE;
				logger.error(apiMsgId, APPLICATION_NAME, "CancelPaymentServiceImpl", "CancelPaymentServiceImpl - processCancelCardPayment", "CANCEL TOKEN FAILED WITH EXCEPTION", AmexLogger.Result.failure, 
						"Unexpected Exception while cancelling the token.", exception);
			}
			if(success){
				String description = ApiConstants.REQUEST_ORIGINATOR_SP.equalsIgnoreCase(requestOriginator) ? ApiConstants.DESCRIPTION_CANCEL_TOKEN_SERVICING_PORTAL : ApiConstants.DESCRIPTION_CANCEL_TOKEN_PARTNER;
				MultiTokenPaymentStatus tokenPaymentStatus = null;
				if(!MultiTokenPaymentStatus.USED.getDbStatusCode().equalsIgnoreCase(payRequestVO.getTransactionStatusCode())){
					tokenPaymentStatus = MultiTokenPaymentStatus.EXPR;
				}else {
					tokenPaymentStatus = MultiTokenPaymentStatus.getPaymentStatus(payRequestVO.getTransactionStatusCode());
				}
				statusUpdateHelper.updateTokenAndPaymentStatus(paymentId, tokenPaymentStatus.getStatusCode(), tokenPaymentStatus.getDbStatusCode(), 
						tokenPaymentStatus.getTokenPaymentDescription(), 0, payRequestVO.getTokenNumber(), TokenStatus.CANCELLED.getStatusCode(), description);
				paymentResponse=createSuccessResponse(paymentId);
			}else {
				paymentResponse=createFailureResponse(vngTokenEnrollmentVO.getResponseCode(), ApiConstants.SYSTEM_ERROR);
			}
		}
		return paymentResponse;
	}*/

	private PaymentResponseType createFailureResponse(String errorCode,String type) {
		PaymentResponseType responseType=new PaymentResponseType();
		List<ErrorDetailsType> errorList=PaymentRequestValidator.getErrorDetails(errorCode);
		ErrorType errorType = new ErrorType();
		errorType.setType(type);
		errorType.getErrorDetails().addAll(errorList);
		responseType.setError(errorType);
		return responseType;
	}
	
	private PaymentResponseType createSuccessResponse(String paymentId) {
		PaymentResponseType responseType=new PaymentResponseType();
		responseType.setPaymentId(paymentId);
		return responseType;
	}
	
	 private Map<String,Object> cancelPaymentDaoInvocation(String apiMsgId,String paymentId,String updateIndicator, String partnerId, String orgId, PaymentResponseVO paymentResponseVO)
	 {
		 Map<String,Object> inCancelPymtMap=updatePaymentInputMap(paymentId,updateIndicator,partnerId,orgId,paymentResponseVO);
		 Map<String,Object> outCancelPymtMap=cancelPaymentDAO.execute(inCancelPymtMap, apiMsgId);
		 if( (StringUtils.stripToEmpty((String) outCancelPymtMap.get(ApiConstants.RES_CODE))).equalsIgnoreCase(ApiConstants.SUC_SSEIN000)){
			 String respCode = ((String) outCancelPymtMap.get(ApiConstants.RES_CODE)).trim();
             logger.info(apiMsgId,"SmartServiceEngine","Process payment API","CancelPaymentServiceImpl - cancelPaymentDaoInvocation",
                 "Cancel payment fetch: SP E3GMR028 is successful",AmexLogger.Result.success, "",ApiConstants.SQLCODE_PARM,
                 outCancelPymtMap.get(ApiConstants.SQL_CODE).toString(),"resp_code", respCode, "resp_msg", outCancelPymtMap.get(ApiConstants.RES_MSSG).toString());          
			}else{
	            String respCode = null;
	            String respDesc = null;
	            String sqlCode = null;
	            if (outCancelPymtMap != null && !outCancelPymtMap.isEmpty()) {
	                respCode = ((String) outCancelPymtMap.get(ApiConstants.RES_CODE));
	                respDesc = ((String) outCancelPymtMap.get(ApiConstants.RES_MSSG));
	                sqlCode = ((String) outCancelPymtMap.get(ApiConstants.SQL_CODE));
	            }
	            logger.error(apiMsgId,"SmartServiceEngine", "Manage Enrollment API", "EnrollmentServiceImpl - manageEnrollment",
	                "Add payee org status update success: SP E3GMR028 is not successful", AmexLogger.Result.failure, "", ApiConstants.SQLCODE_PARM,
	                sqlCode,"resp_code", respCode, "resp_msg",respDesc); 
			 }
		return outCancelPymtMap;
	 }
	 
	 private Map<String,Object> validateCancelPaymentDaoInvocation(String apiMsgId,String paymentId, String partnerId, String orgId){
		 Map<String,Object> inCancelPymtMap=cancelPaymentInputMap(paymentId,null,partnerId,orgId);
		 Map<String,Object> outCancelPymtMap=cancelPaymentDAO.validateCancelPayment(inCancelPymtMap, apiMsgId);
		return outCancelPymtMap;
	 }
	 
	 private Map<String,Object> cancelPaymentInputMap(String paymentId,String updateIndicator,String partnerId, String orgId){
		 Map<String,Object> inCancelPymtMap=new HashMap<String, Object>();
		 inCancelPymtMap.put(ApiConstants.IN_PYMT_ID,paymentId );		 
		 if(updateIndicator != null){
		 	inCancelPymtMap.put(ApiConstants.IN_UPDT_IND,updateIndicator);	
		 }
		 inCancelPymtMap.put(ApiConstants.IN_PRTR_MAC_ID,partnerId);		 
		 inCancelPymtMap.put(ApiConstants.IN_PRTR_ORG_ID,orgId);		 
		 return inCancelPymtMap;
	 }	
	 
	 private Map<String,Object> updatePaymentInputMap(String paymentId,String updateIndicator,String partnerId, String orgId, PaymentResponseVO paymentResponseVO){		 
		Map<String,Object> inCancelPymtMap=new HashMap<String, Object>();
		inCancelPymtMap.put(ApiConstants.IN_SRCE_ORG_ID ,orgId);
		inCancelPymtMap.put(ApiConstants.IN_BUY_PYMT_ID,ApiConstants.CHAR_SPACE);
		inCancelPymtMap.put(ApiConstants.IN_PYMT_SYS_RESP_CD,StringUtils.stripToEmpty(paymentResponseVO.getPaymentStatusCd()));
		inCancelPymtMap.put(ApiConstants.IN_BUY_PYMT_STA,ApiConstants.STATUS_CANCEL);
		inCancelPymtMap.put(ApiConstants.IN_PYMT_SYS_RESP_DS,ApiConstants.SUCCESS);		 
		inCancelPymtMap.put(ApiConstants.IN_PRTR_PYMT_ID,StringUtils.stripToEmpty(paymentResponseVO.getPaymentSystemId()));	
		return inCancelPymtMap;
	 }	
}
