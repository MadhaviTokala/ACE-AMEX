/**
 * <p>
 * Copyright © 2014 AMERICAN EXPRESS. All Rights Reserved.
 * </p>
 * <p>
 * AMERICAN EXPRESS CONFIDENTIAL. All information, copyrights, trade secrets<br>
 * and other intellectual property rights, contained herein are the property<br>
 * of AMERICAN EXPRESS. This document is strictly confidential and must not be
 * <br>
 * copied, accessed, disclosed or used in any manner, in whole or in part,<br>
 * without Amex's express written authorization.
 * </p>
 */
package com.americanexpress.smartserviceengine.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.PayerConstants;
import com.americanexpress.smartserviceengine.common.exception.SSEApplicationException;
import com.americanexpress.smartserviceengine.common.payload.RetrievePaymentLimitRequestType;
import com.americanexpress.smartserviceengine.common.payload.RetrievePaymentLimitResponse;
import com.americanexpress.smartserviceengine.common.payload.RetrievePaymentLimitResponseData;
import com.americanexpress.smartserviceengine.common.payload.RetrievePaymentLimitResponseType;
import com.americanexpress.smartserviceengine.common.util.ApiUtil;
import com.americanexpress.smartserviceengine.common.validator.RetrievePaymentLimitRequestValidator;
import com.americanexpress.smartserviceengine.manager.RetrievePaymentLimitManager;
import com.americanexpress.smartserviceengine.service.RetrievePaymentLimitService;

@Service
public class RetrievePaymentLimitServiceImpl implements RetrievePaymentLimitService {

    private static AmexLogger logger = AmexLogger.create(RetrievePaymentLimitServiceImpl.class);

    @Autowired
    RetrievePaymentLimitManager pymtLimitManager;

    @Override
    public RetrievePaymentLimitResponseType retrievePaymentLimit(RetrievePaymentLimitRequestType requestType,
            String apiMsgId) {
        logger.info(apiMsgId, "SmartServiceEngine", "RetrievePaymentLimitServiceImpl", "retrievePaymentLimit",
            "Start of  retrievePaymentLimit", AmexLogger.Result.success, "START");
        RetrievePaymentLimitResponseType responseType = new RetrievePaymentLimitResponseType();
        RetrievePaymentLimitResponse response = new RetrievePaymentLimitResponse();
        RetrievePaymentLimitResponseData data = new RetrievePaymentLimitResponseData();
        response = RetrievePaymentLimitRequestValidator.validateCommonPaymentRequest(requestType, apiMsgId);
        try {
            if (response.getStatus().equals(ApiConstants.SUCCESS)) {
                response = pymtLimitManager.retrievePaymentLimit(requestType.getRetrievePaymentLimitDtlsRequest().getData(),
                        response.getData().getCommonResponseContext(), apiMsgId);
            }
        } catch (SSEApplicationException e) {
            logger.error(apiMsgId, "SmartServiceEngine", "RetrievePaymentLimitServiceImpl", "retrievePaymentLimit",
                "Exception while retrieving the payment limit", AmexLogger.Result.failure, "", e);
            data.setResponseCode(PayerConstants.INTERNAL_SERVER_ERROR_PL);
            data.setResponseDesc(ApiUtil.getErrorDescription(PayerConstants.INTERNAL_SERVER_ERROR_PL));
            response.setStatus(ApiConstants.FAIL);
            response.setData(data);
        }
        responseType.setRetrievePaymentLimitResponse(response);
        logger.info(apiMsgId, "SmartServiceEngine", "PaymentServiceImpl", "processPayment",
            "END of retrievePaymentLimit", AmexLogger.Result.success, "END");
        return responseType;
    }

}
