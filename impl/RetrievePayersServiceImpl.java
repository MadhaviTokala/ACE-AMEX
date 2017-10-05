package com.americanexpress.smartserviceengine.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.PayerConstants;
import com.americanexpress.smartserviceengine.common.payload.AuthorizedPayer;
import com.americanexpress.smartserviceengine.common.payload.CommonContext;
import com.americanexpress.smartserviceengine.common.payload.RetrievePayersRequestData;
import com.americanexpress.smartserviceengine.common.payload.RetrievePayersRequestType;
import com.americanexpress.smartserviceengine.common.payload.RetrievePayersResponse;
import com.americanexpress.smartserviceengine.common.payload.RetrievePayersResponseData;
import com.americanexpress.smartserviceengine.common.payload.RetrievePayersResponseType;
import com.americanexpress.smartserviceengine.common.util.ApiUtil;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.validator.RetrievePayersValidator;
import com.americanexpress.smartserviceengine.common.vo.PayersDetailsListVO;
import com.americanexpress.smartserviceengine.dao.PayersListDetailsDAO;
import com.americanexpress.smartserviceengine.service.RetrievePayersService;

@Service
public class RetrievePayersServiceImpl implements RetrievePayersService {

    AmexLogger logger = AmexLogger.create(RetrievePayersServiceImpl.class);

    @Resource
    @Qualifier("payersListDetailsDAO")
    private PayersListDetailsDAO payersListDetailsDAO;

    @Resource
    private TivoliMonitoring tivoliMonitoring;

    @SuppressWarnings("unchecked")
    @Override
    public RetrievePayersResponseType retrievePayersDetails(RetrievePayersRequestType requestType, String apiMsgId) {
        RetrievePayersResponseType responseType = new RetrievePayersResponseType();
        RetrievePayersResponse response = new RetrievePayersResponse();
        RetrievePayersResponseData responseData = new RetrievePayersResponseData();
        RetrievePayersValidator payersValidator = new RetrievePayersValidator();

        Map<String, Object> inMap = new HashMap<String, Object>();
        Map<String, Object> outMap = new HashMap<String, Object>();
        response = payersValidator.validateCommonPayersRequest(requestType, apiMsgId);
        CommonContext commonResponseContext = new CommonContext();
        commonResponseContext = response.getData().getCommonResponseContext();

        if (response.getStatus().equals(ApiConstants.SUCCESS)) {
            inMap = createInMap(requestType.getRetrievePayersRequest().getData());
            outMap = payersListDetailsDAO.execute(inMap, apiMsgId);

            logger.debug(apiMsgId, "SmartServiceEngine", "RetrievePayersServiceImpl", "retrieve Payer IDs",
                "Start retrieving Payer Details from SP E3GMR120", AmexLogger.Result.success, "", "SPResponseCode",
                StringUtils.stripToEmpty((String) outMap.get(ApiConstants.SP_RESP_CD)));

			if ("SSEIN000".equals(StringUtils.stripToEmpty((String) outMap.get(ApiConstants.SP_RESP_CD)))) {
				List<PayersDetailsListVO> payerList = new ArrayList<PayersDetailsListVO>();
				List<AuthorizedPayer> authorizedPayers = new ArrayList<AuthorizedPayer>();
				payerList = (List<PayersDetailsListVO>) outMap.get(ApiConstants.RESULT_SET);
				if (payerList != null && !payerList.isEmpty()) {
					for (PayersDetailsListVO payersDetailsListVO : payerList) {
						AuthorizedPayer authorizedPayer = new AuthorizedPayer();
						authorizedPayer.setPayerEmailId(payersDetailsListVO.getPayerDetails());
						logger.info("Payer EmailId: "+ payersDetailsListVO.getPayerDetails());
						authorizedPayers.add(authorizedPayer);
					}
					responseData.setAuthorizedPayers(authorizedPayers);
					responseData.setCommonResponseContext(commonResponseContext);
					response.setData(responseData);
					response.setStatus(ApiConstants.SUCCESS);
				} else {
					responseData.setResponseCode(PayerConstants.SSEAPIPL008);
					responseData.setResponseDesc(ApiUtil.getErrorDescription(PayerConstants.SSEAPIPL008));
					responseData.setCommonResponseContext(commonResponseContext);
					response.setData(responseData);
					response.setStatus(ApiConstants.FAIL);
				}
			} else if ("SSEIN001".equals(StringUtils.stripToEmpty((String) outMap.get(ApiConstants.SP_RESP_CD)))) {
                responseData.setResponseCode(PayerConstants.SSEAPIPL008);
                responseData.setResponseDesc(ApiUtil.getErrorDescription(PayerConstants.SSEAPIPL008));
                responseData.setCommonResponseContext(commonResponseContext);
                response.setData(responseData);
                response.setStatus(ApiConstants.FAIL);
            } else {
            	logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_120_ERR_CD, TivoliMonitoring.SSE_SP_120_ERR_MSG, apiMsgId));
                logger.error(apiMsgId, "SmartServiceEngine", "RetrievePayersServiceImpl", "retrieve Payer IDs",
                    "Failure Response code from SP E3GMR120", AmexLogger.Result.failure, "", "SPResponseCode",
                    StringUtils.stripToEmpty((String) outMap.get(ApiConstants.SP_RESP_CD)));
                responseData.setResponseCode(PayerConstants.INTERNAL_SERVER_ERROR_PL);
                responseData.setResponseDesc(ApiUtil.getErrorDescription(PayerConstants.INTERNAL_SERVER_ERROR_PL));
                responseData.setCommonResponseContext(commonResponseContext);
                response.setData(responseData);
                response.setStatus(ApiConstants.FAIL);
            }
        }
        responseType.setRetrievePayersResponse(response);
        return responseType;
    }

    public Map<String, Object> createInMap(RetrievePayersRequestData data) {
        Map<String, Object> inMap = new HashMap<String, Object>();
        inMap.put(ApiConstants.IN_PRTR_ID, data.getCommonRequestContext().getPartnerId());
        inMap.put(ApiConstants.IN_ORG_ID, data.getOrganizationId());
        inMap.put(ApiConstants.IN_ACCT_ID, data.getPartnerAccountId());
        return inMap;
    }

}
