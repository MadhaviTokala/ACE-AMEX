package com.americanexpress.smartserviceengine.service.impl;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.SchedulerConstants;
import com.americanexpress.smartserviceengine.common.payload.SearchBankRequest;
import com.americanexpress.smartserviceengine.common.payload.SearchBankRequestData;
import com.americanexpress.smartserviceengine.common.payload.SearchBankRequestType;
import com.americanexpress.smartserviceengine.common.payload.SearchBankResponseType;
import com.americanexpress.smartserviceengine.common.util.ApiUtil;
import com.americanexpress.smartserviceengine.common.util.EnvironmentPropertiesUtil;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.vo.AccountDetailsVO;
import com.americanexpress.smartserviceengine.dao.FetchBankDetailsDAO;
import com.americanexpress.smartserviceengine.dao.UpdateBankNameDAO;
import com.americanexpress.smartserviceengine.service.UpdateBankNameService;

public class UpdateBankNameServiceImpl implements UpdateBankNameService {
	
	private static  AmexLogger logger = AmexLogger.create(UpdateBankNameServiceImpl.class);
	
	@Autowired
	private TivoliMonitoring tivoliMonitoring;
	
    @Resource
	 @Qualifier("fetchBankDetailsDAO")
	 private FetchBankDetailsDAO fetchBankDetailsDAO;
	 
    @Resource
	 @Qualifier("updateBankNameDAO")
	 private UpdateBankNameDAO updateBankNameDAO;
     
    private String PROXY_ENV;
    private String PROXY_HOST;
    private int PROXY_PORT;

	@SuppressWarnings("unchecked")
	@Override
	public void updateBankName(String eventId) {
		logger.info(eventId, "SmartServiceEngine", "UpdateBankNameServiceImpl", "updateBankName", "Start of  updateBankName", AmexLogger.Result.success, "Start");
		  PROXY_ENV=EnvironmentPropertiesUtil.getProperty(ApiConstants.PROXY_ENV);
		try{
        HashMap<String, Object> inMap = new HashMap<String, Object>();
        Map<String, Object> outMap = fetchBankDetailsDAO.execute(inMap, eventId);
        if(outMap!=null){
        	String responseCode = StringUtils.stripToEmpty((String)outMap.get(ApiConstants.SP_RESP_CD));
        	String sqlCode = StringUtils.stripToEmpty((String)outMap.get(ApiConstants.SQLCODE_PARM));
        	String responseDesc = StringUtils.stripToEmpty((String)outMap.get(ApiConstants.SP_RESP_MSG));
        	 if (ApiConstants.SP_SUCCESS_SSEIN000.equals(responseCode)) {
                 List<AccountDetailsVO> bankList =  (List<AccountDetailsVO>) outMap.get(SchedulerConstants.RESULT_SET);
                 
                 if(bankList!=null && !bankList.isEmpty()){
                	 logger.info(eventId, "SmartServiceEngine", "Update Bank name Service",
                             "UpdateBankNameServiceImpl : updateBankName", "Start of update bank name Service",
                             AmexLogger.Result.success, "", "Number of records for bankList Cursor ", String.valueOf(bankList.size()));

                	 for(AccountDetailsVO accountDetailsVO:bankList){
                		String transId= UUID.randomUUID().toString();
            			SearchBankRequestType requestType=setValuestoBean(accountDetailsVO);
            			HttpHeaders headers = new HttpHeaders();
            			 headers.set(ApiConstants.CONTENT_TYPE_REST, ApiConstants.CONTENT_TYPE_APP_JSON);
            	            headers.add(ApiConstants.AMEX_REQUEST_ID, eventId);
        				RestTemplate restTemplate = new RestTemplate();
            			HttpEntity<String> entity = new HttpEntity<String>(ApiUtil.jsonToString(requestType),headers);
        				 logger.info(eventId, "SmartServiceEngine", "Update Bank name Service",
                                 "UpdateBankNameServiceImpl : updateBankName", "Invoke Search Bank API",
                                 AmexLogger.Result.success, "","Search Bank Service URL",
                                 EnvironmentPropertiesUtil.getProperty(ApiConstants.ACE_SEARCH_BANK_URL),
                                 "Search Bank Request", ApiUtil.pojoToJSONString(requestType),"Unique transId",transId);
        				if(PROXY_ENV.equals(ApiConstants.TRUE)){
     		               PROXY_HOST=EnvironmentPropertiesUtil.getProperty(ApiConstants.PROXY_HOST);
     		               PROXY_PORT=Integer.parseInt(EnvironmentPropertiesUtil.getProperty(ApiConstants.PROXY_PORT));

     		                SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
     		                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(PROXY_HOST, PROXY_PORT));
     		                clientHttpRequestFactory.setProxy(proxy);
     		                restTemplate.setRequestFactory(clientHttpRequestFactory);
     		        }
        			    ResponseEntity<SearchBankResponseType> result =
        		                restTemplate.exchange(EnvironmentPropertiesUtil.getProperty(ApiConstants.ACE_SEARCH_BANK_URL),
        		                    HttpMethod.POST, entity, SearchBankResponseType.class);
        			    if(result!=null && result.getBody()!=null ){
        			    	 logger.info(eventId, "SmartServiceEngine", "Update Bank name Service",
                                     "UpdateBankNameServiceImpl : updateBankName", "End of Search Bank API",
                                     AmexLogger.Result.success, "", "Search Bank Response", ApiUtil.pojoToJSONString(result.getBody()),"Unique transId",transId);
        					SearchBankResponseType response=result.getBody();	
            				if(response.getSearchBankResponse()!=null && ApiConstants.SUCCESS.equalsIgnoreCase(response.getSearchBankResponse().getStatus()) &&
            						response.getSearchBankResponse().getData()!=null && response.getSearchBankResponse().getData().getBankName()!=null){
            					HashMap<String, Object> updateInMap = new HashMap<String, Object>();
            					updateInMap.put(ApiConstants.IN_ACCT_ID, accountDetailsVO.getPartnerAccountId());
            					updateInMap.put(ApiConstants.IN_ROUTING_NO, accountDetailsVO.getRoutingNumber());
            					updateInMap.put(ApiConstants.IN_BANK_NM, response.getSearchBankResponse().getData().getBankName());
            					HashMap<String,Object> updateOutMap=(HashMap<String, Object>) updateBankNameDAO.execute(updateInMap, eventId);
            					if(updateOutMap!=null && !updateOutMap.isEmpty() && 
            							ApiConstants.SP_SUCCESS_SSEIN000.equals(StringUtils.strip((String) updateOutMap.get(ApiConstants.SP_RESP_CD)))){
            						
            						 logger.info(eventId, "SmartServiceEngine", "Update Bank name Service",
                                             "UpdateBankNameServiceImpl : updateBankName", "E3GMR164 sp update succesfull",
                                             AmexLogger.Result.success, "", "outMap", updateOutMap.toString(),"Unique transId",transId);
            					}else{
            		            	 logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_164_ERR_CD, TivoliMonitoring.SSE_SP_164_ERR_CD, eventId));
            		            	 logger.info(eventId, "SmartServiceEngine", "Update Bank name Service",
                                             "UpdateBankNameServiceImpl : updateBankName", "E3GMR164 sp update not succesfull",
                                             AmexLogger.Result.failure, "","Unique transId",transId);
            						
            					}
            				}else{
            					logger.info(eventId, "SmartServiceEngine", "Update Bank name Service",
                                        "UpdateBankNameServiceImpl : updateBankName", "Search Bank details service call failed or Bank Name is empty",
                                        AmexLogger.Result.failure, "","Unique transId",transId);              					
            				}
             			}else{
             				logger.info(eventId, "SmartServiceEngine", "Update Bank name Service",
                                    "UpdateBankNameServiceImpl : updateBankName", "Search Bank details service call failed",
                                    AmexLogger.Result.failure, "","Unique transId",transId);   
             			}
                	 }
                 }else{
                	 logger.info(eventId, "SmartServiceEngine", "Update Bank name Service", "UpdateBankNameServiceImpl : updateBankName", 
                			 "Start of update bank name Service", AmexLogger.Result.success, "", "Bank list Resultset is Empty ", String.valueOf(bankList.size()));
                 }        		 
        	 }else{
            	 logger.error(eventId, "SmartServiceEngine", "Update Bank name Serivce", "UpdateBankNameServiceImpl - updateBankName", " SP E3GMR164 was NOT successful",
                         AmexLogger.Result.failure, "", ApiConstants.SQLCODE_PARM, sqlCode, "resp_code", responseCode, "resp_msg", responseDesc);
        	 }
        }else{
       	 logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_E3GACA10_ERR_CD, TivoliMonitoring.SSE_SP_E3GACA10_ERR_CD, eventId));

        	logger.error(eventId, "SmartServiceEngine", "Update Bank name Service", "UpdateBankNameServiceImpl : updateBankName",
        			 "Fetch bank details SP E3GMR151", AmexLogger.Result.failure, "outMap is null or error resp_code from SP");
        }
		}catch(Exception e){
			
			logger.error(eventId, "SmartServiceEngine", "Search Bank name Service", "UpdateBankNameServiceImpl : updateBankName", 
					"Search Bank details service call", AmexLogger.Result.failure, "Search Bank details service failed");
		}
	}
	
	private SearchBankRequestType setValuestoBean(AccountDetailsVO accountDetailsVO) {
		SearchBankRequestType requestType=new  SearchBankRequestType();
		SearchBankRequest searchBankRequest=new SearchBankRequest();
		SearchBankRequestData requestData=new  SearchBankRequestData();
		
	//	requestData.setMasterClientId(EnvironmentPropertiesUtil.getProperty(ApiConstants.MASTER_CLIENT_ID));
	//	requestData.setClientId(EnvironmentPropertiesUtil.getProperty(ApiConstants.MASTER_CLIENT_ID));
		requestData.setBankCountry(ApiConstants.COUNTRY_CD);
		requestData.setRoutingNumber(accountDetailsVO.getRoutingNumber());
		requestData.setSearchBy("1");
		searchBankRequest.setData(requestData);
		requestType.setSearchBankRequest(searchBankRequest);
		return requestType;
	}
	

}
