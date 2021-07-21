package uk.gov.hmcts.ethos.replacement.docmosis.service;

import static com.google.common.base.Strings.isNullOrEmpty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ecm.common.client.CcdClient;
import uk.gov.hmcts.ecm.common.exceptions.CaseCreationException;
import uk.gov.hmcts.ecm.common.exceptions.CaseRetrievalException;
import uk.gov.hmcts.ecm.common.exceptions.DocumentManagementException;
import uk.gov.hmcts.ecm.common.helpers.UtilHelper;
import uk.gov.hmcts.ecm.common.model.ccd.CaseData;
import uk.gov.hmcts.ecm.common.model.ccd.CaseDetails;
import uk.gov.hmcts.ecm.common.model.ccd.DocumentInfo;
import uk.gov.hmcts.ecm.common.model.ccd.SubmitEvent;
import uk.gov.hmcts.ecm.common.model.ccd.items.DateListedTypeItem;
import uk.gov.hmcts.ecm.common.model.ccd.items.HearingTypeItem;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.*;
import uk.gov.hmcts.ecm.common.model.listing.ListingData;
import uk.gov.hmcts.ecm.common.model.listing.ListingDetails;
import uk.gov.hmcts.ecm.common.model.listing.items.ListingTypeItem;
import uk.gov.hmcts.ethos.replacement.docmosis.helpers.ListingHelper;
import uk.gov.hmcts.ethos.replacement.docmosis.helpers.ReportHelper;
import static uk.gov.hmcts.ethos.replacement.docmosis.helpers.ReportHelper.CASES_SEARCHED;
import uk.gov.hmcts.ethos.replacement.docmosis.reports.CasesCompletedReport;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service("listingService")
public class ListingService {

    private final TornadoService tornadoService;
    private final CcdClient ccdClient;
    private final CasesCompletedReport casesCompletedReport;

    private static final String MISSING_DOCUMENT_NAME = "Missing document name";
    private static final String MESSAGE = "Failed to generate document for case id : ";

    @Autowired
    public ListingService(TornadoService tornadoService, CcdClient ccdClient, CasesCompletedReport casesCompletedReport) {
        this.tornadoService = tornadoService;
        this.ccdClient = ccdClient;
        this.casesCompletedReport = casesCompletedReport;
    }

    public ListingData listingCaseCreation(ListingDetails listingDetails) {

        var listingData = listingDetails.getCaseData();

        if (listingData.getHearingDocType() != null) {
            listingData.setDocumentName(listingData.getHearingDocType());
        } else if (listingData.getReportType() != null) {
            listingData.setDocumentName(listingData.getReportType());
        } else {
            listingData.setDocumentName(MISSING_DOCUMENT_NAME);
        }

        return listingData;
    }

    public CaseData processListingSingleCasesRequest(CaseDetails caseDetails) {
        var caseData = caseDetails.getCaseData();
        List<ListingTypeItem> listingTypeItems = new ArrayList<>();
        if (caseData.getHearingCollection() != null && !caseData.getHearingCollection().isEmpty()) {
            for (HearingTypeItem hearingTypeItem : caseData.getHearingCollection()) {
                if (hearingTypeItem.getValue().getHearingDateCollection() != null) {
                    log.info("Processing listing single cases");
                    listingTypeItems.addAll(getListingTypeItems(hearingTypeItem,
                            caseData.getPrintHearingDetails(), caseData));
                }
            }
        }
        caseData.setPrintHearingCollection(caseData.getPrintHearingDetails());
        caseData.getPrintHearingCollection().setListingCollection(listingTypeItems);
        caseData.getPrintHearingCollection().clearReportFields();

        return caseData;
    }

    public ListingData setCourtAddressFromCaseData(CaseData caseData) {
        var listingData = caseData.getPrintHearingCollection();
        listingData.setTribunalCorrespondenceAddress(caseData.getTribunalCorrespondenceAddress());
        listingData.setTribunalCorrespondenceTelephone(caseData.getTribunalCorrespondenceTelephone());
        listingData.setTribunalCorrespondenceFax(caseData.getTribunalCorrespondenceFax());
        listingData.setTribunalCorrespondenceEmail(caseData.getTribunalCorrespondenceEmail());
        listingData.setTribunalCorrespondenceDX(caseData.getTribunalCorrespondenceDX());
        return listingData;
    }

    public ListingData processListingHearingsRequest(ListingDetails listingDetails, String authToken) {
        try {
            List<SubmitEvent> submitEvents = getListingHearingsSearch(listingDetails, authToken);
            if (submitEvents != null) {
                log.info(CASES_SEARCHED + submitEvents.size());
                List<ListingTypeItem> listingTypeItems = new ArrayList<>();
                for (SubmitEvent submitEvent : submitEvents) {
                    if (submitEvent.getCaseData().getHearingCollection() != null
                            && !submitEvent.getCaseData().getHearingCollection().isEmpty()) {
                       addListingTypeItems(submitEvent,listingTypeItems,listingDetails );
                    }
                }
                listingDetails.getCaseData().setListingCollection(listingTypeItems);
            }

            listingDetails.getCaseData().clearReportFields();
            return listingDetails.getCaseData();
        } catch (Exception ex) {
            throw new CaseCreationException(MESSAGE + listingDetails.getCaseId() + ex.getMessage());
        }
    }

    private void addListingTypeItems(SubmitEvent submitEvent, List<ListingTypeItem> listingTypeItems,ListingDetails listingDetails) {
        for (HearingTypeItem hearingTypeItem : submitEvent.getCaseData().getHearingCollection()) {
            if (hearingTypeItem.getValue().getHearingDateCollection() != null) {
                listingTypeItems.addAll(getListingTypeItems(hearingTypeItem,
                        listingDetails.getCaseData(), submitEvent.getCaseData()));
            }
        }
    }
    private List<SubmitEvent> getListingHearingsSearch(ListingDetails listingDetails, String authToken)
            throws IOException {
        var listingData = listingDetails.getCaseData();
        Map.Entry<String, String> entry =
                ListingHelper.getListingVenueToSearch(listingData).entrySet().iterator().next();
        String venueToSearchMapping = entry.getKey();
        String venueToSearch = entry.getValue();
        String dateFrom;
        String dateTo;
        boolean dateRange = listingData.getHearingDateType().equals(RANGE_HEARING_DATE_TYPE);
        if (dateRange) {
            dateFrom = listingData.getListingDateFrom();
            dateTo = listingData.getListingDateTo();
        } else {
            dateFrom = listingData.getListingDate();
            dateTo = listingData.getListingDate();
        }

        return ccdClient.retrieveCasesVenueAndDateElasticSearch(
                authToken, UtilHelper.getListingCaseTypeId(listingDetails.getCaseTypeId()),
                dateFrom, dateTo, venueToSearch, venueToSearchMapping);
    }

    private List<ListingTypeItem> getListingTypeItems(HearingTypeItem hearingTypeItem,
                                                      ListingData listingData, CaseData caseData) {
        List<ListingTypeItem> listingTypeItems = new ArrayList<>();
        if (isHearingTypeValid(listingData, hearingTypeItem)) {
            int hearingDateCollectionSize = hearingTypeItem.getValue().getHearingDateCollection().size();
            for (var i = 0; i < hearingDateCollectionSize; i++) {
                log.info("EthosCaseRef Listing: " + caseData.getEthosCaseReference());
                hearingTypeItem.getValue().getHearingNumber();
                log.info("Hearing number: " + hearingTypeItem.getValue().getHearingNumber());
                var dateListedTypeItem = hearingTypeItem.getValue().getHearingDateCollection().get(i);
                boolean isListingVenueValid = isListingVenueValid(listingData, dateListedTypeItem);
                boolean isListingDateValid = isListingDateValid(listingData, dateListedTypeItem);
                log.info("isListingVenueValid: " + isListingVenueValid);
                log.info("isListingDateValid: " + isListingDateValid);
                var isListingStatusValid = true;
                if (!showAllHearingType(listingData)) {
                    isListingStatusValid = isListingStatusValid(dateListedTypeItem);
                    log.info("isListingStatusValid: " + isListingStatusValid);
                }
                if (!isListingVenueValid || !isListingDateValid || !isListingStatusValid) {
                    continue;
                }
                var listingTypeItem = new ListingTypeItem();
                var listingType = ListingHelper.getListingTypeFromCaseData(
                        listingData, caseData, hearingTypeItem.getValue(), dateListedTypeItem.getValue(),
                        i, hearingDateCollectionSize);
                listingTypeItem.setId(String.valueOf(dateListedTypeItem.getId()));
                listingTypeItem.setValue(listingType);
                listingTypeItems.add(listingTypeItem);
            }
        }
        return listingTypeItems;
    }

    public ListingData generateReportData(ListingDetails listingDetails, String authToken) {

        try {
            List<SubmitEvent> submitEvents = getGenericReportSearch(listingDetails, authToken);
            switch (listingDetails.getCaseData().getReportType()) {
                case BROUGHT_FORWARD_REPORT:
                    return ReportHelper.processBroughtForwardDatesRequest(listingDetails, submitEvents);
                case CLAIMS_ACCEPTED_REPORT:
                    return ReportHelper.processClaimsAcceptedRequest(listingDetails, submitEvents);
                case LIVE_CASELOAD_REPORT:
                    return ReportHelper.processLiveCaseloadRequest(listingDetails, submitEvents);
                case CASES_COMPLETED_REPORT:
                    return casesCompletedReport.generateReportData(listingDetails, submitEvents);
                default:
                    return listingDetails.getCaseData();
            }

        } catch (Exception ex) {
            throw new CaseRetrievalException(MESSAGE + listingDetails.getCaseId() + ex.getMessage());
        }
    }

    private List<SubmitEvent> getGenericReportSearch(ListingDetails listingDetails, String authToken)
            throws IOException {
        var listingData = listingDetails.getCaseData();
        boolean dateRange = listingData.getHearingDateType().equals(RANGE_HEARING_DATE_TYPE);
        if (!dateRange) {
            var dateToSearch = LocalDate.parse(listingData.getListingDate(), OLD_DATE_TIME_PATTERN2).toString();
            return ccdClient.retrieveCasesGenericReportElasticSearch(authToken, UtilHelper.getListingCaseTypeId(
                    listingDetails.getCaseTypeId()), dateToSearch, dateToSearch, listingData.getReportType());
        } else {
            var dateToSearchFrom = LocalDate.parse(listingData.getListingDateFrom(),
                    OLD_DATE_TIME_PATTERN2).toString();
            var dateToSearchTo = LocalDate.parse(listingData.getListingDateTo(), OLD_DATE_TIME_PATTERN2).toString();
            return ccdClient.retrieveCasesGenericReportElasticSearch(authToken, UtilHelper.getListingCaseTypeId(
                    listingDetails.getCaseTypeId()), dateToSearchFrom, dateToSearchTo, listingData.getReportType());
        }
    }

    private boolean isListingVenueValid(ListingData listingData, DateListedTypeItem dateListedTypeItem) {
        if (listingData.getListingVenue().equals(ALL_VENUES)) {
            return true;
        } else {
            String venueSearched;
            String venueToSearch = ListingHelper.getListingVenue(listingData);
            log.info("VENUE TO SEARCH: " + venueToSearch);

            if (ListingHelper.isAllScottishVenues(listingData)) {
                venueSearched = dateListedTypeItem.getValue().getHearingVenueDay() != null
                        ? dateListedTypeItem.getValue().getHearingVenueDay()
                        : " ";
                log.info("Checking venue for all scottish level (HearingVenueDay): " + venueSearched);
            } else {
                venueSearched = ListingHelper.getVenueFromDateListedType(dateListedTypeItem.getValue());
                log.info("Checking venue low level: " + venueSearched);
            }
            return venueSearched.trim().equals(venueToSearch.trim());
        }
    }

    private boolean isListingDateValid(ListingData listingData, DateListedTypeItem dateListedTypeItem) {
        boolean dateRange = listingData.getHearingDateType().equals(RANGE_HEARING_DATE_TYPE);
        String dateListed = !isNullOrEmpty(dateListedTypeItem.getValue().getListedDate())
                ? dateListedTypeItem.getValue().getListedDate()
                : "";
        if (dateRange) {
            String dateToSearchFrom = listingData.getListingDateFrom();
            String dateToSearchTo = listingData.getListingDateTo();
            return ListingHelper.getListingDateBetween(dateToSearchFrom, dateToSearchTo, dateListed);
        } else {
            String dateToSearch = listingData.getListingDate();
            return ListingHelper.getListingDateBetween(dateToSearch, "", dateListed);
        }
    }

    private boolean isListingStatusValid(DateListedTypeItem dateListedTypeItem) {
        var dateListedType = dateListedTypeItem.getValue();

        if (dateListedType.getHearingStatus() != null) {
            List<String> invalidHearingStatuses = Arrays.asList(HEARING_STATUS_SETTLED,
                    HEARING_STATUS_WITHDRAWN, HEARING_STATUS_POSTPONED);
            return invalidHearingStatuses.stream().noneMatch(str -> str.equals(dateListedType.getHearingStatus()));
        } else {
            return true;
        }
    }

    private boolean showAllHearingType(ListingData listingData) {
        return !isNullOrEmpty(listingData.getHearingDocType())
                && !isNullOrEmpty(listingData.getHearingDocETCL())
                && listingData.getHearingDocType().equals(HEARING_DOC_ETCL)
                && listingData.getHearingDocETCL().equals(HEARING_ETCL_STAFF)
                && !isNullOrEmpty(listingData.getShowAll())
                && listingData.getShowAll().equals(YES);
    }

    private boolean isHearingTypeValid(ListingData listingData, HearingTypeItem hearingTypeItem) {
        if (!isNullOrEmpty(listingData.getHearingDocType())
                && !isNullOrEmpty(listingData.getHearingDocETCL())
                && listingData.getHearingDocType().equals(HEARING_DOC_ETCL)
                && !listingData.getHearingDocETCL().equals(HEARING_ETCL_STAFF)) {

            var hearingType = hearingTypeItem.getValue();

            if (hearingType.getHearingType() != null) {
                if (hearingType.getHearingType().equals(HEARING_TYPE_PERLIMINARY_HEARING)
                        && hearingType.getHearingPublicPrivate() != null
                        && hearingType.getHearingPublicPrivate().equals(HEARING_TYPE_PRIVATE)) {
                    return false;
                } else {
                    List<String> invalidHearingTypes = Arrays.asList(HEARING_TYPE_JUDICIAL_MEDIATION,
                            HEARING_TYPE_JUDICIAL_MEDIATION_TCC, HEARING_TYPE_PERLIMINARY_HEARING_CM,
                            HEARING_TYPE_PERLIMINARY_HEARING_CM_TCC);
                    return invalidHearingTypes.stream().noneMatch(str -> str.equals(hearingType.getHearingType()));
                }
            }
        }
        return true;
    }

    public DocumentInfo processHearingDocument(ListingData listingData, String caseTypeId, String authToken) {
        try {
            return tornadoService.listingGeneration(authToken, listingData, caseTypeId);
        } catch (Exception ex) {
            throw new DocumentManagementException(MESSAGE + ex.getMessage());
        }
    }
}