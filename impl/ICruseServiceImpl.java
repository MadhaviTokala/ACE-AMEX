package com.americanexpress.smartserviceengine.service.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.common.constants.SchedulerConstants;
import com.americanexpress.smartserviceengine.common.constants.SchedulerErrorConstants;
import com.americanexpress.smartserviceengine.common.exception.SSEApplicationException;
import com.americanexpress.smartserviceengine.common.payload.ABOInfoListType;
import com.americanexpress.smartserviceengine.common.payload.AddressDetailsListType;
import com.americanexpress.smartserviceengine.common.payload.CCFVDBANameType;
import com.americanexpress.smartserviceengine.common.payload.CCFVFIDListType;
import com.americanexpress.smartserviceengine.common.payload.CompanyInfoType;
import com.americanexpress.smartserviceengine.common.payload.ICruseRequestType;
import com.americanexpress.smartserviceengine.common.payload.ICruseResponseType;
import com.americanexpress.smartserviceengine.common.payload.IDDetailsListType;
import com.americanexpress.smartserviceengine.common.payload.PAInfoType;
import com.americanexpress.smartserviceengine.common.payload.PhoneDetailsListType;
import com.americanexpress.smartserviceengine.common.payload.UBOInfoType;
import com.americanexpress.smartserviceengine.common.util.EnvironmentPropertiesUtil;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.vo.ICruseAboVO;
import com.americanexpress.smartserviceengine.common.vo.ICruseBOVO;
import com.americanexpress.smartserviceengine.common.vo.ICruseOrgVO;
import com.americanexpress.smartserviceengine.common.vo.ICrusePAVO;
import com.americanexpress.smartserviceengine.dao.ICruseFetchDataDAO;
import com.americanexpress.smartserviceengine.dao.UpdateKYCStatusDAO;
import com.americanexpress.smartserviceengine.service.ICruseService;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class ICruseServiceImpl implements ICruseService {

	 AmexLogger logger = AmexLogger.create(ICruseServiceImpl.class);

    @Resource
    @Qualifier("iCruseFetchDataDAO")
    private ICruseFetchDataDAO iCruseFetchDataDAO;

    @Resource
    @Qualifier("updateKYCStatusDAO")
    private UpdateKYCStatusDAO updateKYCStatusDAO;

    @Resource
    private TivoliMonitoring tivoliMonitoring;

    @Override
	@SuppressWarnings("unchecked")
    public void fetchRecords(String eventId) throws SSEApplicationException {

    	 int orgSize=0;
    	 int aboSize=0;
    	 int uboSize=0;
    	 int orgPaSize=0;
    	 int accPaSize=0;
    	 int aboCount=0;
    	 int uboCount=0;
    	 int orgPaCount=0;
    	 int accPaCount=0;
        try {
            logger.info(eventId, "SmartServiceEngine", "ICruse Scheduler Service", "ICruseServiceImpl : fetchRecords",
                "Start of  ICruse Scheduler Service", AmexLogger.Result.success, "");

            Map<String, Object> inMap = new HashMap<String, Object>();
            Map<String, Object> outMap = iCruseFetchDataDAO.execute(inMap, eventId);

            if(outMap!=null && !outMap.isEmpty()){
	            List<ICruseOrgVO> orgList = (List<ICruseOrgVO>) outMap.get(SchedulerConstants.RESULTSET1);
	            logger.info(eventId, "SmartServiceEngine", "ICruse Scheduler Service", "ICruseServiceImpl : fetchRecords","Start of  ICruse Scheduler Service",
	            		AmexLogger.Result.success,"", "Org Cursor Size",""+orgList.size());
	            List<ICruseAboVO> aboList = (List<ICruseAboVO>) outMap.get(SchedulerConstants.RESULTSET2);
	            logger.info(eventId, "SmartServiceEngine", "ICruse Scheduler Service", "ICruseServiceImpl : fetchRecords",
		                "Start of  ICruse Scheduler Service", AmexLogger.Result.success,"", "ABO cursor Size", ""+aboList.size());
	            List<ICruseBOVO> boList = (List<ICruseBOVO>) outMap.get(SchedulerConstants.RESULTSET3);
	            logger.info(eventId, "SmartServiceEngine", "ICruse Scheduler Service", "ICruseServiceImpl : fetchRecords",
		                "Start of  ICruse Scheduler Service", AmexLogger.Result.success,"", "BO cursor Size",""+boList.size());
	            List<ICrusePAVO> orgAdminList = (List<ICrusePAVO>) outMap.get(SchedulerConstants.RESULTSET4);
	            logger.info(eventId, "SmartServiceEngine", "ICruse Scheduler Service", "ICruseServiceImpl : fetchRecords",
		                "Start of  ICruse Scheduler Service", AmexLogger.Result.success,"","Org Admin cursor Size",""+orgAdminList.size());
	            List<ICrusePAVO> accountAdminList = (List<ICrusePAVO>) outMap.get(SchedulerConstants.RESULTSET5);
	            logger.info(eventId, "SmartServiceEngine", "ICruse Scheduler Service", "ICruseServiceImpl : fetchRecords",
		                "Start of  ICruse Scheduler Service", AmexLogger.Result.success,"", "Account Admin cursor Size", ""+accountAdminList.size());
	            logger.info(eventId, "SmartServiceEngine", "ICruse Scheduler Service", "ICruseServiceImpl : Create Requests",
		                "Start of creating request for each org to iCruse", AmexLogger.Result.success,"Start" );

	           	 while(orgSize<orgList.size()){
	           		Map<String,Object> updateMap=new HashMap<String,Object>();
	           		ICruseOrgVO iCruseOrgVO=orgList.get(orgSize);
	           		if(verifyStatusCodes(iCruseOrgVO.getOfacCheckStatusCode()) || verifyStatusCodes(iCruseOrgVO.getGndbCheckStatusCode()) || verifyStatusCodes(iCruseOrgVO.getLexisNexisCheckStatusCode())){
	           			ICruseRequestType request=buildOrgRequest(iCruseOrgVO);
		        		updateMap.put(StringUtils.stripToEmpty(iCruseOrgVO.getSseOrgId()),iCruseOrgVO);
		        		ICruseResponseType response=initiateICruseCall(request,eventId);
		        		if(response!=null){
		        			updateCompanyStatus(response,updateMap,eventId);
		        		}
	           		}else{
	           			logger.info(eventId, "SmartServiceEngine", "ICruse Scheduler Service", "ICruseServiceImpl : Create Requests",
	    		                "Status codes are not YS or NS for Org", AmexLogger.Result.success,"","org id",iCruseOrgVO.getSseOrgId());
	           		}
	           		orgSize++;
	           	}
	           	logger.info(eventId, "SmartServiceEngine", "ICruse Scheduler Service", "ICruseServiceImpl : Create Requests",
		                "End of creating request for each org to iCruse and updating in KYC table", AmexLogger.Result.success,"End" );

	           	logger.info(eventId, "SmartServiceEngine", "ICruse Scheduler Service", "ICruseServiceImpl : Create Requests",
		                "Start of creating request for ABO, BO and PA to iCruse", AmexLogger.Result.success,"Start" );
	            while(aboCount<aboList.size() || uboCount<boList.size() || orgPaCount<orgAdminList.size() || accPaCount<accountAdminList.size() ){
	            	Map<String,Object> updateMap=new HashMap<String,Object>();
	            	 ICruseRequestType request1=new ICruseRequestType();
	            	 request1.setAppName(SchedulerConstants.ACE);
	            	 request1.setCreateCompany(SchedulerConstants.IFALSE);
	            	 request1.setCreateABO(SchedulerConstants.IFALSE);
	            	 request1.setCreateUBO(SchedulerConstants.IFALSE);
	            	 request1.setCreatePA(SchedulerConstants.IFALSE);

		            aboSize=0;
		            while(aboCount<aboList.size() && aboSize<10){
		            	ICruseAboVO aboVO=aboList.get(aboCount);
		            	aboCount++;
		            	if(verifyStatusCodes(aboVO.getOfacStatusCode()) || verifyStatusCodes(aboVO.getGndbCheckFlag()) || verifyStatusCodes(aboVO.getLexisNexusFlag()) || verifyStatusCodes(aboVO.getEmailageCheckFlag())){
		            		request1.setCreateABO(SchedulerConstants.ITRUE);
			            	ABOInfoListType aboDetails=buildAboRequest(aboVO);
			            	request1.getAboInfoList().add(aboDetails);
			            	updateMap.put(StringUtils.stripToEmpty(aboVO.getAboEmail())+SchedulerConstants.ABOC,aboVO);
			            	aboSize++;
			            	break;
		            	}else{
		           			logger.info(eventId, "SmartServiceEngine", "ICruse Scheduler Service", "ICruseServiceImpl : Create Requests",
		    		                "Status codes are not YS or NS for ABO", AmexLogger.Result.success,"","org id",aboVO.getSseOrgId());
		           		}
		            }

		            uboSize=0;
			        while(uboCount<boList.size() && uboSize<10-aboSize){
		            	ICruseBOVO boVO=boList.get(uboCount);
		            	if(verifyStatusCodes(boVO.getOfacStatusCode()) || verifyStatusCodes(boVO.getGndbCheckFlag())){
		            		request1.setCreateUBO(SchedulerConstants.ITRUE);
			            	UBOInfoType uboDetails=buildUboRequest(boVO);
			            	request1.getUboInfoList().add(uboDetails);
			            	updateMap.put(StringUtils.stripToEmpty(boVO.getSseBnfcyOwnerId())+SchedulerConstants.UBOC,boVO);
			            	uboSize++;
		            	}else{
		           			logger.info(eventId, "SmartServiceEngine", "ICruse Scheduler Service", "ICruseServiceImpl : Create Requests",
		    		                "Status codes are not YS or NS for BO", AmexLogger.Result.success,"","org id",boVO.getSseOrgId());
		           		}
		            	uboCount++;
			        }

		            orgPaSize=0;
		            while(orgPaCount<orgAdminList.size() && orgPaSize<10-(aboSize+uboSize)){
		            	ICrusePAVO orgAdmin=orgAdminList.get(orgPaCount);
		            	orgPaCount++;
		            	if(verifyStatusCodes(orgAdmin.getEmailageCheckFlag()) || verifyStatusCodes(orgAdmin.getGndbCheckFlag())){
		            		request1.setCreatePA(SchedulerConstants.ITRUE);
			            	PAInfoType paInfo=buildOrgPaRequest(orgAdmin);
			            	request1.getPaInfoList().add(paInfo);
			            	updateMap.put(StringUtils.stripToEmpty(orgAdmin.getAdminEmailID())+SchedulerConstants.OPAC,orgAdmin);
			              	orgPaSize++;
			              	break;
		            	}else{
			           		logger.info(eventId, "SmartServiceEngine", "ICruse Scheduler Service", "ICruseServiceImpl : Create Requests",
			    		               "Status codes are not YS or NS for Org Admin", AmexLogger.Result.success,"","Email Address",orgAdmin.getAdminEmailID());
			           	}
		            }

		            accPaSize=0;
		            if(orgPaSize==0){
			            while(accPaCount<accountAdminList.size() && accPaSize<10-(aboSize+uboSize+orgPaSize)){
			               	ICrusePAVO accAdmin=accountAdminList.get(accPaCount);
			               	accPaCount++;
			               	if(verifyStatusCodes(accAdmin.getEmailageCheckFlag()) || verifyStatusCodes(accAdmin.getGndbCheckFlag())){
			               		request1.setCreatePA(SchedulerConstants.ITRUE);
				               	PAInfoType paInfo=buildAccPaRequest(accAdmin);
					           	request1.getPaInfoList().add(paInfo);
					           	updateMap.put(StringUtils.stripToEmpty(accAdmin.getAdminEmailID())+SchedulerConstants.APAC,accAdmin);
					           	accPaSize++;
					           	break;
			               	}
			               	else{
					        	logger.info(eventId, "SmartServiceEngine", "ICruse Scheduler Service", "ICruseServiceImpl : Create Requests",
					    		           "Status codes are not YS or NS for Account Admin", AmexLogger.Result.success,"","Email Address",accAdmin.getAdminEmailID());
					        }
			           }
		           }
			       logger.info(eventId, "SmartServiceEngine", "ICruse Scheduler Service", "ICruseServiceImpl : Create Requests", "Created request for ABO, BO and PA to iCruse",
			        		AmexLogger.Result.success,"","ABO's requested until now",""+aboCount,"BO's",""+uboCount,"Org Admin's",""+orgPaCount,"Account Admin's",""+accPaCount );
		           ICruseResponseType response=initiateICruseCall(request1,eventId);
		           if(response!=null){
		        	   updateCompanyStatus(response,updateMap,eventId);
		           }
	            }
	         	logger.info(eventId, "SmartServiceEngine", "ICruse Scheduler Service", "ICruseServiceImpl : Create Requests",
		                "End of creating request for ABO, BO and PA to iCruse and updating KYC table", AmexLogger.Result.success,"End" );
          }else{
        	  logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_127_ERR_CD, TivoliMonitoring.SSE_SP_127_ERR_MSG, eventId));
        	  logger.error(eventId, "SmartServiceEngine", "ICruse Scheduler Service", "ICruseServiceImpl : fetchRecords","Empty outmap from E3GMR127 sp",
        			  AmexLogger.Result.success,"");

            }
        } catch (Exception e) {
            logger.error(eventId, "SmartServiceEngine", "ICruse Scheduler Service","OrganizationStatusServiceImpl : fetchRecords",
                "Exception while processing ICruse Scheduler Service call", AmexLogger.Result.failure, "Failed to execute fetchRecords", e, "ErrorMsg:", e.getMessage());
            throw new SSEApplicationException(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD, EnvironmentPropertiesUtil.getProperty(SchedulerErrorConstants.INTERNAL_SERVER_ERR_CD), e);
        }
        logger.info(eventId, "SmartServiceEngine", "ICruse Scheduler Service", "ICruseServiceImpl : fetchRecords",
            "End of  ICruse Scheduler Service", AmexLogger.Result.success, "");
    }

    public ICruseResponseType initiateICruseCall(ICruseRequestType iCruseRequestType,String eventId) throws JsonGenerationException, JsonMappingException, IOException{
		ICruseResponseType response=null;
		try{
			String url = EnvironmentPropertiesUtil.getProperty(SchedulerConstants.ICRUSE_URL);
			RestTemplate restTemplate = new RestTemplate();
			((SimpleClientHttpRequestFactory)restTemplate.getRequestFactory()).setReadTimeout(Integer.parseInt(EnvironmentPropertiesUtil.getProperty(SchedulerConstants.ICRUSE_REQUEST_TIMEOUT_VALUE))*1000);
			((SimpleClientHttpRequestFactory)restTemplate.getRequestFactory()).setConnectTimeout(Integer.parseInt(EnvironmentPropertiesUtil.getProperty(SchedulerConstants.ICRUSE_REQUEST_TIMEOUT_VALUE))*1000);
			HttpEntity<String> entity = new HttpEntity<String>(jsonToString(iCruseRequestType));
			logger.info(eventId,"SmartServiceEngine","ICruse Scheduler Service","ICruseServiceImpl-call iCruse","Calling iCruse API to enroll company",
					AmexLogger.Result.success,"","ICruse Request JSON payload",entity.toString());
			ResponseEntity<String> result = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
			logger.info(eventId,"SmartServiceEngine","ICruse Scheduler Service","ICruseServiceImpl-call iCruse","Response from iCruse API to enroll company",
					AmexLogger.Result.success,"","ICruse Response JSON payload",result.toString());
	        response=stringToJson(result.getBody().toString());
		}
		catch(Exception e){
	       	if(e.getMessage().contains("java.net.SocketTimeoutException")){
	       		logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SHED_ICRUSE_TIMEOUT_ERR_CD,TivoliMonitoring.SHED_ICRUSE_TIMEOUT_ERR_MSG, eventId));
				logger.error(eventId,"SmartServiceEngine","ICruse Scheduler Service","ICruseServiceImpl-Initaite ICruse Call","Time out exception occured in iCruse service",
						AmexLogger.Result.failure,"Exception",e);
	       	}else{
	       		logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_ICRUSE_CALL_ERR_CD,TivoliMonitoring.SSE_ICRUSE_CALL_ERR_MSG, eventId));
				logger.error(eventId,"SmartServiceEngine","ICruse Scheduler Service","ICruseServiceImpl-Initaite ICruse Call","Error occured during iCruse call",
						AmexLogger.Result.failure,"Exception",e);
	       	}
		}
		return response;
	}

	public ICruseResponseType stringToJson(String json) throws JsonParseException, JsonMappingException, IOException{
		ICruseResponseType response = new ObjectMapper().readValue(json, ICruseResponseType.class);
		return response;
	}

	public static String jsonToString(Object request) throws JsonGenerationException, JsonMappingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
        mapper.getSerializationConfig().withSerializationInclusion(Include.NON_NULL);
        mapper.getSerializationConfig().with(SerializationFeature.INDENT_OUTPUT);
        String result = mapper.writeValueAsString(request);
        return result;
    }

	public void updateCompanyStatus(ICruseResponseType iCruseResponseType,Map<String,Object> updateMap, String eventId){
		List<CCFVFIDListType> ccfvfIDList=iCruseResponseType.getCcfvfIDList();
		if (ccfvfIDList != null && !ccfvfIDList.isEmpty()) {
			logger.info(eventId,"SmartServiceEngine","ICruse Scheduler Service","ICruseServiceImpl-Update iCruse Status","Updating status of PA, BO and ABO according to the response from iCruse",
					AmexLogger.Result.success,"");
			for(CCFVFIDListType ccfvfid:ccfvfIDList){
				if(ccfvfid.getStatusCode().equals(SchedulerConstants.RC1)){
					Map<String,Object> inMap=new HashMap<String,Object>();
					try{
						//inMap.put(SchedulerConstants.IN_ORG_ID,updateMap.get(ccfvfid.getUniqueKey()));
						inMap.put(SchedulerConstants.IN_UNIQUE_ID,ccfvfid.getUniqueKey());
						String entity=ccfvfid.getCaseID();
						StringTokenizer st=new StringTokenizer(entity,SchedulerConstants.STOKEN);
						if(st.hasMoreElements()){
							String entityType=st.nextToken();
							if(entityType.equals(SchedulerConstants.UBO)){
								ICruseBOVO boVO=(ICruseBOVO) updateMap.get(StringUtils.stripToEmpty(ccfvfid.getUniqueKey())+SchedulerConstants.UBOC);
								if(boVO != null){
									inMap.put(SchedulerConstants.IN_ENTITY_TYPE,StringUtils.isNotBlank(boVO.getEntityTypeCode()) ? boVO.getEntityTypeCode().trim() : "BO");
									inMap.put(SchedulerConstants.IN_ORG_ID,StringUtils.stripToEmpty(boVO.getSseOrgId()));
								}else{
									logger.info(eventId, "SmartServiceEngine", "ICruse Scheduler Service", "ICruseServiceImpl-Update company Status", "BO list is empty",
											AmexLogger.Result.success, "");
								}
							}else if(entityType.equals(SchedulerConstants.COMP)){
								ICruseOrgVO iCruseOrgVO=(ICruseOrgVO) updateMap.get(StringUtils.stripToEmpty(ccfvfid.getUniqueKey()));
								if(iCruseOrgVO != null){
									inMap.put(SchedulerConstants.IN_ENTITY_TYPE, StringUtils.isNotBlank(iCruseOrgVO.getEntityTypeCode()) ? iCruseOrgVO.getEntityTypeCode().trim() : "ORG");
									inMap.put(SchedulerConstants.IN_ORG_ID,StringUtils.stripToEmpty(iCruseOrgVO.getSseOrgId()));
								}else{
									logger.info(eventId, "SmartServiceEngine", "ICruse Scheduler Service", "ICruseServiceImpl-Update company Status", "Organization list is empty",
											AmexLogger.Result.success, "");
								}
							}else if(entityType.equals(SchedulerConstants.ABO)){
								ICruseAboVO aboVO=(ICruseAboVO) updateMap.get(StringUtils.stripToEmpty(ccfvfid.getUniqueKey())+SchedulerConstants.ABOC);
								if(aboVO != null){
									inMap.put(SchedulerConstants.IN_ENTITY_TYPE,StringUtils.isNotBlank(aboVO.getEntityTypeCode()) ? aboVO.getEntityTypeCode().trim() : "ABO");
									inMap.put(SchedulerConstants.IN_ORG_ID,StringUtils.stripToEmpty(aboVO.getSseOrgId()));
								}else{
									logger.info(eventId, "SmartServiceEngine", "ICruse Scheduler Service", "ICruseServiceImpl-Update company Status", "ABO list is empty",
											AmexLogger.Result.success, "");
								}
							}else{
								ICrusePAVO accAdmin=(ICrusePAVO) updateMap.get(StringUtils.stripToEmpty(ccfvfid.getUniqueKey())+SchedulerConstants.APAC);
								if(accAdmin != null){
									inMap.put(SchedulerConstants.IN_ENTITY_TYPE,accAdmin.getAdminEntityTypeCode());
									inMap.put(SchedulerConstants.IN_ORG_ID,StringUtils.stripToEmpty(accAdmin.getSseOrgId()));
								}else{
									logger.info(eventId, "SmartServiceEngine", "ICruse Scheduler Service", "ICruseServiceImpl-Update company Status", "Account PA list is empty",
											AmexLogger.Result.success, "");
								}
								ICrusePAVO orgAdmin=(ICrusePAVO) updateMap.get(StringUtils.stripToEmpty(ccfvfid.getUniqueKey())+SchedulerConstants.OPAC);
								if(orgAdmin != null){
									inMap.put(SchedulerConstants.IN_ENTITY_TYPE,orgAdmin.getAdminEntityTypeCode());
									inMap.put(SchedulerConstants.IN_ORG_ID,StringUtils.stripToEmpty(orgAdmin.getSseOrgId()));
								}else{
									logger.info(eventId, "SmartServiceEngine", "ICruse Scheduler Service", "ICruseServiceImpl-Update company Status", "Org PA list is empty",
											AmexLogger.Result.success, "");
								}
							}
						}
						inMap.put(SchedulerConstants.IN_TRANS_ID,ccfvfid.getCaseID());
						Map<String, Object> outMap = updateKYCStatusDAO.execute(inMap,eventId);
						if (outMap != null && !outMap.isEmpty()) {
							String responseCode = StringUtils.stripToEmpty((String) outMap.get(SchedulerConstants.RESP_CD));
							String respDesc = StringUtils.stripToEmpty((String) outMap.get(SchedulerConstants.RESP_MSG));
							String sqlCode = StringUtils.stripToEmpty((String) outMap.get(SchedulerConstants.SQLCODE_PARM));
							if(responseCode.equals(SchedulerConstants.SSEUP002) || responseCode.equals(SchedulerConstants.SSEUP001)){
								//logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_125_ERR_CD, TivoliMonitoring.SSE_SP_125_ERR_MSG, eventId));
								logger.info(eventId, "SmartServiceEngine", "ICruse Scheduler Service", "ICruseServiceImpl-Update company Status", "Updating the company status details according to iCruse response",
										AmexLogger.Result.success, "", SchedulerConstants.SQLCODE_PARM, sqlCode, "resp_code", responseCode, "resp_msg", respDesc, "OutMap", outMap.toString());
							}else if(responseCode.equals(SchedulerConstants.SSEUP102) || responseCode.equals(SchedulerConstants.SSEUP101)){
								logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_125_ERR_CD, TivoliMonitoring.SSE_SP_125_ERR_MSG, eventId));
								logger.info(eventId, "SmartServiceEngine", "ICruse Scheduler Service", "ICruseServiceImpl-Update company Status", "Error occured in database while updating the company status details according to iCruse response",
										AmexLogger.Result.success, "", SchedulerConstants.SQLCODE_PARM, sqlCode, "resp_code", responseCode, "resp_msg", respDesc, "OutMap", outMap.toString());
							}else if(responseCode.equals(SchedulerConstants.SSEUP100)){
								logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_125_ERR_CD, TivoliMonitoring.SSE_SP_125_ERR_MSG, eventId));
								logger.info(eventId, "SmartServiceEngine", "ICruse Scheduler Service", "ICruseServiceImpl-Update company Status", "Error occured in database while updating the company status details according to iCruse response",
										AmexLogger.Result.success, "", SchedulerConstants.SQLCODE_PARM, sqlCode, "resp_code", responseCode, "resp_msg", respDesc, "OutMap", outMap.toString());
							}else if(responseCode.equals(SchedulerConstants.SSEUP901)){
								logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_125_ERR_CD, TivoliMonitoring.SSE_SP_125_ERR_MSG, eventId));
								logger.info(eventId, "SmartServiceEngine", "ICruse Scheduler Service", "ICruseServiceImpl-Update company Status", "Error occured in database while updating the company status details according to iCruse response",
										AmexLogger.Result.success, "", SchedulerConstants.SQLCODE_PARM, sqlCode, "resp_code", responseCode, "resp_msg", respDesc, "OutMap", outMap.toString());
							} else{
								logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SSE_SP_125_ERR_CD, TivoliMonitoring.SSE_SP_125_ERR_MSG, eventId));
								logger.info(eventId, "SmartServiceEngine", "ICruse Scheduler Service", "ICruseServiceImpl-Update company Status", "Error occured in database with TEST0125 sp",
										AmexLogger.Result.success, "", SchedulerConstants.SQLCODE_PARM, sqlCode, "resp_code", responseCode, "resp_msg", respDesc, "OutMap", outMap.toString());
							}
						}
					}catch(Exception e){
						logger.error(eventId,"SmartServiceEngine", "ICruse Scheduler Service", "ICruseServiceImpl : 125 sp execution",
								"Error occured during creation of inMap for 125 sp",AmexLogger.Result.failure,"",e, "Exception Message",e.getMessage(),"UniqueKey", ccfvfid.getUniqueKey());
					}
				}else if(ccfvfid.getStatusCode().equals(SchedulerConstants.RCS1)){
					logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SCHED_ICRUSE_1_RESP_ERR_CD, TivoliMonitoring.SCHED_ICRUSE_1_RESP_ERR_MSG, eventId));
					logger.info(eventId, "SmartServiceEngine", "ICruse Scheduler Service", "ICruseServiceImpl-Update company status", "Got -1 status code from icruse",
							AmexLogger.Result.success, "","status Code",ccfvfid.getStatusCode(),"Status Message",ccfvfid.getStatusMessage());
				}else if(ccfvfid.getStatusCode().equals(SchedulerConstants.RCS2)){
					logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SCHED_ICRUSE_2_RESP_ERR_CD, TivoliMonitoring.SCHED_ICRUSE_2_RESP_ERR_MSG, eventId));
					logger.info(eventId, "SmartServiceEngine", "ICruse Scheduler Service", "ICruseServiceImpl-Update company status", "Got -2 status code from icruse",
							AmexLogger.Result.success, "","status Code",ccfvfid.getStatusCode(),"Status Message",ccfvfid.getStatusMessage());
				}else{
					logger.error(tivoliMonitoring.logStatement(TivoliMonitoring.SCHED_ICRUSE_RESP_ERR_CD, TivoliMonitoring.SCHED_ICRUSE_RESP_ERR_MSG, eventId));
					logger.info(eventId, "SmartServiceEngine", "ICruse Scheduler Service", "ICruseServiceImpl-Update company status", "Error occured in iCruse to get details about update of company status",
							AmexLogger.Result.success, "","status Code",iCruseResponseType.getCcfvfIDList().get(0).getStatusCode(),"Status Message",iCruseResponseType.getStatusMessage());
				}
			}
			logger.info(eventId,"SmartServiceEngine","ICruse Scheduler Service","ICruseServiceImpl-Update iCruse Status","Completed Updating the status of PA, BO and ABO according to the response from iCruse",
					AmexLogger.Result.success,"");
		}else{
			logger.info(eventId,"SmartServiceEngine","ICruse Scheduler Service","ICruseServiceImpl-Update iCruse Status","Some error occured in data coming from iCruse response",
					AmexLogger.Result.success,"","StatusCode",iCruseResponseType.getStatusCode(),"StatusMessage",iCruseResponseType.getStatusMessage());
		}
	}
	public ICruseRequestType buildOrgRequest(ICruseOrgVO iCruseOrgVO){
		ICruseRequestType request=new ICruseRequestType();
		request.setAppName(SchedulerConstants.ACE);
       	request.setCreateCompany(SchedulerConstants.IFALSE);
       	request.setCreateABO(SchedulerConstants.IFALSE);
       	request.setCreateUBO(SchedulerConstants.IFALSE);
       	request.setCreatePA(SchedulerConstants.IFALSE);
    	CompanyInfoType companyInfoType = new CompanyInfoType();
    	request.setCreateCompany(SchedulerConstants.ITRUE);
    	if(verifyStatusCodes(iCruseOrgVO.getOfacCheckStatusCode())){
    		companyInfoType.setVerifyBridgerInfo(SchedulerConstants.ITRUE);
    	}else{
    		companyInfoType.setVerifyBridgerInfo(SchedulerConstants.IFALSE);
    	}
    	if(verifyStatusCodes(iCruseOrgVO.getGndbCheckStatusCode())){
    		companyInfoType.setVerifyGNDBInfo(SchedulerConstants.ITRUE);
    	}else{
    		companyInfoType.setVerifyGNDBInfo(SchedulerConstants.IFALSE);
    	}
    	if(verifyStatusCodes(iCruseOrgVO.getLexisNexisCheckStatusCode())){
    		companyInfoType.setVerifyLexisNexisInfo(SchedulerConstants.ITRUE);
    	}else{
    		companyInfoType.setVerifyLexisNexisInfo(SchedulerConstants.IFALSE);
    	}
		companyInfoType.setVerifyEmailInfo(SchedulerConstants.IFALSE);
		companyInfoType.setcCFVFUID(iCruseOrgVO.getSseOrgId());
		companyInfoType.seteINorTIN(iCruseOrgVO.getTaxId());
		companyInfoType.setCompanyName(iCruseOrgVO.getOrgName());
		AddressDetailsListType addressDetailsList = new AddressDetailsListType();
		CCFVDBANameType ccfvDBAName = new CCFVDBANameType();
		PhoneDetailsListType phoneDetailsList=new PhoneDetailsListType();
		companyInfoType.getAddressDetailsList().add(addressDetailsList);
		companyInfoType.getPhoneDetailsList().add(phoneDetailsList);
		companyInfoType.setCcfvDBAName(ccfvDBAName);
		addressDetailsList.setAddrLine1(iCruseOrgVO.getOrgAddlLine1());
		addressDetailsList.setAddrLine2( iCruseOrgVO.getOrgAddlLine2());
		addressDetailsList.setCityName(iCruseOrgVO.getOrgPostalTownName());
		addressDetailsList.setCountryCode(iCruseOrgVO.getOrgCountryCode());
		addressDetailsList.setCountryName(iCruseOrgVO.getOrgCountryName());
		addressDetailsList.setEmailAddr(iCruseOrgVO.getOrgEmailId());
		addressDetailsList.setPostalCode( iCruseOrgVO.getOrgPostalCode());
		addressDetailsList.setState(iCruseOrgVO.getOrgRegionAreaName());
		addressDetailsList.setStateCode(iCruseOrgVO.getOrgRegionAreaCode());
		phoneDetailsList.setPhoneNm(StringUtils.stripToEmpty(iCruseOrgVO.getOrgPhoneNumber()));
		if(StringUtils.isNotBlank(iCruseOrgVO.getOrgPhoneNumber())){
			phoneDetailsList.setPhoneTypeCd(EnvironmentPropertiesUtil.getProperty(SchedulerConstants.ICRUSE_PHONETYPECD));
		}
		ccfvDBAName.setAddrLine1(iCruseOrgVO.getOrgAddlLine1());
		ccfvDBAName.setAddrLine2(iCruseOrgVO.getOrgAddlLine2());
		ccfvDBAName.setCityName(iCruseOrgVO.getOrgPostalTownName());
		ccfvDBAName.setCountryName(iCruseOrgVO.getOrgCountryName());
		ccfvDBAName.setPostalCode(iCruseOrgVO.getOrgPostalCode());
		ccfvDBAName.setState(iCruseOrgVO.getOrgRegionAreaName());

		request.setCompanyInfo(companyInfoType);
		return request;
	}
	public ABOInfoListType buildAboRequest(ICruseAboVO aboVO){
		ABOInfoListType aboDetails=new ABOInfoListType();
    	aboDetails.setfName(StringUtils.stripToEmpty(aboVO.getAboFirstName()));
    	aboDetails.setlName(StringUtils.stripToEmpty(aboVO.getAboLastName()));
    	aboDetails.setmName(StringUtils.stripToEmpty(aboVO.getAboMiddleName()));
    	aboDetails.setDob(StringUtils.stripToEmpty(aboVO.getAboDob().toString()));
    	aboDetails.setTiedToCompany(StringUtils.stripToEmpty(aboVO.getSseOrgId()));
    	if(verifyStatusCodes(aboVO.getLexisNexusFlag())){
    		aboDetails.setVerifyLexisNexisInfo(SchedulerConstants.ITRUE);
    	}else{
    		aboDetails.setVerifyLexisNexisInfo(SchedulerConstants.IFALSE);
    	}
    	if(verifyStatusCodes(aboVO.getOfacStatusCode())){
    		aboDetails.setVerifyBridgerInfo(SchedulerConstants.ITRUE);
    	}else{
    		aboDetails.setVerifyBridgerInfo(SchedulerConstants.IFALSE);
    	}
    	if(verifyStatusCodes(aboVO.getEmailageCheckFlag())){
    		aboDetails.setVerifyEmailInfo(SchedulerConstants.ITRUE);
    	}else{
    		aboDetails.setVerifyEmailInfo(SchedulerConstants.IFALSE);
    	}
    	if(verifyStatusCodes(aboVO.getGndbCheckFlag())){
    		aboDetails.setVerifyGNDBInfo(SchedulerConstants.ITRUE);
    	}else{
    		aboDetails.setVerifyGNDBInfo(SchedulerConstants.IFALSE);
    	}
    	AddressDetailsListType address=new AddressDetailsListType();
    	address.setEmailAddr(StringUtils.stripToEmpty(aboVO.getAboEmail()));
    	address.setWebsite(StringUtils.stripToEmpty(aboVO.getAboWebsite()));
    	address.setAddrLine1(StringUtils.stripToEmpty(aboVO.getAboAddlLine1()));
    	address.setAddrLine2(StringUtils.stripToEmpty(aboVO.getAboAddlLine2()));
    	address.setCityName(StringUtils.stripToEmpty(aboVO.getAboRegionAreaNM()));
    	address.setStateCode(StringUtils.stripToEmpty(aboVO.getAboRegionAreaCD()));
    	address.setState(StringUtils.stripToEmpty(aboVO.getAboRegionAreaNM()));
    	address.setCountryCode(StringUtils.stripToEmpty(aboVO.getAboCtryCode()));
    	address.setCountryName(StringUtils.stripToEmpty(aboVO.getAboCtryName()));
    	address.setPostalCode(StringUtils.stripToEmpty(aboVO.getAboPostalCD()));
    	aboDetails.getAddressDetailsList().add(address);
    	aboDetails.setIpAddress(StringUtils.stripToEmpty(aboVO.getAboIpAddress()));
    	PhoneDetailsListType phone=new PhoneDetailsListType();
    	phone.setPhoneNm(StringUtils.stripToEmpty(aboVO.getAboPhoneNo()));
    	if(StringUtils.isNotBlank(aboVO.getAboPhoneNo())){
    		phone.setPhoneTypeCd(EnvironmentPropertiesUtil.getProperty(SchedulerConstants.ICRUSE_PHONETYPECD));
		}
    	aboDetails.getPhoneDetailsList().add(phone);
    	aboDetails.setSsn(StringUtils.stripToEmpty(aboVO.getAboSsnSerNo()));
    	return aboDetails;
	}

	public UBOInfoType buildUboRequest(ICruseBOVO boVO){
		UBOInfoType uboDetails=new UBOInfoType();
    	String entity=StringUtils.stripToEmpty(boVO.getBnfcyType());
		if( entity.equals(SchedulerConstants.PUBLIC_COMPANY)){
			uboDetails.setUboEntityType(SchedulerConstants.COMP);
		}
		else if(entity.equals(SchedulerConstants.INDIVIDUAL) || entity.equals(SchedulerConstants.PRIVATE_COMPANY)){
			uboDetails.setUboEntityType(SchedulerConstants.INDV);
		}
    	uboDetails.setUboCompanyName(StringUtils.stripToEmpty(boVO.getBnfcyCompanyName()));
    	uboDetails.setTickerSymbol(StringUtils.stripToEmpty(boVO.getBnfcyTickerSymbol()));
    	uboDetails.setfName(StringUtils.stripToEmpty(boVO.getBnfcyFirstName()));
    	uboDetails.setlName(StringUtils.stripToEmpty(boVO.getBnfcyLastName()));
    	uboDetails.setDob(StringUtils.stripToEmpty(boVO.getBnfcyDob().toString()));
    	uboDetails.setTiedToCompany(StringUtils.stripToEmpty(boVO.getSseOrgId()));
    	if(verifyStatusCodes(boVO.getOfacStatusCode())){
    		uboDetails.setVerifyBridgerInfo(SchedulerConstants.ITRUE);
    	}else{
    		uboDetails.setVerifyBridgerInfo(SchedulerConstants.IFALSE);
    	}
    	if(verifyStatusCodes(boVO.getGndbCheckFlag())){
    		uboDetails.setVerifyGNDBInfo(SchedulerConstants.ITRUE);
    	}else{
    		uboDetails.setVerifyGNDBInfo(SchedulerConstants.IFALSE);
    	}
    	uboDetails.setVerifyEmailInfo(SchedulerConstants.IFALSE);
    	uboDetails.setVerifyLexisNexisInfo(SchedulerConstants.IFALSE);
    	uboDetails.setUboUID(StringUtils.stripToEmpty(boVO.getSseBnfcyOwnerId()));
    	return uboDetails;
	}
	public PAInfoType buildOrgPaRequest(ICrusePAVO orgAdmin){
		PAInfoType paInfo=new PAInfoType();
    	paInfo.setfName(StringUtils.stripToEmpty(orgAdmin.getAdminFirstName()));
    	paInfo.setlName(StringUtils.stripToEmpty(orgAdmin.getAdminLastName()));
    	paInfo.setTiedToCompany(StringUtils.stripToEmpty(orgAdmin.getSseOrgId()));
    	if(verifyStatusCodes(orgAdmin.getEmailageCheckFlag())){
    		paInfo.setVerifyEmailInfo(SchedulerConstants.ITRUE);
    	}else{
    		paInfo.setVerifyEmailInfo(SchedulerConstants.IFALSE);
    	}
    	if(verifyStatusCodes(orgAdmin.getGndbCheckFlag())){
    		paInfo.setVerifyGNDBInfo(SchedulerConstants.ITRUE);
    	}else{
    		paInfo.setVerifyGNDBInfo(SchedulerConstants.IFALSE);
    	}
		paInfo.setVerifyBridgerInfo(SchedulerConstants.IFALSE);
		paInfo.setVerifyLexisNexisInfo(SchedulerConstants.IFALSE);
    	AddressDetailsListType address=new AddressDetailsListType();
    	address.setEmailAddr(StringUtils.stripToEmpty(orgAdmin.getAdminEmailID()));
    	address.setWebsite(StringUtils.stripToEmpty(orgAdmin.getAdminWebPage()));
    	paInfo.getAddressDetailsList().add(address);
    	PhoneDetailsListType phone=new PhoneDetailsListType();
    	phone.setPhoneNm(StringUtils.stripToEmpty(orgAdmin.getAdminPhoneNo()));
    	if(StringUtils.isNotBlank(orgAdmin.getAdminPhoneNo())){
    		phone.setPhoneTypeCd(EnvironmentPropertiesUtil.getProperty(SchedulerConstants.ICRUSE_PHONETYPECD));
		}
    	paInfo.getPhoneDetailsList().add(phone);
    	IDDetailsListType idDetails=new IDDetailsListType();
    	idDetails.setIdNbr(StringUtils.stripToEmpty(orgAdmin.getAdminUserId()));
    	paInfo.getIdDetailsList().add(idDetails);
    	paInfo.setIpAddress(StringUtils.stripToEmpty(orgAdmin.getAdminIpAddress()));
    	return paInfo;
	}

	public PAInfoType buildAccPaRequest(ICrusePAVO accAdmin){
		PAInfoType paInfo=new PAInfoType();
    	paInfo.setfName(StringUtils.stripToEmpty(accAdmin.getAdminFirstName()));
    	paInfo.setlName(StringUtils.stripToEmpty(accAdmin.getAdminLastName()));
    	paInfo.setTiedToCompany(StringUtils.stripToEmpty(accAdmin.getSseOrgId()));
    	if(verifyStatusCodes(accAdmin.getEmailageCheckFlag())){
    		paInfo.setVerifyEmailInfo(SchedulerConstants.ITRUE);
    	}else{
    		paInfo.setVerifyEmailInfo(SchedulerConstants.IFALSE);
    	}
    	if(verifyStatusCodes(accAdmin.getGndbCheckFlag())){
    		paInfo.setVerifyGNDBInfo(SchedulerConstants.ITRUE);
    	}else{
    		paInfo.setVerifyGNDBInfo(SchedulerConstants.IFALSE);
    	}
		paInfo.setVerifyBridgerInfo(SchedulerConstants.IFALSE);
		paInfo.setVerifyLexisNexisInfo(SchedulerConstants.IFALSE);
    	AddressDetailsListType address=new AddressDetailsListType();
    	address.setEmailAddr(StringUtils.stripToEmpty(accAdmin.getAdminEmailID()));
    	address.setWebsite(StringUtils.stripToEmpty(accAdmin.getAdminWebpageText()));
    	paInfo.getAddressDetailsList().add(address);
    	PhoneDetailsListType phone=new PhoneDetailsListType();
    	phone.setPhoneNm(StringUtils.stripToEmpty(accAdmin.getAdminPhoneNo()));
    	if(StringUtils.isNotBlank(accAdmin.getAdminPhoneNo())){
    		phone.setPhoneTypeCd(EnvironmentPropertiesUtil.getProperty(SchedulerConstants.ICRUSE_PHONETYPECD));
		}
    	paInfo.getPhoneDetailsList().add(phone);
    	IDDetailsListType idDetails=new IDDetailsListType();
    	idDetails.setIdNbr(StringUtils.stripToEmpty(accAdmin.getAdminPhoneNo()));
    	paInfo.getIdDetailsList().add(idDetails);
    	paInfo.setIpAddress(StringUtils.stripToEmpty(accAdmin.getAdminIpAddress()));
    	return paInfo;
	}

	public boolean verifyStatusCodes(String code){
		boolean status=false;
		if(StringUtils.isNotBlank(code) && (code.equals(SchedulerConstants.YS)||code.equals(SchedulerConstants.NS))){
			status=true;
		}
		return status;
	}
}
