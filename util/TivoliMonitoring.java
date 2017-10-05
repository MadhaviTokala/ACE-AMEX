package com.americanexpress.smartserviceengine.common.util;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class is used to Tivoli Alert Logs in the Log file.
 *
 */
public class TivoliMonitoring {

	private final Logger LOG = LoggerFactory.getLogger(TivoliMonitoring.class);

	@Autowired
	private Properties applicationErrorCodesProperties;

	public static final String GET_ORG_INFO_ERR_CD = "GET_ORG_INFO_ERR_CD";
	public static final String GET_ORG_INFO_ERR_MSG = "GET_ORG_INFO_ERR_MSG";

	public static final String GET_ORG_ACCNT_ERR_CD = "GET_ORG_ACCNT_ERR_CD";
	public static final String GET_ORG_ACCNT_ERR_MSG = "GET_ORG_ACCNT_ERR_MSG";

	public static final String GET_PYMT_DTLS_ERR_CD = "GET_PYMT_DTLS_ERR_CD";
	public static final String GET_PYMT_DTLS_ERR_MSG = "GET_PYMT_DTLS_ERR_MSG";

	public static final String GET_PYMT_STATUS_ERR_CD = "GET_PYMT_STATUS_ERR_CD";
	public static final String GET_PYMT_STATUS_ERR_MSG = "GET_PYMT_STATUS_ERR_MSG";

	public static final String SSE_SCHED_ERR_CD = "SSE_SCHED_ERR_CD";
	public static final String SSE_SCHED_ERR_MSG = "SSE_SCHED_ERR_MSG";

	public static final String SSE_SP_024_ERR_CD = "SSE_SP_024_ERR_CD";
	public static final String SSE_SP_024_ERR_MSG = "SSE_SP_024_ERR_MSG";

	public static final String SSE_SP_021_ERR_CD = "SSE_SP_021_ERR_CD";
	public static final String SSE_SP_021_ERR_MSG = "SSE_SP_021_ERR_MSG";

	public static final String SSE_SP_022_ERR_CD = "SSE_SP_022_ERR_CD";
	public static final String SSE_SP_022_ERR_MSG = "SSE_SP_022_ERR_MSG";

	public static final String SSE_SP_023_ERR_CD = "SSE_SP_023_ERR_CD";
	public static final String SSE_SP_023_ERR_MSG = "SSE_SP_023_ERR_MSG";

	public static final String SSE_SP_027_ERR_CD = "SSE_SP_027_ERR_CD";
	public static final String SSE_SP_027_ERR_MSG = "SSE_SP_027_ERR_MSG";

	public static final String SSE_SP_028_ERR_CD = "SSE_SP_028_ERR_CD";
	public static final String SSE_SP_028_ERR_MSG = "SSE_SP_028_ERR_MSG";

	public static final String SSE_SP_075_ERR_CD = "SSE_SP_075_ERR_CD";
	public static final String SSE_SP_075_ERR_MSG = "SSE_SP_075_ERR_MSG";

	public static final String SSE_SP_076_ERR_CD = "SSE_SP_076_ERR_CD";
	public static final String SSE_SP_076_ERR_MSG = "SSE_SP_076_ERR_MSG";

	public static final String SSE_SP_079_ERR_CD = "SSE_SP_079_ERR_CD";
	public static final String SSE_SP_079_ERR_MSG = "SSE_SP_079_ERR_MSG";

	public static final String SSE_SP_080_ERR_CD = "SSE_SP_080_ERR_CD";
	public static final String SSE_SP_080_ERR_MSG = "SSE_SP_080_ERR_MSG";

	public static final String SCHEDULAR_PAYMENT_INITIATION_EXEDS_CD = "SCHEDULAR_PAYMENT_INITIATION_EXEDS_CD";
	public static final String SCHEDULAR_PAYMENT_INITIATION_EXEDS_MSG = "SCHEDULAR_PAYMENT_INITIATION_EXEDS_MSG";

	public static final String ADD_ORG_INFO_ERR_CD = "ADD_ORG_INFO_ERR_CD";
	public static final String ADD_ORG_INFO_ERR_MSG = "ADD_ORG_INFO_ERR_MSG";

	public static final String UPD_ORG_INFO_ERR_CD = "UPD_ORG_INFO_ERR_CD";
	public static final String UPD_ORG_INFO_ERR_MSG = "UPD_ORG_INFO_ERR_MSG";

	public static final String ADD_ORG_ACCNT_ERR_CD = "ADD_ORG_ACCNT_ERR_CD";
	public static final String ADD_ORG_ACCNT_ERR_MSG = "ADD_ORG_ACCNT_ERR_MSG";

	public static final String UPD_ORG_ACCNT_ERR_CD = "UPD_ORG_ACCNT_ERR_CD";
	public static final String UPD_ORG_ACCNT_ERR_MSG = "UPD_ORG_ACCNT_ERR_MSG";

	public static final String SSE_APPL_ERR_CD = "SSE_APPL_ERR_CD";
	public static final String SSE_APPL_ERR_MSG = "SSE_APPL_ERR_MSG";

	public static final String SSE_DB_CONN_ERR_CD = "SSE_DB_CONN_ERR_CD";
	public static final String SSE_DB_CONN_ERR_MSG = "SSE_DB_CONN_ERR_MSG";

	public static final String ACH_PYMT_INQUIRY_ERR_CD = "ACH_PYMT_INQUIRY_ERR_CD";
	public static final String ACH_PYMT_INQUIRY_ERR_MSG = "ACH_PYMT_INQUIRY_ERR_MSG";

	public static final String CREATE_ACH_ACCT_ERR_CD = "CREATE_ACH_ACCT_ERR_CD";
	public static final String CREATE_ACH_ACCT_ERR_MSG = "CREATE_ACH_ACCT_ERR_MSG";

	public static final String UPDATE_ACH_ACCT_ERR_CD = "UPDATE_ACH_ACCT_ERR_CD";
	public static final String UPDATE_ACH_ACCT_ERR_MSG = "UPDATE_ACH_ACCT_ERR_MSG";

	public static final String ACH_PROCESS_PAYMENT_ERR_CD = "ACH_PROCESS_PAYMENT_ERR_CD";
	public static final String ACH_PROCESS_PAYMENT_ERR_MSG = "ACH_PROCESS_PAYMENT_ERR_MSG";

	public static final String CREATE_ACH_ACCOUNT_ERR_CD = "CREATE_ACH_ACCOUNT_ERR_CD";
	public static final String CREATE_ACH_ACCOUNT_ERR_MSG = "CREATE_ACH_ACCOUNT_ERR_MSG";

	public static final String SSE_SP_001_ERR_CD = "SSE_SP_001_ERR_CD";
	public static final String SSE_SP_001_ERR_MSG = "SSE_SP_001_ERR_MSG";

	public static final String SSE_SP_002_ERR_CD = "SSE_SP_002_ERR_CD";
	public static final String SSE_SP_002_ERR_MSG = "SSE_SP_002_ERR_MSG";

	public static final String SSE_SP_003_ERR_CD = "SSE_SP_003_ERR_CD";
	public static final String SSE_SP_003_ERR_MSG = "SSE_SP_003_ERR_MSG";

	public static final String SSE_SP_005_ERR_CD = "SSE_SP_005_ERR_CD";
	public static final String SSE_SP_005_ERR_MSG = "SSE_SP_005_ERR_MSG";

	public static final String SSE_SP_006_ERR_CD = "SSE_SP_006_ERR_CD";
	public static final String SSE_SP_006_ERR_MSG = "SSE_SP_006_ERR_MSG";

	public static final String SSE_SP_009_ERR_CD = "SSE_SP_009_ERR_CD";
	public static final String SSE_SP_009_ERR_MSG = "SSE_SP_009_ERR_MSG";

	public static final String SSE_SP_011_ERR_CD = "SSE_SP_011_ERR_CD";
	public static final String SSE_SP_011_ERR_MSG = "SSE_SP_011_ERR_MSG";

	public static final String SSE_SP_014_ERR_CD = "SSE_SP_014_ERR_CD";
	public static final String SSE_SP_014_ERR_MSG = "SSE_SP_014_ERR_MSG";

	public static final String SSE_SP_017_ERR_CD = "SSE_SP_017_ERR_CD";
	public static final String SSE_SP_017_ERR_MSG = "SSE_SP_017_ERR_MSG";

	public static final String SSE_SP_018_ERR_CD = "SSE_SP_018_ERR_CD";
	public static final String SSE_SP_018_ERR_MSG = "SSE_SP_018_ERR_MSG";

	public static final String SSE_SP_019_ERR_CD = "SSE_SP_019_ERR_CD";
	public static final String SSE_SP_019_ERR_MSG = "SSE_SP_019_ERR_MSG";

	public static final String SSE_SP_020_ERR_CD = "SSE_SP_020_ERR_CD";
	public static final String SSE_SP_020_ERR_MSG = "SSE_SP_020_ERR_MSG";

	public static final String SSE_SP_025_ERR_CD = "SSE_SP_025_ERR_CD";
	public static final String SSE_SP_025_ERR_MSG = "SSE_SP_025_ERR_MSG";

	public static final String SSE_SP_026_ERR_CD = "SSE_SP_026_ERR_CD";
	public static final String SSE_SP_026_ERR_MSG = "SSE_SP_026_ERR_MSG";

	public static final String SSE_SP_029_ERR_CD = "SSE_SP_029_ERR_CD";
	public static final String SSE_SP_029_ERR_MSG = "SSE_SP_029_ERR_MSG";

	public static final String SSE_SP_045_ERR_CD = "SSE_SP_045_ERR_CD";
	public static final String SSE_SP_045_ERR_MSG = "SSE_SP_045_ERR_MSG";

	public static final String SSE_SP_047_ERR_CD = "SSE_SP_047_ERR_CD";
	public static final String SSE_SP_047_ERR_MSG = "SSE_SP_047_ERR_MSG";

	public static final String SSE_SP_049_ERR_CD = "SSE_SP_049_ERR_CD";
	public static final String SSE_SP_049_ERR_MSG = "SSE_SP_049_ERR_MSG";

	public static final String SSE_SP_052_ERR_CD = "SSE_SP_052_ERR_CD";
	public static final String SSE_SP_052_ERR_MSG = "SSE_SP_052_ERR_MSG";

	public static final String SSE_SP_070_ERR_CD = "SSE_SP_070_ERR_CD";
	public static final String SSE_SP_070_ERR_MSG = "SSE_SP_070_ERR_MSG";

	// ACH Error Codes

	public static final String SSE_SP_057_ERR_CD = "SSE_SP_057_ERR_CD";
	public static final String SSE_SP_057_ERR_MSG = "SSE_SP_057_ERR_MSG";

	public static final String SSE_SP_058_ERR_CD = "SSE_SP_058_ERR_CD";
	public static final String SSE_SP_058_ERR_MSG = "SSE_SP_058_ERR_MSG";

	public static final String SSE_SP_063_ERR_CD = "SSE_SP_063_ERR_CD";
	public static final String SSE_SP_063_ERR_MSG = "SSE_SP_063_ERR_MSG";

	public static final String SSE_SP_064_ERR_CD = "SSE_SP_064_ERR_CD";
	public static final String SSE_SP_064_ERR_MSG = "SSE_SP_064_ERR_MSG";

	public static final String SSE_SP_065_ERR_CD = "SSE_SP_065_ERR_CD";
	public static final String SSE_SP_065_ERR_MSG = "SSE_SP_065_ERR_MSG";

	public static final String SSE_SP_059_ERR_CD = "SSE_SP_059_ERR_CD";
	public static final String SSE_SP_059_ERR_MSG = "SSE_SP_059_ERR_MSG";

	public static final String SSE_SP_066_ERR_CD = "SSE_SP_066_ERR_CD";
	public static final String SSE_SP_066_ERR_MSG = "SSE_SP_066_ERR_MSG";

	public static final String SSE_SP_073_ERR_CD = "SSE_SP_073_ERR_CD";
	public static final String SSE_SP_073_ERR_MSG = "SSE_SP_073_ERR_MSG";

	public static final String SSE_SP_074_ERR_CD = "SSE_SP_074_ERR_CD";
	public static final String SSE_SP_074_ERR_MSG = "SSE_SP_074_ERR_MSG";

	public static final String SSE_SP_084_ERR_CD = "SSE_SP_084_ERR_CD";
	public static final String SSE_SP_084_ERR_MSG = "SSE_SP_084_ERR_MSG";

	public static final String SSE_SP_081_ERR_CD = "SSE_SP_081_ERR_CD";
	public static final String SSE_SP_081_ERR_MSG = "SSE_SP_081_ERR_MSG";

	public static final String SSE_SP_091_ERR_CD = "SSE_SP_091_ERR_CD";
	public static final String SSE_SP_091_ERR_MSG = "SSE_SP_091_ERR_MSG";

	public static final String SSE_SP_067_ERR_CD = "SSE_SP_067_ERR_CD";
	public static final String SSE_SP_067_ERR_MSG = "SSE_SP_067_ERR_MSG";

	public static final String SSE_SP_085_ERR_CD = "SSE_SP_085_ERR_CD";
	public static final String SSE_SP_085_ERR_MSG = "SSE_SP_085_ERR_MSG";

	public static final String SSE_SP_077_ERR_CD = "SSE_SP_077_ERR_CD";
	public static final String SSE_SP_077_ERR_MSG = "SSE_SP_077_ERR_MSG";

	public static final String SSE_SP_054_ERR_CD = "SSE_SP_054_ERR_CD";
	public static final String SSE_SP_054_ERR_MSG = "SSE_SP_054_ERR_MSG";

	public static final String SSE_SP_089_ERR_CD = "SSE_SP_089_ERR_CD";
	public static final String SSE_SP_089_ERR_MSG = "SSE_SP_089_ERR_MSG";

	public static final String SSE_SP_092_ERR_CD = "SSE_SP_092_ERR_CD";
	public static final String SSE_SP_092_ERR_MSG = "SSE_SP_092_ERR_MSG";

	public static final String SSE_SP_097_ERR_CD = "SSE_SP_097_ERR_CD";
	public static final String SSE_SP_097_ERR_MSG = "SSE_SP_097_ERR_MSG";

	public static final String SSE_SP_098_ERR_CD = "SSE_SP_098_ERR_CD";
	public static final String SSE_SP_098_ERR_MSG = "SSE_SP_098_ERR_MSG";

	public static final String SSE_SP_095_ERR_CD = "SSE_SP_095_ERR_CD";
	public static final String SSE_SP_095_ERR_MSG = "SSE_SP_095_ERR_MSG";
	
	public static final String SSE_SP_148_ERR_CD = "SSE_SP_148_ERR_CD";
	public static final String SSE_SP_148_ERR_MSG = "SSE_SP_148_ERR_MSG";
	
	public static final String SSE_MICRO_PYMT_CREDIT_REVERSAL_CD = "SSE_MICRO_PYMT_CREDIT_REVERSAL_CD";
	public static final String SSE_MICRO_PYMT_CREDIT_REVERSAL_MSG = "SSE_MICRO_PYMT_CREDIT_REVERSAL_MSG";

	public static final String SSE_ACH_DEBIT_REVERSAL_DONE_RS_PAID_CREDIT_FOUND_CD = "SSE_ACH_DEBIT_REVERSAL_DONE_RS_PAID_CREDIT_FOUND_CD";
	public static final String SSE_ACH_DEBIT_REVERSAL_DONE_RS_PAID_CREDIT_FOUND_MSG = "SSE_ACH_DEBIT_REVERSAL_DONE_RS_PAID_CREDIT_FOUND_MSG";

	public static final String SSE_ACH_REVERSAL_FAILED_CD = "SSE_ACH_REVERSAL_FAILED_CD";
	public static final String SSE_ACH_REVERSAL_FAILED_MSG = "SSE_ACH_REVERSAL_FAILED_MSG";

	/*public static final String SSE_ACH_DEBIT_FAILED_CREDIT_FOUND_CD = "SSE_ACH_DEBIT_FAILED_CREDIT_FOUND_CD";
	public static final String SSE_ACH_DEBIT_FAILED_CREDIT_FOUND_MSG = "SSE_ACH_DEBIT_FAILED_CREDIT_FOUND_MSG";

	public static final String SSE_ACH_CREDIT_FAILED_DEBIT_FOUND_CD = "SSE_ACH_CREDIT_FAILED_DEBIT_FOUND_CD";
	public static final String SSE_ACH_CREDIT_FAILED_DEBIT_FOUND_MSG = "SSE_ACH_CREDIT_FAILED_DEBIT_FOUND_MSG";
*/
	public static final String SSE_ACH_RS_PAID_CREDIT_FAILED_DEBIT_FOUND_CD = "SSE_ACH_RS_PAID_CREDIT_FAILED_DEBIT_FOUND_CD";
	public static final String SSE_ACH_RS_PAID_CREDIT_FAILED_DEBIT_FOUND_MSG = "SSE_ACH_RS_PAID_CREDIT_FAILED_DEBIT_FOUND_MSG";

	public static final String SSE_ACH_RS_PAID_DEBIT_FOUND_CREDIT_FAILED_CD = "SSE_ACH_RS_PAID_DEBIT_FOUND_CREDIT_FAILED_CD";
	public static final String SSE_ACH_RS_PAID_DEBIT_FOUND_CREDIT_FAILED_MSG = "SSE_ACH_RS_PAID_DEBIT_FOUND_CREDIT_FAILED_MSG";

	public static final String SSE_SP_094_ERR_CD = "SSE_SP_094_ERR_CD";
	public static final String SSE_SP_094_ERR_MSG = "SSE_SP_094_ERR_MSG";

	public static final String SSE_SP_096_ERR_CD = "SSE_SP_096_ERR_CD";
	public static final String SSE_SP_096_ERR_MSG = "SSE_SP_096_ERR_MSG";

	public static final String SSE_SP_046_ERR_CD = "SSE_SP_046_ERR_CD";
	public static final String SSE_SP_046_ERR_MSG = "SSE_SP_046_ERR_MSG";

	public static final String SSE_SCREENDATA_SERVICE_ERR_CD = "SSE_SCREENDATA_SERVICE_ERR_CD";
	public static final String SSE_SCREENDATA_SERVICE_ERR_CD_MSG = "SSE_SCREENDATA_SERVICE_ERR_CD_MSG";

	public static final String SCHED_PYMT_INIT_MICROCREDIT_FAIL_CD = "SCHED_PYMT_INIT_MICROCREDIT_FAIL_CD";
	public static final String SCHED_PYMT_INIT_MICROCREDIT_FAIL_MSG = "SCHED_PYMT_INIT_MICROCREDIT_FAIL_MSG";

	public static final String SCHED_PYMT_INIT_ACHCREDIT_FAIL_CD = "SCHED_PYMT_INIT_ACHCREDIT_FAIL_CD";
	public static final String SCHED_PYMT_INIT_ACHCREDIT_FAIL_MSG = "SCHED_PYMT_INIT_ACHCREDIT_FAIL_MSG";

	public static final String SCHEDULAR_BRIDGER_ERROR_CD = "SCHEDULAR_BRIDGER_ERROR_CD";
	public static final String SCHEDULAR_BRIDGER_ERROR_DESC = "SCHEDULAR_BRIDGER_ERROR_DESC";

	public static final String SCHEDULAR_BRIDGER_POSITIVE_HIT_CD = "SCHEDULAR_BRIDGER_POSITIVE_HIT_CD";
	public static final String SCHEDULAR_BRIDGER_POSITIVE_HIT_DESC = "SCHEDULAR_BRIDGER_POSITIVE_HIT_DESC";

	public static final String GET_PYMT_DTLS_NOT_FOUND_ERR_CD = "GET_PYMT_DTLS_NOT_FOUND_ERR_CD";
	public static final String GET_PYMT_DTLS_NOT_FOUND_ERR_MSG = "GET_PYMT_DTLS_NOT_FOUND_ERR_MSG";

	public static final String GET_PYMT_DTLS_NOT_PROCESSED_ERR_CD = "GET_PYMT_DTLS_NOT_PROCESSED_ERR_CD";
	public static final String GET_PYMT_DTLS_NOT_PROCESSED_ERR_MSG = "GET_PYMT_DTLS_NOT_PROCESSED_ERR_MSG";

	public static final String SSE_SP_126_ERR_CD = "SSE_SP_126_ERR_CD";
	public static final String SSE_SP_126_ERR_MSG = "SSE_SP_126_ERR_MSG";

	// Liberty Specific
	public static final String SSE_LIBERTY_ACH_REVERSAL_FAILED_CD = "SSE_LIBERTY_ACH_REVERSAL_FAILED_CD";
	public static final String SSE_LIBERTY_ACH_REVERSAL_FAILED_MSG = "SSE_LIBERTY_ACH_REVERSAL_FAILED_MSG";

	public static final String SSE_SP_108_ERR_CD = "SSE_SP_108_ERR_CD";
	public static final String SSE_SP_108_ERR_MSG = "SSE_SP_108_ERR_MSG";

	public static final String SSE_SP_143_ERR_CD = "SSE_SP_143_ERR_CD";
	public static final String SSE_SP_143_ERR_MSG = "SSE_SP_143_ERR_MSG";

	public static final String SSE_SP_101_ERR_CD = "SSE_SP_101_ERR_CD";
	public static final String SSE_SP_101_ERR_MSG = "SSE_SP_101_ERR_MSG";

	public static final String SSE_SP_102_ERR_CD = "SSE_SP_102_ERR_CD";
	public static final String SSE_SP_102_ERR_MSG = "SSE_SP_102_ERR_MSG";

	public static final String SSE_SP_103_ERR_CD = "SSE_SP_103_ERR_CD";
	public static final String SSE_SP_103_ERR_MSG = "SSE_SP_103_ERR_MSG";

	public static final String SSE_SP_104_ERR_CD = "SSE_SP_104_ERR_CD";
	public static final String SSE_SP_104_ERR_MSG = "SSE_SP_104_ERR_MSG";

	public static final String SSE_SP_105_ERR_CD = "SSE_SP_105_ERR_CD";
	public static final String SSE_SP_105_ERR_MSG = "SSE_SP_105_ERR_MSG";

	public static final String SSE_SP_106_ERR_CD = "SSE_SP_106_ERR_CD";
	public static final String SSE_SP_106_ERR_MSG = "SSE_SP_106_ERR_MSG";

	public static final String SSE_SP_109_ERR_CD = "SSE_SP_109_ERR_CD";
	public static final String SSE_SP_109_ERR_MSG = "SSE_SP_109_ERR_MSG";

    public static final String SSE_SP_090_ERR_CD = "SSE_SP_090_ERR_CD";
    public static final String SSE_SP_090_ERR_MSG = "SSE_SP_090_ERR_MSG";

    public static final String SSE_SP_147_ERR_CD = "SSE_SP_147_ERR_CD";
    public static final String SSE_SP_147_ERR_MSG = "SSE_SP_147_ERR_MSG";

    public static final String SSE_SP_055_ERR_CD = "SSE_SP_055_ERR_CD";
    public static final String SSE_SP_055_ERR_MSG = "SSE_SP_055_ERR_MSG";

    public static final String SSE_SP_142_ERR_CD = "SSE_SP_142_ERR_CD";
    public static final String SSE_SP_142_ERR_MSG = "SSE_SP_142_ERR_MSG";

	public static final String SSE_SP_121_ERR_CD = "SSE_SP_121_ERR_CD";
	public static final String SSE_SP_121_ERR_MSG = "SSE_SP_121_ERR_MSG";

	public static final String SSE_SP_120_ERR_CD = "SSE_SP_120_ERR_CD";
	public static final String SSE_SP_120_ERR_MSG = "SSE_SP_120_ERR_MSG";

	public static final String SSE_SP_128_ERR_CD = "SSE_SP_128_ERR_CD";
	public static final String SSE_SP_128_ERR_MSG = "SSE_SP_128_ERR_MSG";

	public static final String SSE_SP_053_ERR_CD = "SSE_SP_053_ERR_CD";
	public static final String SSE_SP_053_ERR_MSG = "SSE_SP_053_ERR_MSG";

	public static final String SSE_SP_127_ERR_CD = "SSE_SP_127_ERR_CD";
	public static final String SSE_SP_127_ERR_MSG = "SSE_SP_127_ERR_MSG";

	public static final String SSE_ICRUSE_CALL_ERR_CD = "SSE_ICRUSE_CALL_ERR_CD";
	public static final String SSE_ICRUSE_CALL_ERR_MSG = "SSE_ICRUSE_CALL_ERR_MSG";

	public static final String SCHED_GCS_FAIL_PAYMENT_RECEIPT_CD = "SCHED_GCS_FAIL_PAYMENT_RECEIPT_CD";
    public static final String SCHED_GCS_FAIL_PAYMENT_RECEIPT_MSG = "SCHED_GCS_FAIL_PAYMENT_RECEIPT_MSG";

    public static final String SSE_GCS_FAILURE_ERR_CD = "SSE_GCS_FAILURE_ERR_CD";
    public static final String SSE_GCS_FAILURE_ERR_MSG = "SSE_GCS_FAILURE_ERR_MSG";

    public static final String SSE_SP_150_ERR_CD = "SSE_SP_150_ERR_CD";
    public static final String SSE_SP_150_ERR_MSG = "SSE_SP_150_ERR_MSG";

    public static final String SSE_SP_149_ERR_CD = "SSE_SP_0149_ERR_CD";
    public static final String SSE_SP_149_ERR_MSG = "SSE_SP_0149_ERR_MSG";
    
    public static final String SSE_SP_119_ERR_CD = "SSE_SP_119_ERR_CD";
    public static final String SSE_SP_119_ERR_MSG = "SSE_SP_119_ERR_MSG";

    public static final String SCHED_GCS_FAIL_SUPPLIER_REMITTANCE_CD = "SCHED_GCS_FAIL_SUPPLIER_REMITTANCE_CD";
    public static final String SCHED_GCS_FAIL_SUPPLIER_REMITTANCE_MSG = "SCHED_GCS_FAIL_SUPPLIER_REMITTANCE_MSG";

    public static final String SCHED_GCS_FAIL_ICM_PUSH_CD = "SCHED_GCS_FAIL_ICM_PUSH_CD";
    public static final String SCHED_GCS_FAIL_ICM_PUSH_MSG = "SCHED_GCS_FAIL_ICM_PUSH_MSG";

    public static final String SCHED_GCS_FAIL_ENROLL_SUCCESS_CD = "SCHED_GCS_FAIL_ENROLL_SUCCESS_CD";
    public static final String SCHED_GCS_FAIL_ENROLL_SUCCESS_MSG = "SCHED_GCS_FAIL_ENROLL_SUCCESS_MSG";

    public static final String SCHED_GCS_FAIL_ENROLL_FAILURE_CD = "SCHED_GCS_FAIL_ENROLL_FAILURE_CD";
    public static final String SCHED_GCS_FAIL_ENROLL_FAILURE_MSG = "SCHED_GCS_FAIL_ENROLL_FAILURE_MSG";

    public static final String SCHED_GCS_FAIL_CONFIRM_MICRO_DEBIT_CD = "SCHED_GCS_FAIL_CONFIRM_MICRO_DEBIT_CD";
    public static final String SCHED_GCS_FAIL_CONFIRM_MICRO_DEBIT_MSG = "SCHED_GCS_FAIL_CONFIRM_MICRO_DEBIT_MSG";

    public static final String SCHED_GCS_FAIL_REMAINDER_MAIL_CD = "SCHED_GCS_FAIL_REMAINDER_MAIL_CD";
    public static final String SCHED_GCS_FAIL_REMAINDER_MAIL_MSG = "SCHED_GCS_FAIL_REMAINDER_MAIL_MSG";
 
    public static final String SSE_ROLL_BACK_EXCEP_CD = "SSE_ROLL_BACK_EXCEP_CD";
    public static final String SSE_ROLL_BACK_EXCEP_MSG = "SSE_ROLL_BACK_EXCEP_MSG";

    public static final String SCHED_TANDC_INVALID_URL_CD = "SCHED_TANDC_INVALID_URL_CD";
    public static final String SCHED_TANDC_INVALID_URL_MSG = "SCHED_TANDC_INVALID_URL_MSG";

	public static final String SSE_SP_125_ERR_CD = "SSE_SP_125_ERR_CD";
	public static final String SSE_SP_125_ERR_MSG = "SSE_SP_125_ERR_MSG";

	public static final String SCHED_ICRUSE_1_RESP_ERR_CD="SCHED_ICRUSE_1_RESP_ERR_CD";
	public static final String SCHED_ICRUSE_1_RESP_ERR_MSG="SCHED_ICRUSE_1_RESP_ERR_MSG";
	
	public static final String SCHED_ICRUSE_2_RESP_ERR_CD="SCHED_ICRUSE_2_RESP_ERR_CD";
	public static final String SCHED_ICRUSE_2_RESP_ERR_MSG="SCHED_ICRUSE_2_RESP_ERR_MSG";
	
	public static final String SCHED_ICRUSE_RESP_ERR_CD="SCHED_ICRUSE_RESP_ERR_CD";
	public static final String SCHED_ICRUSE_RESP_ERR_MSG="SCHED_ICRUSE_RESP_ERR_MSG";

	public static final String SHED_ICRUSE_TIMEOUT_ERR_CD = "SHED_ICRUSE_TIMEOUT_ERR_CD";
	public static final String SHED_ICRUSE_TIMEOUT_ERR_MSG = "SHED_ICRUSE_TIMEOUT_ERR_MSG";

        public static final String BIP_TIMEOUT_ERR_CD = "BIP_TIMEOUT_ERR_CD";
        public static final String BIP_TIMEOUT_ERR_MSG = "BIP_TIMEOUT_ERR_MSG";

        public static final String EMM_TIMEOUT_ERR_CD = "EMM_TIMEOUT_ERR_CD";
        public static final String EMM_TIMEOUT_ERR_MSG = "EMM_TIMEOUT_ERR_MSG";

        public static final String BRIDGER_TIMEOUT_ERR_CD = "BRIDGER_TIMEOUT_ERR_CD";
        public static final String BRIDGER_TIMEOUT_ERR_MSG = "BRIDGER_TIMEOUT_ERR_MSG";

		public static final String SSE_SP_093_ERR_CD = "SSE_SP_093_ERR_CD";
		public static final String SSE_SP_093_ERR_MSG = "SSE_SP_093_ERR_MSG";
		
	    public static final String SCHED_GCS_FAIL_CM_WELCOME_RSVP_MAIL_CD = "SCHED_GCS_FAIL_CM_WELCOME_RSVP_MAIL_CD";
	    public static final String SCHED_GCS_FAIL_CM_WELCOME_RSVP_MAIL_MSG = "SCHED_GCS_FAIL_CM_WELCOME_RSVP_MAIL_MSG";

	    public static final String SCHED_GCS_FAIL_CM_REMAINDER_RSVP_MAIL_CD = "SCHED_GCS_FAIL_CM_REMAINDER_RSVP_MAIL_CD";
	    public static final String SCHED_GCS_FAIL_CM_REMAINDER_RSVP_MAIL_MSG = "SCHED_GCS_FAIL_CM_REMAINDER_RSVP_MAIL_MSG";
	    
	    public static final String SCHED_GCS_FAIL_ABO_REMAINDER_RSVP_MAIL_CD = "SCHED_GCS_FAIL_ABO_REMAINDER_RSVP_MAIL_CD";
	    public static final String SCHED_GCS_FAIL_ABO_REMAINDER_RSVP_MAIL_MSG = "SCHED_GCS_FAIL_ABO_REMAINDER_RSVP_MAIL_MSG";

		public static final String SSE_SP_702_ERR_CD = "SSE_SP_702_ERR_CD";
		public static final String SSE_SP_702_ERR_MSG = "SSE_SP_702_ERR_MSG";

		public static final String SSE_SP_100_ERR_CD = "SSE_SP_100_ERR_CD";
		public static final String SSE_SP_100_ERR_MSG = "SSE_SP_100_ERR_MSG";
		
		public static final String SSE_SP_151_ERR_CD = "SSE_SP_151_ERR_CD";
		public static final String SSE_SP_151_ERR_MSG = "SSE_SP_151_ERR_MSG";

		public static final String SSE_SP_152_ERR_CD = "SSE_SP_152_ERR_CD";
		public static final String SSE_SP_152_ERR_MSG = "SSE_SP_152_ERR_MSG";
		
		public static final String SSE_SP_153_ERR_CD = "SSE_SP_153_ERR_CD";
		public static final String SSE_SP_153_ERR_MSG = "SSE_SP_153_ERR_MSG";

		
		public static final String SSE_SP_154_ERR_CD = "SSE_SP_154_ERR_CD";
		public static final String SSE_SP_154_ERR_MSG = "SSE_SP_154_ERR_MSG";
		
		public static final String SSE_SP_155_ERR_CD = "SSE_SP_155_ERR_CD";
		public static final String SSE_SP_155_ERR_MSG = "SSE_SP_155_ERR_MSG";
		
		public static final String SSE_SP_156_ERR_CD = "SSE_SP_156_ERR_CD";
		public static final String SSE_SP_156_ERR_MSG = "SSE_SP_156_ERR_MSG";

		public static final String GDAS_CARD_VALIDATION_ERR_CD="GDAS_CARD_VALIDATION_ERR_CD";
		public static final String GDAS_CARD_VALIDATION_ERR_MSG="GDAS_CARD_VALIDATION_ERR_MSG";

		public static final String GDAS_RESPONSE_NULL_CD="GDAS_RESPONSE_NULL_CD";
		public static final String GDAS_RESPONSE_NULL_MSG="GDAS_RESPONSE_NULL_MSG";

		public static final String NGSS_ENROLL_FPAN_ERR_CD="NGSS_ENROLL_FPAN_ERR_CD";
		public static final String NGSS_ENROLL_FPAN_ERR_MSG="NGSS_ENROLL_FPAN_ERR_MSG";
		
		public static final String SUPP_REMITTANCE_EMAIL_FAIL_ERR_CD = "SUPP_REMITTANCE_EMAIL_FAIL_ERR_CD";
		public static final String SUPP_REMITTANCE_EMAIL_FAIL_ERR_MSG = "SUPP_REMITTANCE_EMAIL_FAIL_ERR_MSG";

	public static final String SSE_SP_701_ERR_CD = "SSE_SP_701_ERR_CD";
	public static final String SSE_SP_701_ERR_MSG = "SSE_SP_701_ERR_MSG";
	
	public static final String SSE_SP_703_ERR_CD = "SSE_SP_703_ERR_CD";
	public static final String SSE_SP_703_ERR_MSG = "SSE_SP_703_ERR_MSG";
	
	public static final String SSE_SP_704_ERR_CD = "SSE_SP_704_ERR_CD";
	public static final String SSE_SP_704_ERR_MSG = "SSE_SP_704_ERR_MSG";
	
	public static final String SSE_SP_705_ERR_CD = "SSE_SP_705_ERR_CD";
	public static final String SSE_SP_705_ERR_MSG = "SSE_SP_705_ERR_MSG";
	
	public static final String SSE_SP_706_ERR_CD = "SSE_SP_706_ERR_CD";
	public static final String SSE_SP_706_ERR_MSG = "SSE_SP_706_ERR_MSG";
	
	public static final String SSE_SP_707_ERR_CD = "SSE_SP_707_ERR_CD";
	public static final String SSE_SP_707_ERR_MSG = "SSE_SP_707_ERR_MSG";
	
	public static final String SSE_SP_708_ERR_CD = "SSE_SP_708_ERR_CD";
	public static final String SSE_SP_708_ERR_MSG = "SSE_SP_708_ERR_MSG";
	
	public static final String SSE_SP_713_ERR_CD = "SSE_SP_713_ERR_CD";
	public static final String SSE_SP_713_ERR_MSG = "SSE_SP_713_ERR_MSG";
	
	public static final String SSE_SP_724_ERR_CD = "SSE_SP_724_ERR_CD";
	public static final String SSE_SP_724_ERR_MSG = "SSE_SP_724_ERR_MSG";
	
	public static final String SSE_SP_709_ERR_CD = "SSE_SP_709_ERR_CD";
	public static final String SSE_SP_709_ERR_MSG = "SSE_SP_709_ERR_MSG";		
	
	
	public static final String SSE_EMAIL_FAILURE_ERR_CD = "SSE_EMAIL_FAILURE_ERR_CD";
    public static final String SSE_EMAIL_FAILURE_ERR_MSG = "SSE_EMAIL_FAILURE_ERR_MSG";
    
    public static final String SSE_TKN_PAY_PROCESS_ERR_CD = "SSE_TKN_PAY_PROCESS_ERR_CD";
    public static final String SSE_TKN_PAY_PROCESS_ERR_MSG = "SSE_TKN_PAY_PROCESS_ERR_MSG";
    
    public static final String SSE_UNABLE_TO_UPDATE_EMAIL_SENT_STATUS_CD = "SSE_UNABLE_TO_UPDATE_EMAIL_SENT_STATUS_CD";
    public static final String SSE_UNABLE_TO_UPDATE_EMAIL_SENT_STATUS_MSG = "SSE_UNABLE_TO_UPDATE_EMAIL_SENT_STATUS_MSG";
    
/*	    public static final String SSE_CANCEL_TKN_PROCESS_ERR_CD = "SSE_CANCEL_TKN_PROCESS_ERR_CD";
	    public static final String SSE_CANCEL_TKN_PROCESS_ERR_MSG = "SSE_CANCEL_TKN_PROCESS_ERR_MSG";*/
    
	public static final String SSE_ACCT_VALIDATION_PROCESS_ERR_CD = "SSE_ACCT_VALIDATION_PROCESS_ERR_CD";
    public static final String SSE_ACCT_VALIDATION_PROCESS_ERR_MSG = "SSE_ACCT_VALIDATION_PROCESS_ERR_MSG";
    
    public static final String SSE_NGSS_RESPONSE_NULL_CD = "SSE_NGSS_RESPONSE_NULL_CD";
    public static final String SSE_NGSS_RESPONSE_NULL_MSG = "SSE_NGSS_RESPONSE_NULL_MSG";

    public static final String SSE_POST_AUTH_RESPONSE_ERR_CD = "SSE_POST_AUTH_RESPONSE_ERR_CD";
    public static final String SSE_POST_AUTH_RESPONSE_ERR_MSG = "SSE_POST_AUTH_RESPONSE_ERR_MSG";
    
    public static final String SSE_EMAIL_AGE_SCORE_VALIDATION_ERR_CD = "SSE_EMAIL_AGE_SCORE_VALIDATION_ERR_CD";
    public static final String SSE_EMAIL_AGE_SCORE_VALIDATION_ERR_MSG = "SSE_EMAIL_AGE_SCORE_VALIDATION_ERR_MSG";
    
    public static final String SSE_ISSUE_TKN_ERR_CD = "SSE_ISSUE_TKN_ERR_CD";
    public static final String SSE_ISSUE_TKN_ERR_MSG = "SSE_ISSUE_TKN_ERR_MSG";
    
    public static final String SSE_MANAGE_TKN_ERR_CD = "SSE_MANAGE_TKN_ERR_CD";
    public static final String SSE_MANAGE_TKN_ERR_MSG = "SSE_MANAGE_TKN_ERR_MSG";
    
    public static final String SSE_EMAIL_AGE_SCORE_RESPONSE_NULL_CD = "SSE_EMAIL_AGE_SCORE_RESPONSE_NULL_CD";
    public static final String SSE_EMAIL_AGE_SCORE_RESPONSE_NULL_MSG = "SSE_EMAIL_AGE_SCORE_RESPONSE_NULL_MSG";
	
    public static final String SSE_DCP_RESPONSE_NULL_CD = "SSE_DCP_RESPONSE_NULL_CD";
    public static final String SSE_DCP_RESPONSE_NULL_MSG = "SSE_DCP_RESPONSE_NULL_MSG";
    
    public static final String SSE_DCP_CARD_VALIDATION_ERR_CD = "SSE_DCP_CARD_VALIDATION_ERR_CD";
    public static final String SSE_DCP_CARD_VALIDATION_ERR_MSG = "SSE_DCP_CARD_VALIDATION_ERR_MSG";
    
    public static final String SSE_SP_A702_ERR_CD = "SSE_SP_A702_ERR_CD";
    public static final String SSE_SP_A702_ERR_MSG = "SSE_SP_A702_ERR_MSG";
    
    public static final String SSE_SP_A703_ERR_CD = "SSE_SP_A703_ERR_CD";
    public static final String SSE_SP_A703_ERR_MSG = "SSE_SP_A703_ERR_MSG";
    
    public static final String SSE_SP_A705_ERR_CD = "SSE_SP_A705_ERR_CD";
    public static final String SSE_SP_A705_ERR_MSG = "SSE_SP_A705_ERR_MSG";
    
    public static final String SSE_SP_GCA_101_ERR_CD = "SSE_SP_GCA_101_ERR_CD";
    public static final String SSE_SP_GCA_101_ERR_MSG = "SSE_SP_GCA_101_ERR_MSG";
    
    public static final String SSE_SP_P715_NO_DOMAIN_CTRLS_CD = "SSE_SP_P715_NO_DOMAIN_CTRLS_CD";
    public static final String SSE_SP_P715_NO_DOMAIN_CTRLS_MSG = "SSE_SP_P715_NO_DOMAIN_CTRLS_MSG";
    
    public static final String SSE_SP_P715_ERR_CD = "SSE_SP_P715_ERR_CD";
    public static final String SSE_SP_P715_ERR_MSG = "SSE_SP_P715_ERR_MSG";
    
    public static final String SSE_SP_P710_ERR_CD = "SSE_SP_P710_ERR_CD";
    public static final String SSE_SP_P710_ERR_MSG = "SSE_SP_P710_ERR_MSG";
    
    public static final String SSE_SP_P711_ERR_CD = "SSE_SP_P711_ERR_CD";
    public static final String SSE_SP_P711_ERR_MSG = "SSE_SP_P711_ERR_MSG";
    
    public static final String SSE_SP_P719_ERR_CD = "SSE_SP_P719_ERR_CD";
    public static final String SSE_SP_P719_ERR_MSG = "SSE_SP_P719_ERR_MSG";    
    

    public static final String SSE_SP_P712_ERR_CD="SSE_SP_P712_ERR_CD";
    public static final String SSE_SP_P712_ERR_MSG="SSE_SP_P712_ERR_MSG";

	public static final String SSE_SP_P713_ERR_CD="SSE_SP_P713_ERR_CD";
	public static final String SSE_SP_P713_ERR_MSG="SSE_SP_P713_ERR_MSG";
	
		//API 2.0 Error Codes
		
		public static final String SSE_SP_165_ERR_CD = "SSE_SP_165_ERR_CD";
		public static final String SSE_SP_165_ERR_MSG = "SSE_SP_165_ERR_MSG";

		public static final String SSE_SP_400_ERR_CD = "SSE_SP_400_ERR_CD";
		public static final String SSE_SP_400_ERR_MSG = "SSE_SP_400_ERR_MSG";

		public static final String SSE_SP_401_ERR_CD = "SSE_SP_401_ERR_CD";
		public static final String SSE_SP_401_ERR_MSG = "SSE_SP_401_ERR_MSG";

		public static final String SSE_SP_402_ERR_CD = "SSE_SP_402_ERR_CD";
		public static final String SSE_SP_402_ERR_MSG = "SSE_SP_402_ERR_MSG";
	public static final String SSE_SP_P714_ERR_CD="SSE_SP_P714_ERR_CD";
	public static final String SSE_SP_P714_ERR_MSG="SSE_SP_P714_ERR_MSG";
	
	public static final String SSE_SP_P716_ERR_CD="SSE_SP_P716_ERR_CD";
	public static final String SSE_SP_P716_ERR_MSG="SSE_SP_P716_ERR_MSG";
	
	public static final String SSE_SP_P717_ERR_CD="SSE_SP_P717_ERR_CD";
	public static final String SSE_SP_P717_ERR_MSG="SSE_SP_P717_ERR_MSG";
	
	public static final String SSE_SP_P720_ERR_CD="SSE_SP_P720_ERR_CD";
	public static final String SSE_SP_P720_ERR_MSG="SSE_SP_P720_ERR_MSG";
	
	public static final String SSE_SP_P721_ERR_CD="SSE_SP_P721_ERR_CD";
	public static final String SSE_SP_P721_ERR_MSG="SSE_SP_P721_ERR_MSG";
	
	public static final String SSE_SP_P723_ERR_CD="SSE_SP_P723_ERR_CD";
	public static final String SSE_SP_P723_ERR_MSG="SSE_SP_P723_ERR_MSG";

    public static final String SSE_SP_GCM_004_ERR_CD = "SSE_SP_GCM_004_ERR_CD";
	public static final String SSE_SP_GCM_004_ERR_MSG = "SSE_SP_GCM_004_ERR_MSG";
	   
	public static final String SSE_SP_GCA_102_ERR_CD = "SSE_SP_GCA_102_ERR_CD";
	public static final String SSE_SP_GCA_102_ERR_MSG = "SSE_SP_GCA_102_ERR_MSG";

	public static final String SSE_SP_406_ERR_CD = "SSE_SP_406_ERR_CD";
	public static final String SSE_SP_406_ERR_MSG = "SSE_SP_406_ERR_MSG";

	public static final String SSE_SP_726_ERR_CD = "SSE_SP_726_ERR_CD";
	public static final String SSE_SP_726_ERR_MSG = "SSE_SP_726_ERR_MSG";

	public static final String SSE_SP_727_ERR_CD = "SSE_SP_727_ERR_CD";
	public static final String SSE_SP_727_ERR_MSG = "SSE_SP_727_ERR_MSG";

	public static final String SSE_SP_728_ERR_CD = "SSE_SP_728_ERR_CD";
	public static final String SSE_SP_728_ERR_MSG = "SSE_SP_728_ERR_MSG";
	
	public static final String SSE_SP_730_ERR_CD = "SSE_SP_730_ERR_CD";
	public static final String SSE_SP_730_ERR_MSG = "SSE_SP_730_ERR_MSG";

	public static final String SSE_SP_731_ERR_CD = "SSE_SP_731_ERR_CD";
	public static final String SSE_SP_731_ERR_MSG = "SSE_SP_731_ERR_MSG";
	
	public static final String SSE_SP_733_ERR_CD = "SSE_SP_733_ERR_CD";
	public static final String SSE_SP_733_ERR_MSG = "SSE_SP_733_ERR_MSG";
	
	public static final String SSE_VNG_ENROLL_ERR_CD = "SSE_VNG_ENROLL_ERR_CD";
	public static final String SSE_VNG_ENROLL_ERR_MSG = "SSE_VNG_ENROLL_ERR_MSG";

	public static final String SSE_VNG_ENROLL_INVALID_REQUEST_ERR_CD = "SSE_VNG_ENROLL_INVALID_REQUEST_ERR_CD";
	public static final String SSE_VNG_ENROLL_INVALID_REQUEST_ERR_MSG = "SSE_VNG_ENROLL_INVALID_REQUEST_ERR_MSG";

	public static final String SSE_VNG_ENROLL_DUPLICATE_ACCOUNT_ERR_CD = "SSE_VNG_ENROLL_DUPLICATE_ACCOUNT_ERR_CD";
	public static final String SSE_VNG_ENROLL_DUPLICATE_ACCOUNT_ERR_MSG = "SSE_VNG_ENROLL_DUPLICATE_ACCOUNT_ERR_MSG";

	public static final String SSE_VNG_ENROLL_ACCOUNT_NOT_FOUND_ERR_CD = "SSE_VNG_ENROLL_ACCOUNT_NOT_FOUND_ERR_CD";
	public static final String SSE_VNG_ENROLL_ACCOUNT_NOT_FOUND_ERR_MSG = "SSE_VNG_ENROLL_ACCOUNT_NOT_FOUND_ERR_MSG";

	public static final String SSE_VNG_ENROLL_AUTHORIZATION_FAILURE_ERR_CD = "SSE_VNG_ENROLL_AUTHORIZATION_FAILURE_ERR_CD";
	public static final String SSE_VNG_ENROLL_AUTHORIZATION_FAILURE_ERR_MSG = "SSE_VNG_ENROLL_AUTHORIZATION_FAILURE_ERR_MSG";
	
	public static final String SSE_VNG_ACC_UPD_ERR_CD = "SSE_VNG_ACC_UPD_ERR_CD";
    public static final String SSE_VNG_ACC_UPD_ERR_MSG = "SSE_VNG_ACC_UPD_ERR_MSG";
    
    public static final String SSE_VNG_ACC_INACTIVE_ERR_CD = "SSE_VNG_ACC_INACTIVE_ERR_CD";
    public static final String SSE_VNG_ACC_INACTIVE_ERR_MSG = "SSE_VNG_ACC_INACTIVE_ERR_MSG";

	public static final String CANCEL_TKN_PROCESS_ERR_CD = "CANCEL_TKN_PROCESS_ERR_CD";
	public static final String CANCEL_TKN_PROCESS_ERR_MSG = "CANCEL_TKN_PROCESS_ERR_MSG";
	
	public static final String SSE_VNG_ACC_EMPTY_DOMAIN_RULES_ERR_CD = "SSE_VNG_ACC_EMPTY_DOMAIN_RULES_ERR_CD";
    public static final String SSE_VNG_ACC_EMPTY_DOMAIN_RULES_ERR_MSG = "SSE_VNG_ACC_EMPTY_DOMAIN_RULES_ERR_MSG";
    
	public static final String SSE_VNG_PTR_EMPTY_DOMAIN_RULES_ERR_CD = "SSE_VNG_PTR_EMPTY_DOMAIN_RULES_ERR_CD";
    public static final String SSE_VNG_PTR_EMPTY_DOMAIN_RULES_ERR_MSG = "SSE_VNG_PTR_EMPTY_DOMAIN_RULES_ERR_MSG";
    
    public static final String SSE_VNG_TOKEN_ENROLL_ERR_CD = "SSE_VNG_TOKEN_ENROLL_ERR_CD";
	public static final String SSE_VNG_TOKEN_ENROLL_ERR_MSG = "SSE_VNG_TOKEN_ENROLL_ERR_MSG";
    
    public static final String SSE_VNG_UPDATE_TOKEN_ENROLL_ERR_CD = "SSE_VNG_UPDATE_TOKEN_ENROLL_ERR_CD";
	public static final String SSE_VNG_UPDATE_TOKEN_ENROLL_ERR_MSG = "SSE_VNG_UPDATE_TOKEN_ENROLL_ERR_MSG";

	public static final String SSE_VNG_CANCEL_TOKEN_ENROLL_ERR_CD = "SSE_VNG_CANCEL_TOKEN_ENROLL_ERR_CD";
	public static final String SSE_VNG_CANCEL_TOKEN_ENROLL_ERR_MSG = "SSE_VNG_CANCEL_TOKEN_ENROLL_ERR_MSG";
	
	public static final String SSE_VNG_TOKEN_UPDATE_INVALID_REQUEST_ERR_CD = "SSE_VNG_TOKEN_UPDATE_INVALID_REQUEST_ERR_CD";
	public static final String SSE_VNG_TOKEN_UPDATE_INVALID_REQUEST_ERR_MSG = "SSE_VNG_TOKEN_UPDATE_INVALID_REQUEST_ERR_MSG";
	
	public static final String SSE_VNG_TOKEN_CANCEL_INVALID_REQUEST_ERR_CD = "SSE_VNG_TOKEN_CANCEL_INVALID_REQUEST_ERR_CD";
	public static final String SSE_VNG_TOKEN_CANCEL_INVALID_REQUEST_ERR_MSG = "SSE_VNG_TOKEN_CANCEL_INVALID_REQUEST_ERR_MSG";
	
	public static final String SSE_VNG_TOKEN_ENROLL_AUTHORIZATION_FAILURE_ERR_CD = "SSE_VNG_TOKEN_ENROLL_AUTHORIZATION_FAILURE_ERR_CD";
	public static final String SSE_VNG_TOKEN_ENROLL_AUTHORIZATION_FAILURE_ERR_MSG = "SSE_VNG_TOKEN_ENROLL_AUTHORIZATION_FAILURE_ERR_MSG";
	
	public static final String SSE_VNG_TOKEN_UPDATE_AUTHORIZATION_FAILURE_ERR_CD = "SSE_VNG_TOKEN_UPDATE_AUTHORIZATION_FAILURE_ERR_CD";
	public static final String SSE_VNG_TOKEN_UPDATE_AUTHORIZATION_FAILURE_ERR_MSG = "SSE_VNG_TOKEN_UPDATE_AUTHORIZATION_FAILURE_ERR_MSG";

	public static final String SSE_VNG_TOKEN_CANCEL_AUTHORIZATION_FAILURE_ERR_CD = "SSE_VNG_TOKEN_CANCEL_AUTHORIZATION_FAILURE_ERR_CD";
	public static final String SSE_VNG_TOKEN_CANCEL_AUTHORIZATION_FAILURE_ERR_MSG = "SSE_VNG_TOKEN_CANCEL_AUTHORIZATION_FAILURE_ERR_MSG";
	
	public static final String SSE_VNG_TOKEN_ENROLL_INVALID_REQUEST_ERR_CD = "SSE_VNG_TOKEN_ENROLL_INVALID_REQUEST_ERR_CD";
	public static final String SSE_VNG_TOKEN_ENROLL_INVALID_REQUEST_ERR_MSG = "SSE_VNG_TOKEN_ENROLL_INVALID_REQUEST_ERR_MSG";

	public static final String SSE_SP_GCM100_ERR_CD = "SSE_SP_GCM100_ERR_CD";
	public static final String SSE_SP_GCM100_ERR_MSG = "SSE_SP_GCM100_ERR_MSG";
	
	public static final String SSE_SP_GCM101_ERR_CD = "SSE_SP_GCM101_ERR_CD";
	public static final String SSE_SP_GCM101_ERR_MSG = "SSE_SP_GCM101_ERR_MSG";

	public static final String SSE_SP_722_ERR_CD = "SSE_SP_722_ERR_CD";
	public static final String SSE_SP_722_ERR_MSG = "SSE_SP_722_ERR_MSG";

	public static final String SSE_SP_725_ERR_CD = "SSE_SP_725_ERR_CD";
	public static final String SSE_SP_725_ERR_MSG = "SSE_SP_725_ERR_MSG";
	/** BIP changes **/
	public static final String SSE_BIP_CREATE_CHECK_PAYMENT_CD = "SSE_BIP_CREATE_CHECK_PAYMENT_CD";
	public static final String SSE_BIP_CREATE_CHECK_PAYMENT_MSG = "SSE_BIP_CREATE_CHECK_PAYMENT_MSG";

	public static final String SSE_BIP_CANCEL_PAYMENT_CD = "SSE_BIP_CANCEL_PAYMENT_CD";
	public static final String SSE_BIP_CANCEL_PAYMENT_MSG = "SSE_BIP_CANCEL_PAYMENT_MSG";
	
	public static final String SSE_SP_031_ERR_CD = "SSE_SP_031_ERR_CD";
	public static final String SSE_SP_031_ERR_MSG = "SSE_SP_031_ERR_MSG";

	public static final String SSE_SP_099_ERR_CD = "SSE_SP_099_ERR_CD";
	public static final String SSE_SP_099_ERR_MSG = "SSE_SP_099_ERR_MSG";

	public static final String BIP_REST_PAYEE_ENROLLMENT_CD = "BIP_REST_PAYEE_ENROLLMENT_CD";
	public static final String BIP_REST_PAYEE_ENROLLMENT_MSG = "BIP_REST_PAYEE_ENROLLMENT_MSG";

	public static final String SSE_SP_030_ERR_CD = "SSE_SP_030_ERR_CD";
	public static final String SSE_SP_030_ERR_MSG = "SSE_SP_030_ERR_MSG";

	public static final String SSE_SP_082_ERR_CD = "SSE_SP_082_ERR_CD";
	public static final String SSE_SP_082_ERR_MSG = "SSE_SP_082_ERR_MSG";

	public static final String BIP_REST_PAYEE_STATUS_INQUIRY_CD = "BIP_REST_PAYEE_STATUS_INQUIRY_CD";
	public static final String BIP_REST_PAYEE_STATUS_INQUIRY_MSG = "BIP_REST_PAYEE_STATUS_INQUIRY_MSG";

	public static final String SSE_SP_521_ERR_CD = "SSE_SP_521_ERR_CD";
	public static final String SSE_SP_521_ERR_MSG = "SSE_SP_521_ERR_MSG";

	public static final String BIP_REST_PAYEE_ACTIVATE_CD = "BIP_REST_PAYEE_ACTIVATE_CD";
	public static final String BIP_REST_PAYEE_ACTIVATE_MSG = "BIP_REST_PAYEE_ACTIVATE_MSG";

	public static final String SSE_BIP_GET_CHECK_PAYMENT_STA_CD = "SSE_BIP_GET_CHECK_PAYMENT_STA_CD";
	public static final String SSE_BIP_GET_CHECK_PAYMENT_STA_MSG = "SSE_BIP_GET_CHECK_PAYMENT_STA_MSG";
	
	public static final String BIP_REST_PAYEE_ACTIVATE_INACTIVATE_CD = "BIP_REST_PAYEE_ACTIVATE_INACTIVATE_CD";
	public static final String BIP_REST_PAYEE_ACTIVATE_INACTIVATE_MSG = "BIP_REST_PAYEE_ACTIVATE_INACTIVATE_MSG";
	
	public static final String SSE_SP_E3GMR023_ERR_CD = "SSE_SP_E3GMR023_ERR_CD";
	public static final String SSE_SP_E3GMR023_ERR_MSG = "SSE_SP_E3GMR023_ERR_MSG";

	public static final String SSE_SOLACE_MSG_PYMT_RECORD_NOT_FOUND = "SSE_SOLACE_MSG_PYMT_RECORD_NOT_FOUND";
	public static final String SSE_SOLACE_MSG_PYMT_RECORD_NOT_FOUND_MSG = "SSE_SOLACE_MSG_PYMT_RECORD_NOT_FOUND_MSG";
	
	public static final String SSE_SP_E3GACP12_ERR_CD = "SSE_SP_E3GACP12_ERR_CD";
	public static final String SSE_SP_E3GACP12_ERR_MSG = "SSE_SP_E3GACP12_ERR_MSG";
	
	public static final String FIFIA_SERVICE_ERR_CD = "FIFIA_SERVICE_ERR_CD";
	public static final String FIFIA_SERVICE_ERR_MSG = "FIFIA_SERVICE_ERR_MSG";
	
	public static final String SEARCH_BANK_ERR_CD = "SEARCH_BANK_ERR_CD";
	public static final String SEARCH_BANK_ERR_MSG = "SEARCH_BANK_ERR_MSG";

	public static final String SSE_SP_164_ERR_CD = "SSE_SP_164_ERR_CD";
	public static final String SSE_SP_164_ERR_MSG = "SSE_SP_164_ERR_MSG";

	public static final String SSE_SP_E3GACA10_ERR_CD = "SSE_SP_E3GACA10_ERR_CD";
	public static final String SSE_SP_E3GACA10_ERR_MSG = "SSE_SP_E3GACA10_ERR_MSG";

	public static final String CREATE_ACH_PAYMENT_REST_SERVICE_CD = "CREATE_ACH_PAYMENT_REST_SERVICE_CD";
	public static final String CREATE_ACH_PAYMENT_REST_SERVICE_MSG = "CREATE_ACH_PAYMENT_REST_SERVICE_MSG";

	
	/**
	 * This method will return a String which is configured in Tivoli
	 *
	 * @param errorCode
	 * @param errorMessage
	 * @param msgId
	 * @return
	 */
	public String logStatement(String errorCode, String errorMessage,
			String msgId) {
		StringBuffer tivoliMessage = new StringBuffer();
		try {
			if (applicationErrorCodesProperties != null) {
				tivoliMessage.append("");
				tivoliMessage.append("TransactionId:" + msgId);
				tivoliMessage.append(" ");

				if (applicationErrorCodesProperties.get(errorCode) != null) {
					tivoliMessage.append(applicationErrorCodesProperties
							.get(errorCode).toString().trim());
					tivoliMessage.append(" ");
				}
				if (applicationErrorCodesProperties.get(errorMessage) != null) {
					tivoliMessage.append(applicationErrorCodesProperties
							.get(errorMessage).toString().trim());
				}
			}
		} catch (Exception e) {
			LOG.error(
					"TransactionId"
							+ msgId
							+ " : TivoliMonitoring : Exception while logging the Tivoli alerts",
					e);
		}
		return tivoliMessage.toString();
	}

	public String logStatement(String count, String errorCode,
			String errorMessage, String msgId) {
		StringBuffer tivoliMessage = new StringBuffer();
		try {
			if (applicationErrorCodesProperties != null) {
				tivoliMessage.append(" ");
				tivoliMessage.append("TransactionId:" + msgId);
				tivoliMessage.append(" ");
				tivoliMessage.append("Count:" + count);
				tivoliMessage.append("");
				tivoliMessage.append(applicationErrorCodesProperties
						.get(errorCode).toString().trim());
				tivoliMessage.append(" ");
				tivoliMessage.append(applicationErrorCodesProperties
						.get(errorMessage).toString().trim());

			}
		} catch (Exception e) {
			LOG.error("TransactionId"+ msgId+
					" : TivoliMonitoring : Exception while logging the Tivoli alerts",e);
		}
		return tivoliMessage.toString();
	}

	public String logStatement(long resultId, String errorCode,
			String errorMessage, String msgId) {
		StringBuffer tivoliMessage = new StringBuffer();
		try {
			if (applicationErrorCodesProperties != null) {
				tivoliMessage.append(" ");
				tivoliMessage.append("TransactionId:" + msgId);
				tivoliMessage.append(" ");
				tivoliMessage.append("ResultId:" + resultId);
				tivoliMessage.append("");
				tivoliMessage.append(applicationErrorCodesProperties
						.get(errorCode).toString().trim());
				tivoliMessage.append(" ");
				tivoliMessage.append(applicationErrorCodesProperties
						.get(errorMessage).toString().trim());

			}
		} catch (Exception e) {
			LOG.error("TransactionId"+ msgId
							+ " : TivoliMonitoring : Exception while logging the Tivoli alerts",e);
		}
		return tivoliMessage.toString();
	}

}
