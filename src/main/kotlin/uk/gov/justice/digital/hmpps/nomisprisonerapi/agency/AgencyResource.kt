package uk.gov.justice.digital.hmpps.nomisprisonerapi.agency

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
import java.time.LocalDate

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
class AgencyResource(private val agencyService: AgencyService) {

  @GetMapping("/agency/{agencyId}")
  @Operation(
    summary = "Gets details of a agency",
    description = "Requires ROLE_NOMIS_AGENCYER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Agency details",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Agency not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getAgency(
    @PathVariable
    @Schema(description = "Agency id (aka agencyId)", example = "WWI")
    agencyId: String,
  ) = agencyService.getAgencyLocation(agencyId)
}

@Schema(description = "A response to get an agency that is not a prison")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AgencyResponse(
  @Schema(description = "The agency id", example = "LCSY02")
  val agencyId: String,
  @Schema(description = "Name of agency", example = "Blackburn YOT")
  val description: String,
  @Schema(description = "Description of agency", example = "Blackburn YOT")
  val longDescription: String?,
  @Schema(description = "Geographic district")
  val district: CodeDescription?,
  @Schema(description = "Agency type")
  val type: CodeDescription,
  @Schema(description = "Indicates if still used", example = "true")
  val active: Boolean,
  @Schema(description = "Date no longer active", example = "2020-01-01")
  val deactivationDate: LocalDate?,
  @Schema(description = "Indicates if data is allowed to be updated", example = "true")
  val updateAllowed: Boolean,
  @Schema(description = "Name of contact at agency", example = "John Smith")
  val contactName: String?,
  // maybe move this to court sub entity in future
  @Schema(description = "Court type")
  val courtType: CodeDescription?,
  @Schema(description = "Disability access code", example = "Y")
  val disabilityAccessCode: String?,
  @Schema(description = "Area")
  val area: CodeDescription?,
  @Schema(description = "Sub-Area")
  val subArea: CodeDescription?,
  @Schema(description = "Region")
  val region: CodeDescription?,
  @Schema(description = "NOMS Region")
  val nomsRegion: CodeDescription?,
  @Schema(description = "Payroll Region")
  val payrollRegion: CodeDescription?,
  @Schema(description = "CJIT code", example = "D62L087")
  val cjitCode: String?,
  @Schema(description = "Local Authorities")
  val localAuthorities: List<CodeDescription>,
  @Schema(description = "Addresses")
  val addresses: List<AgencyAddress>,
  @Schema(description = "Phone numbers")
  val phones: List<AgencyPhoneNumber>,
  @Schema(description = "Email addresses")
  val emailAddresses: List<AgencyEmailAddress>,
)

@Schema(description = "The data held in NOMIS about a phone number")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AgencyPhoneNumber(
  @Schema(description = "Unique NOMIS Id of number")
  val id: Long,
  @Schema(description = "The number")
  val number: String,
  @Schema(description = "Extension")
  val extension: String?,
  @Schema(description = "Phone type")
  val type: CodeDescription,
)

@Schema(description = "The data held in NOMIS about an email address")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AgencyEmailAddress(
  @Schema(description = "Unique NOMIS Id of email address")
  val id: Long,
  @Schema(description = "The email address", example = "john.smith@internet.co.uk")
  val emailAddress: String,
)

@Schema(description = "The data held in NOMIS about an address")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AgencyAddress(
  @Schema(description = "Unique NOMIS Id of number")
  val id: Long,
  @Schema(description = "Address type")
  val type: CodeDescription?,
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
  val phoneNumbers: List<AgencyPhoneNumber>,
  @Schema(description = "true if address validated by Post Office Address file??")
  val validatedPAF: Boolean,
  @Schema(description = "true if address not fixed. for example homeless")
  val noFixedAddress: Boolean?,
  @Schema(description = "true if this is the agency's primary address")
  val primaryAddress: Boolean,
  @Schema(description = "true if this is used for mail")
  val mailAddress: Boolean,
  @Schema(description = "Free format comment about the address")
  val comment: String?,
  @Schema(description = "Date address was valid from")
  val startDate: LocalDate?,
  @Schema(description = "Date address was valid to")
  val endDate: LocalDate?,
)
