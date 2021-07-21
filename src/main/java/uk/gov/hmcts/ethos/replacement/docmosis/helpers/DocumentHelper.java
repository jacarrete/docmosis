package uk.gov.hmcts.ethos.replacement.docmosis.helpers;

import com.google.common.base.Strings;
import static com.google.common.base.Strings.isNullOrEmpty;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ecm.common.helpers.UtilHelper;
import uk.gov.hmcts.ecm.common.idam.models.UserDetails;
import uk.gov.hmcts.ecm.common.model.ccd.Address;
import uk.gov.hmcts.ecm.common.model.ccd.CaseData;
import uk.gov.hmcts.ecm.common.model.ccd.items.*;
import uk.gov.hmcts.ecm.common.model.ccd.types.*;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.*;
import uk.gov.hmcts.ecm.common.model.helper.DefaultValues;
import uk.gov.hmcts.ecm.common.model.multiples.MultipleData;
import static uk.gov.hmcts.ethos.replacement.docmosis.helpers.Helper.nullCheck;

@Slf4j
public class DocumentHelper {

    private DocumentHelper() {
    }

    private static final String VENUE_ADDRESS_OPENING_PROCESSING_ERROR = "Failed while opening or "
            + "processing the entries for the venueAddressValues.xlsx file : ---> ";

    public static StringBuilder buildDocumentContent(CaseData caseData, String accessKey,
                                                     UserDetails userDetails, String caseTypeId,
                                                     InputStream venueAddressInputStream,
                                                     CorrespondenceType correspondenceType,
                                                     CorrespondenceScotType correspondenceScotType,
                                                     MultipleData multipleData,
                                                     DefaultValues allocatedCourtAddress) {
        var sb = new StringBuilder();
        String templateName = getTemplateName(correspondenceType, correspondenceScotType);

        // Start building the instruction
        sb.append("{\n");
        sb.append("\"accessKey\":\"").append(accessKey).append(NEW_LINE);
        sb.append("\"templateName\":\"").append(templateName).append(FILE_EXTENSION).append(NEW_LINE);
        sb.append("\"outputName\":\"").append(OUTPUT_FILE_NAME).append(NEW_LINE);

        // Building the document data
        sb.append("\"data\":{\n");

        if (templateName.equals(ADDRESS_LABELS_TEMPLATE) && multipleData == null) {
            log.info("Labels template");
            sb.append(getAddressLabelsDataSingleCase(caseData));
        } else if (templateName.equals(ADDRESS_LABELS_TEMPLATE)) {
            log.info("Multiple template");
            sb.append(getAddressLabelsDataMultipleCase(multipleData));
        } else {
            log.info("Single template");
            sb.append(getClaimantData(caseData));
            sb.append(getRespondentData(caseData));
            sb.append(getHearingData(caseData, caseTypeId, venueAddressInputStream, correspondenceType,
                    correspondenceScotType));
            sb.append(getCorrespondenceData(correspondenceType));
            sb.append(getCorrespondenceScotData(correspondenceScotType));
            sb.append(getCourtData(caseData, allocatedCourtAddress));
        }

        log.info("Check template names");
        sb.append("\"i").append(getEWSectionName(correspondenceType)
                .replace(".", "_"))
                .append("_enhmcts\":\"").append("[userImage:").append("enhmcts.png]").append(NEW_LINE);
        sb.append("\"i").append(getEWSectionName(correspondenceType)
                .replace(".", "_"))
                .append("_enhmcts1\":\"").append("[userImage:").append("enhmcts.png]").append(NEW_LINE);
        sb.append("\"i").append(getEWSectionName(correspondenceType)
                .replace(".", "_"))
                .append("_enhmcts2\":\"").append("[userImage:").append("enhmcts.png]").append(NEW_LINE);
        sb.append("\"iScot").append(getScotSectionName(correspondenceScotType)
                .replace(".", "_"))
                .append("_schmcts\":\"").append("[userImage:").append("schmcts.png]").append(NEW_LINE);
        sb.append("\"iScot").append(getScotSectionName(correspondenceScotType)
                .replace(".", "_"))
                .append("_schmcts1\":\"").append("[userImage:").append("schmcts.png]").append(NEW_LINE);
        sb.append("\"iScot").append(getScotSectionName(correspondenceScotType)
                .replace(".", "_"))
                .append("_schmcts2\":\"").append("[userImage:").append("schmcts.png]").append(NEW_LINE);

        log.info("Clerk info");
        String userName = nullCheck(userDetails.getFirstName() + " " + userDetails.getLastName());
        sb.append("\"Clerk\":\"").append(nullCheck(userName)).append(NEW_LINE);
        sb.append("\"Today_date\":\"").append(UtilHelper.formatCurrentDate(LocalDate.now())).append(NEW_LINE);
        sb.append("\"TodayPlus28Days\":\"").append(UtilHelper.formatCurrentDatePlusDays(LocalDate.now(), 28))
                .append(NEW_LINE);
        sb.append("\"Case_No\":\"").append(nullCheck(caseData.getEthosCaseReference())).append(NEW_LINE);
        log.info("End document");
        sb.append("}\n");
        sb.append("}\n");

        return sb;
    }

    private static StringBuilder getClaimantAddressUK(Address address) {
        var sb = new StringBuilder();
        sb.append("\"claimant_addressLine1\":\"").append(nullCheck(address.getAddressLine1())).append(NEW_LINE);
        sb.append("\"claimant_addressLine2\":\"").append(nullCheck(address.getAddressLine2())).append(NEW_LINE);
        sb.append("\"claimant_addressLine3\":\"").append(nullCheck(address.getAddressLine3())).append(NEW_LINE);
        sb.append("\"claimant_town\":\"").append(nullCheck(address.getPostTown())).append(NEW_LINE);
        sb.append("\"claimant_county\":\"").append(nullCheck(address.getCounty())).append(NEW_LINE);
        sb.append("\"claimant_postCode\":\"").append(nullCheck(address.getPostCode())).append(NEW_LINE);
        return sb;
    }

    private static StringBuilder getClaimantOrRepAddressUK(Address address) {
        var sb = new StringBuilder();
        sb.append("\"claimant_or_rep_addressLine1\":\"").append(nullCheck(address.getAddressLine1())).append(NEW_LINE);
        sb.append("\"claimant_or_rep_addressLine2\":\"").append(nullCheck(address.getAddressLine2())).append(NEW_LINE);
        sb.append("\"claimant_or_rep_addressLine3\":\"").append(nullCheck(address.getAddressLine3())).append(NEW_LINE);
        sb.append("\"claimant_or_rep_town\":\"").append(nullCheck(address.getPostTown())).append(NEW_LINE);
        sb.append("\"claimant_or_rep_county\":\"").append(nullCheck(address.getCounty())).append(NEW_LINE);
        sb.append("\"claimant_or_rep_postCode\":\"").append(nullCheck(address.getPostCode())).append(NEW_LINE);
        return sb;
    }

    private static StringBuilder getClaimantData(CaseData caseData) {
        log.info("Claimant Data");
        var sb = new StringBuilder();
        var representedTypeC = caseData.getRepresentativeClaimantType();
        Optional<ClaimantIndType> claimantIndType = Optional.ofNullable(caseData.getClaimantIndType());
        if (representedTypeC != null && caseData.getClaimantRepresentedQuestion() != null &&  caseData
                .getClaimantRepresentedQuestion().equals(YES)) {
            log.info("Claimant represented");
            sb.append("\"claimant_or_rep_full_name\":\"").append(nullCheck(representedTypeC.getNameOfRepresentative()))
                    .append(NEW_LINE);
            sb.append("\"claimant_rep_organisation\":\"").append(nullCheck(representedTypeC.getNameOfOrganisation()))
                    .append(NEW_LINE);
            if (representedTypeC.getRepresentativeAddress() != null) {
                sb.append(getClaimantOrRepAddressUK(representedTypeC.getRepresentativeAddress()));
            } else {
                sb.append(getClaimantOrRepAddressUK(new Address()));
            }
            sb.append("\"claimant_reference\":\"").append(nullCheck(representedTypeC.getRepresentativeReference()))
                    .append(NEW_LINE);
            Optional<String> claimantTypeOfClaimant = Optional.ofNullable(caseData.getClaimantTypeOfClaimant());
            if (claimantIndType.isPresent()) {
                sb.append("\"claimant_full_name\":\"").append(nullCheck(claimantIndType.get().claimantFullName()))
                        .append(NEW_LINE);
                sb.append("\"Claimant\":\"").append(nullCheck(claimantIndType.get().claimantFullName()))
                        .append(NEW_LINE);
            } else if (claimantTypeOfClaimant.isPresent() && caseData.getClaimantTypeOfClaimant()
                    .equals(COMPANY_TYPE_CLAIMANT)) {
                log.info("Claimant Company");
                sb.append("\"claimant_full_name\":\"").append(nullCheck(caseData.getClaimantCompany()))
                        .append(NEW_LINE);
                sb.append("\"Claimant\":\"").append(nullCheck(caseData.getClaimantCompany())).append(NEW_LINE);
            } else {
                sb.append("\"claimant_full_name\":\"").append(NEW_LINE);
                sb.append("\"Claimant\":\"").append(NEW_LINE);
            }
        } else {
            log.info("Claimant not represented");
            Optional<String> claimantTypeOfClaimant = Optional.ofNullable(caseData.getClaimantTypeOfClaimant());
            if (claimantTypeOfClaimant.isPresent() && caseData.getClaimantTypeOfClaimant()
                    .equals(COMPANY_TYPE_CLAIMANT)) {
                log.info("Claimant Company");
                sb.append("\"claimant_or_rep_full_name\":\"").append(nullCheck(caseData.getClaimantCompany()))
                        .append(NEW_LINE);
                sb.append("\"claimant_full_name\":\"").append(nullCheck(caseData.getClaimantCompany()))
                        .append(NEW_LINE);
                sb.append("\"Claimant\":\"").append(nullCheck(caseData.getClaimantCompany())).append(NEW_LINE);
            } else {
                log.info("Claimant data");
                if (claimantIndType.isPresent()) {
                    sb.append("\"claimant_or_rep_full_name\":\"").append(nullCheck(claimantIndType.get()
                            .claimantFullName())).append(NEW_LINE);
                    sb.append("\"claimant_full_name\":\"").append(nullCheck(claimantIndType.get().claimantFullName()))
                            .append(NEW_LINE);
                    sb.append("\"Claimant\":\"").append(nullCheck(claimantIndType.get().claimantFullName()))
                            .append(NEW_LINE);
                } else {
                    sb.append("\"claimant_or_rep_full_name\":\"").append(NEW_LINE);
                    sb.append("\"claimant_full_name\":\"").append(NEW_LINE);
                    sb.append("\"Claimant\":\"").append(NEW_LINE);
                    sb.append("\"claimant_rep_organisation\":\"").append(NEW_LINE);
                }
            }
            Optional<ClaimantType> claimantType = Optional.ofNullable(caseData.getClaimantType());
            if (claimantType.isPresent()) {
                sb.append(getClaimantOrRepAddressUK(claimantType.get().getClaimantAddressUK()));
            } else {
                sb.append(getClaimantOrRepAddressUK(new Address()));
            }
        }
        log.info("Claimant address UK");
        Optional<ClaimantType> claimantType = Optional.ofNullable(caseData.getClaimantType());
        if (claimantType.isPresent()) {
            sb.append(getClaimantAddressUK(claimantType.get().getClaimantAddressUK()));
        } else {
            sb.append(getClaimantAddressUK(new Address()));
        }
        return sb;
    }

    private static StringBuilder getRespondentAddressUK(Address address) {
        var sb = new StringBuilder();
        sb.append("\"respondent_addressLine1\":\"").append(nullCheck(address.getAddressLine1())).append(NEW_LINE);
        sb.append("\"respondent_addressLine2\":\"").append(nullCheck(address.getAddressLine2())).append(NEW_LINE);
        sb.append("\"respondent_addressLine3\":\"").append(nullCheck(address.getAddressLine3())).append(NEW_LINE);
        sb.append("\"respondent_town\":\"").append(nullCheck(address.getPostTown())).append(NEW_LINE);
        sb.append("\"respondent_county\":\"").append(nullCheck(address.getCounty())).append(NEW_LINE);
        sb.append("\"respondent_postCode\":\"").append(nullCheck(address.getPostCode())).append(NEW_LINE);
        return sb;
    }

    private static StringBuilder getRespondentOrRepAddressUK(Address address) {
        var sb = new StringBuilder();
        sb.append("\"respondent_or_rep_addressLine1\":\"").append(nullCheck(address.getAddressLine1()))
                .append(NEW_LINE);
        sb.append("\"respondent_or_rep_addressLine2\":\"").append(nullCheck(address.getAddressLine2()))
                .append(NEW_LINE);
        sb.append("\"respondent_or_rep_addressLine3\":\"").append(nullCheck(address.getAddressLine3()))
                .append(NEW_LINE);
        sb.append("\"respondent_or_rep_town\":\"").append(nullCheck(address.getPostTown())).append(NEW_LINE);
        sb.append("\"respondent_or_rep_county\":\"").append(nullCheck(address.getCounty())).append(NEW_LINE);
        sb.append("\"respondent_or_rep_postCode\":\"").append(nullCheck(address.getPostCode())).append(NEW_LINE);
        return sb;
    }

    private static StringBuilder getRespondentData(CaseData caseData) {
        log.info("Respondent Data");
        var sb = new StringBuilder();
        List<RepresentedTypeRItem> representedTypeRList = caseData.getRepCollection();
        List<RespondentSumTypeItem> respondentSumTypeItemList = !CollectionUtils.isEmpty(caseData.getRespondentCollection())
                && !CollectionUtils.isEmpty(caseData.getRepCollection()) ? caseData.getRespondentCollection().stream()
                .filter(a-> a.getValue().getRespondentName()
                        .equals(caseData.getRepCollection().get(0).getValue().getRespRepName())).collect(Collectors.toList()): new ArrayList<>();
        boolean responseNotStruckOut = CollectionUtils.isEmpty(respondentSumTypeItemList)
                || Strings.isNullOrEmpty(respondentSumTypeItemList.get(0).getValue().getResponseStruckOut())
                || respondentSumTypeItemList.get(0).getValue().getResponseStruckOut().equals(NO);
        if (!CollectionUtils.isEmpty(representedTypeRList) && responseNotStruckOut) {
            log.info("Respondent represented");
            var representedTypeR = representedTypeRList.get(0).getValue();
            sb.append("\"respondent_or_rep_full_name\":\"").append(nullCheck(representedTypeR
                    .getNameOfRepresentative())).append(NEW_LINE);
            if (representedTypeR.getRepresentativeAddress() != null) {
                sb.append(getRespondentOrRepAddressUK(representedTypeR.getRepresentativeAddress()));
            } else {
                sb.append(getRespondentOrRepAddressUK(new Address()));
            }
            sb.append("\"respondent_reference\":\"").append(nullCheck(representedTypeR.getRepresentativeReference()))
                    .append(NEW_LINE);
            sb.append("\"respondent_rep_organisation\":\"").append(nullCheck(representedTypeR.getNameOfOrganisation()))
                        .append(NEW_LINE);

        } else {
            log.info("Respondent not represented");
            if (caseData.getRespondentCollection() != null && !caseData.getRespondentCollection().isEmpty()) {
                var respondentSumType = caseData.getRespondentCollection().get(0).getValue();
                sb.append("\"respondent_or_rep_full_name\":\"").append(nullCheck(respondentSumType
                        .getRespondentName())).append(NEW_LINE);
                sb.append(getRespondentOrRepAddressUK(getRespondentAddressET3(respondentSumType)));
            } else {
                sb.append("\"respondent_or_rep_full_name\":\"").append(NEW_LINE);
                sb.append("\"respondent_rep_organisation\":\"").append(NEW_LINE);
                sb.append(getRespondentOrRepAddressUK(new Address()));
            }
        }
        if (caseData.getRespondentCollection() != null && !caseData.getRespondentCollection().isEmpty()) {
            log.info("Respondent collection");
            var respondentSumType = caseData.getRespondentCollection().get(0).getValue();
            sb.append("\"respondent_full_name\":\"").append(nullCheck(respondentSumType.getRespondentName()))
                    .append(NEW_LINE);
            sb.append(getRespondentAddressUK(getRespondentAddressET3(respondentSumType)));
            sb.append("\"Respondent\":\"").append(caseData.getRespondentCollection().size() > 1 ? "1. " : "")
                    .append(nullCheck(respondentSumType.getRespondentName())).append(NEW_LINE);
            sb.append(getRespOthersName(caseData));
            sb.append(getRespAddress(caseData));
        } else {
            sb.append("\"respondent_full_name\":\"").append(NEW_LINE);
            sb.append(getRespondentAddressUK(new Address()));
            sb.append("\"Respondent\":\"").append(NEW_LINE);
            sb.append("\"resp_others\":\"").append(NEW_LINE);
            sb.append("\"resp_address\":\"").append(NEW_LINE);
        }
        return sb;
    }

    private static StringBuilder getRespOthersName(CaseData caseData) {
        log.info("Respondent Others Name");
        var sb = new StringBuilder();
        var atomicInteger = new AtomicInteger(2);
        List<String> respOthers = caseData.getRespondentCollection()
                .stream()
                .skip(1)
                .filter(respondentSumTypeItem -> respondentSumTypeItem.getValue().getResponseStruckOut() == null
                        || respondentSumTypeItem.getValue().getResponseStruckOut().equals(NO))
                .map(respondentSumTypeItem -> atomicInteger.getAndIncrement() + ". "
                        + respondentSumTypeItem.getValue().getRespondentName())
                .collect(Collectors.toList());
        sb.append("\"resp_others\":\"").append(nullCheck(String.join("\\n", respOthers))).append(NEW_LINE);
        return sb;
    }

    private static StringBuilder getRespAddress(CaseData caseData) {
        log.info("Get Resp address");
        var sb = new StringBuilder();
        var atomicInteger = new AtomicInteger(1);
        int size = caseData.getRespondentCollection().size();
        List<String> respAddressList = caseData.getRespondentCollection()
                .stream()
                .filter(respondentSumTypeItem -> respondentSumTypeItem.getValue().getResponseStruckOut() == null
                        || respondentSumTypeItem.getValue().getResponseStruckOut().equals(NO))
                .map(respondentSumTypeItem -> (size > 1 ? atomicInteger.getAndIncrement() + ". " : "")
                        + getRespondentAddressET3(respondentSumTypeItem.getValue()))
                .collect(Collectors.toList());
        sb.append("\"resp_address\":\"").append(nullCheck(String.join("\\n", respAddressList)))
                .append(NEW_LINE);
        return sb;
    }

    private static StringBuilder getHearingData(CaseData caseData, String caseTypeId,
                                                InputStream venueAddressInputStream,
                                                CorrespondenceType correspondenceType,
                                                CorrespondenceScotType correspondenceScotType) {
        log.info("Hearing Data");
        var sb = new StringBuilder();
        //Currently checking collection not the HearingType
        if (caseData.getHearingCollection() != null && !caseData.getHearingCollection().isEmpty()) {
            String correspondenceHearingNumber = getCorrespondenceHearingNumber(
                    correspondenceType, correspondenceScotType);
            log.info("Hearing Number: " + correspondenceHearingNumber);
            var hearingType = getHearingByNumber(caseData.getHearingCollection(), correspondenceHearingNumber);
            log.info("Hearing type info by number");
            if (hearingType.getHearingDateCollection() != null && !hearingType.getHearingDateCollection().isEmpty()) {
                log.info("Hearing dates collection");
                sb.append("\"Hearing_date\":\"").append(nullCheck(getHearingDates(hearingType
                        .getHearingDateCollection()))).append(NEW_LINE);
                String hearingDateAndTime = nullCheck(getHearingDatesAndTime(hearingType.getHearingDateCollection()));
                sb.append("\"Hearing_date_time\":\"").append(hearingDateAndTime).append(NEW_LINE);
                sb.append("\"Hearing_time\":\"").append(getHearingTime(hearingDateAndTime)).append(NEW_LINE);
            } else {
                sb.append("\"Hearing_date\":\"").append(NEW_LINE);
                sb.append("\"Hearing_date_time\":\"").append(NEW_LINE);
                sb.append("\"Hearing_time\":\"").append(NEW_LINE);
            }
            log.info("Checking hearing venue and duration");
            sb.append("\"Hearing_venue\":\"").append(nullCheck(getVenueAddress(
                    hearingType, caseTypeId, venueAddressInputStream))).append(NEW_LINE);
            sb.append("\"Hearing_duration\":\"").append(nullCheck(getHearingDuration(hearingType))).append(NEW_LINE);
        } else {
            sb.append("\"Hearing_date\":\"").append(NEW_LINE);
            sb.append("\"Hearing_date_time\":\"").append(NEW_LINE);
            sb.append("\"Hearing_venue\":\"").append(NEW_LINE);
            sb.append("\"Hearing_duration\":\"").append(NEW_LINE);
            sb.append("\"Hearing_time\":\"").append(NEW_LINE);
        }
        return sb;
    }

    public static String getCorrespondenceHearingNumber(CorrespondenceType correspondenceType,
                                                        CorrespondenceScotType correspondenceScotType) {
        if (correspondenceType != null) {
            return correspondenceType.getHearingNumber();
        } else {
            if (correspondenceScotType != null) {
                return correspondenceScotType.getHearingNumber();
            } else {
                return "";
            }
        }
    }

    public static HearingType getHearingByNumber(List<HearingTypeItem> hearingCollection,
                                                 String correspondenceHearingNumber) {

        var hearingType = new HearingType();

        for (HearingTypeItem hearingTypeItem : hearingCollection) {
            hearingType = hearingTypeItem.getValue();
            if (hearingType.getHearingNumber() != null && hearingType.getHearingNumber().equals(correspondenceHearingNumber)) {
                    break;
            }
        }

        return hearingType;
    }

    private static String getHearingTime(String dateTime) {
        return !dateTime.isEmpty() ? dateTime.substring(dateTime.indexOf("at") + 3) : "";
    }

    private static String getHearingDates(List<DateListedTypeItem> hearingDateCollection) {

        var sb = new StringBuilder();

        List<String> dateListedList = new ArrayList<>();
        for (DateListedTypeItem dateListedTypeItem : hearingDateCollection) {
            if (dateListedTypeItem.getValue().getHearingStatus() != null
                    && dateListedTypeItem.getValue().getHearingStatus().equals(HEARING_STATUS_LISTED)) {
                dateListedList.add(UtilHelper.formatLocalDate(dateListedTypeItem.getValue().getListedDate()));
            }
        }
        sb.append(String.join(", ", dateListedList));

        return sb.toString();
    }

    private static String getHearingDatesAndTime(List<DateListedTypeItem> hearingDateCollection) {

        var sb = new StringBuilder(getHearingDates(hearingDateCollection));
        Iterator<DateListedTypeItem> itr = hearingDateCollection.iterator();
        var earliestTime = LocalTime.of(23, 59);
        var isEmpty = true;

        while (itr.hasNext()) {
            var dateListedType = itr.next().getValue();
            if (dateListedType.getHearingStatus() != null && dateListedType.getHearingStatus()
                    .equals(HEARING_STATUS_LISTED)) {
                var listedDate = LocalDateTime.parse(dateListedType.getListedDate());
                var listedTime = LocalTime.of(listedDate.getHour(), listedDate.getMinute());
                earliestTime = listedTime.isBefore(earliestTime) ? listedTime : earliestTime;
                isEmpty = false;
            }
        }
        if (!isEmpty) {
            sb.append(" at ");
            sb.append(earliestTime.toString());
        }

        return sb.toString();
    }

    private static String getVenueAddress(HearingType hearingType, String caseTypeId,
                                          InputStream venueAddressInputStream) {

        String hearingVenue = getHearingVenue(hearingType, caseTypeId);
        log.info("HearingVenue: " + hearingVenue);
        try (Workbook workbook = new XSSFWorkbook(venueAddressInputStream)) {
            var datatypeSheet = workbook.getSheet(caseTypeId);
            if (datatypeSheet != null) {
                log.info("Processing venue addresses for tab : " + caseTypeId + " within file : "
                        + VENUE_ADDRESS_VALUES_FILE_PATH);
                for (Row currentRow : datatypeSheet) {
                    if (currentRow.getRowNum() == 0) {
                        continue;
                    }
                    String excelHearingVenue = getCellValue(currentRow.getCell(0));
                    log.info("ExcelHearingVenue: " + excelHearingVenue);
                    if (!isNullOrEmpty(excelHearingVenue) && excelHearingVenue.equals(hearingVenue)) {
                        return getCellValue(currentRow.getCell(1));
                    }
                }
            }

        } catch (Exception ex) {
            log.error(VENUE_ADDRESS_OPENING_PROCESSING_ERROR + ex.getMessage());
        }

        return hearingVenue;
    }

    private static String getHearingVenue(HearingType hearingType, String caseTypeId) {
        String hearingVenueToSearch = hearingType.getHearingVenue();
        if (caseTypeId.equals(SCOTLAND_CASE_TYPE_ID)) {
            switch (hearingVenueToSearch) {
                case ABERDEEN_OFFICE:
                    return hearingType.getHearingAberdeen();
                case DUNDEE_OFFICE:
                    return hearingType.getHearingDundee();
                case EDINBURGH_OFFICE:
                    return hearingType.getHearingEdinburgh();
                default:
                    return hearingType.getHearingGlasgow();
            }
        } else {
            return hearingVenueToSearch;
        }
    }

    private static String getCellValue(Cell currentCell) {
        if (currentCell.getCellType() == CellType.STRING) {
            return currentCell.getStringCellValue();
        } else {
            return "";
        }
    }

    static String getHearingDuration(HearingType hearingType) {
        String numType = hearingType.getHearingEstLengthNumType();
        try {
            int tmp = Integer.parseInt(hearingType.getHearingEstLengthNum());
            if (tmp == 1) {
                numType = numType.substring(0, numType.length() - 1);
            }
        } catch (NumberFormatException e) {
            log.error(e.toString());
            numType = hearingType.getHearingEstLengthNumType();
        }
            return String.join(" ",
                    hearingType.getHearingEstLengthNum(), numType);
        }

    public static String getTemplateName(CorrespondenceType correspondenceType,
                                         CorrespondenceScotType correspondenceScotType) {
        if (correspondenceType != null) {
            return correspondenceType.getTopLevelDocuments();
        } else {
            if (correspondenceScotType != null) {
                return correspondenceScotType.getTopLevelScotDocuments();
            } else {
                return "";
            }
        }
    }

    public static String getEWSectionName(CorrespondenceType correspondenceType) {
        if (correspondenceType != null) {
            return getEWPartDocument(correspondenceType);
        }
        return "";
    }

    public static String getScotSectionName(CorrespondenceScotType correspondenceScotType) {
        if (correspondenceScotType != null) {
            return getScotPartDocument(correspondenceScotType);
        }
        return "";
    }

    private static String getEWPartDocument(CorrespondenceType correspondence) {
        if (correspondence.getPart0Documents() != null) {
            return correspondence.getPart0Documents();
        }
        if (correspondence.getPart1Documents() != null) {
            return correspondence.getPart1Documents();
        }
        if (correspondence.getPart2Documents() != null) {
            return correspondence.getPart2Documents();
        }
        if (correspondence.getPart3Documents() != null) {
            return correspondence.getPart3Documents();
        }
        if (correspondence.getPart4Documents() != null) {
            return correspondence.getPart4Documents();
        }
        if (correspondence.getPart5Documents() != null) {
            return correspondence.getPart5Documents();
        }
        if (correspondence.getPart6Documents() != null) {
            return correspondence.getPart6Documents();
        }
        if (correspondence.getPart7Documents() != null) {
            return correspondence.getPart7Documents();
        }
        if (correspondence.getPart8Documents() != null) {
            return correspondence.getPart8Documents();
        }
        if (correspondence.getPart9Documents() != null) {
            return correspondence.getPart9Documents();
        }
        if (correspondence.getPart10Documents() != null) {
            return correspondence.getPart10Documents();
        }
        if (correspondence.getPart11Documents() != null) {
            return correspondence.getPart11Documents();
        }
        if (correspondence.getPart12Documents() != null) {
            return correspondence.getPart12Documents();
        }
        if (correspondence.getPart13Documents() != null) {
            return correspondence.getPart13Documents();
        }
        if (correspondence.getPart14Documents() != null) {
            return correspondence.getPart14Documents();
        }
        if (correspondence.getPart15Documents() != null) {
            return correspondence.getPart15Documents();
        }
        if (correspondence.getPart16Documents() != null) {
            return correspondence.getPart16Documents();
        }
        if (correspondence.getPart17Documents() != null) {
            return correspondence.getPart17Documents();
        }
        if (correspondence.getPart18Documents() != null) {
            return correspondence.getPart18Documents();
        }
        return "";
    }

    private static String getScotPartDocument(CorrespondenceScotType correspondenceScotType) {
        if (correspondenceScotType.getPart0ScotDocuments() != null) {
            return correspondenceScotType.getPart0ScotDocuments();
        }
        if (correspondenceScotType.getPart1ScotDocuments() != null) {
            return correspondenceScotType.getPart1ScotDocuments();
        }
        if (correspondenceScotType.getPart2ScotDocuments() != null) {
            return correspondenceScotType.getPart2ScotDocuments();
        }
        if (correspondenceScotType.getPart3ScotDocuments() != null) {
            return correspondenceScotType.getPart3ScotDocuments();
        }
        if (correspondenceScotType.getPart4ScotDocuments() != null) {
            return correspondenceScotType.getPart4ScotDocuments();
        }
        if (correspondenceScotType.getPart5ScotDocuments() != null) {
            return correspondenceScotType.getPart5ScotDocuments();
        }
        if (correspondenceScotType.getPart6ScotDocuments() != null) {
            return correspondenceScotType.getPart6ScotDocuments();
        }
        if (correspondenceScotType.getPart7ScotDocuments() != null) {
            return correspondenceScotType.getPart7ScotDocuments();
        }
        if (correspondenceScotType.getPart8ScotDocuments() != null) {
            return correspondenceScotType.getPart8ScotDocuments();
        }
        if (correspondenceScotType.getPart9ScotDocuments() != null) {
            return correspondenceScotType.getPart9ScotDocuments();
        }
        if (correspondenceScotType.getPart10ScotDocuments() != null) {
            return correspondenceScotType.getPart10ScotDocuments();
        }
        if (correspondenceScotType.getPart11ScotDocuments() != null) {
            return correspondenceScotType.getPart11ScotDocuments();
        }
        if (correspondenceScotType.getPart12ScotDocuments() != null) {
            return correspondenceScotType.getPart12ScotDocuments();
        }
        if (correspondenceScotType.getPart13ScotDocuments() != null) {
            return correspondenceScotType.getPart13ScotDocuments();
        }
        if (correspondenceScotType.getPart14ScotDocuments() != null) {
            return correspondenceScotType.getPart14ScotDocuments();
        }
        if (correspondenceScotType.getPart15ScotDocuments() != null) {
            return correspondenceScotType.getPart15ScotDocuments();
        }
        return "";
    }

    private static StringBuilder getCorrespondenceData(CorrespondenceType correspondence) {
        log.info("Correspondence data");
        String sectionName = getEWSectionName(correspondence);
        var sb = new StringBuilder();
        if (!sectionName.equals("")) {
            sb.append("\"").append("t").append(sectionName.replace(".", "_"))
                    .append("\":\"").append("true").append(NEW_LINE);
        }
        return sb;
    }

    private static StringBuilder getCorrespondenceScotData(CorrespondenceScotType correspondenceScotType) {
        log.info("Correspondence scot data");
        String scotSectionName = getScotSectionName(correspondenceScotType);
        var sb = new StringBuilder();
        if (!scotSectionName.equals("")) {
            sb.append("\"").append("t_Scot_").append(scotSectionName.replace(".", "_"))
                    .append("\":\"").append("true").append(NEW_LINE);
        }
        return sb;
    }

    private static StringBuilder getCourtData(CaseData caseData, DefaultValues allocatedCourtAddress) {
        var sb = new StringBuilder();
        log.info("Court data");
        if (allocatedCourtAddress != null) {
            sb.append("\"Court_addressLine1\":\"").append(nullCheck(allocatedCourtAddress
                    .getTribunalCorrespondenceAddressLine1())).append(NEW_LINE);
            sb.append("\"Court_addressLine2\":\"").append(nullCheck(allocatedCourtAddress
                    .getTribunalCorrespondenceAddressLine2())).append(NEW_LINE);
            sb.append("\"Court_addressLine3\":\"").append(nullCheck(allocatedCourtAddress
                    .getTribunalCorrespondenceAddressLine3())).append(NEW_LINE);
            sb.append("\"Court_town\":\"").append(nullCheck(allocatedCourtAddress
                    .getTribunalCorrespondenceTown())).append(NEW_LINE);
            sb.append("\"Court_county\":\"").append(NEW_LINE);
            sb.append("\"Court_postCode\":\"").append(nullCheck(allocatedCourtAddress
                    .getTribunalCorrespondencePostCode())).append(NEW_LINE);
            sb.append("\"Court_telephone\":\"").append(nullCheck(allocatedCourtAddress
                    .getTribunalCorrespondenceTelephone())).append(NEW_LINE);
            sb.append("\"Court_fax\":\"").append(nullCheck(allocatedCourtAddress
                    .getTribunalCorrespondenceFax())).append(NEW_LINE);
            sb.append("\"Court_DX\":\"").append(nullCheck(allocatedCourtAddress
                    .getTribunalCorrespondenceDX())).append(NEW_LINE);
            sb.append("\"Court_Email\":\"").append(nullCheck(allocatedCourtAddress
                    .getTribunalCorrespondenceEmail())).append(NEW_LINE);
        } else {
            if (caseData.getTribunalCorrespondenceAddress() != null) {
                sb.append("\"Court_addressLine1\":\"").append(nullCheck(caseData
                        .getTribunalCorrespondenceAddress().getAddressLine1())).append(NEW_LINE);
                sb.append("\"Court_addressLine2\":\"").append(nullCheck(caseData
                        .getTribunalCorrespondenceAddress().getAddressLine2())).append(NEW_LINE);
                sb.append("\"Court_addressLine3\":\"").append(nullCheck(caseData
                        .getTribunalCorrespondenceAddress().getAddressLine3())).append(NEW_LINE);
                sb.append("\"Court_town\":\"").append(nullCheck(caseData
                        .getTribunalCorrespondenceAddress().getPostTown())).append(NEW_LINE);
                sb.append("\"Court_county\":\"").append(nullCheck(caseData
                        .getTribunalCorrespondenceAddress().getCounty())).append(NEW_LINE);
                sb.append("\"Court_postCode\":\"").append(nullCheck(caseData
                        .getTribunalCorrespondenceAddress().getPostCode())).append(NEW_LINE);
            }
            sb.append("\"Court_telephone\":\"").append(nullCheck(caseData
                    .getTribunalCorrespondenceTelephone())).append(NEW_LINE);
            sb.append("\"Court_fax\":\"").append(nullCheck(caseData
                    .getTribunalCorrespondenceFax())).append(NEW_LINE);
            sb.append("\"Court_DX\":\"").append(nullCheck(caseData
                    .getTribunalCorrespondenceDX())).append(NEW_LINE);
            sb.append("\"Court_Email\":\"").append(nullCheck(caseData
                    .getTribunalCorrespondenceEmail())).append(NEW_LINE);
        }
        return sb;
    }

    private static StringBuilder getAddressLabelsDataSingleCase(CaseData caseData) {

        var numberOfCopies = Integer.parseInt(caseData.getAddressLabelsAttributesType().getNumberOfCopies());
        var startingLabel = Integer.parseInt(caseData.getAddressLabelsAttributesType().getStartingLabel());
        String showTelFax = caseData.getAddressLabelsAttributesType().getShowTelFax();
        List<AddressLabelTypeItem> addressLabelCollection = caseData.getAddressLabelCollection();

        return getAddressLabelsData(numberOfCopies, startingLabel, showTelFax, addressLabelCollection);

    }

    private static StringBuilder getAddressLabelsDataMultipleCase(MultipleData multipleData) {

        var numberOfCopies = Integer.parseInt(multipleData.getAddressLabelsAttributesType().getNumberOfCopies());
        var startingLabel = Integer.parseInt(multipleData.getAddressLabelsAttributesType().getStartingLabel());
        String showTelFax = multipleData.getAddressLabelsAttributesType().getShowTelFax();
        List<AddressLabelTypeItem> addressLabelCollection = multipleData.getAddressLabelCollection();

        return getAddressLabelsData(numberOfCopies, startingLabel, showTelFax, addressLabelCollection);

    }

    private static StringBuilder getAddressLabelsData(int numberOfCopies, int startingLabel, String showTelFax,
                                                      List<AddressLabelTypeItem> addressLabelCollection) {

        List<AddressLabelTypeItem> selectedAddressLabelCollection = getSelectedAddressLabels(addressLabelCollection);
        List<AddressLabelTypeItem> copiedAddressLabelCollection =
                getCopiedAddressLabels(selectedAddressLabelCollection, numberOfCopies);

        var sb = new StringBuilder();
        sb.append("\"address_labels_page\":[\n");

        var startingLabelAboveOne = true;

        for (var i = 0; i < copiedAddressLabelCollection.size(); i++) {
            int pageLabelNumber = i + 1;

            if (startingLabel > 1) {
                pageLabelNumber += startingLabel - 1;
            }

            if (pageLabelNumber > ADDRESS_LABELS_PAGE_SIZE) {
                int numberOfFullLabelPages = pageLabelNumber / ADDRESS_LABELS_PAGE_SIZE;
                pageLabelNumber = pageLabelNumber % ADDRESS_LABELS_PAGE_SIZE == 0
                        ? ADDRESS_LABELS_PAGE_SIZE
                        : pageLabelNumber - (numberOfFullLabelPages * ADDRESS_LABELS_PAGE_SIZE);
            }

            if (pageLabelNumber == 1 || startingLabelAboveOne) {
                startingLabelAboveOne = false;
                sb.append("{");
            }

            String templateLabelNumber = (pageLabelNumber < 10)
                    ? "0" + pageLabelNumber : String.valueOf(pageLabelNumber);
            sb.append(getAddressLabel(copiedAddressLabelCollection.get(i).getValue(), templateLabelNumber, showTelFax));

            if (pageLabelNumber == ADDRESS_LABELS_PAGE_SIZE || i == copiedAddressLabelCollection.size() - 1) {
                sb.append("}");
            }

            if (i != copiedAddressLabelCollection.size() - 1) {
                sb.append(",\n");
            }
        }
        sb.append("],\n");
        return sb;
    }

    private static StringBuilder getAddressLabel(AddressLabelType addressLabelType,
                                                 String labelNumber, String showTelFax) {
        var sb = new StringBuilder();
        sb.append("\"").append(LABEL).append(labelNumber).append("_Entity_Name_01\":\"")
                .append(nullCheck(addressLabelType.getLabelEntityName01())).append(NEW_LINE);
        sb.append("\"").append(LABEL).append(labelNumber).append("_Entity_Name_02\":\"")
                .append(nullCheck(addressLabelType.getLabelEntityName02())).append(NEW_LINE);
        sb.append(getAddressLines(addressLabelType, labelNumber));
        sb.append(getTelFaxLine(addressLabelType, labelNumber, showTelFax));
        sb.append("\"").append(LBL).append(labelNumber).append("_Eef\":\"")
                .append(nullCheck(addressLabelType.getLabelEntityReference())).append(NEW_LINE);
        sb.append("\"").append(LBL).append(labelNumber).append("_Cef\":\"")
                .append(nullCheck(addressLabelType.getLabelCaseReference())).append("\"");
        return sb;
    }

    private static StringBuilder getAddressLines(AddressLabelType addressLabelType, String labelNumber) {
        var sb = new StringBuilder();

        var lineNum = 0;
        var addressLine = "";

        if (!isNullOrEmpty(nullCheck(addressLabelType.getLabelEntityAddress().getAddressLine1()))) {
            lineNum++;
            addressLine = nullCheck(addressLabelType.getLabelEntityAddress().getAddressLine1());
            sb.append(getAddressLine(addressLine, labelNumber, lineNum));
        }

        if (!isNullOrEmpty(nullCheck(addressLabelType.getLabelEntityAddress().getAddressLine2()))) {
            lineNum++;
            addressLine = nullCheck(addressLabelType.getLabelEntityAddress().getAddressLine2());
            sb.append(getAddressLine(addressLine, labelNumber, lineNum));
        }

        if (!isNullOrEmpty(nullCheck(addressLabelType.getLabelEntityAddress().getAddressLine3()))) {
            lineNum++;
            addressLine = nullCheck(addressLabelType.getLabelEntityAddress().getAddressLine3());
            sb.append(getAddressLine(addressLine, labelNumber, lineNum));
        }

        if (!isNullOrEmpty(nullCheck(addressLabelType.getLabelEntityAddress().getPostTown()))) {
            lineNum++;
            addressLine = nullCheck(addressLabelType.getLabelEntityAddress().getPostTown());
            sb.append(getAddressLine(addressLine, labelNumber, lineNum));
        }

        if (!isNullOrEmpty(nullCheck(addressLabelType.getLabelEntityAddress().getCounty()))) {
            lineNum++;
            addressLine = nullCheck(addressLabelType.getLabelEntityAddress().getCounty());
            if (lineNum < 5) {
                sb.append(getAddressLine(addressLine, labelNumber, lineNum));
            }
        }

        if (!isNullOrEmpty(nullCheck(addressLabelType.getLabelEntityAddress().getPostCode()))) {
            if (lineNum < 5) {
                lineNum++;
                addressLine = nullCheck(addressLabelType.getLabelEntityAddress().getPostCode());
            } else {
                addressLine += " " + nullCheck(addressLabelType.getLabelEntityAddress().getPostCode());
            }
            sb.append(getAddressLine(addressLine, labelNumber, lineNum));
        }
        return sb;
    }

    private static StringBuilder getAddressLine(String addressLine, String labelNumber, int lineNum) {
        var sb = new StringBuilder();
        String lineNumber = "0" + lineNum;
        sb.append("\"").append(LABEL).append(labelNumber).append("_Address_Line_").append(lineNumber)
                .append("\":\"").append(addressLine).append(NEW_LINE);
        return sb;
    }

    private static StringBuilder getTelFaxLine(AddressLabelType addressLabelType, String labelNumber,
                                               String showTelFax) {
        var sb = new StringBuilder();
        if (showTelFax.equals(YES)) {
            var tel = "";
            var fax = "";

            if (!isNullOrEmpty(addressLabelType.getLabelEntityTelephone())) {
                tel = addressLabelType.getLabelEntityTelephone();
            }

            if (isNullOrEmpty(tel)) {
                if (!isNullOrEmpty(addressLabelType.getLabelEntityFax())) {
                    tel = addressLabelType.getLabelEntityFax();
                }
            } else {
                if (!isNullOrEmpty(addressLabelType.getLabelEntityFax())) {
                    fax = addressLabelType.getLabelEntityFax();
                }
            }

            sb.append("\"").append(LABEL).append(labelNumber).append("_Telephone\":\"").append(tel).append(NEW_LINE);
            sb.append("\"").append(LABEL).append(labelNumber).append("_Fax\":\"").append(fax).append(NEW_LINE);
        }
        return sb;
    }

    public static List<AddressLabelTypeItem> getSelectedAddressLabels(
            List<AddressLabelTypeItem> addressLabelCollection) {

        List<AddressLabelTypeItem> selectedAddressLabels = new ArrayList<>();

        if (addressLabelCollection != null && !addressLabelCollection.isEmpty()) {
            selectedAddressLabels = addressLabelCollection
                    .stream()
                    .filter(addressLabelTypeItem -> addressLabelTypeItem.getValue().getPrintLabel() != null
                            && addressLabelTypeItem.getValue().getPrintLabel().equals(YES))
                    .filter(addressLabelTypeItem -> addressLabelTypeItem.getValue().getFullName() != null
                            || addressLabelTypeItem.getValue().getFullAddress() != null)
                    .collect(Collectors.toList());
        }

        return selectedAddressLabels;
    }

    private static List<AddressLabelTypeItem> getCopiedAddressLabels(List<AddressLabelTypeItem> selectedAddressLabels,
                                                                     int numberOfCopies) {

        List<AddressLabelTypeItem> copiedAddressLabels = new ArrayList<>();
        if (!selectedAddressLabels.isEmpty() && numberOfCopies > 1) {
            for (AddressLabelTypeItem selectedAddressLabel : selectedAddressLabels) {
                var addressLabelType = selectedAddressLabel.getValue();
                for (var i = 0; i < numberOfCopies; i++) {
                    var addressLabelTypeItem = new AddressLabelTypeItem();
                    addressLabelTypeItem.setId(String.valueOf(copiedAddressLabels.size()));
                    addressLabelTypeItem.setValue(addressLabelType);
                    copiedAddressLabels.add(addressLabelTypeItem);
                }
            }
        } else {
            return selectedAddressLabels;
        }

        return copiedAddressLabels;
    }

    public static Address getRespondentAddressET3(RespondentSumType respondentSumType) {

        log.info("Get respondent address ET3");

        return respondentSumType.getResponseRespondentAddress() != null
                ? respondentSumType.getResponseRespondentAddress()
                : respondentSumType.getRespondentAddress();

    }

}
