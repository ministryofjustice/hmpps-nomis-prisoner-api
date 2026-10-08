package uk.gov.justice.digital.hmpps.nomisprisonerapi.agency

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
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
    summary = "Gets details of an agency",
    description = "Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
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

  @GetMapping("/agency/ids/all")
  @Operation(
    summary = "Gets a list of all agency ids",
    description = "Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Agency ids",
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
    ],
  )
  fun getAllAgencies(
    @Schema(description = "Agency types to exclude", example = "INST")
    @RequestParam(required = false) excludeType: List<String> = listOf(),
  ) = agencyService.getAllAgencies(excludeType)

  @PostMapping("/agency/{agencyId}/emails")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Create an agency email address",
    description = "Creates a new email address for an agency. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Agency email address created",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = CreateAgencyEmailAddressResponse::class),
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
  fun createAgencyEmail(
    @PathVariable
    @Schema(description = "Agency id (aka agencyId)", example = "WWI")
    agencyId: String,
    @RequestBody @Valid
    request: CreateAgencyEmailAddressRequest,
  ) = agencyService.createAgencyEmail(agencyId, request)

  @PostMapping("/agency/{agencyId}/phones")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Create an agency phone number",
    description = "Creates a new phone number for an agency, stored at the agency level (not the address level). Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Agency phone number created",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = CreateAgencyPhoneNumberResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "The request contains bad data, for example the phone type code does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
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
  fun createAgencyPhone(
    @PathVariable
    @Schema(description = "Agency id ", example = "WWI")
    agencyId: String,
    @RequestBody @Valid
    request: CreateAgencyPhoneNumberRequest,
  ) = agencyService.createAgencyPhone(agencyId, request)

  @PostMapping("/agency/{agencyId}/addresses")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Create an agency address",
    description = "Creates a new address for an agency. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Agency address created",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = CreateAgencyAddressResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "The request contains bad data, for example the default BUS address type reference data is missing",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
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
  fun createAgencyAddress(
    @PathVariable
    @Schema(description = "Agency id", example = "WWI")
    agencyId: String,
    @RequestBody @Valid
    request: CreateAgencyAddressRequest,
  ) = agencyService.createAgencyAddress(agencyId, request)

  @PutMapping("/agency/{agencyId}/emails")
  @Operation(
    summary = "Refreshes the list of agency email addresses",
    description = "Replaces the existing list of email addresses for an agency with the list supplied. Where possible, existing email addresses are updated in place so their ids are preserved; any extra existing email addresses are removed and any extra new ones are created. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Agency email addresses updated",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = AgencyEmailAddressesResponse::class),
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
  fun updateAgencyEmailAddresses(
    @PathVariable
    @Schema(description = "Agency id", example = "WWI")
    agencyId: String,
    @RequestBody @Valid
    request: UpdateAgencyEmailAddressesRequest,
  ) = agencyService.updateAgencyEmailAddresses(agencyId, request)

  @PutMapping("/agency/{agencyId}/phones")
  @Operation(
    summary = "Refreshes the list of agency phone numbers",
    description = "Replaces the existing list of agency-level phone numbers for an agency with the list supplied. Where possible, existing phone numbers are updated in place so their ids are preserved; any extra existing phone numbers are removed and any extra new ones are created. If a requested phone number already exists against one of the agency's addresses it is left unchanged there and is not duplicated at the agency level. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Agency phone numbers updated",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = AgencyPhoneNumbersResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "The request contains bad data, for example the phone type code does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
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
  fun updateAgencyPhoneNumbers(
    @PathVariable
    @Schema(description = "Agency id", example = "WWI")
    agencyId: String,
    @RequestBody @Valid
    request: UpdateAgencyPhoneNumbersRequest,
  ) = agencyService.updateAgencyPhoneNumbers(agencyId, request)

  @PutMapping("/agency/{agencyId}/addresses")
  @Operation(
    summary = "Refreshes the list of agency addresses",
    description = "Replaces the existing list of addresses for an agency with the list supplied. Where there is a single existing address and a single requested address they are matched directly and updated in place so the id is preserved. Where there is more than one address, there is no id in the request to correlate addresses with, so existing and requested addresses are instead matched by exact postcode; where more than one address shares the same postcode they are matched in the order the existing addresses were created. A null or blank postcode never matches anything, even another null postcode, to avoid silently reassigning the identity of an unrelated address. Any requested address that cannot be matched to an existing one results in a new address being created, defaulting its address type to BUS; any existing address that is not matched by a requested one is removed. City, county and country are looked up by description, matched case insensitively; if a description does not match any reference data the value is left unset on the address and the lookup failure is recorded in the response telemetry. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Agency addresses updated",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = AgencyAddressesResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "The request contains bad data, for example the default BUS address type reference data is missing",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
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
  fun updateAgencyAddresses(
    @PathVariable
    @Schema(description = "Agency id", example = "WWI")
    agencyId: String,
    @RequestBody @Valid
    request: UpdateAgencyAddressesRequest,
  ) = agencyService.updateAgencyAddresses(agencyId, request)
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

@Schema(description = "A response to get agency id")
data class AgencyId(
  @Schema(description = "The agency id", example = "LCSY02")
  val agencyId: String,
)

@Schema(description = "A response to get agency ids")
data class AgencyIdsResponse(
  @Schema(description = "The agency ids")
  val agencyIds: List<AgencyId>,
)

@Schema(description = "A request to create an agency email address")
data class CreateAgencyEmailAddressRequest(
  @Schema(description = "The email address", example = "john.smith@internet.co.uk")
  val emailAddress: String,
)

@Schema(description = "A response to creating an agency email address")
data class CreateAgencyEmailAddressResponse(
  @Schema(description = "Unique NOMIS Id of email address")
  val id: Long,
)

@Schema(description = "A request to create an agency phone number")
data class CreateAgencyPhoneNumberRequest(
  @Schema(description = "The number", example = "0114 555 555")
  val number: String,
  @Schema(description = "Extension", example = "x432")
  val extension: String? = null,
  @Schema(description = "Phone type code", example = "BUS")
  val typeCode: String,
)

@Schema(description = "A response to creating an agency phone number")
data class CreateAgencyPhoneNumberResponse(
  @Schema(description = "Unique NOMIS Id of phone")
  val id: Long,
)

@Schema(description = "A request to refresh and replace the list of email addresses for an agency")
data class UpdateAgencyEmailAddressesRequest(
  @Schema(description = "The complete list of email addresses to hold against the agency, replacing any existing list", example = "[\"john.smith@internet.co.uk\"]")
  val emailAddresses: List<String>,
)

@Schema(description = "A response to refreshing the list of email addresses for an agency")
data class AgencyEmailAddressesResponse(
  @Schema(description = "The list of email addresses now held for the agency")
  val emailAddresses: List<AgencyEmailAddress>,
)

@Schema(description = "A phone number to be held against an agency")
data class UpdateAgencyPhoneNumber(
  @Schema(description = "The number", example = "0114 555 555")
  val number: String,
  @Schema(description = "Extension", example = "x432")
  val extension: String? = null,
  @Schema(description = "Phone type code", example = "BUS")
  val typeCode: String,
)

@Schema(description = "A request to refresh and replace the list of agency-level phone numbers for an agency")
data class UpdateAgencyPhoneNumbersRequest(
  @Schema(description = "The complete list of phone numbers to hold against the agency, replacing any existing agency-level list. Any number that already exists against one of the agency's addresses will be left unchanged there and excluded from the agency-level list")
  val phoneNumbers: List<UpdateAgencyPhoneNumber>,
)

@Schema(description = "A response to refreshing the list of phone numbers for an agency")
data class AgencyPhoneNumbersResponse(
  @Schema(description = "The list of agency-level phone numbers now held for the agency")
  val phoneNumbers: List<AgencyPhoneNumber>,
)

@Schema(description = "A request to create an agency address")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateAgencyAddressRequest(
  @Schema(description = "Flat name or number", example = "Apartment 3")
  val flat: String? = null,
  @Schema(description = "Premise", example = "22")
  val premise: String? = null,
  @Schema(description = "Street", example = "West Street")
  val street: String? = null,
  @Schema(description = "Locality", example = "Keighley")
  val locality: String? = null,
  @Schema(description = "Post code", example = "MK15 2ST")
  val postcode: String? = null,
  @Schema(description = "City description, matched case insensitively. If not found the city will not be set", example = "Sheffield")
  val city: String? = null,
  @Schema(description = "County description, matched case insensitively. If not found the county will not be set", example = "South Yorkshire")
  val county: String? = null,
  @Schema(description = "Country description, matched case insensitively. If not found the country will not be set", example = "England")
  val country: String? = null,
)

@Schema(description = "A response to creating an agency address")
data class CreateAgencyAddressResponse(
  @Schema(description = "Unique NOMIS Id of address")
  val id: Long,
)

@Schema(description = "An address to be held against an agency")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class UpdateAgencyAddress(
  @Schema(description = "Flat name or number", example = "Apartment 3")
  val flat: String? = null,
  @Schema(description = "Premise", example = "22")
  val premise: String? = null,
  @Schema(description = "Street", example = "West Street")
  val street: String? = null,
  @Schema(description = "Locality", example = "Keighley")
  val locality: String? = null,
  @Schema(description = "Post code", example = "MK15 2ST")
  val postcode: String? = null,
  @Schema(description = "City description, matched case insensitively. If not found the city will not be set", example = "Sheffield")
  val city: String? = null,
  @Schema(description = "County description, matched case insensitively. If not found the county will not be set", example = "South Yorkshire")
  val county: String? = null,
  @Schema(description = "Country description, matched case insensitively. If not found the country will not be set", example = "England")
  val country: String? = null,
)

@Schema(description = "A request to refresh and replace the list of addresses for an agency")
data class UpdateAgencyAddressesRequest(
  @Schema(description = "The complete list of addresses to hold against the agency, replacing any existing list. If there is a single existing address and a single requested address they are matched directly; otherwise existing and requested addresses are matched by exact postcode, with a null or blank postcode never matching")
  val addresses: List<UpdateAgencyAddress>,
)

@Schema(description = "A response to refreshing the list of addresses for an agency")
data class AgencyAddressesResponse(
  @Schema(description = "The list of addresses now held for the agency")
  val addresses: List<AgencyAddress>,
)
