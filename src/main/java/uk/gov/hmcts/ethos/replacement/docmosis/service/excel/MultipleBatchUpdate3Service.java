package uk.gov.hmcts.ethos.replacement.docmosis.service.excel;

import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ecm.common.client.CcdClient;
import uk.gov.hmcts.ecm.common.exceptions.CaseCreationException;
import uk.gov.hmcts.ecm.common.model.ccd.CCDRequest;
import uk.gov.hmcts.ecm.common.model.ccd.CaseData;
import uk.gov.hmcts.ecm.common.model.ccd.SubmitEvent;
import uk.gov.hmcts.ecm.common.model.ccd.items.RepresentedTypeRItem;
import uk.gov.hmcts.ecm.common.model.ccd.items.RespondentSumTypeItem;
import uk.gov.hmcts.ecm.common.model.ccd.types.RepresentedTypeC;
import uk.gov.hmcts.ecm.common.model.ccd.types.RepresentedTypeR;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.*;
import uk.gov.hmcts.ecm.common.model.multiples.MultipleData;
import uk.gov.hmcts.ecm.common.model.multiples.MultipleDetails;
import uk.gov.hmcts.ethos.replacement.docmosis.helpers.UpdateDataModelBuilder;

@Slf4j
@Service("multipleBatchUpdate3Service")
public class MultipleBatchUpdate3Service {

    private final MultipleHelperService multipleHelperService;
    private final SingleCasesReadingService singleCasesReadingService;
    private final CcdClient ccdClient;

    @Autowired
    public MultipleBatchUpdate3Service(MultipleHelperService multipleHelperService,
                                       SingleCasesReadingService singleCasesReadingService, CcdClient ccdClient) {
        this.multipleHelperService = multipleHelperService;
        this.singleCasesReadingService = singleCasesReadingService;
        this.ccdClient = ccdClient;
    }

    public void batchUpdate3Logic(String userToken, MultipleDetails multipleDetails,
                                  List<String> errors, SortedMap<String, Object> multipleObjects) {

        var multipleData = multipleDetails.getCaseData();

        log.info("Batch update type = 3");

        String caseToSearch = multipleData.getBatchUpdateCase();

        log.info("Getting the information from: " + caseToSearch);

        SubmitEvent caseSearched = singleCasesReadingService.retrieveSingleCase(
                userToken,
                multipleDetails.getCaseTypeId(),
                caseToSearch,
                multipleData.getMultipleSource());

        log.info("Checking if there will be any change. Otherwise moving to open state");

        if (checkAnyChange(multipleData)) {

            log.info("Removing caseSearched from filtered cases");

            multipleObjects.remove(caseSearched.getCaseData().getEthosCaseReference());

            log.info("Sending updates to single cases with caseSearched");

            multipleHelperService.sendUpdatesToSinglesWithConfirmation(userToken, multipleDetails, errors,
                    multipleObjects, caseSearched.getCaseData());
            if (YES.equals(multipleData.getBatchRemoveClaimantRep())) {
                removeClaimantRep(caseSearched, multipleData);
            }
            if (YES.equals(multipleData.getBatchRemoveRespondentRep())) {
                var representedTypeRToBeRemoved = UpdateDataModelBuilder.getRespondentRepType(multipleData, caseSearched.getCaseData());
                removeRespondentRep(caseSearched.getCaseData(), representedTypeRToBeRemoved);
            }
            if (YES.equals(multipleData.getBatchRemoveClaimantRep()) || YES.equals(multipleData.getBatchRemoveRespondentRep())) {
                submitEventForCase(userToken, multipleDetails.getCaseTypeId(),
                        caseSearched.getCaseId(), caseSearched.getCaseData(),multipleDetails.getJurisdiction());
            }
        }
        else {

            log.info("No changes then move to open state");

            multipleData.setState(OPEN_STATE);
        }

    }
    private void submitEventForCase(String userToken, String caseTypeId, long caseId, CaseData caseData, String jurisdiction) {
       try {
           CCDRequest returnedRequest = ccdClient.startEventForCase(userToken, caseTypeId,
                   jurisdiction, String.valueOf(caseId));
           ccdClient.submitEventForCase(userToken,caseData, caseTypeId,
                   jurisdiction, returnedRequest,String.valueOf(caseId));
       }
          catch (Exception e) {
            throw new CaseCreationException("Error while submitting event for case: " + caseId + e.toString());
        }
    }
    private void removeClaimantRep(SubmitEvent caseSearched, MultipleData multipleData) {
            log.info("Claimant Rep is to be removed for case: " + caseSearched.getCaseData().getEthosCaseReference()
                    + " of multiple: " + multipleData.getMultipleReference());
            var representedTypeC = new RepresentedTypeC();
            caseSearched.getCaseData().setRepresentativeClaimantType(representedTypeC);
            caseSearched.getCaseData().setClaimantRepresentedQuestion(NO);
    }

    private void removeRespondentRep(CaseData caseData, RepresentedTypeR representedType) {

        if (caseData.getRespondentCollection() != null) {
            Optional<RespondentSumTypeItem> respondentSumTypeItemOptional =
                    caseData.getRespondentCollection().stream()
                            .filter(respondentSumTypeItem ->
                                    respondentSumTypeItem.getValue().getRespondentName()
                                            .equals(representedType.getRespRepName()))
                            .findAny();

            if (respondentSumTypeItemOptional.isPresent() && CollectionUtils.isNotEmpty(caseData.getRepCollection())) {
                List<RepresentedTypeRItem> toBeRemoved = caseData.getRepCollection().stream().filter(a-> a.getValue().getRespRepName().equals(representedType.getRespRepName())).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(toBeRemoved)) {
                    log.info("Respondent representatives to be removed are: " + toBeRemoved.size());
                    for (RepresentedTypeRItem r: toBeRemoved) {
                       caseData.getRepCollection().stream().filter(a-> a.getValue().equals(r.getValue()))
                                .findFirst().ifPresent(representedTypeRItem -> representedTypeRItem.setId(null));
                        caseData.getRepCollection().stream().filter(a-> a.getValue().equals(r.getValue()))
                                .findFirst().ifPresent(representedTypeRItem -> representedTypeRItem.setValue(null));
                    }
                    log.info("Size of rep collection:" + caseData.getRepCollection().size());
                }

            }
        }
    }


    private boolean checkAnyChange(MultipleData multipleData) {

        return (
                (multipleData.getBatchUpdateClaimantRep() != null
                        && !multipleData.getBatchUpdateClaimantRep().getValue().getCode().equals(SELECT_NONE_VALUE))
                        || (multipleData.getBatchUpdateJurisdiction() != null
                        && !multipleData.getBatchUpdateJurisdiction().getValue().getCode().equals(SELECT_NONE_VALUE))
                        || (multipleData.getBatchUpdateRespondent() != null
                        && !multipleData.getBatchUpdateRespondent().getValue().getCode().equals(SELECT_NONE_VALUE))
                        || (multipleData.getBatchUpdateJudgment() != null
                        && !multipleData.getBatchUpdateJudgment().getValue().getCode().equals(SELECT_NONE_VALUE))
                        || (multipleData.getBatchUpdateRespondentRep() != null
                        && !multipleData.getBatchUpdateRespondentRep().getValue().getCode().equals(SELECT_NONE_VALUE)
                )
        );
    }

}
