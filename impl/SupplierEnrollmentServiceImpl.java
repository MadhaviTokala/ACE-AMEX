package com.americanexpress.smartserviceengine.service.impl;

import static com.americanexpress.smartserviceengine.common.constants.ServiceConstants.APPLICATION_NAME;
import static com.americanexpress.smartserviceengine.common.constants.ServiceConstants.DEFAULT_PAGINATION_COUNT;
import static com.americanexpress.smartserviceengine.common.constants.ServiceConstants.MIN_DEFAULT_TIMESTAMP;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.ServiceConstants;
import com.americanexpress.smartserviceengine.common.exception.AceException;
import com.americanexpress.smartserviceengine.common.util.EnvironmentPropertiesUtil;
import com.americanexpress.smartserviceengine.common.util.GenericUtilities;
import com.americanexpress.smartserviceengine.common.util.ThreadLocalManager;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.vo.PaginationDataVO;
import com.americanexpress.smartserviceengine.common.vo.SupplierEnrollmentDetailsVO;
import com.americanexpress.smartserviceengine.dao.InsertExceptionDataDAO;
import com.americanexpress.smartserviceengine.dao.SupplierOrgDetailsDAO;
import com.americanexpress.smartserviceengine.service.SupplierEnrollmentService;
import com.americanexpress.soaui.client.data.FraudVerificationRequestVO;
import com.americanexpress.soaui.client.service.FraudVerificationService;

public class SupplierEnrollmentServiceImpl implements SupplierEnrollmentService {

	private static AmexLogger LOGGER = AmexLogger.create(SupplierEnrollmentService.class);
	
	private static final String EVENT_NAME = SupplierEnrollmentService.class.getSimpleName();
	
	@Resource
	private FraudVerificationService fraudVerificationService;
	
	@Resource
	private SupplierOrgDetailsDAO supplierOrgDetailsDAO;
	
	@Resource
	private InsertExceptionDataDAO insertExceptionDataDAO;
	
	@Autowired
	protected TivoliMonitoring tivoliMonitoring;
	
	public void validateSupplierEmailAge(){
		String apiMsgId = ThreadLocalManager.getApiMsgId();
		PaginationDataVO<SupplierEnrollmentDetailsVO> paginationDataVO = new PaginationDataVO<SupplierEnrollmentDetailsVO>();
		paginationDataVO.setLastUpdateTime(MIN_DEFAULT_TIMESTAMP);		
		boolean operationSucess = false; 
		try{
			LOGGER.info(apiMsgId, APPLICATION_NAME, EVENT_NAME, "UpdateSupplierEnrollmentStatus", "Initiating UpdateSupplierEnrollmentStatus Job.", AmexLogger.Result.success, "START");			
			boolean isContinueLoop = true;
			boolean isFirstAttempt = true;
			while(isContinueLoop){
				isContinueLoop = false; 
				LOGGER.info(apiMsgId, APPLICATION_NAME, EVENT_NAME, "prepareSupplierEnrollmentDetails", "get supplier information ", 
						AmexLogger.Result.success, "START");
				SupplierEnrollmentDetailsVO supplierEnrollmentDetailsVO = new SupplierEnrollmentDetailsVO();
				supplierOrgDetailsDAO.prepareSupplierEnrollmentDetails(supplierEnrollmentDetailsVO, paginationDataVO);
				if(isFirstAttempt){
					isFirstAttempt = false;
					LOGGER.info(apiMsgId, APPLICATION_NAME, EVENT_NAME, "cancelTokens", "PERIODIC SUPPLIER EMAIL VALIDATION", AmexLogger.Result.success, 
						 "START", "TOTAL NUMBER OF SUPPLIER ORG TO BE VALIDATED ON" + GenericUtilities.getTodaysDateMMDDYYYY(),
						 String.valueOf(paginationDataVO.getTotalRecordCount()));
				}
				List<SupplierEnrollmentDetailsVO> results = paginationDataVO.getResults();
				if(!GenericUtilities.isNullOrEmpty(results)){
					String lastUpdateTime = null;
					for(SupplierEnrollmentDetailsVO supplierDetails : results){
						LOGGER.info(apiMsgId, APPLICATION_NAME, EVENT_NAME, "SupplierEnrollmentServiceImpl", "validateSupplierEmailAge", AmexLogger.Result.success, 
								 "", "Input record for service",supplierDetails.toString());
						if(supplierDetails.getOrgEmailId() != null && !StringUtils.stripToEmpty(supplierDetails.getOrgEmailId()).isEmpty()){
							Map<String, Object> serviceResp = fraudVerificationService.validateEmail(setEmailAgeVerificationDetails(supplierDetails));
							operationSucess = (Boolean) serviceResp.get(ServiceConstants.OPERATION_SUCCESSFUL);
							supplierDetails.setPaymentResponseCd((String) serviceResp.get(ServiceConstants.RESPONSE_CODE));
							supplierDetails.setPaymentResponseDesc((String) serviceResp.get(ServiceConstants.RESPONSE_MSG));
							boolean isValidEmail = (Boolean) serviceResp.get(ServiceConstants.IS_VALID_EMAIL);						
							if(!isValidEmail &&  operationSucess){
								LOGGER.info(apiMsgId, APPLICATION_NAME, EVENT_NAME, "updateSupplierEnrollmentDetails", "Number of Supplier  to be vaidated: " + results.size(), 
										AmexLogger.Result.success, "START");						
								supplierOrgDetailsDAO.updateSupplierEnrollmentDetails(supplierDetails);
								//if(supplierDetails.getPartnerMacId().equals(EnvironmentPropertiesUtil.getProperty(ApiConstants.INTACCT_PARTNER_ID))){
								if((EnvironmentPropertiesUtil.getProperty(ApiConstants.EXCEPTION_LOGGING_PARTNER_IDS)).contains(supplierDetails.getPartnerMacId())){
									LOGGER.info(apiMsgId, APPLICATION_NAME, EVENT_NAME, "prepareSupplierEnrollmentDetails", "insert exception data for Intacct partner", 
											AmexLogger.Result.success, "");
									insertExceptionDataDAO.execute(setExceptionDataInMap(supplierDetails), apiMsgId);
								}
								
							}
						}
						LOGGER.info(apiMsgId, APPLICATION_NAME, EVENT_NAME, "updateSupplierEnrollmentDetails", "Number of Supplier  to be vaidated: " + results.size(), 
								AmexLogger.Result.success, "END");		
						lastUpdateTime = supplierDetails.getLstUpdtTs();
					}					
					isContinueLoop = hasMoreRecords(apiMsgId, lastUpdateTime, paginationDataVO);					
					if(isContinueLoop){
						paginationDataVO = new PaginationDataVO<SupplierEnrollmentDetailsVO>();
						paginationDataVO.setLastUpdateTime(lastUpdateTime);
					}
				}
		  }		
		}catch(AceException ex){
			LOGGER.error(apiMsgId, APPLICATION_NAME, EVENT_NAME, "UpdateSupplierEnrollmentStatus", "Update Supplier Enrollment Request Failed with Exception", AmexLogger.Result.failure, 
					"Unexpected Exception while updating the supplier details.", ex);
			LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_P711_ERR_CD, TivoliMonitoring.SSE_SP_P711_ERR_MSG, apiMsgId));
		}catch(Exception ex){
			LOGGER.error(apiMsgId, APPLICATION_NAME, EVENT_NAME, "UpdateSupplierEnrollmentStatus", "Update Supplier Enrollment Request Failed with Exception", AmexLogger.Result.failure, 
					"Unexpected Exception while updating the supplier details.", ex);
			LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_P711_ERR_CD, TivoliMonitoring.SSE_SP_P711_ERR_MSG, apiMsgId));
		}
	}
	
	private FraudVerificationRequestVO setEmailAgeVerificationDetails(SupplierEnrollmentDetailsVO supplierDetails){
		FraudVerificationRequestVO tmpfraudVerificationRequestVO = new FraudVerificationRequestVO();
		tmpfraudVerificationRequestVO.setEmail(supplierDetails.getOrgEmailId());
		return tmpfraudVerificationRequestVO;
	}

	
	private boolean hasMoreRecords(String apiMsgId, String lastUpdateTime, PaginationDataVO<SupplierEnrollmentDetailsVO> paginationDataVO){
		boolean isContinueLoop = false;
		if(paginationDataVO.getTotalRecordCount() > DEFAULT_PAGINATION_COUNT && !GenericUtilities.isNullOrEmpty(lastUpdateTime)){
			isContinueLoop = true;
		}
		LOGGER.info(apiMsgId, APPLICATION_NAME, EVENT_NAME, "SupplierEnrollmet.hasMoreRecords", "Has more payments to process?", 
				AmexLogger.Result.success, isContinueLoop? "CONTINUE": "FINISH", "Total Record Count", String.valueOf(paginationDataVO.getTotalRecordCount()));
		return isContinueLoop;
	}
	
	private Map<String,Object> setExceptionDataInMap(SupplierEnrollmentDetailsVO supplierDetails){
		Map<String,Object> inMap=new HashMap<String,Object>();
		inMap.put(ApiConstants.IN_PRTR_MAC_ID,supplierDetails.getPartnerMacId());
		inMap.put(ApiConstants.IN_EXCPT_CD , StringUtils.stripToEmpty(EnvironmentPropertiesUtil.getProperty(ApiConstants.EMAILAGE_EXCP_CD)));
		inMap.put(ApiConstants.IN_PRTR_CUST_ORG_ID,supplierDetails.getAssocPartnerOrgId());
		inMap.put(ApiConstants.IN_PRTR_SPLY_ORG_ID , supplierDetails.getPartnerOrgId());
		inMap.put(ApiConstants.IN_EXCPT_DS , StringUtils.stripToEmpty(EnvironmentPropertiesUtil.getProperty(ApiConstants.EMAILAGE_EXCP_DS)));
		inMap.put(ApiConstants.IN_EXCPT_TYPE_TX, StringUtils.stripToEmpty(EnvironmentPropertiesUtil.getProperty(ApiConstants.EMAILAGE_EXCP_TYPE)));
		inMap.put(ApiConstants.IN_PRTR_ACCT_ID, ApiConstants.CHAR_SPACE);  
		inMap.put(ApiConstants.	IN_BUY_PYMT_ID, ApiConstants.CHAR_SPACE);      
		inMap.put(ApiConstants.IN_SPLY_ACCT_ID, ApiConstants.CHAR_SPACE);     
		return inMap;
	}
	
}