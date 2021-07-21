package uk.gov.hmcts.ethos.replacement.docmosis.helpers;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.ecm.common.helpers.UtilHelper;
import uk.gov.hmcts.ecm.common.idam.models.UserDetails;
import uk.gov.hmcts.ecm.common.model.listing.ListingData;
import uk.gov.hmcts.ecm.common.model.listing.items.AdhocReportTypeItem;
import uk.gov.hmcts.ecm.common.model.listing.types.AdhocReportType;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ecm.common.model.helper.Constants.CASES_COMPLETED_REPORT;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.CLAIMS_ACCEPTED_REPORT;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.FILE_EXTENSION;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.LIVE_CASELOAD_REPORT;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.NEW_LINE;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.OUTPUT_FILE_NAME;
import static uk.gov.hmcts.ethos.replacement.docmosis.helpers.Helper.nullCheck;

@Slf4j
public class ReportDocHelper {
    private static final String REPORT_LIST = "\"Report_List\":[\n";

    private ReportDocHelper() {
    }

    public static StringBuilder buildReportDocumentContent(ListingData listingData, String accessKey,
                                                           String templateName, UserDetails userDetails) {
        var sb = new StringBuilder();

        sb.append("{\n");
        sb.append("\"accessKey\":\"").append(accessKey).append(NEW_LINE);
        sb.append("\"templateName\":\"").append(templateName).append(FILE_EXTENSION).append(NEW_LINE);
        sb.append("\"outputName\":\"").append(OUTPUT_FILE_NAME).append(NEW_LINE);

        log.info("Building report document data");

        sb.append("\"data\":{\n");

        sb.append(ListingHelper.getListingDate(listingData));

        if (listingData.getLocalReportsDetailHdr() != null) {
            sb.append("\"Report_Office\":\"").append(
                    nullCheck(listingData.getLocalReportsDetailHdr().getReportOffice())).append(NEW_LINE);
        }

        switch (listingData.getReportType()) {
            case CLAIMS_ACCEPTED_REPORT:

                log.info("Claims accepted report");

                sb.append(getCasesAcceptedReport(listingData));

                break;
            case LIVE_CASELOAD_REPORT:

                log.info("Live case load report");

                sb.append(getLiveCaseLoadReport(listingData));

                break;
            case CASES_COMPLETED_REPORT:

                log.info("Cases completed report");

                sb.append(getCasesCompletedReport(listingData));

                break;
            default:
                throw new IllegalStateException("Report type - Unexpected value: " + listingData.getReportType());
        }

        String userName = nullCheck(userDetails.getFirstName() + " " + userDetails.getLastName());
        sb.append("\"Report_Clerk\":\"").append(nullCheck(userName)).append(NEW_LINE);

        sb.append("\"Today_date\":\"").append(UtilHelper.formatCurrentDate(LocalDate.now())).append("\"\n");

        sb.append("}\n");
        sb.append("}\n");
        return sb;
    }

    private static StringBuilder getCasesAcceptedReport(ListingData listingData) {
        var sb = new StringBuilder();
        AdhocReportType localReportDetailHdr = listingData.getLocalReportsDetailHdr();
        if (localReportDetailHdr != null) {
            sb.append("\"Multiple_Claims_Accepted\":\"").append(
                    nullCheck(localReportDetailHdr.getMultiplesTotal())).append(NEW_LINE);
            sb.append("\"Singles_Claims_Accepted\":\"").append(
                    nullCheck(localReportDetailHdr.getSinglesTotal())).append(NEW_LINE);
            sb.append("\"Total_Claims_Accepted\":\"").append(
                    nullCheck(localReportDetailHdr.getTotal())).append(NEW_LINE);
        }

        if (listingData.getLocalReportsDetail() != null && !listingData.getLocalReportsDetail().isEmpty()) {
            sb.append(getClaimsAcceptedByCaseType(listingData));
        }
        return sb;
    }

    private static StringBuilder getClaimsAcceptedByCaseType(ListingData listingData) {
        var sb = new StringBuilder();
        Map<Boolean, List<AdhocReportTypeItem>> unsortedMap = listingData.getLocalReportsDetail().stream()
                .collect(Collectors.partitioningBy(localReportDetail ->
                        localReportDetail.getValue().getMultipleRef() != null));
        sb.append("\"Local_Report_By_Type\":[\n");
        Iterator<Map.Entry<Boolean, List<AdhocReportTypeItem>>> entries =
                new TreeMap<>(unsortedMap).entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<Boolean, List<AdhocReportTypeItem>> localReportEntry = entries.next();
            String singleOrMultiple = localReportEntry.getKey() ? "Multiples" : "Singles";
            sb.append("{\"Case_Type\":\"").append(singleOrMultiple).append(NEW_LINE);
            sb.append("\"Claims_Number\":\"").append(localReportEntry.getValue().size()).append(NEW_LINE);
            sb.append(REPORT_LIST);
            for (var i = 0; i < localReportEntry.getValue().size(); i++) {
                sb.append(getAdhocReportCommonTypeRow(localReportEntry.getValue().get(i).getValue()));
                if (i != localReportEntry.getValue().size() - 1) {
                    sb.append(",\n");
                }
            }
            sb.append("]\n");
            if (entries.hasNext()) {
                sb.append("},\n");
            } else {
                sb.append("}],\n");
            }
        }
        return sb;
    }

    private static StringBuilder getAdhocReportCommonTypeRow(AdhocReportType adhocReportType) {
        var sb = new StringBuilder();
        sb.append("{\"Case_Reference\":\"").append(
                nullCheck(adhocReportType.getCaseReference())).append(NEW_LINE);
        sb.append("\"Date_Of_Acceptance\":\"").append(
                nullCheck(adhocReportType.getDateOfAcceptance())).append(NEW_LINE);
        sb.append("\"Multiple_Ref\":\"").append(
                nullCheck(adhocReportType.getMultipleRef())).append(NEW_LINE);
        sb.append("\"Lead_Case\":\"").append(
                nullCheck(adhocReportType.getLeadCase())).append(NEW_LINE);
        sb.append("\"Position\":\"").append(
                nullCheck(adhocReportType.getPosition())).append(NEW_LINE);
        sb.append("\"Date_To_Position\":\"").append(
                nullCheck(adhocReportType.getDateToPosition())).append(NEW_LINE);
        sb.append("\"File_Location\":\"").append(
                nullCheck(adhocReportType.getFileLocation())).append(NEW_LINE);
        sb.append("\"Clerk\":\"").append(
                nullCheck(adhocReportType.getClerk())).append("\"}");
        return sb;
    }

    private static StringBuilder getCasesCompletedReport(ListingData listingData) {
        var sb = new StringBuilder();
        AdhocReportType localReportDetailHdr = listingData.getLocalReportsDetailHdr();
        if (localReportDetailHdr != null) {
            sb.append("\"Cases_Completed_Hearing\":\"").append(
                    nullCheck(localReportDetailHdr.getCasesCompletedHearingTotal())).append(NEW_LINE);
            sb.append("\"Session_Days_Taken\":\"").append(
                    nullCheck(localReportDetailHdr.getSessionDaysTotal())).append(NEW_LINE);
            sb.append("\"Completed_Per_Session_Day\":\"").append(
                    nullCheck(localReportDetailHdr.getCompletedPerSessionTotal())).append(NEW_LINE);

            sb.append("\"No_Conciliation_1\":\"").append(
                    nullCheck(localReportDetailHdr.getConNoneCasesCompletedHearing())).append(NEW_LINE);
            sb.append("\"No_Conciliation_2\":\"").append(
                    nullCheck(localReportDetailHdr.getConNoneSessionDays())).append(NEW_LINE);
            sb.append("\"No_Conciliation_3\":\"").append(
                    nullCheck(localReportDetailHdr.getConNoneCompletedPerSession())).append(NEW_LINE);

            sb.append("\"Fast_Track_1\":\"").append(
                    nullCheck(localReportDetailHdr.getConFastCasesCompletedHearing())).append(NEW_LINE);
            sb.append("\"Fast_Track_2\":\"").append(
                    nullCheck(localReportDetailHdr.getConFastSessionDays())).append(NEW_LINE);
            sb.append("\"Fast_Track_3\":\"").append(
                    nullCheck(localReportDetailHdr.getConFastCompletedPerSession())).append(NEW_LINE);

            sb.append("\"Standard_Track_1\":\"").append(
                    nullCheck(localReportDetailHdr.getConStdCasesCompletedHearing())).append(NEW_LINE);
            sb.append("\"Standard_Track_2\":\"").append(
                    nullCheck(localReportDetailHdr.getConStdSessionDays())).append(NEW_LINE);
            sb.append("\"Standard_Track_3\":\"").append(
                    nullCheck(localReportDetailHdr.getConStdCompletedPerSession())).append(NEW_LINE);

            sb.append("\"Open_Track_1\":\"").append(
                    nullCheck(localReportDetailHdr.getConOpenCasesCompletedHearing())).append(NEW_LINE);
            sb.append("\"Open_Track_2\":\"").append(
                    nullCheck(localReportDetailHdr.getConOpenSessionDays())).append(NEW_LINE);
            sb.append("\"Open_Track_3\":\"").append(
                    nullCheck(localReportDetailHdr.getConOpenCompletedPerSession())).append(NEW_LINE);
        }

        if (listingData.getLocalReportsDetail() != null && !listingData.getLocalReportsDetail().isEmpty()) {
            List<AdhocReportTypeItem> adhocReportTypeItems = listingData.getLocalReportsDetail();
            sb.append(REPORT_LIST);
            for (var i = 0; i < adhocReportTypeItems.size(); i++) {
                sb.append(getAdhocReportCompletedTypeRow(adhocReportTypeItems.get(i).getValue()));
                if (i != adhocReportTypeItems.size() - 1) {
                    sb.append(",\n");
                }
            }
            sb.append("],\n");
        }
        return sb;
    }

    private static StringBuilder getAdhocReportCompletedTypeRow(AdhocReportType adhocReportType) {
        var sb = new StringBuilder();
        sb.append("{\"Case_Reference\":\"").append(
                nullCheck(adhocReportType.getCaseReference())).append(NEW_LINE);
        sb.append("\"Position\":\"").append(
                nullCheck(adhocReportType.getPosition())).append(NEW_LINE);
        sb.append("\"Conciliation_Track\":\"").append(
                nullCheck(adhocReportType.getConciliationTrack())).append(NEW_LINE);
        sb.append("\"Session_Days\":\"").append(
                nullCheck(adhocReportType.getSessionDays())).append(NEW_LINE);
        sb.append("\"Hearing_Number\":\"").append(
                nullCheck(adhocReportType.getHearingNumber())).append(NEW_LINE);
        sb.append("\"Hearing_Date\":\"").append(
                UtilHelper.formatLocalDate(adhocReportType.getHearingDate())).append(NEW_LINE);
        sb.append("\"Hearing_Type\":\"").append(
                nullCheck(adhocReportType.getHearingType())).append(NEW_LINE);
        sb.append("\"Hearing_Judge\":\"").append(
                nullCheck(adhocReportType.getHearingJudge())).append(NEW_LINE);
        sb.append("\"Hearing_Clerk\":\"").append(
                nullCheck(adhocReportType.getHearingClerk())).append("\"}");
        return sb;
    }

    private static StringBuilder getLiveCaseLoadReport(ListingData listingData) {
        var sb = new StringBuilder();

        if (listingData.getLocalReportsDetail() != null && !listingData.getLocalReportsDetail().isEmpty()) {
            List<AdhocReportTypeItem> adhocReportTypeItems = listingData.getLocalReportsDetail();
            sb.append(REPORT_LIST);
            for (var i = 0; i < adhocReportTypeItems.size(); i++) {
                sb.append(getAdhocReportCommonTypeRow(adhocReportTypeItems.get(i).getValue()));
                if (i != adhocReportTypeItems.size() - 1) {
                    sb.append(",\n");
                }
            }
            sb.append("],\n");
        }
        return sb;
    }

}
