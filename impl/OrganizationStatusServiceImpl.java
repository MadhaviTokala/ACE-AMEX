 package com.americanexpress.smartserviceengine.service.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.common.constants.SchedulerConstants;
import com.americanexpress.smartserviceengine.common.constants.SchedulerErrorConstants;
import com.americanexpress.smartserviceengine.common.exception.SSEApplicationException;
import com.americanexpress.smartserviceengine.common.util.EnvironmentPropertiesUtil;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.vo.OrgRequestVO;
import com.americanexpress.smartserviceengine.common.vo.OrgResponseVO;
import com.americanexpress.smartserviceengine.dao.OrganizationDAO;
import com.americanexpress.smartserviceengine.dao.UpdateOrganizationStatusDAO;
import com.americanexpress.smartserviceengine.helper.GetOrganizationInfoRequestHelper;
import com.americanexpress.smartserviceengine.service.OrganizationStatusService;

/**
 * @author vishwakumar_c
 *
 */

public class OrganizationStatusServiceImpl implements OrganizationStatusService{

	AmexLogger logger = AmexLogger.create(OrganizationStatusServiceImpl.class);

	@Autowired
	private OrganizationDAO organizationDAO;
	@Autowired
	private UpdateOrganizationStatusDAO updateOrganizationStatusDAO;
	@Autowired
    private TivoliMonitoring tivoliMonitoring;
	@Autowired
	private GetOrganizationInfoRequestHelper orgInfoRequestHelper;


	@SuppressWarnings("unchecked")
	@Override
	public void getOrganizationData(String eventId) throws SSEApplicationException{


		try{
		 logger.debug(eventId, "SmartServiceEngine", "Organization Status Scheduler Service", "OrganizationStatusServiceImpl : getOrganizationData",
					"Start of  OrganizationStatusService", AmexLogger.Result.success, "");

		List<OrgRequestVO> orgDataResultSet=null;
		Map<String, String> inMap  = new HashMap<String, String>();
		logger.debug(eventId, "SmartServiceEngine", "Organization Status Scheduler Service", "OrganizationStatusServiceImpl : getOrganizationData",
				"Start of Get Organization Details SP E3GMR021", AmexLogger.Result.success, "");

		Map<String, Object> outMap = organizationDAO.execute((HashMap) inMap,eventId);

	     if (outMap != null && !outMap.isEmpty() && outMap
                 .get(SchedulerConstants.RESP_CD) != null && SchedulerConstants.RESPONSE_CODE_SUCCESS.equalsIgnoreCase(outMap
                     .get(SchedulerConstants.RESP_CD).toString().trim())) {
			orgDataResultSet = (List<OrgRequestVO>) outMap.get(SchedulerConstants.RESULT_SET);
			String resultCode = outMap.get("RESP_CD").toString().trim();
			String resultMessage = outMap.get("RESP_MSG").toString().trim();
			String resultParam = outMap.get("SQLCODE_PARM").toString().trim();

			logger.debug(eventId, "SmartServiceEngine", "Organization Status Scheduler Service", "OrganizationStatusServiceImpl : getOrganizationData",
					"End of Get Organization Details SP E3GMR021", AmexLogger.Result.success, "Success Response from SP",SchedulerConstants.RESP_CD, resultCode,SchedulerConstants.RESP_MSG,
					resultMessage,SchedulerConstants.SQLCODE_PARM,resultParam);
		}
		else {
			logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_021_ERR_CD, TivoliMonitoring.SSE_SP_021_ERR_MSG, eventId));
			logger.debug(eventId, "SmartServiceEngine", "Organization Status Scheduler Service", "OrganizationStatusServiceImpl : getOrganizationData",
					"End of Get Organization Details SP E3GMR021", AmexLogger.Result.failure, "outMap is null or error resp_code from SP");
		}

		if(orgDataResultSet != null && !orgDataResultSet.isEmpty()){
			logger.debug(eventId, "SmartServiceEngine", "Organization Status Scheduler Service", "OrganizationStatusServiceImpl : getOrganizationData",
					"Check resultset map for SP E3GMR021", AmexLogger.Result.success, "", "Org ResultSet Size:", new Integer( orgDataResultSet.size()).toString(),
					"Org ResultSet:",
					orgDataResultSet.toString());

			Iterator<OrgRequestVO> iterator = orgDataResultSet.iterator();
			while(iterator.hasNext()){
				OrgRequestVO orgDataDetails = iterator.next();

				logger.info(eventId, "SmartServiceEngine", "Organization Status Scheduler Service", "OrganizationStatusServiceImpl : getOrganizationData",
                                    "Logging for splunk checking ORG status", AmexLogger.Result.success, "",
                                    "partnerId" ,orgDataDetails.getPartnerId(),"organizationId",orgDataDetails.getOrganizationId(), 
                                    "partnerName",orgDataDetails.getPartnername(),"paymentEntityId",orgDataDetails.getPaymentEntityId());
				
				
				logger.debug(eventId, "SmartServiceEngine", "Organization Status Scheduler Service", "OrganizationStatusServiceImpl : getOrganizationData",
						"Calling Organization Status Service - invoke BIP API", AmexLogger.Result.success, "",
						"organizationId" ,orgDataDetails.getOrganizationId(),"paymentEntityId", orgDataDetails.getPaymentEntityId());

				try {

						Map<String, Object> statusMap  = new HashMap<String, Object>();

						statusMap =	orgInfoRequestHelper.getOrganizationInfo(orgDataDetails, eventId);

						OrgResponseVO response = (OrgResponseVO) statusMap.get(SchedulerConstants.RESPONSE_DETAILS);

						Map<String,  Object> inResMap = new HashMap<String,  Object>();

						inResMap.put("PYMT_SYS_ORG_ID", orgDataDetails.getPaymentEntityId());
						inResMap.put("PYMT_SYS_STA_CD", response.getOrgStatusCd());
						inResMap.put("PYMT_SYS_STA_DS", response.getOrgStatusDesc());
						inResMap.put("PYMT_SYS_STA_DT", response.getOrgStatusDate());
						inResMap.put("PYMT_SYS_RESP_CD", response.getOrgRespCd());
						inResMap.put("PYMT_SYS_RESP_DS", response.getOrgRespDesc());
						inResMap.put("PRTR_MAC_ID", orgDataDetails.getPartnerId());
						inResMap.put("PRTR_ORG_ID", response.getOrganizationId());


					  Map outResMap=updateOrganizationStatusDAO.execute(inResMap,eventId);
						logger.debug(eventId, "SmartServiceEngine", "Organization Status Scheduler Service", "OrganizationStatusServiceImpl : getOrganizationData",
								"Start of Calling updateOrganizationDAO", AmexLogger.Result.success, "");
						String respCode = ((String) outMap.get(SchedulerConstants.RESP_CD)).trim();

						if (outResMap != null && !outResMap.isEmpty() &&
								SchedulerConstants.SUC_SSEIN000.equalsIgnoreCase(((String) outResMap.get(SchedulerConstants.RESP_CD)).trim())) {
							logger.debug(eventId,"SmartServiceEngine", "Organization Status Scheduler Service", "OrganizationStatusServiceImpl : getOrganizationData",
									"UpdateOrganizationDAO - SP E3GMR023 is Successful"	,	AmexLogger.Result.success, "",
									SchedulerConstants.SQLCODE_PARM,outMap.get(SchedulerConstants.SQLCODE_PARM).toString(),
									"resp_code", respCode, "resp_msg",	outMap.get(SchedulerConstants.RESP_MSG).toString());


							logger.info(eventId, "SmartServiceEngine", "Organization Status Scheduler Service", "OrganizationStatusServiceImpl : getOrganizationData",
			                                    "Logging for splunk Updatating ORG status", AmexLogger.Result.success, "",
			                                    "partnerId" ,orgDataDetails.getPartnerId(),"organizationId",response.getOrganizationId(),
			                                    "paymentEntityId",orgDataDetails.getPaymentEntityId(),"prgStatusCd",response.getOrgStatusCd(),
			                                    "orgStatusDesc",response.getOrgStatusDesc(),"orgStatusDate",response.getOrgStatusDate(),
			                                    "orgRespCd",response.getOrgRespCd(),"orgRespDesc",response.getOrgRespDesc());




						}

						else if(outResMap != null && !outResMap.isEmpty() &&
								((SchedulerErrorConstants.RNF_SSEIN002).equalsIgnoreCase(((String) outResMap.get(SchedulerConstants.RESP_CD)).trim())
								||(SchedulerErrorConstants.RNF_SSEIN004).equalsIgnoreCase(((String) outResMap.get(SchedulerConstants.RESP_CD)).trim())))
						{
							logger.debug(
									eventId,"SmartServiceEngine", "Organization Status Scheduler Service", "OrganizationStatusServiceImpl : getOrganizationData",
									"UpdateOrganizationDAO - SP E3GMR023 is not Successful"	,	AmexLogger.Result.failure, "RECORD NOT FOUND",
									SchedulerConstants.SQLCODE_PARM,
									outMap.get(SchedulerConstants.SQLCODE_PARM).toString(),
									"resp_code", respCode, "resp_msg",
									outMap.get(SchedulerConstants.RESP_MSG).toString());
						}
						else {
								// Update Organization SP execution failed
							logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_023_ERR_CD, TivoliMonitoring.SSE_SP_023_ERR_MSG, eventId));
								logger.error(
										eventId,"SmartServiceEngine", "Organization Status Scheduler Service", "OrganizationStatusServiceImpl : getOrganizationData",
										"UpdateOrganizationDAO - SP E3GMR023 is not Successful"	,	AmexLogger.Result.failure, "EXCEPTION OCCURRED WITH CODE",
										SchedulerConstants.SQLCODE_PARM,
										outMap.get(SchedulerConstants.SQLCODE_PARM).toString(),
										"resp_code", respCode, "resp_msg",
										outMap.get(SchedulerConstants.RESP_MSG).toString());
							}

						logger.debug(eventId, "SmartServiceEngine", "Organization Status Scheduler Service", "OrganizationStatusServiceImpl : getOrganizationData",
								"End of Calling updateOrganizationDAO", AmexLogger.Result.success, "");
	    			}catch (SSEApplicationException ex) {
						String responseCode = ex.getResponseCode();
						logger.error(eventId, "SmartServiceEngine", "Organization Status Scheduler Service", "OrganizationStatusServiceImpl : getOrganizationData",
								"Exception while Calling Organization Status Service - invoke BIP API",
								AmexLogger.Result.failure,
								 "SSEApplication Exception in getOrganizationData", ex, "ErrorMsg:", ex.getMessage(),"responseCode", responseCode, "responseDesc", ex.getResponseDescription());
					}catch(Exception e){
						logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SCHED_ERR_CD, TivoliMonitoring.SSE_SCHED_ERR_MSG, eventId));
	    				logger.error(eventId, "SmartServiceEngine", "Organization Status Scheduler Service", "OrganizationStatusServiceImpl : getOrganizationData",
							"Exception while Calling Organization Status Service - invoke BIP API", AmexLogger.Result.failure,"Failed to execute getOrganizationData",e, "ErrorMsg:", e.getMessage());
				}
			}

		}
		else
		{
			logger.debug(eventId, "SmartServiceEngine", "Organization Status Scheduler Service", "OrganizationStatusServiceImpl : getOrganizationData",
					"Check resultset map for SP E3GMR021", AmexLogger.Result.failure, "Result set is null");
		}
		 logger.debug(eventId, "SmartServiceEngine", "Organization Status Scheduler Service", "OrganizationStatusServiceImpl : getOrganizationData",
					"End of  OrganizationStatusService", AmexLogger.Result.success, "");
	} catch(Exception e){
		logger.error(eventId, "SmartServiceEngine", "Organization Status Scheduler Service", "OrganizationStatusServiceImpl : getOrganizationData",
				"Exception while processing Organization Status Service call", AmexLogger.Result.failure,"Failed to execute getOrganizationData",e, "ErrorMsg:", e.getMessage());
		throw new SSEApplicationException(
				SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD,
				EnvironmentPropertiesUtil
						.getProperty(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD), e);
	}
	}


}
