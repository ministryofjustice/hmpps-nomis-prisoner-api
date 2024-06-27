package uk.gov.justice.digital.hmpps.nomisprisonerapi.casenotes

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
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
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import java.time.LocalDateTime

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class CaseNotesResource(
  private val caseNotesService: CaseNotesService,
) {

  @PreAuthorize("hasRole('ROLE_NOMIS_CASENOTES')")
  @GetMapping("/casenotes/{caseNoteId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "get a case note by id",
    description = "Retrieves a prisoner case note. Requires ROLE_NOMIS_CASENOTES",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CASENOTES",
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

  @PreAuthorize("hasRole('ROLE_NOMIS_CASENOTES')")
  @PostMapping("/prisoners/{offenderNo}/casenotes")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Creates a case note on a prisoner",
    description = "Creates a case note on the prisoner's latest booking. Requires ROLE_NOMIS_CASENOTES",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CASENOTES",
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

  @PreAuthorize("hasRole('ROLE_NOMIS_CASENOTES')")
  @PutMapping("/casenotes/{caseNoteId}")
  @Operation(
    summary = "Amends a case note on a prisoner",
    description = "Updates the specified case note. Requires ROLE_NOMIS_CASENOTES",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "CaseNote Updated",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = CaseNoteResponse::class)),
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CASENOTES",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "CaseNote does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun amendCaseNote(
    @Schema(description = "Case note id", example = "1234567")
    @PathVariable
    caseNoteId: Long,
    @RequestBody @Valid
    request: AmendCaseNoteRequest,
  ): CaseNoteResponse = caseNotesService.amendCaseNote(caseNoteId, request)

  @PreAuthorize("hasRole('ROLE_NOMIS_CASENOTES')")
  @GetMapping("/prisoners/{offenderNo}/casenotes")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Gets all case notes for a prisoner",
    description = "Retrieves all case notes for a specific prisoner, for migration or reconciliation. Requires ROLE_NOMIS_CASENOTES",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CASENOTES",
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

//  @PreAuthorize("hasRole('ROLE_NOMIS_CASENOTES')")
//  @GetMapping("/bookings/ids")
//  @ResponseStatus(HttpStatus.OK)
//  @Operation(
//    summary = "Gets all booking ids",
//    description = "Retrieves all booking ids subject to filters, for migration or reconciliation. Requires ROLE_NOMIS_CASENOTES",
//    responses = [
//      ApiResponse(
//        responseCode = "200",
//        description = "CaseNotes Returned",
//        content = [
//          Content(mediaType = "application/json", schema = Schema(implementation = Page::class)),
//        ],
//      ),
//      ApiResponse(
//        responseCode = "401",
//        description = "Unauthorized to access this endpoint",
//        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
//      ),
//      ApiResponse(
//        responseCode = "403",
//        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_CASENOTES",
//        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
//      ),
//      ApiResponse(
//        responseCode = "404",
//        description = "Prisoner does not exist or has no bookings",
//        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
//      ),
//    ],
//  )
//  fun getBookings(
//    @Schema(description = "Start or minimum booking id", example = "12345678") @RequestParam fromId: Long?,
//    @Schema(description = "End or maximum booking id", example = "98765432") toId: Long?,
//    @Schema(description = "If true return only bookings currently in prison") activeOnly: Boolean,
//    pageable: Pageable,
//  ): Page<BookingIdResponse> = caseNotesService.getAllBookingIds(fromId, toId, activeOnly, pageable)
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
  @Schema(description = "Date case note occurred")
  val occurrenceDateTime: LocalDateTime? = null,
  @Schema(description = "Free format text of person or department that created the case note")
  val authorUsername: String,
  @Schema(description = "Prison id")
  val prisonId: String? = null,
  @Schema(description = "Free format text body of case note")
  var caseNoteText: String? = null,
  @Schema(description = "Whether the case note was amended", example = "false")
  val amended: Boolean,
)

@Schema(description = "A request to create a case note in NOMIS")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateCaseNoteRequest(
  @NotBlank
  @Schema(description = "The case note type")
  val caseNoteType: String,
  @NotBlank
  @Schema(description = "The case note subtype")
  val caseNoteSubType: String,
  @Schema(description = "Date case note occurred")
  val occurrenceDateTime: LocalDateTime,
  @NotBlank
  @Schema(description = "Free format text of person or department that created the case note")
  val authorUsername: String,
  @NotBlank
  @Size(max = 4000) // For Swagger - custom annotations not well supported
  @Schema(description = "Free format text body of case note")
  val caseNoteText: String,
)

@Schema(description = "A response after a case note created in NOMIS")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateCaseNoteResponse(
  @Schema(description = "The id of this case note")
  val id: Long,
)

@Schema(description = "A request to amend a case note in NOMIS")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AmendCaseNoteRequest(
  @NotBlank
  @Schema(description = "The case note type")
  val caseNoteType: String,
  @NotBlank
  @Schema(description = "The case note subtype")
  val caseNoteSubType: String,
  @Schema(description = "Date case note occurred")
  val occurrenceDateTime: LocalDateTime,
  @NotBlank
  @Schema(description = "Free format text of person or department that created the case note")
  val authorUsername: String,
  @NotBlank
  @Size(max = 4000)
  @Schema(description = "Free format text body of case note")
  val caseNoteText: String,
)

data class BookingIdResponse(
  val bookingId: Long,
)
