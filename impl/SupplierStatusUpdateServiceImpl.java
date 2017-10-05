package com.americanexpress.smartserviceengine.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.payve.organizationmanagementservice.v1.addVendorInfoService.SupplierResponseVO;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.SchedulerConstants;
import com.americanexpress.smartserviceengine.common.enums.PayeeStatus;
import com.americanexpress.smartserviceengine.common.exception.SSEApplicationException;
import com.americanexpress.smartserviceengine.common.util.CommonUtils;
import com.americanexpress.smartserviceengine.common.util.DateTimeUtil;
import com.americanexpress.smartserviceengine.common.util.GenericUtilities;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.vo.OrgRequestVO;
import com.americanexpress.smartserviceengine.dao.SupplierStatusDAO;
import com.americanexpress.smartserviceengine.dao.UpdateOrganizationStatusDAO;
import com.americanexpress.smartserviceengine.helper.CheckSupplierRestClientHelper;
import com.americanexpress.smartserviceengine.service.SupplierStatusUpdateService;

public class SupplierStatusUpdateServiceImpl implements SupplierStatusUpdateService {
	
	private static AmexLogger LOGGER = AmexLogger.create(SupplierStatusUpdateServiceImpl.class);

	@Autowired
	private SupplierStatusDAO supplierStatusDAO;
	
	@Autowired
	private UpdateOrganizationStatusDAO updateOrganizationStatusDAO;
	
	@Autowired
	private TivoliMonitoring tivoliMonitoring;
	
	@Autowired
	private CheckSupplierRestClientHelper supplierRestClientHelper;
		
	@Override
	public void getSupplierOrgStatus(String eventId) throws SSEApplicationException {
	
		
		
		String statusTimeStamp=SchedulerConstants.EMPTY_TIMESTAMP;
		String activateTimeStamp=SchedulerConstants.EMPTY_TIMESTAMP;
		
		try {
			 LOGGER.info(eventId, "SmartServiceEngine", "Supplier Status Update Scheduler Service", "SupplierStatusUpdateServiceImpl : getSupplierOrgStatus",
						"Start of  SupplierStatusUpdateService", AmexLogger.Result.success, "");		 
		
			 
			 fetchSupplierRecords(statusTimeStamp, activateTimeStamp, eventId);
			 
		 }catch(Exception e)
		 {
			 LOGGER.error(eventId, "SmartServiceEngine", "Supplier Status Scheduler Service", "SupplierStatusUpdateServiceImpl : getSupplierOrgStatus",
						"Exception while processing Supplier Status Service call", AmexLogger.Result.failure,"Failed to execute getSupplierOrgStatus",e, "ErrorMsg:", e.getMessage());
				
		 }
	}


	private void fetchSupplierRecords(String statusTimeStamp, String activateTimeStamp, String eventId) throws Exception {
		 Map<String,Object> inMap=new HashMap<String, Object>();
		 
		 inMap.put(SchedulerConstants.IN_LST_UPDT_TS, statusTimeStamp);
		 inMap.put(SchedulerConstants.IN_ACT_LST_UPDT_TS, activateTimeStamp);
		List<OrgRequestVO> supplierStatusResultSet;
		List<OrgRequestVO> supplierActivateResultSet;
		LOGGER.info(eventId, "SmartServiceEngine", "Supplier Status Update Scheduler Service", "SupplierStatusUpdateServiceImpl : getSupplierOrgStatus",
					"Start of Get Supplier Details SP E3GMR082", AmexLogger.Result.success, "");
		 
		 Map<String,Object> outMap=supplierStatusDAO.execute(inMap, eventId);
		 if (outMap != null && !outMap.isEmpty() && outMap
		         .get(SchedulerConstants.RESP_CD) != null && SchedulerConstants.RESPONSE_CODE_SUCCESS.equalsIgnoreCase(outMap
		             .get(SchedulerConstants.RESP_CD).toString().trim())) {
			String resultCode = outMap.get(SchedulerConstants.RESP_CD).toString().trim();
			String resultMessage = outMap.get(SchedulerConstants.SP_RESP_MSG).toString().trim();
			String resultParam = outMap.get(SchedulerConstants.SQLCODE_PARM).toString().trim();
			String statusOutCount=outMap.get(SchedulerConstants.OUT_COUNT).toString().trim();
			String activateOutCount = outMap.get(SchedulerConstants.OUT_ACT_COUNT).toString().trim();
			supplierStatusResultSet = (List<OrgRequestVO>) outMap.get(SchedulerConstants.RESULT_SET1);
			supplierActivateResultSet = (List<OrgRequestVO>) outMap.get(SchedulerConstants.RESULT_SET2);

			LOGGER.info(eventId, "SmartServiceEngine", "Supplier Status Scheduler Service", "SupplierStatusUpdateServiceImpl : getSupplierOrgStatus",
					"End of fetch Supplier Details SP E3GMR082", AmexLogger.Result.success, "Success Response from SP",SchedulerConstants.RESP_CD, resultCode,SchedulerConstants.RESP_MSG,
					resultMessage,SchedulerConstants.SQLCODE_PARM,resultParam,SchedulerConstants.OUT_COUNT,statusOutCount,SchedulerConstants.OUT_ACT_COUNT,activateOutCount);
			if(supplierStatusResultSet!=null && !supplierStatusResultSet.isEmpty()){
				invokeBIPSupplierStatusInquiryService(eventId,supplierStatusResultSet);
			}else{
				LOGGER.info(eventId, "SmartServiceEngine", "Supplier Status Scheduler Service", "SupplierStatusUpdateServiceImpl : getSupplierOrgStatus",
						"End of  fetch Supplier Details SP E3GMR082", AmexLogger.Result.success, "Result set for supplier status update is null or empty");
			}
			
			if(supplierActivateResultSet!=null && !supplierActivateResultSet.isEmpty()){
				invokeBIPSupplierActivateService(eventId,supplierActivateResultSet);
			}else{
				LOGGER.info(eventId, "SmartServiceEngine", "Supplier Status Scheduler Service", "SupplierStatusUpdateServiceImpl : getSupplierOrgStatus",
						"End of  fetch Supplier Details SP E3GMR082", AmexLogger.Result.success, "Result set for supplier activate is null or empty");
			}
			
			OrgRequestVO orgRequestVO=null;
			if(Integer.valueOf(statusOutCount)>20){
				orgRequestVO=supplierStatusResultSet.get(supplierStatusResultSet.size()-1);
				statusTimeStamp=orgRequestVO.getLastUpdTimeStamp();
			}else{
		        statusTimeStamp = SchedulerConstants.HIGH_TIMESTAMP;

			}
			orgRequestVO=null;
			if(Integer.valueOf(activateOutCount)>20){
				orgRequestVO=supplierActivateResultSet.get(supplierActivateResultSet.size()-1);
				activateTimeStamp=orgRequestVO.getLastUpdTimeStamp();
			}else{
		        activateTimeStamp = SchedulerConstants.HIGH_TIMESTAMP;

			}
			
			 if (!(SchedulerConstants.HIGH_TIMESTAMP.equalsIgnoreCase(statusTimeStamp) && SchedulerConstants.HIGH_TIMESTAMP.equalsIgnoreCase(activateTimeStamp))) {
				 fetchSupplierRecords(statusTimeStamp, activateTimeStamp, eventId);
		        }
		} else {
				LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_082_ERR_CD, TivoliMonitoring.SSE_SP_082_ERR_MSG, eventId));
				LOGGER.error(eventId, "SmartServiceEngine", "Supplier Status Scheduler Service", "SupplierStatusUpdateServiceImpl : getSupplierOrgStatus",
						"End of fetch Supplier Details SP E3GMR082", AmexLogger.Result.failure, "outMap is null or error resp_code from SP");
			}
	}


	private void invokeBIPSupplierStatusInquiryService(String eventId,
			List<OrgRequestVO> supplierDataResultSet)
			 {
			for(OrgRequestVO orgRequestVO: supplierDataResultSet){
				try{
				if(StringUtils.isNotBlank(orgRequestVO.getPaymentEntityId())){
					ResponseEntity<String> responseEntityVO=supplierRestClientHelper.getSupplierStatus(orgRequestVO.getPaymentEntityId());
					if(responseEntityVO!=null && responseEntityVO.getStatusCode()!=null){
						int statusCode=Integer.parseInt(responseEntityVO.getStatusCode().toString());
					
					if(statusCode != HttpStatus.SC_INTERNAL_SERVER_ERROR && statusCode != HttpStatus.SC_BAD_REQUEST 
							&& statusCode != HttpStatus.SC_GATEWAY_TIMEOUT){
						if(responseEntityVO!=null && responseEntityVO.getBody()!=null){
						String responseEntity=responseEntityVO.getBody();
		    			SupplierResponseVO supplierResponseVO = GenericUtilities.jsonToJava(responseEntity, SupplierResponseVO.class);
		    			if(supplierResponseVO!=null && StringUtils.isNotBlank(supplierResponseVO.getStatusCd())){
		        			if(!ApiConstants.CHAR_ZERO.equals(supplierResponseVO.getStatusCd())){
		        				Map<String,Object> inUpdMap=createInmap(eventId,orgRequestVO,supplierResponseVO);		       
							Map<String,Object> outUpdMap=updateOrganizationStatusDAO.execute(inUpdMap, eventId);
							 if (outUpdMap != null && !outUpdMap.isEmpty() && outUpdMap
					                 .get(SchedulerConstants.RESP_CD) != null && SchedulerConstants.RESPONSE_CODE_SUCCESS.equalsIgnoreCase(outUpdMap
					                     .get(SchedulerConstants.RESP_CD).toString().trim())){
								 String resultUpdCode = outUpdMap.get(SchedulerConstants.RESP_CD).toString().trim();
									String resultUpdMessage = outUpdMap.get(SchedulerConstants.SP_RESP_MSG).toString().trim();
									String resultUpdParam = outUpdMap.get(SchedulerConstants.SQLCODE_PARM).toString().trim();
								 LOGGER.info(eventId, "SmartServiceEngine", "Supplier Status Scheduler Service", "SupplierStatusUpdateServiceImpl : invokeBIPSupplierStatusInquiryService",
											"update Supplier status- SP E3GMR023", AmexLogger.Result.success, "Success Response from SP",SchedulerConstants.RESP_CD, resultUpdCode,SchedulerConstants.RESP_MSG,
											resultUpdMessage,SchedulerConstants.SQLCODE_PARM,resultUpdParam);
								
							 }else{
								 LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_023_ERR_CD, TivoliMonitoring.SSE_SP_023_ERR_MSG, eventId));
									LOGGER.error(eventId, "SmartServiceEngine", "Supplier Status Scheduler Service", "SupplierStatusUpdateServiceImpl : invokeBIPSupplierStatusInquiryService",
											"Update Supplier status- SP E3GMR023", AmexLogger.Result.failure, "outMap is null or error resp_code from SP");							
								 
							 }
		    			}else{
		    				LOGGER.info(eventId, "SmartServiceEngine", "Supplier Status Scheduler Service", "SupplierStatusUpdateServiceImpl : invokeBIPSupplierStatusInquiryService",
		    						"End of invoke BIP supplier status inquiry service", AmexLogger.Result.success, "Supplier status is Inprogress at BIP end");
		    			}

		        		}else{
		        			LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.BIP_REST_PAYEE_STATUS_INQUIRY_CD, TivoliMonitoring.BIP_REST_PAYEE_STATUS_INQUIRY_MSG, eventId));								
							LOGGER.error(eventId,"SmartServiceEngine","Supplier Status Scheduler Service","SupplierStatusUpdateServiceImpl - invokeBIPSupplierStatusInquiryService","Failure from BIP for supplier status inquiry service",AmexLogger.Result.failure, "");				
					
		        		}
					}else{
	        			LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.BIP_REST_PAYEE_STATUS_INQUIRY_CD, TivoliMonitoring.BIP_REST_PAYEE_STATUS_INQUIRY_MSG, eventId));								
						LOGGER.error(eventId,"SmartServiceEngine","Supplier Status Scheduler Service","SupplierStatusUpdateServiceImpl - invokeBIPSupplierStatusInquiryService","Failure from BIP for supplier status inquiry service",AmexLogger.Result.failure, "");				
				
	        		}
	        		
						}else{								
						LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.BIP_REST_PAYEE_STATUS_INQUIRY_CD, TivoliMonitoring.BIP_REST_PAYEE_STATUS_INQUIRY_MSG, eventId));								
						LOGGER.error(eventId,"SmartServiceEngine","Supplier Status Scheduler Service","SupplierStatusUpdateServiceImpl - invokeBIPSupplierStatusInquiryService","Failure from BIP for supplier status inquiry service",AmexLogger.Result.failure, "");				
				
						
					}}else{						
				LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.BIP_REST_PAYEE_STATUS_INQUIRY_CD, TivoliMonitoring.BIP_REST_PAYEE_STATUS_INQUIRY_MSG, eventId));								
				LOGGER.error(eventId,"SmartServiceEngine","Supplier Status Scheduler Service","SupplierStatusUpdateServiceImpl - invokeBIPSupplierStatusInquiryService","Failure from BIP for supplier status inquiry service",AmexLogger.Result.failure, "");		
		}
			}
			}catch(Exception e){
		    	if(e.getMessage().contains("java.net.SocketTimeoutException")){
		    		LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.BIP_TIMEOUT_ERR_CD, TivoliMonitoring.BIP_TIMEOUT_ERR_MSG, eventId));                
		    		LOGGER.error(eventId,"SmartServiceEngine","Supplier Status Scheduler Service","SupplierStatusUpdateServiceImpl - invokeBIPSupplierStatusInquiryService","Time out exception occured in Bip service",
							AmexLogger.Result.failure,"Exception",e);
		  
		    	}else{
					LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.BIP_REST_PAYEE_STATUS_INQUIRY_CD, TivoliMonitoring.BIP_REST_PAYEE_STATUS_INQUIRY_MSG, eventId));								
				LOGGER.error(eventId,"SmartServiceEngine","Supplier Status Scheduler Service","SupplierStatusUpdateServiceImpl - invokeBIPSupplierStatusInquiryService","Failure from BIP for supplier status inquiry service",AmexLogger.Result.failure, "");
		    	}
    		}
			}
		
	}
	
	private void invokeBIPSupplierActivateService(String eventId, List<OrgRequestVO> supplierActivateResultSet) throws Exception {
		for(OrgRequestVO orgRequestVO: supplierActivateResultSet){
			try{
			if(StringUtils.isNotBlank(orgRequestVO.getPaymentEntityId())){
				if(StringUtils.isNotBlank(orgRequestVO.getPartnerId())){
					String payvePartnerId = CommonUtils.getPayvePartnerEntityId(orgRequestVO.getPartnerId());
					if(StringUtils.isNotBlank(payvePartnerId)){
						ResponseEntity<String> responseEntityVO=supplierRestClientHelper.activateSupplier(payvePartnerId, orgRequestVO.getPaymentEntityId());
						if(responseEntityVO!=null && responseEntityVO.getStatusCode()!=null){
							int statusCode=Integer.parseInt(responseEntityVO.getStatusCode().toString());
							if(statusCode != HttpStatus.SC_INTERNAL_SERVER_ERROR && statusCode != HttpStatus.SC_BAD_REQUEST && statusCode != HttpStatus.SC_GATEWAY_TIMEOUT){
								if(responseEntityVO!=null && responseEntityVO.getBody()!=null){
									String responseEntity=responseEntityVO.getBody();
						    		SupplierResponseVO supplierResponseVO = GenericUtilities.jsonToJava(responseEntity, SupplierResponseVO.class);
						    		if(supplierResponseVO!=null && StringUtils.isNotBlank(supplierResponseVO.getStatusCd())){
						    			Map<String,Object> inUpdMap=createInmap(eventId,orgRequestVO,supplierResponseVO);
										Map<String,Object> outUpdMap=updateOrganizationStatusDAO.execute(inUpdMap, eventId);
										if (outUpdMap != null && !outUpdMap.isEmpty() && outUpdMap.get(SchedulerConstants.RESP_CD) != null && 
												SchedulerConstants.RESPONSE_CODE_SUCCESS.equalsIgnoreCase(outUpdMap.get(SchedulerConstants.RESP_CD).toString().trim())){
											String resultUpdCode = outUpdMap.get(SchedulerConstants.RESP_CD).toString().trim();
											String resultUpdMessage = outUpdMap.get(SchedulerConstants.SP_RESP_MSG).toString().trim();
											String resultUpdParam = outUpdMap.get(SchedulerConstants.SQLCODE_PARM).toString().trim();
											LOGGER.info(eventId, "SmartServiceEngine", "Supplier Status Scheduler Service", "SupplierStatusUpdateServiceImpl : invokeBIPSupplierActivateService",
													"update Supplier status- SP E3GMR023", AmexLogger.Result.success, "Success Response from SP",SchedulerConstants.RESP_CD, resultUpdCode,SchedulerConstants.RESP_MSG,
													resultUpdMessage,SchedulerConstants.SQLCODE_PARM,resultUpdParam);
										}else{
											LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_023_ERR_CD, TivoliMonitoring.SSE_SP_023_ERR_MSG, eventId));
											LOGGER.error(eventId, "SmartServiceEngine", "Supplier Status Scheduler Service", "SupplierStatusUpdateServiceImpl : invokeBIPSupplierActivateService",
													"Update Supplier status- SP E3GMR023", AmexLogger.Result.failure, "outMap is null or error resp_code from SP");							
										}
			
						        	}else{
						        		LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.BIP_REST_PAYEE_ACTIVATE_CD, TivoliMonitoring.BIP_REST_PAYEE_ACTIVATE_MSG, eventId));								
										LOGGER.error(eventId,"SmartServiceEngine","Supplier Status Scheduler Service","SupplierStatusUpdateServiceImpl - invokeBIPSupplierActivateService","Failure from BIP for supplier status inquiry service",AmexLogger.Result.failure, "");				
					        		}
								}else{								
									LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.BIP_REST_PAYEE_ACTIVATE_CD, TivoliMonitoring.BIP_REST_PAYEE_ACTIVATE_MSG, eventId));								
									LOGGER.error(eventId,"SmartServiceEngine","Supplier Status Scheduler Service","SupplierStatusUpdateServiceImpl - invokeBIPSupplierActivateService","Failure from BIP for supplier status inquiry service",AmexLogger.Result.failure, "");				
								}
							}else{						
								LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.BIP_REST_PAYEE_ACTIVATE_CD, TivoliMonitoring.BIP_REST_PAYEE_ACTIVATE_MSG, eventId));								
								LOGGER.error(eventId,"SmartServiceEngine","Supplier Status Scheduler Service","SupplierStatusUpdateServiceImpl - invokeBIPSupplierActivateService","Failure from BIP for supplier status inquiry service",AmexLogger.Result.failure, "");		
							}
						} else {	
							LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.BIP_REST_PAYEE_ACTIVATE_CD, TivoliMonitoring.BIP_REST_PAYEE_ACTIVATE_MSG, eventId));							

							LOGGER.error(eventId,"SmartServiceEngine","Supplier Status Scheduler Service","SupplierStatusUpdateServiceImpl - invokeBIPSupplierActivateService","Failure from BIP for supplier status inquiry service",AmexLogger.Result.failure, "");		
						}
					} else {
						LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.BIP_REST_PAYEE_ACTIVATE_CD, TivoliMonitoring.BIP_REST_PAYEE_ACTIVATE_MSG, eventId));								
						LOGGER.error(eventId,"SmartServiceEngine","Supplier Status Scheduler Service","SupplierStatusUpdateServiceImpl - invokeBIPSupplierActivateService","Invalid Partner id",AmexLogger.Result.failure, "");
					}
				} else {
					LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.BIP_REST_PAYEE_ACTIVATE_CD, TivoliMonitoring.BIP_REST_PAYEE_ACTIVATE_MSG, eventId));								
					LOGGER.error(eventId,"SmartServiceEngine","Supplier Status Scheduler Service","SupplierStatusUpdateServiceImpl - invokeBIPSupplierActivateService","Received empty Partner id from DB",AmexLogger.Result.failure, "");
				}
			} else {
				LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.BIP_REST_PAYEE_ACTIVATE_CD, TivoliMonitoring.BIP_REST_PAYEE_ACTIVATE_MSG, eventId));								
				LOGGER.error(eventId,"SmartServiceEngine","Supplier Status Scheduler Service","SupplierStatusUpdateServiceImpl - invokeBIPSupplierActivateService","Received empty Vendor System id from DB",AmexLogger.Result.failure, "");
			}
		
	}catch(Exception e){

    	if(e.getMessage().contains("java.net.SocketTimeoutException")){
    		LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.BIP_TIMEOUT_ERR_CD, TivoliMonitoring.BIP_TIMEOUT_ERR_MSG, eventId));                
    		LOGGER.error(eventId,"SmartServiceEngine","Supplier Status Scheduler Service","SupplierStatusUpdateServiceImpl - invokeBIPSupplierActivateService","Time out exception occured in Bip service",
					AmexLogger.Result.failure,"Exception",e);
  
    	}else{
		LOGGER.error(tivoliMonitoring.logStatement(TivoliMonitoring.BIP_REST_PAYEE_ACTIVATE_CD, TivoliMonitoring.BIP_REST_PAYEE_ACTIVATE_MSG, eventId));								
		LOGGER.error(eventId,"SmartServiceEngine","Supplier Status Scheduler Service","SupplierStatusUpdateServiceImpl - invokeBIPSupplierActivateService","Failure from BIP for supplier status inquiry service",AmexLogger.Result.failure, "");
    	}
	
	}
	}
		
	}


	private Map<String, Object> createInmap(String eventId,OrgRequestVO orgRequestVO, SupplierResponseVO supplierResponseVO) {
		
		Map<String, Object> inMap=new HashMap<String,Object>();
		
			String statusCd=ApiConstants.status.get(supplierResponseVO.getStatusCd());	

		inMap.put(ApiConstants.PYMT_SYS_ORG_ID,orgRequestVO.getPaymentEntityId());
		inMap.put(ApiConstants.PYMT_SYS_STA_CD, statusCd);
		inMap.put(ApiConstants.PYMT_SYS_STA_DS,PayeeStatus.getStatusCodeDBMapping(statusCd));
		inMap.put(ApiConstants.PYMT_SYS_STA_DT, DateTimeUtil.getCurDateyyyyMMdd());
		inMap.put(ApiConstants.PYMT_SYS_RESP_CD, supplierResponseVO.getStatusCd());
		inMap.put(ApiConstants.PYMT_SYS_RESP_DS, SchedulerConstants.SUCCESS);
		inMap.put(ApiConstants.PRTR_MAC_ID, orgRequestVO.getPartnerId());
		inMap.put(ApiConstants.PRTR_ORG_ID, orgRequestVO.getOrganizationId());
		
	
		return inMap;
	}
	
	

		
	
}
