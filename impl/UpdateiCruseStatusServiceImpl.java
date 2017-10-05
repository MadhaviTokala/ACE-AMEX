package com.americanexpress.smartserviceengine.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.ApiErrorConstants;
import com.americanexpress.smartserviceengine.common.exception.SSEApplicationException;
import com.americanexpress.smartserviceengine.common.payload.UpdateiCruseStatusRequestType;
import com.americanexpress.smartserviceengine.common.payload.UpdateiCruseStatusResponse;
import com.americanexpress.smartserviceengine.common.payload.UpdateiCruseStatusResponseData;
import com.americanexpress.smartserviceengine.common.payload.UpdateiCruseStatusResponseType;
import com.americanexpress.smartserviceengine.common.util.ApiUtil;
import com.americanexpress.smartserviceengine.common.validator.UpdateiCruseStatusRequestValidator;
import com.americanexpress.smartserviceengine.manager.UpdateiCruseStatusManager;
import com.americanexpress.smartserviceengine.service.UpdateiCruseStatusService;

@Service
public class UpdateiCruseStatusServiceImpl implements UpdateiCruseStatusService {

    private static AmexLogger logger = AmexLogger.create(UpdateiCruseStatusServiceImpl.class);

    @Autowired
    UpdateiCruseStatusManager updateiCruseStatusManager;

    @Override
    public UpdateiCruseStatusResponseType updateiCruseStatus(UpdateiCruseStatusRequestType request, String apiMsgId) {

        UpdateiCruseStatusResponseType responseType = new UpdateiCruseStatusResponseType();
        UpdateiCruseStatusResponse response = new UpdateiCruseStatusResponse();
        UpdateiCruseStatusResponseData data = new UpdateiCruseStatusResponseData();

        response = UpdateiCruseStatusRequestValidator.validateCommoniCruseRequest(request, apiMsgId);
        logger.info(apiMsgId, "SmartServiceEngine", "UpdateiCruseStatusServiceImpl", "updateiCruseStatus",
                "Fields Validation successful", AmexLogger.Result.success, "");
        try {

        	 if(response.getStatus().equals(ApiConstants.SUCCESS)){
        		 response =updateiCruseStatusManager.updateiCruseStatus(request.getUpdateiCruseStatusRequest().getData(),response.getData().getCommonResponseContext(), apiMsgId);
        	 }
        } catch (SSEApplicationException e) {
        	data.setResponseCode(ApiErrorConstants.INTERNAL_SERVER_ERROR_ICS);
        	data.setResponseDesc(ApiUtil.getErrorDescription(ApiErrorConstants.INTERNAL_SERVER_ERROR_ICS));
            response.setData(data);
        }
        responseType.setUpdateiCruseStatusResponse(response);
        return responseType;
    }
}
