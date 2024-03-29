package uk.gov.justice.digital.hmpps.nomisprisonerapi.incidents

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentStatus
import java.time.LocalDate
import java.time.LocalDateTime

@RestController
@Validated
@RequestMapping(value = ["/incidents"], produces = [MediaType.APPLICATION_JSON_VALUE])
class IncidentResource(private val incidentService: IncidentService) {

  @PreAuthorize("hasRole('ROLE_NOMIS_INCIDENTS')")
  @GetMapping("/ids")
  @Operation(
    summary = "get incident IDs by filter",
    description = "Retrieves a paged list of incident ids by filter. Requires ROLE_NOMIS_INCIDENTS.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Pageable list of ids are returned",
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
        description = "Forbidden to access this endpoint when role NOMIS_INCIDENTS not present",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getIdsByFilter(
    pageRequest: Pageable,
    @RequestParam(value = "fromDate", required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(
      description = "Filter results by those that were created on or after the given date",
      example = "2021-11-03",
    )
    fromDate: LocalDate?,
    @RequestParam(value = "toDate", required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(
      description = "Filter results by those that were created on or before the given date",
      example = "2021-11-03",
    )
    toDate: LocalDate?,
  ): Page<IncidentIdResponse> =
    incidentService.findIdsByFilter(
      pageRequest = pageRequest,
      IncidentFilter(
        toDate = toDate,
        fromDate = fromDate,
      ),
    )

  @PreAuthorize("hasRole('ROLE_NOMIS_INCIDENTS')")
  @GetMapping("/{incidentId}")
  @Operation(
    summary = "Get incident details",
    description = "Gets incident details. Requires role NOMIS_INCIDENTS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "OK",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires role NOMIS_INCIDENTS",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Not found",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
    ],
  )
  fun getIncident(
    @Schema(description = "Incident id") @PathVariable incidentId: Long,
  ) = incidentService.getIncident(incidentId)
}

@Schema(description = "Incident Details")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class IncidentResponse(
  @Schema(description = "The incident id")
  val incidentId: Long,
  @Schema(description = "The id of the questionnaire associated with this incident")
  val questionnaireId: Long,
  @Schema(description = "A summary of the incident")
  val title: String?,
  @Schema(description = "The incident details")
  val description: String?,
  @Schema(description = "Prison where the incident occurred")
  val prison: CodeDescription,

  @Schema(description = "Status details")
  val status: IncidentStatus,
  @Schema(description = "The incident questionnaire type")
  val type: String,

  @Schema(description = "If the response is locked ie if the response is completed")
  val lockedResponse: Boolean,

  @Schema(description = "The date and time of the incident")
  val incidentDateTime: LocalDateTime,

  @Schema(description = "The staff member who reported the incident")
  val reportingStaff: Staff,
  @Schema(description = "The date and time the incident was reported")
  val reportedDateTime: LocalDateTime,

  @Schema(description = "Staff involved in the incident")
  val staffParties: List<StaffParty>,

  @Schema(description = "Offenders involved in the incident")
  val offenderParties: List<OffenderParty>,

  @Schema(description = "Requirements for completing the incident report")
  val requirements: List<Requirement>,

  @Schema(description = "Questions asked for the incident")
  val questions: List<Question>,

  @Schema(description = "Historical questionnaire details for the incident")
  val history: List<History>,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Staff(
  @Schema(description = "Username of first account related to staff")
  val username: String,
  @Schema(description = "NOMIS staff id")
  val staffId: Long,
  @Schema(description = "First name of staff member")
  val firstName: String,
  @Schema(description = "Last name of staff member")
  val lastName: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class StaffParty(
  @Schema(description = "Staff involved in the incident")
  val staff: Staff,
  @Schema(description = "Staff role in the incident")
  val role: CodeDescription,
  @Schema(description = "General information about the incident")
  val comment: String?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Offender(
  @Schema(description = "NOMIS id")
  val offenderNo: String,
  @Schema(description = "First name of staff member")
  val firstName: String?,
  @Schema(description = "Last name of staff member")
  val lastName: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OffenderParty(
  @Schema(description = "Offender involved in the incident")
  val offender: Offender,
  @Schema(description = "Offender role in the incident")
  val role: CodeDescription,
  @Schema(description = "The outcome of the incident")
  val outcome: CodeDescription?,
  @Schema(description = "General information about the incident")
  val comment: String?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Requirement(
  @Schema(description = "The update required to the incident report")
  val comment: String?,
  @Schema(description = "Date the requirement was recorded")
  val date: LocalDate,
  @Schema(description = "The staff member who made the requirement request")
  val staff: Staff,
  @Schema(description = "The reporting location of the staff")
  val prisonId: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Question(
  @Schema(description = "The questionnaire question id")
  val questionId: Long,
  @Schema(description = "The sequence number of the question for this incident")
  val sequence: Int,
  @Schema(description = "The Question being asked")
  val question: String,
  @Schema(description = "List of Responses to this question")
  val answers: List<Response> = listOf(),
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Response(
  @Schema(description = "The id of the questionnaire question answer")
  val questionResponseId: Long?,
  @Schema(description = "The sequence number of the response for this incident")
  val sequence: Int,
  @Schema(description = "The answer text")
  val answer: String?,
  @Schema(description = "Comment added to the response by recording staff")
  val comment: String?,
  @Schema(description = "Recording staff")
  val recordingStaff: Staff,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class History(
  @Schema(description = "The history questionnaire id for the incident")
  val questionnaireId: Long,
  @Schema(description = "The questionnaire type")
  val type: String,
  @Schema(description = "The questionnaire description")
  val description: String?,
  @Schema(description = "Questions asked for the questionnaire")
  val questions: List<HistoryQuestion>,
  @Schema(description = "When the questionnaire was changed")
  val incidentChangeDate: LocalDate,
  @Schema(description = "Who changed the questionnaire")
  val incidentChangeStaff: Staff,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class HistoryQuestion(
  @Schema(description = "The sequence number of the response question for this incident")
  val questionId: Long,
  @Schema(description = "The sequence number of the question for this incident")
  val sequence: Int,
  @Schema(description = "The Question being asked")
  val question: String,
  @Schema(description = "Historical list of Responses to this question")
  val answers: List<HistoryResponse> = listOf(),
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class HistoryResponse(
  @Schema(description = "The id of the questionnaire question answer")
  val questionResponseId: Long?,
  @Schema(description = "The sequence number of the response for this incident")
  val responseSequence: Int,
  @Schema(description = "The answer text")
  val answer: String?,
  @Schema(description = "Comment added to the response by recording staff")
  val comment: String?,
  @Schema(description = "Recording staff")
  val recordingStaff: Staff,
)
