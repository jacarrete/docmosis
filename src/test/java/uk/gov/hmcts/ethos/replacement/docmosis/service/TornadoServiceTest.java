package uk.gov.hmcts.ethos.replacement.docmosis.service;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.ecm.common.idam.models.UserDetails;
import uk.gov.hmcts.ecm.common.model.bulk.BulkData;
import uk.gov.hmcts.ecm.common.model.ccd.CaseData;
import uk.gov.hmcts.ecm.common.model.ccd.DocumentInfo;
import uk.gov.hmcts.ecm.common.model.ccd.SubmitEvent;
import uk.gov.hmcts.ecm.common.model.listing.ListingData;
import uk.gov.hmcts.ecm.common.model.listing.items.ListingTypeItem;
import uk.gov.hmcts.ecm.common.model.listing.types.ListingType;
import uk.gov.hmcts.ecm.common.model.multiples.MultipleDetails;
import uk.gov.hmcts.ethos.replacement.docmosis.config.TornadoConfiguration;
import uk.gov.hmcts.ethos.replacement.docmosis.helpers.HelperTest;
import uk.gov.hmcts.ethos.replacement.docmosis.helpers.MultipleUtil;
import uk.gov.hmcts.ethos.replacement.docmosis.idam.IdamApi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.*;

public class TornadoServiceTest {

    @InjectMocks
    private TornadoService tornadoService;
    @Mock
    private DocumentManagementService documentManagementService;
    private UserService userService;
    private DocumentInfo documentInfo;
    private CaseData caseData;
    private BulkData bulkData;
    private ListingData listingData;
    private UserDetails userDetails;
    private String userToken;
    private TreeMap<String, Object> multipleObjectsFlags;
    private TreeMap<String, Object> multipleObjectsSubMultiple;
    private MultipleDetails multipleDetails;
    private List<SubmitEvent> submitEventList;

    @Before
    public void setUp() {
        documentInfo = new DocumentInfo();
        TornadoConfiguration tornadoConfiguration = new TornadoConfiguration();
        tornadoConfiguration.setUrl("http://google.com");
        caseData = new CaseData();
        bulkData = new BulkData();
        bulkData.setScheduleDocName(LIST_CASES_CONFIG);
        bulkData.setSearchCollection(new ArrayList<>());
        listingData = new ListingData();
        ListingTypeItem listingTypeItem = new ListingTypeItem();
        ListingType listingType = new ListingType();
        listingType.setCauseListDate("2019-12-12");
        listingTypeItem.setId("1111");
        listingTypeItem.setValue(listingType);
        listingData.setHearingDocType(HEARING_DOC_ETCL);
        listingData.setHearingDocETCL(HEARING_ETCL_STAFF);
        listingData.setHearingDateType(SINGLE_HEARING_DATE_TYPE);
        listingData.setListingVenue("Glasgow");
        listingData.setListingCollection(new ArrayList<>(Collections.singleton(listingTypeItem)));
        userDetails = HelperTest.getUserDetails();
        IdamApi idamApi = authorisation -> userDetails;
        userService = new UserService(idamApi);
        tornadoService = new TornadoService(tornadoConfiguration, documentManagementService, userService);
        userToken = "authToken";
        multipleObjectsFlags = MultipleUtil.getMultipleObjectsFlags();
        multipleObjectsSubMultiple = MultipleUtil.getMultipleObjectsSubMultiple();
        multipleDetails = new MultipleDetails();
        multipleDetails.setCaseData(MultipleUtil.getMultipleData());
        multipleDetails.setCaseTypeId("Manchester_Multiple");
        submitEventList = MultipleUtil.getSubmitEvents();
    }

    @Test(expected = Exception.class)
    public void documentGenerationError() throws IOException {
        when(userService.getUserDetails(anyString())).thenThrow(new RuntimeException());
        tornadoService.documentGeneration(userToken, caseData, MANCHESTER_CASE_TYPE_ID);
    }

    @Test
    public void documentGeneration() throws IOException {
        DocumentInfo documentInfo1 = tornadoService.documentGeneration(userToken, caseData, MANCHESTER_CASE_TYPE_ID);
        assertEquals(documentInfo.toString(), documentInfo1.toString());
    }

    @Test
    public void listingGeneration() throws IOException {
        DocumentInfo documentInfo1 = tornadoService.listingGeneration(userToken, listingData, MANCHESTER_LISTING_CASE_TYPE_ID);
        assertEquals(documentInfo.toString(), documentInfo1.toString());
    }

    @Test
    public void scheduleGeneration() throws IOException {
        DocumentInfo documentInfo1 = tornadoService.scheduleGeneration(userToken, bulkData);
        assertEquals(documentInfo.toString(), documentInfo1.toString());
    }

    @Test
    public void scheduleMultipleGenerationFlags() throws IOException {
        DocumentInfo documentInfo1 = tornadoService.scheduleMultipleGeneration(userToken,
                multipleDetails.getCaseData(),
                multipleObjectsFlags,
                submitEventList);
        assertEquals(documentInfo.toString(), documentInfo1.toString());
    }

    @Test
    public void scheduleMultipleGenerationFlagsClaimantCompany() throws IOException {
        submitEventList.get(0).getCaseData().setClaimantCompany("Company");
        DocumentInfo documentInfo1 = tornadoService.scheduleMultipleGeneration(userToken,
                multipleDetails.getCaseData(),
                multipleObjectsFlags,
                submitEventList);
        assertEquals(documentInfo.toString(), documentInfo1.toString());
    }

    @Test
    public void scheduleMultipleGenerationFlagsNullClaimant() throws IOException {
        submitEventList.get(0).getCaseData().setClaimantIndType(null);
        DocumentInfo documentInfo1 = tornadoService.scheduleMultipleGeneration(userToken,
                multipleDetails.getCaseData(),
                multipleObjectsFlags,
                submitEventList);
        assertEquals(documentInfo.toString(), documentInfo1.toString());
    }

    @Test
    public void scheduleMultipleGenerationWrongScheduleDocName() throws IOException {
        multipleDetails.getCaseData().setScheduleDocName("WRONG_SCHEDULE_NAME");
        DocumentInfo documentInfo1 = tornadoService.scheduleMultipleGeneration(userToken,
                multipleDetails.getCaseData(),
                multipleObjectsSubMultiple,
                submitEventList);
        assertEquals(documentInfo.toString(), documentInfo1.toString());
    }

    @Test
    public void scheduleMultipleGenerationSubMultiple() throws IOException {
        multipleDetails.getCaseData().setScheduleDocName(LIST_CASES_CONFIG);
        DocumentInfo documentInfo1 = tornadoService.scheduleMultipleGeneration(userToken,
                multipleDetails.getCaseData(),
                multipleObjectsSubMultiple,
                submitEventList);
        assertEquals(documentInfo.toString(), documentInfo1.toString());
    }

}