package com.americanexpress.smartserviceengine.service.impl;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.ApiErrorConstants;
import com.americanexpress.smartserviceengine.common.payload.CommonContext;
import com.americanexpress.smartserviceengine.common.payload.UpdatePaymentStatusRequestData;
import com.americanexpress.smartserviceengine.common.payload.UpdatePaymentStatusRequestType;
import com.americanexpress.smartserviceengine.common.payload.UpdatePaymentStatusResponse;
import com.americanexpress.smartserviceengine.common.payload.UpdatePaymentStatusResponseData;
import com.americanexpress.smartserviceengine.common.payload.UpdatePaymentStatusResponseType;
import com.americanexpress.smartserviceengine.common.util.ApiUtil;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.validator.UpdatePaymentStatusValidator;
import com.americanexpress.smartserviceengine.common.vo.UpdatePaymentStatusVO;
import com.americanexpress.smartserviceengine.manager.UpdatePaymentStatusManager;
import com.americanexpress.smartserviceengine.service.UpdatePymtStatusService;

@Service
public class UpdatePymtStatusServiceImpl implements UpdatePymtStatusService{

    private static final AmexLogger logger = AmexLogger.create(UpdatePymtStatusServiceImpl.class);

    @Resource
    private UpdatePaymentStatusManager updatePaymentStatusManager;

    @Resource
    private TivoliMonitoring tivoliMonitoring;

    @Override
    public UpdatePaymentStatusResponseType updatePymtStatus(UpdatePaymentStatusRequestType requestType, String apiMsgId) {
        logger.debug(apiMsgId, "SmartServiceEngine", "Update Payment Status API","Start of Update Payment Status service",
            "Processing Update Payment Status API request",AmexLogger.Result.success, "");
        UpdatePaymentStatusResponseType responseType = new UpdatePaymentStatusResponseType();
        UpdatePaymentStatusResponse response = new UpdatePaymentStatusResponse();
        CommonContext commonResponseContext = new CommonContext();
        String responseCode = null;
        try{
            response = UpdatePaymentStatusValidator.validateCommonContextRequest(requestType, apiMsgId);

            /*
             * If common field validation is failure, return error response
             */
            if(response != null && response.getData() != null) {
                responseCode = response.getData().getResponseCode();
                if ( StringUtils.isNotBlank(responseCode)) {
                    responseType.setUpdatePaymentStatusResponse(response);
                    return responseType;
                }
            }

            /*
             * If Request data field validation is successful, continue further
             * processing
             */
            CommonContext commonContext = requestType.getUpdatePaymentStatusRequest().getData().getCommonRequestContext();
            commonResponseContext.setClientId(commonContext.getClientId());
            commonResponseContext.setRequestId(commonContext.getRequestId());
            commonResponseContext.setPartnerName(commonContext.getPartnerName());
            commonResponseContext.setTimestamp(ApiUtil.getCurrentTimeStamp());

            logger.info("Validating Client ID: "+requestType.getUpdatePaymentStatusRequest().getData().getCommonRequestContext().getClientId());

            //Boolean result = partnerValidationManager.validatePartnerId(requestType.getUpdatePaymentStatusRequest().getData().getCommonRequestContext().getClientId(), apiMsgId);

            /*if (!result){
				responseCode = ApiErrorConstants.SSEAPIPS003;
				UpdatePaymentStatusValidator.setFailureResponse(commonResponseContext, responseCode, response);
				responseType.setUpdatePaymentStatusResponse(response);
				return responseType;
			}*/

            String errorCode = UpdatePaymentStatusValidator.validateRequestData(requestType.getUpdatePaymentStatusRequest().getData(), apiMsgId);
            if(StringUtils.isNotBlank(errorCode)){
                UpdatePaymentStatusValidator.setFailureResponse(commonResponseContext, errorCode, response);
                responseType.setUpdatePaymentStatusResponse(response);
                return responseType;
            }

            UpdatePaymentStatusVO updatePaymentStatusVO = updatePaymentStatusManager.updatePaymentStatus(convertToRequestvo(requestType.getUpdatePaymentStatusRequest().getData()), apiMsgId);
            convertToResponseType(updatePaymentStatusVO, response, commonResponseContext);
        }catch(Exception exception){
            responseCode = ApiErrorConstants.RETURN_API_INTERNAL_SERVER_ERR_CD;
            setFailureResponse(commonResponseContext, responseCode, response);
            logger.error(apiMsgId,"SmartServiceEngine","Update Payment Status Service",
                "Exception in Update Payment Status service","Exception occured while processing service layer of Update Payment Status Service",
                AmexLogger.Result.failure,"Exception in Update Payment Status service layer ", exception,
                "resp_code", responseCode, "resp_msg",ApiUtil.getErrorDescription(responseCode));
            logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_APPL_ERR_CD, TivoliMonitoring.SSE_APPL_ERR_MSG, apiMsgId));
        }
        responseType.setUpdatePaymentStatusResponse(response);
        return responseType;
    }

    private UpdatePaymentStatusVO convertToRequestvo(UpdatePaymentStatusRequestData requestData){
        UpdatePaymentStatusVO updatePaymentStatusvo = new UpdatePaymentStatusVO();
        CommonContext commonContext = requestData.getCommonRequestContext();
        updatePaymentStatusvo.setRequestId(commonContext.getRequestId());
        updatePaymentStatusvo.setPartnerId(commonContext.getPartnerId());
        updatePaymentStatusvo.setTimestamp(commonContext.getTimestamp());
        updatePaymentStatusvo.setUpId(requestData.getUpId());
        updatePaymentStatusvo.setCardAcctNum(requestData.getCardAcctNum());
        updatePaymentStatusvo.setPaymentConfNmbr(requestData.getPaymentConfNmbr());
        updatePaymentStatusvo.setPaymentType(requestData.getPaymentType());
        updatePaymentStatusvo.setPaymentStatus(requestData.getPaymentStatus());
        updatePaymentStatusvo.setReturnCd(requestData.getReturnCd());
        updatePaymentStatusvo.setReturnDesc(requestData.getReturnDesc());
        return updatePaymentStatusvo;
    }

    private void convertToResponseType(UpdatePaymentStatusVO updatePaymentStatusVO,UpdatePaymentStatusResponse response, CommonContext commonResponseContext) {
        if(ApiConstants.SUCCESS.equals(updatePaymentStatusVO.getStatus())){
            response.setStatus(ApiConstants.SUCCESS);
            UpdatePaymentStatusResponseData successResponseData = new UpdatePaymentStatusResponseData();
            successResponseData.setCommonResponseContext(commonResponseContext);
            successResponseData.getCommonResponseContext().setTimestamp(ApiUtil.getCurrentTimeStamp());
            successResponseData.setResponseCode(updatePaymentStatusVO.getResponseCode());
            successResponseData.setResponseDesc(updatePaymentStatusVO.getResponseDesc());
            response.setData(successResponseData);
        }else{
            UpdatePaymentStatusValidator.setFailureResponse(commonResponseContext, updatePaymentStatusVO.getResponseCode(), response);
        }
    }

    public void setFailureResponse(CommonContext commonResponseContext, String errorCode,
            UpdatePaymentStatusResponse response) {
        UpdatePaymentStatusResponseData failureResponseData = new UpdatePaymentStatusResponseData();
        failureResponseData.setCommonResponseContext(commonResponseContext);
        failureResponseData.setResponseCode(errorCode);
        failureResponseData.setResponseDesc(ApiUtil.getErrorDescription(errorCode));
        failureResponseData.getCommonResponseContext().setTimestamp(ApiUtil.getCurrentTimeStamp());
        response.setData(failureResponseData);
        response.setStatus(ApiConstants.FAIL);
    }

}
