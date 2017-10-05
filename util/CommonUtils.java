/**
 *
 */
package com.americanexpress.smartserviceengine.common.util;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.soap.SOAPMessage;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.w3c.dom.NodeList;

import com.americanexpress.amexlogger.AmexLogger;
import com.americanexpress.smartserviceengine.common.constants.ApiConstants;
import com.americanexpress.smartserviceengine.common.constants.ApiErrorConstants;
import com.americanexpress.smartserviceengine.common.constants.SchedulerConstants;
import com.americanexpress.smartserviceengine.common.enums.ACHBankNameValues;
import com.americanexpress.smartserviceengine.common.payload.CardActivationDetailsType;
import com.americanexpress.smartserviceengine.common.payload.CardActivationRequest;
import com.americanexpress.smartserviceengine.common.payload.CardActivationResponse;
import com.americanexpress.smartserviceengine.common.payload.CardActivationResponseType;
import com.americanexpress.smartserviceengine.common.payload.CardMemberDataType;
import com.americanexpress.smartserviceengine.common.payload.CardMemberDetailsResponse;
import com.americanexpress.smartserviceengine.common.payload.CardMemberDetailsResponseType;
import com.americanexpress.smartserviceengine.common.payload.DropInsertIndType;
import com.americanexpress.smartserviceengine.common.payload.PaymentMethodType;
import com.americanexpress.smartserviceengine.common.payload.PayorAttachmentIdType;
import com.americanexpress.smartserviceengine.common.payload.PayorAttachmentType;
import com.americanexpress.smartserviceengine.common.payload.PymtDocAttachmentsType;
import com.americanexpress.smartserviceengine.common.payload.ServiceType;
import com.americanexpress.smartserviceengine.common.payload.v2.ACHAccount;
import com.americanexpress.smartserviceengine.common.payload.v2.ACHPaymentType;
import com.americanexpress.smartserviceengine.common.payload.v2.AccountDetails;
import com.americanexpress.smartserviceengine.common.payload.v2.CardAccount;
import com.americanexpress.smartserviceengine.common.payload.v2.CheckAccount;
import com.americanexpress.smartserviceengine.common.payload.v2.EnrollmentRequestType;
import com.americanexpress.smartserviceengine.common.payload.v2.PaymentRequestType;
import com.americanexpress.smartserviceengine.common.vo.FetchBankHolidaysResultSetVO;
import com.americanexpress.util.juice.Juice;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * <p>
 * Copyright © 2013 AMERICAN EXPRESS. All Rights Reserved.
 * </p>
 * <p>
 * AMERICAN EXPRESS CONFIDENTIAL. All information, copyrights, trade secrets<br>
 * and other intellectual property rights, contained herein are the property<br>
 * of AMERICAN EXPRESS. This document is strictly confidential and must not be <br>
 * copied, accessed, disclosed or used in any manner, in whole or in part,<br>
 * without Amex's express written authorization.[+-]?(90(\.(00?)?)?|(0?\d|[1-8]\d)(\.(\d\d?)?)?)
 * </p>
 */
public class CommonUtils {
    private static final AmexLogger LOG = AmexLogger.create(CommonUtils.class);

    public static final String alphaNumericRegexPattern = "^[a-zA-Z0-9]+$";

    public static final String alphaNumericHyphenUnderscoreRegexPattern = "^[a-zA-Z0-9\\-_]+$";

    public static final String alphaNumericWithSpaceRegexPattern = "^[a-zA-Z0-9\\s]+$";

    public static final String alphaNumericRegexPattern1 = "^[a-zA-Z0-9\\-]+$";

    public static final String alphaRegexPattern = "^[a-zA-Z]+$";
    public static final String alphaplusCommaRegexPattern = "^[a-zA-Z\\,]+$";

    public static final String numericRegexPattern = "^[0-9]+$";
    public static final String numericZipRegexPattern = "^[0-9\\-]+$";
    
    public static final String numericwithDotRegexPattern = "^[0-9\\.]+$";

    public static final String alphaNumSpaceRegexPattern = "^[a-zA-Z0-9\\s]+$";

    public static final String alphaNumSpaceHyphenRegexPattern = "^[a-zA-Z0-9\\s\\-_]+$";
    public static final String orgIdRegexPattern = "^[a-zA-Z0-9\\s\\.\\-\\[\\?\\\\\\]!|$:_@/(),*%~#^£¢¤¬]+$";
    public static final String alphaNumHyphenPlusRegexPattern = "^[a-zA-Z0-9\\-+]+$";

    public static final String alphaNumHyphenRegexPattern = "^[a-zA-Z0-9\\-]+$";

    public static final String alphaNumHyphenForwardSlashRegexPattern = "^[a-zA-Z0-9\\-/]+$";

    public static final String numericHyphenRegexPattern = "^[0-9\\-]+$";

    public static final String alphaNumericSpclCharRegexPattern = "^[a-zA-Z0-9\\s\\.\\-\\[\\?\\\\\\]"
            + ",()/!\\|\\$:_@\\*%~#\\^£¢¤¬]+$";
    public static final String alphaNumericSpecialCharRegexPattern = "^[a-zA-Z0-9\\s\\.\\-\\?\\\\"
            + ",='+{}`;()/!\\|\\$:_@\\*%~#\\^£¢¤¬]+$";
    public static final String alphaNumericSpclCharPattern = "^[a-zA-Z0-9\\s\\.\\-\\[\\?\\\\\\]"
            + ",()/!\\|\\$:_@\\*%~#\\^]+$";

    public static final String alphaNumericSpclCharRegexPatternFromBIP = "^[a-zA-Z0-9\\s\\.\\-\\[\\?\\\\\\]"
            + ",!\\|\\$:_@\\*%~#\\^£¢¤¬]+$";

    public static final String alphaNumericSpclCharRegexPatternDBAN = "^[a-zA-Z0-9\\s\\.\\-\\[\\?\\\\\\]"
            + ",!\\|\\$:_@\\*\\/()%~#\\^£¢¤¬]+$";

    public static final String alphaSpclCharRegexPattern = "^[a-zA-Z\\s\\.\\-\\[\\?\\\\\\]"
            + ",()/!\\|\\$:_@\\*%~#\\^£¢¤¬]+$";

    public static final String alphaSpclCharRegexPatternFromBIP = "^[a-zA-Z\\s\\.\\-\\[\\?\\\\\\]"
            + ",!\\|\\$:_@\\*%~#\\^£¢¤¬]+$";

    public static final String nameRegexPattern = "^[a-zA-Z\\s\\.\\-\\[\\?\\\\\\]" + ",()/!|$:_@*%~#^'£¢¤¬]";

    public static final String addressLineRegexPattern = "^[a-zA-Z0-9\\s\\.\\-\\[\\?\\\\\\]!|$:_@/(),*%~#^£¢¤¬]{1,40}$";

    public static final String cityNameRegexPattern = "^[a-zA-Z\\s\\.,]+$";

    public static final String stateRegexPattern = "^[a-zA-Z]{1,35}$";

    public static final String countryRegexPattern = "^[a-zA-Z]{1,20}$";

    // public static final String zipcodeRegexPattern =
    // "^\\d{5}$|^\\d{5}-\\d{4}$";

    public static final String emailRegexPattern = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
            + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    public static final String thresholdLimitAmtRegexPattern =
            "(?=.)^\\$?(([1-9][0-9]{0,2}(,[0-9]{3}){1,8})|[0-9]{1,8})?(\\.[0-9]{1,2})?$";

    public static final String expDateRegexPattern = "^(0[1-9]|1[0-2])[0-9][0-9]$";

    public static final String expDateMMYYYYRegexPattern = "^(0[1-9]|1[0-2])[2][0-9][0-9][0-9]$";

    public static final String accountNoRegexPattern = "^[0-9]{3,17}$";

    public static final String routingNoRegexPattern = "^[0-9]{9}$";

    public static final String fractRoutingNoRegexPattern = "^[0-9]{2}+[\\-]+[0-9]{4}+[/]+[0-9]{4}+$";

    public static final String cardMemberCityPatten = "^[a-zA-Z]{1,40}$";

    public static final String binRangeRegexPattern = "^[0-9]{6}$";

    public static final String lastFourRegexPattern = "^[0-9]{4}$";

    public static final String expMonthRegexPattern = "^[0-9]{2}$";

    public static final String expYearRegexPattern = "^[0-9]{4}$";

    //public static final String ipRegexPattern = "^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$";
    public static final String ipRegexPattern = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

    /*
     * ZipCode should have either 5 digits or field length has to be 10 with all digits except at position 6 which
     * should be '
     */
    // public static final String zipCodeRegexPattern =
    // "^\\d{5}$|^\\d{5}-\\d{4}$";
    public static final String zipCodeRegexPattern = "^[a-zA-Z0-9]{5}$|^[a-zA-Z0-9]{5}-[a-zA-Z0-9]{4}$";

    public static final String reqIdRegexPattern = "^[a-zA-Z0-9\\-\\_]{1,30}$";

    public static final String partnerIdRegexPattern = "^[a-zA-Z0-9\\-\\_\\s]{1,36}$";

    public static final String partnerNameRegexPattern = "^[a-zA-Z0-9\\-\\_\\s]{1,40}$";

    public static final String custAliasRegexPattern = "^[a-zA-Z0-9\\-\\_\\s\\.\\@]{1,40}$";

    public static final String cnTokenRegexPattern = "^[a-zA-Z0-9]{40}$";

    // Maximum allowed Payment amount is 99999999.99
    public static final String amountPositiveRegexPattern = "^(?!\\.?$)\\d{0,8}(\\.\\d{0,2})?$";

    // For ACH it is 16,4
    public static final String achAmountPositiveRegexPattern = "^(?!\\.?$)\\d{0,12}(\\.\\d{0,4})?$";

    public static final String amountRegexPattern = "^[-,+]?(?!\\.?$)\\d{0,15}(\\.\\d{0,2})?$";

    public static final String dateYYYYMMDDRegexPattern = "^((19|20)\\d\\d)(0[1-9]|1[012])(0[1-9]|[12][0-9]|3[01])$";

    public static final String dobRegexPattern = "^(1[0-2]|0[1-9])/(3[01]|[12][0-9]|0[1-9])/[0-9]{4}$";

    // public static final String upIdRegexPattern = "^[a-zA-Z0-9/-]{1,35}$";

    public static final String templateNameRegexPattern = "^[a-zA-Z0-9\\-\\_\\s]{1,100}$";

    public static final String returnCodeRegexPattern = "[R]{1}[0-9]{2}";

    public static final String publishedURLRegexPattern = "^(https?|http)://[-a-zA-Z0-9+&/_.]*[-a-zA-Z]";

    public static final String versionNoRegexPattern = "^(?!\\.?$)\\d{0,3}(\\.\\d{0,2})?$";

    public static final String alphaNumericSpclCharRegexPatternForWebActionPath = "^[a-zA-Z0-9\\s\\.\\-\\[\\?\\\\\\]"
            + "/;,()!\\|\\$:_@\\*%~#\\^£¢¤¬]+$";

    public static final String aboNameFieldRegex="^[a-zA-Z\\s\\-']+$";
    
    public static final String cardTypeValue = "CPC;CC";
	public static final String primaryCtct = "TRUE;FALSE";
	public static final String countryCodeCard = "US";
	public static final String cardSalutationRegexPattern ="^(MR|MRS|MS)[\\.]?+$";

	private static final String MM_YY = "MMyy";
    
    public static final String orgPhoneNumberRegexPattern = "^[0-9-.+()/:?]{1,25}+$";
    
    public static final String cardAmountPositiveRegexPattern = "^(?!\\.?$)\\d{0,8}(\\.\\d{0,2})?$";
    public static final String percentageRegexPattern = "^(?!\\.?$)\\d{0,3}(\\.\\d{0,2})?$";

    private CommonUtils() {
        super();
    }

    public static Boolean validateAlphaWithLengthForAboNameField(String alpNumVal, int length) {
        if (alpNumVal == null || "".equals(alpNumVal.trim()) || length < 1 || alpNumVal.length() > length) {
            return false;
        } else {
            return regexValidator(alpNumVal, aboNameFieldRegex);
        }
    }

    public static Boolean validateAlphaNumericWithLengthForWebActionPath(String alpNumVal, int length) {
        if (alpNumVal == null || "".equals(alpNumVal.trim()) || length < 1 || alpNumVal.length() > length) {
            return false;
        } else {
            return regexValidator(alpNumVal, alphaNumericSpclCharRegexPatternForWebActionPath);
        }
    }

    public static Boolean validateAlphaNumericWithLength(String alpNumVal, int length) {
        if (alpNumVal == null || "".equals(alpNumVal.trim()) || length < 1 || alpNumVal.length() > length) {
            return false;
        } else {
            return regexValidator(alpNumVal, alphaNumericRegexPattern);
        }
    }

    public static Boolean validateAlphaNumericHyphenWithLength(String alpNumVal, int length) {
        if (alpNumVal == null || "".equals(alpNumVal.trim()) || length < 1 || alpNumVal.length() > length) {
            return false;
        } else {
            return regexValidator(alpNumVal, alphaNumHyphenRegexPattern);
        }
    }

    public static Boolean validateAlphaNumericHyphenForwardSlashWithLength(String alpNumVal, int length) {
        if (alpNumVal == null || "".equals(alpNumVal.trim()) || length < 1 || alpNumVal.length() > length) {
            return false;
        } else {
            return regexValidator(alpNumVal, alphaNumHyphenForwardSlashRegexPattern);
        }
    }

    public static Boolean validateAlphaNumericHypUnderscoreWithLength(String alpNumVal, int length) {
        if (alpNumVal == null || "".equals(alpNumVal.trim()) || length < 1 || alpNumVal.length() > length) {
            return false;
        } else {
            return regexValidator(alpNumVal, alphaNumericHyphenUnderscoreRegexPattern);
        }
    }

    public static Boolean validateAlphaNumericWithSpaceLength(String alpNumVal, int length) {
        if (alpNumVal == null || "".equals(alpNumVal.trim()) || length < 1 || alpNumVal.length() > length) {
            return false;
        } else {
            return regexValidator(alpNumVal, alphaNumericWithSpaceRegexPattern);
        }
    }

    public static Boolean validateAlphaNumericWithHyphen(String alpNumVal, int length) {
        if (alpNumVal == null || "".equals(alpNumVal.trim()) || length < 1 || alpNumVal.length() > length) {
            return false;
        } else {
            return regexValidator(alpNumVal, alphaNumericRegexPattern1);
        }
    }

    public static Boolean validateNumericHypWithLength(String alpNumVal, int length) {
        if (alpNumVal == null || "".equals(alpNumVal.trim()) || length < 1 || alpNumVal.length() > length) {
            return false;
        } else {
            return regexValidator(alpNumVal, numericHyphenRegexPattern);
        }
    }

    public static Boolean validateAlphaWithLength(String alphaVal, int length) {
        if (alphaVal == null || "".equals(alphaVal.trim()) || length < 1 || alphaVal.length() > length) {
            return false;
        } else {
            return regexValidator(alphaVal, alphaRegexPattern);
        }
    }

    public static Boolean validateAlphaCommaWithLength(String alphaVal, int length) {
        if (alphaVal == null || "".equals(alphaVal.trim()) || length < 1 || alphaVal.length() > length) {
            return false;
        } else {
            return regexValidator(alphaVal, alphaplusCommaRegexPattern);
        }
    }

    public static Boolean validateNumericWithLength(String numVal, int length) {
        if (numVal == null || "".equals(numVal.trim()) || numVal.contains(" ") || length < 1 || numVal.length() > length) {
            return false;
        } else {
            return regexValidator(numVal, numericRegexPattern);
        }
    }

    public static Boolean validateSicCode(String sicCode, int length) {
        if (sicCode.length() > length) {
            return false;
        } else {
            return regexValidator(sicCode, alphaNumericRegexPattern);
        }
    }

    public static Boolean validateNumericDotWithLength(String numVal, int length) {
        if (numVal == null || "".equals(numVal.trim()) || length < 1 || numVal.length() > length) {
            return false;
        } else {
            return regexValidator(numVal, numericwithDotRegexPattern);
        }
    }

    public static Boolean validateNumericValue(String numVal, int length, String apiMsgId) {
        if (numVal == null || "".equals(numVal.trim()) || numVal.length() > length) {
            return false;
        } else if (!isValidNumber(numVal, apiMsgId)) {
            return false;
        } else {
            return regexValidator(numVal, numericRegexPattern);
        }
    }

    public static Boolean hasAchAccount(String cardDetails) {

        Boolean achFlag = false;

        if (cardDetails != null && !cardDetails.isEmpty()) {

            String accArray[] = cardDetails.split("#");

            for (String cardNo : accArray) {

                if (cardNo != null) {
                    achFlag = regexValidator(cardNo, numericRegexPattern);
                }

            }
        }
        return achFlag;
    }

    /*
     * private static boolean isValidNumber(String number, String apiMsgId){ boolean isValidNumber = false; try{
     * isValidNumber = Integer.parseInt(number.trim()) > 0;
     *
     * }catch(NumberFormatException e){ LOG.error( apiMsgId, "SmartServicetEngine", "Exception API",
     * "CommonUtils:isValidNumber", "Invalid Page Index", AmexLogger.Result.failure,
     * "Invalid Page Index. Unable to parse the String"); } return isValidNumber; }
     */

    public static Boolean validateAlpNumHypPlusWithLength(String numVal, int length) {
        if (numVal == null || "".equals(numVal.trim()) || length < 1 || numVal.length() > length) {
            return false;
        } else {
            return regexValidator(numVal, alphaNumHyphenPlusRegexPattern);
        }
    }

    public static Boolean validateAlpNumSpaceHypWithLength(String numVal, int length) {
        if (numVal == null || "".equals(numVal.trim()) || length < 1 || numVal.length() > length) {
            return false;
        } else {
            return regexValidator(numVal, alphaNumSpaceHyphenRegexPattern);
        }
    }

    public static Boolean validateAlpNumSpaceWithLength(String stringVal, int length) {
        if (stringVal == null || "".equals(stringVal.trim()) || length < 1 || stringVal.length() > length) {
            return false;
        } else {
            return regexValidator(stringVal, alphaNumSpaceRegexPattern);
        }
    }

    public static Boolean validateAlpNumSpaceHypWithLengthForOrgId(String orgId, int length) {
        if (orgId == null || "".equals(orgId.trim()) || length < 1 || orgId.length() > length) {
            return false;
        } else {
            return regexValidator(orgId, orgIdRegexPattern);
        }
    }

    public static Boolean validateAlpNumSpclCharWithLength(String numVal, int length) {
        if (numVal == null || "".equals(numVal.trim()) || length < 1 || numVal.length() > length) {
            return false;
        } else {
            return regexValidator(numVal, alphaNumericSpclCharPattern);
        }
    }

    public static Boolean validateAlphaNumSpclCharWithLength(String numVal, int length) {
        if (numVal == null || "".equals(numVal.trim()) || numVal.length() < 1 || numVal.length() > length) {
            return false;
        } else {
            return regexValidator(numVal, alphaNumericSpclCharRegexPattern);
        }
    }

    public static Boolean validateAlphaNumericSpclCharWithLength(String numVal, int length) {
        if (numVal == null || "".equals(numVal.trim()) || length < 1 || numVal.length() > length) {
            return false;
        } else {
            return regexValidator(numVal, alphaNumericSpclCharRegexPattern);
        }
    }

    public static Boolean validateAlphaSpclCharWithLength(String numVal, int length) {
        if (numVal == null || "".equals(numVal.trim()) || length < 1 || numVal.length() > length) {
            return false;
        } else {
            return regexValidator(numVal, alphaSpclCharRegexPattern);
        }
    }

    public static Boolean validateAlphaNumericSpclCharWithLengthFromBIP(String numVal, int length) {
        if (numVal == null || "".equals(numVal.trim()) || length < 1 || numVal.length() > length) {
            return false;
        } else {
            return regexValidator(numVal, alphaNumericSpclCharRegexPatternFromBIP);
        }
    }

    public static Boolean validateAlphaNumericSpclCharWithLengthDBAN(String numVal, int length) {
        if (numVal == null || "".equals(numVal.trim()) || length < 1 || numVal.length() > length) {
            return false;
        } else {
            return regexValidator(numVal, alphaNumericSpclCharRegexPatternDBAN);
        }
    }

    public static Boolean validateAlphaSpclCharWithLengthFromBIP(String numVal, int length) {
        if (numVal == null || "".equals(numVal.trim()) || length < 1 || numVal.length() > length) {
            return false;
        } else {
            return regexValidator(numVal, alphaSpclCharRegexPatternFromBIP);
        }
    }

    public static Boolean validateThresholdLimitAmount(String thresholdLimitAmt) {
        if (thresholdLimitAmt == null || "".equals(thresholdLimitAmt.trim())) {
            return false;
        } else {
            return regexValidator(thresholdLimitAmt, thresholdLimitAmtRegexPattern);
        }
    }

    public static Boolean validateAmount(String amount, String apiMsgId) {
        if (amount == null || "".equals(amount.trim())) {
            return false;
        }else if(!regexValidator(amount, amountRegexPattern)){
        	return false;
        }
        else if (!isValidAmountWithNeg(amount, apiMsgId)) {
        	LOG.info("negative chk");
            return false;
        } else {
        	/*LOG.info("reg chk");
            return regexValidator(amount, amountRegexPattern);*/
        	return true;
        }
    }

    public static Boolean validatePositiveAmount(String amount, String apiMsgId) {
        if (amount == null || "".equals(amount.trim())) {
            return false;
        } else if (!isValidAmount(amount, apiMsgId)) {
            return false;
        } else {
            return regexValidator(amount, amountPositiveRegexPattern);
        }
    }

    public static Boolean validatePositiveAmountForACH(String amount, String apiMsgId) {
        if (amount == null || "".equals(amount.trim())) {
            return false;
        } else if (!isValidAmount(amount, apiMsgId)) {
            return false;
        } else {
            return regexValidator(amount, achAmountPositiveRegexPattern);
        }
    }

    private static boolean isValidAmount(String amount, String apiMsgId) {
        boolean isValidAmount = false;
        try {
            isValidAmount = Double.parseDouble(amount.trim()) > 0;

        } catch (NumberFormatException e) {
            LOG.error(apiMsgId, "SmartServiceEngine", "Payment API", "CommonUtils:isValidAmount",
                "Invalid Payment Amount", AmexLogger.Result.failure,
                    "Invalid Payment Amount. Unable to parse the amount String");
        }
        return isValidAmount;
    }

    private static boolean isValidAmountWithNeg(String amount, String apiMsgId) {
        boolean isValidAmount = false;
        try {
            if (!Double.isNaN(Double.parseDouble(amount))) {
                isValidAmount = true;
            }
        } catch (NumberFormatException e) {
            LOG.error(apiMsgId, "SmartServiceEngine", "Payment API", "CommonUtils:isValidAmount",
                "Invalid Payment Amount", AmexLogger.Result.failure,
                    "Invalid Payment Amount. Unable to parse the amount String");
        }
        return isValidAmount;
    }

    public static Boolean validateZipCode(String cmZipCode) {

        if (isEmpty(cmZipCode)) {
            return false;
        } else {
            return regexValidator(cmZipCode, zipCodeRegexPattern);
        }
    }

    public static Boolean validateOrgAddressPostalCodeWithLength(String numVal, int length) {
        if (numVal == null || "".equals(numVal.trim()) || length < 1 || numVal.length() > length) {
            return false;
        } else {
            return regexValidator(numVal, numericZipRegexPattern);
        }
    }

    public static Boolean validateRequestId(String requestId) {
        if (requestId == null || ("".equals(requestId.trim()))) {
            return false;
        } else {
            return regexValidator(requestId, reqIdRegexPattern);
        }
    }

    /**
     * Method to validate the Partner Name
     *
     * @param partnerName
     * @return
     */
    public static Boolean validatePartnerName(String partnerName, String apiMsgId) {

        boolean blnPartnerNameNotFound = false;
        String propPartnerNames = null;
        if (partnerName == null || ("".equals(partnerName.trim()))
                || !regexValidator(partnerName, partnerNameRegexPattern)) {
            return false;
        } else {
            /*
             * Check if PartnerName is configured in SSE Properties file
             */
            propPartnerNames = EnvironmentPropertiesUtil.getProperty(ApiConstants.PARTNER_NAMES);
            if (propPartnerNames != null && propPartnerNames.indexOf(";") != -1) {
                String partnerNameList[] = propPartnerNames.split(";");
                for (int i = 0; i < partnerNameList.length; i++) {
                    String propPartnerName = partnerNameList[i];

                    if (propPartnerName.equalsIgnoreCase(partnerName)) {
                        return true;
                    } else {
                        blnPartnerNameNotFound = true;
                    }
                }
            }

            if (blnPartnerNameNotFound) {
                LOG.error(apiMsgId, "SmartServiceEngine", "Enrollment API Service", "CommonUtils:validatePartnerId",
                    "Invalid Request Object", AmexLogger.Result.failure,
                    "Partner Name is not configured in PAYMENT_NAMES property", "partnerName", partnerName);

            } else {
                LOG.error(apiMsgId, "SmartServiceEngine", "Enrollment API Service", "CommonUtils:validatePartnerId",
                    "Invalid Request Object", AmexLogger.Result.failure,
                    "Property PAYMENT_NAMES is not configured properly in SSE Properties file", "PAYMENT_NAMES value",
                    propPartnerNames);
            }

        }
        return false;

    }

    public static Boolean validateTimestamp(String timestamp) {
        if (timestamp == null) {
            return false;
        } else {
            try {
                // Example timestamp: 2013-12-13T11:10:00.715-05:00
                String trimTimestamp = null;
                trimTimestamp = timestamp.trim();
                if (trimTimestamp.length() != 29) {
                    return false;
                }
                XMLGregorianCalendar xmlCalendar = DateTimeUtil.toXMLGregorianCalendar(trimTimestamp);

                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
                format.setLenient(false);

                if (xmlCalendar != null) {

                    format.setLenient(false);
                    format.parse(trimTimestamp);
                    return true;

                } else {
                    return false;
                }

            } catch (Exception e) {
                return false;
            }
        }

    }

    public static boolean validateACHEmail(String emailAddress, boolean blnSupplier) {
        boolean result = true;

        if (blnSupplier) {
            LOG.info("Inside validating Email for Supplier");
            if (emailAddress != null && (emailAddress.length() > 0 && emailAddress.length() < 255)) {
                LOG.info("Inside succesful validation of first if check for Supplier");
                result = regexValidator(emailAddress, emailRegexPattern);
            } else {
                result = false;
            }
        } else {
            LOG.info("Inside validating Email for Customer");
            //if (emailAddress != null) {
            //since email id is optional for customer , we allow empty and spaces
            if (StringUtils.isNotBlank(emailAddress)) {
            	if(!(emailAddress.length() > 0 && emailAddress.length() < 255)){
            		return false;
            	}
                LOG.info("Inside succesful validation of first if check for Customer");
                result = regexValidator(emailAddress, emailRegexPattern);
            }
        }

        return result;
    }

    public static boolean validateEmail(String emailAddress, boolean blnSupplier) {

        if (blnSupplier) {

            if (emailAddress == null || emailAddress.length() > 254) {
                return false;
            } else {
                return regexValidator(emailAddress, emailRegexPattern);
            }
        } else {

            if (emailAddress == null || emailAddress.length() > 50) {
                return false;
            } else {
                return regexValidator(emailAddress, emailRegexPattern);
            }
        }
    }

    public static boolean validateEmail(String emailAddress) {
    	if(emailAddress == null){
    		return false;
    	}else if (emailAddress != null && StringUtils.isBlank(emailAddress.trim())) {
            return false;
        } else if (emailAddress.trim().length() > 254) {
            return false;
        } else {
            return regexValidator(emailAddress.trim(), emailRegexPattern);
        }
    }
    
    public static boolean validateUpdateEmail(String emailAddress) {

        if (emailAddress.trim().length() > 254) {
           return false;
       } else {
           return regexValidator(emailAddress.trim(), emailRegexPattern);
       }
   }

    public static boolean isEmpty(String value) {

        return value == null || "".equals(value.trim());

    }

    public static Boolean validateEnrollActionType(String enrollActionType) {
        boolean result = false;
        if (enrollActionType != null
                && (enrollActionType.equalsIgnoreCase(ApiConstants.ACTION_TYPE_ADD)
                        || enrollActionType.equalsIgnoreCase(ApiConstants.ACTION_TYPE_UPDATE) || enrollActionType
                        .equalsIgnoreCase(ApiConstants.ACTION_TYPE_DELETE))) {
            result = true;
        }
        return result;
    }

    public static Boolean validateACHEnrollmentCategory(String enrollCategory) {
        boolean result = false;
        if (enrollCategory != null
                && (enrollCategory.equals(ApiConstants.CATEGORY_CUST_ENROLL)
                        || enrollCategory.equalsIgnoreCase(ApiConstants.CATEGORY_SUPP_ENROLL) || enrollCategory
                        .equalsIgnoreCase(ApiConstants.CATEGORY_ACCT_ENROLL))) {
            result = true;
        }
        return result;
    }

    public static Boolean validateEnrollmentCategory(String enrollCategory) {
        boolean result = false;
        if (StringUtils.isBlank(enrollCategory)) {
            result = true;
        } else if (ApiConstants.CATEGORY_CUST_ENROLL.equals(enrollCategory)
                || ApiConstants.CATEGORY_SUPP_ENROLL.equals(enrollCategory)
                || ApiConstants.CATEGORY_ACCT_ENROLL.equals(enrollCategory)
                || ApiConstants.CATEGORY_CUST_ACCT_ENROLL.equals(enrollCategory)
                || ApiConstants.CATEGORY_SUPP_ACCT_ENROLL.equals(enrollCategory)) {
            result = false;
        } else {
            result = true;
        }
        return result;
    }

    public static Boolean validateActiveOrgStatus(String orgStatus) {
        boolean result = false;
        if (orgStatus != null && (orgStatus.equalsIgnoreCase(ApiConstants.STATUS_ACTIVE))) {
            result = true;
        }
        return result;
    }

    public static Boolean validateActiveAccountStatus(String accStatus) {
        boolean result = false;
        if (accStatus != null && (accStatus.equalsIgnoreCase(ApiConstants.STATUS_ACTIVE) || accStatus
                        .equalsIgnoreCase(ApiConstants.CHAR_S))) {
            result = true;
        }
        return result;
    }

    public static Boolean validateAccountType(String accountType, int length) {
        boolean result = false;
        if (accountType != null && accountType.length() <= length
                && (ApiConstants.ACCT_TYPE_CHECK.equalsIgnoreCase(accountType))) {
            result = true;
        }
        return result;
    }

    public static Boolean validatePaymentMethod(String paymentMtd) {
        boolean result = false;
        if (paymentMtd != null
                && paymentMtd.trim() != ""
                && (PaymentMethodType.ACH.getPaymentMethod().equalsIgnoreCase(paymentMtd) ||
                    	PaymentMethodType.CHECK.getPaymentMethod().equalsIgnoreCase(paymentMtd) || 
                    	PaymentMethodType.CARD.getPaymentMethod().equalsIgnoreCase(paymentMtd)) ) {
        	result = true;
        }
        return result;
    }

    public static Boolean validateServiceCode(String serviceCode) {
        boolean result = false;

        if (serviceCode != null
                && serviceCode.trim() != ""
                && (ApiConstants.SERVICE_CODE_CHECK.equals(serviceCode)
                        || ApiConstants.SERVICE_CODE_CARD.equals(serviceCode)
                        || ApiConstants.SERVICE_CODE_ACH.equals(serviceCode) || ApiConstants.SERVICE_CODE_IWIRE
                        .equals(serviceCode))) {

            result = true;
        }
        return result;
    }

    //TODO All configurations should come from DB and loaded into cache at the time of server startup.
    public static boolean validateSupportedServiceCode(String serviceCode, String partnerName) {
    	boolean isValid = false;
    	String[] subscriptions = PartnerSubscriptions.getSubscriptions(partnerName);
    	if(subscriptions != null && subscriptions.length > 0){
    		for(String subscription : subscriptions){
    			if(subscription.equalsIgnoreCase(serviceCode)){
    				isValid = true;
    				break;
    			}
    		}
    	}
        return isValid;
    }

    public static Boolean validateSupportedSupplierFeeInd(String suppFeeInd) {
        boolean result = false;

        if (ApiConstants.CHAR_Y.equalsIgnoreCase(suppFeeInd) || ApiConstants.CHAR_N.equalsIgnoreCase(suppFeeInd)) {

            result = true;
        }
        return result;
    }

    public static Boolean validateDuplicateServiceCodes(List<ServiceType> serviceCodes) {
        List<String> sc = new ArrayList<String>();

        for (ServiceType serviceType : serviceCodes) {

            sc.add(serviceType.getServiceCode());
        }

        Set<String> set = new HashSet<String>(sc);

        LOG.info("# of serviceCodes provided" + sc.size());
        LOG.info("# of unique serviceCodes provided" + set.size());

        return !(set.size() < serviceCodes.size());
    }

    public static Boolean validateServiceCodeList(List<ServiceType> serviceCodes) {

        if (serviceCodes != null && !serviceCodes.isEmpty()) {
            for (ServiceType serviceCode : serviceCodes) {

                boolean validServiceCode = validateServiceCode(serviceCode.getServiceCode());
                if (!validServiceCode) {
                    return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }

    public static Boolean validateSupportedServiceCodeList(List<ServiceType> serviceCodes, String partnerName) {

        if (serviceCodes != null && !serviceCodes.isEmpty()) {
            for (ServiceType serviceCode : serviceCodes) {

                boolean validServiceCode = validateSupportedServiceCode(serviceCode.getServiceCode(), partnerName);
                if (!validServiceCode) {
                    return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }

    public static Boolean validateCustomerFeeBill(String custFeeBill) {
        boolean result = false;
        if (custFeeBill != null
                && (custFeeBill.equalsIgnoreCase(ApiConstants.CHAR_Y) || custFeeBill.equalsIgnoreCase(ApiConstants.CHAR_N) || custFeeBill
                        .equals(ApiConstants.CHAR_BLANKSPACE))) {
            result = true;
        }
        return result;
    }

    public static Boolean validateCheckNumberingInd(String checkNumberingInd) {
        boolean result = false;
        if (checkNumberingInd.equalsIgnoreCase(ApiConstants.CHAR_Y)
                || checkNumberingInd.trim().equals(ApiConstants.CHAR_BLANKSPACE)) {
            result = true;
        }
        return result;
    }

    public static Boolean validateCheckPrintServiceInd(String checkPrintServiceInd) {
        boolean result = false;
        if (checkPrintServiceInd != null
                && (checkPrintServiceInd.equalsIgnoreCase(ApiConstants.CHAR_S) || checkPrintServiceInd
                        .equalsIgnoreCase(ApiConstants.CHAR_N))) {
            result = true;
        }
        return result;
    }

    public static Boolean validateFastForwardServiceInd(String fastForwardServiceInd) {
        boolean result = false;
        if (fastForwardServiceInd != null
                && (fastForwardServiceInd.equalsIgnoreCase(ApiConstants.CHAR_FF) || fastForwardServiceInd
                        .equalsIgnoreCase(ApiConstants.CHAR_STD))) {
            result = true;
        }
        return result;
    }

    public static Boolean validateSupplierNotificationInd(String supplierNotificationInd) {
        boolean result = false;
        if (supplierNotificationInd != null && supplierNotificationInd.equalsIgnoreCase(ApiConstants.CHAR_N)) {
            result = true;
        }
        return result;
    }

    public static Boolean validateHoldInd(String holdInd) {
        boolean result = false;
        if (holdInd.equalsIgnoreCase(ApiConstants.CHAR_BLANKSPACE) || holdInd.equalsIgnoreCase(ApiConstants.CHAR_N)) {
            result = true;
        }
        return result;
    }

    public static Boolean validateMailVendorMethodCd(String mailVendorMethodCd) {

        return mailVendorMethodCd != null && ApiConstants.mailVendorMtdCodes.contains(mailVendorMethodCd.trim());

    }

    public static Boolean validateMailVendor(String mailVendor) {

        return mailVendor != null && ApiConstants.mailVendors.contains(mailVendor.trim());

    }

    public static Boolean validateDeliveryMethod(String delMethod) {
        return delMethod != null && ApiConstants.delMethods.contains(delMethod.trim());
    }

    public static Boolean validateRoutingNo(String routingNo) {
        if (StringUtils.isBlank(routingNo)) {
            return false;
        } else {
            return regexValidator(routingNo, routingNoRegexPattern);
        }
    }

    public static Boolean validateAccountNo(String accountNumber) {
        if (StringUtils.isBlank(accountNumber)) {
            return false;
        } else {
            return regexValidator(accountNumber, accountNoRegexPattern);
        }
    }

    public static Boolean validateFractionalRoutingNo(String fractionalRoutingNo) {

        return fractionalRoutingNo != null && !"".equalsIgnoreCase(fractionalRoutingNo.trim());
    }

    public static Boolean validateEnableThreshold(String enableThreshold) {
        boolean result = false;
        if (enableThreshold != null
                && enableThreshold.trim().length() > 0
                && (ApiConstants.CHAR_Y.equalsIgnoreCase(enableThreshold) || ApiConstants.CHAR_N
                        .equalsIgnoreCase(enableThreshold))) {
            result = true;
        }
        return result;
    }

    public static Boolean validateIWireSettlementMethod(String iwireSettlementMtd) {
        boolean result = false;
        if (iwireSettlementMtd != null
                && ("AC".equalsIgnoreCase(iwireSettlementMtd) || "WF".equalsIgnoreCase(iwireSettlementMtd))) {
            result = true;
        }
        return result;
    }

    // This is temp solution to convert 3 CHAR country code to 2 CHAR, for
    // some DB logging purpose.
    // As we will move to more international markets we need to implement
    // rigid solution to this using some property file mapping etc.
    private static final Map<String, String> cntryCdMap = Collections.unmodifiableMap(new HashMap<String, String>() {
        {
            put("USA", "US");
        }
    });

    private static final Map<String, String> PymtErrCdMap = Collections.unmodifiableMap(new HashMap<String, String>() {
        {
            put("SSEAPIST001", "P01");
            put("SSEAPIEN046", "P02");
            put("SSEAPIEN044", "P03");
            put("SSEAPIEN192", "P04");
        }
    });

    public static Boolean validateFirstLastName(String city, int length) {
        if (city == null || "".equals(city.trim()) || length < 1 || city.length() > length) {
            return false;
        } else {
            return regexValidator(city, nameRegexPattern);
        }
    }

    public static Boolean validateAlphaNumbericWithSymbols(String string, int length) {
        if (string == null || "".equals(string.trim()) || length < 1 || string.length() > length) {
            return false;
        } else {
            return regexValidator(string, nameRegexPattern);
        }
    }

    /**
     * Validate the Partner Id
     *
     * @param partnerId
     * @return
     */
    public static Boolean validatePartnerId(String partnerId, String apiMsgId) {

        if (partnerId == null || ("".equals(partnerId.trim())) || !regexValidator(partnerId, partnerIdRegexPattern)) {
            return false;
        } else {

            return true;

        }
    }

    public static Boolean validateCountryCode(String countryCode) {
        if (countryCode == null) {
            return false;
        } else {
            // TODO Right now USA is the only valid country, once the
            // partnership goes international we need to change the logic here
            return ApiConstants.COUNTRY_USA.equals(countryCode);
        }
    }

    public static Boolean validateCurrencyCode(String currencyCode) {
        if (currencyCode == null) {
            return false;
        } else {
            // TODO Right now USD is the only valid country, once the
            // partnership goes international we need to change the logic here
            return ApiConstants.CURRENCY_USD.equals(currencyCode);
        }
    }

    /*
     * public static Boolean validateCurrencyAmount(String currencyAmount) { if (currencyAmount == null) { return false;
     * } else { try { new BigDecimal(currencyAmount); return true; } catch (Exception e) { return false; } } }
     */

    private static Boolean regexValidator(String validateString, String regExPattern) {
    	boolean isValid=false;
    	if(validateString != null)
    	{  
    		validateString=validateString.trim();
	        Pattern patternObj = Pattern.compile(regExPattern);
	        Matcher matchObj = patternObj.matcher(validateString);
	        if (matchObj.matches()) {
	            isValid=true;
	        }
    	}
    	return isValid;
    }

    public static String getAlpha2CntryCode(String input) {
        String output = "";
        if (cntryCdMap.containsKey(input)) {
            output = cntryCdMap.get(input);
        } else {
            // TODO Returning Empty String in case key doesn't exit in map to
            // prevent database insertion error
            LOG.error("Corresponding 2 CHAR Alpha Country Code doesn't exist for : " + input);
        }
        return output;
    }

    public static String getRtrnRsnCd(String input) {
        String output = "";
        if (PymtErrCdMap.containsKey(input)) {
            output = PymtErrCdMap.get(input);
        } else {
            LOG.error("Corresponding 3 CHAR error Code doesn't exist for : " + input
                + " Sending Internal Server Error error code");
            output = "P01";
        }
        return output;
    }

    public static boolean validateCity(String cityName, int length) {
        if (isEmpty(cityName) || length < 1 || cityName.length() > length) {
            return false;
        } else {
            return regexValidator(cityName, cityNameRegexPattern);
        }
    }

    public static String getRandomId() {
        return Long.toString(UUID.randomUUID().getMostSignificantBits());
    }

    public static Boolean validateUSACountryName(String countryName) {
        return countryName != null && ApiConstants.COUNTRY_USA.equalsIgnoreCase(countryName);
    }

    public static Boolean validateUSAState(String stateCode) {
        return stateCode != null && ApiConstants.usStateCodeList.contains(stateCode.toUpperCase());
    }

    public static String currentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMddyyyy");
        return sdf.format(System.currentTimeMillis());
    }

    public static String getMessageId(int len) {

        String randomStr = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(randomStr.charAt(rnd.nextInt(randomStr.length())));
        }
        SimpleDateFormat sdfcurr = new SimpleDateFormat("ddHHmmss");
        String timestampCurr = sdfcurr.format(System.currentTimeMillis());

        return timestampCurr + sb.toString();

    }

    public static String getPayvePartnerEntityId(String partnerId) {
    	Map<String, String> partnerIndicatorMap = ApiUtil.readPropertiesMap(ApiConstants.SSE_PAYVE_PARTNERID_MAP);
		return partnerIndicatorMap.get(partnerId);
    }

    public static String validatePayorAttachments(List<PayorAttachmentType> payorAttachments) {
        String errorCode = null;

        if (payorAttachments.size() > 5) {
            errorCode = ApiErrorConstants.SSEAPIEN106;

            return errorCode;
        }

        Set<String> duplAttachmentIds = CommonUtils.findDuplicateAttachmentIds(payorAttachments);
        if (!duplAttachmentIds.isEmpty()) {
            errorCode = ApiErrorConstants.SSEAPIEN032;

            return errorCode;
        }

        for (PayorAttachmentType payorAttach : payorAttachments) {
            String attachId = payorAttach.getAttachmentId();
            byte[] attachFile = payorAttach.getAttachmentFile();

            if (StringUtils.isNotBlank(attachId) && !CommonUtils.validateAlphaNumSpclCharWithLength(attachId, 40)) {
                errorCode = ApiErrorConstants.SSEAPIEN147;
                return errorCode;
            }
            if (StringUtils.isBlank(attachId) && attachFile != null) {
                errorCode = ApiErrorConstants.SSEAPIEN033;
                return errorCode;

            }
            if (StringUtils.isBlank(attachId) && (attachFile == null || attachFile.length <1 )) {
                errorCode = ApiErrorConstants.SSEAPIEN035;
                return errorCode;
            }

        }
        return errorCode;

    }

    public static Set<String> findDuplicateAttachmentIds(List<PayorAttachmentType> payorAttachments) {

        final Set<String> duplicateAttachmentIds = new HashSet<String>();
        final Set<String> uniqueSet = new HashSet<String>();

        for (PayorAttachmentType payorAttach : payorAttachments) {
            if (!uniqueSet.add(payorAttach.getAttachmentId())) {
                duplicateAttachmentIds.add(payorAttach.getAttachmentId());
            }
        }
        return duplicateAttachmentIds;
    }

    public static Set<String> findDuplicatePymtAttachmentIds(List<PymtDocAttachmentsType> pymtDocAttachments) {

        final Set<String> duplicateAttachmentIds = new HashSet<String>();
        final Set<String> uniqueSet = new HashSet<String>();

        for (PymtDocAttachmentsType pymtDocAttachment : pymtDocAttachments) {
            if (pymtDocAttachment.getPymtAttachId() != null && !uniqueSet.add(pymtDocAttachment.getPymtAttachId())) {
                duplicateAttachmentIds.add(pymtDocAttachment.getPymtAttachId());
            }
        }
        return duplicateAttachmentIds;
    }

    public static Set<String> findDuplicatePayorAttachmentIds(List<PayorAttachmentIdType> payorAttachments) {

        final Set<String> duplicateAttachmentIds = new HashSet<String>();
        final Set<String> uniqueSet = new HashSet<String>();

        for (PayorAttachmentIdType payorAttach : payorAttachments) {
            if (payorAttach.getPayorAttachmentId() != null && !uniqueSet.add(payorAttach.getPayorAttachmentId())) {
                duplicateAttachmentIds.add(payorAttach.getPayorAttachmentId());
            }
        }
        return duplicateAttachmentIds;
    }

    public static Set<String> findDuplicateDropInsertInd(List<DropInsertIndType> dropInsertindicators) {

        final Set<String> duplicateDropInsertIndSet = new HashSet<String>();
        final Set<String> uniqueSet = new HashSet<String>();

        for (DropInsertIndType dropInd : dropInsertindicators) {
            if (dropInd.getDropInsertIndicatorId() != null && !uniqueSet.add(dropInd.getDropInsertIndicatorId())) {
                duplicateDropInsertIndSet.add(dropInd.getDropInsertIndicatorId());
            }
        }
        return duplicateDropInsertIndSet;
    }

    public static Boolean validateOverridePymtMtd(String overridePymtMtd) {
        boolean result = false;
        if (overridePymtMtd != null
                && (overridePymtMtd.equalsIgnoreCase(ApiConstants.CHAR_Y)
                        || overridePymtMtd.equalsIgnoreCase(ApiConstants.CHAR_N) || overridePymtMtd.trim()
                        .equals(ApiConstants.CHAR_BLANKSPACE))) {
            result = true;
        }
        return result;
    }

    public static Boolean validateDateFormat(String dateString) {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");

        try {
            format.setLenient(false);
            format.parse(dateString);
            return true;
        } catch (ParseException e) {
            return false;
        }

    }

    public static Boolean validateDateFormatYYYYMMDD(String dateString) {
        if (dateString == null || isEmpty(dateString)) {
            return false;
        } else {
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
            try {
                format.setLenient(false);
                format.parse(dateString);
                return regexValidator(dateString, dateYYYYMMDDRegexPattern);

            } catch (ParseException e) {
                return false;
            }

        }

    }

    public static Boolean validateTwinDates(String dateString1, String dateString2) {

        if (dateString1 == null || isEmpty(dateString1) || dateString2 == null || isEmpty(dateString2)) {
            return false;
        } else {
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
            try {
                format.setLenient(false);
                Date d1 = format.parse(dateString1);
                Date d2 = format.parse(dateString2);
                return !(d2.before(d1));

            } catch (ParseException e) {
                return false;
            }

        }

    }

    public static Boolean validateHandlingSLA(String handlingSLAInd) {
        boolean result = false;
        if (handlingSLAInd != null
                && (handlingSLAInd.equalsIgnoreCase(ApiConstants.CHAR_S) || handlingSLAInd
                        .equalsIgnoreCase(ApiConstants.CHAR_N))) {
            result = true;
        }
        return result;
    }

    public static Boolean validateStatusCategory(String statusCategory) {
        boolean result = false;
        if (statusCategory != null
                && (statusCategory.equals(ApiConstants.CATEGORY_STATUS_ORG)
                        || statusCategory.equalsIgnoreCase(ApiConstants.CATEGORY_STATUS_ACCOUNT) || statusCategory
                        .equalsIgnoreCase(ApiConstants.CATEGORY_STATUS_PAYMENT))) {
            result = true;
        }
        return result;
    }

    public static Boolean validateOrgCategory(String orgCategory) {
        boolean result = false;
        if (orgCategory != null
                && (orgCategory.equalsIgnoreCase(ApiConstants.CATEGORY_ORG_CUSTOMER) || orgCategory
                        .equalsIgnoreCase(ApiConstants.CATEGORY_ORG_SUPPLIER))) {
            result = true;
        }
        return result;
    }

    public static Boolean validateBinaryDataIndicator(String binaryDataInd) {
        boolean result = false;
        if (binaryDataInd != null
                && (binaryDataInd.equalsIgnoreCase(ApiConstants.CHAR_Y) || binaryDataInd
                        .equalsIgnoreCase(ApiConstants.CHAR_N))) {
            result = true;
        }
        return result;
    }

    public static String getDescFromStatusCd(String orgStatusCd) {
        String orgStatusDesc = "";
        if (null != orgStatusCd) {
            if (ApiConstants.STATUS_INPROGRESS.equalsIgnoreCase(orgStatusCd)) {
                orgStatusDesc = ApiConstants.STATUSDESC_INPROGRESS;
            } else if (ApiConstants.STATUS_ACTIVE.equalsIgnoreCase(orgStatusCd)) {
                orgStatusDesc = ApiConstants.STATUSDESC_ACTIVE;
            } else if (ApiConstants.STATUS_INACTIVE.equalsIgnoreCase(orgStatusCd)) {
                orgStatusDesc = ApiConstants.STATUSDESC_INACTIVE;
            } else if (ApiConstants.STATUS_VERIFICATION_FAILED.equalsIgnoreCase(orgStatusCd)) {
                orgStatusDesc = ApiConstants.STATUSDESC_VERIFICATION_FAILED;
            } else if (ApiConstants.STATUS_HOLD.equalsIgnoreCase(orgStatusCd)) {
                orgStatusDesc = ApiConstants.STATUSDESC_HOLD;
            }else {
                orgStatusDesc = " ";
            }
        }
        return orgStatusDesc;
    }

    public static String validatePymtDocAttachments(List<PymtDocAttachmentsType> pymtDocAttachments) {
        String errorCode = null;

        if (pymtDocAttachments.size() > 5) {
            errorCode = ApiErrorConstants.SSEAPIPY017;

            return errorCode;
        }

        Set<String> duplAttachmentIds = CommonUtils.findDuplicatePymtAttachmentIds(pymtDocAttachments);
        if (!duplAttachmentIds.isEmpty()) {
            errorCode = ApiErrorConstants.SSEAPIPY049;

            return errorCode;
        }

        for (PymtDocAttachmentsType payorAttach : pymtDocAttachments) {
            String attachId = payorAttach.getPymtAttachId();
            byte[] attachFile = payorAttach.getPymtAttachFile();

            if ((StringUtils.isBlank(attachId) && attachFile != null) || (attachId != null && attachFile == null)) {
                errorCode = ApiErrorConstants.SSEAPIPY050;
                return errorCode;

            }
            if (attachId != null && !attachId.isEmpty() && !CommonUtils.validateAlpNumSpclCharWithLength(attachId, 40)) {
                errorCode = ApiErrorConstants.SSEAPIPY007;
                return errorCode;
            }

        }
        return errorCode;

    }

    public static String getStatusDescFromStatusCd(String statusCd) {
        String orgStatusDesc = null;
        if (null != statusCd) {
            if (ApiConstants.STATUS_INPROGRESS.equalsIgnoreCase(statusCd)
                    || ApiConstants.STATUS_BIP_INPROGRESS.equalsIgnoreCase(statusCd)) {
                orgStatusDesc = ApiConstants.STATUSDESC_INPROGRESS;
            } else if (ApiConstants.STATUS_ACTIVE.equalsIgnoreCase(statusCd)
                    || ApiConstants.STATUS__BIP_ACTIVE.equalsIgnoreCase(statusCd)) {
                orgStatusDesc = ApiConstants.STATUSDESC_ACTIVE;
            } else if (ApiConstants.STATUS_INACTIVE.equalsIgnoreCase(statusCd)
                    || ApiConstants.STATUS_BIP_INACTIVE.equalsIgnoreCase(statusCd)) {
                orgStatusDesc = ApiConstants.STATUSDESC_INACTIVE;
            } else if (ApiConstants.STATUS_VERIFICATION_FAILED.equalsIgnoreCase(statusCd)
                    || ApiConstants.STATUS_BIP_VERIFICATION_FAILED.equalsIgnoreCase(statusCd)) {
                orgStatusDesc = ApiConstants.STATUSDESC_VERIFICATION_FAILED;
            } else if (ApiConstants.STATUS_FAILED.equalsIgnoreCase(statusCd)) {
                orgStatusDesc = ApiConstants.STATUSDESC_FAILED;
            }
        }
        return orgStatusDesc;
    }

    /*
     * public static Boolean validateSupplierOrgTermsAndConditionsInd( List<ServiceType> serviceCodes) { for
     * (ServiceType serviceType : serviceCodes) { if (ApiConstants.SERVICE_CODE_ACH.equalsIgnoreCase(serviceType
     * .getServiceCode())) { String orgTermsAndCondInd = serviceType .getOrgTermsAndConditionsInd();
     *
     * if (orgTermsAndCondInd != null && !ApiConstants.CHAR_Y .equalsIgnoreCase(orgTermsAndCondInd)) { return false; } }
     * } return true; }
     */

    public static Boolean validateCustomerOrgTermsAndConditionsInd(List<ServiceType> serviceCodes) {
        String orgTermsAndCondInd = null;
        for (ServiceType serviceType : serviceCodes) {
            if (ApiConstants.SERVICE_CODE_ACH.equalsIgnoreCase(serviceType.getServiceCode())) {
                if (serviceType != null && serviceType.getOrgTermsAndConditionsInd() != null
                        && !serviceType.getOrgTermsAndConditionsInd().trim().isEmpty()) {
                    orgTermsAndCondInd = serviceType.getOrgTermsAndConditionsInd().trim();
                }

                if (orgTermsAndCondInd == null || (orgTermsAndCondInd != null && orgTermsAndCondInd.equalsIgnoreCase(ApiConstants.CHAR_Y))) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    public static Boolean validateSupplierFeeInd(String supplierFeeInd) {
        boolean result = false;
        if (supplierFeeInd != null
                && (supplierFeeInd.equalsIgnoreCase(ApiConstants.CHAR_Y)
                        || supplierFeeInd.equalsIgnoreCase(ApiConstants.CHAR_N) || supplierFeeInd
                        .equals(ApiConstants.CHAR_BLANKSPACE))) {
            result = true;
        }
        return result;
    }

    public static Boolean validateAcctTermsAndConditionsInd(String termsAndCondInd) {

        if (termsAndCondInd == null || !ApiConstants.CHAR_Y.equalsIgnoreCase(termsAndCondInd)) {
            return false;
        }

        return true;
    }

    /**
     * Check if more than one Service Code is passed in request.
     *
     * @param serviceCodes
     * @return
     */
    public static boolean validateServiceCodeRestriction(List<ServiceType> serviceCodes) {

        if (serviceCodes != null && !serviceCodes.isEmpty() && serviceCodes.size() == 1) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isAchService(List<ServiceType> serviceCodes) {
        boolean blnAchService = false;
        if(serviceCodes != null && !serviceCodes.isEmpty()){
        	for (ServiceType serviceType : serviceCodes) {
                if (ApiConstants.SERVICE_CODE_ACH.equalsIgnoreCase(serviceType.getServiceCode())) {
                    blnAchService = true;
                }
            }
        }
        return blnAchService;
    }

    // ACH Changes
    public static Boolean validateOrgInfoServiceCode(String serviceCode) {
        boolean result = false;
        if (ApiConstants.orgInfoServiceCodes.contains(serviceCode)) {
            result = true;
        }
        return result;
    }

    public static Boolean isAchServiceSubscribed(List<ServiceType> serviceCodes) {
        boolean result = false;
        for (ServiceType serviceType : serviceCodes) {
            if (ApiConstants.SERVICE_CODE_ACH.equalsIgnoreCase(serviceType.getServiceCode())) {
                result = true;
            }
        }
        return result;
    }

    public static String getExternalStatusCd(String statusCd) {
        String statusDesc = "";
        if (null != statusCd) {
            if (ApiConstants.STATUS_BIP_INPROGRESS.equalsIgnoreCase(statusCd)) {
                statusDesc = ApiConstants.STATUS_INPROGRESS;
            } else if (ApiConstants.STATUS__BIP_ACTIVE.equalsIgnoreCase(statusCd)) {
                statusDesc = ApiConstants.STATUS_ACTIVE;
            } else if (ApiConstants.STATUS_BIP_INACTIVE.equalsIgnoreCase(statusCd)) {
                statusDesc = ApiConstants.STATUS_INACTIVE;
            } else if (ApiConstants.STATUS_BIP_VERIFICATION_FAILED.equalsIgnoreCase(statusCd)) {
                statusDesc = ApiConstants.STATUS_VERIFICATION_FAILED;
            } else {
                statusDesc = " ";
            }
        }
        return statusDesc;
    }

    public static boolean validateIPAddress(String ipAddress) {
        if (StringUtils.isBlank(ipAddress)) {
            return false;
        }else if (ipAddress.length() > 15) {
        	return false;
		}else {
            return regexValidator(ipAddress, ipRegexPattern);
        }
    }

    public static boolean validatefirstTxnDt(String strDate) {
        if (strDate == null || !regexValidator(strDate, dobRegexPattern)) {
            return false;
        }
        String trimTimestamp = strDate.trim();
        if (trimTimestamp.length() != 10) {
            return false;
        }
        try {
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
            format.setLenient(false);
            Date date = format.parse(trimTimestamp);
            Timestamp timestamp1 = new Timestamp(date.getTime());
            Timestamp currentTimestamp = new Timestamp(new Date().getTime() + (1000 * 60 * 60 * 24));
            if (timestamp1.compareTo(currentTimestamp) > 0) {
                return false;
            }
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    public static Boolean validateAdminSubmitDate(String timestamp) {
        if (timestamp == null) {
            return false;
        } else {
            try {
                // Example timestamp: 2013-12-13T11:10:00.715-05:00
                String trimTimestamp = timestamp.trim();
                if (timestamp.length() != 19) {
                    return false;
                }
                SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
                format.setLenient(false);
                Date date = format.parse(trimTimestamp);
                Timestamp timestamp1 = new Timestamp(date.getTime());
                Timestamp currentTimestamp = new Timestamp(new Date().getTime());
                if (timestamp1.compareTo(currentTimestamp) > 0) {
                    return false;
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    public static boolean validateDOB(String dob) {
        if (!regexValidator(dob, dobRegexPattern)) {
            return true;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        sdf.setLenient(false);
        Date currentdate = new Date();
        boolean flag = false;
        try {
            Date date1 = sdf.parse(sdf.format(currentdate));
            Date date2 = sdf.parse(dob);
            if (date2.compareTo(date1) > 0 || date2.compareTo(date1) == 0) {
                flag = true;
            } else {
                flag = false;
            }
        } catch (Exception e) {
            return true;
        }
        return flag;
    }

    public static Boolean validateUpId(String alpNumVal, int length) {
        if (alpNumVal.length() > length) {
            return false;
        }

        return true;
    }

    public static List<String> getSuccessResponseCodes() {
        List<String> successResponseCodes = new ArrayList<String>();
        successResponseCodes.add(ApiErrorConstants.SSERET00);
        successResponseCodes.add(ApiErrorConstants.SSERET01);
        successResponseCodes.add(ApiErrorConstants.SSERET02);
        successResponseCodes.add(ApiErrorConstants.SSERET03);
        successResponseCodes.add(ApiErrorConstants.SSERET04);
        successResponseCodes.add(ApiErrorConstants.SSERET05);
        successResponseCodes.add(ApiErrorConstants.SSERET06);
        successResponseCodes.add(ApiErrorConstants.SSERET37);
        successResponseCodes.add(ApiErrorConstants.SSERET38);
        successResponseCodes.add(ApiErrorConstants.SSERET40);
        return successResponseCodes;
    }

    public static Boolean validateAccountType(String accountType) {
        boolean result = false;
        if (accountType != null && (accountType.equalsIgnoreCase("SAV") || accountType.equalsIgnoreCase("CHK"))) {
            result = true;
        }
        return result;
    }

    public static String generateRandom() {

        String msgId = "SSE" + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 17);
        return msgId;
    }

    public static String currentTimeWithFormatYYYYMMDDHHmmss() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
        String timestamp = sdf.format(new Date());
        return timestamp;
    }

    public static String currentRequestTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss.SSSSSS");
        String timestamp = sdf.format(System.currentTimeMillis());
        return timestamp;
    }

    private static boolean isValidNumber(String number, String apiMsgId) {
        boolean isValidNumber = false;
        try {
            isValidNumber = Double.parseDouble(number.trim()) > 0;

        } catch (NumberFormatException e) {
            LOG.error(apiMsgId, "SmartServiceEngine", "Exception API", "CommonUtils:isValidNumber",
                "Invalid Page Index", AmexLogger.Result.failure, "Invalid Page Index. Unable to parse the String");
        }
        return isValidNumber;
    }

    public static Boolean validateReturnCode(String alpNumVal) {
        return regexValidator(alpNumVal, returnCodeRegexPattern);
    }

    /**
     * Validate the Partner Id
     *
     * @param partnerId
     * @return
     */
    public static Boolean validateTemplateName(String templateName, String apiMsgId) {

        if (templateName == null || ("".equals(templateName.trim()))
                || !regexValidator(templateName, templateNameRegexPattern)) {
            return false;
        } else {

            return true;

        }
    }

    public static boolean validateClinetId(String clientId, String clinetIdSSE) {
        boolean flag = false;
        if (StringUtils.isNotBlank(clientId)) {
            String propClientId = EnvironmentPropertiesUtil.getProperty(clinetIdSSE);
            flag = StringUtils.equals(clientId, propClientId);
        } else {
            flag = false;
        }
        return flag;
    }


    public static boolean validateClinetId(String clientId) {
        boolean flag = false;
        if (StringUtils.isNotBlank(clientId)) {
            String propClientId = EnvironmentPropertiesUtil.getProperty(ApiConstants.EMM_CLIENTID_MAP);
            flag = StringUtils.equals(clientId, propClientId);
        } else {
            flag = false;
        }
        return flag;
    }

    public static Boolean validatePublishedURL(String publishedURL) {

        if (publishedURL == null || ("".equals(publishedURL.trim()))
                || !regexValidator(publishedURL, publishedURLRegexPattern)) {
            return false;
        } else {

            return true;

        }
    }

    public static String[] getPayvePartnerEntityId() {

        String[] payvePartnerArray = null;
        String pidMapStr = EnvironmentPropertiesUtil.getProperty(ApiConstants.SSE_PAYVE_PARTNERID_MAP);
        if (pidMapStr != null) {
            String[] pidArry = pidMapStr.split(ApiConstants.CHAR_SEMICOLON);
            if (pidArry != null && pidArry.length > 0) {
                String[] pidInfoArray = pidArry[0].split(ApiConstants.CHAR_COLON);
                if (pidInfoArray != null && pidInfoArray.length > 0) {
                    return pidInfoArray;
                }
            }
        }
        return payvePartnerArray;
    }

    public static String getPartnerName() {

        String partnerName = null;
        String partName = EnvironmentPropertiesUtil.getProperty(ApiConstants.PARTNER_NAMES);
        if (partName != null) {
            String[] partNameArry = partName.split(ApiConstants.CHAR_SEMICOLON);
            if (partNameArry != null && partNameArry.length > 0) {
                return partNameArry[0];
            }
        }
        return partnerName;
    }

    public static Boolean validateVersionNo(String versionNo, int maxLength) {

        if (versionNo == null || "".equals(versionNo.trim()) || versionNo.length() < 1
                || versionNo.length() > maxLength) {
            return false;
        } else {
            return regexValidator(versionNo, versionNoRegexPattern);
        }
    }

    public static double generate_MicroAmount() {

		DecimalFormat twoDForm = new DecimalFormat("#.##");
		double random = 0.0d;
		do {
			random = new Random().nextDouble();
		}while(random < 0.01);

		double result = (random * (1 - 0.01));
		result = Double.valueOf(twoDForm.format(result));
		return result;

    }

    public static boolean validateEmailWithComma(String alphaVal, int length) {
        boolean emailRegexCheck = false;
        if(alphaVal == null || alphaVal.length() > length ){
        	return false;
        }else if (alphaVal != null && alphaVal.indexOf(",") != -1) {
            String emailList[] = alphaVal.split(",");
            if (emailList.length > 10) {
                return false;
            }
            if (alphaVal.charAt(alphaVal.length() - 1) == ',') {
                return false;
            }
            for (int i = 0; i < emailList.length; i++) {
                String emailAddress = emailList[i];
                if (emailAddress == null || emailAddress.length() > 254) {
                    return false;
                } else {
                    emailRegexCheck = true;

                }

                if (emailRegexCheck) {
                    if (!regexValidator(emailAddress, emailRegexPattern)) {
                        return false;
                    }

                }

            }
            return true;

        } else {
            return regexValidator(alphaVal, emailRegexPattern);
        }

    }
    
    
    public static boolean validateEmailWithLength(String alphaVal, int length) {
               
        if (alphaVal == null || "".equals(alphaVal.trim()) || alphaVal.length() < 1
                || alphaVal.length() > length) {
            return false;
        } 
        else {
            return regexValidator(alphaVal, emailRegexPattern);
        }

    }

    public static String generatePymtRefNo(String pymtType){
        String paymtRefNum = StringUtils.EMPTY;
        if (ApiConstants.PAYMENT_TYPE_CODE_D.equals(pymtType)){
        	paymtRefNum = ApiConstants.SSE_CNFM_NBR_PFRX_SD+RandomStringUtils.random(8, 0, 10, false, true, "0123456789".toCharArray());
        }else if (ApiConstants.PAYMENT_TYPE_CODE_C.equals(pymtType)){
        	paymtRefNum = ApiConstants.SSE_CNFM_NBR_PFRX_SC+RandomStringUtils.random(8, 0, 10, false, true, "0123456789".toCharArray());
        }
        return paymtRefNum;
    }

    public static boolean validateLastRunTS(String lastRunTimestamp){
        if(validateTimestamp(lastRunTimestamp)){
            try {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
                format.setLenient(false);
                Date date = format.parse(lastRunTimestamp);
                Timestamp timestamp1 = new Timestamp(date.getTime());
                Timestamp currentTimestamp = new Timestamp(new Date().getTime());
                if (timestamp1.compareTo(currentTimestamp) > 0) {
                    return false;
                }
                return true;
            } catch (ParseException e) {
                return false;
            }
        }
        return false;
    }

    public static String maskAccRoutingNumber(String number, int length, String apiMsgId) {
    	  String maskedNum = null;
    	  try{
    	   if (StringUtils.isNotBlank(number)){
    	    maskedNum = number.trim();
    	    if (maskedNum.length() >= length) {
    	     maskedNum = ApiConstants.MASK.substring(0,maskedNum.length()-4) + maskedNum.substring(maskedNum.length() - 4, maskedNum.length());
    	    } else{
    	     maskedNum = number;
    	    }
    	   }
    	  }catch(Exception e){
    		  LOG.error(apiMsgId, "SmartServiceEngine", "Mask PII Data", "CommonUtils:maskAccRoutingNumber",
    	                "Exception while masking ACC/RTR number in logs", AmexLogger.Result.failure, "Exception while masking ACC/RTR number in logs");
    	  }
    	  return maskedNum;
    	 }

    public static String maskingNumber(String mapValue,String number,int length, String apiMsgId){
    	if(StringUtils.isNotBlank(number)){
    		mapValue=mapValue.replace(number, maskAccRoutingNumber(number, length, apiMsgId));
    	}
    	return mapValue;
    }
    
    public static Boolean validateCardTypeForCard(String enActType) {
        
    	String cardTyprArr[] = cardTypeValue.split(ApiConstants.CHAR_SEMICOLON);
    	List<String> cardTypeList = Arrays.asList(cardTyprArr);
    	if(cardTypeList.contains(enActType.toUpperCase())){
    		return true;  
    	}else{
        	return false;
        }
        
    }
    
    public static Boolean validateCountryCodeForCard(String countryCode) {
    	if (StringUtils.isBlank(countryCode)) {
            	return false;
        } 
        String countryCodeArr[] = countryCodeCard.split(ApiConstants.CHAR_SEMICOLON);
    	List<String> countryCodeList = Arrays.asList(countryCodeArr);
    	if(countryCodeList.contains(countryCode.toUpperCase())){
    		return true;  
    	}else{
        	return false;
        }
    }
    
    public static Boolean validateExpiryDateForCard(String expDate) {
      	if(expDate.length()!=4){
      		return false;
      	}
		int mm = Integer.parseInt(expDate.substring(0, 2));
		if(mm==0 || mm>12){
	  		return false;
	  	}else{
	  		return true;
	  	}
		//return yy > currYY? true : yy == currYY && mm >= currMM;
    }
    
    public static Boolean isCardExpired(String expDate) {
      	
		int mm = Integer.parseInt(expDate.substring(0, 2));
		int yy = Integer.parseInt(expDate.substring(2, 4));
		String mmYY = new SimpleDateFormat(MM_YY).format(System.currentTimeMillis());
		int currMM = Integer.parseInt(mmYY.substring(0, 2));
		int currYY = Integer.parseInt(mmYY.substring(2, 4));
		
		return yy > currYY? true : yy == currYY && mm >= currMM;
    }
    
    public static Boolean validateEnrollActTypeForCard(String enActType, int length) {
        if (enActType == null || "".equals(enActType.trim()) || length < 1
                || enActType.length() > length) {
            	return false;
        } else if(enActType.equalsIgnoreCase(ApiConstants.ENROLLMNT_ACT_TYPE_CARD)){
        		return true;        	
        }else{
        	return false;
        }
        
    }
    

	public static String maskXmlPayload(SOAPMessage oSOAPMessage,String xmlMessageString, String serviceIdentifierStr, int nonMaskLength ,String apiMsgId){
				
		 NodeList nodes = null;
		 String maskXmlString="";
		 String xmlKey = "";
		 try{
			 nodes = oSOAPMessage.getSOAPBody().getElementsByTagName(serviceIdentifierStr);
	         if(nodes.getLength()>0)
	         {
                           
	         	//String compareStr="AcctNbr:"; 
	    	 	xmlKey = EnvironmentPropertiesUtil.getProperty(serviceIdentifierStr);
	    		String[] formStr =xmlKey.split(ApiConstants.C3_XML_SPLIT_CHAR);
	    		String completeRegEx="";		
    		
           		for(String compareStrng: formStr){
          			    //completeRegEx ="<(.*):"+compareStrng+">(.*?)</(.*):"+compareStrng+">(.*)";
           				completeRegEx =ApiConstants.C3_XML_GROUP1+compareStrng+ApiConstants.C3_XML_GROUP2+compareStrng+ApiConstants.C3_XML_GROUP3;
          			    //System.out.println(completeRegEx);
		           		Pattern pattern = Pattern.compile(completeRegEx);
		           		Matcher matcher = pattern.matcher(xmlMessageString);
		           		String nonMaskXmlValue=null;
		           		int i=2;
		           		if (matcher.find())
		           		{
		           			nonMaskXmlValue=matcher.group(i);
		           			maskXmlString = xmlMessageString.replace(nonMaskXmlValue, 
			           				maskField(nonMaskXmlValue,(nonMaskXmlValue.length()-nonMaskLength), nonMaskXmlValue.length(), 
			           				ApiConstants.C3_MASK_CHAR,"",ApiConstants.TRUEBOOLEAN));
	           				
		           		}else{
		           			LOG.error(apiMsgId, "Commerce Concierge", "Mask xml PII Data", "GenericUtils:maskXmlPayload",
		          	                "unable to find the xmlkey in the soap Msg", AmexLogger.Result.failure, 
		          	                "unable to find the xmlkey in soap_Msg","compareStrng",compareStrng);
		           		}
		           		
		           	/*	if(StringUtils.isNotBlank(nonMaskXmlValue)){
		           			maskXmlString = maskXmlString.replace(nonMaskXmlValue, 
		           				GenericUtilities.maskField(nonMaskXmlValue,(nonMaskXmlValue.length()-nonMaskLength), nonMaskXmlValue.length(), 
		           				ApiConstants.C3_MASK_CHAR,"",ApiConstants.TRUEBOOLEAN));		           			
		           		}*/
           		}
           		//System.out.println("maskXmlString "+maskXmlString);
        
	         }else{
	        	 LOG.error(apiMsgId, "Commerce Concierge", "Mask xml PII Data", "GenericUtils:maskXmlPayload",
          	                "unable to Identify the SOAP service Type", AmexLogger.Result.failure, 
          	                "unable to Identify the SOAP_MSG","serviceIdentifierStr",serviceIdentifierStr);
           	 }
	         }catch(Exception e){
	        	 LOG.error(apiMsgId, "Commerce Concierge", "Mask xml PII Data", "GenericUtils:maskXmlPayload",
      	                "Exception while masking maskXmlPayload_in logs", AmexLogger.Result.failure, "Exception while masking maskXmlPayload in logs");
	   		
			}
         
         return maskXmlString;
    }
	 
	public static String maskField(String number, int startIndex, int endIndex,char replaceCharacter, String apiMsgId, boolean isFpan) {
		String maskedNum = null;
		try {
			if (StringUtils.isNotBlank(number)) {
				maskedNum = number.trim();
				String b = maskedNum.substring(startIndex, endIndex);
				if (isFpan) {
					maskedNum = StringUtils.leftPad(b, number.length(),replaceCharacter);
				} else {
					String startKey = number.substring(0,startIndex);
			        String endKey = number.substring(endIndex , number.length());
			        String padded = StringUtils.rightPad(startKey, endIndex,replaceCharacter); 
			        maskedNum = padded.concat(endKey);	
				}
			}
		} catch (Exception e) {
			LOG.error(apiMsgId, "Commerce Concierge", "Mask PII Data",
					"GenericUtils:maskField",
					"Exception while masking Field in logs",
					AmexLogger.Result.failure,
					"Exception while masking Field in logs");
			maskedNum = number;
		}
		return maskedNum;
	}
	 	 
	 public static String maskJsonPayload(String jsonPayloadString, String maskValue,int nonMaskLength ,String apiMsgId,boolean isFPAN){
	    String maskedJsonPayloadStr="";
	    try{
	    	if(maskValue!=null && !maskValue.isEmpty()){	    		
	    		if(isFPAN){
	    			maskedJsonPayloadStr=jsonPayloadString.replace(maskValue,maskField(maskValue,(maskValue.length()-nonMaskLength), maskValue.length(),ApiConstants.C3_MASK_CHAR,apiMsgId,isFPAN));
	    		}else{
	    			maskedJsonPayloadStr=jsonPayloadString.replace(maskValue,maskField(maskValue,SchedulerConstants.START_MASK_TOKEN_INDEX,SchedulerConstants.END_MASK_TOKEN_INDEX,ApiConstants.C3_MASK_CHAR,apiMsgId,isFPAN));
	    		}
	    		}
	    }catch(Exception e){
	    	LOG.error(apiMsgId, "Commerce Concierge", "Mask PII Data", "GenericUtils:maskJsonPayload",
	                "Exception while masking jsonPayload_in logs", AmexLogger.Result.failure, "Exception while masking jsonPayload in logs");
	   		
	   	  }
		return maskedJsonPayloadStr;
	}
	 
	public static Map<String, Object> maskFieldInMap(Map<String, Object> inMap,String maskFieldKey, int nonMaskLength, String apiMsgId,boolean isFPAN) {
		Map<String, Object> inMaskMap = new HashMap<String, Object>(inMap);
		String maskFieldValue = "";
		try {
			maskFieldValue = ((String) inMap.get(maskFieldKey));
			if (StringUtils.isNotBlank(maskFieldValue)) {
				if (isFPAN) {
					inMaskMap.put(maskFieldKey,	maskField(maskFieldValue,(maskFieldValue.length() - nonMaskLength),	maskFieldValue.length(),ApiConstants.C3_MASK_CHAR,apiMsgId, isFPAN));
				} else {
					inMaskMap.put(maskFieldKey,	maskField(maskFieldValue,SchedulerConstants.START_MASK_TOKEN_INDEX,SchedulerConstants.END_MASK_TOKEN_INDEX,ApiConstants.C3_MASK_CHAR,apiMsgId, isFPAN));
				}

			}
		} catch (Exception e) {
			LOG.error(apiMsgId, "Commerce Concierge", "Mask PII Data",
					"GenericUtils:maskFieldInMap",
					"Exception while masking HMAP Field in logs",
					AmexLogger.Result.failure,
					"Exception while masking DB Field in logs");

		}
		return inMaskMap;
	}
    /*handled validations orgtermscondind for oly customer and supplier simlar to ACH - 22/7/2016 */
    public static Boolean validateCardOrgTermsAndConditionsInd(String orgTermsAndCondInd, boolean isCustomer) {        
        if (/*isCustomer &&*/ !(ApiConstants.CHAR_Y.equalsIgnoreCase(orgTermsAndCondInd))) {
                return false;
            }/*else if (!isCustomer && ((ApiConstants.CHAR_Y.equalsIgnoreCase(orgTermsAndCondInd))
            		&& StringUtils.isNotBlank(orgTermsAndCondInd) )) {
            	return false;                	
            }*/
        return true;
    }
    
    public static boolean validateOrgEmailId(String emailAddress) {
        if(StringUtils.isBlank(emailAddress)){
            return false;
        }else if(emailAddress.length() > 50){
            return false;
        }else{
            return regexValidator(emailAddress, emailRegexPattern);
        }
    }
    
    public static boolean validateOrgEmailId(String emailAddress, int emailLength) {
        if(StringUtils.isBlank(emailAddress)){
            return false;
        }else if(emailAddress.length() > emailLength){
            return false;
        }else{
            return regexValidator(emailAddress, emailRegexPattern);
        }
    }
    
    public static Boolean isCardServiceSubscribed(List<ServiceType> serviceCodes) {
        boolean result = false;
        for (ServiceType serviceType : serviceCodes) {
            if(ApiConstants.SERVICE_CODE_CARD.equalsIgnoreCase(serviceType.getServiceCode()) ) {
                result = true;
            }
        }
        return result;
    }
    
    public static Boolean validateOrgPhoneNumberWithLength(String numVal){
        return regexValidator(numVal, orgPhoneNumberRegexPattern);
    }
    
    public static Boolean validateCardNumberWithLength(String numVal,int length){
    	if (numVal == null || "".equals(numVal.trim()) || numVal.length() < 1
                || numVal.length() > length) {
            return false;
        } else {
            if(!StringUtils.isNumeric(numVal)){
            	return false;
            }
        }
		return true;   	
    }
    
    public static String getRequestId(HttpHeaders header){
		String str = null;
		if(header.get(ApiConstants.AMEX_REQUEST_ID) != null){
			str=StringUtils.stripToEmpty(header.get(ApiConstants.AMEX_REQUEST_ID).get(0));
		}
		return str;
	}
    
    public static String getCMRequestId(HttpHeaders header){
		String str = null;
		if(header.get(ApiConstants.REQUEST_ID) != null){
			str=StringUtils.stripToEmpty(header.get(ApiConstants.REQUEST_ID).get(0));
		}
		return str;
	}
    
    public static String getTimeStamp(HttpHeaders header){
		String str = null;
		if(header.get(ApiConstants.TIMESTAMP) != null){
			str=StringUtils.stripToEmpty(header.get(ApiConstants.TIMESTAMP).get(0));
		}
		return str;
	}
	public static String getRequestorId(HttpHeaders header){
		String str = null;
		if(header.get(ApiConstants.REQUESTOR_ID) != null){
			str=StringUtils.stripToEmpty(header.get(ApiConstants.REQUESTOR_ID).get(0));
		}
		return str;
	}

	public static boolean validateRequestorId(String numVal,int length) {
		if (numVal == null || "".equals(numVal.trim()) || numVal.length() < 1 || numVal.length() > length) {
            return false;
        } else {
        	return regexValidator(numVal, alphaNumericHyphenUnderscoreRegexPattern);
        }
	}

	public static boolean validateRequestorIdValue(String requestorId) {
		if(!EnvironmentPropertiesUtil.getProperty(ApiConstants.API_REQUESTOR_ID).equals(requestorId)){
			return false;
		}
		return true;
	}
	
	//2.0 changes – To retrieve X-AMEX-API-KEY(Partner id) from Header- Start
	public static String getAMEXAPIKey(HttpHeaders header){
		String str = null;
		if(header.get(ApiConstants.AMEX_API_KEY) != null){
			str=StringUtils.stripToEmpty(header.get(ApiConstants.AMEX_API_KEY).get(0));
		}
		return str;
	}
	//2.0 changes – To retrieve X-AMEX-API-KEY(Partner id) from Header- End
	
	public static Boolean validatePositiveAmountForCard(String amount, String apiMsgId) {
        if (amount == null || "".equals(amount.trim()) || amount.contains(" ")) {
            return false;
        } else if (!isValidAmount(amount, apiMsgId)) {
            return false;
        } else {
            return regexValidator(amount, cardAmountPositiveRegexPattern);
        }
    }
	
	public static String maskCardNumber(String number, int length, String apiMsgId) {
  	  String maskedNum = null;
  	  try{
  	   if (StringUtils.isNotBlank(number)){
  	    maskedNum = number.trim();
  	    if (maskedNum.length() >= length) {
  	     maskedNum = ApiConstants.MASK.substring(0,maskedNum.length()-length) + maskedNum.substring(maskedNum.length() - length, maskedNum.length());
  	    } else{
  	     maskedNum = number;
  	    }
  	   }
  	  }catch(Exception e){
  		  LOG.error(apiMsgId, "SmartServiceEngine", "Mask PII Data", "CommonUtils:maskAccRoutingNumber",
  	                "Exception while masking ACC/RTR number in logs", AmexLogger.Result.failure, "Exception while masking ACC/RTR number in logs");
  	  }
  	  return maskedNum;
  	 }
	
	public static String maskCardActivationCardNumber(String requestLog,CardActivationRequest cardActivationRequest, String apiMsgId){
		if(cardActivationRequest != null){
			List<CardMemberDataType> memberData=cardActivationRequest.getCardMemberData();
			if(memberData != null && !memberData.isEmpty()){
				for(CardMemberDataType cmData:memberData){
					if(cmData.getCardNumber()!= null && StringUtils.isNotEmpty(cmData.getCardNumber())){
						requestLog=requestLog.replace(cmData.getCardNumber(), maskCardNumber(cmData.getCardNumber(), 5 , apiMsgId));
					}
				}
			}
		}
		return requestLog;
	}
	
	public static String maskCAResponseCardNum(String response,String apiMsgId) throws JsonParseException, JsonMappingException, IOException{
		CardActivationResponseType responseType = new ObjectMapper().readValue(response, CardActivationResponseType.class);
		if(responseType != null){
			CardActivationResponse res=responseType.getCardActivationResponse();
			if(res != null){
				List<CardActivationDetailsType> data = res.getActivationDtls();
				if(data!=null && !data.isEmpty()){
					for(CardActivationDetailsType cardNumDet:data){
						if(cardNumDet.getCardNumber()!= null && StringUtils.isNotEmpty(cardNumDet.getCardNumber())){
							String cardNum=cardNumDet.getCardNumber();
							response=response.replace(cardNum, CommonUtils.maskCardNumber(cardNum, 5, apiMsgId));
						}
					}
				}
			}
		}
		return response;
	}
	
	public static String maskCMResponse(String response,String apiMsgId) throws JsonParseException, JsonMappingException, IOException{
		CardMemberDetailsResponseType responseType = new ObjectMapper().readValue(response, CardMemberDetailsResponseType.class);
		if(responseType != null){
			CardMemberDetailsResponse res=responseType.getCmDtlsResponse();
			if(res!=null){
				List<String>  cardsList = res.getCardsList();
				if(cardsList != null && !cardsList.isEmpty()){
					for(String cardNum:cardsList){
						response=response.replace(cardNum, CommonUtils.maskCardNumber(cardNum, 5, apiMsgId));
					}
				}
			}
		}
		return response;
	}
	
	
	public static boolean isValidCardExpiryDate(String expiryDate,String apiMsgId) {
		boolean isValidExpDate = true;
		
		if(StringUtils.isNotBlank(expiryDate) && !CommonUtils.isCardExpired(expiryDate)){
				isValidExpDate = false;
		}

		return isValidExpDate;
	}
	
	public static Boolean validateExpiryDateNumericWithLength(String numVal, int length) {
        if (numVal == null || "".equals(numVal.trim()) || length != numVal.length()) {
            return false;
        } else {
            return regexValidator(numVal, numericRegexPattern);
        }
    }
	


	public static String getParnerIdForAccess(String rootKey, String apiMsgId){
		StringBuffer stringKeyBuffer = new StringBuffer();
		try{			
			String rootValue = null;		
			rootValue = EnvironmentPropertiesUtil.getProperty(rootKey);				
			if(StringUtils.isNotBlank(rootValue)){					
				String rootSubKeyArry[] = rootValue.split(ApiConstants.CHAR_SEMICOLON);
				if(rootSubKeyArry.length>0){					
					
					for(String subKey:rootSubKeyArry){						
						if(subKey.contains(ApiConstants.CHAR_PIPE)){							
							String subKeyArry[] = subKey.split(ApiConstants.CHAR_PIPE_REG_EXP);
							stringKeyBuffer.append(getStringKeyValue(subKeyArry, apiMsgId));
							stringKeyBuffer.append(ApiConstants.CHAR_SEMICOLON);
						}else{
							stringKeyBuffer.append(getStringKeyValue(subKey, apiMsgId));
							stringKeyBuffer.append(ApiConstants.CHAR_SEMICOLON);
						}						
					}
				}				
			}				
			return stringKeyBuffer.toString();		
		}catch(Exception e){
			LOG.error(apiMsgId, "SmartServiceEngine", "getParnerIdAccess", "getParnerIdForAccess", "UNABLE TO retrieve partner id for etv token functionality.", AmexLogger.Result.failure, 
					"Error while updating Email Sent status in database", e);
		return stringKeyBuffer.toString();
		}		
		}
	
	
	public static String getStringKeyValue(String[] stringKeyArry, String apiMsgId){
		StringBuffer stringKeyBuffer = new StringBuffer();
		int keyCount=0;
		try{			
			if(stringKeyArry.length>0){					
				for(String stringKey:stringKeyArry){
					keyCount++;
					stringKeyBuffer.append(getStringKeyValue(stringKey, apiMsgId));
					if(keyCount<stringKeyArry.length){
						stringKeyBuffer.append(ApiConstants.CHAR_PIPE);
					}
				}
			 }				
			return stringKeyBuffer.toString();		
		}catch(Exception e){
			LOG.error(apiMsgId, "SmartServiceEngine", "getStringKeyValue", "getStringKeyValue from i/p Array", "UNABLE TO retrieve key value array from property.", AmexLogger.Result.failure, 
			"Error while updating Email Sent status in database", e);
		return stringKeyBuffer.toString();
		}		
	}
	
	
	public static String getStringKeyValue(String stringKey, String apiMsgId){
		StringBuffer stringKeyBuffer = new StringBuffer();
		try{							
			String stringValue = EnvironmentPropertiesUtil.getProperty(stringKey);
			if(StringUtils.isNotBlank(stringValue)){
				stringKeyBuffer.append(stringValue);
			}							
			return stringKeyBuffer.toString();		
		}catch(Exception e){
			LOG.error(apiMsgId, "SmartServiceEngine", "getStringKeyValue", "getStringKeyValue from string", "UNABLE TO retrieve key from property.", AmexLogger.Result.failure, 
			"Error while updating Email Sent status in database", e);
		return stringKeyBuffer.toString();
		}		
	}
	public static HashMap<String,String> getHashMapKeyValue(String stringKey, String apiMsgId){
		HashMap<String,String> keyValueMap = new HashMap<String,String>();
		try{
			String reqIdArry[] = stringKey.split(ApiConstants.CHAR_SEMICOLON);
			for(String reqId : reqIdArry){
				String reqSubIdArry[] = reqId.split(ApiConstants.CHAR_PIPE_REG_EXP);
				if(reqSubIdArry.length>1){
					keyValueMap.put(reqSubIdArry[0], reqSubIdArry[1]);	
				}					
			}
			return keyValueMap;
		}catch(Exception e){
			LOG.error(apiMsgId, "SmartServiceEngine", "getHashMapKeyValue", "getHashMapKeyValue", "UNABLE TO from key value map from property.", AmexLogger.Result.failure, 
			"Error while updating Email Sent status in database", e);
		return keyValueMap;
		}	
	}

	public static String getFormattedMessage(String message,Object[] arguments){
		return  MessageFormat.format(message, arguments);
	}
	
	public static String generateUniqueNumber(int length){
		return RandomStringUtils.random(length, 0, 62, true, true, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghigklmnopqrstuvwxyz0123456789-".toCharArray());
	}
	
	//2.0 changes – Code to generate unique id-Start
	public static String generateUniqueCode(String filterIfAny, int totalLength , int startIndex , int endIndex , String charSequence){
		try{
			return RandomStringUtils.random(totalLength, startIndex, endIndex, true, true, charSequence.toCharArray());
		}catch(Exception e){
			return ApiConstants.CHAR_SPACE;
		}
    }
	//2.0 changes – Code to generate unique id-End

	
	public static String maskingAccountRequest(EnrollmentRequestType requestType , String apiMsgId) throws JsonGenerationException, JsonMappingException, IOException{
		String maskedLog=ApiUtil.pojoToJSONString(requestType);
		if(requestType!=null && requestType.getAccountdetails()!=null){
        	maskedLog=maskingRequest(maskedLog,requestType.getAccountdetails(), ThreadLocalManager.getApiMsgId());
	     }
		return maskedLog;
	}
	
	public static String maskingRequest(String requestLog,AccountDetails accDetails, String apiMsgId){
	    	List<CardAccount> cardAccDetails=accDetails.getCardAccts();
	    	if(cardAccDetails!=null && !cardAccDetails.isEmpty()){
	    		for(CardAccount cardAccount:cardAccDetails){
	    			if(StringUtils.isNotBlank(cardAccount.getCardNumber())){
	    				requestLog=requestLog.replace(cardAccount.getCardNumber().trim(), CommonUtils.maskCardNumber(cardAccount.getCardNumber(), 5, apiMsgId));
	    			}
	    		}
	    	}
	    	
	    	List<ACHAccount> achAccDetails=accDetails.getAchAccts();
	    	if(achAccDetails!=null && !achAccDetails.isEmpty()){
	    		for(ACHAccount achAccount:achAccDetails){
	    			if(null != achAccount.getBank() && StringUtils.isNotBlank(achAccount.getBank().getAccountNumber())){
	    				requestLog=requestLog.replace(achAccount.getBank().getAccountNumber(), CommonUtils.maskCardNumber(achAccount.getBank().getAccountNumber(), 5, apiMsgId));
	    			}
	    			if(null != achAccount.getBank() && StringUtils.isNotBlank(achAccount.getBank().getRoutingNumber())){
	    				requestLog=requestLog.replace(achAccount.getBank().getRoutingNumber(), CommonUtils.maskCardNumber(achAccount.getBank().getRoutingNumber(), 5, apiMsgId));
	    			}
	    		}
	    	}
	    	
	    	List<CheckAccount> chkAccDetails=accDetails.getCheckAccts();
	    	if(chkAccDetails!=null && !chkAccDetails.isEmpty()){
	    		for(CheckAccount chkAccount:chkAccDetails){
	    			if(null != chkAccount.getBank() && StringUtils.isNotBlank(chkAccount.getBank().getAccountNumber())){
	    				requestLog=requestLog.replace(chkAccount.getBank().getAccountNumber(), CommonUtils.maskCardNumber(chkAccount.getBank().getAccountNumber(), 5, apiMsgId));
	    			}
	    			if(null != chkAccount.getBank() && StringUtils.isNotBlank(chkAccount.getBank().getRoutingNumber())){
	    				requestLog=requestLog.replace(chkAccount.getBank().getRoutingNumber(), CommonUtils.maskCardNumber(chkAccount.getBank().getRoutingNumber(), 5, apiMsgId));
	    			}
	    		}
	    	}

	    	return requestLog;
	}
	
	public static String maskingPaymentRequest(PaymentRequestType requestType) throws JsonGenerationException, JsonMappingException, IOException{
		String maskedLog=ApiUtil.pojoToJSONString(requestType);
		if(requestType!=null && requestType.getAchPayment()!=null){
        	maskedLog=maskingPaymtReq(maskedLog,requestType.getAchPayment(), ThreadLocalManager.getApiMsgId());
	     }
		return maskedLog;
	}
	public static String maskingPaymtReq(String requestLog, ACHPaymentType achPaymentType, String apiMsgId)
	{
		if(null != achPaymentType)
		{
			if(StringUtils.isNotBlank(achPaymentType.getPayeeAccountId())){
				requestLog=requestLog.replace(achPaymentType.getPayeeAccountId(), CommonUtils.maskCardNumber(achPaymentType.getPayeeAccountId(), 5, apiMsgId));
			}
			if(StringUtils.isNotBlank(achPaymentType.getPayerAccountId())){
				requestLog=requestLog.replace(achPaymentType.getPayerAccountId(), CommonUtils.maskCardNumber(achPaymentType.getPayerAccountId(), 5, apiMsgId));
			}
		}
		return requestLog;
	}
	
	public static String getFormattedMsg(String message,int allowableLength){
		String frmattedMsg  = " ";
		if(StringUtils.isNotBlank(message)){
			if(message.length()>allowableLength){
				frmattedMsg =  message.substring(0, allowableLength-1);	
			}else{
				frmattedMsg =  message;
			}
		}		
		return frmattedMsg;
		
	}
	
	public static String getServiceType(String service){
		if("AC".equalsIgnoreCase(service))
			return "ACH";
		else
			return service;			
	}
	
	public static Map<String, String> getListOfStatusCodes(){
		Map<String, String> statusCodes = new HashMap<String, String>();
		statusCodes.put("1", "INPR");
		statusCodes.put("11", "SENT");
		statusCodes.put("15", "AUTH");
		statusCodes.put("13", "PAID");
		statusCodes.put("99", "FAIL");
		statusCodes.put("14", "CANCEL");
		return statusCodes;
	}
	public static Boolean validateVarianceWithAmount(String amount, int length) {
        if (amount == null || "".equals(amount.trim()) || amount.contains(" ") || amount.length() > length) {
            return false;
        }else {
            return regexValidator(amount, cardAmountPositiveRegexPattern);
        }
    }
	
	public static Boolean validateVarianceWithPercent(String percentage, int length, String apiMsgId) {
        if (percentage == null || "".equals(percentage.trim()) || percentage.contains(" ") || percentage.length() > length) {
            return false;
        }else {
        	boolean isValidNumber = regexValidator(percentage, percentageRegexPattern);
        	 if(isValidNumber){
        		 try {
                     isValidNumber = Double.parseDouble(percentage.trim()) <= 100;

                 } catch (NumberFormatException e) {
                     LOG.error(apiMsgId, "SmartServiceEngine", "Exception API", "CommonUtils:isValidNumber",
                         "Invalid Page Index", AmexLogger.Result.failure, "Invalid Page Index. Unable to parse the String");
                 }
        	 }
            return isValidNumber;
        }
    }
	
	public static String convertStringAmountToInt(String amtStr) {
		BigDecimal amtStrBD = new BigDecimal(amtStr);
		amtStrBD = amtStrBD.multiply(new BigDecimal(100));
		BigDecimal truncated= amtStrBD.setScale(0,BigDecimal.ROUND_DOWN);		
		return String.valueOf(truncated);		
	}
	
	public static Boolean validateAlphaNumericSpecialCharWithLength(String numVal, int length) {
        if (numVal == null || "".equals(numVal.trim()) || length < 1 || numVal.length() > length) {
            return false;
        } else {
            return regexValidator(numVal, alphaNumericSpecialCharRegexPattern);
        }
    }
	
	public static String encryptData(String data, String encryptionType, String key) throws GeneralSecurityException{
		Juice juice=new Juice(encryptionType, key);
		String encryptedData = juice.encrypt(data);
		return encryptedData;
	}
	
	public static String calculateEffectiveDate(Date datenow,String scenario,CacheManager cacheManager, String bankName, String pymtAmount,String apiMsgId, String pymtTransType) throws ParseException{
		Calendar cal = Calendar.getInstance();
		cal.setTime(datenow);
		Cache bankHolidaysListCache= cacheManager.getCache(ApiConstants.BANK_HOLIDAYS_LIST_CACHE);
		SimpleDateFormat holidaysdf = new SimpleDateFormat(ApiConstants.HOL_DF);
		String todayDate = holidaysdf.format(datenow);
		ValueWrapper element = bankHolidaysListCache.get(todayDate);
		FetchBankHolidaysResultSetVO holiday= null;
		if(element != null){
			holiday = (FetchBankHolidaysResultSetVO)element.get();
			LOG.info(apiMsgId, "SmartServiceEngine", "CommonUtils", "processPayment",
		            "Calculating effective Date", AmexLogger.Result.success, "",todayDate,"Holiday");
		}
		SimpleDateFormat sdf1 = new SimpleDateFormat(ApiConstants.DATEFRMT_MMDDYYYY);
		DateFormat format2=new SimpleDateFormat(ApiConstants.DAY_FORMAT); 
		String daynow=format2.format(datenow);
		SimpleDateFormat parser = new SimpleDateFormat(ApiConstants.TIME_FORMAT);
		if(scenario.equals(ApiConstants.ON_US)){
			if(holiday != null || daynow.equals(ApiConstants.DAY_SAT) || daynow.equals(ApiConstants.DAY_SUN)){
				workingDays(cal,1,bankHolidaysListCache,datenow,scenario,false);
				LOG.info(apiMsgId, "SmartServiceEngine", "CommonUtils", "processPayment",
			            "Calculating effective Date", AmexLogger.Result.success, "","Transaction Type","ON_US",todayDate,"Holiday/Saturday/Sunday");
			} else {
				LOG.info(apiMsgId, "SmartServiceEngine", "CommonUtils", "processPayment",
			            "Calculating effective Date", AmexLogger.Result.success, "","Transaction Type","ON_US",todayDate,daynow);
				String parsed=parser.format(datenow);
				Date timenow = parser.parse(parsed);
				String cutOffTime = ACHBankNameValues.getCutOffTimeForBank(bankName);
				Date lastTime = parser.parse(cutOffTime);
				if(timenow.after(lastTime)){
					workingDays(cal,1,bankHolidaysListCache,datenow,scenario,true);
				}
			}
		}
		if(scenario.equals(ApiConstants.OFF_US)){
			if(holiday != null || daynow.equals(ApiConstants.DAY_SAT) || daynow.equals(ApiConstants.DAY_SUN)){
				workingDays(cal,1,bankHolidaysListCache,datenow,scenario,false);
				LOG.info(apiMsgId, "SmartServiceEngine", "CommonUtils", "processPayment",
			            "Calculating effective Date", AmexLogger.Result.success, "","Transaction Type","ON_US",todayDate,"Holiday/Saturday/Sunday");
			} else {
				LOG.info(apiMsgId, "SmartServiceEngine", "CommonUtils", "processPayment",
			            "Calculating effective Date", AmexLogger.Result.success, "","Transaction Type","ON_US",todayDate,daynow);
				String parsed=parser.format(datenow);
				Date timenow = parser.parse(parsed);
				String sameDayCutoffTime = ACHBankNameValues.getSameDayCutOffTimeForBank(bankName);
				double sameDayPymtLimit = ACHBankNameValues.getSameDayPymtLimit(bankName);
				Date sameDayEffTime = parser.parse(sameDayCutoffTime);
				Double pymtAmt = Double.valueOf(pymtAmount);
				boolean isSameDayAchEnabled = ACHBankNameValues.isSameDayEnabled(bankName);
				
				if(isSameDayAchEnabled && timenow.before(sameDayEffTime) && pymtAmt <= sameDayPymtLimit && (null != pymtTransType && ApiConstants.CHAR_C.equalsIgnoreCase(pymtTransType))){
					//Same Day ACH criteria met. Effective Date is same Day.
					LOG.info(apiMsgId, "SmartServiceEngine", "CommonUtils", "processPayment",
				            "Calculating effective Date", AmexLogger.Result.success, "","Transaction Type","isSameDay","Day",daynow);
					
				}else{
					String cutOffTime = ACHBankNameValues.getCutOffTimeForBank(bankName);
					Date lastTime = parser.parse(cutOffTime);
					if(timenow.before(lastTime) && (daynow.equals(ApiConstants.DAY_MON) || daynow.equals(ApiConstants.DAY_TUE) || daynow.equals(ApiConstants.DAY_WED) || daynow.equals(ApiConstants.DAY_THU))){
						workingDays(cal,1,bankHolidaysListCache,datenow,scenario,false);
					}
					if(timenow.after(lastTime) && (daynow.equals(ApiConstants.DAY_MON) || daynow.equals(ApiConstants.DAY_TUE) || daynow.equals(ApiConstants.DAY_WED))){
						workingDays(cal,2,bankHolidaysListCache,datenow,scenario,true);
					}
					if(timenow.after(lastTime) && daynow.equals(ApiConstants.DAY_THU)){
						workingDays(cal,2,bankHolidaysListCache,datenow,scenario,true);
					}
					if(timenow.before(lastTime) && daynow.equals(ApiConstants.DAY_FRI)){
						workingDays(cal,1,bankHolidaysListCache,datenow,scenario,false);
					}
					if(timenow.after(lastTime) && daynow.equals(ApiConstants.DAY_FRI)){
						workingDays(cal,1,bankHolidaysListCache,datenow,scenario,true);
					}
				}
			}
		}
		String finDate = sdf1.format(cal.getTime());
		LOG.info(apiMsgId, "SmartServiceEngine", "CommonUtils", "processPayment",
	            "Calculating effective Date", AmexLogger.Result.success, "","Effective Date",finDate);
		return finDate;
	}

	public static Calendar workingDays(Calendar cal,int noOfDays,Cache bankHolidaysListCache,Date datenow, String scenario, boolean afterCutOff) throws ParseException{
		int count=0;
		String daynow = "";
		SimpleDateFormat sdf1 = new SimpleDateFormat(ApiConstants.DATEFORMAT_SDF);
		SimpleDateFormat holidaysdf = new SimpleDateFormat(ApiConstants.HOL_DF);
		DateFormat format2=new SimpleDateFormat(ApiConstants.DAY_FORMAT); 
		while(count!=noOfDays){
			cal.add(Calendar.DATE, 1);
			String date5 = sdf1.format(cal.getTime());
			datenow= sdf1.parse(date5);
			daynow=format2.format(datenow);
			String todayDate = holidaysdf.format(datenow);
			ValueWrapper element = bankHolidaysListCache.get(todayDate);
			FetchBankHolidaysResultSetVO holiday= null;
			if(element != null){
				holiday = (FetchBankHolidaysResultSetVO)element.get();
			}
			if(holiday!=null && scenario.equals(ApiConstants.OFF_US) && count < noOfDays && afterCutOff){
				count++;
			}
			if(holiday == null && !daynow.equals(ApiConstants.DAY_SAT) && !daynow.equals(ApiConstants.DAY_SUN)){
				count++;
			} 
		}
		return cal;
	}

}
