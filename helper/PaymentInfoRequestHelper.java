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
package com.americanexpress.smartserviceengine.helper;

import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.payve.handler.ext.PaymentManagementServiceExt;
import com.americanexpress.payve.paymentmanagementservice.v1.GetPaymentDetailsFaultMsg;
import com.americanexpress.payve.paymentmanagementservice.v1.IPaymentManagementService;
import com.americanexpress.payve.paymentmanagementservice.v1.getpaymentdetails.FaultType;
import com.americanexpress.payve.paymentmanagementservice.v1.getpaymentdetails.GetPaymentDetailsRespGrpType;
import com.americanexpress.payve.paymentmanagementservice.v1.getpaymentdetails.GetPaymentDetailsType;
import com.americanexpress.payve.paymentmanagementservice.v1.getpaymentdetails.PymntDtlsBlkType;
import com.americanexpress.payve.paymentmanagementservice.v1.getpaymentdetails.RequestType;
import com.americanexpress.payve.paymentmanagementservice.v1.getpaymentdetails.ResponseType;
import com.americanexpress.payve.paymentmanagementservice.v1.getpaymentdetails.StatusType;
import com.americanexpress.payve.serviceheader.v4.TrackingHdrType;
import com.americanexpress.smartserviceengine.client.PaymentInfoServiceClient;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.ApiErrorConstants;
import com.americanexpress.smartserviceengine.common.constants.SchedulerConstants;
import com.americanexpress.smartserviceengine.common.constants.SchedulerErrorConstants;
import com.americanexpress.smartserviceengine.common.exception.SSEApplicationException;
import com.americanexpress.smartserviceengine.common.util.EnvironmentPropertiesUtil;
import com.americanexpress.smartserviceengine.common.util.SOAPOperationsLoggingHandler;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.vo.PayRequestVO;
import com.americanexpress.smartserviceengine.common.vo.PayResponseVO;

/**
 * This class contains methods (soap client) to call the PaymentManagementService BIP
 * API.
 *
 */
@Service
public class PaymentInfoRequestHelper {

	private static AmexLogger logger = AmexLogger.create(PaymentInfoRequestHelper.class);

	@Autowired
	private TivoliMonitoring tivoliMonitoring;

	/**
	 * This method invokes the service call and populates the success response
	 * from service to response beans.
	 *
	 * @param requestWrapper
	 * @param eventId
	 * @return ResponseType object with the Map
	 * @throws GeneralSecurityException
	 * @throws SSEApplicationException
	 */

	public Map<String, Object> getPaymentInfo(PayRequestVO requestWrapper, String eventId)throws GeneralSecurityException, SSEApplicationException {
		Map<String, Object> map = new HashMap<String, Object>();
		logger.info(eventId,"SmartServiceEngine","PaymentInfoRequestHelper","PaymentInfoRequestHelper: getPaymentInfo: starts",
				"Before invoking getPaymentManagementService",AmexLogger.Result.success, "", "BuyerPymtRefId", requestWrapper.getBipPaymentReferenceId(),
				"PymtSubmissionDate", requestWrapper.getPaymentSubmissionDate());
		ResponseType responseType = null;
		map = callGetPaymentInfoService(requestWrapper, eventId);// Calling the GetOrganizationInfo service
		responseType = (ResponseType) map.get(SchedulerConstants.RESPONSE_DETAILS);
		logger.info(eventId, "SmartServiceEngine", "PaymentInfoRequestHelper", "PaymentInfoRequestHelper: getPaymentInfo: ends",
				"After invoking getPaymentManagementService", AmexLogger.Result.success, "", "responseCode", responseType.getStatus().getRespCd());
		map = buildResponse(responseType, requestWrapper, eventId);// set the response attributes to OrgResponseVO object
		return map;
	}

	/**
	 * This method creates request object, calls the soap service.
	 *
	 * @param requestWrapper
	 * @param payvePartnerId
	 * @param eventId
	 * @return map object
	 * @throws GeneralSecurityException
	 * @throws SSEApplicationException
	 */
	private Map<String, Object> callGetPaymentInfoService(PayRequestVO requestWrapper, String eventId) throws GeneralSecurityException, SSEApplicationException {
		logger.info(eventId,"SmartServiceEngine","PaymentInfoRequestHelper","PaymentInfoRequestHelper: callGetPaymentInfoService: starts",
				"Before invoking getPaymentInfo service", AmexLogger.Result.success, "");
		Map<String, Object> map = new HashMap<String, Object>();
		try {
			PaymentManagementServiceExt getPaymentInfoService = new PaymentManagementServiceExt();
			IPaymentManagementService proxy = getPaymentInfoService.getPaymentManagementServicePort();
			BindingProvider bindingProvider = (BindingProvider) proxy;
			bindingProvider.getBinding().getHandlerChain().add(new SOAPOperationsLoggingHandler());
			Map<String, Object> requestContext = bindingProvider.getRequestContext();
			requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,EnvironmentPropertiesUtil.getProperty(SchedulerConstants.BIP_PAY_SOAP_SERVICE_URL));
			requestContext.put(ApiConstants.REQUEST_TIMEOUT, EnvironmentPropertiesUtil.getProperty(ApiConstants.BIP_REQUEST_TIMEOUT_VALUE));
			logger.debug(eventId,"SmartServiceEngine","PaymentInfoRequestHelper", "PaymentInfoRequestHelper: callGetPaymentInfoService",
					"Calling getPaymentInfo BIP service",AmexLogger.Result.success, "","BIP SOAP Endpoint URL",
					EnvironmentPropertiesUtil.getProperty(SchedulerConstants.BIP_PAY_SOAP_SERVICE_URL));
			PaymentInfoServiceClient client = new PaymentInfoServiceClient();

			//Tracking Header Section
			TrackingHdrType trackingHdrType = client.buildTrackingHdrType(eventId);
			final Holder<TrackingHdrType> trackingHolder = new Holder<TrackingHdrType>(trackingHdrType);

			//Build Request Section
			RequestType reqType = client.buildRequestType(requestWrapper);
			GetPaymentDetailsType getPaymentDetailsType = client.buildGetPaymentDetailsType(reqType);
			final Holder<GetPaymentDetailsType> requestHolder = new Holder<GetPaymentDetailsType>(getPaymentDetailsType);

			//build Response object
			ResponseType response = client.buildResponseType();
			final Holder<ResponseType> responseHolder = new Holder<ResponseType>(response);

			proxy.getPaymentDetails(reqType, requestHolder, responseHolder, trackingHolder);// Calling getPaymentDetails SOAP service
			ResponseType respType = responseHolder.value;
			if (null == respType) {
				logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.GET_PYMT_DTLS_ERR_CD, TivoliMonitoring.GET_PYMT_DTLS_ERR_MSG, eventId));
				logger.error(eventId,"SmartServiceEngine","PaymentInfoRequestHelper","PaymentInfoRequestHelper: callGetPaymentInfoService",
						"After invoking getPaymentInfoService ",AmexLogger.Result.failure,"Null response from getPaymentInfoService", "");
				throw new SSEApplicationException(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD,EnvironmentPropertiesUtil.getProperty(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD));
			}
			logger.debug(eventId,"SmartServiceEngine","PaymentInfoRequestHelper","PaymentInfoRequestHelper: callGetPaymentInfoService",
					"After calling getPaymentInfoService BIP service",AmexLogger.Result.success, "", "responseCode: ",respType.getStatus().getRespCd());
			map.put(SchedulerConstants.RESPONSE_DETAILS, respType);
		} catch (GetPaymentDetailsFaultMsg ex) {
			FaultType faultType = ex.getFaultInfo().getFault();
			String faultDetail = faultType.getFaultDetail();
			String faultCode = faultType.getFaultCode();
			String sseRespCode = null;
			String sseRespDesc = null;

			if (faultCode != null) {// Error response from AddOrgInfo SOAP API
				sseRespCode = EnvironmentPropertiesUtil.getProperty(faultCode);//Get the corresponding SSE response code and response desc from Properties files
				if (sseRespCode != null) {
					sseRespDesc = EnvironmentPropertiesUtil.getProperty(sseRespCode);
				} else {
					if (faultDetail.indexOf(" ") > 0) {//Get the response description from FaultDetail element in soap fault
						faultDetail = faultDetail.substring(faultDetail.indexOf(" ") + 1);
					}
					sseRespCode = SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD;
					sseRespDesc = EnvironmentPropertiesUtil.getProperty(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD);
					if(faultCode.equals("07000")) {//If "Datapower Internal Server Error" is returned, raise Tivoli alert.
						logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.GET_PYMT_DTLS_ERR_CD, TivoliMonitoring.GET_PYMT_DTLS_ERR_MSG, eventId));
					}
				}
			} else {
				sseRespCode = SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD;
				sseRespDesc = EnvironmentPropertiesUtil.getProperty(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD);
				logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.GET_PYMT_DTLS_ERR_CD, TivoliMonitoring.GET_PYMT_DTLS_ERR_MSG, eventId));
			}
			logger.error(eventId,"SmartServiceEngine","PaymentInfoRequestHelper","PaymentInfoRequestHelper: callGetPaymentInfoService",
					"SOAP Fault Error occured during getPaymentInfo service call",AmexLogger.Result.failure, faultDetail, ex, "fault-Actor",
					faultType.getFaultActor(), "fault-Code", faultCode,"fault-String", faultType.getFaultString(), "fault-Detail",
					faultDetail, "SSEResponseCode", sseRespCode,"SSEResponseDesc", sseRespDesc);
			throw new SSEApplicationException(sseRespCode, sseRespDesc, ex);
		} catch (SSEApplicationException ex) {
			throw ex;
		} catch (WebServiceException exception) {
			
			logger.error(eventId,"SmartServiceEngine","PaymentInfoRequestHelper","PaymentInfoRequestHelper: callGetPaymentInfoService",
					"Exception occured during getPaymentInfo service call",AmexLogger.Result.failure, exception.getMessage(), exception);
			if (exception.getMessage().contains("java.net.SocketTimeoutException")) {
				logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.BIP_TIMEOUT_ERR_CD, TivoliMonitoring.BIP_TIMEOUT_ERR_MSG, eventId));
				throw new SSEApplicationException("Exception occured during Payment Info service call",ApiErrorConstants.BIP_TIMEOUT_SSEAPIEN901,
						EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.BIP_TIMEOUT_SSEAPIEN901),exception);
			} else {
				logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.GET_PYMT_DTLS_ERR_CD, TivoliMonitoring.GET_PYMT_DTLS_ERR_MSG, eventId));
				throw new SSEApplicationException("Exception occured during Payment Info service call",
						ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD,EnvironmentPropertiesUtil.getProperty(ApiErrorConstants.ENROLL_INTERNAL_SERVER_ERR_CD),exception);
			}
		} catch (Exception ex) {
			logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.GET_PYMT_DTLS_ERR_CD, TivoliMonitoring.GET_PYMT_DTLS_ERR_MSG, eventId));
			logger.error(eventId,"SmartServiceEngine","PaymentInfoRequestHelper","PaymentInfoRequestHelper: callGetPaymentInfoService",
					"Exception occured during getPaymentInfo service call",AmexLogger.Result.failure, ex.getMessage(), ex);
			throw new SSEApplicationException(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD,EnvironmentPropertiesUtil.getProperty(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD), ex);
		}
		logger.info(eventId,"SmartServiceEngine","PaymentInfoRequestHelper","PaymentInfoRequestHelper: callGetPaymentInfoService: ends",
				"After invoking getPaymentInfo service",AmexLogger.Result.success, "");
		return map;
	}

	/**
	 * Utility method to populate the java beans from service response
	 *
	 * @param responseType
	 * @param requestWrapper
	 * @param eventId
	 * @return Map with response bean in it.
	 * @throws SSEApplicationException
	 */
	private Map<String, Object> buildResponse(ResponseType responseType,PayRequestVO requestWrapper, String eventId) throws SSEApplicationException {
		Map<String, Object> map = new HashMap<String, Object>();
		try {
			PayResponseVO response = new PayResponseVO();
			if (null != responseType) {
				StatusType statusType = responseType.getStatus();
				String responseCode = statusType.getRespCd();
				String responseDesc = statusType.getRespDesc();
				String explCd = statusType.getExplCd();
				String explDesc = statusType.getExplDesc();
				logger.debug(eventId,"SmartServiceEngine","PaymentInfoRequestHelper","PaymentInfoRequestHelper: buildResponse",
						"Response from Payment Inquiry service",AmexLogger.Result.success, "","responseCode", responseCode,
						"responseDescription", responseDesc, "explanationCode", explCd, "explanationDesc", explDesc );
				//TODO need to confirm this param
				response.setPaymentSystemResponseCode(responseCode);
				response.setPaymentSystemResponseDesc(responseDesc);
				response.setPaymentSystemExplCd(explCd);
				response.setPaymentSystemExplDesc(explDesc);

				if(SchedulerConstants.SUCCESS_RESP_CD.equalsIgnoreCase(responseCode)){
					map.put(SchedulerConstants.ERROR_RESPONSE_FLAG, SchedulerConstants.FALSE);

					if(null != responseType.getGetPaymentDetailsRespGrp() && null != responseType.getGetPaymentDetailsRespGrp().getPymtDtlsBlk()){
						GetPaymentDetailsRespGrpType getPaymentDetailsRespGrpType = responseType.getGetPaymentDetailsRespGrp();
						response.setBuyerPaymentRefId(getPaymentDetailsRespGrpType.getPymtDtlsBlk().getBuyPymtRefId());
						response.setBipOrganizationId(getPaymentDetailsRespGrpType.getBuyId());

						PymntDtlsBlkType pymntDtlsBlkType = getPaymentDetailsRespGrpType.getPymtDtlsBlk();
						response.setBuyerPaymentStatus(pymntDtlsBlkType.getBuyPymtSta());

						response.setPymtStatDt(getPaymentDetailsRespGrpType.getPymtDtlsBlk().getPymtStatDt());
						response.setCheckNbr(getPaymentDetailsRespGrpType.getPymtDtlsBlk().getCheckNbr());
						response.setPymtAmt(getPaymentDetailsRespGrpType.getPymtDtlsBlk().getPymtAmt());
						response.setPymtMthd(getPaymentDetailsRespGrpType.getPymtDtlsBlk().getPymtMthd());
					}else{ //GetPaymentDetailsRespGrp is null
						logger.error(eventId,"SmartServiceEngine","PaymentInfoRequestHelper","PaymentInfoRequestHelper: buildResponse",
								"GetPaymentDetailsRespGrp is null",AmexLogger.Result.failure, "");
					}
				}else{ //Failure Response code from Payve
					logger.error(eventId,"SmartServiceEngine","PaymentInfoRequestHelper","PaymentInfoRequestHelper: buildResponse",
							"Failure Response code from Payment Details Service",AmexLogger.Result.failure, "","responseCode", responseCode,
							"responseMessage", responseDesc, "Expl_Code", explCd, "Expl_Desc", explDesc);
					// Error response from getPaymentInfo SOAP API
					map.put(SchedulerConstants.ERROR_RESPONSE_FLAG, SchedulerConstants.TRUE);
				}
			}else{ //responseType is null
				logger.error(eventId,"SmartServiceEngine","PaymentInfoRequestHelper","PaymentInfoRequestHelper: buildResponse",
						"responseType object is null", AmexLogger.Result.failure, "");
			}
			map.put(SchedulerConstants.RESPONSE_DETAILS, response);
		} catch (Exception ex) {
			logger.error(eventId,"SmartServiceEngine","PaymentInfoRequestHelper","PaymentInfoRequestHelper: buildResponse",
					"Exception occured during getPaymentInfo service call",AmexLogger.Result.failure, ex.getMessage(), ex);
			logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SCHED_ERR_CD, TivoliMonitoring.SSE_SCHED_ERR_MSG, eventId));
			throw new SSEApplicationException(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD,EnvironmentPropertiesUtil.getProperty(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD),ex);
		}
		return map;
	}
}
