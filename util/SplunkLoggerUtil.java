package com.americanexpress.smartserviceengine.common.util;

import java.util.Iterator;

import org.springframework.stereotype.Component;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.payload.GetStatusRequestData;
import com.americanexpress.smartserviceengine.common.payload.GetStatusRequestType;
import com.americanexpress.smartserviceengine.common.payload.GetStatusResponse;
import com.americanexpress.smartserviceengine.common.payload.GetStatusResponseData;
import com.americanexpress.smartserviceengine.common.payload.GetStatusResponseType;
import com.americanexpress.smartserviceengine.common.payload.ManageEnrollmentRequestData;
import com.americanexpress.smartserviceengine.common.payload.ManageEnrollmentResponseData;
import com.americanexpress.smartserviceengine.common.payload.ManageEnrollmentResponseType;
import com.americanexpress.smartserviceengine.common.payload.ProcessPaymentRequestData;
import com.americanexpress.smartserviceengine.common.payload.ProcessPaymentRequestType;
import com.americanexpress.smartserviceengine.common.payload.ProcessPaymentResponseData;
import com.americanexpress.smartserviceengine.common.payload.ProcessPaymentResponseType;
import com.americanexpress.smartserviceengine.common.payload.RetrievePayersRequestData;
import com.americanexpress.smartserviceengine.common.payload.RetrievePayersResponseData;
import com.americanexpress.smartserviceengine.common.payload.RetrievePayersResponseType;
import com.americanexpress.smartserviceengine.common.payload.UpdateiCruseStatusRequestData;
import com.americanexpress.smartserviceengine.common.payload.UpdateiCruseStatusResponseData;
import com.americanexpress.smartserviceengine.common.payload.UpdateiCruseStatusResponseType;
import com.americanexpress.smartserviceengine.common.payload.v2.AccountStatusResponseType;
import com.americanexpress.smartserviceengine.common.payload.v2.EnrollmentResponseType;
import com.americanexpress.smartserviceengine.common.payload.v2.PymtStatusResponseType;
import com.americanexpress.smartserviceengine.common.splunk.SplunkLogger;
import com.americanexpress.smartserviceengine.common.vo.StatusRequestVO;

@Component
public class SplunkLoggerUtil {
	
	 private static AmexLogger logger = AmexLogger.create(SplunkLoggerUtil.class);
	
	 public void iCruseSplunkLogger( UpdateiCruseStatusResponseType responseType,String apiMsgId,UpdateiCruseStatusRequestData requestData){
		 if(null!=responseType)
	     {
	         if(null!=responseType.getUpdateiCruseStatusResponse())
	         {
	             if(null!=responseType.getUpdateiCruseStatusResponse().getData())
	             {
	             	UpdateiCruseStatusResponseData objUpdateiCruseStatusResponseData = responseType.getUpdateiCruseStatusResponse().getData();
	             	String status = SplunkLogger.status(responseType);
	                 
	             	if (status != null && ("success".equalsIgnoreCase(status))){                		
	             		
	             		 logger.info(apiMsgId, "SmartServiceEngine", "updateiCrusestatus API", "IcruseController update iCruse status API",
	                              "End of processing iCruse status update", AmexLogger.Result.success, "","status",status,
	                              "partnerId", SplunkLogger.iCrusePartnerId(objUpdateiCruseStatusResponseData),
	                              "partnerName", SplunkLogger.iCrusePartnerName(objUpdateiCruseStatusResponseData),
	                              "iCruseTransactionId",SplunkLogger.iCruseTransactionId(requestData),
	                              "iCruseAlertType",SplunkLogger.iCruseAlertType(requestData),
	                              "iCruseCheckType", SplunkLogger.iCruseCheckType(requestData),
	                              "iCruseSicCode", SplunkLogger.iCruseSicCode(requestData),
	                              "iCruseResult",SplunkLogger.iCruseResult(requestData),
	                              "iCruseResponseType", SplunkLogger.iCruseResponseType(requestData)
	                              );
	             		
	             	}else{
	             		logger.info(apiMsgId, "SmartServiceEngine", "updateiCrusestatus API", "IcruseController update iCruse status API",
	                             "End of processing iCruse status update", AmexLogger.Result.success, "","status",status,
	                             "partnerId", SplunkLogger.iCrusePartnerId(objUpdateiCruseStatusResponseData),
	                             "partnerName", SplunkLogger.iCrusePartnerName(objUpdateiCruseStatusResponseData),
	                             "iCruseTransactionId",SplunkLogger.iCruseTransactionId(requestData),
	                             "iCruseAlertType",SplunkLogger.iCruseAlertType(requestData),
	                             "iCruseCheckType", SplunkLogger.iCruseCheckType(requestData),
	                             "iCruseSicCode", SplunkLogger.iCruseSicCode(requestData),
	                             "iCruseResult",SplunkLogger.iCruseResult(requestData),
	                             "iCruseResponseType", SplunkLogger.iCruseResponseType(requestData),
	                             "responseCode", SplunkLogger.responseCode(objUpdateiCruseStatusResponseData),
	                             "responseDesc", SplunkLogger.responseDesc(objUpdateiCruseStatusResponseData));
	             	}
	             }
	         }
	     }
	}
	
	 public void retrievePayersSplunkLogger(RetrievePayersResponseType responseType,String apiMsgId,RetrievePayersRequestData requestData){		 
		 if(null!=responseType)
	        {
	            if(null!=responseType.getRetrievePayersResponse())
	            {
	                if(null!=responseType.getRetrievePayersResponse().getData())
	                {
	                	RetrievePayersResponseData objRetrievePayersResponseData = responseType.getRetrievePayersResponse().getData();
	                    
	                    String status = SplunkLogger.status(responseType);
	                    String bipOrgId=  null;
	                   
	                	if (status != null && ("success".equalsIgnoreCase(status))){
	                		
	                		logger.info(apiMsgId, "SmartServiceEngine", "Retrieve Payer IDs API",
	                                "Processing Retrieve Payer IDs API service",
	                                "End of Retrieving Payer IDs API",
	                                AmexLogger.Result.success,"","status",status,
	                                "partnerId",SplunkLogger.partnerId(objRetrievePayersResponseData),
	                                "partnerName", SplunkLogger.partnerName(objRetrievePayersResponseData),
	                                "organizationId", SplunkLogger.organizationId(requestData),
	        	    				"partnerAccountId", SplunkLogger.partnerAccountId(requestData),
	                                "authorizedPayerNos", SplunkLogger.authorizedPayerNos(objRetrievePayersResponseData));
	                		
	                	}else{
	                		
	                		logger.info(apiMsgId, "SmartServiceEngine", "Retrieve Payer IDs API",
	                                "Processing Retrieve Payer IDs API service",
	                                "End of Retrieving Payer IDs API",
	                                AmexLogger.Result.success,"","status",status,
	                                "partnerId",SplunkLogger.partnerId(objRetrievePayersResponseData),                    
	                                "partnerName", SplunkLogger.partnerName(objRetrievePayersResponseData),
	                                "organizationId", SplunkLogger.organizationId(requestData),
	                                "partnerAccountId", SplunkLogger.partnerAccountId(requestData),
	                                "authorizedPayerNos", SplunkLogger.authorizedPayerNos(objRetrievePayersResponseData),
	                                "responseCode", SplunkLogger.responseCode(objRetrievePayersResponseData),
	                                "responseDesc", SplunkLogger.responseDesc(objRetrievePayersResponseData));
	                       }
	                }
	            }
	        }
	 }
	 
	 public void manageEnrollmentSplunkLogger( ManageEnrollmentResponseType responseType, String apiMsgId,ManageEnrollmentRequestData objManageEnrollmnetRequestData){
		 if(null!=responseType)
	        {
	            if(null!=responseType.getManageEnrollmentResponse())
	            {
	                if(null!=responseType.getManageEnrollmentResponse().getData())
	                {
	                    ManageEnrollmentResponseData objManageEnrollmentResponseData = responseType.getManageEnrollmentResponse().getData();
	                    
	                    String status = SplunkLogger.status(responseType);
	                    String bipOrgId=  null;
	                   
	                	if (status != null && ("success".equalsIgnoreCase(status)|| "partial_success".equalsIgnoreCase(status))){
	                    
	                    logger.info(apiMsgId, "SmartServiceEngine", "Manage Enrollment API",
	                        "SmartServiceEngineController Manage Enrollment Response",
	                        "End of processing Manage Enrollment API request",
	                        AmexLogger.Result.success,"","status",status,
	                        "partnerId",SplunkLogger.partnerId(objManageEnrollmentResponseData),                    
	                        "partnerName", SplunkLogger.partnerName(objManageEnrollmentResponseData),
	                        "orgCheckStatus",SplunkLogger.orgCheckStatus(objManageEnrollmentResponseData),
	                        "orgACHStatus",SplunkLogger.orgACHStatus(objManageEnrollmentResponseData),
	                        "organizationId", SplunkLogger.organizationId(objManageEnrollmnetRequestData),
	                        "associatedOrgId",SplunkLogger.associatedOrgID(objManageEnrollmnetRequestData),
	                        "enrollmentCategory", SplunkLogger.enrollmentCategory(objManageEnrollmnetRequestData),
	                        "enrollmentActionType",SplunkLogger.enrollmentActionType(objManageEnrollmnetRequestData),
	                        "accountStatus",SplunkLogger.accountStatus(objManageEnrollmentResponseData),
	                        "serviceCode",SplunkLogger.serviceCode(objManageEnrollmnetRequestData),
	                        "orgName",SplunkLogger.orgName(objManageEnrollmnetRequestData),
	                        "organizationStatus",SplunkLogger.organizationStatus(objManageEnrollmnetRequestData),
	                        "paymentMethod",SplunkLogger.paymenMethod(objManageEnrollmentResponseData),
	                        "bipOrgId",ThreadLocalManager.getBipOrgid());
	                        
	                        
	                	}else{
	                		
	                		logger.info(apiMsgId, "SmartServiceEngine", "Manage Enrollment API",
	                                "SmartServiceEngineController Manage Enrollment Response",
	                                "End of processing Manage Enrollment API request",
	                                AmexLogger.Result.success,"","status",status,
	                                "partnerId",SplunkLogger.partnerId(objManageEnrollmentResponseData),                    
	                                "partnerName", SplunkLogger.partnerName(objManageEnrollmentResponseData),
	                                "orgCheckStatus",SplunkLogger.orgCheckStatus(objManageEnrollmentResponseData),
	                                "orgACHStatus",SplunkLogger.orgACHStatus(objManageEnrollmentResponseData),
	                                "organizationId", SplunkLogger.organizationId(objManageEnrollmnetRequestData),
	                                "associatedOrgId",SplunkLogger.associatedOrgID(objManageEnrollmnetRequestData),
	                                "enrollmentCategory", SplunkLogger.enrollmentCategory(objManageEnrollmnetRequestData),
	                                "enrollmentActionType",SplunkLogger.enrollmentActionType(objManageEnrollmnetRequestData),
	                                "accountStatus",SplunkLogger.accountStatus(objManageEnrollmentResponseData),
	                                "serviceCode",SplunkLogger.serviceCode(objManageEnrollmnetRequestData),
	                                "orgName",SplunkLogger.orgName(objManageEnrollmnetRequestData),
	                                "paymentMethod",SplunkLogger.paymenMethodRequest(objManageEnrollmnetRequestData),
	                                "organizationStatus",SplunkLogger.organizationStatus(objManageEnrollmnetRequestData),
	                                "bipOrgId",ThreadLocalManager.getBipOrgid(),
	                                "responseCode", SplunkLogger.responseCode(objManageEnrollmentResponseData),
	                                "responseDesc", SplunkLogger.responseDesc(objManageEnrollmentResponseData));
	                		
	                	}
	                	
	                }
	            }
	        }
	 }
	 
	 
	 public void reconciliationSplunkLogger(StatusRequestVO statusRequestVO, String apiMsgId){
		 
		 String orgId = null;
	     String associatedOrgId = null;
	     String paymentMethod = null;
		 
		 if (statusRequestVO.getAssociateOrgId() != null && !("".equals(statusRequestVO.getAssociateOrgId().trim()))){
			 orgId = statusRequestVO.getOrgId();
			 associatedOrgId = statusRequestVO.getAssociateOrgId();
		 }else{
			 orgId = statusRequestVO.getOrgId();
			 associatedOrgId = "";
		 }
		 if(statusRequestVO.getServiceCd()!= null && statusRequestVO.getServiceCd().equalsIgnoreCase(ApiConstants.SERVICE_CODE_CHECK)){
			 paymentMethod = ApiConstants.PAYMENT_MTD_CHECK;
		 }
		 else if(statusRequestVO.getServiceCd()!= null && statusRequestVO.getServiceCd().equalsIgnoreCase(ApiConstants.SERVICE_CODE_CARD)){
			 paymentMethod = ApiConstants.PAYMENT_MTD_CARD;
		 }
		 if(statusRequestVO.getServiceCd()!= null && statusRequestVO.getServiceCd().equalsIgnoreCase(ApiConstants.SERVICE_CODE_ACH)){
			 paymentMethod = ApiConstants.PAYMENT_MTD_ACH;
		 }
		 
		 if(statusRequestVO.getOperation().equalsIgnoreCase("OrgStatus"))
         {
             logger.info(apiMsgId, "SmartServiceEngine", "Get Status API",
                 "ReconciliationController OrgStatus request",
                 "Start Processing Status API request for OrgStatus",
                 AmexLogger.Result.success,"",
                 "partnerId",statusRequestVO.getPartnerId(),
                 "organizationId", orgId,
                 "associatedOrgId",associatedOrgId,
                 "statusCategory",statusRequestVO.getOperation());
         }
		 if(statusRequestVO.getOperation().equalsIgnoreCase("AccStatus"))
         {

             logger.info(apiMsgId, "SmartServiceEngine", "Get Status API",
                 "ReconciliationController AccStatus request",
                 "Start Processing Status API request for AccStatus",
                 AmexLogger.Result.success,"",
                 "partnerId",statusRequestVO.getPartnerId(),
                 "organizationId", orgId,
                 "associatedOrgId",associatedOrgId,
                 "statusCategory",statusRequestVO.getOperation(),
                 "partnerAccountId",statusRequestVO.getAccId(),
                 "paymentMethod", paymentMethod);

         }

         if(statusRequestVO.getOperation().equalsIgnoreCase("PaymentStatus"))
         {
             logger.info(apiMsgId, "SmartServiceEngine", "Get Status API",
                 "ReconciliationController PaymentStatus request",
                 "Start Processing Status API request for PaymentStatus",
                 AmexLogger.Result.success,"",
                 "partnerId",statusRequestVO.getPartnerId(),
                 "organizationId", orgId,
                 "associatedOrgId",associatedOrgId,
                 "statusCategory",statusRequestVO.getOperation(),
                 "paymentMethod", paymentMethod,
                 "buyerPymtRefId", statusRequestVO.getPymtId());
         }
	 }
	 
	 public static String getReconciliationStatus(EnrollmentResponseType enrollmentResponseType) {
		 String status = ApiConstants.SUCCESS;

		 if (null != enrollmentResponseType) {
			 if (null != enrollmentResponseType.getError()) {
				 if(null != enrollmentResponseType.getError().getErrorDetails() && !enrollmentResponseType.getError().getErrorDetails().isEmpty()){
					 status = ApiConstants.FAIL;
				 }
			 }
		 }
		 return status;
	 }
	 
	 public static String getReconciliationStatus(AccountStatusResponseType accountStatusResponseType) {
		 String status = ApiConstants.SUCCESS;

		 if (null != accountStatusResponseType) {
			 if (null != accountStatusResponseType.getError()) {
				 if(null != accountStatusResponseType.getError().getErrorDetails() && !accountStatusResponseType.getError().getErrorDetails().isEmpty()){
					 status = ApiConstants.FAIL;
				 }
			 }
		 }
		 return status;
	 }
	 
	 public static String getReconciliationStatus(PymtStatusResponseType pymtStatusResponseType) {
		 String status = ApiConstants.SUCCESS;

		 if (null != pymtStatusResponseType) {
			 if (null != pymtStatusResponseType.getError()) {
				 if(null != pymtStatusResponseType.getError().getErrorDetails() && !pymtStatusResponseType.getError().getErrorDetails().isEmpty()){
					 status = ApiConstants.FAIL;
				 }
			 }
		 }
		 return status;
	 }
	 
	 public static String getReconciliationResponseCd(EnrollmentResponseType enrollmentResponseType) {
		 String responseCd = "";

		 if (null != enrollmentResponseType) {
			 if (null != enrollmentResponseType.getError()) {
				 if(null != enrollmentResponseType.getError().getErrorDetails() && !enrollmentResponseType.getError().getErrorDetails().isEmpty()){
					 responseCd =  enrollmentResponseType.getError().getErrorDetails().get(0).getCode();
				 }
			 }
		 }
		 return responseCd;
	 }
	 
	 public static String getReconciliationResponseDesc(EnrollmentResponseType enrollmentResponseType) {
		 String responseDesc = "";

		 if (null != enrollmentResponseType) {
			 if (null != enrollmentResponseType.getError()) {
				 if(null != enrollmentResponseType.getError().getErrorDetails() && !enrollmentResponseType.getError().getErrorDetails().isEmpty()){
					 responseDesc =  enrollmentResponseType.getError().getErrorDetails().get(0).getMessage();
				 }
			 }
		 }
		 return responseDesc;
	 }
	 
	 public void processSplunkLogger( ProcessPaymentResponseType responseType,String apiMsgId,ProcessPaymentRequestType request){
		 if(null!=responseType)
	        {
	            if(null!=responseType.getProcessPaymentResponse())
	            {
	                if(null!=responseType.getProcessPaymentResponse().getData())
	                {
	                    ProcessPaymentResponseData objProcessPaymentResponseData = responseType.getProcessPaymentResponse().getData();
	                    String response ="";
	                    if(SplunkLogger.ststus(responseType).equalsIgnoreCase("success"))
	                    {
	                        ProcessPaymentRequestData objProcessPaymentRequestData =  request.getProcessPaymentRequest().getData();
	                        logger.info(apiMsgId, "SmartServiceEngine", "Process Payment API",
	                            "SmartServiceEngineController  Process payment Response",
	                            "END Processing Status API request for Process payment",
	                            AmexLogger.Result.success,"","status",SplunkLogger.ststus(responseType),
	                            "partnerId",SplunkLogger.partnerId(objProcessPaymentResponseData),                    
	                            "partnerName", SplunkLogger.partnerName(objProcessPaymentResponseData),
	                            "buyerPymtRefId", SplunkLogger.buyerPymtRefId(objProcessPaymentResponseData),
	                            "organizationId", SplunkLogger.buyerOrgId(objProcessPaymentRequestData),
	                            "responseCode", SplunkLogger.ststus(responseType),
	                            "responseDesc", SplunkLogger.ststus(responseType),
	                            "paymentMethod", SplunkLogger.paymentMethod(objProcessPaymentRequestData),
	                            "buyerAccountId", SplunkLogger.buyeraccountId(objProcessPaymentRequestData)
	                            );
	                    }
	                    if(SplunkLogger.ststus(responseType).equalsIgnoreCase("fail"))
	                    {
	                        ProcessPaymentRequestData objProcessPaymentRequestData =  request.getProcessPaymentRequest().getData();
	                        logger.info(apiMsgId, "SmartServiceEngine", "Process Payment API",
	                            "SmartServiceEngineController  Process payment Response",
	                            "END Processing Status API request for Process payment",
	                            AmexLogger.Result.failure,"","status",SplunkLogger.ststus(responseType),
	                            "partnerId",SplunkLogger.partnerId(objProcessPaymentResponseData),                    
	                            // "partnerName", SplunkLogger.partnerName(objProcessPaymentResponseData),
	                            //"buyerPymtRefId", SplunkLogger.buyerPymtRefId(objProcessPaymentResponseData),
	                            "partnerName",SplunkLogger.partnerName(objProcessPaymentRequestData),
	                            "buyerPymtRefId",SplunkLogger.buyerPymtRefId(objProcessPaymentRequestData),
	                            "organizationId", SplunkLogger.buyerOrgId(objProcessPaymentRequestData),
	                            "responseCode", SplunkLogger.responseCode(objProcessPaymentResponseData),
	                            "responseDesc", SplunkLogger.responseDesc(objProcessPaymentResponseData),
	                            "paymentMethod", SplunkLogger.paymentMethod(objProcessPaymentRequestData),
	                            "buyerAccountId", SplunkLogger.buyeraccountId(objProcessPaymentRequestData)
	                            );
	                    }
	                }
	            }
	        }
	 }
	 
	 public void reconciliationOrgResponseSplunkLogger(EnrollmentResponseType enrollmentResponseType, String apiMsgId, StatusRequestVO statusRequestVO){
		 
		 String orgId = null;
	     String associatedOrgId = null;
	     String paymentMethod = null;
		 
		 if (statusRequestVO.getAssociateOrgId() != null && !("".equals(statusRequestVO.getAssociateOrgId().trim()))){
			 orgId = statusRequestVO.getOrgId();
			 associatedOrgId = statusRequestVO.getAssociateOrgId();
		 }else{
			 orgId = statusRequestVO.getOrgId();
			 associatedOrgId = "";
		 }
		 
		 String organizationCategory = null;
		 
		 if(statusRequestVO.getOrgId().startsWith(ApiConstants.ORG_SUPPLIER)){
			 organizationCategory = ApiConstants.CATEGORY_ORG_SUPPLIER;
		 }else{
			 organizationCategory = ApiConstants.CATEGORY_ORG_CUSTOMER; 
		 }
		
		 if(statusRequestVO.getOperation().equalsIgnoreCase("OrgStatus")){
			 String status = getReconciliationStatus(enrollmentResponseType);

			 if (status != null && "success".equalsIgnoreCase(status)){
				 logger.info(apiMsgId, "SmartServiceEngine", "Get Status API",
						 "ReconciliationController OrgStatus request",
						 "Start Processing Status API request for OrgStatus",
						 AmexLogger.Result.success,"","status",status,
						 "partnerId",statusRequestVO.getPartnerId(),
						 "organizationId", orgId,
						 "associatedOrgId",associatedOrgId,
						 "statusCategory",statusRequestVO.getOperation(),
						 "serviceCode",SplunkLogger.reconciliationServiceCode(enrollmentResponseType),
						 "organizationCategory",organizationCategory,
						 "orgName",enrollmentResponseType.getName(),
						 "organizationStatus",SplunkLogger.reconciliationStatus(enrollmentResponseType),
						 "orgStatusDesc",SplunkLogger.reconciliationDescription(enrollmentResponseType),
						 "orgStatusDate",SplunkLogger.reconciliationDate(enrollmentResponseType)
						 );
			 }else{
				 logger.info(apiMsgId, "SmartServiceEngine", "Get Status API",
						 "ReconciliationController OrgStatus request",
						 "Start Processing Status API request for OrgStatus",
						 AmexLogger.Result.success,"","status",status,
						 "partnerId",statusRequestVO.getPartnerId(),
						 "organizationId", orgId,
						 "associatedOrgId",associatedOrgId,
						 "statusCategory",statusRequestVO.getOperation(),
						 "serviceCode",SplunkLogger.reconciliationServiceCode(enrollmentResponseType),
						 "organizationCategory",organizationCategory,
						 "orgName",enrollmentResponseType.getName(),
						 "organizationStatus",SplunkLogger.reconciliationStatus(enrollmentResponseType),
						 "orgStatusDesc",SplunkLogger.reconciliationDescription(enrollmentResponseType),
						 "orgStatusDate",SplunkLogger.reconciliationDate(enrollmentResponseType),
						 "responseCode",getReconciliationResponseCd(enrollmentResponseType),
						 "responseDesc",getReconciliationResponseDesc(enrollmentResponseType));
			 }

		 }
		 
	 } 
	 
	 
	 public void reconciliationAccountResponseSplunkLogger(AccountStatusResponseType accountStatusResponseType, String apiMsgId, StatusRequestVO statusRequestVO){
		 
		 String orgId = "";
	     String associatedOrgId = "";
	     String paymentMethod = "";
		 
		 if (statusRequestVO.getAssociateOrgId() != null && !("".equals(statusRequestVO.getAssociateOrgId().trim()))){
			 orgId = statusRequestVO.getOrgId();
			 associatedOrgId = statusRequestVO.getAssociateOrgId();
		 }else{
			 orgId = statusRequestVO.getOrgId();
			 associatedOrgId = "";
		 }
		 
		 
		 if(statusRequestVO.getServiceCd()!= null && statusRequestVO.getServiceCd().equalsIgnoreCase(ApiConstants.SERVICE_CODE_CHECK)){
			 paymentMethod = ApiConstants.PAYMENT_MTD_CHECK;
		 }
		 else if(statusRequestVO.getServiceCd()!= null && statusRequestVO.getServiceCd().equalsIgnoreCase(ApiConstants.SERVICE_CODE_CARD)){
			 paymentMethod = ApiConstants.PAYMENT_MTD_CARD;
		 }
		 if(statusRequestVO.getServiceCd()!= null && statusRequestVO.getServiceCd().equalsIgnoreCase(ApiConstants.SERVICE_CODE_ACH)){
			 paymentMethod = ApiConstants.PAYMENT_MTD_ACH;
		 }
		 
		 if (statusRequestVO.getOperation().equalsIgnoreCase("AccStatus")){
			 String status = getReconciliationStatus(accountStatusResponseType);
			 		 
	         if (status != null && "success".equalsIgnoreCase(status)){
	                logger.info(apiMsgId, "SmartServiceEngine", "Get Status API",
	                    "ReconciliationController AccStatus Response",
	                    "End Processing Status API request for AccStatus-response",
	                    AmexLogger.Result.success,"","status",status,
	                    "partnerId",statusRequestVO.getPartnerId(),
						"organizationId", orgId,
						"associatedOrgId",associatedOrgId,
						"statusCategory",statusRequestVO.getOperation(),
	                    "partnerAccountId",statusRequestVO.getAccId(),
	                    "accountStatus",SplunkLogger.reconciliationAccountStatus(accountStatusResponseType),
	                    "accountStatusDesc",SplunkLogger.reconciliationAccountStatusDesc(accountStatusResponseType),
	                    "accountStatusDate",SplunkLogger.reconciliationAccountStatusDate(accountStatusResponseType),
	                    "paymentMethod",paymentMethod	                    
	                    );
	                    
	            }else{
	                logger.info(apiMsgId, "SmartServiceEngine", "Get Status API",
	                    "ReconciliationController AccStatus Response",
	                    "End Processing Status API request for AccStatus-response",
	                    AmexLogger.Result.success,"","status",status,
	                    "partnerId",statusRequestVO.getPartnerId(),
						"organizationId", orgId,
						"associatedOrgId",associatedOrgId,
						"statusCategory",statusRequestVO.getOperation(),
						"partnerAccountId",statusRequestVO.getAccId(),
	                    "accountStatus",SplunkLogger.reconciliationAccountStatus(accountStatusResponseType),
	                    "accountStatusDesc",SplunkLogger.reconciliationAccountStatusDesc(accountStatusResponseType),
	                    "accountStatusDate",SplunkLogger.reconciliationAccountStatusDate(accountStatusResponseType),
	                    "paymentMethod",paymentMethod
	                    );
	            }
			 
		 }
	 
	 }
	 
	 
public void reconciliationPaymentResponseSplunkLogger(PymtStatusResponseType pymtStatusResponseType, String apiMsgId, StatusRequestVO statusRequestVO){
		 
		String orgId = "";
	    String associatedOrgId = "";
	    String paymentMethod = "";
		 
		 if (statusRequestVO.getAssociateOrgId() != null && !("".equals(statusRequestVO.getAssociateOrgId().trim()))){
			 orgId = statusRequestVO.getOrgId();
			 associatedOrgId = statusRequestVO.getAssociateOrgId();
		 }else{
			 orgId = statusRequestVO.getOrgId();
			 associatedOrgId = "";
		 }
		 
		 if(pymtStatusResponseType.getSubscriptionId()!= null && pymtStatusResponseType.getSubscriptionId().toString().equalsIgnoreCase(ApiConstants.SERVICE_CODE_CHECK)){
			 paymentMethod = ApiConstants.PAYMENT_MTD_CHECK;
		 }
		 else if(pymtStatusResponseType.getSubscriptionId()!= null && pymtStatusResponseType.getSubscriptionId().toString().equalsIgnoreCase(ApiConstants.SERVICE_CODE_CARD)){
			 paymentMethod = ApiConstants.PAYMENT_MTD_CARD;
		 }
		 if(pymtStatusResponseType.getSubscriptionId()!= null && pymtStatusResponseType.getSubscriptionId().toString().equalsIgnoreCase(ApiConstants.SERVICE_CODE_ACH)){
			 paymentMethod = ApiConstants.PAYMENT_MTD_ACH;
		 }
		 
		 if (statusRequestVO.getOperation().equalsIgnoreCase("PaymentStatus")){
			 String status = getReconciliationStatus(pymtStatusResponseType);
			 
			 if (status != null && "success".equalsIgnoreCase(status)){
	                logger.info(apiMsgId, "SmartServiceEngine", "Get Status API",
	                    "ReconciliationController AccStatus Response",
	                    "End Processing Status API request for AccStatus-response",
	                    AmexLogger.Result.success,"","status",status,
	                    "partnerId",statusRequestVO.getPartnerId(),
						"organizationId", orgId,
						"associatedOrgId",associatedOrgId,
						"statusCategory",statusRequestVO.getOperation(),
						"paymentMethod", paymentMethod,
	                    "buyerOrgId",statusRequestVO.getOrgId(),
	                    "supplierOrgId",statusRequestVO.getAssociateOrgId(),
	                    "paymentStatusCode",SplunkLogger.reconciliationPymtStatus(pymtStatusResponseType),
	                    "paymentStatusDesc",SplunkLogger.reconciliationPymtStatusDesc(pymtStatusResponseType),
	                    "paymentStatusDate",SplunkLogger.reconciliationPymtStatusDate(pymtStatusResponseType)	                                       
	                    );
	                    
	            }else{
	                logger.info(apiMsgId, "SmartServiceEngine", "Get Status API",
	                    "ReconciliationController AccStatus Response",
	                    "End Processing Status API request for AccStatus-response",
	                    AmexLogger.Result.success,"","status",status,
	                    "partnerId",statusRequestVO.getPartnerId(),
						"organizationId", orgId,
						"associatedOrgId",associatedOrgId,
						"statusCategory",statusRequestVO.getOperation(),
						"paymentMethod", paymentMethod,
	                    "buyerOrgId",statusRequestVO.getOrgId(),
	                    "supplierOrgId",statusRequestVO.getAssociateOrgId(),
	                    "paymentStatusCode",SplunkLogger.reconciliationPymtStatus(pymtStatusResponseType),
	                    "paymentStatusDesc",SplunkLogger.reconciliationPymtStatusDesc(pymtStatusResponseType),
	                    "paymentStatusDate",SplunkLogger.reconciliationPymtStatusDate(pymtStatusResponseType)
	                    );
	            }
			 
			 }
			 
		 }
	 
 
	 public void getStatusSplunkLogger( GetStatusRequestType request,String apiMsgId,GetStatusResponseType responseType){
		 GetStatusRequestData  objGetStatusRequestData = request.getGetStatusRequest().getData();
	     GetStatusResponse getStatusResponse = responseType.getGetStatusResponse();
	     GetStatusResponseData objGetStatusResponseData =   responseType.getGetStatusResponse().getData();
	        if(SplunkLogger.statusCategory(request.getGetStatusRequest().getData()).equalsIgnoreCase("OrgStatus"))
	        {
	            String status = SplunkLogger.getStatus(getStatusResponse);
	            if (status != null && "success".equalsIgnoreCase(status)){
	                logger.info(apiMsgId, "SmartServiceEngine", "Get Status API",
	                    "SmartServiceEngineController OrgStatus Response",
	                    "End Processing Status API request for OrgStatus-response",
	                    AmexLogger.Result.success,"","status",status,
	                    "partnerId",SplunkLogger.partnerId(objGetStatusResponseData),
	                    "organizationId", SplunkLogger.organizationId(objGetStatusRequestData),
	                    "associatedOrgId",SplunkLogger.associatedOrgID(objGetStatusRequestData),
	                    "statusCategory",SplunkLogger.statusCategory(objGetStatusRequestData),
	                    "serviceCode",SplunkLogger.serviceCode(objGetStatusRequestData),
	                    "organizationCategory",SplunkLogger.organizationCategory(objGetStatusRequestData),
	                    "partnerName",SplunkLogger.partnerName(objGetStatusRequestData),
	                    "orgName",SplunkLogger.orgName(objGetStatusResponseData),
	                    "organizationStatus",SplunkLogger.orgStatus(objGetStatusResponseData),
	                    "orgStatusDesc",SplunkLogger.orgStatusDesc(objGetStatusResponseData),
	                    "orgStatusDate",SplunkLogger.orgStatusDate(objGetStatusResponseData)
	                    );
	            }else{
	                logger.info(apiMsgId, "SmartServiceEngine", "Get Status API",
	                    "SmartServiceEngineController OrgStatus Response",
	                    "End Processing Status API request for OrgStatus-response",
	                    AmexLogger.Result.success,"","status",status,
	                    "partnerId",SplunkLogger.partnerId(objGetStatusResponseData),
	                    "organizationId", SplunkLogger.organizationId(objGetStatusRequestData),
	                    "associatedOrgId",SplunkLogger.associatedOrgID(objGetStatusRequestData),
	                    "statusCategory",SplunkLogger.statusCategory(objGetStatusRequestData),
	                    "partnerAccountId",SplunkLogger.PartnerAccountId(objGetStatusRequestData),
	                    "serviceCode",SplunkLogger.serviceCode(objGetStatusRequestData),
	                    "organizationCategory",SplunkLogger.organizationCategory(objGetStatusRequestData),
	                    "partnerName",SplunkLogger.partnerName(objGetStatusRequestData),
	                    "orgName",SplunkLogger.orgName(objGetStatusResponseData),
	                    "responseCode",SplunkLogger.responseCode(objGetStatusResponseData),
	                    "responseDesc",SplunkLogger.responseDesc(objGetStatusResponseData),
	                    "organizationId",SplunkLogger.organizationId(objGetStatusResponseData),
	                    "partnerName",SplunkLogger.partnerName(objGetStatusResponseData),
	                    "organizationStatus",SplunkLogger.orgStatus(objGetStatusResponseData),
	                    "orgStatusDesc",SplunkLogger.orgStatusDesc(objGetStatusResponseData),
	                    "orgStatusDate",SplunkLogger.orgStatusDate(objGetStatusResponseData));
	            }
	        }
	        if(SplunkLogger.statusCategory(request.getGetStatusRequest().getData()).equalsIgnoreCase("AccStatus"))
	        {
	            String status = SplunkLogger.getStatus(getStatusResponse);
	            if (status != null && "success".equalsIgnoreCase(status)){
	                logger.info(apiMsgId, "SmartServiceEngine", "Get Status API",
	                    "SmartServiceEngineController AccStatus Response",
	                    "End Processing Status API request for AccStatus-response",
	                    AmexLogger.Result.success,"","status",status,
	                    "partnerId",SplunkLogger.partnerId(objGetStatusResponseData),
	                    "organizationId", SplunkLogger.organizationId(objGetStatusRequestData),
	                    "associatedOrgId",SplunkLogger.associatedOrgID(objGetStatusRequestData),
	                    "statusCategory",SplunkLogger.statusCategory(objGetStatusRequestData),
	                    "partnerAccountId",SplunkLogger.PartnerAccountId(objGetStatusRequestData),
	                    "accountStatus",SplunkLogger.accountStatus(objGetStatusResponseData),
	                    "accountStatusDesc",SplunkLogger.accountStatusDesc(objGetStatusResponseData),
	                    "accountStatusDate",SplunkLogger.accountStatusDate(objGetStatusResponseData),
	                    "paymentMethod",SplunkLogger.paymentMethod(objGetStatusRequestData),
	                    "partnerName",SplunkLogger.partnerName(objGetStatusResponseData));
	            }else{
	                logger.info(apiMsgId, "SmartServiceEngine", "Get Status API",
	                    "SmartServiceEngineController AccStatus Response",
	                    "End Processing Status API request for AccStatus-response",
	                    AmexLogger.Result.success,"","status",status,
	                    "partnerId",SplunkLogger.partnerId(objGetStatusResponseData),
	                    "organizationId", SplunkLogger.organizationId(objGetStatusRequestData),
	                    "associatedOrgId",SplunkLogger.associatedOrgID(objGetStatusRequestData),
	                    "statusCategory",SplunkLogger.statusCategory(objGetStatusRequestData),
	                    "partnerAccountId",SplunkLogger.PartnerAccountId(objGetStatusRequestData),
	                    "accountStatus",SplunkLogger.accountStatus(objGetStatusResponseData),
	                    "accountStatusDesc",SplunkLogger.accountStatusDesc(objGetStatusResponseData),
	                    "accountStatusDate",SplunkLogger.accountStatusDate(objGetStatusResponseData),
	                    "paymentMethod",SplunkLogger.paymentMethod(objGetStatusRequestData),
	                    "responseCode",SplunkLogger.responseCode(objGetStatusResponseData),
	                    "responseDesc",SplunkLogger.responseDesc(objGetStatusResponseData),
	                    "partnerName",SplunkLogger.partnerName(objGetStatusResponseData));
	            }
	        }
	        if(SplunkLogger.statusCategory(request.getGetStatusRequest().getData()).equalsIgnoreCase("PaymentStatus"))
	        {
	            String status = SplunkLogger.getStatus(getStatusResponse);
	            if (status != null && "success".equalsIgnoreCase(status)){
	                logger.info(apiMsgId, "SmartServiceEngine", "Get Status API",
	                    "SmartServiceEngineController PaymentStatus response",
	                    "End Processing Status API request for PaymentStatus-response",
	                    AmexLogger.Result.success,"","status",status,
	                    "partnerId",SplunkLogger.partnerId(objGetStatusResponseData),
	                    "organizationId", SplunkLogger.organizationId(objGetStatusRequestData),
	                    "associatedOrgId",SplunkLogger.associatedOrgID(objGetStatusRequestData),
	                    "statusCategory",SplunkLogger.statusCategory(objGetStatusRequestData),
	                    "partnerName",SplunkLogger.partnerName(objGetStatusRequestData),
	                    "paymentMethod", SplunkLogger.paymentMethod(objGetStatusRequestData),
	                    "paymentValue",SplunkLogger.paymentValue(objGetStatusResponseData),
	                    "buyerPymtRefId",SplunkLogger.buyerPymtRefId(objGetStatusResponseData),
	                    "buyerOrgId",SplunkLogger.buyerOrgId(objGetStatusResponseData),
	                    "supplierOrgId",SplunkLogger.supplierOrgId(objGetStatusResponseData),
	                    "supplierAccountId",SplunkLogger.supplierAccountId(objGetStatusResponseData),
	                    "paymentStatusCode",SplunkLogger.paymentStatusCode(objGetStatusResponseData),
	                    "paymentStatusDesc",SplunkLogger.paymentStatusDesc(objGetStatusResponseData),
	                    "paymentStatusDate",SplunkLogger.paymentStatusDate(objGetStatusResponseData),
	                    "buyerAccountId",SplunkLogger.buyerAccountId(objGetStatusResponseData),
	                    "currencyCode",SplunkLogger.currencyCode(objGetStatusResponseData)
	                        );
	            }else{
	                logger.info(apiMsgId, "SmartServiceEngine", "Get Status API",
	                    "SmartServiceEngineController PaymentStatus response",
	                    "End Processing Status API request for PaymentStatus-response",
	                    AmexLogger.Result.success,"","status",status,
	                    "partnerId",SplunkLogger.partnerId(objGetStatusResponseData),
	                    "organizationId", SplunkLogger.organizationId(objGetStatusRequestData),
	                    "associatedOrgId",SplunkLogger.associatedOrgID(objGetStatusRequestData),
	                    "statusCategory",SplunkLogger.statusCategory(objGetStatusRequestData),
	                    "partnerName",SplunkLogger.partnerName(objGetStatusRequestData),
	                    "paymentMethod", SplunkLogger.paymentMethod(objGetStatusRequestData),
	                    "responseCode",SplunkLogger.responseCode(objGetStatusResponseData),
	                    "responseDesc",SplunkLogger.responseDesc(objGetStatusResponseData),
	                    "paymentValue",SplunkLogger.paymentValue(objGetStatusResponseData),
	                    "buyerPymtRefId",SplunkLogger.buyerPymtRefId(objGetStatusResponseData),
	                    "buyerOrgId",SplunkLogger.buyerOrgId(objGetStatusResponseData),
	                    "supplierOrgId",SplunkLogger.supplierOrgId(objGetStatusResponseData),
	                    "supplierAccountId",SplunkLogger.supplierAccountId(objGetStatusResponseData),
	                    "buyerAccountId",SplunkLogger.buyerAccountId(objGetStatusResponseData),
	                    "currencyCode",SplunkLogger.currencyCode(objGetStatusResponseData)
	                        );
	            }
	        }
	 }
	 
	 public void getStatusApiSplunkLogger(GetStatusRequestType request,String apiMsgId){
		 GetStatusRequestData  objGetStatusRequestData = request.getGetStatusRequest().getData();
		 if(SplunkLogger.statusCategory(request.getGetStatusRequest().getData()).equalsIgnoreCase("OrgStatus"))
         {
             logger.info(apiMsgId, "SmartServiceEngine", "Get Status API",
                 "SmartServiceEngineController  OrgStatus request",
                 "Start Processing Status API request for OrgStatus",
                 AmexLogger.Result.success,"","partnerId",SplunkLogger.partnerId(objGetStatusRequestData),
                 "organizationId", SplunkLogger.organizationId(objGetStatusRequestData),
                 "associatedOrgId",SplunkLogger.associatedOrgID(objGetStatusRequestData),
                 "statusCategory",SplunkLogger.statusCategory(objGetStatusRequestData),
                 "serviceCode",SplunkLogger.serviceCode(objGetStatusRequestData),
                 "organizationCategory",SplunkLogger.organizationCategory(objGetStatusRequestData),
                 "partnerName",SplunkLogger.partnerName(objGetStatusRequestData)
                     );
         }

         if(SplunkLogger.statusCategory(request.getGetStatusRequest().getData()).equalsIgnoreCase("AccStatus"))
         {

             logger.info(apiMsgId, "SmartServiceEngine", "Get Status API",
                 "SmartServiceEngineController  AccStatus request",
                 "Start Processing Status API request for AccStatus",
                 AmexLogger.Result.success,"","partnerId",SplunkLogger.partnerId(objGetStatusRequestData),
                 "organizationId", SplunkLogger.organizationId(objGetStatusRequestData),
                 "associatedOrgId",SplunkLogger.associatedOrgID(objGetStatusRequestData),
                 "statusCategory",SplunkLogger.statusCategory(objGetStatusRequestData),
                 "partnerAccountId",SplunkLogger.PartnerAccountId(objGetStatusRequestData),
                 "partnerName",SplunkLogger.partnerName(objGetStatusRequestData),
                 "paymentMethod", SplunkLogger.paymentMethod(objGetStatusRequestData)
                     );

         }

         if(SplunkLogger.statusCategory(request.getGetStatusRequest().getData()).equalsIgnoreCase("PaymentStatus"))
         {
             logger.info(apiMsgId, "SmartServiceEngine", "Get Status API",
                 "SmartServiceEngineController  PaymentStatus request",
                 "Start Processing Status API request for PaymentStatus",
                 AmexLogger.Result.success,"","partnerId",SplunkLogger.partnerId(objGetStatusRequestData),
                 "organizationId", SplunkLogger.organizationId(objGetStatusRequestData),
                 "associatedOrgId",SplunkLogger.associatedOrgID(objGetStatusRequestData),
                 "statusCategory",SplunkLogger.statusCategory(objGetStatusRequestData),
                 "partnerName",SplunkLogger.partnerName(objGetStatusRequestData),
                 "paymentMethod", SplunkLogger.paymentMethod(objGetStatusRequestData),
                 "buyerPymtRefId", SplunkLogger.buyerPymtRefId(objGetStatusRequestData)
                     );
         }

	 }
	 
	 public void processPaymentApiSplunkLogger(ProcessPaymentRequestType request,String apiMsgId){
		 ProcessPaymentRequestData objProcessPaymentRequestData =  request.getProcessPaymentRequest().getData();

         logger.info(apiMsgId, "SmartServiceEngine", "Process Payment API",
             "SmartServiceEngineController  Process payment Request",
             "Start Processing Status API request for Process payment",AmexLogger.Result.success,"","partnerId",SplunkLogger.partnerId(objProcessPaymentRequestData),
             "partnerName",SplunkLogger.partnerName(objProcessPaymentRequestData),                    
             "organizationId", SplunkLogger.buyerOrgId(objProcessPaymentRequestData),
             "supplierOrgId",SplunkLogger.supplierOrgId(objProcessPaymentRequestData),
             "buyerPymtRefId",SplunkLogger.buyerPymtRefId(objProcessPaymentRequestData),
             "paymentValue",SplunkLogger.paymentValue(objProcessPaymentRequestData),
             "currencyCode",SplunkLogger.currencyCode(objProcessPaymentRequestData),
             "paymentMethod",SplunkLogger.paymentMethod(objProcessPaymentRequestData),
             "pymtUserIds",SplunkLogger.pymtUserIds(objProcessPaymentRequestData),
             "checkNo",SplunkLogger.checkNo(objProcessPaymentRequestData),
             "buyerAccountId",SplunkLogger.buyeraccountId(objProcessPaymentRequestData),
             "supplierAccountId",SplunkLogger.suppaccountId(objProcessPaymentRequestData)
             );

	 }
	 
	 public void manageEnrollmentApiSplunkLogger(ManageEnrollmentRequestData objManageEnrollmnetRequestData,String apiMsgId){
		 logger.info(apiMsgId, "SmartServiceEngine", "Manage Enrollment API",
	                "SmartServiceEngineController Manage Enrollment API",
	                "Start of processing Manage Enrollment API request",
	                AmexLogger.Result.success,"","partnerId",SplunkLogger.partnerId(objManageEnrollmnetRequestData), 
	                "organizationId", SplunkLogger.organizationId(objManageEnrollmnetRequestData),
	                "associatedOrgId",SplunkLogger.associatedOrgID(objManageEnrollmnetRequestData),
	                "enrollmentCategory", SplunkLogger.enrollmentCategory(objManageEnrollmnetRequestData),
	                "enrollmentActionType",SplunkLogger.enrollmentActionType(objManageEnrollmnetRequestData), 
	                "serviceCode",SplunkLogger.serviceCode(objManageEnrollmnetRequestData),
	                "orgName",SplunkLogger.orgName(objManageEnrollmnetRequestData), 
	                "partnerName",SplunkLogger.partnerName(objManageEnrollmnetRequestData),
	                "organizationStatus",SplunkLogger.organizationStatus(objManageEnrollmnetRequestData), 
	                "isAddEnrollmentRequest", Boolean.toString(SplunkLogger.isAddEnrollmentRequest(objManageEnrollmnetRequestData)));
	 }
	 
	 public void iCruseApiSplunkLogger( UpdateiCruseStatusRequestData requestData,String apiMsgId){
		 logger.info(apiMsgId, "SmartServiceEngine", "updateiCrusestatus API", "IcruseController update iCruse status API",
                 "Start of processing iCruse status update", AmexLogger.Result.success, "","partnerId", SplunkLogger.iCrusePartnerId(requestData),
                 "partnerName", SplunkLogger.iCrusePartnerName(requestData),
                 "iCruseTransactionId",SplunkLogger.iCruseTransactionId(requestData),
                 "iCruseAlertType",SplunkLogger.iCruseAlertType(requestData), "iCruseCheckType", SplunkLogger.iCruseCheckType(requestData),
                 "iCruseSicCode", SplunkLogger.iCruseSicCode(requestData),
                 "iCruseResult",SplunkLogger.iCruseResult(requestData),
                 "iCruseResponseType", SplunkLogger.iCruseResponseType(requestData));
	 }
	 
	 public void paymentsApiSplunkLogger(RetrievePayersRequestData requestData,String apiMsgId){
		 logger.info(
 			    apiMsgId,
 			    "SmartServiceEngine",
 				"Retrieve Payer IDs API",
 				"Processing Retrieve Payer IDs API",
 				"Start of retrieving payer IDs",
 				AmexLogger.Result.success, "", "partnerId", SplunkLogger.partnerId(requestData),
 				"partnerName", SplunkLogger.partnerName(requestData),
 				"organizationId", SplunkLogger.organizationId(requestData),
 				"partnerAccountId", SplunkLogger.partnerAccountId(requestData));
         
	 }
	 
	 public void logStatistics() {
	        Iterator<String> keyIterator = ThreadLocalManager.getRequestStatistics().keySet().iterator();
	        StringBuilder statisticsString = new StringBuilder("{ApiMsgID:["+ThreadLocalManager.getApiMsgId()+"]}");

	        while(keyIterator.hasNext()) {
	            String key = keyIterator.next();
	            statisticsString.append("{");
	            statisticsString.append(key);
	            statisticsString.append(":");
	            statisticsString.append(ThreadLocalManager.getRequestStatistics().get(key));
	            statisticsString.append("}");
	        }
	        logger.info(statisticsString.toString());

	        ThreadLocalManager.clearApiMsgId();
	        ThreadLocalManager.clearRequestStatistics();
	   }

}
