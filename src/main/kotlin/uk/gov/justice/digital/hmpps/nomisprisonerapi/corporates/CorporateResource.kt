package uk.gov.justice.digital.hmpps.nomisprisonerapi.corporates

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.NomisAudit
import java.time.LocalDate

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class CorporateResource(private val corporateService: CorporateService) {

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @GetMapping("/corporates/ids")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get all Ids",
    description = "Retrieves all corporate Ids - typically for a migration. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Page of corporate Ids",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CONTACTPERSONS",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getCorporateIds(
    @PageableDefault(size = 20, sort = ["corporateId"], direction = Sort.Direction.ASC)
    pageRequest: Pageable,
    @RequestParam(value = "fromDate")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(
      description = "Filter results by corporate that were created on or after the given date",
      example = "2021-11-03",
    )
    fromDate: LocalDate?,
    @RequestParam(value = "toDate")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(
      description = "Filter results by corporate that were created on or before the given date",
      example = "2021-11-03",
    )
    toDate: LocalDate?,
  ): Page<CorporateOrganisationIdResponse> = corporateService.findCorporateIdsByFilter(
    pageRequest = pageRequest,
    CorporateFilter(
      toDate = toDate,
      fromDate = fromDate,
    ),
  )

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @GetMapping("/corporates/{corporateId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get a corporate by corporateId Id",
    description = "Retrieves a corporate and details. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Corporate Information Returned",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = CorporateOrganisation::class),
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CONTACTPERSONS",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Corporate does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getCorporateById(
    @Schema(description = "Corporate Id", example = "12345")
    @PathVariable
    corporateId: Long,
  ): CorporateOrganisation = corporateService.getCorporateById(corporateId)

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @PostMapping("/corporates")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a corporate organisation",
    description = "Creates a new corporate record. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Corporate creates",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Caseload does not exist",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CONTACTPERSONS",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun createCorporate(
    @RequestBody @Valid
    request: CreateCorporateOrganisationRequest,
  ) = corporateService.createCorporate(request)

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PutMapping("/corporates/{corporateId}")
  @Operation(
    summary = "Update corporate organisation",
    description = "Updates an existing corporate record. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Corporate updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Caseload does not exist",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CONTACTPERSONS",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Corporate does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun updateCorporate(
    @PathVariable
    corporateId: Long,
    @RequestBody @Valid
    request: UpdateCorporateOrganisationRequest,
  ) = corporateService.updateCorporate(corporateId, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/corporates/{corporateId}")
  @Operation(
    summary = "Delete corporate organisation",
    description = "Deletes an existing corporate record. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Corporate deleted or did not exist",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CONTACTPERSONS",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun deleteCorporate(
    @PathVariable
    corporateId: Long,
  ) = corporateService.deleteCorporate(corporateId)

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @PostMapping("/corporates/{corporateId}/address")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a corporate address",
    description = "Creates a new corporate address record. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Corporate address created",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = CreateCorporateAddressResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "The request contains bad data for example type code does not exist",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CONTACTPERSONS",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Corporate does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun createCorporateAddress(
    @PathVariable
    corporateId: Long,
    @RequestBody @Valid
    request: CreateCorporateAddressRequest,
  ) = corporateService.createCorporateAddress(corporateId, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @PutMapping("/corporates/{corporateId}/address/{addressId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Update a corporate address",
    description = "Updates a corporate address record. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Corporate address updated",
      ),
      ApiResponse(
        responseCode = "400",
        description = "The request contains bad data for example type code does not exist",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CONTACTPERSONS",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Corporate or address does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun updateCorporateAddress(
    @PathVariable
    corporateId: Long,
    @PathVariable
    addressId: Long,
    @RequestBody @Valid
    request: UpdateCorporateAddressRequest,
  ) = corporateService.updateCorporateAddress(corporateId = corporateId, addressId = addressId, request = request)

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @DeleteMapping("/corporates/{corporateId}/address/{addressId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Delete a corporate address",
    description = "Deletes a corporate address record. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Corporate address deleted",
      ),
      ApiResponse(
        responseCode = "400",
        description = "The address exists but not for the supplied corporate",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CONTACTPERSONS",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun deleteCorporateAddress(
    @PathVariable
    corporateId: Long,
    @PathVariable
    addressId: Long,
  ) = corporateService.deleteCorporateAddress(corporateId = corporateId, addressId = addressId)

  @PreAuthorize("hasRole('ROLE_NOMIS_CONTACTPERSONS')")
  @PostMapping("/corporates/{corporateId}/address/{addressId}/phone")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a corporate address phone",
    description = "Creates a new corporate address phone record. Requires ROLE_NOMIS_CONTACTPERSONS",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Corporate address phone created",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = CreateCorporatePhoneResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "The request contains bad data for example type code does not exist",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CONTACTPERSONS",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Corporate or address does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun createCorporateAddressPhone(
    @PathVariable
    corporateId: Long,
    @PathVariable
    addressId: Long,
    @RequestBody @Valid
    request: CreateCorporatePhoneRequest,
  ) = corporateService.createCorporateAddressPhone(corporateId = corporateId, addressId = addressId, request = request)
}

@Schema(description = "The data held in NOMIS about a corporate entity")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CorporateOrganisation(
  @Schema(description = "Unique NOMIS Id of corporate")
  val id: Long,
  @Schema(description = "The corporate name", example = "Boots")
  val name: String,
  @Schema(description = "The associated caseload e.g COOKHAM WOOD (HMP)")
  val caseload: CodeDescription? = null,
  @Schema(description = "User comment")
  val comment: String? = null,
  @Schema(description = "Programme number")
  val programmeNumber: String? = null,
  @Schema(description = "VAT number")
  val vatNumber: String? = null,
  @Schema(description = "Is active")
  val active: Boolean = true,
  @Schema(description = "Date made inactive")
  val expiryDate: LocalDate? = null,
  @Schema(description = "List of types this organisation is associated with, for instance ACCOM - Accommodation Provider")
  val types: List<CorporateOrganisationType> = listOf(),
  @Schema(description = "List of phone numbers for the corporate")
  val phoneNumbers: List<CorporatePhoneNumber>,
  @Schema(description = "List of addresses for the corporate")
  val addresses: List<CorporateAddress>,
  @Schema(description = "List of internet addresses for the corporate")
  val internetAddresses: List<CorporateInternetAddress>,
  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)

@Schema(description = "The data held in NOMIS about a corporate entity")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CorporateOrganisationType(
  @Schema(description = "The type of corporate, for instance ACCOM - Accommodation Provider ")
  val type: CodeDescription,
  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)

@Schema(description = "The data held in NOMIS about a phone number")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CorporatePhoneNumber(
  @Schema(description = "Unique NOMIS Id of number")
  val id: Long,
  @Schema(description = "The number")
  val number: String,
  @Schema(description = "Extension")
  val extension: String?,
  @Schema(description = "Phone type")
  val type: CodeDescription,
  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)

@Schema(description = "The data held in NOMIS about an address")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CorporateAddress(
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
  val phoneNumbers: List<CorporatePhoneNumber>,
  @Schema(description = "true if address validated by Post Office Address file??")
  val validatedPAF: Boolean,
  @Schema(description = "true if address not fixed. for example homeless")
  val noFixedAddress: Boolean?,
  @Schema(description = "true if this is the corporate's primary address")
  val primaryAddress: Boolean,
  @Schema(description = "true if this is used for mail")
  val mailAddress: Boolean,
  @Schema(description = "Free format comment about the address")
  val comment: String?,
  @Schema(description = "Date address was valid from")
  val startDate: LocalDate?,
  @Schema(description = "Date address was valid to")
  val endDate: LocalDate?,
  @Schema(description = "True if this is a service organisation")
  val isServices: Boolean,
  @Schema(description = "Business hours")
  val businessHours: String?,
  @Schema(description = "Contact person")
  val contactPersonName: String?,
  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)

@Schema(description = "The data held in NOMIS about a internet address")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CorporateInternetAddress(
  @Schema(description = "Unique NOMIS Id of internet address")
  val id: Long,
  @Schema(description = "The internet address", example = "john.smith@internet.co.uk")
  val internetAddress: String,
  @Schema(description = "Type of address", examples = ["WEB", "EMAIL"])
  val type: String,
  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)

data class CorporateOrganisationIdResponse(
  @Schema(description = "The corporate Id")
  val corporateId: Long,
)

@Schema(description = "Request to create a corporate organisation in NOMIS")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateCorporateOrganisationRequest(
  @Schema(description = "Unique Id of corporate")
  val id: Long,
  @Schema(description = "The corporate name", example = "Boots")
  val name: String,
  @Schema(description = "Is active")
  val active: Boolean = true,
  @Schema(description = "Date made inactive")
  val expiryDate: LocalDate? = null,
  @Schema(description = "The associated caseload code", example = "WWI")
  val caseloadId: String? = null,
  @Schema(description = "User comment")
  val comment: String? = null,
  @Schema(description = "Programme number")
  val programmeNumber: String? = null,
  @Schema(description = "VAT number")
  val vatNumber: String? = null,
)

@Schema(description = "Request to update a corporate organisation in NOMIS")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class UpdateCorporateOrganisationRequest(
  @Schema(description = "The corporate name", example = "Boots")
  val name: String,
  @Schema(description = "Is active")
  val active: Boolean = true,
  @Schema(description = "Date made inactive")
  val expiryDate: LocalDate? = null,
  @Schema(description = "The associated caseload code", example = "WWI")
  val caseloadId: String? = null,
  @Schema(description = "User comment")
  val comment: String? = null,
  @Schema(description = "Programme number")
  val programmeNumber: String? = null,
  @Schema(description = "VAT number")
  val vatNumber: String? = null,
)

@Schema(description = "Request to create a corporate organisation address in NOMIS")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateCorporateAddressRequest(
  @Schema(description = "Address type", example = "BUS")
  val typeCode: String? = null,
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
  @Schema(description = "City code", example = "25343")
  val cityCode: String? = null,
  @Schema(description = "County code", example = "S.YORKSHIRE")
  val countyCode: String? = null,
  @Schema(description = "Country code", example = "ENG")
  val countryCode: String? = null,
  @Schema(description = "true if address not fixed. for example homeless")
  val noFixedAddress: Boolean = false,
  @Schema(description = "true if this is the corporate's primary address")
  val primaryAddress: Boolean = false,
  @Schema(description = "true if this is used for mail")
  val mailAddress: Boolean = false,
  @Schema(description = "Free format comment about the address")
  val comment: String? = null,
  @Schema(description = "Date address was valid from")
  val startDate: LocalDate = LocalDate.now(),
  @Schema(description = "Date address was valid to")
  val endDate: LocalDate? = null,
  @Schema(description = "True if this is a service organisation")
  val isServices: Boolean = false,
  @Schema(description = "Business hours")
  val businessHours: String? = null,
  @Schema(description = "Contact person")
  val contactPersonName: String? = null,
)

data class CreateCorporateAddressResponse(
  @Schema(description = "The address Id")
  val id: Long,
)

@Schema(description = "Request to update a corporate organisation address in NOMIS")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class UpdateCorporateAddressRequest(
  @Schema(description = "Address type", example = "BUS")
  val typeCode: String? = null,
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
  @Schema(description = "City code", example = "25343")
  val cityCode: String? = null,
  @Schema(description = "County code", example = "S.YORKSHIRE")
  val countyCode: String? = null,
  @Schema(description = "Country code", example = "ENG")
  val countryCode: String? = null,
  @Schema(description = "true if address not fixed. for example homeless")
  val noFixedAddress: Boolean = false,
  @Schema(description = "true if this is the corporate's primary address")
  val primaryAddress: Boolean = false,
  @Schema(description = "true if this is used for mail")
  val mailAddress: Boolean = false,
  @Schema(description = "Free format comment about the address")
  val comment: String? = null,
  @Schema(description = "Date address was valid from")
  val startDate: LocalDate = LocalDate.now(),
  @Schema(description = "Date address was valid to")
  val endDate: LocalDate? = null,
  @Schema(description = "True if this is a service organisation")
  val isServices: Boolean = false,
  @Schema(description = "Business hours")
  val businessHours: String? = null,
  @Schema(description = "Contact person")
  val contactPersonName: String? = null,
)

data class CreateCorporatePhoneRequest(
  @Schema(description = "The number", example = "0114 555 555")
  val number: String,
  @Schema(description = "Extension", example = "x432")
  val extension: String? = null,
  @Schema(description = "Phone type code", example = "MOB")
  val typeCode: String,
)

data class UpdateCorporatePhoneRequest(
  @Schema(description = "The number", example = "0114 555 555")
  val number: String,
  @Schema(description = "Extension", example = "x432")
  val extension: String? = null,
  @Schema(description = "Phone type code", example = "MOB")
  val typeCode: String,
)

data class CreateCorporatePhoneResponse(
  @Schema(description = "Unique NOMIS Id of phone")
  val id: Long,
)
