package uk.gov.hmcts.ethos.replacement.docmosis.helpers;

import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.ecm.common.model.ccd.items.RepresentedTypeRItem;
import uk.gov.hmcts.ecm.common.model.ccd.types.AddressLabelsAttributesType;
import uk.gov.hmcts.ecm.common.model.ccd.types.RepresentedTypeC;
import uk.gov.hmcts.ecm.common.model.ccd.types.RepresentedTypeR;
import uk.gov.hmcts.ecm.common.model.labels.LabelPayloadEvent;
import uk.gov.hmcts.ecm.common.model.multiples.MultipleDetails;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.*;
import static uk.gov.hmcts.ethos.replacement.docmosis.helpers.LabelsHelper.MAX_NUMBER_LABELS;

public class LabelsHelperTest {

    private MultipleDetails multipleDetails;
    private List<LabelPayloadEvent> labelPayloadEvents;
    private AddressLabelsAttributesType addressLabelsAttributesType;

    @Before
    public void setUp() {
        multipleDetails = new MultipleDetails();
        multipleDetails.setCaseData(MultipleUtil.getMultipleData());
        multipleDetails.setCaseTypeId("Leeds_Multiple");
        labelPayloadEvents = MultipleUtil.getLabelPayloadEvents();
        addressLabelsAttributesType = new AddressLabelsAttributesType();
    }

    @Test
    public void customiseSelectedAddressesMultiplesEmptySelectedAddresses() {
        assertNull(LabelsHelper.customiseSelectedAddressesMultiples(labelPayloadEvents,
                multipleDetails.getCaseData()));
    }

    @Test
    public void customiseSelectedAddressesMultiples() {
        multipleDetails.getCaseData().setAddressLabelsSelectionTypeMSL(
                new ArrayList<>(Arrays.asList(CLAIMANT_ADDRESS_LABEL, CLAIMANT_REP_ADDRESS_LABEL)));
        assertEquals(2, Objects.requireNonNull(
                LabelsHelper.customiseSelectedAddressesMultiples(labelPayloadEvents, multipleDetails.getCaseData())).size());
    }

    @Test
    public void customiseSelectedAddressesMultiplesClaimant() {
        labelPayloadEvents.get(0).getLabelPayloadES().setClaimantTypeOfClaimant(INDIVIDUAL_TYPE_CLAIMANT);
        labelPayloadEvents.get(0).getLabelPayloadES().setClaimantType(null);
        multipleDetails.getCaseData().setAddressLabelsSelectionTypeMSL(
                new ArrayList<>(Arrays.asList(CLAIMANT_ADDRESS_LABEL, CLAIMANT_REP_ADDRESS_LABEL)));
        assertEquals(2,
                Objects.requireNonNull(
                        LabelsHelper.customiseSelectedAddressesMultiples(labelPayloadEvents, multipleDetails.getCaseData())).size());
    }

    @Test
    public void customiseSelectedAddressesMultiplesClaimantRep() {
        labelPayloadEvents.get(0).getLabelPayloadES().setClaimantRepresentedQuestion(YES);
        RepresentedTypeC representedTypeC = new RepresentedTypeC();
        representedTypeC.setNameOfRepresentative("Name");
        representedTypeC.setRepresentativeReference("1234");
        labelPayloadEvents.get(0).getLabelPayloadES().setRepresentativeClaimantType(representedTypeC);
        multipleDetails.getCaseData().setAddressLabelsSelectionTypeMSL(
                new ArrayList<>(Arrays.asList(CLAIMANT_ADDRESS_LABEL, CLAIMANT_REP_ADDRESS_LABEL)));
        assertEquals(3,
                Objects.requireNonNull(
                        LabelsHelper.customiseSelectedAddressesMultiples(labelPayloadEvents, multipleDetails.getCaseData())).size());
    }

    @Test
    public void customiseSelectedAddressesMultiplesRespondent() {
        multipleDetails.getCaseData().setAddressLabelsSelectionTypeMSL(
                new ArrayList<>(Collections.singletonList(RESPONDENTS_ADDRESS__LABEL)));
        assertEquals(2,
                Objects.requireNonNull(
                        LabelsHelper.customiseSelectedAddressesMultiples(labelPayloadEvents, multipleDetails.getCaseData())).size());
    }

    @Test
    public void customiseSelectedAddressesMultiplesRespondentRep() {
        RepresentedTypeR representedTypeR = new RepresentedTypeR();
        representedTypeR.setNameOfRepresentative("Name");
        representedTypeR.setRepresentativeReference("1234");
        RepresentedTypeRItem representedTypeRItem = new RepresentedTypeRItem();
        representedTypeRItem.setId("12345");
        representedTypeRItem.setValue(representedTypeR);
        List<RepresentedTypeRItem> repCollection = new ArrayList<>();
        repCollection.add(representedTypeRItem);
        labelPayloadEvents.get(0).getLabelPayloadES().setRepCollection(repCollection);
        multipleDetails.getCaseData().setAddressLabelsSelectionTypeMSL(
                new ArrayList<>(Arrays.asList(CLAIMANT_ADDRESS_LABEL, RESPONDENTS_REPS_ADDRESS__LABEL)));
        assertEquals(3,
                Objects.requireNonNull(
                        LabelsHelper.customiseSelectedAddressesMultiples(labelPayloadEvents, multipleDetails.getCaseData())).size());
    }

    @Test
    public void midValidateAddressLabelsMultiple() {
        addressLabelsAttributesType.setNumberOfSelectedLabels("2");
        addressLabelsAttributesType.setNumberOfCopies("1");
        List<String> errors = LabelsHelper.midValidateAddressLabelsErrors(addressLabelsAttributesType, MULTIPLE_CASE_TYPE);
        assertEquals(0, errors.size());
    }

    @Test
    public void midValidateAddressLabelsMultipleErrors() {
        addressLabelsAttributesType.setNumberOfSelectedLabels("20000");
        addressLabelsAttributesType.setNumberOfCopies("3");
        List<String> errors = LabelsHelper.midValidateAddressLabelsErrors(addressLabelsAttributesType, MULTIPLE_CASE_TYPE);
        assertEquals(1, errors.size());
        assertEquals(ADDRESS_LABELS_LABELS_LIMIT_ERROR + " of " + MAX_NUMBER_LABELS, errors.get(0));
    }

    @Test
    public void midValidateAddressLabelsSelectErrors() {
        addressLabelsAttributesType.setNumberOfSelectedLabels("0");
        List<String> errors = LabelsHelper.midValidateAddressLabelsErrors(addressLabelsAttributesType, SINGLE_CASE_TYPE);
        assertEquals(1, errors.size());
        assertEquals(ADDRESS_LABELS_SELECT_ERROR, errors.get(0));
    }

    @Test
    public void midValidateAddressLabelsCopiesErrors() {
        addressLabelsAttributesType.setNumberOfSelectedLabels("2");
        addressLabelsAttributesType.setNumberOfCopies(".");
        List<String> errors = LabelsHelper.midValidateAddressLabelsErrors(addressLabelsAttributesType, SINGLE_CASE_TYPE);
        assertEquals(1, errors.size());
        assertEquals(ADDRESS_LABELS_COPIES_ERROR, errors.get(0));
    }

    @Test
    public void midValidateAddressLabelsLimitNumberCopiesErrors() {
        addressLabelsAttributesType.setNumberOfSelectedLabels("2");
        addressLabelsAttributesType.setNumberOfCopies("11");
        List<String> errors = LabelsHelper.midValidateAddressLabelsErrors(addressLabelsAttributesType, SINGLE_CASE_TYPE);
        assertEquals(1, errors.size());
        assertEquals(ADDRESS_LABELS_COPIES_LESS_10_ERROR, errors.get(0));
    }
}