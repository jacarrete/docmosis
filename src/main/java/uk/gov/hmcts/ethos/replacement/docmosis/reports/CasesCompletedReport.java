package uk.gov.hmcts.ethos.replacement.docmosis.reports;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ecm.common.helpers.UtilHelper;
import uk.gov.hmcts.ecm.common.model.ccd.CaseData;
import uk.gov.hmcts.ecm.common.model.ccd.SubmitEvent;
import uk.gov.hmcts.ecm.common.model.ccd.items.DateListedTypeItem;
import uk.gov.hmcts.ecm.common.model.ccd.items.HearingTypeItem;
import uk.gov.hmcts.ecm.common.model.ccd.types.DateListedType;
import uk.gov.hmcts.ecm.common.model.ccd.types.HearingType;
import uk.gov.hmcts.ecm.common.model.listing.ListingData;
import uk.gov.hmcts.ecm.common.model.listing.ListingDetails;
import uk.gov.hmcts.ecm.common.model.listing.items.AdhocReportTypeItem;
import uk.gov.hmcts.ecm.common.model.listing.types.AdhocReportType;
import uk.gov.hmcts.ethos.replacement.docmosis.helpers.ReportHelper;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ecm.common.model.helper.Constants.CLOSED_STATE;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.CONCILIATION_TRACK_FAST_TRACK;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.CONCILIATION_TRACK_NO_CONCILIATION;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.CONCILIATION_TRACK_NUMBER_FOUR;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.CONCILIATION_TRACK_NUMBER_ONE;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.CONCILIATION_TRACK_NUMBER_THREE;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.CONCILIATION_TRACK_NUMBER_TWO;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.CONCILIATION_TRACK_OPEN_TRACK;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.CONCILIATION_TRACK_STANDARD_TRACK;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.HEARING_STATUS_HEARD;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.HEARING_TYPE_JUDICIAL_HEARING;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.HEARING_TYPE_PERLIMINARY_HEARING;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.HEARING_TYPE_PERLIMINARY_HEARING_CM;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.HEARING_TYPE_PERLIMINARY_HEARING_CM_TCC;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.JURISDICTION_OUTCOME_DISMISSED_AT_HEARING;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.JURISDICTION_OUTCOME_SUCCESSFUL_AT_HEARING;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.JURISDICTION_OUTCOME_UNSUCCESSFUL_AT_HEARING;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.OLD_DATE_TIME_PATTERN2;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.POSITION_TYPE_CASE_INPUT_IN_ERROR;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.POSITION_TYPE_CASE_TRANSFERRED_OTHER_COUNTRY;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.POSITION_TYPE_CASE_TRANSFERRED_SAME_COUNTRY;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.YES;

@Service
@Slf4j
public class CasesCompletedReport {
    static final String ZERO = "0";
    static final String ZERO_DECIMAL = "0.00";
    static final String COMPLETED_PER_SESSION_FORMAT = "%.2f";

    static final List<String> INVALID_POSITION_TYPES = Arrays.asList(
            POSITION_TYPE_CASE_INPUT_IN_ERROR,
            POSITION_TYPE_CASE_TRANSFERRED_SAME_COUNTRY,
            POSITION_TYPE_CASE_TRANSFERRED_OTHER_COUNTRY);

    static final List<String> VALID_JURISDICTION_OUTCOMES = Arrays.asList(
            JURISDICTION_OUTCOME_SUCCESSFUL_AT_HEARING,
            JURISDICTION_OUTCOME_UNSUCCESSFUL_AT_HEARING,
            JURISDICTION_OUTCOME_DISMISSED_AT_HEARING);

    static final List<String> VALID_HEARING_TYPES = Arrays.asList(
            HEARING_TYPE_JUDICIAL_HEARING,
            HEARING_TYPE_PERLIMINARY_HEARING,
            HEARING_TYPE_PERLIMINARY_HEARING_CM,
            HEARING_TYPE_PERLIMINARY_HEARING_CM_TCC);

    public ListingData generateReportData(ListingDetails listingDetails, List<SubmitEvent> submitEvents) {
        initReport(listingDetails);

        if (CollectionUtils.isNotEmpty(submitEvents)) {
            executeReport(listingDetails, submitEvents);
        }

        listingDetails.getCaseData().clearReportFields();
        return listingDetails.getCaseData();
    }

    private void initReport(ListingDetails listingDetails) {
        var adhocReportType = new AdhocReportType();

        adhocReportType.setCasesCompletedHearingTotal(ZERO);
        adhocReportType.setSessionDaysTotal(ZERO);
        adhocReportType.setCompletedPerSessionTotal(ZERO_DECIMAL);
        adhocReportType.setConNoneCasesCompletedHearing(ZERO);
        adhocReportType.setConNoneSessionDays(ZERO);
        adhocReportType.setConNoneCompletedPerSession(ZERO_DECIMAL);
        adhocReportType.setConFastCasesCompletedHearing(ZERO);
        adhocReportType.setConFastSessionDays(ZERO);
        adhocReportType.setConFastCompletedPerSession(ZERO_DECIMAL);
        adhocReportType.setConStdCasesCompletedHearing(ZERO);
        adhocReportType.setConStdSessionDays(ZERO);
        adhocReportType.setConStdCompletedPerSession(ZERO_DECIMAL);
        adhocReportType.setConOpenCasesCompletedHearing(ZERO);
        adhocReportType.setConOpenSessionDays(ZERO);
        adhocReportType.setConOpenCompletedPerSession(ZERO_DECIMAL);
        adhocReportType.setReportOffice(UtilHelper.getListingCaseTypeId(listingDetails.getCaseTypeId()));

        var listingData = listingDetails.getCaseData();
        listingData.setLocalReportsDetailHdr(adhocReportType);
        listingData.setLocalReportsDetail(new ArrayList<>());
    }

    private void executeReport(ListingDetails listingDetails, List<SubmitEvent> submitEvents) {
        log.info(String.format("Cases Completed report case type id %s search results: %d", listingDetails.getCaseTypeId(), submitEvents.size()));
        var localReportsDetailHdr = listingDetails.getCaseData().getLocalReportsDetailHdr();
        var localReportsDetailList = listingDetails.getCaseData().getLocalReportsDetail();
        for (var submitEvent : submitEvents) {
            if (isValidCaseForCasesCompletedReport(submitEvent)) {
                var localReportsDetailItem =
                        getCasesCompletedDetailItem(listingDetails, submitEvent.getCaseData());
                if (localReportsDetailItem.getValue() != null) {
                    updateCasesCompletedDetailHdr(localReportsDetailItem, localReportsDetailHdr);
                    localReportsDetailList.add(localReportsDetailItem);
                }
            }
        }
    }

    private boolean isValidCaseForCasesCompletedReport(SubmitEvent submitEvent) {
        return (submitEvent.getState() != null
                && submitEvent.getState().equals(CLOSED_STATE)
                && isCaseWithHearings(submitEvent.getCaseData())
                && isValidPositionType(submitEvent.getCaseData())
                && isValidJurisdictionOutcome(submitEvent.getCaseData()));
    }

    private boolean isValidPositionType(CaseData caseData) {
        if (caseData.getPositionType() != null) {
            return !INVALID_POSITION_TYPES.contains(caseData.getPositionType());
        } else {
            return true;
        }
    }

    private boolean isCaseWithHearings(CaseData caseData) {
        return CollectionUtils.isNotEmpty (caseData.getHearingCollection());
    }

    private boolean isValidJurisdictionOutcome(CaseData caseData) {
        if (CollectionUtils.isNotEmpty(caseData.getJurCodesCollection())) {
            for (var jurCodesTypeItem : caseData.getJurCodesCollection()) {
                var jurCodesType = jurCodesTypeItem.getValue();
                if (VALID_JURISDICTION_OUTCOMES.contains(jurCodesType.getJudgmentOutcome())) {
                    return true;
                }
            }
            return false;
        } else {
            return true;
        }
    }

    private AdhocReportTypeItem getCasesCompletedDetailItem(ListingDetails listingDetails, CaseData caseData) {
        var adhocReportTypeItem = new AdhocReportTypeItem();

        var listingData = listingDetails.getCaseData();
        for (var hearingTypeItem : caseData.getHearingCollection()) {
            if (isValidHearing(hearingTypeItem)) {
                var hearingType = hearingTypeItem.getValue();

                var latestSession = getLatestDisposedHearingSession(hearingType.getHearingDateCollection(), listingData);
                if (latestSession != null) {
                    var reportDetail = createReportDetail(caseData, hearingType, latestSession);
                    adhocReportTypeItem.setValue(reportDetail);
                }
            }
        }
        return adhocReportTypeItem;
    }

    private boolean isValidHearing(HearingTypeItem hearingTypeItem) {
        if (CollectionUtils.isEmpty(hearingTypeItem.getValue().getHearingDateCollection())) {
            return false;
        }

        return VALID_HEARING_TYPES.contains(hearingTypeItem.getValue().getHearingType());
    }

    private DateListedType getLatestDisposedHearingSession(List<DateListedTypeItem> hearings, ListingData listingData) {
        var filteredHearingSessions = hearings.stream()
                .filter(h->HEARING_STATUS_HEARD.equals(h.getValue().getHearingStatus()))
                .filter(h->YES.equals(h.getValue().getHearingCaseDisposed()))
                .filter(h-> {
                    var listingDate = h.getValue().getListedDate().substring(0, 10);
                    return ReportHelper.validateMatchingDate(listingData, listingDate);
                })
                .collect(Collectors.toList());

        if (filteredHearingSessions.isEmpty()) {
            return null;
        } else {
            return Collections.max(filteredHearingSessions,
                    Comparator.comparing(c -> c.getValue().getListedDate())).getValue();
        }
    }

    private long getSessionDays(List<DateListedTypeItem> hearings, LocalDate latestSessionDate) {
        return hearings.stream()
                .filter(h->HEARING_STATUS_HEARD.equals(h.getValue().getHearingStatus()))
                .filter(h-> {
                    var sessionDate = LocalDate.parse(h.getValue().getListedDate().substring(0, 10), OLD_DATE_TIME_PATTERN2);
                    return !sessionDate.isAfter(latestSessionDate);
                }).count();
    }

    private AdhocReportType createReportDetail(CaseData caseData, HearingType hearingType, DateListedType latestSession) {
        var adhocReportType = new AdhocReportType();
        adhocReportType.setCaseReference(caseData.getEthosCaseReference());
        adhocReportType.setPosition(caseData.getCurrentPosition());
        adhocReportType.setConciliationTrack(getConciliationTrack(caseData));
        adhocReportType.setConciliationTrackNo(getConciliationTrackNumber(caseData.getConciliationTrack()));

        var latestSessionDate = LocalDate.parse(latestSession.getListedDate().substring(0, 10), OLD_DATE_TIME_PATTERN2);
        var sessionDays = getSessionDays(hearingType.getHearingDateCollection(), latestSessionDate);
        adhocReportType.setSessionDays(String.valueOf(sessionDays));

        adhocReportType.setHearingNumber(hearingType.getHearingNumber());
        adhocReportType.setHearingDate(latestSession.getListedDate());
        adhocReportType.setHearingType(hearingType.getHearingType());
        adhocReportType.setHearingJudge(hearingType.getJudge());
        adhocReportType.setHearingClerk(latestSession.getHearingClerk());

        return adhocReportType;
    }

    private String getConciliationTrack(CaseData caseData) {
        return StringUtils.isNotBlank(caseData.getConciliationTrack() ) ?
                caseData.getConciliationTrack() : CONCILIATION_TRACK_NO_CONCILIATION;
    }

    private String getConciliationTrackNumber(String conciliationTrack) {
        if (CONCILIATION_TRACK_NO_CONCILIATION.equals(conciliationTrack)) {
            return CONCILIATION_TRACK_NUMBER_ONE;
        } else if (CONCILIATION_TRACK_FAST_TRACK.equals(conciliationTrack)) {
            return CONCILIATION_TRACK_NUMBER_TWO;
        } else if (CONCILIATION_TRACK_STANDARD_TRACK.equals(conciliationTrack)) {
            return CONCILIATION_TRACK_NUMBER_THREE;
        } else if (CONCILIATION_TRACK_OPEN_TRACK.equals(conciliationTrack)) {
            return CONCILIATION_TRACK_NUMBER_FOUR;
        } else {
            return CONCILIATION_TRACK_NUMBER_ONE;
        }
    }

    private void updateCasesCompletedDetailHdr(AdhocReportTypeItem localReportsDetailItem,
                                                      AdhocReportType localReportsDetailHdr) {
        var adhocReportType = localReportsDetailItem.getValue();

        switch (adhocReportType.getConciliationTrackNo()) {
            case CONCILIATION_TRACK_NUMBER_ONE:
                handleTrackOne(adhocReportType, localReportsDetailHdr);
                break;
            case CONCILIATION_TRACK_NUMBER_TWO:
                handleTrackTwo(adhocReportType, localReportsDetailHdr);
                break;
            case CONCILIATION_TRACK_NUMBER_THREE:
                handleTrackThree(adhocReportType, localReportsDetailHdr);
                break;
            case CONCILIATION_TRACK_NUMBER_FOUR:
                handleTrackFour(adhocReportType, localReportsDetailHdr);
                break;
            default:
                return;
        }

        updateTotals(adhocReportType, localReportsDetailHdr);
    }

    private void handleTrackOne(AdhocReportType adhocReportType, AdhocReportType localReportsDetailHdr) {
        var completedAtHearingPerTrack = Integer.parseInt(localReportsDetailHdr.getConNoneCasesCompletedHearing());
        var sessionDaysTakenPerTrack = Integer.parseInt(localReportsDetailHdr.getConNoneSessionDays());

        completedAtHearingPerTrack++;
        sessionDaysTakenPerTrack += Integer.parseInt(adhocReportType.getSessionDays());
        var completedPerSessionDayPerTrack = (double)completedAtHearingPerTrack / sessionDaysTakenPerTrack;

        localReportsDetailHdr.setConNoneCasesCompletedHearing(Integer.toString(completedAtHearingPerTrack));
        localReportsDetailHdr.setConNoneSessionDays(Integer.toString(sessionDaysTakenPerTrack));
        localReportsDetailHdr.setConNoneCompletedPerSession(String.format(COMPLETED_PER_SESSION_FORMAT, completedPerSessionDayPerTrack));
    }

    private void handleTrackTwo(AdhocReportType adhocReportType, AdhocReportType localReportsDetailHdr) {
        var completedAtHearingPerTrack = Integer.parseInt(localReportsDetailHdr.getConFastCasesCompletedHearing());
        var sessionDaysTakenPerTrack = Integer.parseInt(localReportsDetailHdr.getConFastSessionDays());

        completedAtHearingPerTrack++;
        sessionDaysTakenPerTrack += Integer.parseInt(adhocReportType.getSessionDays());
        var completedPerSessionDayPerTrack = (double)completedAtHearingPerTrack / sessionDaysTakenPerTrack;

        localReportsDetailHdr.setConFastCasesCompletedHearing(Integer.toString(completedAtHearingPerTrack));
        localReportsDetailHdr.setConFastSessionDays(Integer.toString(sessionDaysTakenPerTrack));
        localReportsDetailHdr.setConFastCompletedPerSession(String.format(COMPLETED_PER_SESSION_FORMAT, completedPerSessionDayPerTrack));
    }

    private void handleTrackThree(AdhocReportType adhocReportType, AdhocReportType localReportsDetailHdr) {
        var completedAtHearingPerTrack = Integer.parseInt(localReportsDetailHdr.getConStdCasesCompletedHearing());
        var sessionDaysTakenPerTrack = Integer.parseInt(localReportsDetailHdr.getConStdSessionDays());

        completedAtHearingPerTrack++;
        sessionDaysTakenPerTrack += Integer.parseInt(adhocReportType.getSessionDays());
        var completedPerSessionDayPerTrack = (double)completedAtHearingPerTrack / sessionDaysTakenPerTrack;

        localReportsDetailHdr.setConStdCasesCompletedHearing(Integer.toString(completedAtHearingPerTrack));
        localReportsDetailHdr.setConStdSessionDays(Integer.toString(sessionDaysTakenPerTrack));
        localReportsDetailHdr.setConStdCompletedPerSession(String.format(COMPLETED_PER_SESSION_FORMAT, completedPerSessionDayPerTrack));
    }

    private void handleTrackFour(AdhocReportType adhocReportType, AdhocReportType localReportsDetailHdr) {
        var completedAtHearingPerTrack = Integer.parseInt(localReportsDetailHdr.getConOpenCasesCompletedHearing());
        var sessionDaysTakenPerTrack = Integer.parseInt(localReportsDetailHdr.getConOpenSessionDays());

        completedAtHearingPerTrack++;
        sessionDaysTakenPerTrack += Integer.parseInt(adhocReportType.getSessionDays());
        var completedPerSessionDayPerTrack = (double)completedAtHearingPerTrack / sessionDaysTakenPerTrack;

        localReportsDetailHdr.setConOpenCasesCompletedHearing(Integer.toString(completedAtHearingPerTrack));
        localReportsDetailHdr.setConOpenSessionDays(Integer.toString(sessionDaysTakenPerTrack));
        localReportsDetailHdr.setConOpenCompletedPerSession(String.format(COMPLETED_PER_SESSION_FORMAT, completedPerSessionDayPerTrack));
    }

    private void updateTotals(AdhocReportType adhocReportType, AdhocReportType localReportsDetailHdr) {
        var completedAtHearingTotal = Integer.parseInt(localReportsDetailHdr.getCasesCompletedHearingTotal());
        var sessionDaysTakenTotal = Integer.parseInt(localReportsDetailHdr.getSessionDaysTotal());

        completedAtHearingTotal++;
        sessionDaysTakenTotal += Integer.parseInt(adhocReportType.getSessionDays());
        var completedPerSessionDayTotal = (double)completedAtHearingTotal / sessionDaysTakenTotal;

        localReportsDetailHdr.setCasesCompletedHearingTotal(Integer.toString(completedAtHearingTotal));
        localReportsDetailHdr.setSessionDaysTotal(Integer.toString(sessionDaysTakenTotal));
        localReportsDetailHdr.setCompletedPerSessionTotal(String.format(COMPLETED_PER_SESSION_FORMAT, completedPerSessionDayTotal));
    }
}
