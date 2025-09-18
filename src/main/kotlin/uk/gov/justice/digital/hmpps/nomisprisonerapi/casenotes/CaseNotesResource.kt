package uk.gov.justice.digital.hmpps.nomisprisonerapi.casenotes

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
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
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.NoteSourceCode
import java.time.LocalDateTime

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
class CaseNotesResource(
  private val caseNotesService: CaseNotesService,
) {

  @GetMapping("/casenotes/{caseNoteId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "get a case note by id",
    description = "Retrieves a prisoner case note. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "CaseNote Information Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CaseNoteResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "CaseNote does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getCaseNote(
    @Schema(description = "Id", example = "1234578")
    @PathVariable
    caseNoteId: Long,
  ): CaseNoteResponse = caseNotesService.getCaseNote(caseNoteId)

  @PostMapping("/prisoners/{offenderNo}/casenotes")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a case note on a prisoner",
    description = "Creates a case note on the prisoner's latest booking. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "CaseNote Created",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CreateCaseNoteResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "One or more fields in the request contains invalid data",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Prisoner does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun createCaseNote(
    @Schema(description = "Offender no (aka prisoner number)", example = "A1234AK")
    @PathVariable
    offenderNo: String,
    @RequestBody @Valid
    request: CreateCaseNoteRequest,
  ): CreateCaseNoteResponse = caseNotesService.createCaseNote(offenderNo, request)

  @PutMapping("/casenotes/{caseNoteId}")
  @Operation(
    summary = "Updates a case note on a prisoner",
    description = "Updates the specified case note. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "200", description = "CaseNote Updated"),
      ApiResponse(
        responseCode = "400",
        description = "One or more fields in the request contains invalid data",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "CaseNote does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun updateCaseNote(
    @Schema(description = "Case note id", example = "1234567")
    @PathVariable
    caseNoteId: Long,
    @RequestBody @Valid
    request: UpdateCaseNoteRequest,
  ) {
    caseNotesService.updateCaseNote(caseNoteId, request)
  }

  @DeleteMapping("/casenotes/{caseNoteId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Deletes a case note",
    description = "Deletes the specified case note. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(responseCode = "201", description = "CaseNote Deleted"),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "CaseNote does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun deleteCaseNote(
    @Schema(description = "Case note id", example = "1234567")
    @PathVariable
    caseNoteId: Long,
  ) {
    caseNotesService.deleteCaseNote(caseNoteId)
  }

  @GetMapping("/prisoners/{offenderNo}/casenotes")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Gets all case notes for a prisoner",
    description = "Retrieves all case notes for a specific prisoner, for migration or merge. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Case notes Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = PrisonerCaseNotesResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Prisoner does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getCaseNotesForPrisoner(
    @Schema(description = "Offender No AKA prisoner number", example = "A3745XD")
    @PathVariable
    offenderNo: String,
  ): PrisonerCaseNotesResponse = caseNotesService.getCaseNotes(offenderNo)

  @GetMapping("/prisoners/{offenderNo}/casenotes/reconciliation")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Gets all case notes for a prisoner for reconciliation.",
    description = """Retrieves all case notes for a specific prisoner, for reconciliation. 
    This endpoint doesn't try to split out a case note into the amendments, simply just returns the text as stored in
    NOMIS. This is because the notes are truncated at 4,000 characters so we can end up with more amendments in DPS
    than there are in NOMIS. We therefore just return the text as stored in NOMIS and then transform the DPS text to
    match.
    Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Case notes Returned",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = PrisonerCaseNotesResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Prisoner does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getCaseNotesForPrisonerForReconciliation(
    @Schema(description = "Offender No AKA prisoner number", example = "A3745XD")
    @PathVariable
    offenderNo: String,
  ): PrisonerCaseNotesResponse = caseNotesService.getCaseNotesForReconciliation(offenderNo)
}

@Schema(description = "The list of case notes held against a booking")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PrisonerCaseNotesResponse(
  val caseNotes: List<CaseNoteResponse>,
)

@Schema(description = "The data held in NOMIS about a case note associated with a prisoner")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CaseNoteResponse(
  @Schema(description = "The primary key")
  val caseNoteId: Long,
  @Schema(description = "The prisoner's bookingId related to this case note")
  val bookingId: Long,
  @Schema(description = "The case note type")
  val caseNoteType: CodeDescription,
  @Schema(description = "The case note subtype")
  val caseNoteSubType: CodeDescription,
  @Schema(description = "Whether system-generated")
  val noteSourceCode: NoteSourceCode? = null,
  @Schema(description = "Datetime case note occurred")
  val occurrenceDateTime: LocalDateTime? = null,
  @Schema(description = "Datetime case note was created by user")
  val creationDateTime: LocalDateTime? = null,

  @Schema(description = "Author STAFF_ID")
  val authorStaffId: Long,
  @Schema(description = "Author username or login name")
  @Deprecated("There could be more than one for this staff id - Use authorUsernames instead")
  val authorUsername: String,
  @Schema(description = "Author first name")
  val authorFirstName: String?,
  @Schema(description = "Author last name")
  val authorLastName: String,
  @Schema(description = "List of all usernames for authorStaffId")
  val authorUsernames: List<String>?,

  @Schema(description = "Prison id")
  val prisonId: String? = null,
  @Schema(description = "Free format text body of case note")
  var caseNoteText: String,

  @Schema(description = "Amendments to the text")
  val amendments: List<CaseNoteAmendment> = mutableListOf(),

  @Schema(description = "Created DB timestamp")
  var createdDatetime: LocalDateTime,
  @Schema(description = "Created DB username")
  var createdUsername: String,

  @Schema(description = "Which screen (or DPS) created the case note", example = "OIDABCDE")
  val auditModuleName: String? = null,

  @Schema(description = "Which system (Nomis or DPS) created the case note", example = "DPS")
  val sourceSystem: SourceSystem,
)

enum class SourceSystem { DPS, NOMIS }

@Schema(description = "A request to create a case note in NOMIS")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateCaseNoteRequest(
  @NotBlank
  @Schema(description = "The case note type")
  val caseNoteType: String,

  @NotBlank
  @Schema(description = "The case note subtype")
  val caseNoteSubType: String,

  @Schema(description = "Date and time case note occurred")
  val occurrenceDateTime: LocalDateTime,

  @Schema(description = "Date and time case note was created")
  val creationDateTime: LocalDateTime,

  @NotBlank
  @Schema(description = "Free format text of person or department that created the case note")
  val authorUsername: String,

  @NotBlank
  @Schema(description = "Free format text body of case note")
  val caseNoteText: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CaseNoteAmendment(
  @Schema(description = "Free format text body of amendment")
  val text: String,

  @Schema(description = "Author login name of person or department that added the amendment")
  val authorUsername: String,

  @Schema(description = "Author STAFF_ID")
  val authorStaffId: Long?,

  @Schema(description = "Author first name")
  val authorFirstName: String?,

  @Schema(description = "Author last name")
  val authorLastName: String?,

  @Schema(description = "Amendment created timestamp")
  val createdDateTime: LocalDateTime,

  @Schema(description = "Which system (Nomis or DPS) created the amendment", example = "DPS")
  val sourceSystem: SourceSystem,
)

@Schema(description = "A response after a case note created in NOMIS")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateCaseNoteResponse(
  @Schema(description = "The id of this case note")
  val id: Long,

  @Schema(description = "The booking id of this case note (which is the prisoner's latest at creation time)")
  val bookingId: Long,
)

@Schema(description = "A request to amend a case note in NOMIS")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class UpdateCaseNoteRequest(
  @NotBlank
  @Schema(description = "Free format text body of the amendment")
  val text: String,

  @Schema(description = "Amendments to the text")
  val amendments: List<UpdateAmendment> = mutableListOf(),
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UpdateAmendment(
  @NotBlank
  @Schema(description = "Free format text body of amendment")
  val text: String,

  @NotBlank
  @Schema(description = "Author login name of person or department that added the amendment")
  val authorUsername: String,

  @Schema(description = "Amendment created timestamp")
  val createdDateTime: LocalDateTime,
)
