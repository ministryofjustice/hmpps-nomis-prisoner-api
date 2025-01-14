package uk.gov.justice.digital.hmpps.nomisprisonerapi.corporates

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
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

@Schema(description = "The data held in NOMIS about a address number")
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
  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)

@Schema(description = "The data held in NOMIS about a internetAddress address")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CorporateInternetAddress(
  @Schema(description = "Unique NOMIS Id of internetAddress address")
  val id: Long,
  @Schema(description = "The internetAddress address", example = "john.smith@internet.co.uk")
  val internetAddress: String,
  @Schema(description = "Type of address", examples = ["WEB", "EMAIL"])
  val type: String,
  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)
