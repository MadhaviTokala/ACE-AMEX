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
import com.americanexpress.smartserviceengine.common.vo.AccRequestVO;
import com.americanexpress.smartserviceengine.common.vo.AccResponseVO;
import com.americanexpress.smartserviceengine.dao.AccountDAO;
import com.americanexpress.smartserviceengine.dao.UpdateAccountStatusDAO;
import com.americanexpress.smartserviceengine.helper.GetAccountInfoRequestHelper;
import com.americanexpress.smartserviceengine.service.AccountStatusService;

/**
 * @author vishwakumar_c
 *
 */
public class AccountStatusServiceImpl implements AccountStatusService {

	@Autowired
	private AccountDAO accountDAO;
	@Autowired
	private UpdateAccountStatusDAO updateAccountStatusDAO;
	@Autowired
	private TivoliMonitoring tivoliMonitoring;
	@Autowired
	private GetAccountInfoRequestHelper accInfoRequestHelper;

	@Override
	@SuppressWarnings("unchecked")
	public void getAccountData(String eventId) throws SSEApplicationException{

		AmexLogger logger = AmexLogger.create(AccountStatusServiceImpl.class);

		try{

			logger.info(eventId, "SmartServiceEngine", "AccountStatusServiceImpl", "getAccountData",
					"Start of  AccountStatusService", AmexLogger.Result.success, "");

			List<AccRequestVO> accDataResultSet=null;
			Map<String, String> inMap  = new HashMap<String, String>();

			logger.debug(eventId, "SmartServiceEngine", "AccountStatusServiceImpl", "getAccountData",
					"Start of Get Account Details SP E3GMR022", AmexLogger.Result.success, "");

			Map<String, Object> outMap = accountDAO.execute((HashMap) inMap,eventId);

            if (outMap != null && !outMap.isEmpty() && outMap
                    .get(SchedulerConstants.RESP_CD) != null && SchedulerConstants.RESPONSE_CODE_SUCCESS.equalsIgnoreCase(outMap
                        .get(SchedulerConstants.RESP_CD).toString().trim())) {
				accDataResultSet = (List<AccRequestVO>) outMap.get(SchedulerConstants.RESULT_SET);
				String resultCode = outMap.get("RESP_CD").toString().trim();
				String resultMessage = outMap.get("RESP_MSG").toString().trim();
				String resultParam = outMap.get("SQLCODE_PARM").toString().trim();

				logger.debug(eventId, "SmartServiceEngine", "AccountStatusServiceImpl", "getAccountData",
						"End of Get Account Details SP E3GMR022", AmexLogger.Result.success, "Success Response from SP",SchedulerConstants.RESP_CD, resultCode,SchedulerConstants.RESP_MSG,
						resultMessage,SchedulerConstants.SQLCODE_PARM,resultParam);
			}else {
				logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_022_ERR_CD, TivoliMonitoring.SSE_SP_022_ERR_MSG, eventId));
				logger.debug(eventId, "SmartServiceEngine", "AccountStatusServiceImpl", "getAccountData",
						"End of Get Account Details SP E3GMR022", AmexLogger.Result.failure, "outMap is null or error resp_code from SP");
			}

			if(accDataResultSet != null && !accDataResultSet.isEmpty() ){
				logger.debug(eventId, "SmartServiceEngine", "AccountStatusServiceImpl", "getAccountData",
						"Check resultset map for SP E3GMR022", AmexLogger.Result.success, "",
						"Acct ResultSet Size:", new Integer( accDataResultSet.size()).toString(),
						"Acct ResultSet:",
						accDataResultSet.toString());

				Iterator<AccRequestVO> iterator = accDataResultSet.iterator();



				while(iterator.hasNext()){
					AccRequestVO accDataDetails = iterator.next();
					logger.debug(eventId, "SmartServiceEngine", "AccountStatusServiceImpl", "getAccountData",
							"Calling Account Status Service - invoke BIP API", AmexLogger.Result.success, "",
							"organizationId",accDataDetails.getOrganizationId(),
							"partnerAccountId", accDataDetails.getAccountId());

					       logger.info(eventId, "SmartServiceEngine", "AccountStatusServiceImpl", "getAccountData",
		                                        "Logging for splunk checking Account status", AmexLogger.Result.success, "",
		                                        "partnerId", accDataDetails.getPartnerId(),
		                                        "organizationId",accDataDetails.getOrganizationId(),
		                                        "partnerName",accDataDetails.getPartnername(),
		                                        "paymentEntityId",accDataDetails.getPaymentEntityId(),
		                                        "accountId",accDataDetails.getAccountId(),
		                                        "paymentInd",accDataDetails.getPaymentInd() );


					try {

						Map<String, Object> statusMap  = new HashMap<String, Object>();
						statusMap =	accInfoRequestHelper.getAccountInfo(accDataDetails, eventId);
						AccResponseVO response = (AccResponseVO) statusMap.get(SchedulerConstants.RESPONSE_DETAILS);
						Map<String,  Object> inResMap = new HashMap<String,  Object>();
						inResMap.put(SchedulerConstants.IN_PRTR_ACCT_ID, response.getPartnerAccId());
						inResMap.put(SchedulerConstants.IN_PYMT_SYS_ACCT_ID, response.getPayveAcctId());
						inResMap.put(SchedulerConstants.IN_PYMT_SYS_STA_CD, response.getAccStatusCd());
						inResMap.put(SchedulerConstants.IN_PYMT_SYS_STA_DS, response.getAccStatusDesc());
						inResMap.put(SchedulerConstants.IN_PYMT_SYS_RESP_CD, response.getAccRespCd());
						inResMap.put(SchedulerConstants.IN_PYMT_SYS_RESP_DS,response.getAccRespDesc());

						Map<String, Object> outResMap=updateAccountStatusDAO.execute(inResMap,eventId);
						logger.debug(eventId, "SmartServiceEngine", "AccountStatusServiceImpl", "getAccountData",
								"Start of Calling updateAccountDAO", AmexLogger.Result.success, "");

						String respCode = ((String) outResMap.get(SchedulerConstants.RESP_CD));

						if (outResMap != null && !outResMap.isEmpty() &&
								SchedulerConstants.SUC_SSEIN000.equalsIgnoreCase(((String) outResMap.get(SchedulerConstants.RESP_CD)).trim())) {

							logger.info(
									eventId,"SmartServiceEngine", "AccountStatusServiceImpl",
									"getAccountData","UpdateAccountDAO - SP E3GMR024 is Successful",	AmexLogger.Result.success, "",
									SchedulerConstants.SQLCODE_PARM,outMap.get(SchedulerConstants.SQLCODE_PARM).toString(),
									"resp_code", respCode, "resp_msg",	outMap.get(SchedulerConstants.RESP_MSG).toString());

							       logger.info(eventId, "SmartServiceEngine", "AccountStatusServiceImpl", "getAccountData",
		                                                        "Logging for splunk Updatating Account status to DB", AmexLogger.Result.success, "",
		                                                        "partnerAccId", response.getPartnerAccId(),
		                                                        "payveAcctId",response.getPayveAcctId(),
		                                                        "accStatusCd",response.getAccStatusCd(),
		                                                        "accStatusDesc",response.getAccStatusDesc(),
		                                                        "accRespCd",response.getAccRespCd(),
		                                                        "accRespDesc",response.getAccRespDesc(),
		                                                        "paymentMethod",response.getPaymentMethod(),
		                                                        "organizationId",accDataDetails.getOrganizationId(),
		                                                        "partnerName",accDataDetails.getPartnername(),
		                                                        "paymentEntityId",accDataDetails.getPaymentEntityId(),
		                                                        "accountId",accDataDetails.getAccountId());


						}

						else if(outResMap != null && !outResMap.isEmpty() &&
								((SchedulerErrorConstants.RNF_SSEIN100).equalsIgnoreCase(((String) outResMap.get(SchedulerConstants.RESP_CD)).trim()))
								||	(SchedulerErrorConstants.RNF_SSEIN200).equalsIgnoreCase(((String) outResMap.get(SchedulerConstants.RESP_CD)).trim()))
						{
							logger.info(
									eventId,"SmartServiceEngine", "AccountStatusServiceImpl",
									"getAccountData","UpdateAccountDAO -SP E3GMR024 is not Successful",	AmexLogger.Result.failure, "RECORD NOT FOUND",
									SchedulerConstants.SQLCODE_PARM,
									outMap.get(SchedulerConstants.SQLCODE_PARM).toString(),
									"resp_code", respCode, "resp_msg",
									outMap.get(SchedulerConstants.RESP_MSG).toString());
						}
						else {
							// Update Organization SP execution failed
							logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_024_ERR_CD, TivoliMonitoring.SSE_SP_024_ERR_MSG, eventId));
							logger.error(
									eventId,"SmartServiceEngine", "AccountStatusServiceImpl",
									"getAccountData","UpdateAccountDAO -SP E3GMR024 is not Successful",	AmexLogger.Result.failure, "EXCEPTION OCCURRED WITH CODE",
									SchedulerConstants.SQLCODE_PARM,
									outMap.get(SchedulerConstants.SQLCODE_PARM).toString(),
									"resp_code", respCode, "resp_msg",
									outMap.get(SchedulerConstants.RESP_MSG).toString());
						}

						logger.debug(eventId, "SmartServiceEngine", "AccountStatusServiceImpl", "getAccountData",
								"End of Calling updateaccountDAO", AmexLogger.Result.success, "");
					}
					catch (SSEApplicationException ex) {
						String responseCode = ex.getResponseCode();
						logger.error(eventId, "SmartServiceEngine", "AccountStatusServiceImpl", "getAccountData",
								"Exception while Calling Account Status Service - invoke BIP API", AmexLogger.Result.failure,
								"Failed to execute getAccountData",
								"responseCode", responseCode, "responseDesc", ex.getResponseDescription());
					}catch(Exception e){
						logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SCHED_ERR_CD, TivoliMonitoring.SSE_SCHED_ERR_MSG, eventId));
						logger.error(eventId, "SmartServiceEngine", "AccountStatusServiceImpl", "getAccountData",
								"Exception while Calling Account Status Service - invoke BIP API",
								AmexLogger.Result.failure, "Failed to execute getAccountData",e,
								"ErrorMsg:", e.getMessage());
					}
				}
			}
			else
			{
				logger.debug(eventId, "SmartServiceEngine", "AccountStatusServiceImpl", "getAccountData",
						"Check resultset map for SP E3GMR022", AmexLogger.Result.failure, "Result set is null");
			}
			logger.info(eventId, "SmartServiceEngine", "AccountStatusServiceImpl", "getAccountData",
					"End of  AccountStatusService", AmexLogger.Result.success, "");
		}
		catch(Exception e){
			logger.error(eventId, "SmartServiceEngine", "AccountStatusServiceImpl", "getAccountData",
					"Exception while processing Account Status Service call", AmexLogger.Result.failure, "Failed to execute getAccountData",e, "ErrorMsg:", e.getMessage());

			throw new SSEApplicationException(
					SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD,
					EnvironmentPropertiesUtil
					.getProperty(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD), e);
		}
	}

}

