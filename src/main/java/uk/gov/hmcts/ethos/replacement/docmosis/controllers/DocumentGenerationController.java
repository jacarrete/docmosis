package uk.gov.hmcts.ethos.replacement.docmosis.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ecm.common.model.ccd.CCDCallbackResponse;
import uk.gov.hmcts.ecm.common.model.ccd.CCDRequest;
import uk.gov.hmcts.ecm.common.model.ccd.CaseData;
import uk.gov.hmcts.ecm.common.model.ccd.DocumentInfo;
import uk.gov.hmcts.ethos.replacement.docmosis.helpers.Helper;
import uk.gov.hmcts.ethos.replacement.docmosis.service.DocumentGenerationService;
import uk.gov.hmcts.ethos.replacement.docmosis.service.EventValidationService;
import uk.gov.hmcts.ethos.replacement.docmosis.service.VerifyTokenService;

import java.util.List;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.SCOTLAND_CASE_TYPE_ID;

@Slf4j
@RestController
public class DocumentGenerationController {

    private static final String LOG_MESSAGE = "received notification request for case reference :    ";

    private static final String GENERATED_DOCUMENT_URL = "Please download the document from : ";

    private final DocumentGenerationService documentGenerationService;

    private final VerifyTokenService verifyTokenService;

    private final EventValidationService eventValidationService;

    @Autowired
    public DocumentGenerationController(DocumentGenerationService documentGenerationService, VerifyTokenService verifyTokenService,
                                        EventValidationService eventValidationService) {
        this.documentGenerationService = documentGenerationService;
        this.verifyTokenService = verifyTokenService;
        this.eventValidationService = eventValidationService;
    }

    @PostMapping(value = "/midAddressLabels", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "populates the address labels list with the user selected addresses.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<CCDCallbackResponse> midAddressLabels(
            @RequestBody CCDRequest ccdRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("MID ADDRESS LABELS ---> " + LOG_MESSAGE + ccdRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error("Invalid Token {}", userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        CaseData caseData = ccdRequest.getCaseDetails().getCaseData();
        caseData = documentGenerationService.midAddressLabels(caseData);

        return ResponseEntity.ok(CCDCallbackResponse.builder()
                .data(caseData)
                .build());
    }

    @PostMapping(value = "/generateDocument", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "generate a document.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<CCDCallbackResponse> generateDocument(
            @RequestBody CCDRequest ccdRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("GENERATE LETTER ---> " + LOG_MESSAGE + ccdRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error("Invalid Token {}", userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        List<String> errors = eventValidationService.validateHearingNumber(ccdRequest.getCaseDetails().getCaseData());

        if (errors.isEmpty()) {
            DocumentInfo documentInfo = documentGenerationService.processDocumentRequest(ccdRequest, userToken);
            ccdRequest.getCaseDetails().getCaseData().setDocMarkUp(documentInfo.getMarkUp());

            if(ccdRequest.getCaseDetails().getCaseTypeId().equals(SCOTLAND_CASE_TYPE_ID)) {
                ccdRequest.getCaseDetails().getCaseData().setCorrespondenceScotType(null);
            } else {
                ccdRequest.getCaseDetails().getCaseData().setCorrespondenceType(null);
            }

            return ResponseEntity.ok(CCDCallbackResponse.builder()
                    .data(ccdRequest.getCaseDetails().getCaseData())
                    .errors(errors)
                    .significant_item(Helper.generateSignificantItem(documentInfo))
                    .build());
        }
        else {
            return ResponseEntity.ok(CCDCallbackResponse.builder()
                    .data(ccdRequest.getCaseDetails().getCaseData())
                    .errors(errors)
                    .build());
        }
    }

    @PostMapping(value = "/generateDocumentConfirmation", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "generate a document confirmation.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<CCDCallbackResponse> generateDocumentConfirmation(
            @RequestBody CCDRequest ccdRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("GENERATE LETTER CONFIRMATION ---> " + LOG_MESSAGE + ccdRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error("Invalid Token {}", userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        return ResponseEntity.ok(CCDCallbackResponse.builder()
                .data(ccdRequest.getCaseDetails().getCaseData())
                .confirmation_header(GENERATED_DOCUMENT_URL + ccdRequest.getCaseDetails().getCaseData().getDocMarkUp())
                .build());
    }
}
