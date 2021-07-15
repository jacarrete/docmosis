package uk.gov.hmcts.ethos.replacement.docmosis.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ecm.common.model.bulk.BulkCallbackResponse;
import uk.gov.hmcts.ecm.common.model.bulk.BulkRequest;
import uk.gov.hmcts.ecm.common.model.ccd.CCDCallbackResponse;
import uk.gov.hmcts.ecm.common.model.ccd.DocumentInfo;
import uk.gov.hmcts.ecm.common.model.ccd.SubmitEvent;
import uk.gov.hmcts.ethos.replacement.docmosis.helpers.Helper;
import uk.gov.hmcts.ethos.replacement.docmosis.service.BulkCreationService;
import uk.gov.hmcts.ethos.replacement.docmosis.service.BulkSearchService;
import uk.gov.hmcts.ethos.replacement.docmosis.service.BulkUpdateService;
import uk.gov.hmcts.ethos.replacement.docmosis.service.DocumentGenerationService;
import uk.gov.hmcts.ethos.replacement.docmosis.service.SubMultipleService;
import uk.gov.hmcts.ethos.replacement.docmosis.service.VerifyTokenService;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.ET1_ONLINE_CASE_SOURCE;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.MANUALLY_CREATED_POSITION;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.SELECT_ALL_VALUE;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.SELECT_NONE_VALUE;
import static uk.gov.hmcts.ethos.replacement.docmosis.service.BulkCreationService.BULK_CREATION_STEP;
import static uk.gov.hmcts.ethos.replacement.docmosis.service.BulkCreationService.UPDATE_SINGLES_STEP;

@Slf4j
@RequiredArgsConstructor
@RestController
public class BulkActionsController {

    private static final String LOG_MESSAGE = "received notification request for bulk reference :    ";
    private static final String GENERATED_DOCUMENTS_URL = "Please download the documents from : ";
    private static final String INVALID_TOKEN = "Invalid Token {}";

    private final BulkCreationService bulkCreationService;
    private final BulkUpdateService bulkUpdateService;
    private final BulkSearchService bulkSearchService;
    private final DocumentGenerationService documentGenerationService;
    private final SubMultipleService subMultipleService;
    private final VerifyTokenService verifyTokenService;

    @PostMapping(value = "/createBulk", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "creates a bulk case. Retrieves cases by ethos case reference.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })

     public ResponseEntity<BulkCallbackResponse> createBulk(
            @RequestBody BulkRequest bulkRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("CREATE BULK ---> " + LOG_MESSAGE + bulkRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        var bulkCasesPayload = bulkSearchService.bulkCasesRetrievalRequest(
                bulkRequest.getCaseDetails(), userToken, true);

        var bulkRequestPayload = bulkCreationService.bulkCreationLogic(
                bulkRequest.getCaseDetails(), bulkCasesPayload, userToken, BULK_CREATION_STEP);

        return ResponseEntity.ok(BulkCallbackResponse.builder()
                .errors(bulkRequestPayload.getErrors())
                .data(bulkRequestPayload.getBulkDetails().getCaseData())
                .build());
    }

    @PostMapping(value = "/createBulkES", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "creates a bulk case. Retrieves cases by ethos case reference.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<BulkCallbackResponse> createBulkES(
            @RequestBody BulkRequest bulkRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("CREATE BULKES ---> " + LOG_MESSAGE + bulkRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        if (bulkRequest.getCaseDetails().getCaseData().getMultipleSource() == null
                || bulkRequest.getCaseDetails().getCaseData().getMultipleSource().trim().equals("")) {
            bulkRequest.getCaseDetails().getCaseData().setMultipleSource(MANUALLY_CREATED_POSITION);
        }

        var bulkCasesPayload = bulkSearchService.bulkCasesRetrievalRequestElasticSearch(
                bulkRequest.getCaseDetails(), userToken, true, true);

        var bulkRequestPayload = bulkCreationService.bulkCreationLogic(
                bulkRequest.getCaseDetails(), bulkCasesPayload, userToken, BULK_CREATION_STEP);

        return ResponseEntity.ok(BulkCallbackResponse.builder()
                .errors(bulkRequestPayload.getErrors())
                .data(bulkRequestPayload.getBulkDetails().getCaseData())
                .build());
    }

    @PostMapping(value = "/afterSubmittedBulk", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "display the bulk info.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<BulkCallbackResponse> afterSubmittedBulk(
            @RequestBody BulkRequest bulkRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("AFTER SUBMITTED BULK ---> " + LOG_MESSAGE + bulkRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        if (bulkRequest.getCaseDetails().getCaseData().getMultipleSource() != null
                && !bulkRequest.getCaseDetails().getCaseData().getMultipleSource().equals(ET1_ONLINE_CASE_SOURCE)) {
            var bulkCasesPayload = bulkSearchService.bulkCasesRetrievalRequestElasticSearch(
                    bulkRequest.getCaseDetails(), userToken, true, false);
            bulkCreationService.bulkCreationLogic(bulkRequest.getCaseDetails(), bulkCasesPayload, userToken,
                    UPDATE_SINGLES_STEP);
        }

        return ResponseEntity.ok(BulkCallbackResponse.builder()
                .data(bulkRequest.getCaseDetails().getCaseData())
                .confirmation_header("Updates are being processed...")
                .build());
    }

    @PostMapping(value = "/updateBulk", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "updates cases in a bulk case. Update cases in searchCollection by given fields.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<BulkCallbackResponse> updateBulk(
            @RequestBody BulkRequest bulkRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("UPDATE BULK ---> " + LOG_MESSAGE + bulkRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        var bulkRequestPayload = bulkUpdateService.bulkUpdateLogic(bulkRequest.getCaseDetails(),
                userToken);

        bulkRequestPayload = bulkUpdateService.clearUpFields(bulkRequestPayload);

        return ResponseEntity.ok(BulkCallbackResponse.builder()
                .errors(bulkRequestPayload.getErrors())
                .data(bulkRequestPayload.getBulkDetails().getCaseData())
                .build());
    }

    @PostMapping(value = "/updateBulkCase", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update a bulk case. Update the multiple collection.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<BulkCallbackResponse> updateBulkCase(
            @RequestBody BulkRequest bulkRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("UPDATE BULK CASE IDS ---> " + LOG_MESSAGE + bulkRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        var bulkRequestPayload = bulkCreationService.bulkUpdateCaseIdsLogic(bulkRequest, userToken,
                false);

        return ResponseEntity.ok(BulkCallbackResponse.builder()
                .errors(bulkRequestPayload.getErrors())
                .data(bulkRequestPayload.getBulkDetails().getCaseData())
                .build());
    }

    @PostMapping(value = "/generateBulkLetter", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "generate a bulk of letters.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<BulkCallbackResponse> generateBulkLetter(
            @RequestBody BulkRequest bulkRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("GENERATE BULK LETTER ---> " + LOG_MESSAGE + bulkRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        var bulkDocumentInfo = documentGenerationService.processBulkDocumentRequest(bulkRequest,
                userToken);
        bulkRequest.getCaseDetails().getCaseData().setDocMarkUp(bulkDocumentInfo.getMarkUps());
        documentGenerationService.clearUserChoicesForMultiples(bulkRequest.getCaseDetails());

        return ResponseEntity.ok(BulkCallbackResponse.builder()
                .errors(bulkDocumentInfo.getErrors())
                .data(bulkRequest.getCaseDetails().getCaseData())
                .build());
    }

    @PostMapping(value = "/generateBulkLetterConfirmation", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "generate a bulk letter confirmation.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<BulkCallbackResponse> generateBulkLetterConfirmation(
            @RequestBody BulkRequest bulkRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("GENERATE BULK LETTER CONFIRMATION ---> " + LOG_MESSAGE + bulkRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        return ResponseEntity.ok(BulkCallbackResponse.builder()
                .data(bulkRequest.getCaseDetails().getCaseData())
                .confirmation_header(GENERATED_DOCUMENTS_URL
                        + bulkRequest.getCaseDetails().getCaseData().getDocMarkUp())
                .build());
    }

    @PostMapping(value = "/midSearchBulk", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "searches cases in a bulk case. Look for cases in multipleCollection by fields. "
            + "Mid event callback.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<BulkCallbackResponse> midSearchBulk(
            @RequestBody BulkRequest bulkRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("MID SEARCH BULK ---> " + LOG_MESSAGE + bulkRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        var bulkRequestPayload = bulkSearchService.bulkMidSearchLogic(
                bulkRequest.getCaseDetails(), false);

        return ResponseEntity.ok(BulkCallbackResponse.builder()
                .errors(bulkRequestPayload.getErrors())
                .data(bulkRequestPayload.getBulkDetails().getCaseData())
                .build());
    }

    @PostMapping(value = "/searchBulk", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "searches cases in a bulk case. Look for cases in multipleCollection by fields.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<BulkCallbackResponse> searchBulk(
            @RequestBody BulkRequest bulkRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("SEARCH BULK ---> " + LOG_MESSAGE + bulkRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        var bulkRequestPayload = bulkSearchService.bulkSearchLogic(bulkRequest.getCaseDetails());

        return ResponseEntity.ok(BulkCallbackResponse.builder()
                .errors(bulkRequestPayload.getErrors())
                .data(bulkRequestPayload.getBulkDetails().getCaseData())
                .build());
    }

    @PostMapping(value = "/midCreateSubMultiple", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "create a sub multiple looking for cases in multipleCollection. Mid event callback.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<BulkCallbackResponse> midCreateSubMultiple(
            @RequestBody BulkRequest bulkRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("MID CREATE SUB MULTIPLE ---> " + LOG_MESSAGE + bulkRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        var bulkRequestPayload = bulkSearchService.bulkMidSearchLogic(
                bulkRequest.getCaseDetails(), true);

        return ResponseEntity.ok(BulkCallbackResponse.builder()
                .errors(bulkRequestPayload.getErrors())
                .data(bulkRequestPayload.getBulkDetails().getCaseData())
                .build());
    }

    @PostMapping(value = "/createSubMultiple", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "create a sub multiple looking for cases in multipleCollection.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<BulkCallbackResponse> createSubMultiple(
            @RequestBody BulkRequest bulkRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("CREATE SUB MULTIPLE ---> " + LOG_MESSAGE + bulkRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        var bulkRequestPayload = subMultipleService.createSubMultipleLogic(
                bulkRequest.getCaseDetails());

        return ResponseEntity.ok(BulkCallbackResponse.builder()
                .errors(bulkRequestPayload.getErrors())
                .data(bulkRequestPayload.getBulkDetails().getCaseData())
                .build());
    }

    @PostMapping(value = "/subMultipleDynamicList", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "populate a dynamic list with all sub multiple names.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<BulkCallbackResponse> subMultipleDynamicList(
            @RequestBody BulkRequest bulkRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("SUB MULTIPLE DYNAMIC LIST ---> " + LOG_MESSAGE + bulkRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        var bulkRequestPayload = subMultipleService.populateSubMultipleDynamicListLogic(
                bulkRequest.getCaseDetails());

        return ResponseEntity.ok(BulkCallbackResponse.builder()
                .errors(bulkRequestPayload.getErrors())
                .data(bulkRequestPayload.getBulkDetails().getCaseData())
                .build());
    }

    @PostMapping(value = "/filterDefaultedAllDynamicList", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "populate a dynamic list with all sub multiple names and Select All as default.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<BulkCallbackResponse> filterDefaultedAllDynamicList(
            @RequestBody BulkRequest bulkRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("FILTER DEFAULTED ALL DYNAMIC LIST ---> " + LOG_MESSAGE + bulkRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        var bulkRequestPayload = subMultipleService.populateFilterDefaultedDynamicListLogic(
                bulkRequest.getCaseDetails(), SELECT_ALL_VALUE);

        return ResponseEntity.ok(BulkCallbackResponse.builder()
                .errors(bulkRequestPayload.getErrors())
                .data(bulkRequestPayload.getBulkDetails().getCaseData())
                .build());
    }

    @PostMapping(value = "/filterDefaultedNoneDynamicList", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "populate a dynamic list with all sub multiple names and jurCodes and None as default.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<BulkCallbackResponse> filterDefaultedNoneDynamicList(
            @RequestBody BulkRequest bulkRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("FILTER DEFAULTED NONE DYNAMIC LIST ---> " + LOG_MESSAGE + bulkRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        var bulkRequestPayload = subMultipleService.populateFilterDefaultedDynamicListLogic(
                bulkRequest.getCaseDetails(), SELECT_NONE_VALUE);

        return ResponseEntity.ok(BulkCallbackResponse.builder()
                .errors(bulkRequestPayload.getErrors())
                .data(bulkRequestPayload.getBulkDetails().getCaseData())
                .build());
    }

    @PostMapping(value = "/midUpdateSubMultiple", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "populates Sub multiple name and search collection for edit.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<BulkCallbackResponse> midUpdateSubMultiple(
            @RequestBody BulkRequest bulkRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("MID UPDATE SUB MULTIPLE ---> " + LOG_MESSAGE + bulkRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        var bulkRequestPayload = subMultipleService.bulkMidUpdateLogic(bulkRequest.getCaseDetails());

        return ResponseEntity.ok(BulkCallbackResponse.builder()
                .errors(bulkRequestPayload.getErrors())
                .data(bulkRequestPayload.getBulkDetails().getCaseData())
                .build());
    }

    @PostMapping(value = "/updateSubMultiple1", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update the name and the list of cases of a sub multiple.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<BulkCallbackResponse> updateSubMultiple1(
            @RequestBody BulkRequest bulkRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("UPDATE SUB MULTIPLE ---> " + LOG_MESSAGE + bulkRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        var bulkRequestPayload = subMultipleService.updateSubMultipleLogic(bulkRequest.getCaseDetails());

        return ResponseEntity.ok(BulkCallbackResponse.builder()
                .errors(bulkRequestPayload.getErrors())
                .data(bulkRequestPayload.getBulkDetails().getCaseData())
                .build());
    }

    @PostMapping(value = "/deleteSubMultiple", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "delete a Sub multiple from a dynamic list.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<BulkCallbackResponse> deleteSubMultiple(
            @RequestBody BulkRequest bulkRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("DELETE SUB MULTIPLE ---> " + LOG_MESSAGE + bulkRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        var bulkRequestPayload = subMultipleService.deleteSubMultipleLogic(bulkRequest.getCaseDetails());

        return ResponseEntity.ok(BulkCallbackResponse.builder()
                .errors(bulkRequestPayload.getErrors())
                .data(bulkRequestPayload.getBulkDetails().getCaseData())
                .build());
    }

    @PostMapping(value = "/generateBulkSchedule", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "generate a multiple schedule.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<BulkCallbackResponse> generateBulkSchedule(
            @RequestBody BulkRequest bulkRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("GENERATE BULK SCHEDULE ---> " + LOG_MESSAGE + bulkRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        var bulkDocumentInfo = documentGenerationService.processBulkScheduleRequest(
                bulkRequest, userToken);

        if (bulkDocumentInfo.getErrors().isEmpty()) {
            bulkRequest.getCaseDetails().getCaseData().setDocMarkUp(bulkDocumentInfo.getMarkUps());
            return ResponseEntity.ok(BulkCallbackResponse.builder()
                    .data(bulkRequest.getCaseDetails().getCaseData())
                    .significant_item(Helper.generateSignificantItem(bulkDocumentInfo.getDocumentInfo() != null
                            ? bulkDocumentInfo.getDocumentInfo() : new DocumentInfo(), new ArrayList<>()))
                    .build());
        } else {
            return ResponseEntity.ok(BulkCallbackResponse.builder()
                    .errors(bulkDocumentInfo.getErrors())
                    .data(bulkRequest.getCaseDetails().getCaseData())
                    .build());
        }
    }

    @PostMapping(value = "/generateBulkScheduleConfirmation", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "generate a multiple schedule confirmation.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<BulkCallbackResponse> generateBulkScheduleConfirmation(
            @RequestBody BulkRequest bulkRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("GENERATE BULK SCHEDULE CONFIRMATION ---> " + LOG_MESSAGE + bulkRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        return ResponseEntity.ok(BulkCallbackResponse.builder()
                .data(bulkRequest.getCaseDetails().getCaseData())
                .confirmation_header(
                        GENERATED_DOCUMENTS_URL + bulkRequest.getCaseDetails().getCaseData().getDocMarkUp())
                .build());
    }

    @PostMapping(value = "/preAcceptBulk", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "accept a bulk of cases.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<BulkCallbackResponse> preAcceptBulk(
            @RequestBody BulkRequest bulkRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("PRE ACCEPT BULK ---> " + LOG_MESSAGE + bulkRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        List<SubmitEvent> submitEvents =
                bulkSearchService.retrievalCasesForPreAcceptRequest(bulkRequest.getCaseDetails(), userToken);

        var bulkRequestPayload = bulkUpdateService.bulkPreAcceptLogic(bulkRequest.getCaseDetails(),
                submitEvents, userToken, false);

        return ResponseEntity.ok(BulkCallbackResponse.builder()
                .errors(bulkRequestPayload.getErrors())
                .data(bulkRequestPayload.getBulkDetails().getCaseData())
                .build());
    }

}

