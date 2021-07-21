package uk.gov.hmcts.ethos.replacement.docmosis.helpers;

import uk.gov.hmcts.ecm.common.model.ccd.CaseData;
import uk.gov.hmcts.ecm.common.model.ccd.items.JudgementTypeItem;
import uk.gov.hmcts.ecm.common.model.ccd.items.JurCodesTypeItem;
import uk.gov.hmcts.ecm.common.model.ccd.items.RepresentedTypeRItem;
import uk.gov.hmcts.ecm.common.model.ccd.items.RespondentSumTypeItem;
import uk.gov.hmcts.ecm.common.model.ccd.types.*;
import uk.gov.hmcts.ecm.common.model.multiples.MultipleData;
import uk.gov.hmcts.ecm.common.model.servicebus.datamodel.UpdateDataModel;

import java.util.List;
import java.util.Optional;

public class UpdateDataModelBuilder {

    private UpdateDataModelBuilder() {
        // Access through static methods
    }

    public static UpdateDataModel build(MultipleData multipleData, CaseData caseData) {
        return UpdateDataModel.builder()
                .managingOffice(multipleData.getManagingOffice())
                .fileLocation(multipleData.getFileLocation())
                .fileLocationGlasgow(multipleData.getFileLocationGlasgow())
                .fileLocationAberdeen(multipleData.getFileLocationAberdeen())
                .fileLocationDundee(multipleData.getFileLocationDundee())
                .fileLocationEdinburgh(multipleData.getFileLocationEdinburgh())
                .clerkResponsible(multipleData.getClerkResponsible())
                .positionType(multipleData.getPositionType())
                .receiptDate(multipleData.getReceiptDate())
                .hearingStage(multipleData.getHearingStage())

                .isRespondentRepRemovalUpdate(multipleData.getBatchRemoveRespondentRep())
                .isClaimantRepRemovalUpdate(multipleData.getBatchRemoveClaimantRep())

                .representativeClaimantType(getRepresentativeClaimantType(multipleData, caseData))
                .jurCodesType(getJurCodesType(multipleData, caseData))
                .respondentSumType(getRespondentSumType(multipleData, caseData))
                .judgementType(getJudgementType(multipleData, caseData))
                .representedType(getRespondentRepType(multipleData, caseData))
                .build();
    }

    private static JurCodesType getJurCodesType(MultipleData multipleData, CaseData caseData) {
        if (caseData == null) {
            return null;
        }

        List<JurCodesTypeItem> jurCodesCollection = caseData.getJurCodesCollection();

        if (multipleData.getBatchUpdateJurisdiction().getValue() != null
                && jurCodesCollection != null) {
            String jurCodeToSearch = multipleData.getBatchUpdateJurisdiction().getValue().getLabel();
            Optional<JurCodesTypeItem> jurCodesTypeItemOptional =
                    jurCodesCollection.stream()
                            .filter(jurCodesTypeItem ->
                                    jurCodesTypeItem.getValue().getJuridictionCodesList().equals(jurCodeToSearch))
                            .findAny();

            if (jurCodesTypeItemOptional.isPresent()) {
                return jurCodesTypeItemOptional.get().getValue();
            }
        }

        return null;
    }

    private static RespondentSumType getRespondentSumType(MultipleData multipleData, CaseData caseData) {
        if (caseData == null) {
            return null;
        }

        List<RespondentSumTypeItem> respondentCollection = caseData.getRespondentCollection();

        if (multipleData.getBatchUpdateRespondent().getValue() != null
                && respondentCollection != null) {
            String respondentToSearch = multipleData.getBatchUpdateRespondent().getValue().getLabel();
            Optional<RespondentSumTypeItem> respondentSumTypeItemOptional =
                    respondentCollection.stream()
                            .filter(respondentSumTypeItem ->
                                    respondentSumTypeItem.getValue().getRespondentName().equals(respondentToSearch))
                            .findAny();

            if (respondentSumTypeItemOptional.isPresent()) {
                return respondentSumTypeItemOptional.get().getValue();
            }
        }

        return null;
    }

    private static JudgementType getJudgementType(MultipleData multipleData, CaseData caseData) {
        if (caseData == null) {
            return null;
        }

        List<JudgementTypeItem> judgementCollection = caseData.getJudgementCollection();

        if (multipleData.getBatchUpdateJudgment().getValue() != null
                && judgementCollection != null) {
            String judgementIdToSearch = multipleData.getBatchUpdateJudgment().getValue().getCode();
            Optional<JudgementTypeItem> judgementTypeItemOptional =
                    judgementCollection.stream()
                            .filter(judgementTypeItem ->
                                    judgementTypeItem.getId().equals(judgementIdToSearch))
                            .findAny();

            if (judgementTypeItemOptional.isPresent()) {
                return judgementTypeItemOptional.get().getValue();
            }
        }

        return null;
    }

    public static RepresentedTypeR getRespondentRepType(MultipleData multipleData, CaseData caseData) {
        if (caseData == null) {
            return null;
        }
        List<RepresentedTypeRItem> repCollection = caseData.getRepCollection();

        if (multipleData.getBatchUpdateRespondentRep().getValue() != null
                && repCollection != null) {
            String respondentRepIdToSearch = multipleData.getBatchUpdateRespondentRep().getValue().getCode();
            Optional<RepresentedTypeRItem> representedTypeRItemOptional =
                    repCollection.stream()
                            .filter(representedTypeRItem ->
                                    representedTypeRItem.getId().equals(respondentRepIdToSearch))
                            .findAny();

            if (representedTypeRItemOptional.isPresent()) {
                return representedTypeRItemOptional.get().getValue();
            }
        }

        return null;
    }

    private static RepresentedTypeC getRepresentativeClaimantType(MultipleData multipleData, CaseData caseData) {
        if (caseData == null) {
            return null;
        }

        var representedTypeC = caseData.getRepresentativeClaimantType();
        if (multipleData.getBatchUpdateClaimantRep() != null && representedTypeC != null) {
            String claimantRepresentative = multipleData.getBatchUpdateClaimantRep().getValue().getCode();

            if (claimantRepresentative.equals(representedTypeC.getNameOfRepresentative())) {
                return representedTypeC;
            }
        }

        return null;
    }
}
