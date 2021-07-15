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
import uk.gov.hmcts.ecm.common.model.ccd.*;
import uk.gov.hmcts.ecm.common.model.helper.DefaultValues;
import uk.gov.hmcts.ethos.replacement.docmosis.helpers.BFHelper;
import uk.gov.hmcts.ethos.replacement.docmosis.helpers.FlagsImageHelper;
import uk.gov.hmcts.ethos.replacement.docmosis.helpers.Helper;
import uk.gov.hmcts.ethos.replacement.docmosis.service.*;


import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.*;
import static uk.gov.hmcts.ethos.replacement.docmosis.helpers.CallbackRespHelper.*;

@Slf4j
@RequiredArgsConstructor
@RestController
public class CaseActionsForCaseWorkerController {

    private static final String LOG_MESSAGE = "received notification request for case reference :    ";
    private static final String INVALID_TOKEN = "Invalid Token {}";
    private static final String EVENT_FIELDS_VALIDATION = "Event fields validation: ";
    private final CaseTransferService caseTransferService;
    private final CaseCreationForCaseWorkerService caseCreationForCaseWorkerService;
    private final CaseRetrievalForCaseWorkerService caseRetrievalForCaseWorkerService;
    private final CaseUpdateForCaseWorkerService caseUpdateForCaseWorkerService;
    private final CaseManagementForCaseWorkerService caseManagementForCaseWorkerService;
    private final DefaultValuesReaderService defaultValuesReaderService;
    private final SingleReferenceService singleReferenceService;
    private final VerifyTokenService verifyTokenService;
    private final EventValidationService eventValidationService;
    private final SingleCaseMultipleMidEventValidationService singleCaseMultipleMidEventValidationService;
    private final AddSingleCaseToMultipleService addSingleCaseToMultipleService;

    @PostMapping(value = "/createCase", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "create a case for a caseWorker.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<CCDCallbackResponse> createCase(
            @RequestBody CCDRequest ccdRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("CREATE CASE ---> " + LOG_MESSAGE + ccdRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        var submitEvent = caseCreationForCaseWorkerService.caseCreationRequest(ccdRequest, userToken);
        log.info("Case created correctly with case Id: " + submitEvent.getCaseId());

        return getCallbackRespEntityNoErrors(ccdRequest.getCaseDetails().getCaseData());
    }

    @PostMapping(value = "/retrieveCase", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "retrieve a case for a caseWorker.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<CCDCallbackResponse> retrieveCase(
            @RequestBody CCDRequest ccdRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("RETRIEVE CASE ---> " + LOG_MESSAGE + ccdRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        var submitEvent = caseRetrievalForCaseWorkerService.caseRetrievalRequest(userToken,
                ccdRequest.getCaseDetails().getCaseTypeId(),
                ccdRequest.getCaseDetails().getJurisdiction(), "1550576532211563");
        log.info("Case received correctly with id: " + submitEvent.getCaseId());

        return getCallbackRespEntityNoErrors(ccdRequest.getCaseDetails().getCaseData());
    }

    @PostMapping(value = "/retrieveCases", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "retrieve cases for a caseWorker.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<CCDCallbackResponse> retrieveCases(
            @RequestBody CCDRequest ccdRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("RETRIEVE CASES ---> " + LOG_MESSAGE + ccdRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        List<SubmitEvent> submitEvents = caseRetrievalForCaseWorkerService.casesRetrievalRequest(ccdRequest, userToken);
        log.info("Cases received: " + submitEvents.size());
        submitEvents.forEach(submitEvent -> log.info(String.valueOf(submitEvent.getCaseId() + '\n')));

        return getCallbackRespEntityNoErrors(ccdRequest.getCaseDetails().getCaseData());
    }

    @PostMapping(value = "/updateCase", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update a case for a caseWorker.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<CCDCallbackResponse> updateCase(
            @RequestBody CCDRequest ccdRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("UPDATE CASE ---> " + LOG_MESSAGE + ccdRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        var submitEvent = caseUpdateForCaseWorkerService.caseUpdateRequest(ccdRequest, userToken);
        log.info("Case updated correctly with id: " + submitEvent.getCaseId());

        return getCallbackRespEntityNoErrors(ccdRequest.getCaseDetails().getCaseData());
    }

    @PostMapping(value = "/preDefaultValues", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update pre default values in a case.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<CCDCallbackResponse> preDefaultValues(
            @RequestBody CCDRequest ccdRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("PRE DEFAULT VALUES ---> " + LOG_MESSAGE);

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        ccdRequest.getCaseDetails().getCaseData().setClaimantTypeOfClaimant(defaultValuesReaderService.getClaimantTypeOfClaimant());

        return getCallbackRespEntityNoErrors(ccdRequest.getCaseDetails().getCaseData());
    }

    @PostMapping(value = "/postDefaultValues", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update the case with some default values after submitted.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<CCDCallbackResponse> postDefaultValues(
            @RequestBody CCDRequest ccdRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("POST DEFAULT VALUES ---> " + LOG_MESSAGE + ccdRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        var caseData = ccdRequest.getCaseDetails().getCaseData();

        List<String> errors = eventValidationService.validateReceiptDate(caseData);

        if (errors.isEmpty()) {
            var defaultValues = getPostDefaultValues(ccdRequest.getCaseDetails());
            defaultValuesReaderService.getCaseData(caseData, defaultValues);
            caseManagementForCaseWorkerService.caseDataDefaults(caseData);
            generateEthosCaseReference(caseData, ccdRequest);
            FlagsImageHelper.buildFlagsImageFileName(caseData);
            caseData.setMultipleFlag(caseData.getCaseType() != null
                    && caseData.getCaseType().equals(MULTIPLE_CASE_TYPE) ? YES : NO);
        }

        log.info("PostDefaultValues for case: " + caseData.getEthosCaseReference());

        return getCallbackRespEntityErrors(errors, caseData);
    }

    @PostMapping(value = "/amendCaseDetails", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "amend the case details for a single case and validates receipt date.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<CCDCallbackResponse> amendCaseDetails(
            @RequestBody CCDRequest ccdRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("AMEND CASE DETAILS ---> " + LOG_MESSAGE + ccdRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        var caseData = ccdRequest.getCaseDetails().getCaseData();

        List<String> errors = eventValidationService.validateReceiptDate(caseData);


        if (errors.isEmpty()) {
            boolean caseStateValidated = eventValidationService.validateCaseState(ccdRequest.getCaseDetails());
            if (!caseStateValidated) {
                errors.add(ccdRequest.getCaseDetails().getCaseData().getEthosCaseReference() + " Case has not been Accepted.");
            }
        }

        log.info("Event fields and/or case state validation for case " + ccdRequest.getCaseDetails().getCaseData().getEthosCaseReference() + ": " + errors);
        if (errors.isEmpty()) {
            var defaultValues = getPostDefaultValues(ccdRequest.getCaseDetails());
            log.info("Post Default values loaded: " + defaultValues);
            defaultValuesReaderService.getCaseData(caseData, defaultValues);
            caseManagementForCaseWorkerService.dateToCurrentPosition(caseData);
            FlagsImageHelper.buildFlagsImageFileName(caseData);

            addSingleCaseToMultipleService.addSingleCaseToMultipleLogic(
                    userToken, caseData, ccdRequest.getCaseDetails().getCaseTypeId(),
                    ccdRequest.getCaseDetails().getJurisdiction(),
                    ccdRequest.getCaseDetails().getCaseId(), errors);

        }

        return getCallbackRespEntityErrors(errors, caseData);
    }

    @PostMapping(value = "/amendClaimantDetails", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "amend the case claimant details for a single case.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<CCDCallbackResponse> amendClaimantDetails(
            @RequestBody CCDRequest ccdRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("AMEND CLAIMANT DETAILS ---> " + LOG_MESSAGE + ccdRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        var caseData = ccdRequest.getCaseDetails().getCaseData();
        caseManagementForCaseWorkerService.claimantDefaults(caseData);

        return getCallbackRespEntityNoErrors(caseData);
    }

    @PostMapping(value = "/amendRespondentDetails", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "amend respondent details for a single case.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<CCDCallbackResponse> amendRespondentDetails(
            @RequestBody CCDRequest ccdRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("AMEND RESPONDENT DETAILS ---> " + LOG_MESSAGE + ccdRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        var caseData = ccdRequest.getCaseDetails().getCaseData();
        List<String> errors = eventValidationService.validateActiveRespondents(caseData);
        if (errors.isEmpty()) {
            errors = eventValidationService.validateET3ResponseFields(caseData);
            if (errors.isEmpty()) {
                caseData = caseManagementForCaseWorkerService.struckOutRespondents(ccdRequest);
            }
        }

        log.info(EVENT_FIELDS_VALIDATION + errors);

        return getCallbackRespEntityErrors(errors, caseData);
    }

    @PostMapping(value = "/amendRespondentRepresentative", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "amend respondent representative for a single case.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<CCDCallbackResponse> amendRespondentRepresentative(
            @RequestBody CCDRequest ccdRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("AMEND RESPONDENT REPRESENTATIVE ---> " + LOG_MESSAGE + ccdRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        var caseData = ccdRequest.getCaseDetails().getCaseData();
        List<String> errors = eventValidationService.validateRespRepNames(caseData);

        log.info(EVENT_FIELDS_VALIDATION + errors);

        return getCallbackRespEntityErrors(errors, caseData);
    }

    @PostMapping(value = "/updateHearing", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update hearing details for a single case.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<CCDCallbackResponse> updateHearing(
            @RequestBody CCDRequest ccdRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("UPDATE HEARING ---> " + LOG_MESSAGE + ccdRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        var caseDetails = ccdRequest.getCaseDetails();
        FlagsImageHelper.buildFlagsImageFileName(caseDetails.getCaseData());

        return getCallbackRespEntityNoErrors(caseDetails.getCaseData());
    }

    @PostMapping(value = "/allocateHearing", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update postponed date when allocating a hearing.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<CCDCallbackResponse> allocateHearing(
            @RequestBody CCDRequest ccdRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("ALLOCATE HEARING ---> " + LOG_MESSAGE + ccdRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        var caseData = ccdRequest.getCaseDetails().getCaseData();
        Helper.updatePostponedDate(caseData);

        return getCallbackRespEntityNoErrors(caseData);
    }

    @PostMapping(value = "/restrictedCases", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "change restricted reporting for a single case")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<CCDCallbackResponse> restrictedCases(
            @RequestBody CCDRequest ccdRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("RESTRICTED CASES ---> " + LOG_MESSAGE + ccdRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        var caseData = ccdRequest.getCaseDetails().getCaseData();
        FlagsImageHelper.buildFlagsImageFileName(caseData);

        return getCallbackRespEntityNoErrors(caseData);
    }

    @PostMapping(value = "/amendHearing", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "amend hearing details for a single case.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<CCDCallbackResponse> amendHearing(
            @RequestBody CCDRequest ccdRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("AMEND HEARING ---> " + LOG_MESSAGE + ccdRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        var caseData = ccdRequest.getCaseDetails().getCaseData();
        caseManagementForCaseWorkerService.amendHearing(caseData, ccdRequest.getCaseDetails().getCaseTypeId());

        return getCallbackRespEntityNoErrors(caseData);
    }

    @PostMapping(value = "/amendCaseState", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "amend the case state for a single case.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<CCDCallbackResponse> amendCaseState(
            @RequestBody CCDRequest ccdRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("AMEND CASE STATE ---> " + LOG_MESSAGE + ccdRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        List<String> errors = new ArrayList<>();
        var caseData = ccdRequest.getCaseDetails().getCaseData();

        if (ccdRequest.getCaseDetails().getState().equals(CLOSED_STATE)) {
            errors = eventValidationService.validateJurisdictionOutcome(caseData);
            log.info(EVENT_FIELDS_VALIDATION + errors);
        }

        return getCallbackRespEntityErrors(errors, caseData);
    }

    @PostMapping(value = "/midRespondentAddress", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "populates the mid dynamic fixed list with the respondent addresses.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<CCDCallbackResponse> midRespondentAddress(
            @RequestBody CCDRequest ccdRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("MID RESPONDENT ADDRESS ---> " + LOG_MESSAGE + ccdRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        var caseData = Helper.midRespondentAddress(ccdRequest.getCaseDetails().getCaseData());

        return getCallbackRespEntityNoErrors(caseData);
    }

    @PostMapping(value = "/jurisdictionValidation", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "validates jurisdiction entries to prevent duplicates.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<CCDCallbackResponse> jurisdictionValidation(
            @RequestBody CCDRequest ccdRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("JURISDICTION VALIDATION ---> " + LOG_MESSAGE + ccdRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        List<String> errors = new ArrayList<>();
        var caseData =  ccdRequest.getCaseDetails().getCaseData();
        eventValidationService.validateJurisdictionCodes(caseData, errors);
        log.info(EVENT_FIELDS_VALIDATION + errors);

        return getCallbackRespEntityErrors(errors, caseData);
    }

    @PostMapping(value = "/generateCaseRefNumbers", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "generates ethos case numbers according to caseRefNumberCount field.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<CCDCallbackResponse> generateCaseRefNumbers(
            @RequestBody CCDRequest ccdRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("GENERATE CASE REF NUMBERS ---> " + LOG_MESSAGE + ccdRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        var caseData = caseCreationForCaseWorkerService.generateCaseRefNumbers(ccdRequest);

        return getCallbackRespEntityNoErrors(caseData);
    }

    @PostMapping(value = "/midRespondentECC", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "populates the mid dynamic list with the respondent names.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<CCDCallbackResponse> midRespondentECC(
            @RequestBody CCDRequest ccdRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("MID RESPONDENT ECC ---> " + LOG_MESSAGE + ccdRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }
        List<String> errors = new ArrayList<>();
        var caseData = caseManagementForCaseWorkerService.createECC(ccdRequest.getCaseDetails(),
                userToken, errors, MID_EVENT_CALLBACK);

        return getCallbackRespEntityErrors(errors, caseData);
    }

    @PostMapping(value = "/createECC", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "create a new Employer Contract Claim.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<CCDCallbackResponse> createECC(
            @RequestBody CCDRequest ccdRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("CREATE ECC ---> " + LOG_MESSAGE + ccdRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        List<String> errors = new ArrayList<>();
        var caseData = caseManagementForCaseWorkerService.createECC(
                ccdRequest.getCaseDetails(), userToken, errors, ABOUT_TO_SUBMIT_EVENT_CALLBACK);
        generateEthosCaseReference(caseData, ccdRequest);

        return getCallbackRespEntityErrors(errors, caseData);
    }

    @PostMapping(value = "/linkOriginalCaseECC", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "send an update to the original case with the new ECC reference created to link it.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<CCDCallbackResponse> linkOriginalCaseECC(
            @RequestBody CCDRequest ccdRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("LINK ORIGINAL CASE ECC ---> " + LOG_MESSAGE + ccdRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }
        List<String> errors = new ArrayList<>();
        var caseData = caseManagementForCaseWorkerService.createECC(ccdRequest.getCaseDetails(),
                userToken, errors, SUBMITTED_CALLBACK);

        return getCallbackRespEntityErrors(errors, caseData);
    }

    @PostMapping(value = "/singleCaseMultipleMidEventValidation", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "validates the multiple and sub multiple in the single case when moving to a multiple.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<CCDCallbackResponse> singleCaseMultipleMidEventValidation(
            @RequestBody CCDRequest ccdRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("SINGLE CASE MULTIPLE MID EVENT VALIDATION ---> " + LOG_MESSAGE
                + ccdRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        List<String> errors = new ArrayList<>();
        var caseDetails = ccdRequest.getCaseDetails();

        singleCaseMultipleMidEventValidationService.singleCaseMultipleValidationLogic(
                userToken, caseDetails, errors);

        return getCallbackRespEntity(errors, caseDetails);
    }

    @PostMapping(value = "/hearingMidEventValidation", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "validates the hearing number and the hearing days to prevent their creation.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<CCDCallbackResponse> hearingMidEventValidation(
            @RequestBody CCDRequest ccdRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("HEARING MID EVENT VALIDATION ---> " + LOG_MESSAGE + ccdRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        var caseDetails = ccdRequest.getCaseDetails();
        List<String> errors = Helper.hearingMidEventValidation(caseDetails.getCaseData());

        return getCallbackRespEntity(errors, caseDetails);
    }

    @PostMapping(value = "/dynamicListBfActions", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "populate bf actions in dynamic lists.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<CCDCallbackResponse> dynamicListBfActions(
            @RequestBody CCDRequest ccdRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("DYNAMIC LIST BF ACTIONS ---> " + LOG_MESSAGE + ccdRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        var caseData = ccdRequest.getCaseDetails().getCaseData();
        BFHelper.populateDynamicListBfActions(caseData);

        return getCallbackRespEntityNoErrors(caseData);
    }

    @PostMapping(value = "/bfActions", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "updates the dateEntered by the user with the current date.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<CCDCallbackResponse> bfActions(
            @RequestBody CCDRequest ccdRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("BF ACTIONS ---> " + LOG_MESSAGE + ccdRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        var caseData = ccdRequest.getCaseDetails().getCaseData();
        BFHelper.updateBfActionItems(caseData);

        return getCallbackRespEntityNoErrors(caseData);
    }

    @PostMapping(value = "/judgmentValidation", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "validates jurisdiction codes within judgement collection.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<CCDCallbackResponse> judgmentValidation(
            @RequestBody CCDRequest ccdRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("JUDGEMENT VALIDATION ---> " + LOG_MESSAGE + ccdRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        var caseData =  ccdRequest.getCaseDetails().getCaseData();
        List<String> errors = eventValidationService.validateJurisdictionCodesWithinJudgement(caseData);

        return getCallbackRespEntityErrors(errors, caseData);
    }

    @PostMapping(value = "/depositValidation", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "validates deposit amount and deposit refunded.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<CCDCallbackResponse> depositValidation(
            @RequestBody CCDRequest ccdRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("DEPOSIT VALIDATION ---> " + LOG_MESSAGE + ccdRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        var caseData =  ccdRequest.getCaseDetails().getCaseData();
        List<String> errors = eventValidationService.validateDepositRefunded(caseData);

        return getCallbackRespEntityErrors(errors, caseData);
    }

    @PostMapping(value = "/dynamicListOffices", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "populates all offices except the current one in dynamic lists.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<CCDCallbackResponse> dynamicListOffices(
            @RequestBody CCDRequest ccdRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("DYNAMIC LIST OFFICES ---> " + LOG_MESSAGE + ccdRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        var caseData = ccdRequest.getCaseDetails().getCaseData();
        Helper.populateDynamicListOffices(caseData, ccdRequest.getCaseDetails().getCaseTypeId());

        return getCallbackRespEntityNoErrors(caseData);
    }

    @PostMapping(value = "/createCaseTransfer", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "create a new Case in a different office.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<CCDCallbackResponse> createCaseTransfer(
            @RequestBody CCDRequest ccdRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("CREATE CASE TRANSFER ---> " + LOG_MESSAGE + ccdRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        List<String> errors = new ArrayList<>();
        caseTransferService.createCaseTransfer(ccdRequest.getCaseDetails(), errors, userToken);

        return getCallbackRespEntityErrors(errors, ccdRequest.getCaseDetails().getCaseData());
    }

    @PostMapping(value = "/aboutToStartDisposal", consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update the position type to case closed.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Accessed successfully",
                    response = CCDCallbackResponse.class),
            @ApiResponse(code = 400, message = "Bad Request"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    public ResponseEntity<CCDCallbackResponse> aboutToStartDisposal(
            @RequestBody CCDRequest ccdRequest,
            @RequestHeader(value = "Authorization") String userToken) {
        log.info("ABOUT TO START DISPOSAL ---> " + LOG_MESSAGE + ccdRequest.getCaseDetails().getCaseId());

        if (!verifyTokenService.verifyTokenSignature(userToken)) {
            log.error(INVALID_TOKEN, userToken);
            return ResponseEntity.status(FORBIDDEN.value()).build();
        }

        var caseData = ccdRequest.getCaseDetails().getCaseData();
        Helper.updatePositionTypeToClosed(caseData);

        return getCallbackRespEntityNoErrors(caseData);
    }

    private DefaultValues getPostDefaultValues(CaseDetails caseDetails) {
        String caseTypeId = caseDetails.getCaseTypeId();
        String managingOffice = caseDetails.getCaseData().getManagingOffice() != null
                ? caseDetails.getCaseData().getManagingOffice() : "";

        return defaultValuesReaderService.getDefaultValues(managingOffice, caseTypeId);
    }

    private void generateEthosCaseReference(CaseData caseData, CCDRequest ccdRequest) {
        if (caseData.getEthosCaseReference() == null || caseData.getEthosCaseReference().trim().equals("")) {
            log.info("Case Type Id: " + ccdRequest.getCaseDetails().getCaseTypeId());
            String reference = singleReferenceService.createReference(
                    ccdRequest.getCaseDetails().getCaseTypeId(), 1);
            log.info("Reference generated: " + reference);
            caseData.setEthosCaseReference(reference);
        }
    }

}
