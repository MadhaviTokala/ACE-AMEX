package com.americanexpress.smartserviceengine.service.impl;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.dao.FetchAssociateOrgIdDAO;
import com.americanexpress.smartserviceengine.service.FetchAssociateOrgIdService;

@Service
public class FetchAssociateOrgIdServiceImpl implements FetchAssociateOrgIdService{
	
	private static AmexLogger LOG = AmexLogger.create(FetchAssociateOrgIdServiceImpl.class);
	
	@Resource
	 @Qualifier("fetchAssociateOrgIdDAO")
	 private FetchAssociateOrgIdDAO fetchAssociateOrgIdDAO;
	
	@Resource
	private TivoliMonitoring tivoliMonitoring;

	@Override
	public String fetchAssociateOrgId(String sseOrgId,  String apiMsgId) {		
		
		String associateOrgId=null;
		Map<String,Object> inMap=new HashMap<String,Object>();
		inMap.put(ApiConstants.IN_SSE_ORG_ID, sseOrgId);
		Map<String,Object> outMap=fetchAssociateOrgIdDAO.execute(inMap, apiMsgId);
		if(outMap!=null && !outMap.isEmpty()){
			if(outMap.get(ApiConstants.O_BUYER_ORG_ID)!=null){
			associateOrgId=StringUtils.isNotBlank(outMap.get(ApiConstants.O_BUYER_ORG_ID).toString().trim())?outMap.get(ApiConstants.O_BUYER_ORG_ID).toString().trim():null;
			}
			LOG.info(apiMsgId,"SmartServiceEngine","Manage Enrollment API","FetchAssociateOrgIdServiceImpl - fetchAssociateOrgId", "Fetch associate org id : SP E3GMR406 is successful",
		                AmexLogger.Result.success, "",ApiConstants.SQLCODE_PARM, outMap.get(ApiConstants.SQLCODE_PARM).toString(),
		                "resp_code", outMap.get(ApiConstants.SP_RESP_CD).toString(), "resp_msg",outMap.get(ApiConstants.SP_RESP_MSG).toString());

		}else{
			LOG.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_406_ERR_CD, TivoliMonitoring.SSE_SP_406_ERR_MSG, apiMsgId));

			LOG.error(apiMsgId,"SmartServiceEngine", "Manage Enrollment API", "FetchAssociateOrgIdServiceImpl - fetchAssociateOrgId",
                    "Fetch associate org id : SP E3GMR406 is not successful", AmexLogger.Result.failure, "", ApiConstants.SQLCODE_PARM,
                    outMap.get(ApiConstants.SQLCODE_PARM).toString(),"resp_code", outMap.get(ApiConstants.SP_RESP_CD).toString(), "resp_msg",outMap.get(ApiConstants.SP_RESP_MSG).toString());
         
		}
		return associateOrgId;
	}



}
