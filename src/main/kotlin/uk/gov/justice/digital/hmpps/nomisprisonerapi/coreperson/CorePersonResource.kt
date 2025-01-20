package uk.gov.justice.digital.hmpps.nomisprisonerapi.coreperson

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.NomisAudit
import java.time.LocalDate
import java.time.LocalDateTime

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class CorePersonResource(private val corePersonService: CorePersonService) {
  @PreAuthorize("hasRole('ROLE_NOMIS_CORE_PERSON')")
  @GetMapping("/core-person/{prisonNumber}")
  @Operation(
    summary = "Get an offender by prison number",
    description = "Retrieves an offender. Requires ROLE_NOMIS_CORE_PERSON",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Core person information returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = CorePerson::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CORE_PERSON",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Offender does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getOffender(
    @Schema(
      description = "Prison number aka noms id / offender id display",
      example = "A1234BC",
    ) @PathVariable prisonNumber: String,
  ): CorePerson = corePersonService.getOffender(prisonNumber)
}

@Schema(description = "The data held in NOMIS for an offender")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class CorePerson(
  @Schema(description = "The prison number")
  val prisonNumber: String,
  @Schema(description = "In/Out Status", example = "IN, OUT, TRN")
  val inOutStatus: String?,
  @Schema(description = "Indicates that the person is currently in prison")
  val activeFlag: Boolean,
  @Schema(description = "List of offender records for the person")
  val offenders: List<CoreOffender>,
  @Schema(description = "List of distinct sentence start dates")
  val sentenceStartDates: List<LocalDate>,
  @Schema(description = "List of addresses for the person")
  val addresses: List<OffenderAddress>,
  @Schema(description = "List of phone numbers for the person")
  val phoneNumbers: List<OffenderPhoneNumber>,
  @Schema(description = "List of email addresses for the person")
  val emailAddresses: List<OffenderEmailAddress>,
  @Schema(description = "List of nationalities for the person")
  val nationalities: List<OffenderNationality>,
  @Schema(description = "List of nationality details for the person")
  val nationalityDetails: List<OffenderNationalityDetails>,
  @Schema(description = "List of sexual orientations for the person")
  val sexualOrientations: List<OffenderSexualOrientation>,
  @Schema(description = "List of disabilities for the person")
  val disabilities: List<OffenderDisability>,
  @Schema(description = "List of disabilities for the person")
  val interestsToImmigration: List<OffenderInterestToImmigration>,
  @Schema(description = "Current belief and history of all beliefs for the person")
  val beliefs: List<OffenderBelief>,
)

@Schema(description = "The data held in NOMIS for an offender.")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CoreOffender(
  @Schema(description = "The offender id")
  val offenderId: Long,
  @Schema(description = "Title of this offender record")
  val title: CodeDescription?,
  @Schema(description = "First name of this offender record")
  val firstName: String,
  @Schema(description = "Middle name of this offender record")
  val middleName1: String?,
  @Schema(description = "Second middle name of this offender record")
  val middleName2: String?,
  @Schema(description = "Surname name of this offender record")
  val lastName: String,
  @Schema(description = "Date of birth of this offender record")
  val dateOfBirth: LocalDate?,
  @Schema(description = "Birth place of this offender record")
  val birthPlace: String?,
  @Schema(description = "Birth country of this offender record")
  val birthCountry: CodeDescription?,
  @Schema(description = "Race of this offender record")
  val ethnicity: CodeDescription?,
  @Schema(description = "Sex of this offender record")
  val sex: CodeDescription?,
  @Schema(description = "The offender record associated with the current booking")
  val workingName: Boolean,
  @Schema(description = "List of identifiers for the offender")
  val identifiers: List<Identifier>,
)

@Schema(description = "The data held in NOMIS for an offender's identifiers")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Identifier(
  @Schema(description = "Unique NOMIS sequence for this identifier for this person")
  val sequence: Long,
  @Schema(description = "The identifier type")
  val type: CodeDescription,
  @Schema(description = "The identifier value", example = "NE121212T")
  val identifier: String,
  @Schema(description = "The issued authority", example = "Police")
  val issuedAuthority: String?,
  @Schema(description = "The issued date")
  val issuedDate: LocalDate?,
  @Schema(description = "Verified")
  val verified: Boolean,
)

@Schema(description = "The data held in NOMIS about a address number")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class OffenderAddress(
  @Schema(description = "Unique NOMIS Id of number")
  val addressId: Long,
  @Schema(description = "Flat name or number", example = "Apartment 3")
  val flat: String?,
  @Schema(description = "Premise", example = "22")
  val premise: String?,
  @Schema(description = "Street", example = "West Street")
  val street: String?,
  @Schema(description = "Locality", example = "Keighley")
  val locality: String?,
  @Schema(description = "Post code", example = "MK15 2ST")
  val postcode: String?,
  @Schema(description = "City")
  val city: CodeDescription?,
  @Schema(description = "County")
  val county: CodeDescription?,
  @Schema(description = "Country")
  val country: CodeDescription?,
  @Schema(description = "List of phone numbers for the address")
  val phoneNumbers: List<OffenderPhoneNumber>,
  @Schema(description = "true if address validated by Post Office Address file??")
  val validatedPAF: Boolean,
  @Schema(description = "true if address not fixed. for example homeless")
  val noFixedAddress: Boolean?,
  @Schema(description = "true if this is the person's primary address")
  val primaryAddress: Boolean,
  @Schema(description = "true if this is used for mail")
  val mailAddress: Boolean,
  @Schema(description = "Free format comment about the address")
  val comment: String?,
  @Schema(description = "Date address was valid from")
  val startDate: LocalDate?,
  @Schema(description = "Date address was valid to")
  val endDate: LocalDate?,
  @Schema(description = "Usages for the address, also known as types")
  val usages: List<OffenderAddressUsage>,
)

@Schema(description = "The data held in NOMIS about a phone number")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OffenderPhoneNumber(
  @Schema(description = "Unique NOMIS Id of number")
  val phoneId: Long,
  @Schema(description = "The number")
  val number: String,
  @Schema(description = "Extension")
  val extension: String?,
  @Schema(description = "Phone type")
  val type: CodeDescription,
)

@Schema(description = "The data held in NOMIS about a email address")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OffenderEmailAddress(
  @Schema(description = "Unique NOMIS Id of email address")
  val emailAddressId: Long,
  @Schema(description = "The email address", example = "john.smith@internet.co.uk")
  val email: String,
)

@Schema(description = "Nationality details held against a booking")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OffenderNationality(
  @Schema(description = "The booking's unique identifier", example = "1234567")
  val bookingId: Long,
  @Schema(description = "The value of the profile info")
  val nationality: CodeDescription,
  @Schema(description = "The start date of the booking", example = "2020-07-17T12:34:56")
  val startDateTime: LocalDateTime,
  @Schema(description = "The end date of the booking, or null if the booking is still active", example = "2021-07-16T12:34:56")
  val endDateTime: LocalDateTime?,
  @Schema(description = "Whether this is the latest booking or not. Note that latest does not imply active.", example = "true")
  val latestBooking: Boolean,
)

@Schema(description = "Further nationality details held against a booking")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OffenderNationalityDetails(
  @Schema(description = "The booking's unique identifier", example = "1234567")
  val bookingId: Long,
  @Schema(description = "Details on the nationality")
  val details: String,
  @Schema(description = "The start date of the booking", example = "2020-07-17T12:34:56")
  val startDateTime: LocalDateTime,
  @Schema(description = "The end date of the booking, or null if the booking is still active", example = "2021-07-16T12:34:56")
  val endDateTime: LocalDateTime?,
  @Schema(description = "Whether this is the latest booking or not. Note that latest does not imply active.", example = "true")
  val latestBooking: Boolean,
)

@Schema(description = "Sexual orientation details held against a booking")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OffenderSexualOrientation(
  @Schema(description = "The booking's unique identifier", example = "1234567")
  val bookingId: Long,
  @Schema(description = "The value of the profile info")
  val sexualOrientation: CodeDescription,
  @Schema(description = "The start date of the booking", example = "2020-07-17T12:34:56")
  val startDateTime: LocalDateTime,
  @Schema(description = "The end date of the booking, or null if the booking is still active", example = "2021-07-16T12:34:56")
  val endDateTime: LocalDateTime?,
  @Schema(description = "Whether this is the latest booking or not. Note that latest does not imply active.", example = "true")
  val latestBooking: Boolean,
)

@Schema(description = "Disability details held against a booking")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OffenderDisability(
  @Schema(description = "The booking's unique identifier", example = "1234567")
  val bookingId: Long,
  @Schema(description = "The value of the profile info")
  val disability: Boolean,
  @Schema(description = "The start date of the booking", example = "2020-07-17T12:34:56")
  val startDateTime: LocalDateTime,
  @Schema(description = "The end date of the booking, or null if the booking is still active", example = "2021-07-16T12:34:56")
  val endDateTime: LocalDateTime?,
  @Schema(description = "Whether this is the latest booking or not. Note that latest does not imply active.", example = "true")
  val latestBooking: Boolean,
)

@Schema(description = "Interest to immigration details held against a booking")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OffenderInterestToImmigration(
  @Schema(description = "The booking's unique identifier", example = "1234567")
  val bookingId: Long,
  @Schema(description = "The value of the profile info")
  val interestToImmigration: Boolean,
  @Schema(description = "The start date of the booking", example = "2020-07-17T12:34:56")
  val startDateTime: LocalDateTime,
  @Schema(description = "The end date of the booking, or null if the booking is still active", example = "2021-07-16T12:34:56")
  val endDateTime: LocalDateTime?,
  @Schema(description = "Whether this is the latest booking or not. Note that latest does not imply active.", example = "true")
  val latestBooking: Boolean,
)

@Schema(description = "Offender beliefs")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OffenderBelief(
  @Schema(description = "Offender belief id", example = "1123456")
  val beliefId: Long,
  @Schema(description = "Belief", example = "SCIE")
  val belief: CodeDescription,
  @Schema(description = "Date the belief started", example = "2024-01-01")
  val startDate: LocalDate,
  @Schema(description = "Date the belief ended", example = "2024-12-12")
  val endDate: LocalDate? = null,
  @Schema(description = "Was a reason given for change of belief?")
  val changeReason: Boolean? = null,
  @Schema(description = "Comments describing reason for change of belief")
  val comments: String? = null,
  @Schema(description = "Verified flag")
  val verified: Boolean,
  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)

@Schema(description = "Offender address usage")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OffenderAddressUsage(
  @Schema(description = "Offender belief id", example = "1123456")
  val addressId: Long,
  @Schema(description = "Address usage")
  val usage: CodeDescription,
  @Schema(description = "Whether the address usage is active")
  val active: Boolean,
)
