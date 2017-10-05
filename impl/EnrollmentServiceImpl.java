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

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;

import javax.annotation.Resource;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.americanexpress.ace.vng.dao.FetchCardAccountsDAO;
import com.americanexpress.ace.vng.enrollment.handler.VNGAccountHandler;
import com.americanexpress.ace.vng.enrollment.helper.VNGEnrollmentHelper;
import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.ApiErrorConstants;
import com.americanexpress.smartserviceengine.common.exception.SSEApplicationException;
import com.americanexpress.smartserviceengine.common.payload.AccountDetailsResponseType;
import com.americanexpress.smartserviceengine.common.payload.AccountDetailsType;
import com.americanexpress.smartserviceengine.common.payload.CheckAccountDetailsType;
import com.americanexpress.smartserviceengine.common.payload.CommonContext;
import com.americanexpress.smartserviceengine.common.payload.ContactDetailRespType;
import com.americanexpress.smartserviceengine.common.payload.ManageEnrollmentRequestData;
import com.americanexpress.smartserviceengine.common.payload.ManageEnrollmentRequestType;
import com.americanexpress.smartserviceengine.common.payload.ManageEnrollmentResponse;
import com.americanexpress.smartserviceengine.common.payload.ManageEnrollmentResponseData;
import com.americanexpress.smartserviceengine.common.payload.ManageEnrollmentResponseType;
import com.americanexpress.smartserviceengine.common.payload.OrganizationInfoType;
import com.americanexpress.smartserviceengine.common.payload.PaymentMethodType;
import com.americanexpress.smartserviceengine.common.payload.ServiceType;
import com.americanexpress.smartserviceengine.common.splunk.SplunkLogger;
import com.americanexpress.smartserviceengine.common.util.ApiUtil;
import com.americanexpress.smartserviceengine.common.util.CommonUtils;
import com.americanexpress.smartserviceengine.common.util.DateTimeUtil;
import com.americanexpress.smartserviceengine.common.util.EnvironmentPropertiesUtil;
import com.americanexpress.smartserviceengine.common.util.PartnerIdMapping;
import com.americanexpress.smartserviceengine.common.util.SpringThreadPooledExecutor;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.validator.EnrollmentRequestValidator;
import com.americanexpress.smartserviceengine.common.vo.AcctValidationVO;
import com.americanexpress.smartserviceengine.common.vo.ValidateEnrollmentRespVO;
import com.americanexpress.smartserviceengine.manager.CreateACHAccountManager;
import com.americanexpress.smartserviceengine.manager.CreateAccountManager;
import com.americanexpress.smartserviceengine.manager.CreateAchOrganizationManager;
import com.americanexpress.smartserviceengine.manager.CreateCardAccountManager;
import com.americanexpress.smartserviceengine.manager.CreateCardOrganizationManager;
import com.americanexpress.smartserviceengine.manager.CreateOrganizationManager;
import com.americanexpress.smartserviceengine.manager.DeleteACHAccountManager;
import com.americanexpress.smartserviceengine.manager.DeleteAccountManager;
import com.americanexpress.smartserviceengine.manager.DeleteAchOrganizationManager;
import com.americanexpress.smartserviceengine.manager.DeleteCardAccountManager;
import com.americanexpress.smartserviceengine.manager.DeleteCardOrganizationManager;
import com.americanexpress.smartserviceengine.manager.DeleteOrganizationManager;
import com.americanexpress.smartserviceengine.manager.PartnerValidationManager;
import com.americanexpress.smartserviceengine.manager.UpdateACHAccountManager;
import com.americanexpress.smartserviceengine.manager.UpdateAccountManager;
import com.americanexpress.smartserviceengine.manager.UpdateAchOrganizationManager;
import com.americanexpress.smartserviceengine.manager.UpdateCardAccountManager;
import com.americanexpress.smartserviceengine.manager.UpdateCardOrganizationManager;
import com.americanexpress.smartserviceengine.manager.UpdateOrganizationManager;
import com.americanexpress.smartserviceengine.service.EnrollmentService;
import com.americanexpress.smartserviceengine.util.EnrollmentUtils;

/**
 * This service class contains methods for processing enrolling service
 *
 */
@Service
public class EnrollmentServiceImpl implements EnrollmentService {

	private static AmexLogger logger = AmexLogger.create(EnrollmentServiceImpl.class);

	@Resource
	private EnrollmentUtils enrollmentUtils;
	
	@Resource
	private EnrollmentRequestValidator enrollmentRequestValidator;

	@Resource
	@Qualifier("createAccountManager")
	private CreateAccountManager createAccountManager;

	@Resource
	@Qualifier("createOrganizationManager")
	private CreateOrganizationManager createOrganizationManager;

	@Resource
	@Qualifier("createAchOrganizationManager")
	private CreateAchOrganizationManager createAchOrganizationManager;

	@Resource
	@Qualifier("updateOrganizationManager")
	private UpdateOrganizationManager updateOrganizationManager;

	@Resource
	@Qualifier("UpdateAchOrganizationManager")
	private UpdateAchOrganizationManager updateAchOrganizationManager;

	@Resource
	@Qualifier("updateAccountManager")
	private UpdateAccountManager updateAccountManager;

	@Resource
	private PartnerValidationManager partnerValidationManager;

	@Resource
	@Qualifier("deleteOrganizationManager")
	private DeleteOrganizationManager deleteOrganizationManager;

	@Resource
	@Qualifier("deleteAccountManager")
	private DeleteAccountManager deleteAccountManager;

	@Resource
	private TivoliMonitoring tivoliMonitoring;

	@Resource
	@Qualifier("createACHAccountManager")
	private CreateACHAccountManager createACHAccountManager;

	@Resource
	@Qualifier("updateACHAccountManager")
	private UpdateACHAccountManager updateACHAccountManager;

	@Resource
	@Qualifier("deleteACHAccountManager")
	private DeleteACHAccountManager deleteACHAccountManager;

	@Resource
	@Qualifier("deleteAchOrganizationManager")
	private DeleteAchOrganizationManager deleteAchOrganizationManager;
	
	@Resource
	@Qualifier("createCardOrganizationManager")
	private CreateCardOrganizationManager createCardOrganizationManager;
	
	@Resource
	@Qualifier("updateCardOrganizationManager")
	private UpdateCardOrganizationManager updateCardOrganizationManager;

	@Resource
	@Qualifier("createCardAccountManager")
	private CreateCardAccountManager createCardAccountManager;
	
	
	@Resource
	@Qualifier("updateCardAccountManager")
	private UpdateCardAccountManager updateCardAccountManager;
	
	@Resource
	@Qualifier("deleteCardOrganizationManager")
	private DeleteCardOrganizationManager deleteCardOrganizationManager;

	@Resource
	@Qualifier("deleteCardAccountManager")
	private DeleteCardAccountManager deleteCardAccountManager;
	
	@Resource
	private SpringThreadPooledExecutor cardPymtThreadPoolExecutor;
	
	@Resource
	private VNGEnrollmentHelper vngEnrollmentHelper;
	
	@Resource
    private FetchCardAccountsDAO fetchCardAccountsDAO;
	
	@Autowired
    private PartnerIdMapping partnerIdMapping;
	
	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.americanexpress.smartserviceengine.api.service.EnrollmentService#
	 * manageEnrollment(com.americanexpress.smartserviceengine.api.payload.pojo.
	 * ManageEnrollmentRequestData, java.lang.String)
	 */
	//2.0 changes – Added boolean isAPI2_0 to differentiate the request from INTACCT or TS/Sage
	@Override
	public ManageEnrollmentResponseType manageEnrollment(ManageEnrollmentRequestType requestType, boolean isAPI2_0, String apiMsgId) {
		logger.info(apiMsgId,"SmartServiceEngine", "Manage Enrollment API","Start of manageEnrollment service",
				"Processing Manage Enrollment API request",AmexLogger.Result.success, "");
	              final long startTimeStamp = System.currentTimeMillis();

		ManageEnrollmentResponseType responseType = new ManageEnrollmentResponseType();
		ManageEnrollmentResponse response = new ManageEnrollmentResponse();
		CommonContext commonResponseContext = new CommonContext();
		String responseCode = null;
		//boolean blnAcctSuccess = false;
		//boolean blnAcctFailure = false;
		List<AccountDetailsResponseType> respAcctDetails = null;
		ManageEnrollmentResponse orgResponse = null;
		ManageEnrollmentResponse orgAchResponse = null;
		ManageEnrollmentResponse orgCardResponse = null;
		String errorRespFlag = null;
		String organizationId = null;
		List<ValidateEnrollmentRespVO> validBusinessRespSuccessList = new ArrayList<ValidateEnrollmentRespVO>();
		List<ValidateEnrollmentRespVO> validBusinessRespFailureList = new ArrayList<ValidateEnrollmentRespVO>();
		boolean blnBusValidPartialSuccess = false;
		boolean blnFieldValidPartialSuccess = false;
		int acctSuccessCount = 0;
		int acctFailureCount = 0;
		int partialAccountSize = 0;
		String enrollmentCategory = new String();
		boolean blnAchOrgUpdFailure = false;
		boolean blnChkOrgUpdFailure = false;
		boolean blnCardOrgUpdFailure = false;

		String orgUpdServiceInd = null;

		try {

			/*
			 * Validate the Request Payload for common fields
			 */
			//2.0 changes – Setting boolean isAPI2_0 to enrollmentRequestValidator to differentiate the validations for INTACCT or TS/Sage
			//enrollmentRequestValidator.setACEAPI2_0(isAPI2_0);
			response = enrollmentRequestValidator.validateCommonEnrollmentRequest(requestType, apiMsgId);

			if(response != null && response.getData() != null && response.getData().getResponseCode() != null) {
				responseType.setManageEnrollmentResponse(response);
				final long endTimeStamp = System.currentTimeMillis();
				logger.info(apiMsgId,"SmartServiceEngine", "Manage Enrollment API","End of  manageEnrollment service",
                                    "Time taken to execute manageEnrollment",AmexLogger.Result.success, "", "Time ", SplunkLogger.timeStats(endTimeStamp-startTimeStamp)+" ms");

				return responseType;
			}
			/*
			 * If common field validation is successful, continue further
			 * processing
			 */
			commonResponseContext.setPartnerId(requestType.getManageEnrollmentRequest().getData()
					.getCommonRequestContext().getPartnerId());
			commonResponseContext.setRequestId(requestType.getManageEnrollmentRequest().getData()
					.getCommonRequestContext().getRequestId());
			commonResponseContext.setPartnerName(requestType.getManageEnrollmentRequest().getData()
					.getCommonRequestContext().getPartnerName());
			commonResponseContext.setTimestamp(ApiUtil.getCurrentTimeStamp());

			if (response != null) {

				logger.info(apiMsgId,"SmartServiceEngine", "Manage Enrollment API","Start of Validating Partner ID",
						"Validating Partner ID",AmexLogger.Result.success, "", "partnerId", SplunkLogger.partnerId(requestType.getManageEnrollmentRequest().getData()),
                        "partnerName", SplunkLogger.partnerName(requestType.getManageEnrollmentRequest().getData()));
				/**
				 * Partner Id eh-cache changes-Comparing partner id in request with list of partner ids in cache.
				 * If partner Id is valid PartnerValidationManager returns corresponding partner name.  
				 */
				String partnerName = partnerValidationManager.validatePartnerId(requestType.getManageEnrollmentRequest().getData()
						.getCommonRequestContext().getPartnerId(),apiMsgId);

				if (partnerName != null) {
					requestType.getManageEnrollmentRequest().getData().getCommonRequestContext().setPartnerName(partnerName);
						logger.info(apiMsgId,"SmartServiceEngine", "Manage Enrollment API","End of Validating Partner ID",
								"Validating Partner ID was successful",AmexLogger.Result.success, "", "partnerId", SplunkLogger.partnerId(requestType.getManageEnrollmentRequest().getData()),
                                "partnerName", SplunkLogger.partnerName(requestType.getManageEnrollmentRequest().getData()));
				}else{
					responseCode = ApiErrorConstants.SSEAPIEN078;
					enrollmentRequestValidator.setFailureResponse(commonResponseContext, responseCode, requestType.getManageEnrollmentRequest().getData().getOrganizationInfo(), null,response, true);
				}
			}

			/*
			 * If common field validation is failure, return error response
			 */

			if(response != null && response.getData() != null) {
				responseCode = response.getData().getResponseCode();
				if ( responseCode != null) {
					responseType.setManageEnrollmentResponse(response);
					final long endTimeStamp = System.currentTimeMillis();
					logger.info(apiMsgId,"SmartServiceEngine", "Manage Enrollment API","End of  manageEnrollment service",
	                                    "Time taken to execute manageEnrollment",AmexLogger.Result.success, "", "Time ", SplunkLogger.timeStats(endTimeStamp-startTimeStamp)+" ms");

	                                return responseType;
				}
			}

			ManageEnrollmentRequestData requestData = requestType.getManageEnrollmentRequest().getData();

			organizationId = requestData.getOrganizationId();
			String enrollmentActionType = requestData.getEnrollmentActionType();
			enrollmentCategory = requestData.getEnrollmentCategory();
			boolean blnOrgFieldValidation = false;
			Map<String, List<AcctValidationVO>> acctValidationVOMap = null;
			boolean blnFieldValidateAcct = false;
			
			if (!enrollmentCategory.equalsIgnoreCase(ApiConstants.CATEGORY_ACCT_ENROLL)) {
				// supplier roll back indicator value will be activate only in case of supplier activate api 
				// to avoid contact details validation, as for activate api we do not have request body				
				/*if(StringUtils.isNotBlank(requestData.getSupplierOrgRollbackInd()) && requestData.getSupplierOrgRollbackInd().equalsIgnoreCase(ApiConstants.ACTIVATE))
				{
					blnOrgFieldValidation=true;
				}else{*/
				blnOrgFieldValidation = enrollmentRequestValidator.validateOrgEnrollmentRequest(requestData,commonResponseContext, response, apiMsgId);
				//}
				/*
				 * If Org Field validation fails, return error response
				 */
				if (!blnOrgFieldValidation) {
					responseType.setManageEnrollmentResponse(response);
					final long endTimeStamp = System.currentTimeMillis();
					logger.info(apiMsgId,"SmartServiceEngine", "Manage Enrollment API","End of  manageEnrollment service",
	                                    "Time taken to execute manageEnrollment",AmexLogger.Result.success, "", "Time ", SplunkLogger.timeStats(endTimeStamp-startTimeStamp)+" ms");

	                                 return responseType;
				}

				if (enrollmentCategory.equalsIgnoreCase(ApiConstants.CATEGORY_CUST_ACCT_ENROLL)
						|| enrollmentCategory.equalsIgnoreCase(ApiConstants.CATEGORY_SUPP_ACCT_ENROLL)) {
					blnFieldValidateAcct = true;
				}
			} else {
				blnFieldValidateAcct = true;
			}

			if (blnFieldValidateAcct) {
				acctValidationVOMap = enrollmentRequestValidator.validateAcctEnrollmentRequest(requestData, apiMsgId);
				List<AcctValidationVO> acctValidRespCodeList = acctValidationVOMap.get(ApiConstants.ACCT_VALIDATION_RESP_CODE);
				String acctValidRespCode = null;
				if (acctValidRespCodeList != null && !acctValidRespCodeList.isEmpty()) {
					acctValidRespCode = acctValidRespCodeList.get(0).getErrorCode();
				}
				if (acctValidRespCode != null) {
					enrollmentRequestValidator.setFailureResponse(commonResponseContext, acctValidRespCode,requestData.getOrganizationInfo(),
							requestData.getOrganizationId(), response, true);
					responseType.setManageEnrollmentResponse(response);
					final long endTimeStamp = System.currentTimeMillis();
					logger.info(apiMsgId,"SmartServiceEngine", "Manage Enrollment API","End of  manageEnrollment service",
	                                    "Time taken to execute manageEnrollment",AmexLogger.Result.success, "", "Time ", SplunkLogger.timeStats(endTimeStamp-startTimeStamp)+" ms");
	                                  return responseType;
				} else {
					List<AcctValidationVO> successAcctValidList = acctValidationVOMap.get(ApiConstants.ACCT_VALIDATION_SUCCESS);
					List<AcctValidationVO> failureAcctValidList = acctValidationVOMap.get(ApiConstants.ACCT_VALIDATION_FAILURE);

					if (successAcctValidList != null && failureAcctValidList == null) {
						//blnFieldValidSuccess = true;
					} else if (successAcctValidList == null && failureAcctValidList != null) {
						//blnFieldValidFailure = true;
						/*List<AccountDetailsResponseType> acctDetailsResp = createFailureAcctRespList(failureAcctValidList);
						response.getData().setAcctDetails(acctDetailsResp);*/
						
						//blnFieldValidFailure = true;
						AcctValidationVO acctValidationVO = failureAcctValidList.get(0);
						List<AccountDetailsResponseType> acctDetailsResp = null;
						AccountDetailsResponseType acctCardDetailsResp = null;
						if(ApiConstants.ENROLLMNT_ACT_TYPE_CARD.equalsIgnoreCase(acctValidationVO.getPaymentMethod())){
							//Displaying card details in response type for Account - Card related validation failures
							acctDetailsResp = createCardFailureAcctRespList(failureAcctValidList);
							response.getData().setAcctDetails(acctDetailsResp);
						}else{
							//Displaying card details in response type for Account - Non Card related validation failures
							acctDetailsResp = createFailureAcctRespList(failureAcctValidList);
							response.getData().setAcctDetails(acctDetailsResp);
						}
						
						response.setStatus(ApiConstants.FAIL);
						//String respCode = acctDetailsResp.get(0).getResponseCode();
						//response.getData().setResponseCode(respCode);
						//response.getData().setResponseDesc(EnvironmentPropertiesUtil.getProperty(respCode));
						response.getData().setCommonResponseContext(commonResponseContext);
						response.getData().setOrganizationId(organizationId);
						responseType.setManageEnrollmentResponse(response);
						final long endTimeStamp = System.currentTimeMillis();
						logger.info(apiMsgId,"SmartServiceEngine", "Manage Enrollment API","End of  manageEnrollment service",
		                                    "Time taken to execute manageEnrollment",AmexLogger.Result.success, "", "Time ", SplunkLogger.timeStats(endTimeStamp-startTimeStamp)+" ms");
		                return responseType;
					} else if (successAcctValidList != null && failureAcctValidList != null) {
						blnFieldValidPartialSuccess = true;
						List<AccountDetailsResponseType> checkAcctDetailsResp = createFailureAcctRespList(failureAcctValidList);
						response.getData().setAcctDetails(checkAcctDetailsResp);
						response.setStatus(ApiConstants.PARTIAL_SUCCESS);
						response.getData().setCommonResponseContext(commonResponseContext);
						response.getData().setOrganizationId(organizationId);
						removeFailureAcctsFromRequest(requestData,failureAcctValidList);
					} else {
						enrollmentRequestValidator.setFailureResponse(commonResponseContext,ApiErrorConstants.SSEAPIEN002,requestData.getOrganizationInfo(),
								requestData.getOrganizationId(), response, true);
						responseType.setManageEnrollmentResponse(response);
						final long endTimeStamp = System.currentTimeMillis();
						logger.info(apiMsgId,"SmartServiceEngine", "Manage Enrollment API","End of  manageEnrollment service",
		                                    "Time taken to execute manageEnrollment",AmexLogger.Result.success, "", "Time ", SplunkLogger.timeStats(endTimeStamp-startTimeStamp)+" ms");
						return responseType;
					}
				}
			}

			/*
			 * Business Rule validations in SSE
			 */
			boolean achServiceSubscription = false;
			boolean cardServiceSubscription = false;
			if(ApiConstants.CATEGORY_CUST_ENROLL.equals(requestData.getEnrollmentCategory()) || ApiConstants.CATEGORY_SUPP_ENROLL.equals(requestData.getEnrollmentCategory())){
				//ACH Changes
				if(requestData.getOrganizationInfo() != null){
					List<ServiceType> serviceTypes = requestData.getOrganizationInfo().getSubscribeServices();
					if (serviceTypes != null && !serviceTypes.isEmpty()){
						if(CommonUtils.isAchServiceSubscribed(requestData.getOrganizationInfo().getSubscribeServices())){
							achServiceSubscription = true;
						}
						if(CommonUtils.isCardServiceSubscribed(requestData.getOrganizationInfo().getSubscribeServices())){
							cardServiceSubscription = true;
						}
					}else{
						if(requestData.getOrganizationInfo().getAchOrgDetails() != null){
							achServiceSubscription = true;
						}
						if(requestData.getOrganizationInfo().getCardOrgDetails() != null){
							cardServiceSubscription = true;
						}
					}
					List<ServiceType> serTypes=requestData.getOrganizationInfo().getUnsubscribeServices();
					if (serTypes != null && !serTypes.isEmpty()){
						if(CommonUtils.isAchServiceSubscribed(requestData.getOrganizationInfo().getUnsubscribeServices())){
							achServiceSubscription = true;
						}
						if(CommonUtils.isCardServiceSubscribed(requestData.getOrganizationInfo().getUnsubscribeServices())){
							cardServiceSubscription = true;
						}
					}
					orgUpdServiceInd = findServiceForUpdateOrg(requestData);
				}
			}
			logger.info("orgUpdServiceInd..:" + orgUpdServiceInd);

			Map<String, List<ValidateEnrollmentRespVO>> validateEnrollRespMap = performBusinessRuleValidations(requestData, apiMsgId, achServiceSubscription, cardServiceSubscription, orgUpdServiceInd);
			validBusinessRespSuccessList = validateEnrollRespMap.get(ApiConstants.BUSINESS_VALIDATION_SUCCESS);
			validBusinessRespFailureList = validateEnrollRespMap.get(ApiConstants.BUSINESS_VALIDATION_FAILURE);

			if (validBusinessRespSuccessList == null && validBusinessRespFailureList != null) {
				//blnBusValidFailure = true;
				if(requestData.getEnrollmentCategory().equals(ApiConstants.CATEGORY_ACCT_ENROLL)){
					ValidateEnrollmentRespVO validateEnrollmentRespVO = validBusinessRespFailureList.get(0);
					String respCode = validateEnrollmentRespVO.getAccRespCode();
					if (StringUtils.isNotBlank(respCode)) {
						List<AccountDetailsResponseType> cardAcctDetailsList =null;
						AccountDetailsResponseType acctCardDetailsResp = null;
						if(ApiConstants.ENROLLMNT_ACT_TYPE_CARD.equalsIgnoreCase(validateEnrollmentRespVO.getPaymentMethod())){
							//Displaying card details in response type for Account - Card related validation failures
							cardAcctDetailsList = createCardBusValidFailureAcctRespList(validateEnrollmentRespVO);
							response.getData().setAcctDetails(cardAcctDetailsList);
						}else{						
							AccountDetailsResponseType checkAcctDetailResp = new AccountDetailsResponseType();
							checkAcctDetailResp.setAccStatus(ApiConstants.STATUS_FAILED);
							checkAcctDetailResp.setPartnerAccountId(validateEnrollmentRespVO.getPartnerAcctId());
							checkAcctDetailResp.setPaymentMethod(validateEnrollmentRespVO.getPaymentMethod());
							checkAcctDetailResp.setResponseCode(respCode);
							checkAcctDetailResp.setResponseDesc(ApiUtil.getErrorDescription(respCode));
							List<AccountDetailsResponseType> achAcctDetails = new ArrayList<AccountDetailsResponseType>();
							achAcctDetails.add(checkAcctDetailResp);
							response.getData().setAcctDetails(achAcctDetails);
						}
					} else {
						response.getData().setResponseCode(respCode);
						response.getData().setResponseDesc(ApiUtil.getErrorDescription(respCode));
					}
				}else{
					String orgRespCode = validBusinessRespFailureList.get(0).getOrgRespCode();
					if (orgRespCode == null) {
						List<AccountDetailsResponseType> checkAcctDetailsResp = null;
						if(response != null &&response.getData() != null
								&& response.getData().getAcctDetails() != null
								&& !response.getData().getAcctDetails().isEmpty()) {
							checkAcctDetailsResp = response.getData().getAcctDetails();
						} else {
							checkAcctDetailsResp = new ArrayList<AccountDetailsResponseType>();
						}
						createBusValidateFailureAcctRespList(validBusinessRespFailureList, checkAcctDetailsResp);
						response.getData().setAcctDetails(checkAcctDetailsResp);
					} else {
						response.getData().setResponseCode(orgRespCode);
						response.getData().setResponseDesc(ApiUtil.getErrorDescription(orgRespCode));
					}
				}
				response.setStatus(ApiConstants.FAIL);
				response.getData().setCommonResponseContext(commonResponseContext);
				response.getData().setOrganizationId(organizationId);
				responseType.setManageEnrollmentResponse(response);
				final long endTimeStamp = System.currentTimeMillis();
				logger.info(apiMsgId,"SmartServiceEngine", "Manage Enrollment API","End of  manageEnrollment service",
                                    "Time taken to execute manageEnrollment",AmexLogger.Result.success, "", "Time ", SplunkLogger.timeStats(endTimeStamp-startTimeStamp)+" ms");
				return responseType;
			} else if (validBusinessRespSuccessList != null && validBusinessRespFailureList != null) {
				blnBusValidPartialSuccess = true;
				List<AccountDetailsResponseType> checkAcctDetailsResp = null;
				if(response != null &&response.getData() != null && response.getData().getAcctDetails() != null
						&& !response.getData().getAcctDetails().isEmpty()) {
					checkAcctDetailsResp = response.getData().getAcctDetails();
				} else {
					checkAcctDetailsResp = new ArrayList<AccountDetailsResponseType>();
				}
				createBusValidateFailureAcctRespList(validBusinessRespFailureList, checkAcctDetailsResp);
				response.getData().setAcctDetails(checkAcctDetailsResp);
				response.setStatus(ApiConstants.PARTIAL_SUCCESS);
				response.getData().setCommonResponseContext(commonResponseContext);
				response.getData().setOrganizationId(organizationId);
				removeBusFailureAcctsFromRequest(requestData,validBusinessRespFailureList);
			} else if (validBusinessRespSuccessList != null && validBusinessRespFailureList == null) {
				logger.info("Business Validation is Success");
			}else {
				enrollmentRequestValidator.setFailureResponse(commonResponseContext, ApiErrorConstants.SSEAPIEN002,requestData.getOrganizationInfo(),
						requestData.getOrganizationId(), response, true);
				responseType.setManageEnrollmentResponse(response);
				final long endTimeStamp = System.currentTimeMillis();
				logger.info(apiMsgId,"SmartServiceEngine", "Manage Enrollment API","End of  manageEnrollment service",
                                    "Time taken to execute manageEnrollment",AmexLogger.Result.success, "", "Time ", SplunkLogger.timeStats(endTimeStamp-startTimeStamp)+" ms");
				return responseType;
			}

			if (requestData != null) {
				requestType.getManageEnrollmentRequest().setData(requestData);
			}
			List<Object> responseList = null;
			List<Object> responseAchList = null;
			List<Object> responseCardList = null;
			String vngPymtIndicatorFlg =EnvironmentPropertiesUtil.getOtherPropertyValues(ApiConstants.ISVNGPYMTROUTEINDICATOR.toUpperCase());
			ExecutorService executorService = cardPymtThreadPoolExecutor.getExecutorService();
			// If EnrollActionType is ADD
			if (ApiConstants.ACTION_TYPE_ADD.equalsIgnoreCase(enrollmentActionType)) {
				boolean blnAccEnroll = false;
				if (!enrollmentCategory.equalsIgnoreCase(ApiConstants.CATEGORY_ACCT_ENROLL)) {
					//To enroll an customer or supplier organization
					if (achServiceSubscription) {
						//2.0 changes – setting boolean isAPI2_0 to ValidateEnrollmentRespVO.Used to differentiate request in manager layer.
						validBusinessRespSuccessList.get(0).setAPI2_0(isAPI2_0);
						responseAchList = createAchOrganizationManager.process(requestType, validBusinessRespSuccessList.get(0), apiMsgId);
					} else if(cardServiceSubscription){
						validBusinessRespSuccessList.get(0).setAPI2_0(isAPI2_0);
						responseCardList = createCardOrganizationManager.process(requestType, validBusinessRespSuccessList.get(0), apiMsgId);
					}else {
						//2.0 changes – setting boolean isAPI2_0 to ValidateEnrollmentRespVO.Used to differentiate request in manager layer.
						validBusinessRespSuccessList.get(0).setAPI2_0(isAPI2_0);
						responseList = createOrganizationManager.process(requestType, validBusinessRespSuccessList.get(0), apiMsgId);
					}

					if (responseList != null && !responseList.isEmpty()) {
						orgResponse = (ManageEnrollmentResponse) responseList.get(0);
						errorRespFlag = createOrganizationManager.getErrorResponseFlag();
						if (ApiConstants.FALSE.equalsIgnoreCase(errorRespFlag)
								&& (enrollmentCategory.equals(ApiConstants.CATEGORY_CUST_ACCT_ENROLL) || enrollmentCategory.equals(ApiConstants.CATEGORY_SUPP_ACCT_ENROLL))) {
							blnAccEnroll = true;// Flag for Account enrollment
						}
					}
					if (responseCardList != null && !responseCardList.isEmpty()) {
						orgCardResponse = (ManageEnrollmentResponse) responseCardList.get(0);
						errorRespFlag = createCardOrganizationManager.getErrorResponseFlag();
						if (ApiConstants.FALSE.equalsIgnoreCase(errorRespFlag) && (enrollmentCategory.equals(ApiConstants.CATEGORY_CUST_ACCT_ENROLL) || enrollmentCategory.equals(ApiConstants.CATEGORY_SUPP_ACCT_ENROLL))) {
							// Flag for Account enrollment
							blnAccEnroll = true;
						}
					}

					if (responseAchList != null && !responseAchList.isEmpty()) {
						orgAchResponse = (ManageEnrollmentResponse) responseAchList.get(0);
						errorRespFlag = createAchOrganizationManager.getErrorResponseFlag();
					}
				} else {
					blnAccEnroll = true;// Flag for Account enrollment

				}
				if (blnAccEnroll) {// If Request contains Account enrollment
					//2.0 changes – Passing boolean isAPI2_0 to createAccount method,used to differentiate account enrollment request in manager layer. 
					responseList = createAccount(requestType, responseType, response, validBusinessRespSuccessList, requestData, isAPI2_0 , apiMsgId);
					final long endTimeStamp = System.currentTimeMillis();
					logger.info(apiMsgId,"SmartServiceEngine", "Manage Enrollment API","End of  manageEnrollment service",
	                                    "Time taken to execute manageEnrollment",AmexLogger.Result.success, "", "Time ", SplunkLogger.timeStats(endTimeStamp-startTimeStamp)+" ms");
	                return responseType;
				}
			} else if (ApiConstants.ACTION_TYPE_UPDATE.equalsIgnoreCase(enrollmentActionType)) { //If enrollmentActionType is UPDATE
				boolean blnAccEnroll = false;
				boolean isError = false;
				if (!enrollmentCategory.equalsIgnoreCase(ApiConstants.CATEGORY_ACCT_ENROLL)) {
					String currentServiceEnrolled = null;
					ValidateEnrollmentRespVO validEnrollResp = null;
					if (validBusinessRespSuccessList != null && !validBusinessRespSuccessList.isEmpty()) {
						validEnrollResp = validBusinessRespSuccessList.get(0);
						validEnrollResp.setOrgUpdServiceInd(orgUpdServiceInd);
						
						if (validEnrollResp != null && validEnrollResp.getSrvcFlag() != null) {
							currentServiceEnrolled = validEnrollResp.getSrvcFlag().trim();
						}
						logger.info("requestData.getOrganizationInfo().getAchOrgDetails()" + requestData.getOrganizationInfo().getAchOrgDetails());
						logger.info("currentServiceEnrolled: " +currentServiceEnrolled);
						/*if (requestData.getOrganizationInfo() != null && requestData.getOrganizationInfo().getSubscribeServices() == null && currentServiceEnrolled != null) {
							responseList = validateAccountEnrollment(requestData,responseType, response, commonResponseContext, organizationId, currentServiceEnrolled);
							if(responseList!=null && !responseList.isEmpty()){
								final long endTimeStamp = System.currentTimeMillis();
								logger.info(apiMsgId,"SmartServiceEngine", "Manage Enrollment API","End of  manageEnrollment service",
				                                    "Time taken to execute manageEnrollment",AmexLogger.Result.success, "", "Time ", SplunkLogger.timeStats(endTimeStamp-startTimeStamp)+" ms");
								return responseType;
							}
						}*/

						// TODO Handle if the common fields are passed, only the corresponding already subscribed services will be updated
						if (orgUpdServiceInd.equals(ApiConstants.ORG_UPD_SRVC_IND_COMMON)
								|| orgUpdServiceInd.equals(ApiConstants.ORG_UPD_SRVC_IND_COMMON_PLUS_ACH)
								|| orgUpdServiceInd.equals(ApiConstants.ORG_UPD_SRVC_IND_COMMON_PLUS_CHK)) {
							logger.info("Inside Common Org Updates block "+ orgUpdServiceInd);
							 //Check if ACH service is already subscribed for this Org and update request comes with COMMON Elements
							if((currentServiceEnrolled != null && currentServiceEnrolled.equals(ApiConstants.CHAR_ONLY_ACH))
									&& orgUpdServiceInd.equals(ApiConstants.ORG_UPD_SRVC_IND_COMMON)){
								logger.info("Inside ACH block of update flow for Orgnaization subscribed for " + currentServiceEnrolled);
								responseAchList = updateAchOrganizationManager.process(requestType,orgUpdServiceInd, validBusinessRespSuccessList.get(0), apiMsgId);
							}
							
							if((currentServiceEnrolled != null && currentServiceEnrolled.equals(ApiConstants.CHAR_ONLY_CARD))
									&& orgUpdServiceInd.equals(ApiConstants.ORG_UPD_SRVC_IND_COMMON)){
								logger.info("Inside ACH block of update flow for Orgnaization subscribed for " + currentServiceEnrolled);
								responseCardList = updateCardOrganizationManager.process(requestType,orgUpdServiceInd, validBusinessRespSuccessList.get(0), apiMsgId);
							}

							 //Check if CHK service is already subscribed for this Org and update request comes with only COMMON Elements
							if((currentServiceEnrolled != null && currentServiceEnrolled.equals(ApiConstants.CHAR_ONLY_CHK))
									&& orgUpdServiceInd.equals(ApiConstants.ORG_UPD_SRVC_IND_COMMON)){
								logger.info("Inside check block of update flow for Orgnaization subscribed for " +currentServiceEnrolled);
								//Modifying OrgUpdServiceInd flag to update SSE DB with common Details for organization having prior check subscription
								orgUpdServiceInd = ApiConstants.ORG_UPD_SRVC_IND_CHK;
								logger.info("Modifying OrgUpdServiceInd flag to update SSE DB with common Details " +currentServiceEnrolled);

								if (validEnrollResp.getPayveOrgId() != null) {
									try{
										responseList = updateOrganizationManager.process(requestType, orgUpdServiceInd, validBusinessRespSuccessList.get(0), apiMsgId);
									} catch(SSEApplicationException e) {
										responseList = new ArrayList<Object>();
										setFailureResponse(responseType, response, commonResponseContext, organizationId, e);
										responseList.add(response);
									}
									if(isError){
									    final long endTimeStamp = System.currentTimeMillis();
									    logger.info(apiMsgId,"SmartServiceEngine", "Manage Enrollment API","End of  manageEnrollment service",
						                                    "Time taken to execute manageEnrollment",AmexLogger.Result.success, "", "Time ", SplunkLogger.timeStats(endTimeStamp-startTimeStamp)+" ms");
									    return responseType;
									}
								}
							}

							/*
							 * Check if Both services are already subscribed for this Org and update request comes with COMMON
							 * Elements OR update request comes with Common Elements Plus ACH or CHK specific fields
							 */

							if(((currentServiceEnrolled != null && (currentServiceEnrolled.contains(ApiConstants.CHAR_C) || currentServiceEnrolled.contains(ApiConstants.CHAR_A))) 
									&& orgUpdServiceInd.equals(ApiConstants.ORG_UPD_SRVC_IND_COMMON))
									|| ((orgUpdServiceInd.equals(ApiConstants.ORG_UPD_SRVC_IND_COMMON_PLUS_ACH))
											|| orgUpdServiceInd.equals(ApiConstants.ORG_UPD_SRVC_IND_COMMON_PLUS_CHK))){

								logger.info("Inside Common block of update flow for Organization subscribed for " + currentServiceEnrolled);
								if((!orgUpdServiceInd.equals(ApiConstants.ORG_UPD_SRVC_IND_COMMON) && ApiConstants.CHAR_ONLY_ACH.equals(currentServiceEnrolled)) ||
										(currentServiceEnrolled.contains(ApiConstants.CHAR_A) && (currentServiceEnrolled.contains(ApiConstants.CHAR_C) ||currentServiceEnrolled.contains(ApiConstants.CHAR_D)))) {
									responseAchList = updateAchOrganizationManager.process(requestType, orgUpdServiceInd, validBusinessRespSuccessList.get(0), apiMsgId);
								}

								if((!orgUpdServiceInd.equals(ApiConstants.ORG_UPD_SRVC_IND_COMMON) && ApiConstants.CHAR_ONLY_CHK.equals(currentServiceEnrolled)) ||
										(currentServiceEnrolled.contains(ApiConstants.CHAR_C) && (currentServiceEnrolled.contains(ApiConstants.CHAR_A) ||currentServiceEnrolled.contains(ApiConstants.CHAR_D)))) {
									try{
										if(orgUpdServiceInd.equals(ApiConstants.ORG_UPD_SRVC_IND_COMMON) && ApiConstants.CHAR_ONLY_CHK.equals(currentServiceEnrolled)) {
											//Modifying OrgUpdServiceInd flag to update SSE DB with common Details for organization having prior check subscription
											orgUpdServiceInd = ApiConstants.ORG_UPD_SRVC_IND_CHK;
										}
										if (responseAchList != null && !responseAchList.isEmpty()) {
											orgAchResponse = (ManageEnrollmentResponse) responseAchList.get(0);
											String respCode = orgAchResponse.getData().getResponseCode();
											if(StringUtils.isEmpty(respCode)){
												responseList = updateOrganizationManager.process(requestType, orgUpdServiceInd, validBusinessRespSuccessList.get(0), apiMsgId);
											}
										}else{
											responseList = updateOrganizationManager.process(requestType, orgUpdServiceInd, validBusinessRespSuccessList.get(0), apiMsgId);
										}
									} catch(SSEApplicationException e) {
										isError = true;
										responseList = new ArrayList<Object>();
										setFailureResponse(responseType, response, commonResponseContext, organizationId, e);
										responseList.add(response);
									}
									if(isError){
									    final long endTimeStamp = System.currentTimeMillis();
									    logger.info(apiMsgId,"SmartServiceEngine", "Manage Enrollment API","End of  manageEnrollment service",
						                                    "Time taken to execute manageEnrollment",AmexLogger.Result.success, "", "Time ", SplunkLogger.timeStats(endTimeStamp-startTimeStamp)+" ms");
									    return responseType;
									}
								}
								ManageEnrollmentResponse returnResponse=checkResponseCodes(currentServiceEnrolled,orgUpdServiceInd,responseList,responseAchList,organizationId);
                                if(returnResponse!=null){
                                        responseType.setManageEnrollmentResponse(returnResponse);
                                        final long endTimeStamp = System.currentTimeMillis();
                                        logger.info(apiMsgId,"SmartServiceEngine", "Manage Enrollment API","End of  manageEnrollment service",
                                            "Time taken to execute manageEnrollment",AmexLogger.Result.success, "", "Time ", SplunkLogger.timeStats(endTimeStamp-startTimeStamp)+" ms");
                                        return responseType;
                                }

							}
						}
						else if (achServiceSubscription || (orgUpdServiceInd.equals(ApiConstants.ORG_UPD_SRVC_IND_ACH)) || (orgUpdServiceInd.equals(ApiConstants.ORG_UPD_SRVC_IND_ADD_ACH))) {
							logger.info("Inside only ACH Subscription block");
							responseAchList = updateAchOrganizationManager.process(requestType, orgUpdServiceInd, validBusinessRespSuccessList.get(0), apiMsgId);
						} else if(orgUpdServiceInd.equals(ApiConstants.ORG_UPD_SRVC_IND_CHK) || (orgUpdServiceInd.equals(ApiConstants.ORG_UPD_SRVC_IND_ADD_CHK))) {
							try{
								responseList = updateOrganizationManager.process(requestType, orgUpdServiceInd, validBusinessRespSuccessList.get(0), apiMsgId);
							}catch(SSEApplicationException e){
								isError = true;
								responseList = new ArrayList<Object>();
								setFailureResponse(responseType, response, commonResponseContext, organizationId, e);
								responseList.add(response);
							}
							if(isError){
							    final long endTimeStamp = System.currentTimeMillis();
							    logger.info(apiMsgId,"SmartServiceEngine", "Manage Enrollment API","End of  manageEnrollment service",
				                                    "Time taken to execute manageEnrollment",AmexLogger.Result.success, "", "Time ", SplunkLogger.timeStats(endTimeStamp-startTimeStamp)+" ms");
							    return responseType;
							}
						} else if(orgUpdServiceInd.equals(ApiConstants.ORG_UPD_SRVC_IND_COMMON_PLUS_CARD) || orgUpdServiceInd.equals(ApiConstants.ORG_UPD_SRVC_IND_CARD)
								|| orgUpdServiceInd.equals(ApiConstants.ORG_UPD_SRVC_IND_ADD_CARD)){
							try{
								responseCardList = updateCardOrganizationManager.process(requestType, orgUpdServiceInd,validBusinessRespSuccessList.get(0),apiMsgId);
							} catch(SSEApplicationException e){
								isError = true;
								responseCardList = new ArrayList<Object>();
								setFailureResponse(responseType, response, commonResponseContext, organizationId, e);
								response.getData().setOrgCheckStatus(null);
								responseCardList.add(response);
							}
						} else if(orgUpdServiceInd.equals(ApiConstants.ORG_UPD_CTC_SHRTNAME) || orgUpdServiceInd.equals(ApiConstants.ORG_UPD_CTC_SHRTNAME_COMMON)){
							try{
								if(currentServiceEnrolled.contains(ApiConstants.CHAR_C)){
									responseList=updateOrganizationManager.process(requestType, orgUpdServiceInd, validBusinessRespSuccessList.get(0), apiMsgId);
								}
								else if(currentServiceEnrolled.contains(ApiConstants.CHAR_D)){
									responseCardList=updateCardOrganizationManager.process(requestType, orgUpdServiceInd,validBusinessRespSuccessList.get(0),apiMsgId);
								}
								else{
									//setting error response for only contactDetails update when org is not enrolled for card or check
									enrollmentRequestValidator.setFailureResponse(commonResponseContext, ApiErrorConstants.SSEAPIEN303,
											requestType.getManageEnrollmentRequest().getData().getOrganizationInfo(), null,response, true);
								}
							} catch(SSEApplicationException e){
								isError = true;
								responseList = new ArrayList<Object>();
								setFailureResponse(responseType, response, commonResponseContext, organizationId, e);
								responseList.add(response);
							}
						}
					}

					if (responseList != null && !responseList.isEmpty()) {
						orgResponse = (ManageEnrollmentResponse) responseList.get(0);
						errorRespFlag = updateOrganizationManager.getErrorResponseFlag();
						if(ApiConstants.TRUE.equals(errorRespFlag)){
							blnChkOrgUpdFailure = true;
						}

					}
					if (responseAchList != null && !responseAchList.isEmpty()) {
						orgAchResponse = (ManageEnrollmentResponse) responseAchList.get(0);
						errorRespFlag = updateAchOrganizationManager.getErrorResponseFlag();
						if(ApiConstants.TRUE.equals(errorRespFlag)){
							blnAchOrgUpdFailure = true;
						}
					}
					if(responseCardList != null && !responseCardList.isEmpty()) {
						orgCardResponse = (ManageEnrollmentResponse) responseCardList.get(0);
						errorRespFlag = updateCardOrganizationManager.getErrorResponseFlag();
						if(ApiConstants.TRUE.equals(errorRespFlag)){
							blnCardOrgUpdFailure  = true;
						}
					}
					logger.info("blnChkOrgUpdFailure "+blnChkOrgUpdFailure
							+" ,blnAchOrgUpdFailure "+blnAchOrgUpdFailure
							+" ,blnCardOrgUpdFailure "+blnCardOrgUpdFailure
							+" ,currentServiceEnrolled "+currentServiceEnrolled							
							+" ,validEnrollResp.getOrgName() "+validEnrollResp.getOrgName()
							+" ,requestData.getOrganizationInfo().getOrgName() "+(requestData.getOrganizationInfo()!=null?requestData.getOrganizationInfo().getOrgName():"")
							+" ,orgUpdServiceInd "+orgUpdServiceInd);
					
					logger.info("vngPymtIndicatorFlg:"+ vngPymtIndicatorFlg);
					
					if(ApiConstants.CHAR_Y.equals(vngPymtIndicatorFlg)){						
						if(requestData.getOrganizationInfo()!=null && requestData.getOrganizationInfo().getOrgName()!=null 
								&& currentServiceEnrolled.contains(ApiConstants.CHAR_D)
								&& (ApiConstants.CATEGORY_CUST_ENROLL.equalsIgnoreCase(requestData.getEnrollmentCategory()) ) 
								&& !(blnChkOrgUpdFailure || blnAchOrgUpdFailure || blnCardOrgUpdFailure) 
								&& (!requestData.getOrganizationInfo().getOrgName().equalsIgnoreCase(validEnrollResp.getOrgName())) ){
							/* cust org name update , currentServiceEnrolled will not come from sp for Unsubscription services */
							validEnrollResp.setUpdateCustOrgCommonFieldRequest(ApiConstants.TRUEBOOLEAN);							
							executorService.submit(new VNGAccountHandler(requestData.getOrganizationInfo().getOrgName(),apiMsgId, tivoliMonitoring,
													vngEnrollmentHelper,validEnrollResp,fetchCardAccountsDAO));
						}else if(orgUpdServiceInd.equals(ApiConstants.ORG_UPD_SRVC_IND_CARD) &&
								requestData.getOrganizationInfo() !=null && 
								(ApiConstants.CATEGORY_CUST_ENROLL.equalsIgnoreCase(requestData.getEnrollmentCategory()) ) &&
								(!CollectionUtils.isEmpty(requestData.getOrganizationInfo().getUnsubscribeServices())) &&
								ApiConstants.SERVICE_CODE_CARD.equalsIgnoreCase(requestData.getOrganizationInfo().getUnsubscribeServices().get(0).getServiceCode())){
							/* customer org unsubscription */
							executorService.submit(new VNGAccountHandler(requestData.getOrganizationInfo().getOrgName(),apiMsgId, tivoliMonitoring,
													vngEnrollmentHelper,validEnrollResp,fetchCardAccountsDAO));
						}
					
					}
				} else {
					blnAccEnroll = true;
				}

				/*
				 * If Request contains Account details
				 */
				if (blnAccEnroll) {
					responseList = updateAccount(requestType, responseType, response, validBusinessRespSuccessList, requestData, apiMsgId);
					final long endTimeStamp = System.currentTimeMillis();
					logger.info(apiMsgId,"SmartServiceEngine", "Manage Enrollment API","End of  manageEnrollment service",
	                                    "Time taken to execute manageEnrollment",AmexLogger.Result.success, "", "Time ", SplunkLogger.timeStats(endTimeStamp-startTimeStamp)+" ms");
					return responseType;
				}
			} else if (ApiConstants.ACTION_TYPE_DELETE.equalsIgnoreCase(enrollmentActionType)) {
				boolean blnAccEnroll = false;
				String currentServiceEnrolled = null;
				ValidateEnrollmentRespVO validEnrollResp = null;
				if (!enrollmentCategory.equalsIgnoreCase(ApiConstants.CATEGORY_ACCT_ENROLL)) {
					// TODO Handle if the common fields are passed, only the corresponding already subscribed services will be updated
					if (validBusinessRespSuccessList != null && !validBusinessRespSuccessList.isEmpty()) {
						validEnrollResp = validBusinessRespSuccessList.get(0);
						logger.info("validEnrollResp.getPayveOrgId(): " +validEnrollResp.getPayveOrgId());
						
						logger.info("validEnrollResp.getSrvcFlag() : " +validEnrollResp.getSrvcFlag());
						if (validEnrollResp != null && validEnrollResp.getSrvcFlag() != null){
							currentServiceEnrolled = validEnrollResp.getSrvcFlag().trim();
						}
						/*
						 * Check which service is already subscribed for this Org (A/C/B) and then invoke their corresponding OrganizationManager's process method
						 */
						if (currentServiceEnrolled != null && currentServiceEnrolled.equals(ApiConstants.CHAR_ONLY_ACH)){
							responseAchList = deleteAchOrganizationManager.process(requestType, validBusinessRespSuccessList.get(0), currentServiceEnrolled, apiMsgId);
						}else if (currentServiceEnrolled != null && currentServiceEnrolled.equals(ApiConstants.CHAR_ONLY_CHK)){
							responseList = deleteOrganizationManager.process(requestType, validBusinessRespSuccessList.get(0), currentServiceEnrolled, apiMsgId);
						}else if (currentServiceEnrolled != null && currentServiceEnrolled.equals(ApiConstants.CHAR_ONLY_CARD)){
							responseCardList = deleteCardOrganizationManager.process(requestType, validBusinessRespSuccessList.get(0), currentServiceEnrolled, apiMsgId);
						}else {//if (currentServiceEnrolled != null && (currentServiceEnrolled.contains(ApiConstants.CHAR_C) && currentServiceEnrolled.contains(ApiConstants.CHAR_A))){
//							responseAchList = deleteAchOrganizationManager.process(requestType, validBusinessRespSuccessList.get(0), currentServiceEnrolled, apiMsgId);
							try{
							responseList = deleteOrganizationManager.process(requestType, validBusinessRespSuccessList.get(0), currentServiceEnrolled, apiMsgId);
							}catch(SSEApplicationException e) {
								responseList = new ArrayList<Object>();
								ManageEnrollmentResponseData failureResponseData = new ManageEnrollmentResponseData();
								failureResponseData.setCommonResponseContext(commonResponseContext);
								failureResponseData.setResponseCode(e.getResponseCode());
								failureResponseData.setResponseDesc(e.getResponseDescription());
								failureResponseData.getCommonResponseContext().setTimestamp(ApiUtil.getCurrentTimeStamp());
								failureResponseData.setOrganizationId(organizationId);
								failureResponseData.setOrgCheckStatus(ApiConstants.STATUS_FAILED);
								failureResponseData.setOrgACHStatus(ApiConstants.STATUS_FAILED);
								response.setData(failureResponseData);
								response.setStatus(ApiConstants.FAIL);
								responseList.add(response);
							}
						}
						
						
						
					}
					if (responseList != null && !responseList.isEmpty()) {
						orgResponse = (ManageEnrollmentResponse) responseList.get(0);
						errorRespFlag = deleteOrganizationManager.getErrorResponseFlag();
						if (ApiConstants.FALSE.equalsIgnoreCase(errorRespFlag)
								&& (enrollmentCategory.equals(ApiConstants.CATEGORY_CUST_ACCT_ENROLL) || enrollmentCategory.equals(ApiConstants.CATEGORY_SUPP_ACCT_ENROLL))) {
							blnAccEnroll = true;
						}
					}

					if (responseAchList != null && !responseAchList.isEmpty()) {
						orgAchResponse = (ManageEnrollmentResponse) responseAchList.get(0);
						errorRespFlag = deleteAchOrganizationManager.getErrorResponseFlag();
						if (ApiConstants.FALSE.equalsIgnoreCase(errorRespFlag) && (enrollmentCategory.equals(ApiConstants.CATEGORY_CUST_ACCT_ENROLL)
								|| enrollmentCategory.equals(ApiConstants.CATEGORY_SUPP_ACCT_ENROLL))) {
							blnAccEnroll = true;
						}
					}
					
					if (responseCardList != null && !responseCardList.isEmpty()) {
						orgCardResponse = (ManageEnrollmentResponse) responseCardList.get(0);
						errorRespFlag = deleteCardOrganizationManager.getErrorResponseFlag();
						if (ApiConstants.FALSE.equalsIgnoreCase(errorRespFlag)
								&& (enrollmentCategory.equals(ApiConstants.CATEGORY_CUST_ACCT_ENROLL) || enrollmentCategory
										.equals(ApiConstants.CATEGORY_SUPP_ACCT_ENROLL))) {
							blnAccEnroll = true;
						}
					}
					
					logger.info("currentServiceEnrolled "+currentServiceEnrolled							
							+" ,requestData.getEnrollmentCategory() "+requestData.getEnrollmentCategory()
							+" ,errorRespFlag "+errorRespFlag);
				
					
					if(ApiConstants.CHAR_Y.equals(vngPymtIndicatorFlg)){						
												
						if(currentServiceEnrolled.contains(ApiConstants.CHAR_D) && (ApiConstants.CATEGORY_CUST_ENROLL.
							equalsIgnoreCase(requestData.getEnrollmentCategory()) ) && !BooleanUtils.toBoolean(errorRespFlag)){							
							validEnrollResp.setDeleteCustOrgRequest(ApiConstants.TRUEBOOLEAN);
							executorService.submit(new VNGAccountHandler(null,apiMsgId, tivoliMonitoring,vngEnrollmentHelper,validEnrollResp,fetchCardAccountsDAO));							
						}
					}
				} else {
					blnAccEnroll = true;
				}

				/*
				 * If Request contains Account details
				 */
				if (blnAccEnroll) {
					if (requestData.getAccountDetails().getAchAcctDetails()!=null && !requestData.getAccountDetails().getAchAcctDetails().isEmpty()){
						responseList = deleteACHAccountManager.process(requestType.getManageEnrollmentRequest().getData(), validBusinessRespSuccessList.get(0), apiMsgId);
					}else if (requestData.getAccountDetails().getCardAcctDetails()!=null && !requestData.getAccountDetails().getCardAcctDetails().isEmpty()){
						responseList = deleteCardAccountManager.process(requestType.getManageEnrollmentRequest().getData(),validBusinessRespSuccessList.get(0), apiMsgId);
						createCardResponseType(requestType, responseType, response, responseList, apiMsgId);
						return responseType;
					}else{
						responseList = deleteAccountManager.process(requestType.getManageEnrollmentRequest().getData(), validBusinessRespSuccessList.get(0), apiMsgId);
					}
					createResponseType(requestType, responseType, response, responseList, apiMsgId);
					final long endTimeStamp = System.currentTimeMillis();
					logger.info(apiMsgId,"SmartServiceEngine", "Manage Enrollment API","End of  manageEnrollment service",
	                                    "Time taken to execute manageEnrollment",AmexLogger.Result.success, "", "Time ", SplunkLogger.timeStats(endTimeStamp-startTimeStamp)+" ms");
					return responseType;
				}
			}
			if (orgResponse != null && respAcctDetails == null) {
				response = orgResponse;
				if (!ApiConstants.ACTION_TYPE_DELETE.equalsIgnoreCase(enrollmentActionType) && !enrollmentCategory.equalsIgnoreCase(ApiConstants.CATEGORY_ACCT_ENROLL)) {
					if (orgAchResponse != null) {
						logger.info("Org ACH Subscription Status :" + orgAchResponse.getData().getOrgACHStatus());
						String achOrgStatus = orgAchResponse.getData().getOrgACHStatus();
						String cardOrgStatus = orgAchResponse.getData().getOrgCardStatus();
						response.getData().setOrgACHStatus(achOrgStatus);
						if(cardOrgStatus != null){
							response.getData().setOrgCardStatus(cardOrgStatus);
						}
						if (achOrgStatus != null && ApiConstants.STATUS_FAILED.equals(achOrgStatus)) {
							response.getData().setResponseCode(orgAchResponse.getData().getResponseCode());
							response.getData().setResponseDesc(orgAchResponse.getData().getResponseDesc());
						}
					} else {
						if(!(orgUpdServiceInd.equalsIgnoreCase(ApiConstants.ORG_UPD_CTC_SHRTNAME) || orgUpdServiceInd.equalsIgnoreCase(ApiConstants.ORG_UPD_CTC_SHRTNAME_COMMON)))
						response.getData().setOrgACHStatus(null);
					}
				}
				if ((ApiConstants.TRUE).equals(errorRespFlag)) {
					response.setStatus(ApiConstants.FAIL);
				} else {
					response.setStatus(ApiConstants.SUCCESS);
				}
			}
			if (orgAchResponse != null && respAcctDetails == null) {
				response = orgAchResponse;
				if (!ApiConstants.ACTION_TYPE_DELETE.equalsIgnoreCase(enrollmentActionType) && !enrollmentCategory
						.equalsIgnoreCase(ApiConstants.CATEGORY_ACCT_ENROLL)){
				if (orgResponse!= null ){
					logger.info("Org CHK Subscription Status :" + orgResponse.getData().getOrgCheckStatus());
					String chkOrgStatus = orgResponse.getData().getOrgCheckStatus();
					String cardOrgStatus = orgResponse.getData().getOrgCardStatus();
					response.getData().setOrgCheckStatus(chkOrgStatus);
					if(cardOrgStatus != null){
						response.getData().setOrgCardStatus(cardOrgStatus);
					}
					if(chkOrgStatus != null && ApiConstants.STATUS_FAILED.equals(chkOrgStatus)) {
						response.getData().setResponseCode(orgResponse.getData().getResponseCode());
						response.getData().setResponseDesc(orgResponse.getData().getResponseDesc());
					}
				} else {
					response.getData().setOrgCheckStatus(null);
				}
			}
				if ((ApiConstants.TRUE).equals(errorRespFlag)) {
					response.setStatus(ApiConstants.FAIL);
				} else {
					response.setStatus(ApiConstants.SUCCESS);
				}
			}
			
			
			if (orgCardResponse != null && respAcctDetails == null) {
				response = orgCardResponse;
				if (!ApiConstants.ACTION_TYPE_DELETE.equalsIgnoreCase(enrollmentActionType) && !enrollmentCategory
						.equalsIgnoreCase(ApiConstants.CATEGORY_ACCT_ENROLL)){				
				if (orgResponse!= null){
					logger.info("Org CHK Subscription Status :" + orgResponse.getData().getOrgCheckStatus());
					String orgStatus = orgResponse.getData().getOrgCheckStatus();
					//String achOrgStatus = null;
					response.getData().setOrgCheckStatus(orgStatus);
					if(orgStatus != null && ApiConstants.STATUS_FAILED.equals(orgStatus)) {
						response.getData().setResponseCode(orgResponse.getData().getResponseCode());
						response.getData().setResponseDesc(orgResponse.getData().getResponseDesc());
					}
				}			
				}
				if ((ApiConstants.TRUE).equals(errorRespFlag)) {
					response.setStatus(ApiConstants.FAIL);
				} else {
					response.setStatus(ApiConstants.SUCCESS);
				}
			}

			if(((responseAchList != null && blnAchOrgUpdFailure) && (responseList != null &&  !blnChkOrgUpdFailure))
					||((responseAchList != null && !blnAchOrgUpdFailure) && (responseList != null && blnChkOrgUpdFailure))) {
				response.setStatus(ApiConstants.FAIL);
			}

			if (respAcctDetails != null && !respAcctDetails.isEmpty()) {
				if (orgResponse != null) {
					response = orgResponse;
				} else {
					buildCommonResponse(requestType, response, apiMsgId);
				}

				response.getData().setAcctDetails(respAcctDetails);
				String finalStatus = null;
				int totalAcctSize = respAcctDetails.size();
				if(acctSuccessCount == totalAcctSize && acctFailureCount ==0) {
					finalStatus = ApiConstants.SUCCESS;
				} else if(acctSuccessCount == 0 && acctFailureCount == totalAcctSize) {
					finalStatus = ApiConstants.FAIL;
				} else if((acctSuccessCount != partialAccountSize && acctFailureCount != partialAccountSize)
						|| (acctSuccessCount == partialAccountSize && blnBusValidPartialSuccess)
						|| (acctSuccessCount == partialAccountSize && blnFieldValidPartialSuccess)) {
					finalStatus = ApiConstants.PARTIAL_SUCCESS;
				}  else {
					finalStatus = ApiConstants.FAIL;
				}
				response.setStatus(finalStatus);
			}
		} catch (SSEApplicationException e) {
			responseCode = e.getResponseCode();
			setFailureResponse(commonResponseContext, responseCode, organizationId, response, orgUpdServiceInd,  enrollmentCategory);
			logger.error(apiMsgId,"SmartServiceEngine","Manage Enrollment API","Exception in manageEnrollment service",
					"Exception occured while processing service layer of ManageEnrollment Service",AmexLogger.Result.failure,
					"Exception in ManageEnrollment service layer ", e,"errorCode", responseCode, "errorDesc",ApiUtil.getErrorDescription(responseCode));
			if (null != responseCode && responseCode.equals(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD)) {
				logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_APPL_ERR_CD, TivoliMonitoring.SSE_APPL_ERR_MSG, apiMsgId));
			}
		} catch (Exception e) {
			responseCode = ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD;
			setFailureResponse(commonResponseContext, responseCode, organizationId, response, orgUpdServiceInd, enrollmentCategory);
			logger.error(apiMsgId,"SmartServiceEngine","Manage Enrollment API","Exception in manageEnrollment service",
					"Exception occured while processing service layer of ManageEnrollment Service",AmexLogger.Result.failure,
					"Exception in ManageEnrollment service layer ", e,"errorCode", responseCode, "errorDesc",ApiUtil.getErrorDescription(responseCode));
			logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_APPL_ERR_CD, TivoliMonitoring.SSE_APPL_ERR_MSG, apiMsgId));
		}
		responseType.setManageEnrollmentResponse(response);
		final long endTimeStamp = System.currentTimeMillis();
		logger.info(apiMsgId,"SmartServiceEngine", "Manage Enrollment API","End of  manageEnrollment service",
                    "Time taken to execute manageEnrollment",AmexLogger.Result.success, "", "Time ", SplunkLogger.timeStats(endTimeStamp-startTimeStamp)+" ms");
		return responseType;
	}

	private List<Object> validateAccountEnrollment(ManageEnrollmentResponseType responseType, ManageEnrollmentResponse response,
			CommonContext commonResponseContext, String organizationId, String currentServiceEnrolled) {
		List<Object> responseList;
		logger.info("Inside rejection Flow");
		responseList = new ArrayList<Object>();
		ManageEnrollmentResponseData failureResponseData = new ManageEnrollmentResponseData();
		if(currentServiceEnrolled.equals(ApiConstants.CHAR_ONLY_CHK)) {
			logger.info("ACH Service is not subscribed for Organization");
			failureResponseData.setResponseCode(ApiErrorConstants.SSEAPIEN184);
			failureResponseData	.setResponseDesc(ApiUtil.getErrorDescription(ApiErrorConstants.SSEAPIEN184));
		} else {
			logger.info("Check Service is not subscribed for Organization");
			failureResponseData.setResponseCode(ApiErrorConstants.SSEAPIEN200);
			failureResponseData.setResponseDesc(ApiUtil.getErrorDescription(ApiErrorConstants.SSEAPIEN200));
		}
		failureResponseData.setCommonResponseContext(commonResponseContext);
		failureResponseData.getCommonResponseContext().setTimestamp(ApiUtil.getCurrentTimeStamp());
		failureResponseData.setOrganizationId(organizationId);
		// failureResponseData.setOrgCheckStatus(ApiConstants.STATUS_FAILED);
		response.setData(failureResponseData);
		response.setStatus(ApiConstants.FAIL);
		responseList.add(response);
		responseType.setManageEnrollmentResponse(response);
		return responseList;
	}

	private List<Object> updateAccount(ManageEnrollmentRequestType requestType, ManageEnrollmentResponseType responseType,
			ManageEnrollmentResponse response, List<ValidateEnrollmentRespVO> validBusinessRespSuccessList,
			ManageEnrollmentRequestData requestData, String apiMsgId) throws SSEApplicationException, GeneralSecurityException {
		List<Object> responseList;
		if (requestData.getAccountDetails().getAchAcctDetails()!=null && !requestData.getAccountDetails().getAchAcctDetails().isEmpty()){
			responseList = updateACHAccountManager.process(requestType.getManageEnrollmentRequest().getData(), validBusinessRespSuccessList.get(0), apiMsgId);
		}else if (requestData.getAccountDetails().getCardAcctDetails()!=null && !requestData.getAccountDetails().getCardAcctDetails().isEmpty()){
			responseList = updateCardAccountManager.process(requestType.getManageEnrollmentRequest().getData(), validBusinessRespSuccessList.get(0), apiMsgId);
			createCardResponseType(requestType, responseType, response, responseList, apiMsgId);
			return responseList;
		}else{
			responseList = updateAccountManager.process(requestType.getManageEnrollmentRequest().getData(), validBusinessRespSuccessList.get(0), apiMsgId);
		}
		createResponseType(requestType, responseType, response, responseList, apiMsgId);
		return responseList;
	}

	//2.0 changes – Added boolean isAPI2_0 as parameter to createAccount method,used to differentiate account enrollment request in manager layer. 
	private List<Object> createAccount(ManageEnrollmentRequestType requestType, ManageEnrollmentResponseType responseType,
			ManageEnrollmentResponse response, List<ValidateEnrollmentRespVO> validBusinessRespSuccessList,
			ManageEnrollmentRequestData requestData,boolean isAPI2_0,String apiMsgId) throws SSEApplicationException, GeneralSecurityException {
		List<Object> responseList;
		if (requestData.getAccountDetails().getAchAcctDetails()!=null && !requestData.getAccountDetails().getAchAcctDetails().isEmpty()){
			//2.0 changes – Setting boolean isAPI2_0 to ValidateEnrollmentRespVO ,used to differentiate account enrollment request in manager layer.
			validBusinessRespSuccessList.get(0).setAPI2_0(isAPI2_0);
			responseList = createACHAccountManager.process(requestType.getManageEnrollmentRequest().getData(), validBusinessRespSuccessList.get(0), apiMsgId);
		}else if (requestData.getAccountDetails().getCardAcctDetails()!=null && !requestData.getAccountDetails().getCardAcctDetails().isEmpty()) {
			//2.0 changes – Setting boolean isAPI2_0 to ValidateEnrollmentRespVO ,used to differentiate account enrollment request in manager layer. 
			validBusinessRespSuccessList.get(0).setAPI2_0(isAPI2_0);
			responseList = createCardAccountManager.process(requestType.getManageEnrollmentRequest().getData(),validBusinessRespSuccessList.get(0), apiMsgId);
			createCardResponseType(requestType, responseType, response, responseList, apiMsgId);
			return responseList;
		}else{
			//2.0 changes – Setting boolean isAPI2_0 to ValidateEnrollmentRespVO ,used to differentiate account enrollment request in manager layer. 
			validBusinessRespSuccessList.get(0).setAPI2_0(isAPI2_0);
			responseList = createAccountManager.process(requestType.getManageEnrollmentRequest().getData(), validBusinessRespSuccessList.get(0), apiMsgId);
		}
		createResponseType(requestType, responseType, response, responseList, apiMsgId);
		return responseList;
	}

	private ManageEnrollmentResponse checkResponseCodes(String currentServiceEnrolled,String orgUpdServiceInd, List<Object> responseList,List<Object> responseAchList, String organizationId) {
		if((orgUpdServiceInd.equals(ApiConstants.ORG_UPD_SRVC_IND_COMMON) ||orgUpdServiceInd.equals(ApiConstants.ORG_UPD_SRVC_IND_COMMON_PLUS_ACH)) && 
				(currentServiceEnrolled.contains(ApiConstants.CHAR_A) && currentServiceEnrolled.contains(ApiConstants.CHAR_C))){
			if(responseAchList!=null){
				ManageEnrollmentResponse response=(ManageEnrollmentResponse) responseAchList.get(0);
				if(response!=null){
					String responseCode=response.getData().getResponseCode();
					if(responseCode!=null){
						response.getData().setOrganizationId(organizationId);
						response.getData().setOrgACHStatus(ApiConstants.STATUS_FAILED);
						return response;
					}
				}
			}

		}
		if(orgUpdServiceInd.equals(ApiConstants.ORG_UPD_SRVC_IND_COMMON_PLUS_CHK) && 
				(currentServiceEnrolled.contains(ApiConstants.CHAR_A) && currentServiceEnrolled.contains(ApiConstants.CHAR_C))){
			if(responseList!=null){
				ManageEnrollmentResponse response=(ManageEnrollmentResponse) responseList.get(0);
				if(response!=null){
					String responseCode=response.getData().getResponseCode();
					if(responseCode!=null){
						response.getData().setOrganizationId(organizationId);
						response.getData().setOrgCheckStatus(ApiConstants.STATUS_FAILED);
						return response;
					}
				}
			}

		}
		return null;
	}

	private void setFailureResponse(ManageEnrollmentResponseType responseType, ManageEnrollmentResponse response, CommonContext commonResponseContext, String organizationId,
			SSEApplicationException e) {
		ManageEnrollmentResponseData failureResponseData = new ManageEnrollmentResponseData();
		failureResponseData.setCommonResponseContext(commonResponseContext);
		failureResponseData.setResponseCode(e.getResponseCode());
		failureResponseData.setResponseDesc(e.getResponseDescription());
		failureResponseData.getCommonResponseContext().setTimestamp(ApiUtil.getCurrentTimeStamp());
		failureResponseData.setOrganizationId(organizationId);
		failureResponseData.setOrgCheckStatus(ApiConstants.STATUS_FAILED);
		response.setData(failureResponseData);
		response.setStatus(ApiConstants.FAIL);
		responseType.setManageEnrollmentResponse(response);
	}

	private List<AccountDetailsResponseType> createFailureAcctRespList(List<AcctValidationVO> acctValidationVOList) {
		List<AccountDetailsResponseType> acctDetailsRespList = new ArrayList<AccountDetailsResponseType>();
		AccountDetailsResponseType acctDetailResp = null;
		for (AcctValidationVO acctValidationVO : acctValidationVOList) {
			acctDetailResp = new AccountDetailsResponseType();
			acctDetailResp.setAccStatus(ApiConstants.STATUS_FAILED);
			acctDetailResp.setPartnerAccountId(acctValidationVO.getPartnerAccountId());
			if (acctValidationVO.getLineOfBusiness() != null) {
				acctDetailResp.setLineOfBusiness(acctValidationVO.getLineOfBusiness());
			}
			acctDetailResp.setPaymentMethod(acctValidationVO.getPaymentMethod());
			acctDetailResp.setResponseCode(acctValidationVO.getErrorCode());
			acctDetailResp.setResponseDesc(ApiUtil.getErrorDescription(acctValidationVO.getErrorCode()));
			acctDetailsRespList.add(acctDetailResp);
		}
		return acctDetailsRespList;
	}

	private void createBusValidateFailureAcctRespList(List<ValidateEnrollmentRespVO> validateEnrollmentRespVO, List<AccountDetailsResponseType> checkAcctDetailsResp) {
		AccountDetailsResponseType checkAcctDetailResp = null;
		for (ValidateEnrollmentRespVO acctValidationVO : validateEnrollmentRespVO) {
			checkAcctDetailResp = new AccountDetailsResponseType();
			checkAcctDetailResp.setAccStatus(ApiConstants.STATUS_FAILED);
			checkAcctDetailResp.setPartnerAccountId(acctValidationVO.getPartnerAcctId());
			if (acctValidationVO.getLineOfBusiness() != null) {
				checkAcctDetailResp.setLineOfBusiness(acctValidationVO.getLineOfBusiness());
			}
			checkAcctDetailResp.setPaymentMethod(ApiConstants.PAYMENT_MTD_CHECK);
			checkAcctDetailResp.setResponseCode(acctValidationVO.getAccRespCode());
			checkAcctDetailResp.setResponseDesc(acctValidationVO.getAccRespDesc());
			checkAcctDetailsResp.add(checkAcctDetailResp);
		}
	}

	public void setFailureResponse(CommonContext commonResponseContext, String errorCode, String organizationId,
			ManageEnrollmentResponse response, String orgUpdServiceInd, String enrollmentCategory) {
		ManageEnrollmentResponseData failureResponseData = new ManageEnrollmentResponseData();
		failureResponseData.setCommonResponseContext(commonResponseContext);
		failureResponseData.setResponseCode(errorCode);
		failureResponseData.setResponseDesc(ApiUtil.getErrorDescription(errorCode));
		failureResponseData.getCommonResponseContext().setTimestamp(ApiUtil.getCurrentTimeStamp());
		failureResponseData.setOrganizationId(organizationId);
		if(!enrollmentCategory.equals(ApiConstants.CATEGORY_ACCT_ENROLL) && orgUpdServiceInd != null && (orgUpdServiceInd.equals(ApiConstants.ORG_UPD_SRVC_IND_COMMON) || orgUpdServiceInd.equals(ApiConstants.ORG_UPD_SRVC_IND_COMMON_PLUS_ACH) || orgUpdServiceInd.equals(ApiConstants.ORG_UPD_SRVC_IND_COMMON_PLUS_CHK))){
			failureResponseData.setOrgCheckStatus(ApiConstants.STATUS_FAILED);
			failureResponseData.setOrgACHStatus(ApiConstants.STATUS_FAILED);
		}
		else if(!enrollmentCategory.equals(ApiConstants.CATEGORY_ACCT_ENROLL) && orgUpdServiceInd != null && (orgUpdServiceInd.equals(ApiConstants.ORG_UPD_SRVC_IND_ACH) || orgUpdServiceInd.equals(ApiConstants.ORG_UPD_SRVC_IND_ADD_ACH))){
			failureResponseData.setOrgACHStatus(ApiConstants.STATUS_FAILED);
		}
		else if(!enrollmentCategory.equals(ApiConstants.CATEGORY_ACCT_ENROLL) && orgUpdServiceInd != null && (orgUpdServiceInd.equals(ApiConstants.ORG_UPD_SRVC_IND_CHK) || orgUpdServiceInd.equals(ApiConstants.ORG_UPD_SRVC_IND_ADD_CHK))){
			failureResponseData.setOrgCheckStatus(ApiConstants.STATUS_FAILED);
		}
		response.setData(failureResponseData);
		response.setStatus(ApiConstants.FAIL);
	}

	private void buildCommonResponse(ManageEnrollmentRequestType requestWrapper,ManageEnrollmentResponse response, String apiMsgId)
			throws SSEApplicationException {
		ManageEnrollmentResponseData responseData = null;
		if (response.getData() != null) {
			responseData = response.getData();
		} else {
			responseData = new ManageEnrollmentResponseData();
		}
		// CommonContext Object
		CommonContext commonResponseContext = new CommonContext();
		commonResponseContext.setPartnerId(requestWrapper.getManageEnrollmentRequest().getData().getCommonRequestContext().getPartnerId());
		commonResponseContext.setRequestId(requestWrapper.getManageEnrollmentRequest().getData().getCommonRequestContext().getRequestId());
		commonResponseContext.setPartnerName(requestWrapper.getManageEnrollmentRequest().getData().getCommonRequestContext().getPartnerName());
		String timestamp = null;
		try {
			TimeZone timeZone = DateTimeUtil.getTimeZone(requestWrapper
					.getManageEnrollmentRequest().getData()
					.getCommonRequestContext().getTimestamp());
			timestamp = DateTimeUtil.getSystemTime(timeZone);
		} catch (Exception e) {
			logger.error(apiMsgId, "SmartServiceEngine", "Manage Enrollment API", "EnrollmentServiceImpl: buildCommonResponse",
					"After invoking addOrganizationAccounts service", AmexLogger.Result.failure, "Exception while getting the system current date", "");
		}
		commonResponseContext.setTimestamp(timestamp);
		responseData.setCommonResponseContext(commonResponseContext);
		responseData.setOrganizationId(requestWrapper.getManageEnrollmentRequest().getData().getOrganizationId());
		response.setData(responseData);
	}

	private Map<String, List<ValidateEnrollmentRespVO>> performBusinessRuleValidations(ManageEnrollmentRequestData requestData, String apiMsgId,
			Boolean achServiceSubscription, Boolean cardServiceSubscription, String orgUpdServiceInd) throws SSEApplicationException {

	    logger.info(apiMsgId,"SmartServiceEngine", "Manage Enrollment API","Start of performing Business Validations on Org Enrollment Request",
				"Performing Business Validations on Org Enrollment Request",AmexLogger.Result.success, "",
				"enrollmentCategory", SplunkLogger.enrollmentCategory(requestData),
				"enrollmentActionType", SplunkLogger.enrollmentActionType(requestData),
				"organizationId", SplunkLogger.organizationId(requestData),
                "associatedOrgId",SplunkLogger.associatedOrgID(requestData));
            final long startTimeStamp = System.currentTimeMillis();

		String actionType = requestData.getEnrollmentActionType();
		String categoryType = requestData.getEnrollmentCategory();
		Map<String, List<ValidateEnrollmentRespVO>> validateEnrollRespMap = new HashMap<String, List<ValidateEnrollmentRespVO>>();
		List<ValidateEnrollmentRespVO> validateEnrollRespList = null;
		List<ValidateEnrollmentRespVO> tmpValidateEnrollRespList = null;
		boolean blnValidateAcct = false;

		if (!categoryType.equalsIgnoreCase(ApiConstants.CATEGORY_ACCT_ENROLL)) {
			if (actionType.equalsIgnoreCase(ApiConstants.ACTION_TYPE_ADD)) {
				if (achServiceSubscription) {//ACH Changes
					validateEnrollRespList = createAchOrganizationManager.validateEnrollment(requestData, orgUpdServiceInd, apiMsgId);
				} else if(cardServiceSubscription){
					validateEnrollRespList = createCardOrganizationManager.validateEnrollment(requestData, orgUpdServiceInd, apiMsgId);
				}else {
					validateEnrollRespList = createOrganizationManager.validateEnrollment(requestData, orgUpdServiceInd, apiMsgId);
				}
			} else if (actionType.equalsIgnoreCase(ApiConstants.ACTION_TYPE_UPDATE)) {

				logger.info("orgUpdServiceInd upd: " + orgUpdServiceInd);
				logger.info("achServiceSubscription - upd: " + achServiceSubscription);

				if (achServiceSubscription) {//ACH Changes
					validateEnrollRespList = updateAchOrganizationManager.validateEnrollment(requestData, orgUpdServiceInd, apiMsgId);
				} else if(cardServiceSubscription){
					validateEnrollRespList = updateCardOrganizationManager.validateEnrollment(requestData, orgUpdServiceInd, apiMsgId);
				} else {
					validateEnrollRespList = updateOrganizationManager.validateEnrollment(requestData, orgUpdServiceInd, apiMsgId);
				}
				logger.info("orgUpdServiceInd: " + orgUpdServiceInd);
			} else if (actionType.equalsIgnoreCase(ApiConstants.ACTION_TYPE_DELETE)) {
				validateEnrollRespList = deleteOrganizationManager.validateEnrollment(requestData, orgUpdServiceInd, apiMsgId);
			}

			logger.info("validateEnrollRespList... " +validateEnrollRespList);
			if (validateEnrollRespList != null && !validateEnrollRespList.isEmpty()) {
				tmpValidateEnrollRespList = new ArrayList<ValidateEnrollmentRespVO>();
				ValidateEnrollmentRespVO validateEnrollmentRespVO = validateEnrollRespList.get(0);
				tmpValidateEnrollRespList.add(validateEnrollmentRespVO);
				String responseCode = validateEnrollmentRespVO.getSpRespCode();
				if ((ApiConstants.SP_SUCCESS_SSEINB02).equalsIgnoreCase(responseCode) || (ApiConstants.SP_SUCCESS_SSEIS000).equalsIgnoreCase(responseCode)
						|| (ApiConstants.SP_SUCCESS_SSEINB00).equalsIgnoreCase(responseCode)) {
					validateEnrollRespMap.put(ApiConstants.BUSINESS_VALIDATION_SUCCESS,tmpValidateEnrollRespList);
				} else {
					validateEnrollRespMap.put(ApiConstants.BUSINESS_VALIDATION_FAILURE,tmpValidateEnrollRespList);
				}

				/*
				 * If enroll category is CustAcct and SuppAcct enrollment and business validation for Org is successfull, perform
				 * business validation for account after Org enrollment
				 */

				if ((categoryType.equalsIgnoreCase(ApiConstants.CATEGORY_CUST_ACCT_ENROLL) || categoryType.equalsIgnoreCase(ApiConstants.CATEGORY_SUPP_ACCT_ENROLL))
						&& validateEnrollRespMap.get(ApiConstants.BUSINESS_VALIDATION_FAILURE) == null) {
					blnValidateAcct = true;
				}
			}
		} else {
			blnValidateAcct = true;
		}
		if (blnValidateAcct) {
			if (requestData.getAccountDetails().getAchAcctDetails()!=null && !requestData.getAccountDetails().getAchAcctDetails().isEmpty()){
				if (actionType.equalsIgnoreCase(ApiConstants.ACTION_TYPE_ADD)) {
					validateEnrollRespList = createACHAccountManager.validateEnrollment(requestData, apiMsgId);
				} else if (actionType.equalsIgnoreCase(ApiConstants.ACTION_TYPE_UPDATE)) {
					validateEnrollRespList = updateACHAccountManager.validateEnrollment(requestData, apiMsgId);
				} else if (actionType.equalsIgnoreCase(ApiConstants.ACTION_TYPE_DELETE)) {
					validateEnrollRespList = deleteACHAccountManager.validateEnrollment(requestData, apiMsgId);
				}
			}else{
				if (actionType.equalsIgnoreCase(ApiConstants.ACTION_TYPE_ADD)) {
					validateEnrollRespList = createAccountManager.validateEnrollment(requestData, apiMsgId);
				} else if (actionType.equalsIgnoreCase(ApiConstants.ACTION_TYPE_UPDATE)) {
					validateEnrollRespList = updateAccountManager.validateEnrollment(requestData, apiMsgId);
				} else if (actionType.equalsIgnoreCase(ApiConstants.ACTION_TYPE_DELETE)) {
					validateEnrollRespList = deleteAccountManager.validateEnrollment(requestData, apiMsgId);
				}
			}
			if (validateEnrollRespList != null && !validateEnrollRespList.isEmpty()) {
				List<ValidateEnrollmentRespVO> tmpSuccessValidateEnrollRespList = new ArrayList<ValidateEnrollmentRespVO>();
				List<ValidateEnrollmentRespVO> tmpFailureValidateEnrollRespList = new ArrayList<ValidateEnrollmentRespVO>();
				for (ValidateEnrollmentRespVO validateRespVo : validateEnrollRespList) {
					String responseCode = validateRespVo.getSpRespCode();
					if (ApiConstants.SP_SUCCESS_SSEINB02.equalsIgnoreCase(responseCode) || (ApiConstants.SP_SUCCESS_SSEINB00).equalsIgnoreCase(responseCode)) {
						tmpSuccessValidateEnrollRespList.add(validateRespVo);
					} else {
						tmpFailureValidateEnrollRespList.add(validateRespVo);
					}
				}
				if (!tmpSuccessValidateEnrollRespList.isEmpty()) {
					validateEnrollRespMap.put(ApiConstants.BUSINESS_VALIDATION_SUCCESS, tmpSuccessValidateEnrollRespList);
				}

				if (!tmpFailureValidateEnrollRespList.isEmpty()) {
					validateEnrollRespMap.put(ApiConstants.BUSINESS_VALIDATION_FAILURE, tmpFailureValidateEnrollRespList);
				}
			}
		}
		final long endTimeStamp = System.currentTimeMillis();
                logger.info("Time taken to execute performBusinessRuleValidations:"+(endTimeStamp-startTimeStamp) +" ms");

		logger.info(apiMsgId,"SmartServiceEngine", "Manage Enrollment API","End of performing Business Validations on Org Enrollment Request",
				"Performed Business Validations on Org Enrollment Request",AmexLogger.Result.success, "");
		   return validateEnrollRespMap;
	}

	private void removeFailureAcctsFromRequest(ManageEnrollmentRequestData origRequestData, List<AcctValidationVO> failureAcctValidationVOList) {
		if(null != origRequestData && null != origRequestData.getAccountDetails() && null != origRequestData.getAccountDetails().getCheckAcctDetails()){
			List<CheckAccountDetailsType> checkAcctDetailsList = origRequestData.getAccountDetails().getCheckAcctDetails();
			if(null != checkAcctDetailsList && !checkAcctDetailsList.isEmpty()){
				for (Iterator<CheckAccountDetailsType> iterator = checkAcctDetailsList
						.iterator(); iterator.hasNext();) {
					CheckAccountDetailsType checkAcctDetail = iterator.next();
					for (AcctValidationVO failureAcctValidVO : failureAcctValidationVOList) {
						if ( null != failureAcctValidVO && null != failureAcctValidVO.getPartnerAccountId()
								&& null != checkAcctDetail && null != checkAcctDetail.getPartnerAccountId() && failureAcctValidVO.getPartnerAccountId().equals(
										checkAcctDetail.getPartnerAccountId())
										&& failureAcctValidVO.getPaymentMethod().equals(ApiConstants.PAYMENT_MTD_CHECK)) {
							iterator.remove();
						} else {
							if(checkAcctDetail.getPartnerAccountId() == null) {
								logger.info("Null Partner Account Id is passed" );
								iterator.remove();
								break;
							}
						}
					}
				}
			}
		}
	}

	private void removeBusFailureAcctsFromRequest(
			ManageEnrollmentRequestData origRequestData,
			List<ValidateEnrollmentRespVO> busFailureAcctsList) {

		if (null != origRequestData
				&& null != origRequestData.getAccountDetails()
				&& null != origRequestData.getAccountDetails()
				.getCheckAcctDetails()) {
			List<CheckAccountDetailsType> checkAcctDetailsList = origRequestData
					.getAccountDetails().getCheckAcctDetails();
			if (null != checkAcctDetailsList && !checkAcctDetailsList.isEmpty()) {
				for (Iterator<CheckAccountDetailsType> iterator = checkAcctDetailsList
						.iterator(); iterator.hasNext();) {
					CheckAccountDetailsType checkAcctDetail = iterator.next();
					for (ValidateEnrollmentRespVO failureAcctValidVO : busFailureAcctsList) {
						if (null != failureAcctValidVO && null != failureAcctValidVO.getPartnerAcctId()
								&& null != checkAcctDetail
								&& null != checkAcctDetail.getPartnerAccountId()
								&& failureAcctValidVO.getPartnerAcctId().equals(
										checkAcctDetail.getPartnerAccountId())
										&& failureAcctValidVO.getPaymentMethod()
										.equals(ApiConstants.PAYMENT_MTD_CHECK)) {
							iterator.remove();
						}
					}
				}
			}
		}

	}


	private String findServiceForUpdateOrg(ManageEnrollmentRequestData requestData) {
		String orgUpdServiceInd = "";
		String enrollCategory = requestData.getEnrollmentCategory();
		String actionType = requestData.getEnrollmentActionType();
		//2.0 changes – <your change in brief>
		if ((requestData.getOrganizationInfo()!= null && (requestData.getOrganizationInfo().getSubscribeServices() == null || requestData.getOrganizationInfo().getSubscribeServices().isEmpty()))
				&& (enrollCategory.equals(ApiConstants.CATEGORY_CUST_ENROLL) || enrollCategory.equals(ApiConstants.CATEGORY_SUPP_ENROLL))
						&& actionType.equalsIgnoreCase(ApiConstants.ACTION_TYPE_UPDATE)) {
			logger.info("Inside Normal Fields updated block" );

			OrganizationInfoType orgInfo = requestData.getOrganizationInfo();
			if ((orgInfo.getOrgAddress() != null || orgInfo.getOrgName() != null)
					&& (orgInfo.getOrgCheckDetails() != null)) {
				logger.info("Both Check and Common fields are passed" );
				orgUpdServiceInd = ApiConstants.ORG_UPD_SRVC_IND_COMMON_PLUS_CHK;
			}

			else if ((orgInfo.getOrgAddress() != null || orgInfo.getOrgName() != null)
					&& (orgInfo.getAchOrgDetails() != null && (orgInfo.getAchOrgDetails().getDoingBusinessAsName() != null
					|| orgInfo.getAchOrgDetails().getOrgPhoneNumber() != null || orgInfo.getAchOrgDetails().getOrgEmailId() != null
					|| orgInfo.getAchOrgDetails().getMailingAddress() != null || orgInfo.getAchOrgDetails().getSupplierFeeInd() != null
					|| orgInfo.getAchOrgDetails().getAdminDetails() != null))) {
				logger.info("Both ACH and Common fields are passed" );
				orgUpdServiceInd = ApiConstants.ORG_UPD_SRVC_IND_COMMON_PLUS_ACH;
			}


			else if(orgInfo.getAchOrgDetails() != null && (orgInfo.getAchOrgDetails().getDoingBusinessAsName() != null
					|| orgInfo.getAchOrgDetails().getOrgPhoneNumber() != null || orgInfo.getAchOrgDetails().getOrgEmailId() != null
					|| orgInfo.getAchOrgDetails().getMailingAddress() != null || orgInfo.getAchOrgDetails().getAdminDetails() != null)){
				logger.info("Only ACH Specific fields are passed" );
				orgUpdServiceInd = ApiConstants.ORG_UPD_SRVC_IND_ACH;
			}

			else if(orgInfo.getOrgCheckDetails() != null){
				logger.info("Only CHECK Specific fields are passed" );
				orgUpdServiceInd = ApiConstants.ORG_UPD_SRVC_IND_CHK;
			}
			else if((orgInfo.getOrgAddress()!=null || orgInfo.getOrgName()!=null)
					&& (orgInfo.getCardOrgDetails()!=null && (orgInfo.getCardOrgDetails().getOrgEmailId()!=null || orgInfo.getCardOrgDetails().getAdminDetails()!= null ))){
				logger.info("Both Card and common fields are passed" );
				orgUpdServiceInd = ApiConstants.ORG_UPD_SRVC_IND_COMMON_PLUS_CARD;
			}
			else if(orgInfo.getCardOrgDetails()!=null && (orgInfo.getCardOrgDetails().getOrgEmailId()!=null || orgInfo.getCardOrgDetails().getAdminDetails()!=null)){
				logger.info("Only Card Specific fields are passed" );
				orgUpdServiceInd = ApiConstants.ORG_UPD_SRVC_IND_CARD;
			}
			else if((orgInfo.getContactDetails() !=null || orgInfo.getOrgShortName()!=null) && (orgInfo.getCardOrgDetails()==null
					&& orgInfo.getOrgCheckDetails() == null) && (orgInfo.getOrgAddress() != null || orgInfo.getOrgName() != null)){
				logger.info("only contact Details or orgShortName with common is passed which is applicable for card or check");
				orgUpdServiceInd = ApiConstants.ORG_UPD_CTC_SHRTNAME_COMMON;
			}
			else if((orgInfo.getContactDetails() !=null || orgInfo.getOrgShortName()!=null) && (orgInfo.getCardOrgDetails()==null 
					&& orgInfo.getOrgCheckDetails() == null)){
				logger.info("only contact Details or orgShortName is passed which is applicable for card or check");
				orgUpdServiceInd = ApiConstants.ORG_UPD_CTC_SHRTNAME;
			}
			else if (orgInfo.getOrgAddress() != null || orgInfo.getOrgName() != null) {
				logger.info("Only common fields are passed" );
				orgUpdServiceInd = ApiConstants.ORG_UPD_SRVC_IND_COMMON;
			}
		}else if ((requestData.getOrganizationInfo() != null && requestData.getOrganizationInfo().getSubscribeServices() != null)
				&& (enrollCategory.equals(ApiConstants.CATEGORY_CUST_ENROLL) || enrollCategory.equals(ApiConstants.CATEGORY_SUPP_ENROLL))
						&& actionType.equalsIgnoreCase(ApiConstants.ACTION_TYPE_UPDATE)){
			logger.info("Inside update block with subscribe services section" );

			OrganizationInfoType orgInfo = requestData.getOrganizationInfo();
			if ((orgInfo.getOrgAddress() != null || orgInfo.getOrgName() != null)
					&& (orgInfo.getContactDetails() != null || orgInfo.getOrgCheckDetails() != null || orgInfo.getOrgShortName() != null)) {
				orgUpdServiceInd = ApiConstants.ORG_UPD_SRVC_IND_COMMON_PLUS_CHK;
			}

			else if ((orgInfo.getOrgAddress() != null || orgInfo.getOrgName() != null) && (orgInfo.getAchOrgDetails() != null
					&& (orgInfo.getAchOrgDetails().getDoingBusinessAsName() != null || orgInfo.getAchOrgDetails().getOrgPhoneNumber() != null
					|| orgInfo.getAchOrgDetails().getOrgEmailId() != null || orgInfo.getAchOrgDetails().getMailingAddress() != null || orgInfo.getAchOrgDetails().getSupplierFeeInd() != null))) {
				orgUpdServiceInd = ApiConstants.ORG_UPD_SRVC_IND_COMMON_PLUS_ACH;
			}

			else if ((orgInfo.getSubscribeServices() != null  && !orgInfo.getSubscribeServices().isEmpty() && orgInfo.getSubscribeServices().get(0) != null
					&& orgInfo.getSubscribeServices().get(0).getServiceCode() != null &&
					orgInfo.getSubscribeServices().get(0).getServiceCode().equalsIgnoreCase(ApiConstants.SERVICE_CODE_ACH))
					&& (orgInfo.getAchOrgDetails() != null && (orgInfo.getAchOrgDetails().getDoingBusinessAsName() != null
					|| orgInfo.getAchOrgDetails().getOrgPhoneNumber() != null || orgInfo.getAchOrgDetails().getOrgEmailId() != null
					|| orgInfo.getAchOrgDetails().getMailingAddress() != null || orgInfo.getAchOrgDetails().getSupplierFeeInd() != null))){
				orgUpdServiceInd = ApiConstants.ORG_UPD_SRVC_IND_ADD_ACH;
			}

			else if ((orgInfo.getSubscribeServices() != null && !orgInfo.getSubscribeServices().isEmpty() && orgInfo.getSubscribeServices().get(0) != null && orgInfo.getSubscribeServices().get(0).getServiceCode() != null &&
					orgInfo.getSubscribeServices().get(0).getServiceCode().equalsIgnoreCase(ApiConstants.SERVICE_CODE_CHECK))
					&& (orgInfo.getContactDetails() != null || orgInfo.getOrgCheckDetails() != null || orgInfo.getOrgShortName() != null)){
				orgUpdServiceInd = ApiConstants.ORG_UPD_SRVC_IND_ADD_CHK;
			}
			
			else if((orgInfo.getSubscribeServices() != null && !orgInfo.getSubscribeServices().isEmpty() && orgInfo.getSubscribeServices().get(0) != null && orgInfo.getSubscribeServices().get(0).getServiceCode()!=null &&
					orgInfo.getSubscribeServices().get(0).getServiceCode().equalsIgnoreCase(ApiConstants.SERVICE_CODE_CARD))
					&& orgInfo.getCardOrgDetails() != null){
				orgUpdServiceInd = ApiConstants.ORG_UPD_SRVC_IND_ADD_CARD;
			}

			/*if(requestData.getOrganizationInfo().getAchOrgDetails() != null && requestData.getOrganizationInfo().getAchOrgDetails().getDoingBusinessAsName() != null
					|| requestData.getOrganizationInfo().getAchOrgDetails().getOrgPhoneNumber() != null
					|| requestData.getOrganizationInfo().getAchOrgDetails().getOrgEmailId() != null){

				orgUpdServiceInd = ApiConstants.ORG_UPD_SRVC_IND_ACH;
				return orgUpdServiceInd;
			}

			if(requestData.getOrganizationInfo().getContactDetails() != null
					|| requestData.getOrganizationInfo().getOrgCheckDetails() != null
					|| requestData.getOrganizationInfo().getOrgShortName() != null
					|| requestData.getOrganizationInfo().getTaxId() != null){

				orgUpdServiceInd = ApiConstants.ORG_UPD_SRVC_IND_CHK;
				return orgUpdServiceInd;
			}*/

			else if (orgInfo.getSubscribeServices() != null && !orgInfo.getSubscribeServices().isEmpty() && orgInfo.getSubscribeServices().get(0) != null && orgInfo.getSubscribeServices().get(0).getServiceCode() != null
					&& orgInfo.getSubscribeServices().get(0).getServiceCode().equalsIgnoreCase(ApiConstants.SERVICE_CODE_ACH)){
				orgUpdServiceInd = ApiConstants.ORG_UPD_SRVC_IND_ADD_ACH;
			}

			else if (orgInfo.getSubscribeServices() != null && !orgInfo.getSubscribeServices().isEmpty() && orgInfo.getSubscribeServices().get(0) != null && orgInfo.getSubscribeServices().get(0).getServiceCode() != null
					&& orgInfo.getSubscribeServices().get(0).getServiceCode().equalsIgnoreCase(ApiConstants.SERVICE_CODE_CHECK)){
				orgUpdServiceInd = ApiConstants.ORG_UPD_SRVC_IND_ADD_CHK;
			}
			
			else if(orgInfo.getSubscribeServices() != null && !orgInfo.getSubscribeServices().isEmpty() && orgInfo.getSubscribeServices().get(0) != null && orgInfo.getSubscribeServices().get(0).getServiceCode()!=null &&
					orgInfo.getSubscribeServices().get(0).getServiceCode().equalsIgnoreCase(ApiConstants.SERVICE_CODE_CARD)){
				orgUpdServiceInd = ApiConstants.ORG_UPD_SRVC_IND_ADD_CARD;
			}
		}

		 if (requestData.getOrganizationInfo() != null && requestData.getOrganizationInfo().getUnsubscribeServices() != null
				&& requestData.getOrganizationInfo().getUnsubscribeServices().get(0) != null){
			logger.info("Inside unsubscribeServices section" );

			if (requestData.getOrganizationInfo().getUnsubscribeServices().get(0).getServiceCode() != null &&
					requestData.getOrganizationInfo().getUnsubscribeServices().get(0).getServiceCode().equalsIgnoreCase(ApiConstants.SERVICE_CODE_ACH)){
				orgUpdServiceInd = ApiConstants.ORG_UPD_SRVC_IND_ACH;
			}

			else if (requestData.getOrganizationInfo().getUnsubscribeServices().get(0).getServiceCode() != null &&
					requestData.getOrganizationInfo().getUnsubscribeServices().get(0).getServiceCode().equalsIgnoreCase(ApiConstants.SERVICE_CODE_CHECK)){
				orgUpdServiceInd = ApiConstants.ORG_UPD_SRVC_IND_CHK;
			}
			
			else if(requestData.getOrganizationInfo().getUnsubscribeServices().get(0).getServiceCode() != null &&
					requestData.getOrganizationInfo().getUnsubscribeServices().get(0).getServiceCode().equalsIgnoreCase(ApiConstants.SERVICE_CODE_CARD)){
				orgUpdServiceInd = ApiConstants.ORG_UPD_SRVC_IND_CARD;
			}
		}
		logger.info( "Inside findServiceForUpdateOrg method , orgUpdServiceInd calculated :" + orgUpdServiceInd);
		return orgUpdServiceInd;
	}

	private void createResponseType(ManageEnrollmentRequestType requestType, ManageEnrollmentResponseType responseType,
			ManageEnrollmentResponse response, List<Object> responseList, String apiMsgId) throws SSEApplicationException {
		List<AccountDetailsResponseType> respAcctDetails;
		if (responseList != null && !responseList.isEmpty()) {
			int partialAccountSize = responseList.size();
			if(responseList != null && !responseList.isEmpty()){
				if(partialAccountSize == 1){
					AccountDetailsResponseType accountDetailsResponseType = (AccountDetailsResponseType)responseList.get(0);
					accountDetailsResponseType.setPaymentMethod(isACHAccount(requestType.getManageEnrollmentRequest().getData()) ? PaymentMethodType.ACH.getPaymentMethod() : PaymentMethodType.CHECK.getPaymentMethod());
					String respCode = accountDetailsResponseType.getResponseCode();
					if(StringUtils.isNotBlank(respCode)){
						response.setStatus(ApiConstants.FAIL);
					}else{
						response.setStatus(ApiConstants.SUCCESS);
					}
				}else if(partialAccountSize == 2){
					String chkRespCode = ((AccountDetailsResponseType)responseList.get(0)).getResponseCode();
					String achRespCode = ((AccountDetailsResponseType)responseList.get(1)).getResponseCode();
					if(StringUtils.isBlank(chkRespCode) && StringUtils.isBlank(achRespCode)){
						response.setStatus(ApiConstants.SUCCESS);
					}else if(StringUtils.isNotBlank(chkRespCode) && StringUtils.isNotBlank(achRespCode)){
						response.setStatus(ApiConstants.FAIL);
					}
				}
			}
			buildCommonResponse(requestType, response, apiMsgId);
			respAcctDetails = new ArrayList<AccountDetailsResponseType>();
			for (Object createResp : responseList) {
				respAcctDetails.add((AccountDetailsResponseType) createResp);
			}
			response.getData().setAcctDetails(respAcctDetails);
			responseType.setManageEnrollmentResponse(response);
		}
	}

	private boolean isACHAccount(ManageEnrollmentRequestData requestData){
		boolean isACHAccount = false;
		if(requestData != null){
			AccountDetailsType accountDetailsType = requestData.getAccountDetails();
			if(accountDetailsType != null){
				if(accountDetailsType.getAchAcctDetails() != null && !accountDetailsType.getAchAcctDetails().isEmpty()){
					isACHAccount =  true;
				}
			}
		}
		return isACHAccount;
	}
	
	
	private void createCardDeleteResponseType(ManageEnrollmentRequestType requestType, ManageEnrollmentResponseType responseType,
			ManageEnrollmentResponse response, String apiMsgId) throws SSEApplicationException {
			response.setStatus(ApiConstants.SUCCESS);
			buildCommonResponse(requestType, response, apiMsgId);
			response.getData().setOrgCardStatus("I");
			responseType.setManageEnrollmentResponse(response);		
	}

	private List<ValidateEnrollmentRespVO> validateCardOrgEnrollment(ManageEnrollmentRequestData requestData, String orgUpdServiceInd,String apiMsgId) {
		ValidateEnrollmentRespVO validateEnrollmentRespVO=new ValidateEnrollmentRespVO();
		List<ValidateEnrollmentRespVO> validateEnrollmentRespVOList=new ArrayList<ValidateEnrollmentRespVO>();
		if(requestData.getEnrollmentActionType().equals("ADD")){
			if(requestData.getOrganizationInfo().getContactDetails()==null){
				validateEnrollmentRespVO.setOrgRespCode(ApiErrorConstants.SSEAPIEN153);
				validateEnrollmentRespVOList.add(validateEnrollmentRespVO);    			
			}
			if(requestData.getOrganizationInfo().getOrgAddress()==null){
				validateEnrollmentRespVO.setOrgRespCode(ApiErrorConstants.SSEAPIEN135);
				validateEnrollmentRespVOList.add(validateEnrollmentRespVO);
			}
			if(requestData.getEnrollmentCategory().equals("2") && requestData.getOrganizationInfo().getCardOrgDetails()!=null && requestData.getOrganizationInfo().getCardOrgDetails().getOrgEmailId()==null){
				validateEnrollmentRespVO.setOrgRespCode(ApiErrorConstants.SSEAPIEN157);
				validateEnrollmentRespVOList.add(validateEnrollmentRespVO); 
			}
		}
		if(requestData.getEnrollmentActionType().equals("UPD")){
			List<ServiceType> services=requestData.getOrganizationInfo().getSubscribeServices();
			if(services!=null && services.get(0).getServiceCode()!=null){
				if(requestData.getOrganizationInfo().getOrgName()!=null || requestData.getOrganizationInfo().getOrgAddress()!=null){
					validateEnrollmentRespVO.setOrgRespCode(ApiErrorConstants.SSEAPIEN199);
					validateEnrollmentRespVOList.add(validateEnrollmentRespVO);    
				}
				if(requestData.getEnrollmentCategory().equals("2") && requestData.getOrganizationInfo().getCardOrgDetails()!=null && requestData.getOrganizationInfo().getCardOrgDetails().getOrgEmailId()==null){
					validateEnrollmentRespVO.setOrgRespCode(ApiErrorConstants.SSEAPIEN157);
					validateEnrollmentRespVOList.add(validateEnrollmentRespVO); 
				}
			}
		}
		return validateEnrollmentRespVOList;
	}
	
	public ManageEnrollmentResponse responseForAddCardOrgservice(ManageEnrollmentRequestData orgData,ManageEnrollmentResponse response){
		List<ContactDetailRespType> contactDetails=new ArrayList<ContactDetailRespType>();
		ContactDetailRespType contact=new ContactDetailRespType();
			contact.setContactId("C22332");
			contact.setContactName("james");
			contactDetails.add(contact);
			response.getData().setContactDetails(contactDetails);
			response.getData().setOrgCardStatus("A");
		response.getData().setOrganizationId(orgData.getOrganizationId());
		response.setStatus(ApiConstants.SUCCESS);
		return response;
	}
	
	public ManageEnrollmentResponse responseForUpdCardOrgservice(ManageEnrollmentRequestData orgData,ManageEnrollmentResponse response){
		List<ContactDetailRespType> contactDetails=new ArrayList<ContactDetailRespType>();
		ContactDetailRespType contact=new ContactDetailRespType();
			if(orgData.getOrganizationInfo().getUnsubscribeServices()!=null){
				response.getData().setOrgCardStatus("I");
			}
			else if(orgData.getOrganizationInfo().getSubscribeServices()!=null){
				contact.setContactId("C22332");
				contact.setContactName("james");
				contactDetails.add(contact);
				response.getData().setContactDetails(contactDetails);
				response.getData().setOrgCardStatus("A");
			}
			else{
				response.getData().setOrgCardStatus("A");
				if(orgData.getOrganizationInfo().getOrgAddress()!=null || orgData.getOrganizationInfo().getOrgName()!=null){
					response.getData().setOrgCheckStatus("P");
					response.getData().setOrgACHStatus("P");
				}
			}
		response.getData().setOrganizationId(orgData.getOrganizationId());
		response.setStatus(ApiConstants.SUCCESS);
		return response;
	}

	private ManageEnrollmentResponse responseForDelCardOrgservice(ManageEnrollmentRequestData requestData,ManageEnrollmentResponse response) {
		response.getData().setOrganizationId(requestData.getOrganizationId());
		response.getData().setOrgCardStatus("I");
		response.setStatus(ApiConstants.SUCCESS);
		return response;
	}
	

	
	private List<ValidateEnrollmentRespVO> validateCardAccountEnrollment(ManageEnrollmentRequestData requestData, String orgUpdServiceInd, String apiMsgId)
                throws SSEApplicationException {
		List<ValidateEnrollmentRespVO> validateEnrollmentRespVOList = new ArrayList<ValidateEnrollmentRespVO>();
		ValidateEnrollmentRespVO validateEnrollmentRespVO = new ValidateEnrollmentRespVO();    	
    	if(requestData.getAccountDetails()!=null ){    		
    			if( (requestData.getAccountDetails().getCardAcctDetails()==null||
    							requestData.getAccountDetails().getCardAcctDetails().size()<1)){
    				validateEnrollmentRespVO.setOrgRespCode(ApiErrorConstants.SSEAPIEN285);
    	        	validateEnrollmentRespVOList.add(validateEnrollmentRespVO);    				
    			}	        	
		}
		return validateEnrollmentRespVOList;
	}
	
	
	private List<Object> createCardResponseInfo(ManageEnrollmentRequestData requestData, String apiMsgId, String cardAcctStatus) throws SSEApplicationException {
				List<Object> responseList = new ArrayList<Object>();
				AccountDetailsResponseType enrollmentCardDetailsResponse = new AccountDetailsResponseType();
				
				enrollmentCardDetailsResponse.setPaymentMethod("CA");
				if(requestData.getAccountDetails().getCardAcctDetails().get(0)!=null 
						&& requestData.getAccountDetails().getCardAcctDetails().get(0).getPartnerAccountId()!=null){
					enrollmentCardDetailsResponse.setPartnerAccountId(requestData.getAccountDetails().getCardAcctDetails().get(0).getPartnerAccountId());	
				}							
				enrollmentCardDetailsResponse.setAccStatus(cardAcctStatus);						
				responseList.add(enrollmentCardDetailsResponse);
				return responseList;
	}
	private void createCardResponseType(ManageEnrollmentRequestType requestType, ManageEnrollmentResponseType responseType,
			ManageEnrollmentResponse response, List<Object> responseList, String apiMsgId) throws SSEApplicationException {
		List<AccountDetailsResponseType> respCardAcctDetails;
		
		if (responseList != null && !responseList.isEmpty()) {
			int partialAccountSize = responseList.size();
			if(responseList != null && !responseList.isEmpty()){
				if(partialAccountSize == 1){
					AccountDetailsResponseType cardDetailResp = (AccountDetailsResponseType)responseList.get(0);
					String respCode = cardDetailResp.getResponseCode();
					if(StringUtils.isNotBlank(respCode)){
						response.setStatus(ApiConstants.FAIL);
					}else{
						response.setStatus(ApiConstants.SUCCESS);
					}
				}else if(partialAccountSize == 2){
					
					String chkRespCode = ((AccountDetailsResponseType)responseList.get(0)).getResponseCode();
					String achRespCode = ((AccountDetailsResponseType)responseList.get(1)).getResponseCode();
					if(StringUtils.isBlank(chkRespCode) && StringUtils.isBlank(achRespCode)){
						response.setStatus(ApiConstants.SUCCESS);
					}else if(StringUtils.isNotBlank(chkRespCode) && StringUtils.isNotBlank(achRespCode)){
						response.setStatus(ApiConstants.FAIL);
					}
				}
			}
			
			buildCommonResponse(requestType, response, apiMsgId);
			respCardAcctDetails = new ArrayList<AccountDetailsResponseType>();
			for (Object createResp : responseList) {
				respCardAcctDetails.add((AccountDetailsResponseType) createResp);
			}			
			response.getData().setAcctDetails(respCardAcctDetails);			
			responseType.setManageEnrollmentResponse(response);
		}
	}
	
	private List<AccountDetailsResponseType> createCardFailureAcctRespList(List<AcctValidationVO> acctValidationVOList) {
		
		List<AccountDetailsResponseType> cardAcctDetailsList = new ArrayList<AccountDetailsResponseType>();
		AccountDetailsResponseType acctCardDetailResp = null;
		for (AcctValidationVO acctValidationVO : acctValidationVOList) {
			acctCardDetailResp = new AccountDetailsResponseType();
			acctCardDetailResp.setAccStatus(ApiConstants.STATUS_FAILED);
			acctCardDetailResp.setPartnerAccountId(acctValidationVO.getPartnerAccountId());
			acctCardDetailResp.setPaymentMethod(acctValidationVO.getPaymentMethod());
			acctCardDetailResp.setResponseCode(acctValidationVO.getErrorCode());
			acctCardDetailResp.setResponseDesc(ApiUtil.getErrorDescription(acctValidationVO.getErrorCode()));
			
		}
		cardAcctDetailsList.add(acctCardDetailResp);
		return cardAcctDetailsList;
	}
	
	
	private List<AccountDetailsResponseType> createCardBusValidFailureAcctRespList(ValidateEnrollmentRespVO validateEnrollmentRespVO) {
		List<AccountDetailsResponseType> cardAcctDetailsList = new ArrayList<AccountDetailsResponseType>();
		AccountDetailsResponseType acctCardDetailResp = new AccountDetailsResponseType();
		/*for business validation failure (account already enrolled) for Cards , we take this value from SP for other validation failure this would be empty*/
		if(StringUtils.isNotBlank(validateEnrollmentRespVO.getAccStatusCode())){
			acctCardDetailResp.setAccStatus(validateEnrollmentRespVO.getAccStatusCode());
		}else{
			acctCardDetailResp.setAccStatus(ApiConstants.STATUS_FAILED);	
		}
		acctCardDetailResp.setPartnerAccountId(validateEnrollmentRespVO.getPartnerAcctId());
		acctCardDetailResp.setPaymentMethod(validateEnrollmentRespVO.getPaymentMethod());
		acctCardDetailResp.setResponseCode(validateEnrollmentRespVO.getAccRespCode());
		acctCardDetailResp.setResponseDesc(ApiUtil.getErrorDescription(validateEnrollmentRespVO.getAccRespCode()));
		cardAcctDetailsList.add(acctCardDetailResp);
		return cardAcctDetailsList;
	}
}
