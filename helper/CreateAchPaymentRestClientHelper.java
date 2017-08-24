package com.americanexpress.smartserviceengine.helper;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.createpaymentservice.ACHPaymentRequestType;
import com.americanexpress.createpaymentservice.ArrangementDetailsType;
import com.americanexpress.createpaymentservice.CustomerDetailsType;
import com.americanexpress.createpaymentservice.EnrollmentDetailsType;
import com.americanexpress.createpaymentservice.FinancialInstitutionAccountDetailsType;
import com.americanexpress.createpaymentservice.LineOfBusinessType;
import com.americanexpress.createpaymentservice.RegistrationType;
import com.americanexpress.createpaymentservice.RemittancesType;
import com.americanexpress.createpaymentservice.ServiceAttributesType;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.exception.SSEApplicationException;
import com.americanexpress.smartserviceengine.common.payload.ManageEnrollmentRequestData;
import com.americanexpress.smartserviceengine.common.util.DateTimeUtil;
import com.americanexpress.smartserviceengine.common.util.EnvironmentPropertiesUtil;
import com.americanexpress.smartserviceengine.common.util.GenericUtilities;
import com.americanexpress.smartserviceengine.common.util.TivoliMonitoring;
import com.americanexpress.smartserviceengine.common.vo.ValidateAchPymtRespVO;
import com.americanexpress.smartserviceengine.common.vo.ValidateEnrollmentRespVO;

@Service
public class CreateAchPaymentRestClientHelper {
	
    private static AmexLogger logger = AmexLogger.create(CreateAchPaymentRestClientHelper.class);
    
    @Autowired
    private TivoliMonitoring tivoliMonitoring;

    public void generateRestTemplate(String requestId, ValidateAchPymtRespVO validateAchPymtRespVO,String apiMsgId, boolean isScheduler, String pymtTransType) throws SSEApplicationException{
    	try{
    	ACHPaymentRequestType requestType=createPaymentRequest( requestId,  validateAchPymtRespVO, apiMsgId,  isScheduler,  pymtTransType);
		String jsonPayload = GenericUtilities.javaToJson(requestType);
		RestTemplate rest=new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.set(ApiConstants.CONTENT_TYPE_REST,ApiConstants.CONTENT_TYPE_APP_JSON);
		ResponseEntity<String> result=null;
		HttpEntity<String> entity = new HttpEntity(jsonPayload, headers);
		result = rest.exchange(EnvironmentPropertiesUtil.getProperty(ApiConstants.EMM_PAYMENT_REST_SERVICE_URL), HttpMethod.POST, entity, String.class);
		System.out.println(result);
    	}catch(Exception e){
    		System.out.println(e);
    		throw new SSEApplicationException("","","",e);
    	}
    }
    
   public ACHPaymentRequestType createPaymentRequest(String requestId, ValidateAchPymtRespVO validateAchPymtRespVO,String apiMsgId, boolean isScheduler, String pymtTransType) throws ParseException{
	   ACHPaymentRequestType requestType=new ACHPaymentRequestType();
	//   requestType.setVersion("5.0");
	   String partnerInd=validateAchPymtRespVO.getPartnerIndicator();
	   requestType.setServiceAttributes(createServiceAttributes(partnerInd,requestId));  
	    requestType.setRemittances(setRemittancesDetails(partnerInd,validateAchPymtRespVO,pymtTransType));
	   return requestType;
   }


private List<RemittancesType> setRemittancesDetails(String partnerInd,ValidateAchPymtRespVO validateAchPymtRespVO,String pymtTransType) throws ParseException {
	List<RemittancesType> remittances=new ArrayList<RemittancesType>();
	RemittancesType remittancesType=new RemittancesType();
		   remittancesType.setRequestSequenceNbr("1");
		   remittancesType.setLineOfBusiness(setLineOfBusiness(partnerInd));
		   remittancesType.setCustomerDetails(setCustomerDetails(validateAchPymtRespVO));
		   remittancesType.setRegistration(setRegistrationDetails(validateAchPymtRespVO,pymtTransType));
		   remittances.add(remittancesType);
	 
	return remittances;
}


private RegistrationType setRegistrationDetails(ValidateAchPymtRespVO validateAchPymtRespVO,String pymtTransType) throws ParseException {
	RegistrationType registration=new RegistrationType();
	registration.setLobServiceId(ApiConstants.EMM_GETSTATUS_LOB_SERV_ID);
	registration.setChangeReasonCd(ApiConstants.CHNG_RSN_CD);
	registration.setArrangementDetails(setArrangementDetails(validateAchPymtRespVO, pymtTransType));
	return registration;
}


private List<ArrangementDetailsType> setArrangementDetails(ValidateAchPymtRespVO validateAchPymtRespVO,String pymtTransType) throws ParseException {
	List<ArrangementDetailsType> arrangementDetails=new ArrayList<ArrangementDetailsType>();
	ArrangementDetailsType arrangementDetailsType=new ArrangementDetailsType();
		arrangementDetailsType.setEnrollment(setEnrollmentDetails(validateAchPymtRespVO));
		arrangementDetailsType.setConfirmationTxt(validateAchPymtRespVO.getPayArngSetupCfmNbr());
		arrangementDetailsType.setFrequencyCd(ApiConstants.ORG_UPD_SRVC_IND_COMMON);
		//arrangementDetailsType.setScheduleDt("");
		arrangementDetailsType.setOptionCd(ApiConstants.ORG_UPD_SRVC_IND_COMMON);
		arrangementDetailsType.setPaymentAmt(validateAchPymtRespVO.getPaymentValue());
		arrangementDetailsType.setTermsAndConditionsAcceptenceInd(ApiConstants.CHAR_Y);
	//	arrangementDetailsType.setTermsAndConditionsVersionId("");
	//	arrangementDetailsType.setTermsAndConditionsAcceptenceDt("");
		arrangementDetailsType.setChangeReasonCd(ApiConstants.CHNG_RSN_CD);
		arrangementDetailsType.setCreditDebitCd(pymtTransType);
		arrangementDetailsType.setSecCd(ApiConstants.EMM_PROCESS_SEC_CD);
		arrangementDetailsType.setCompanyId(validateAchPymtRespVO.getCompanyId());
		arrangementDetailsType.setEntryEffectiveDt(DateTimeUtil.getDb2Date(validateAchPymtRespVO.getPaymentEffectiveDate()));
		/*arrangementDetailsType.setReprocessPaymentInd("");*/
	
		arrangementDetails.add(arrangementDetailsType);
	return arrangementDetails;
}


private EnrollmentDetailsType setEnrollmentDetails(ValidateAchPymtRespVO validateAchPymtRespVO) {
	EnrollmentDetailsType enrollmentDetails=new EnrollmentDetailsType();
	enrollmentDetails.setSaveEnrollmentInd(ApiConstants.CHAR_N);
	enrollmentDetails.setEnrollmentFormatCd(ApiConstants.EMM_PYMT_ENRID_FMT_CD);
	enrollmentDetails.setEnrollmentId(String.valueOf(validateAchPymtRespVO.getSrcEnrollId()));
	enrollmentDetails.setEnrollmentTransactionTypeCd(ApiConstants.ENROLL_TRANS_TYPE_CD);
	enrollmentDetails.setTermsAndConditionsAcceptenceInd(ApiConstants.CHAR_Y);
//enrollmentDetails.setTermsAndConditionsVersionId("");
//	enrollmentDetails.setTermsAndConditionsAcceptenceDt("");
	enrollmentDetails.setChangeReasonCd(ApiConstants.CHNG_RSN_CD);
	enrollmentDetails.setFinancialInstitutionAccountDetails(setFinancailAccountDetails());
	return enrollmentDetails;
}


private FinancialInstitutionAccountDetailsType setFinancailAccountDetails() {
	FinancialInstitutionAccountDetailsType financialAccountDetails=new FinancialInstitutionAccountDetailsType();
/*	financialAccountDetails.setRoutingTransitNbr("");
	financialAccountDetails.setAccountNbr("");*/
	financialAccountDetails.setAccountTypeCd(ApiConstants.EMM_GETSTATUS_ACCTID_FMT_CD);
	financialAccountDetails.setInstrumentTypeCd(ApiConstants.EMM_GETSTATUS_ACCTID_FMT_CD);
/*	financialAccountDetails.setOwnerTypeCd("");
	financialAccountDetails.setOrganizationNm("");
	financialAccountDetails.setPrefixNm("");
	financialAccountDetails.setFirstNm("");
	financialAccountDetails.setMiddleNm("");
	financialAccountDetails.setLastNm("");*/
	return financialAccountDetails;
}

private CustomerDetailsType setCustomerDetails(ValidateAchPymtRespVO validateAchPymtRespVO) {
	CustomerDetailsType customerDetails=new CustomerDetailsType();
	customerDetails.setAccountTypeCd(ApiConstants.EMM_GETSTATUS_ACCT_TYP_CD);
	customerDetails.setCustomerAccountNbr(validateAchPymtRespVO.getCardAcctNbr());
	customerDetails.setOrganizationNm(StringUtils.isNotBlank(validateAchPymtRespVO.getOrgName()) ? validateAchPymtRespVO.getOrgName().trim() : StringUtils.EMPTY);
	/*customerDetails.setPrefixNm("");
	customerDetails.setFirstNm("");
	customerDetails.setMiddleNm("");
	customerDetails.setLastNm("");*/
	return customerDetails;
}


private LineOfBusinessType setLineOfBusiness(String partnerInd ) {
	LineOfBusinessType lineOfBusiness=new LineOfBusinessType();
	if(partnerInd != null && ApiConstants.PARTNER_IND_LIBERTY.equals(partnerInd)){
		lineOfBusiness.setInitiatorId(ApiConstants.EMM_LIBERTY_LOB);
	}else{
		lineOfBusiness.setInitiatorId("AMSSE");

	}
	
	lineOfBusiness.setUnitId(ApiConstants.EMM_GETSTATUS_LOB_UNIT_ID);
	lineOfBusiness.setClearingCountryCd(ApiConstants.EMM_GETSTATUS_CNTRYCD);
	lineOfBusiness.setClearingCurrencyCd(ApiConstants.EMM_GETSTATUS_CLR_CURR_CD);
	lineOfBusiness.setServiceId(ApiConstants.EMM_GETSTATUS_LOB_SERV_ID);
	return lineOfBusiness;
}


private ServiceAttributesType createServiceAttributes(String partnerInd,String requestId) {
	   ServiceAttributesType serviceAttributes=new ServiceAttributesType();
	   serviceAttributes.setLastUpdateId(requestId);
	   serviceAttributes.setMessageId("1244444");
	   serviceAttributes.setMessagePostTime("2016-10-11T11:00:39.438-0700");
	   serviceAttributes.setSendResponseCd(ApiConstants.CHAR_Y);
	 /*  if(partnerInd != null && ApiConstants.PARTNER_IND_LIBERTY.equals(partnerInd)){
		   serviceAttributes.setSourceEnum(Integer.parseInt(EnvironmentPropertiesUtil.getProperty(ApiConstants.EMM_LIBERTY_SOURCECD)));
	   }else{
		   serviceAttributes.setSourceEnum(Integer.parseInt(EnvironmentPropertiesUtil.getProperty(ApiConstants.EMM_INTACCT_SOURCECD)));

	   }*/
	   serviceAttributes.setSourceEnum("PS_SSE");

	   return serviceAttributes;
}


}
