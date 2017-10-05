package com.americanexpress.smartserviceengine.service.impl;

import java.sql.Blob;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.sql.rowset.serial.SerialBlob;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.americanexpress.ace.security.helper.SecurityHelper;
import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.payve.organizationmanagementservice.v1.addVendorInfoService.SupplierRequestVO;
import com.americanexpress.payve.organizationmanagementservice.v1.addVendorInfoService.SupplierResponseVO;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.SchedulerConstants;
import com.americanexpress.smartserviceengine.common.exception.SSEApplicationException;
import com.americanexpress.smartserviceengine.common.util.CommonUtils;
import com.americanexpress.smartserviceengine.common.util.DateTimeUtil;
import com.americanexpress.smartserviceengine.common.util.EnvironmentPropertiesUtil;
import com.americanexpress.smartserviceengine.common.util.GenericUtilities;
import com.americanexpress.smartserviceengine.common.vo.ValidateEnrollmentRespVO;
import com.americanexpress.smartserviceengine.common.vo.ValidateEnrollmentVO;
import com.americanexpress.smartserviceengine.dao.RollbackUpdOrgDetailsDAO;
import com.americanexpress.smartserviceengine.dao.UpdateOrganizationDAO;
import com.americanexpress.smartserviceengine.dao.UpdateOrganizationStatusDAO;
import com.americanexpress.smartserviceengine.service.FetchBIPSupplierOrgIdService;
import com.americanexpress.smartserviceengine.util.EnrollmentUtils;
//import org.springframework.http.MediaType;

@Service
public class FetchBIPSupplierOrgIdServiceImpl implements FetchBIPSupplierOrgIdService
{
	private static AmexLogger logger = AmexLogger.create(FetchBIPSupplierOrgIdServiceImpl.class);
	
	@Resource
	private EnrollmentUtils enrollmentUtils;
	
	@Resource
	@Qualifier("updateOrganizationDAO")
	private UpdateOrganizationDAO updateOrganizationDAO;
	
	@Resource
	@Qualifier("rollbackUpdOrgDetailsDAO")
	private RollbackUpdOrgDetailsDAO rollbackUpdOrgDetailsDAO;
	
	 @Override
	 public String fetchBIPSupplierOrgId(String supOrgId, String buyOrgId, String partnerId, String apiMsgId, boolean deActivation) throws SSEApplicationException 
	 {
		 
		 logger.info(apiMsgId, "SmartServiceEngine", "fetchBIPSupplierOrgId", "fetchBIPSupplierOrgId", " to get buyer organizaiton id ", 
				 AmexLogger.Result.success, "", " buyer org id :",buyOrgId," supplier org id is :",supOrgId);
		 
		 ValidateEnrollmentVO validateEnrollmentVO = new ValidateEnrollmentVO();		 
		 validateEnrollmentVO.setPartnerOrgId(supOrgId);
		 validateEnrollmentVO.setAssociatedOrgId(buyOrgId);
		 validateEnrollmentVO.setSubscribeServiceIds(ApiConstants.CHAR_ZERO);
		 validateEnrollmentVO.setPartnerId(partnerId);
		 
		 if(deActivation)			 
		 {
 			 // de-activation    IN_SRVC_ID     0			   IN_USRVC_ID   1
			 validateEnrollmentVO.setSubscribeServiceIds(ApiConstants.CHAR_ZERO);
			 validateEnrollmentVO.setUnsubscribeServiceIds(ApiConstants.CATEGORY_CUST_ENROLL);
		 }else{
			 // activation    IN_SRVC_ID     1			   IN_USRVC_ID    0
			 validateEnrollmentVO.setSubscribeServiceIds(ApiConstants.CATEGORY_CUST_ENROLL);
			 validateEnrollmentVO.setUnsubscribeServiceIds(ApiConstants.CHAR_ZERO);
		 }
		 
		 ValidateEnrollmentRespVO validateEnrollmentRespVO = enrollmentUtils.validateUpdateOrganization(validateEnrollmentVO, apiMsgId);
		 
		 String bipSupplierOrdId=null;
		 
		 if(null != validateEnrollmentRespVO && StringUtils.isNotBlank(validateEnrollmentRespVO.getSpRespCode())				  
				 && (ApiConstants.SP_SUCCESS_SSEINB00).equalsIgnoreCase(validateEnrollmentRespVO.getSpRespCode()))
		 {
				 bipSupplierOrdId = validateEnrollmentRespVO.getPayveOrgId();	 
		 }
		 logger.info(apiMsgId, "SmartServiceEngine", "fetchBIPSupplierOrgId", "fetchBIPSupplierOrgId", " to get buyer organizaiton id", 
				 AmexLogger.Result.success, "", " buyer org id :",buyOrgId," supplier org id is :",supOrgId,
				 " OUT_SRVC_FLAG is :",validateEnrollmentRespVO.getSrvcFlag(),
				 " BIP supplier organization id is :",bipSupplierOrdId);
		 return bipSupplierOrdId;
	 }
	 
	 public Map<String, Object>  updateOrgDetails(String orgId, String srvId,String apiMsgId) throws ParseException,SQLException
	 {
		 logger.info(apiMsgId, "SmartServiceEngine", "updateOrgDetails", "updateOrgDetails", " to invoke E3GMR002 procedure ", 
				 AmexLogger.Result.success, "", " buyer org id :",orgId," service id is :",srvId);
		 
		 Map<String, Object> outMap = new HashMap<String, Object>();
		 Map<String, Object> inMap = new HashMap<String, Object>();
		 inMap.put(ApiConstants.IN_ORG_ID, orgId);
		 inMap.put(ApiConstants.IN_USRVC_ID, srvId);
		 inMap.put(ApiConstants.IN_PYMT_SYS_STAT_DT,DateTimeUtil.getDB2SqlDate(DateTimeUtil.getCurDateyyyyMMdd()));
		 inMap.put(ApiConstants.IN_ENROLL_REQ_ID,ApiConstants.CHAR_BLANKSPACE);		 
		 inMap.put(ApiConstants.IN_ENROLL_TS, DateTimeUtil.getStringToDB2SqlTimeStamp(ApiConstants.SP_DEF_TIME_STAMP_DATE));
		 inMap.put(ApiConstants.IN_ORG_GRP_ID,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_PRTR_MAC_ID,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_PRTR_NM,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_ENROLL_ACT_TYPE_CD,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_ENROLL_CTGY_TYPE_CD,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_ORG_TYPE_TX,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_PRTR_ORG_ID,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_ASSO_PRTR_ORG_ID,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_PARNT_PRTR_ORG_ID,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_ORG_NM,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_ORG_SHRT_NM,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_ORG_STA_CD,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_TAX_ID,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_AD_LINE_1_TX,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_AD_LINE_2_TX,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_AD_LINE_3_TX,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_AD_LINE_4_TX,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_AD_LINE_5_TX,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_AD_POST_TOWN_NM,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_AD_RGN_AREA_CD,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_AD_RGN_AREA_NM,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_AD_CTRY_CD,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_AD_CTRY_NM,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_AD_PSTL_CD_TX,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_SRVC_ID,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_CUST_FEE_BILL_IN,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_MAIL_VEND_MTHD_CD,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_MAIL_DLVR_ID,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_MAIL_VEND_ACCT_NO,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_MAIL_VEND_NM,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_CHK_NO_IN,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_CHK_PRNT_SRVC_IN,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_USE_FAST_FWD_SRVC_CD,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_SPLY_NTFY_IN,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_HOLD_IN,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_PAYER_ATTACH_1_ID,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_PAYER_ATTACH_2_ID,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_PAYER_ATTACH_3_ID,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_PAYER_ATTACH_4_ID,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_PAYER_ATTACH_5_ID,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_PAYER_ATTACH_1_FG,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_PAYER_ATTACH_2_FG,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_PAYER_ATTACH_3_FG,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_PAYER_ATTACH_4_FG,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_PAYER_ATTACH_5_FG,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_ORG_CTC_DET,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_PYMT_SYS_ORG_ID,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_PYMT_SYS_STAT_CD,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_PYMT_SYS_STAT_DES,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_PYMT_SYS_RESP_CD,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_PYMT_SYS_RESP_DES,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_ORG_ENROLL_STA_CD,ApiConstants.CHAR_BLANKSPACE);
		 inMap.put(ApiConstants.IN_ADDRESS_ID,ApiConstants.CHAR_BLANKSPACE);
		 
		 String space = ApiConstants.CHAR_SPACE;
		 Blob emptyBlob = new SerialBlob(space.getBytes());
			 
		 inMap.put(ApiConstants.IN_PAYER_ATTACH_1_FL, new SerialBlob(emptyBlob));
		 inMap.put(ApiConstants.IN_PAYER_ATTACH_2_FL,new SerialBlob(emptyBlob));
		 inMap.put(ApiConstants.IN_PAYER_ATTACH_3_FL,new SerialBlob(emptyBlob));
		 inMap.put(ApiConstants.IN_PAYER_ATTACH_4_FL,new SerialBlob(emptyBlob));
		 inMap.put(ApiConstants.IN_PAYER_ATTACH_5_FL,new SerialBlob(emptyBlob));
			
		 outMap = updateOrganizationDAO.execute(inMap, apiMsgId);
		
		 logger.info(apiMsgId, "SmartServiceEngine", "updateOrgDetails", "updateOrgDetails", " after invoking E3GMR002 procedure ", 
				 AmexLogger.Result.success, "", " buyer org id :",orgId," service id is :",srvId, "outmap is :"+outMap.toString());
		 return outMap;
	 }
	 
	 public  void updateOrgDetailsRollback(String orgId, String srvId, String reqFlag, String prevFlag, String apiMsgId)
	 {
			logger.info(apiMsgId, "SmartServiceEngine", "updateOrgDetailsRollback", "updateOrgDetailsRollback", " roll back the data through E3GMRO95 proc start ", 
					 AmexLogger.Result.success, "", " for org id :",orgId, " srvId :", srvId, " reqFlag : ", reqFlag, " prevFlag :", prevFlag);
			 Map<String, Object> outMap = new HashMap<String, Object>();
			 Map<String, Object> inMap = new HashMap<String, Object>();
			 
			 inMap.put(ApiConstants.IN_ORG_ID, orgId);
			 inMap.put(ApiConstants.IN_UNSUB_SRVC_CODE , Integer.parseInt(ApiConstants.SERVICE_CODE_CHECK)); //SMALLINT     1
			 inMap.put(ApiConstants.IN_REQ_FLAG, reqFlag);// U 
			 inMap.put(ApiConstants.IN_PREV_CHK_SYS_STA_CD,prevFlag);
			 inMap.put(ApiConstants.IN_SRVC_CODE,Integer.parseInt(ApiConstants.CHAR_ZERO));
			 inMap.put(ApiConstants.IN_ORG_STA_CD,ApiConstants.CHAR_BLANKSPACE);
			 inMap.put(ApiConstants.IN_PREV_ACH_SYS_STA_CD,ApiConstants.CHAR_BLANKSPACE);
			 
			 outMap = rollbackUpdOrgDetailsDAO.execute(inMap, apiMsgId);
				String respCode = null;
				String respDesc = null;
				String sqlCode = null;
				if (outMap != null && !outMap.isEmpty()){

					respCode = ((String) outMap.get(ApiConstants.SP_RESP_CODE));
					respDesc = ((String) outMap.get(ApiConstants.SP_RESP_MSG));
					sqlCode = ((String) outMap.get(ApiConstants.SQLCODE_PARM));
				} 		 
			 logger.info(apiMsgId, "SmartServiceEngine", "updateOrgDetailsRollback", "updateOrgDetailsRollback", " roll back the data through E3GMR095 proc end ", 
					 AmexLogger.Result.success, "", " for org id :",orgId," response code is :",respCode, " and response description is :",respDesc," and sql code is :"+sqlCode);			
	 }
		
		@Override
		public SupplierResponseVO fetchBIPSupplierOrgIdStatus(String bipSupOrdId, String apiMsgId,String activeOrinActive,String partnerId) throws Exception
		{
			logger.info(apiMsgId,"SmartServiceEngine","fetchBIPSupplierOrgIdStatus","fetchBIPSupplierOrgIdStatus",
				                "Start of invoking BIP Vendor inactivate API service",AmexLogger.Result.success, "");
			SupplierResponseVO suppRespObj =null;
			String serviceURI=EnvironmentPropertiesUtil.getProperty(ApiConstants.BIP_REST_SERVICE_PAYEE_ENROLLMENT_URI)+ApiConstants.CHAR_BACKSLASH+bipSupOrdId+ApiConstants.CHAR_BACKSLASH+activeOrinActive;
			String serviceURL=EnvironmentPropertiesUtil.getProperty(ApiConstants.BIP_REST_SERVICE_PAYEE_ENROLLMENT_URL)+ApiConstants.CHAR_BACKSLASH+bipSupOrdId+ApiConstants.CHAR_BACKSLASH+activeOrinActive;
			
			boolean isError = false;
			long startTimeStamp = System.currentTimeMillis();
			try{
			SupplierRequestVO enrollRequestVO = new SupplierRequestVO();
			String entityId=CommonUtils.getPayvePartnerEntityId(partnerId);
			enrollRequestVO.setEntityId(entityId);
			String jsonPayload=GenericUtilities.javaToJson(enrollRequestVO);
			suppRespObj=generateRestTemplate(ApiConstants.HTTPMETHODPUT, serviceURI, serviceURL, apiMsgId, jsonPayload, true);
			}catch(SSEApplicationException ex)
			{				 
			  isError = true;
			  logger.error(apiMsgId, "SmartServieEngine", "fetchBIPSupplierOrgIdStatus", "fetchBIPSupplierOrgIdStatus", " BIP organization deactivate request failed due to ",
					AmexLogger.Result.failure, "unexpected exception ", ex, " responseCode",ex.getResponseCode()," responseDescription", ex.getResponseDescription());
			}
			finally
			{
				final long endTimeStamp = System.currentTimeMillis();
				logger.info(apiMsgId, "SmartServieEngine", 	"fetchBIPSupplierOrgIdStatus",
				"fetchBIPSupplierOrgIdStatus", "request completed", isError ? AmexLogger.Result.failure	: AmexLogger.Result.success,
				"deactivate request completed",	"Total time taken to get response from BIP service is",
				(endTimeStamp - startTimeStamp) + " milliseconds("+ (endTimeStamp - startTimeStamp) / 1000.00+ " seconds)");
			}
			return suppRespObj;
		}
		

		
		@javax.annotation.Resource
		private SecurityHelper securityHelper;
		
		private SupplierResponseVO generateRestTemplate(String httpMethod, String serviceURI,
				String serviceURL,String apiMsgID,String payLoad,boolean ifEnrollment) throws Exception {
			logger.info(apiMsgID, "SmartServiceEngine", "generateRestTemplate", "generateRestTemplate", " invoking BIP vendor call start", 
					 AmexLogger.Result.success, "");
			SupplierResponseVO supRespVoObj=null;
			try
			{
			    String port=EnvironmentPropertiesUtil.getProperty(ApiConstants.BIP_REST_SERVICE_PORT);
			    String host=EnvironmentPropertiesUtil.getProperty(ApiConstants.BIP_REST_SERVICE_HOST);
			    String clientId=EnvironmentPropertiesUtil.getProperty(ApiConstants.BIP_REST_SERVICE_CLIENT_ID);
			    String secretKey=EnvironmentPropertiesUtil.getProperty(ApiConstants.BIP_REST_SERVICE_SECRET_KEY);
			    String authenticationHeader = securityHelper.getAuthenticationHeader(serviceURL, serviceURI, payLoad,secretKey, clientId, host, port,httpMethod);
			    //System.setProperty("https.protocols", "TLSv1.1,TLSv1.2");
				HttpHeaders headers = new HttpHeaders();
				if(ifEnrollment)
				{
					headers.set(ApiConstants.CONTENTTYPE,ApiConstants.APPLICATION_JSON);
					headers.set(ApiConstants.HTTP_METHOD, httpMethod.toString());
					headers.set(ApiConstants.REQUEST_URL, serviceURL);
				}
				else
				{
					headers.setAccept(Arrays.asList(org.springframework.http.MediaType.APPLICATION_JSON));	
				}
				headers.set(ApiConstants.AUTHORIZATION, authenticationHeader);
				headers.set(ApiConstants.AMEX_API_KEY,clientId);
				
				RestTemplate rest=new RestTemplate();
				HttpEntity<String> entity = new HttpEntity(payLoad, headers);
				logger.debug(apiMsgID, "SmartServiceEngine", "FetchBIPSupplierOrgIdServiceImpl", "generateRestTemplate", "Request ", AmexLogger.Result.success, 
							"Rest HTTP GET request Completed Successfully", "Json request", payLoad," for url ",serviceURL);
				ResponseEntity<SupplierResponseVO> result = rest.exchange(serviceURL, HttpMethod.PUT, entity, SupplierResponseVO.class);
				logger.debug(apiMsgID, "SmartServiceEngine", "FetchBIPSupplierOrgIdServiceImpl", "generateRestTemplate", "Request Completed", AmexLogger.Result.success, 
							"Rest HTTP GET request Completed Successfully", "Json Response", result.toString());
				if(null != result && result.getStatusCode()==HttpStatus.OK)
				{
					if(null != result.getBody())
					{
						supRespVoObj = result.getBody();
					}
				}
			}
			catch (HttpClientErrorException e) 
			{
			       String responseBody=e.getResponseBodyAsString();
			       logger.debug(apiMsgID,"SmartServiceEngine","FetchBIPSupplierOrgIdServiceImpl","generateRestTemplate",
			    		   "Failure from BIP for payment enrollment",AmexLogger.Result.failure, "","Response from BIP",responseBody);
			       if(responseBody!=null)
			       {			       
			    	supRespVoObj = GenericUtilities.jsonToJava(responseBody, SupplierResponseVO.class);
			       }
			}
			logger.info(apiMsgID, "SmartServiceEngine", "generateRestTemplate", "generateRestTemplate", " invoking BIP vendor call end", 
					 AmexLogger.Result.success, "");
			return supRespVoObj;
		}
		
		@Autowired
		private UpdateOrganizationStatusDAO updateOrganizationStatusDAO;
		
		@Override
		public String updateSupplierOrgId(String bipOrgId, String statusCd, String statusDesc, String partnerId, String orgId, String eventId) throws ParseException
		{
			logger.info(eventId, "SmartServiceEngine", "updateSupplierOrgId", "updateSupplierOrgId", 
					" updating the organization in ORG_PYMT_SYS_ENROLL_STA table through E3GMR023 start", 
					 AmexLogger.Result.success, "", " for vendor supplier id :",bipOrgId);
			
			String respCd=ApiConstants.CHAR_BLANKSPACE;
			String respDesc=ApiConstants.CHAR_BLANKSPACE;
			String sqlCode=ApiConstants.CHAR_BLANKSPACE;
			
			Map<String,  Object> inResMap = new HashMap<String,  Object>();

			inResMap.put(ApiConstants.PYMT_SYS_ORG_ID, bipOrgId); //74197772
			inResMap.put(ApiConstants.PYMT_SYS_STA_CD, statusCd); // A or I
			inResMap.put(ApiConstants.PYMT_SYS_STA_DS, statusDesc); // Active or Inactive			
			inResMap.put(ApiConstants.PYMT_SYS_STA_DT, DateTimeUtil.getDB2SqlDate(DateTimeUtil.getCurDateyyyyMMdd())); // 08/22/2016			
			inResMap.put(ApiConstants.PYMT_SYS_RESP_CD, ApiConstants.CHAR_ZERO); //0
			inResMap.put(ApiConstants.PYMT_SYS_RESP_DS, ApiConstants.IC_SUCCESS); // success
			inResMap.put(ApiConstants.PRTR_MAC_ID, partnerId); // edb98542-7fee-4400-a741-e7a6e8c7365b 
			inResMap.put(ApiConstants.PRTR_ORG_ID, orgId); //OSLUmPUG4OPpz3lF
			Map<String, Object> outResMap=updateOrganizationStatusDAO.execute(inResMap,eventId);
			if (outResMap != null && !outResMap.isEmpty())
			{
				respCd = StringUtils.stripToEmpty((String) outResMap.get(SchedulerConstants.RESP_CD));
				respDesc = StringUtils.stripToEmpty((String) outResMap.get(SchedulerConstants.RESP_MSG));
				sqlCode = StringUtils.stripToEmpty((String) outResMap.get(ApiConstants.SQLCODE_PARM));
			}
			
			logger.info(eventId, "SmartServiceEngine", "updateSupplierOrgId", "updateSupplierOrgId", " updating the organization in ORG_PYMT_SYS_ENROLL_STA table through E3GMR023 end", 
					 AmexLogger.Result.success, "", " for vendor supplier id :",bipOrgId," response code is :",respCd, " and response description is :",respDesc," and sql code is :"+sqlCode);
			return respCd;
		} // end of updateSupplierOrgId()

}
