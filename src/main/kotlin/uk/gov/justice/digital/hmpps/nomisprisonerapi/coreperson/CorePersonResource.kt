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
  @Schema(description = "The offender id. This will change if a different alias is made current")
  val offenderId: Long,
  @Schema(description = "Title of the person")
  val title: CodeDescription?,
  @Schema(description = "First name of the person")
  val firstName: String,
  @Schema(description = "Middle name of the person")
  val middleName1: String?,
  @Schema(description = "Second middle name of the person")
  val middleName2: String?,
  @Schema(description = "Surname name of the person")
  val lastName: String,
  @Schema(description = "Date of birth of the person")
  val dateOfBirth: LocalDate?,
  @Schema(description = "Birth place of the person")
  val birthPlace: String?,
  @Schema(description = "Race of the person")
  val race: CodeDescription?,
  @Schema(description = "Sex of the person")
  val sex: CodeDescription?,
  @Schema(description = "List of aliases for the person. These are the other offender records.")
  val aliases: List<Alias>,
  @Schema(description = "List of identifiers for the person.")
  val identifiers: List<Identifier>,
  @Schema(description = "List of addresses for the person")
  val addresses: List<OffenderAddress>,
  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)

@Schema(description = "The data held in NOMIS for an offender alias")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Alias(
  @Schema(description = "The offender id")
  val offenderId: Long,
  @Schema(description = "Title of the person")
  val title: CodeDescription?,
  @Schema(description = "First name of the person")
  val firstName: String,
  @Schema(description = "Middle name of the person")
  val middleName1: String?,
  @Schema(description = "Second middle name of the person")
  val middleName2: String?,
  @Schema(description = "Surname name of the person")
  val lastName: String,
  @Schema(description = "Date of birth of the person")
  val dateOfBirth: LocalDate?,
  @Schema(description = "Race of the person")
  val race: CodeDescription?,
  @Schema(description = "Sex of the person")
  val sex: CodeDescription?,
  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)

@Schema(description = "The data held in NOMIS for an offender's identifiers")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Identifier(
  @Schema(description = "Unique NOMIS sequence for this identifier for this person")
  val sequence: Long,
  @Schema(description = "The offender id")
  val offenderId: Long,
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
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OffenderAddress(
  @Schema(description = "Unique NOMIS Id of number")
  val addressId: Long,
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
