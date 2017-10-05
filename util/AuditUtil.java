package com.americanexpress.smartserviceengine.common.util;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.payload.AccountDetailsResponseType;
import com.americanexpress.smartserviceengine.common.payload.AccountDetailsType;
import com.americanexpress.smartserviceengine.common.payload.CheckAccountDetailsType;
import com.americanexpress.smartserviceengine.common.payload.CommonContext;
import com.americanexpress.smartserviceengine.common.payload.ManageEnrollmentRequestData;
import com.americanexpress.smartserviceengine.common.payload.ManageEnrollmentRequestType;
import com.americanexpress.smartserviceengine.common.payload.ManageEnrollmentResponseType;
import com.americanexpress.smartserviceengine.common.payload.ProcessPaymentRequestData;
import com.americanexpress.smartserviceengine.common.payload.ProcessPaymentRequestType;
import com.americanexpress.smartserviceengine.common.payload.ProcessPaymentResponseData;
import com.americanexpress.smartserviceengine.common.payload.ProcessPaymentResponseType;
import com.americanexpress.smartserviceengine.common.payload.ServiceType;
import com.americanexpress.smartserviceengine.common.vo.AuditVO;

public class AuditUtil {

    private static final AmexLogger LOG = AmexLogger.create(AuditUtil.class);

    /**
     * To populate the Audit Bean with Enrollment Request/Response Data
     * @param apiMsgId
     * @param actionType
     * @param requestType
     * @param request
     * @param responseType
     * @return
     */
    public AuditVO createEnrollAuditBean(String apiMsgId, String actionType, String requestType, ManageEnrollmentRequestType request, ManageEnrollmentResponseType enrollResponse){

        AuditVO auditBean = null;
        LOG.debug(apiMsgId, "SmartServiceEngine",
            "Manage Enrollment API - Audit Logging",
            "createEnrollRequestBean method",
            "Start populating AuditBean with enrollment request/response data", AmexLogger.Result.success,
                "");
        try{

            //Create replica of ManageEnrollmentRequestType object to log the JSON payload after removing images
            ObjectMapper mapper = new ObjectMapper();
            String payloadString = mapper.writeValueAsString(request);
            ManageEnrollmentRequestType enrollRequest = mapper.readValue(payloadString, ManageEnrollmentRequestType.class);


            if(null != enrollRequest && null != enrollRequest.getManageEnrollmentRequest() && null != enrollRequest.getManageEnrollmentRequest().getData()){

                auditBean = new AuditVO();
                auditBean.setApiMsgId(apiMsgId);

                ManageEnrollmentRequestData data = enrollRequest.getManageEnrollmentRequest().getData();

                //If the action type is response, get the CommonRequest data, account List and payload from Response object
                if(actionType.equals(ApiConstants.RESPONSE) && null != enrollResponse && null != enrollResponse.getManageEnrollmentResponse()
                        && null != enrollResponse.getManageEnrollmentResponse().getData()){
                    auditBean = setCommonContextData(enrollResponse.getManageEnrollmentResponse().getData().getCommonResponseContext(), apiMsgId, auditBean);
                    auditBean.setAccountList(getAccountList(enrollResponse.getManageEnrollmentResponse().getData().getAcctDetails()));
                    //No Binary data (images) to remove from response payload.
                    auditBean.setRequestData(ApiUtil.pojoToJSONString(enrollResponse));

                }else if (actionType.equals(ApiConstants.REQUEST)){ //Else, get CommonRequest data, account List and payload from Request
                    auditBean = setCommonContextData(data.getCommonRequestContext(), apiMsgId, auditBean);
                    auditBean.setAccountList(getAccountList(data.getAccountDetails()));
                    //Remove Binary data (images) from payload
                    auditBean.setRequestData(ApiUtil.jsonToString(removeImagesFromEnrlRequest(apiMsgId, enrollRequest)));

                }

                auditBean.setEnrollActionType((null != data.getEnrollmentActionType()) ? data.getEnrollmentActionType() : ApiConstants.CHAR_SPACE);
                auditBean.setEnrollCatType((null != data.getEnrollmentCategory()) ? data.getEnrollmentCategory() : ApiConstants.CHAR_SPACE);
                auditBean.setOrgId((null != data.getOrganizationId()) ? data.getOrganizationId() : ApiConstants.CHAR_SPACE);
                auditBean.setAssOrgId((null != data.getAssociatedOrgId()) ? data.getAssociatedOrgId() : ApiConstants.CHAR_SPACE);

                if(null != data.getOrganizationInfo()){
                    List <ServiceType> subscribeSrvCdList = data.getOrganizationInfo().getSubscribeServices();
                    List <ServiceType> unsubscribeSrvCdList = data.getOrganizationInfo().getUnsubscribeServices();

                    auditBean.setSubServiceList(getServiceList(subscribeSrvCdList));
                    auditBean.setUnsubServiceList(getServiceList(unsubscribeSrvCdList));
                }else{
                    auditBean.setSubServiceList(ApiConstants.CHAR_SPACE);
                    auditBean.setUnsubServiceList(ApiConstants.CHAR_SPACE);
                }
                //Not applicable for Enrollment API. Set space
                auditBean.setBuyerPaymntId(ApiConstants.CHAR_SPACE);

                auditBean.setActionType(actionType);
                auditBean.setRequestType(requestType);
                auditBean.setJvmName(ApiUtil.getHostName());
            }
        }catch(Exception e){
            LOG.error(apiMsgId,"SmartServiceEngine","createEnrollRequestBean", "Exception block - createEnrollRequestBean",
                " Exception while creating AuditBean with enrollment data for logging", AmexLogger.Result.failure,"Exception while creating AuditBean for enrollment logging"
                ,e);
        }
        return auditBean;
    }


    /**
     * Utility method to get the concatenated string from list of entries (subscribed/unsubscribed services for audit logging)
     * @param srvcCdList
     * @return
     */
    private String getServiceList(List <ServiceType> srvcCdList){

        String serviceString = ApiConstants.CHAR_SPACE;

        if(null != srvcCdList && !srvcCdList.isEmpty()){

            List<String> serviceCodes = new ArrayList<String>();

            for(ServiceType serviceType : srvcCdList){
                if(null != serviceType){
                    serviceCodes.add(serviceType.getServiceCode());
                }
            }

            if(null != serviceCodes && !serviceCodes.isEmpty()){
                serviceString = StringUtils.join(serviceCodes.toArray(), ApiConstants.CHAR_PIPE);
            }

        }
        return serviceString;
    }

    /**
     * Utility method to generate the pipe separated accountId string from account details list object
     * @param accDetails
     * @return
     */
    private String getAccountList(AccountDetailsType accDetails){
        String acctString = ApiConstants.CHAR_SPACE;
        if(null != accDetails){

            List<CheckAccountDetailsType> checkAccountDetailsList = accDetails.getCheckAcctDetails();

            if(null != checkAccountDetailsList && ! checkAccountDetailsList.isEmpty()){
                List<String> accDetailsList = new ArrayList<String>();
                for(CheckAccountDetailsType chkAccDetails : checkAccountDetailsList){
                    if(null != chkAccDetails){
                        accDetailsList.add(chkAccDetails.getPartnerAccountId());
                    }
                }
                if(null != accDetailsList && !accDetailsList.isEmpty()){
                    acctString = StringUtils.join(accDetailsList.toArray(), ApiConstants.CHAR_PIPE);
                }
            }
        }
        return acctString;
    }

    /**
     * Overloaded method to generate the pipe separated account ID list from response
     * @param accDetails
     * @return
     */
    private String getAccountList(List<AccountDetailsResponseType> accDetails){
        String acctString = ApiConstants.CHAR_SPACE;

        if(null != accDetails && !accDetails.isEmpty()){
            List<String> accDetailsList = new ArrayList<String>();
            for(AccountDetailsResponseType acctDetailsRespType : accDetails){
                if(null != acctDetailsRespType){
                    accDetailsList.add(acctDetailsRespType.getPartnerAccountId());
                }
            }
            if(null != accDetailsList && !accDetailsList.isEmpty()){
                acctString = StringUtils.join(accDetailsList.toArray(), ApiConstants.CHAR_PIPE);
            }
        }

        return acctString;
    }

    /**
     * Removes images from request data and creates JSON string from the request.
     * To avoid storing large data in DB, remove images from the request json
     * @param enrollRequest
     * @return
     */
    private ManageEnrollmentRequestType removeImagesFromEnrlRequest(String apiMsgId, ManageEnrollmentRequestType requestObj) throws IOException{

        if(null != requestObj && null != requestObj.getManageEnrollmentRequest() && null != requestObj.getManageEnrollmentRequest().getData()){

            if(null != requestObj.getManageEnrollmentRequest().getData().getAccountDetails() &&
                    null != requestObj.getManageEnrollmentRequest().getData().getAccountDetails().getCheckAcctDetails() &&
                    !requestObj.getManageEnrollmentRequest().getData().getAccountDetails().getCheckAcctDetails().isEmpty()){

                for(int i=0; i<requestObj.getManageEnrollmentRequest().getData().getAccountDetails().getCheckAcctDetails().size(); i++){

                    if(null != requestObj.getManageEnrollmentRequest().getData().getAccountDetails().getCheckAcctDetails().get(i)){

                        requestObj.getManageEnrollmentRequest().getData().getAccountDetails().getCheckAcctDetails().get(i).setCheckLayoutImg(null);
                        requestObj.getManageEnrollmentRequest().getData().getAccountDetails().getCheckAcctDetails().get(i).setLogoImage(null);

                        if(null != requestObj.getManageEnrollmentRequest().getData().getAccountDetails().getCheckAcctDetails().get(i).getSignatureDetails()
                                && !requestObj.getManageEnrollmentRequest().getData().getAccountDetails().getCheckAcctDetails().get(i).getSignatureDetails().isEmpty()){
                            for(int j=0; j<requestObj.getManageEnrollmentRequest().getData().getAccountDetails().getCheckAcctDetails().get(i).getSignatureDetails().size(); j++){

                                if(null != requestObj.getManageEnrollmentRequest().getData().getAccountDetails().getCheckAcctDetails().get(i).getSignatureDetails().get(j)){
                                    requestObj.getManageEnrollmentRequest().getData().getAccountDetails().getCheckAcctDetails().get(i).getSignatureDetails().get(j).setSignature1Image(null);
                                    requestObj.getManageEnrollmentRequest().getData().getAccountDetails().getCheckAcctDetails().get(i).getSignatureDetails().get(j).setSignature2Image(null);

                                }

                            }
                        }
                    }

                }
            }

            if(null != requestObj.getManageEnrollmentRequest().getData().getOrganizationInfo() &&
                    null != requestObj.getManageEnrollmentRequest().getData().getOrganizationInfo().getOrgCheckDetails() &&
                    null != requestObj.getManageEnrollmentRequest().getData().getOrganizationInfo().getOrgCheckDetails().getPayorAttachment() &&
                    !requestObj.getManageEnrollmentRequest().getData().getOrganizationInfo().getOrgCheckDetails().getPayorAttachment().isEmpty()){

                for(int i=0; i<requestObj.getManageEnrollmentRequest().getData().getOrganizationInfo().getOrgCheckDetails().getPayorAttachment().size(); i++){

                    if(null != requestObj.getManageEnrollmentRequest().getData().getOrganizationInfo().getOrgCheckDetails().getPayorAttachment().get(i)){
                        requestObj.getManageEnrollmentRequest().getData().getOrganizationInfo().getOrgCheckDetails().getPayorAttachment().get(i).setAttachmentFile(null);
                    }

                }
            }

        }
        return requestObj;
    }

    /**
     * Utility method to read Common context data and set it to AuditBean
     * @param commonContext
     * @param apiMsgId
     * @param auditBean
     * @return
     */
    private AuditVO setCommonContextData(CommonContext commonContext, String apiMsgId, AuditVO auditBean){

        if( null != commonContext && null != commonContext.getPartnerId()){
            auditBean.setPartnerId(commonContext.getPartnerId());
        }else{
            auditBean.setPartnerId(ApiConstants.CHAR_SPACE);
        }

        if(null != commonContext){

            auditBean.setReqTimestamp(getRequestTimestamp(commonContext.getTimestamp(), apiMsgId));
        }

        return auditBean;
    }


    /**
     * Utility method to convert the request timestamp to DB timestamp format
     * @param timestamp
     * @param apiMsgId
     * @return
     */
    private String getRequestTimestamp(String timestamp, String apiMsgId){
        String outTime = ApiConstants.CHAR_SPACE;
        SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        SimpleDateFormat out = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try{
            if(null != timestamp && !"".equals(timestamp)){
                Date date = in.parse(timestamp);
                outTime = out.format(date);
            }else{
                outTime = out.format(new Date());
            }
        }catch(ParseException e){
            LOG.error(apiMsgId,"SmartServiceEngine","getRequestTimestamp", "Exception block - getRequestTimestamp",
                " Exception while setting request timestamp to AuditBean for logging", AmexLogger.Result.failure,"Exception while setting request timestamp to AuditBean for logging"
                ,e);
        }
        return outTime;
    }

    /**
     * To populate the Audit Bean with Payment Request/Response Data
     * @param apiMsgId
     * @param actionType
     * @param requestType
     * @param request
     * @param responseType
     * @return
     */
    public AuditVO createPaymentAuditBean(String apiMsgId, String actionType, String requestType, ProcessPaymentRequestType request, ProcessPaymentResponseType paymentResponse){

        AuditVO auditBean = null;
        LOG.debug(apiMsgId, "SmartServiceEngine",
            "Process Payment API - Audit Logging",
            "createPaymentAuditBean method",
            "Start populating AuditBean with payment request/response data", AmexLogger.Result.success,
                "");

        try{

            //Create replica of ProcessPaymentRequestType object to log the JSON payload after removing images
            ObjectMapper mapper = new ObjectMapper();
            String payloadString = mapper.writeValueAsString(request);
            ProcessPaymentRequestType paymentRequest = mapper.readValue(payloadString, ProcessPaymentRequestType.class);


            if(null != paymentRequest && null != paymentRequest.getProcessPaymentRequest() && null != paymentRequest.getProcessPaymentRequest().getData()){

                auditBean = new AuditVO();
                auditBean.setApiMsgId(apiMsgId);

                ProcessPaymentRequestData requestData =  paymentRequest.getProcessPaymentRequest().getData();

                //If the action type is response, get the CommonRequest data, buyer payment ID and payload from Response object
                if(actionType.equals(ApiConstants.RESPONSE) && null != paymentResponse && null != paymentResponse.getProcessPaymentResponse()
                        && null != paymentResponse.getProcessPaymentResponse().getData()){
                    ProcessPaymentResponseData responseData = paymentResponse.getProcessPaymentResponse().getData();

                    auditBean = setCommonContextData(responseData.getCommonResponseContext(), apiMsgId, auditBean);

                    auditBean.setBuyerPaymntId((null != responseData.getBuyerPymtRefId()) ? responseData.getBuyerPymtRefId() : ApiConstants.CHAR_SPACE);

                    //No Binary data (images) to remove from response payload.
                    auditBean.setRequestData(ApiUtil.pojoToJSONString(paymentResponse));

                }else if (actionType.equals(ApiConstants.REQUEST)){ //Else, get CommonRequest data, buyer payment ID and payload from Request
                    auditBean = setCommonContextData(requestData.getCommonRequestContext(), apiMsgId, auditBean);

                    if(null != requestData.getPaymentDetails() && null != requestData.getPaymentDetails().getBuyerPymtRefId()){
                        auditBean.setBuyerPaymntId(requestData.getPaymentDetails().getBuyerPymtRefId());
                    }else{
                        auditBean.setBuyerPaymntId(ApiConstants.CHAR_SPACE);
                    }

                    ProcessPaymentRequestType requestForClob = new ProcessPaymentRequestType();
                    requestForClob.setProcessPaymentRequest(paymentRequest.getProcessPaymentRequest());
                    auditBean.setRequestData(ApiUtil.jsonToString(removeImagesFromPymtRequest(apiMsgId, paymentRequest)));
                }

                //Not applicable for payment. set EnrollAction and enrollCategory to default values
                auditBean.setEnrollActionType(ApiConstants.CHAR_SPACE);
                auditBean.setEnrollCatType(ApiConstants.CHAR_SPACE);

                if(null != requestData.getPaymentDetails()){
                    auditBean.setOrgId((null != requestData.getPaymentDetails().getSupplierOrgId()) ? requestData.getPaymentDetails().getSupplierOrgId() : ApiConstants.CHAR_SPACE);
                    auditBean.setAssOrgId((null != requestData.getPaymentDetails().getBuyerOrgId()) ? requestData.getPaymentDetails().getBuyerOrgId() : ApiConstants.CHAR_SPACE);
                }else{
                    auditBean.setOrgId(ApiConstants.CHAR_SPACE);
                    auditBean.setAssOrgId(ApiConstants.CHAR_SPACE);
                }

                if(null != requestData.getPaymentDetails() && null != requestData.getPaymentDetails().getSupplierAcctDetails() && null != requestData.getPaymentDetails().getSupplierAcctDetails().getAccountId()){
                    auditBean.setAccountList(requestData.getPaymentDetails().getSupplierAcctDetails().getAccountId());
                }else{
                    auditBean.setAccountList(ApiConstants.CHAR_SPACE);
                }

                //Not applicable for Payment API. set default values.
                auditBean.setSubServiceList(ApiConstants.CHAR_SPACE);
                auditBean.setUnsubServiceList(ApiConstants.CHAR_SPACE);

                auditBean.setActionType(actionType);
                auditBean.setRequestType(requestType);
                auditBean.setJvmName(ApiUtil.getHostName());
            }

        }catch(Exception e){
            LOG.error(apiMsgId,"SmartServiceEngine","createPaymentAuditBean", "Exception block - createPaymentAuditBean",
                " Exception while creating AuditBean with process Payment data for logging", AmexLogger.Result.failure,"Exception while creating AuditBean for process payment logging"
                ,e);
        }
        return auditBean;
    }

    /**
     * To remove the images from process Payment API request JSON
     * @param requestData
     * @return
     */
    private ProcessPaymentRequestType removeImagesFromPymtRequest(String apiMsgId, ProcessPaymentRequestType paymentRequest) throws IOException{

        if(null != paymentRequest && null != paymentRequest.getProcessPaymentRequest() && null != paymentRequest.getProcessPaymentRequest().getData()){

            if(null != paymentRequest.getProcessPaymentRequest().getData().getPaymentDetails() &&
                    null != paymentRequest.getProcessPaymentRequest().getData().getPaymentDetails().getPymtDocAttachments()
                    && !paymentRequest.getProcessPaymentRequest().getData().getPaymentDetails().getPymtDocAttachments().isEmpty()){

                for(int i=0; i<paymentRequest.getProcessPaymentRequest().getData().getPaymentDetails().getPymtDocAttachments().size(); i++){
                    if(null != paymentRequest.getProcessPaymentRequest().getData().getPaymentDetails().getPymtDocAttachments().get(i)){
                        paymentRequest.getProcessPaymentRequest().getData().getPaymentDetails().getPymtDocAttachments().get(i).setPymtAttachFile(null);
                    }
                }
            }
        }
        return paymentRequest;
    }
}
