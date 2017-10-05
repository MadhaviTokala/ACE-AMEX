package com.americanexpress.smartserviceengine.service.impl;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.ApiErrorConstants;
import com.americanexpress.smartserviceengine.common.payload.UpdateTCchangeRequestType;
import com.americanexpress.smartserviceengine.common.payload.UpdateTCchangeResponseType;
import com.americanexpress.smartserviceengine.common.util.ApiUtil;
import com.americanexpress.smartserviceengine.common.util.CommonUtils;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.validator.UpdateTcChangeRequestValidator;
import com.americanexpress.smartserviceengine.manager.UpdateTcChangeManager;
import com.americanexpress.smartserviceengine.service.UpdateTcChangeService;
//import com.americanexpress.smartserviceengine.api.util.ApiUtil;

@Service
public class UpdateTcChangeServiceImpl implements UpdateTcChangeService {

	    private static AmexLogger logger = AmexLogger.create(UpdateTcChangeServiceImpl.class);

	    @Autowired
	    private UpdateTcChangeManager  updateTcChangeManager;


	    @Autowired
	    private TivoliMonitoring tivoliMonitoring;


	   @Override
	public UpdateTCchangeResponseType updateTcChanges(UpdateTCchangeRequestType request, String apiMsgId) {

		   logger.info(
	                 apiMsgId,
	                 "SmartServiceEngine",
	                 "UpdateTcChangeServiceImpl",
	                 "updateTcChanges",
	                 "Satrt of update Payment TC Change Request",
	                 AmexLogger.Result.success, "Start");



		   String responseCode = null;
		   UpdateTCchangeResponseType objUpdateTCchangeResponseType = new UpdateTCchangeResponseType();

		   try {

			   objUpdateTCchangeResponseType = UpdateTcChangeRequestValidator.validateExceptionRequest(request, apiMsgId);
			   if (objUpdateTCchangeResponseType.getUpdateTCchangeResponse() != null && objUpdateTCchangeResponseType.getUpdateTCchangeResponse().getData() != null && objUpdateTCchangeResponseType.getUpdateTCchangeResponse().getData().getResponseCode() != null) {
				   return objUpdateTCchangeResponseType;
	            }else {

	            	//if (objUpdateTCchangeResponseType != null){
	            	if (!CommonUtils.validateClinetId(request.getUpdateTCchangeRequest().getData().getCommonRequestContext().getClientId(),ApiConstants.ICM_CLIENTID_MAP ) ) {
	            		responseCode = ApiErrorConstants.SSEAPITC003;
	            		UpdateTcChangeRequestValidator.setFailureResponse(request.getUpdateTCchangeRequest().getData().getCommonRequestContext(), responseCode, objUpdateTCchangeResponseType);

	        		}


	    		/*	logger.info("Validating Partner ID: "+request.getUpdateTCchangeRequest().getData().getCommonRequestContext().getPartnerId());
	    			Boolean result = partnerValidationManager.validatePartnerId(request.getUpdateTCchangeRequest().getData().getCommonRequestContext().getClientId(), apiMsgId);
	    			if (!result){
	    				responseCode = ApiErrorConstants.SSEAPIEN078;
	    				updateTcChangeRequestValidator.setFailureResponse(request.getUpdateTCchangeRequest().getData().getCommonRequestContext(), responseCode, objUpdateTCchangeResponseType);
	    				}*/
	    			}

			   /*
	             * If common field validation is failure, return error response
	             */
			   if (objUpdateTCchangeResponseType.getUpdateTCchangeResponse() != null && objUpdateTCchangeResponseType.getUpdateTCchangeResponse().getData() != null && objUpdateTCchangeResponseType.getUpdateTCchangeResponse().getData().getResponseCode() != null) {
	            	return objUpdateTCchangeResponseType;
	            }else{
	            	objUpdateTCchangeResponseType = updateTcChangeManager.UpdateTcChange(request.getUpdateTCchangeRequest().getData(), apiMsgId);
	            }

		} catch (Exception e) {


	            logger.error(
	                apiMsgId,
	                "SmartServiceEngine",
	                "UpdateTcChangeServiceImpl",
	                "Exception in UpdateTcChangeServiceImpl",
	                "Exception occured while processing service layer of UpdateTcChange Service",
	                AmexLogger.Result.failure,
	                "Exception in UpdateTcChange service layer ", e,
	                "resp_code", responseCode, "resp_msg",
	                ApiUtil.getErrorDescription(responseCode),"error_msg", e.getMessage());
	            logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_APPL_ERR_CD, TivoliMonitoring.SSE_APPL_ERR_MSG, apiMsgId));

		}
		   logger.info(
	                 apiMsgId,
	                 "SmartServiceEngine",
	                 "UpdateTcChangeServiceImpl",
	                 "updateTcChanges",
	                 "End of update Payment TC Change Request",
	                 AmexLogger.Result.success, "End");
		return objUpdateTCchangeResponseType;
	}




}
