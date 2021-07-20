package uk.gov.hmcts.ethos.replacement.docmosis.helpers;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.ecm.common.helpers.UtilHelper;
import uk.gov.hmcts.ecm.common.model.ccd.CaseData;
import uk.gov.hmcts.ecm.common.model.ccd.SubmitEvent;
import uk.gov.hmcts.ecm.common.model.ccd.items.BFActionTypeItem;
import uk.gov.hmcts.ecm.common.model.listing.ListingData;
import uk.gov.hmcts.ecm.common.model.listing.ListingDetails;
import uk.gov.hmcts.ecm.common.model.listing.items.AdhocReportTypeItem;
import uk.gov.hmcts.ecm.common.model.listing.items.BFDateTypeItem;
import uk.gov.hmcts.ecm.common.model.listing.types.AdhocReportType;
import uk.gov.hmcts.ecm.common.model.listing.types.BFDateType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.*;

@Slf4j
public class ReportHelper {

    public static final String CASES_SEARCHED = "Cases searched: ";

    private ReportHelper() {
    }

    public static boolean validateMatchingDate(ListingData listingData, String matchingDate) {
        boolean dateRange = listingData.getHearingDateType().equals(RANGE_HEARING_DATE_TYPE);
        if (!dateRange) {
            String dateToSearch = listingData.getListingDate();
            return ListingHelper.getMatchingDateBetween(dateToSearch, "", matchingDate, false);
        } else {
            String dateToSearchFrom = listingData.getListingDateFrom();
            String dateToSearchTo = listingData.getListingDateTo();
            return ListingHelper.getMatchingDateBetween(dateToSearchFrom, dateToSearchTo, matchingDate, true);
        }
    }

    public static ListingData processBroughtForwardDatesRequest(ListingDetails listingDetails,
                                                                List<SubmitEvent> submitEvents) {

        if (submitEvents != null && !submitEvents.isEmpty()) {
            log.info(CASES_SEARCHED + submitEvents.size());
            List<BFDateTypeItem> bfDateTypeItems = new ArrayList<>();
            for (SubmitEvent submitEvent : submitEvents) {
               addBfDateTypeItems(submitEvent, listingDetails, bfDateTypeItems);
            }
            listingDetails.getCaseData().setBfDateCollection(bfDateTypeItems);
        }

        listingDetails.getCaseData().clearReportFields();
        return listingDetails.getCaseData();
    }
    private static void addBfDateTypeItems(SubmitEvent submitEvent, ListingDetails listingDetails,List<BFDateTypeItem> bfDateTypeItems ){
        if (submitEvent.getCaseData().getBfActions() != null
                && !submitEvent.getCaseData().getBfActions().isEmpty()) {
            for (BFActionTypeItem bfActionTypeItem : submitEvent.getCaseData().getBfActions()) {
                var bfDateTypeItem = getBFDateTypeItem(bfActionTypeItem,
                        listingDetails.getCaseData(), submitEvent.getCaseData());
                if (bfDateTypeItem.getValue() != null) {
                    bfDateTypeItems.add(bfDateTypeItem);
                }
            }
        }
    }

    public static ListingData processClaimsAcceptedRequest(ListingDetails listingDetails,
                                                           List<SubmitEvent> submitEvents) {

        if (submitEvents != null && !submitEvents.isEmpty()) {
            log.info(CASES_SEARCHED + submitEvents.size());
            var totalCases = 0;
            var totalSingles = 0;
            var totalMultiples = 0;
            var localReportsDetailHdr = new AdhocReportType();
            List<AdhocReportTypeItem> localReportsDetailList = new ArrayList<>();
            for (SubmitEvent submitEvent : submitEvents) {
                AdhocReportTypeItem localReportsDetailItem =
                        getClaimsAcceptedDetailItem(listingDetails, submitEvent.getCaseData());
                if (localReportsDetailItem.getValue() != null) {
                    totalCases++;
                    if (localReportsDetailItem.getValue().getCaseType().equals(SINGLE_CASE_TYPE)) {
                        totalSingles++;
                    } else {
                        totalMultiples++;
                    }
                    localReportsDetailList.add(localReportsDetailItem);
                }
            }
            localReportsDetailHdr.setTotal(Integer.toString(totalCases));
            localReportsDetailHdr.setSinglesTotal(Integer.toString(totalSingles));
            localReportsDetailHdr.setMultiplesTotal(Integer.toString(totalMultiples));
            localReportsDetailHdr.setReportOffice(UtilHelper.getListingCaseTypeId(listingDetails.getCaseTypeId()));
            listingDetails.getCaseData().setLocalReportsDetailHdr(localReportsDetailHdr);
            listingDetails.getCaseData().setLocalReportsDetail(localReportsDetailList);
        }
        listingDetails.getCaseData().clearReportFields();
        return listingDetails.getCaseData();
    }

    public static ListingData processLiveCaseloadRequest(ListingDetails listingDetails,
                                                         List<SubmitEvent> submitEvents) {

        if (submitEvents != null && !submitEvents.isEmpty()) {
            log.info(CASES_SEARCHED + submitEvents.size());
            List<AdhocReportTypeItem> localReportsDetailList = new ArrayList<>();
            for (SubmitEvent submitEvent : submitEvents) {
                AdhocReportTypeItem localReportsDetailItem =
                        getLiveCaseloadDetailItem(listingDetails, submitEvent.getCaseData());
                if (localReportsDetailItem.getValue() != null) {
                    localReportsDetailList.add(localReportsDetailItem);
                }
            }

            var localReportsDetailHdr = new AdhocReportType();
            localReportsDetailHdr.setReportOffice(UtilHelper.getListingCaseTypeId(listingDetails.getCaseTypeId()));
            listingDetails.getCaseData().setLocalReportsDetailHdr(localReportsDetailHdr);
            listingDetails.getCaseData().setLocalReportsDetail(localReportsDetailList);

            var localReportsSummaryHdr = new AdhocReportType();
            var singlesTotal = localReportsDetailList.stream().distinct()
                    .filter(reportItem->reportItem.getValue().getCaseType().equals(SINGLE_CASE_TYPE)).count();
            localReportsSummaryHdr.setSinglesTotal(String.valueOf(singlesTotal));

            var multiplesTotal = localReportsDetailList.stream().distinct()
                    .filter(reportItem->reportItem.getValue().getCaseType().equals(MULTIPLE_CASE_TYPE)).count();
            localReportsSummaryHdr.setMultiplesTotal(String.valueOf(multiplesTotal));

            var total = singlesTotal + multiplesTotal;
            localReportsSummaryHdr.setTotal(String.valueOf(total));
            listingDetails.getCaseData().setLocalReportsSummaryHdr(localReportsSummaryHdr);

        }

        listingDetails.getCaseData().clearReportFields();
        return listingDetails.getCaseData();
    }

    private static BFDateTypeItem getBFDateTypeItem(BFActionTypeItem bfActionTypeItem,
                                                    ListingData listingData, CaseData caseData) {
        var bfDateTypeItem = new BFDateTypeItem();
        var bfActionType = bfActionTypeItem.getValue();
        if (!isNullOrEmpty(bfActionType.getBfDate()) && isNullOrEmpty(bfActionType.getCleared())) {
            boolean matchingDateIsValid = validateMatchingDate(listingData, bfActionType.getBfDate());
            boolean clerkResponsibleIsValid = validateClerkResponsible(listingData, caseData);
            if (matchingDateIsValid && clerkResponsibleIsValid) {
                var bfDateType = new BFDateType();
                bfDateType.setCaseReference(caseData.getEthosCaseReference());
                if (!Strings.isNullOrEmpty(bfActionType.getAllActions())){
                    bfDateType.setBroughtForwardAction(bfActionType.getAllActions());
                }
                else if (!Strings.isNullOrEmpty(bfActionType.getCwActions())){
                    bfDateType.setBroughtForwardAction(bfActionType.getCwActions());
                }
                bfDateType.setBroughtForwardDate(bfActionType.getBfDate());
                bfDateType.setBroughtForwardDateCleared(bfActionType.getCleared());
                bfDateType.setBroughtForwardDateReason(bfActionType.getNotes());
                bfDateTypeItem.setId(String.valueOf(bfActionTypeItem.getId()));
                bfDateTypeItem.setValue(bfDateType);
            }
        }
        return bfDateTypeItem;
    }

    private static AdhocReportTypeItem getClaimsAcceptedDetailItem(ListingDetails listingDetails, CaseData caseData) {
        var adhocReportTypeItem = new AdhocReportTypeItem();
        var listingData = listingDetails.getCaseData();
        if (caseData.getPreAcceptCase() != null && caseData.getPreAcceptCase().getDateAccepted() != null) {
            boolean matchingDateIsValid =
                    validateMatchingDate(listingData, caseData.getPreAcceptCase().getDateAccepted());
            if (matchingDateIsValid) {
                var adhocReportType = new AdhocReportType();
                adhocReportType.setCaseType(caseData.getCaseType());
                getCommonReportDetailFields(listingDetails, caseData, adhocReportType);
                adhocReportTypeItem.setValue(adhocReportType);
            }
        }
        return adhocReportTypeItem;
    }

    private static AdhocReportTypeItem getLiveCaseloadDetailItem(ListingDetails listingDetails, CaseData caseData) {
        var adhocReportTypeItem = new AdhocReportTypeItem();
        var listingData = listingDetails.getCaseData();
        if (caseData.getPreAcceptCase() != null && caseData.getPreAcceptCase().getDateAccepted() != null) {
            boolean matchingDateIsValid =
                    validateMatchingDate(listingData, caseData.getPreAcceptCase().getDateAccepted());
            boolean liveCaseloadIsValid = liveCaseloadIsValid(caseData);
            if (matchingDateIsValid && liveCaseloadIsValid) {
                var adhocReportType = new AdhocReportType();
                adhocReportType.setReportOffice(getTribunalOffice(listingDetails, caseData));
                // TODO : hearingCollection.Hearing_stage implementation
                getCommonReportDetailFields(listingDetails, caseData, adhocReportType);
                adhocReportTypeItem.setValue(adhocReportType);
            }
        }
        return adhocReportTypeItem;
    }

    private static boolean validateClerkResponsible(ListingData listingData, CaseData caseData) {
        if (listingData.getClerkResponsible() != null) {
            if (caseData.getClerkResponsible() != null) {
                return listingData.getClerkResponsible().equals(caseData.getClerkResponsible());
            }
            return false;
        }
        return true;
    }

    private static void getCommonReportDetailFields(ListingDetails listingDetails, CaseData caseData,
                                                    AdhocReportType adhocReportType) {
        adhocReportType.setCaseReference(caseData.getEthosCaseReference());
        adhocReportType.setDateOfAcceptance(caseData.getPreAcceptCase().getDateAccepted());
        adhocReportType.setMultipleRef(caseData.getMultipleReference());
        adhocReportType.setLeadCase(caseData.getLeadClaimant());
        adhocReportType.setPosition(caseData.getCurrentPosition());
        adhocReportType.setDateToPosition(caseData.getDateToPosition());
        adhocReportType.setFileLocation(getFileLocation(listingDetails, caseData));
        adhocReportType.setClerk(caseData.getClerkResponsible());
    }

    private static String getFileLocation(ListingDetails listingDetails, CaseData caseData) {
        String caseTypeId = UtilHelper.getListingCaseTypeId(listingDetails.getCaseTypeId());
        if (!caseTypeId.equals(SCOTLAND_CASE_TYPE_ID)) {
            return caseData.getFileLocation();
        } else {
            switch (caseData.getManagingOffice()) {
                case DUNDEE_OFFICE:
                    return caseData.getFileLocationDundee();
                case GLASGOW_OFFICE:
                    return caseData.getFileLocationGlasgow();
                case ABERDEEN_OFFICE:
                    return caseData.getFileLocationAberdeen();
                case EDINBURGH_OFFICE:
                    return caseData.getFileLocationEdinburgh();
                default:
                    return "";
            }
        }
    }

    private static String getTribunalOffice(ListingDetails listingDetails, CaseData caseData) {
        String caseTypeId = UtilHelper.getListingCaseTypeId(listingDetails.getCaseTypeId());
        return caseTypeId.equals(SCOTLAND_CASE_TYPE_ID) ? caseData.getManagingOffice() : caseTypeId;
    }

    private static boolean liveCaseloadIsValid(CaseData caseData) {
        if (caseData.getPositionType() != null) {
            List<String> invalidPositionTypes = Arrays.asList(POSITION_TYPE_REJECTED,
                    POSITION_TYPE_CASE_CLOSED,
                    POSITION_TYPE_CASE_INPUT_IN_ERROR,
                    POSITION_TYPE_CASE_TRANSFERRED_SAME_COUNTRY,
                    POSITION_TYPE_CASE_TRANSFERRED_OTHER_COUNTRY);
            return invalidPositionTypes.stream().noneMatch(str -> str.equals(caseData.getPositionType()));
        }
        return true;
    }
}
