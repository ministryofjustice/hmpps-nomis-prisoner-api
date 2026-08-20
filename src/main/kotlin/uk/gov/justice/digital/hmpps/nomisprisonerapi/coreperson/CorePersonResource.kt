package uk.gov.justice.digital.hmpps.nomisprisonerapi.coreperson

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
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.NomisAudit
import java.time.LocalDate

@RestController
@Validated
@PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
@RequestMapping("/core-person", produces = [MediaType.APPLICATION_JSON_VALUE])
class CorePersonResource(private val corePersonService: CorePersonService) {
  @GetMapping("/{prisonNumber}")
  @Operation(
    summary = "Get an offender by prison number",
    description = "Retrieves an offender. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Core person information returned",
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

  @GetMapping("{offenderId}/identifier/{sequenceNumber}")
  @Operation(
    summary = "Get an identifier by offender id and sequence number",
    description = "Retrieves an offender identifier. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Identifier returned",
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
        description = "Identifier or offender does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getIdentifier(
    @Schema(description = "The offender id", example = "1234567")
    @PathVariable offenderId: Long,
    @Schema(description = "The sequence number", example = "3")
    @PathVariable sequenceNumber: Int,
  ) = corePersonService.getIdentifier(offenderId, sequenceNumber)

  @GetMapping("alias/{offenderId}")
  @Operation(
    summary = "Get an alias by offender id",
    description = "Retrieves an alias. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Alias returned",
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
        description = "Alias does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getAlias(
    @Schema(description = "The offender id", example = "1234567")
    @PathVariable offenderId: Long,
  ): CoreOffender = corePersonService.getAlias(offenderId)

  @GetMapping("{prisonNumber}/aliases-identifiers")
  @Operation(
    summary = "Get the aliases and identifiers for an offender by prison number",
    description = "Retrieves the aliases and offenders for an offender. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Identifier returned",
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
  fun getOffenderAliasesAndIdentifiers(
    @Schema(
      description = "Prison number aka noms id / offender id display",
      example = "A1234BC",
    ) @PathVariable prisonNumber: String,
  ) = corePersonService.getOffenderAliasesAndIdentifiers(prisonNumber)

  @GetMapping("/{prisonNumber}/religions")
  @Operation(
    summary = "Get all the religion information for an offender by prison number",
    description = "Retrieves the religion information for an offender. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Core religion information returned",
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
  fun getOffenderReligionsByPrisonNumber(
    @Schema(
      description = "Prison number aka noms id / offender id display",
      example = "A1234BC",
    ) @PathVariable prisonNumber: String,
  ): List<OffenderBelief> = corePersonService.getOffenderReligions(prisonNumber)

  @PostMapping("/{prisonNumber}/merge")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Update the offender by prison number as a result of a merge",
    description = "Updates the offender information as a result of a merge. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
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
  fun updateOffenderByPrisonNumberAfterMerge(
    @Schema(
      description = "Prison number aka noms id / offender id display",
      example = "A1234BC",
    ) @PathVariable prisonNumber: String,
    @RequestBody @Valid
    request: CorePersonMergeRequest,
  ) {
    corePersonService.updateOffenderAfterMerge(prisonNumber, request)
  }
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
  val offenders: List<CoreOffender>?,
  @Schema(description = "Current belief and history of all beliefs for the person")
  val beliefs: List<OffenderBelief>?,
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
  @Schema(description = "Name type of this offender record")
  val nameType: CodeDescription?,
  @Schema(description = "Date this offender record was created. This is separate from the CREATE_DATETIME audit column.")
  val createDate: LocalDate?,
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
  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)

@Schema(description = "Update request for offender merge")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CorePersonMergeRequest(
  @Schema(description = "List of religions to be updated for the offender after a merge")
  val religions: List<CorePersonReligionRequest>,
)

@Schema(description = "Update request for offender belief merge")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CorePersonReligionRequest(
  @Schema(description = "Offender belief id", example = "1123456")
  val beliefId: Long,
  @Schema(description = "Date the belief ended", example = "2024-12-12")
  val endDate: LocalDate? = null,
)
