package uk.gov.hmcts.ethos.replacement.docmosis.model.helper;

import java.time.format.DateTimeFormatter;

public class Constants {

    public static final DateTimeFormatter OLD_DATE_TIME_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    public static final DateTimeFormatter NEW_DATE_PATTERN = DateTimeFormatter.ofPattern("E, d MMM yyyy");
    public static final DateTimeFormatter NEW_DATE_TIME_PATTERN = DateTimeFormatter.ofPattern("E, d MMM yyyy HH:mm:ss");
    public static final String NEW_LINE = "\",\n";
    public static final String OUTPUT_FILE_NAME = "document.docx";

    public static final String PRE_DEFAULT_XLSX_FILE_PATH = "preDefaultValues.xlsx";
    public static final String POST_DEFAULT_XLSX_FILE_PATH = "postDefaultValues.xlsx";
    public static final String MANCHESTER_CASE_TYPE_ID = "EmpTrib_MVP_1.0_Manc";
    public static final String MANCHESTER_USERS_CASE_TYPE_ID = "Manchester_Users_Demo";
    public static final String GLASGOW_CASE_TYPE_ID = "EmpTrib_MVP_1.0_Glas";
    public static final String ETHOS_BULK_CASE_TYPE_ID = "ETHOS_BULK_ACTION_v3";

    public static final String PENDING_STATE = "Pending";

    public static final String SUBMITTED_STATE = "1_Submitted";

    public static final String CREATION_EVENT_TRIGGER_ID = "initiateCase";

    public static final String UPDATE_EVENT_TRIGGER_ID = "amendCaseDetails";

    public static final String UPDATE_EVENT_TRIGGER_ID_BULK = "amendCaseDetailsBulk";

    public static final String UPDATE_BULK_EVENT_TRIGGER_ID = "updateBulkAction";

}
