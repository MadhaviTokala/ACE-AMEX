package com.americanexpress.smartserviceengine.service.impl;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.ApiErrorConstants;
import com.americanexpress.smartserviceengine.common.exception.SSEApplicationException;
import com.americanexpress.smartserviceengine.common.payload.v2.AccountStatusResponseType;
import com.americanexpress.smartserviceengine.common.payload.v2.EnrollmentResponseType;
import com.americanexpress.smartserviceengine.common.payload.v2.PymtStatusResponseType;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.validator.ReconciliationValidator;
import com.americanexpress.smartserviceengine.common.vo.StatusRequestVO;
import com.americanexpress.smartserviceengine.manager.PartnerValidationManager;
import com.americanexpress.smartserviceengine.manager.ReconciliationAccManager;
import com.americanexpress.smartserviceengine.manager.ReconciliationOrgManager;
import com.americanexpress.smartserviceengine.manager.ReconciliationPymtManager;
import com.americanexpress.smartserviceengine.service.ReconciliationService;

@Service
public class ReconciliationServiceImpl implements ReconciliationService{

	private static AmexLogger logger = AmexLogger.create(ReconciliationServiceImpl.class);
	
	@Resource
	private PartnerValidationManager partnerValidationManager;

	@Resource
    private TivoliMonitoring tivoliMonitoring;
	
	@Resource
	private ReconciliationOrgManager reconciliationOrgManager;
	
	@Resource
	private ReconciliationAccManager reconciliationAccManager;
	
	@Resource
	private ReconciliationPymtManager reconciliationPymtManager;

	@Override
	public EnrollmentResponseType getOrgStatus(StatusRequestVO statusRequestVO) {
		EnrollmentResponseType enrollmentResponseType = new EnrollmentResponseType();
		String responseCode = ReconciliationValidator.validateReconOrgRequest(statusRequestVO);
		
		if(null != responseCode){
			return ReconciliationValidator.buildOrgFailureResponse(responseCode, ApiConstants.VALIDATION_ERROR);
		}
		
		try{
		String partnerName = partnerValidationManager.validatePartnerId(statusRequestVO.getPartnerId(), statusRequestVO.getApiMsgId());
		
		logger.info("PartnerName:"+partnerName);
		
		if (partnerName != null) {
				enrollmentResponseType = reconciliationOrgManager.getOrgStatus(statusRequestVO);
		}else{
			enrollmentResponseType = ReconciliationValidator.buildOrgFailureResponse(ApiErrorConstants.SSEAPIEN078, ApiConstants.VALIDATION_ERROR);
		}
		
		}catch(SSEApplicationException sse){
			responseCode = sse.getResponseCode();
			logger.error(statusRequestVO.getApiMsgId(), "ORG Reconciliation API", "getOrgStatus method of ReconciliationServiceImpl", "Exception in ORG reconciliation API", "Exception occured while processing the request",
					AmexLogger.Result.failure, "Exception in ORG Reconciliation service layer ", sse, "error_msg", sse.getMessage());
			logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_APPL_ERR_CD, TivoliMonitoring.SSE_APPL_ERR_MSG, statusRequestVO.getApiMsgId()));
			enrollmentResponseType = ReconciliationValidator.buildOrgFailureResponse(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_ST, ApiConstants.SYSTEM_ERROR);
		}catch(Exception e){
			logger.error(statusRequestVO.getApiMsgId(), "ORG Reconciliation API", "getOrgStatus method of ReconciliationServiceImpl", "Exception in ORG reconciliation API", "Exception occured while processing the request",
					AmexLogger.Result.failure, "Exception in ORG Reconciliation service layer ", e, "error_msg", e.getMessage());
			logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_APPL_ERR_CD, TivoliMonitoring.SSE_APPL_ERR_MSG, statusRequestVO.getApiMsgId()));
			enrollmentResponseType = ReconciliationValidator.buildOrgFailureResponse(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_ST, ApiConstants.SYSTEM_ERROR);
		}
		return enrollmentResponseType;
	}

	@Override
	public AccountStatusResponseType getAccStatus(StatusRequestVO statusRequestVO) {
		AccountStatusResponseType accountStatusResponseType = new AccountStatusResponseType();
		String responseCode = ReconciliationValidator.validateReconAccRequest(statusRequestVO);
		
		if(null != responseCode){
			return ReconciliationValidator.buildAccFailureResponse(responseCode, ApiConstants.VALIDATION_ERROR);
		}
		
		try{
		String partnerName = partnerValidationManager.validatePartnerId(statusRequestVO.getPartnerId(), statusRequestVO.getApiMsgId());
		logger.info("PartnerName:"+partnerName);
		
		if (partnerName != null) {
			accountStatusResponseType = reconciliationAccManager.getAccStatus(statusRequestVO);
		}else{
			accountStatusResponseType = ReconciliationValidator.buildAccFailureResponse(ApiErrorConstants.SSEAPIEN078, ApiConstants.VALIDATION_ERROR);
		}
		
		}catch(SSEApplicationException sse){
			responseCode = sse.getResponseCode();
			logger.error(statusRequestVO.getApiMsgId(), "ORG Reconciliation API", "getOrgStatus method of ReconciliationServiceImpl", "Exception in ORG reconciliation API", "Exception occured while processing the request",
					AmexLogger.Result.failure, "Exception in ORG Reconciliation service layer ", sse, "error_msg", sse.getMessage());
			logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_APPL_ERR_CD, TivoliMonitoring.SSE_APPL_ERR_MSG, statusRequestVO.getApiMsgId()));
			accountStatusResponseType = ReconciliationValidator.buildAccFailureResponse(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_ST, ApiConstants.SYSTEM_ERROR);
		}catch(Exception e){
			logger.error(statusRequestVO.getApiMsgId(), "ORG Reconciliation API", "getOrgStatus method of ReconciliationServiceImpl", "Exception in ORG reconciliation API", "Exception occured while processing the request",
					AmexLogger.Result.failure, "Exception in ORG Reconciliation service layer ", e, "error_msg", e.getMessage());
			logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_APPL_ERR_CD, TivoliMonitoring.SSE_APPL_ERR_MSG, statusRequestVO.getApiMsgId()));
			accountStatusResponseType = ReconciliationValidator.buildAccFailureResponse(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_ST, ApiConstants.SYSTEM_ERROR);
		}
		return accountStatusResponseType;
	}

	@Override
	public PymtStatusResponseType getPymtStatus(StatusRequestVO statusRequestVO) {
		PymtStatusResponseType pymtStatusResponseType = new PymtStatusResponseType();
		
		String responseCode = ReconciliationValidator.validateReconPymtRequest(statusRequestVO);
		
		if(null != responseCode){
			return ReconciliationValidator.buildPymtFailureResponse(responseCode, ApiConstants.VALIDATION_ERROR);
		}

		try{
		String partnerName = partnerValidationManager.validatePartnerId(statusRequestVO.getPartnerId(), statusRequestVO.getApiMsgId());
		logger.info("PartnerName:"+partnerName);
		
		if (partnerName != null) {
			pymtStatusResponseType = reconciliationPymtManager.getPymtStatus(statusRequestVO);
		}else{
			pymtStatusResponseType = ReconciliationValidator.buildPymtFailureResponse(ApiErrorConstants.SSEAPIPY012, ApiConstants.VALIDATION_ERROR);
		}

		//pymtStatusResponseType = reconciliationPymtManager.getPymtStatus(statusRequestVO);
		
		}catch(SSEApplicationException sse){
			responseCode = sse.getResponseCode();
			logger.error(statusRequestVO.getApiMsgId(), "ORG Reconciliation API", "getOrgStatus method of ReconciliationServiceImpl", "Exception in ORG reconciliation API", "Exception occured while processing the request",
					AmexLogger.Result.failure, "Exception in ORG Reconciliation service layer ", sse, "error_msg", sse.getMessage());
			logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_APPL_ERR_CD, TivoliMonitoring.SSE_APPL_ERR_MSG, statusRequestVO.getApiMsgId()));
			pymtStatusResponseType = ReconciliationValidator.buildPymtFailureResponse(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_ST, ApiConstants.SYSTEM_ERROR);
		}catch(Exception e){
			logger.error(statusRequestVO.getApiMsgId(), "ORG Reconciliation API", "getOrgStatus method of ReconciliationServiceImpl", "Exception in ORG reconciliation API", "Exception occured while processing the request",
					AmexLogger.Result.failure, "Exception in ORG Reconciliation service layer ", e, "error_msg", e.getMessage());
			logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_APPL_ERR_CD, TivoliMonitoring.SSE_APPL_ERR_MSG, statusRequestVO.getApiMsgId()));
			pymtStatusResponseType = ReconciliationValidator.buildPymtFailureResponse(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD_ST, ApiConstants.SYSTEM_ERROR);
		}

		return pymtStatusResponseType;
	}

	
	
}
