package uk.gov.hmcts.ethos.replacement.docmosis.helpers;

import com.sun.istack.NotNull;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ecm.common.model.ccd.CCDCallbackResponse;
import uk.gov.hmcts.ecm.common.model.ccd.CaseData;
import uk.gov.hmcts.ecm.common.model.ccd.CaseDetails;
import uk.gov.hmcts.ecm.common.model.ccd.DocumentInfo;
import uk.gov.hmcts.ecm.common.model.listing.ListingCallbackResponse;
import uk.gov.hmcts.ecm.common.model.listing.ListingData;
import uk.gov.hmcts.ecm.common.model.multiples.MultipleCallbackResponse;
import uk.gov.hmcts.ecm.common.model.multiples.MultipleDetails;

import java.util.List;

public class CallbackRespHelper {

    private CallbackRespHelper() {
    }

    @NotNull
    public static ResponseEntity<CCDCallbackResponse> getCallbackRespEntity(
            List<String> errors, CaseDetails caseDetails) {
        return ResponseEntity.ok(CCDCallbackResponse.builder()
                .errors(errors)
                .data(caseDetails.getCaseData())
                .build());
    }

    @NotNull
    public static ResponseEntity<CCDCallbackResponse> getCallbackRespEntityNoErrors(
            CaseData caseData) {

        return ResponseEntity.ok(CCDCallbackResponse.builder()
                .data(caseData)
                .build());
    }

    @NotNull
    public static ResponseEntity<CCDCallbackResponse> getCallbackRespEntityErrors(
            List<String> errors, CaseData caseData) {

        return ResponseEntity.ok(CCDCallbackResponse.builder()
                .data(caseData)
                .errors(errors)
                .build());
    }

    @NotNull
    public static ResponseEntity<ListingCallbackResponse> getListingCallbackRespEntityErrors(
            List<String> errors, ListingData listingData) {

        return ResponseEntity.ok(ListingCallbackResponse.builder()
                .data(listingData)
                .errors(errors)
                .build());
    }

    @NotNull
    public static ResponseEntity<ListingCallbackResponse> getListingCallbackRespEntity(
            List<String> errors, ListingData listingData) {

        return ResponseEntity.ok(ListingCallbackResponse.builder()
                .data(listingData)
                .errors(errors)
                .build());
    }

    @NotNull
    public static ResponseEntity<MultipleCallbackResponse> getMultipleCallbackRespEntity(
            List<String> errors, MultipleDetails multipleDetails) {

        return ResponseEntity.ok(MultipleCallbackResponse.builder()
                .errors(errors)
                .data(multipleDetails.getCaseData())
                .build());
    }

    @NotNull
    public static ResponseEntity<MultipleCallbackResponse> getMultipleCallbackRespEntityDocInfo(
            List<String> errors, MultipleDetails multipleDetails, DocumentInfo documentInfo) {
        if (errors.isEmpty()) {

            multipleDetails.getCaseData().setDocMarkUp(documentInfo.getMarkUp());

            return ResponseEntity.ok(MultipleCallbackResponse.builder()
                    .data(multipleDetails.getCaseData())
                    .significant_item(Helper.generateSignificantItem(documentInfo, errors))
                    .build());

        } else {

            return ResponseEntity.ok(MultipleCallbackResponse.builder()
                    .errors(errors)
                    .data(multipleDetails.getCaseData())
                    .build());

        }
    }

}
