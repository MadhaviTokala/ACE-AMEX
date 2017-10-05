package com.americanexpress.smartserviceengine.service.impl;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.ApiErrorConstants;
import com.americanexpress.smartserviceengine.common.exception.SSEApplicationException;
import com.americanexpress.smartserviceengine.common.payload.CommonContext;
import com.americanexpress.smartserviceengine.common.payload.GetExceptionsRequestType;
import com.americanexpress.smartserviceengine.common.payload.GetExceptionsResponseType;
import com.americanexpress.smartserviceengine.common.util.ApiUtil;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.validator.ExceptionRequestValidator;
import com.americanexpress.smartserviceengine.manager.ExceptionDataManager;
import com.americanexpress.smartserviceengine.manager.PartnerValidationManager;
import com.americanexpress.smartserviceengine.service.ExceptionDataService;

@Service
public class ExceptionDataServiceImpl implements ExceptionDataService {

    private static AmexLogger logger = AmexLogger.create(ExceptionDataServiceImpl.class);

    @Resource
    private ExceptionDataManager  exceptionDataManager;
    
    @Resource
	private PartnerValidationManager partnerValidationManager;

    @Resource
    private TivoliMonitoring tivoliMonitoring;

    public GetExceptionsResponseType getExceptionData(GetExceptionsRequestType request,String apiMsgId) {

        String responseCode = null;
        CommonContext commonResponseContext = new CommonContext();
        GetExceptionsResponseType exceptionDataResponce = new GetExceptionsResponseType();
        try{

            exceptionDataResponce = ExceptionRequestValidator.validateExceptionRequest(request, apiMsgId);
            
            if (exceptionDataResponce != null){
    			
    			logger.info("Validating Partner ID: "+request.getGetExceptionsRequest().getData().getCommonRequestContext().getPartnerId());
    			
    			/**
				 * Partner Id eh-cache changes-Comparing partner id in request with list of partner ids in cache.
				 * If partner Id is valid PartnerValidationManager returns corresponding partner name.  
				 */
				String partnerName = partnerValidationManager.validatePartnerId(request.getGetExceptionsRequest().getData().getCommonRequestContext().getPartnerId(),apiMsgId);

				if (partnerName != null) {
						logger.info(apiMsgId,"SmartServiceEngine", "Get Exception API Service","End of Validating Partner ID",
								"Validating Partner ID was successful",AmexLogger.Result.success, "", "partnerId", request.getGetExceptionsRequest().getData().getCommonRequestContext().getPartnerId());

				}else{
					responseCode = ApiErrorConstants.SSEAPIEN078;
		            ExceptionRequestValidator.setFailureResponse(commonResponseContext, responseCode, exceptionDataResponce);


				}
            }

            /*
             * If common field validation is failure, return error response
             */

            if (exceptionDataResponce.getGetExceptionsResponse() != null && exceptionDataResponce.getGetExceptionsResponse().getData() != null && exceptionDataResponce.getGetExceptionsResponse().getData().getResponseCode() != null) {
                return exceptionDataResponce;
            }

            /*
             * If common field validation is successful, continue further
             * processing
             */
            commonResponseContext.setPartnerId(request.getGetExceptionsRequest().getData().getCommonRequestContext().getPartnerId());
            commonResponseContext.setRequestId(request.getGetExceptionsRequest().getData().getCommonRequestContext().getRequestId());
            commonResponseContext.setPartnerName(request.getGetExceptionsRequest().getData().getCommonRequestContext().getPartnerName());
            commonResponseContext.setTimestamp(ApiUtil.getCurrentTimeStamp());

            exceptionDataResponce = exceptionDataManager.getExceptionData(request.getGetExceptionsRequest().getData(),apiMsgId);

        } catch (SSEApplicationException e) {
            responseCode = e.getResponseCode();
            ExceptionRequestValidator.setFailureResponse(commonResponseContext, responseCode, exceptionDataResponce);
            logger.error(
                apiMsgId,
                "SmartServiceEngine",
                "Get Exception API Service",
                "Exception in getException service",
                "Exception occured while processing service layer of getException Service",
                AmexLogger.Result.failure,
                "Exception in getException service layer", e, "resp_code",
                responseCode, "resp_msg",
                ApiUtil.getErrorDescription(responseCode), "error_msg", e.getErrorMessage());

            //Tivoli Monitoring alert
            if (null != responseCode
                    && responseCode.equals(
                        ApiErrorConstants.EXC_API_INTERNAL_SERVER_ERR_CD)) {
                logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_APPL_ERR_CD, TivoliMonitoring.SSE_APPL_ERR_MSG, apiMsgId));
            }

        } catch (Exception e) {
            responseCode = ApiErrorConstants.EXC_API_INTERNAL_SERVER_ERR_CD;
            ExceptionRequestValidator.setFailureResponse(commonResponseContext, responseCode, exceptionDataResponce);
            logger.error(
                apiMsgId,
                "SmartServiceEngine",
                "Get Exception API Service",
                "Exception in getException service",
                "Exception occured while processing service layer of getException Service",
                AmexLogger.Result.failure,
                "Exception in getException service layer ", e, "resp_code",
                responseCode, "resp_msg",
                ApiUtil.getErrorDescription(responseCode), "error_msg", e.getMessage());
            logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_APPL_ERR_CD, TivoliMonitoring.SSE_APPL_ERR_MSG, apiMsgId));
        }
        return exceptionDataResponce;

    }
}
