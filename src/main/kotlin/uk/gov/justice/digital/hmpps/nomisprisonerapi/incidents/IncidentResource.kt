package uk.gov.justice.digital.hmpps.nomisprisonerapi.incidents

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentStatus
import java.time.LocalDate
import java.time.LocalDateTime

@RestController
@Validated
@RequestMapping(value = ["/incidents"], produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_NOMIS_INCIDENTS')")
class IncidentResource(private val incidentService: IncidentService) {

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
    @PageableDefault(size = 20)
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
  ): Page<IncidentIdResponse> = incidentService.findIdsByFilter(
    pageRequest = pageRequest,
    IncidentFilter(
      toDate = toDate,
      fromDate = fromDate,
    ),
  )

  @GetMapping("/booking/{bookingId}")
  @Operation(
    summary = "Get a list of Incidents for a booking",
    description = "Gets a list of all incidents relating to an offender booking. Requires role NOMIS_INCIDENTS",
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
  fun getIncidentsForBooking(
    @Schema(description = "booking id")
    @PathVariable
    bookingId: Long,
  ) = incidentService.getIncidentsForBooking(bookingId)

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

  @GetMapping("/reconciliation/agencies")
  @Operation(
    summary = "Retrieve a list of all agencies by id that have raised incidents)",
    description = "Retrieve a list of all agencies by id that have raised incidents, including prisons and PECS. Requires authorised access",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "OK",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [
          Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class)),
        ],
      ),
    ],
  )
  fun getIncidentAgencies() = incidentService.findAllIncidentAgencyIds()

  @GetMapping("/reconciliation/agency/{agencyId}/counts")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Gets incident counts",
    description = "Retrieves open and closed incident counts for an agency.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Reconciliation data returned",
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
    ],
  )
  fun getIncidentCountsForReconciliation(
    @Schema(description = "Agency Id", example = "LEI")
    @PathVariable
    agencyId: String,
  ) = incidentService.getIncidentCountsForReconciliation(agencyId)

  @GetMapping("/reconciliation/agency/{agencyId}/ids")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Gets ids of open incidents at an agency",
    description = "Retrieves paged ids for open incidents for an agency.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Pageable list of reconciliation ids are returned",
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
    ],
  )
  fun getOpenIncidentIdsForReconciliation(
    @PageableDefault
    pageRequest: Pageable,
    @Schema(description = "Agency Id", example = "LEI")
    @PathVariable
    agencyId: String,
  ) = incidentService.getOpenIncidentIdsForReconciliation(agencyId, pageRequest)

  @PutMapping("/{incidentId}")
  @Operation(
    summary = "create or update an incident using the specified id",
    description = "Create or update an incident. Requires ROLE_NOMIS_INCIDENTS",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Incident created or updated",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_INCIDENTS",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Prisoner does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "409",
        description = "Incident already exists",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun upsertIncident(
    @Schema(description = "Incident id") @PathVariable incidentId: Long,
    @RequestBody @Valid
    request: UpsertIncidentRequest,
  ) {
    incidentService.upsertIncident(incidentId, request)
  }

  @DeleteMapping("/{incidentId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Delete an incident using the specified id",
    description = "Delete an incident. Requires ROLE_NOMIS_INCIDENTS",
    responses = [
      ApiResponse(
        responseCode = "204",
        description = "Incident delete",
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
        description = "Forbidden to access this endpoint. Requires ROLE_NOMIS_INCIDENTS",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Incident does not exist",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun deleteIncident(
    @Schema(description = "Incident id") @PathVariable incidentId: Long,
  ) {
    incidentService.deleteIncident(incidentId)
  }
}

@Schema(description = "Incident Request")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class UpsertIncidentRequest(
  @Schema(description = "A summary of the incident")
  val title: String,
  @Schema(description = "The incident details")
  val description: String,
  @Schema(description = "Amendments to the incident details")
  val descriptionAmendments: List<UpsertDescriptionAmendmentRequest>,
  @Schema(description = "Prison where the incident occurred")
  val location: String,
  @Schema(description = "Status details")
  val statusCode: String,
  @Schema(description = "The incident questionnaire type")
  val typeCode: String,
  @Schema(description = "The date and time of the incident")
  val incidentDateTime: LocalDateTime,
  @Schema(description = "The date and time the incident was reported")
  val reportedDateTime: LocalDateTime,
  @Schema(description = "The username of the person who reported the incident")
  val reportedBy: String,
  @Schema(description = "Requirements for completing the incident report")
  val requirements: List<UpsertIncidentRequirementRequest> = listOf(),
  @Schema(description = "Offenders involved in the incident")
  val offenderParties: List<UpsertOffenderPartyRequest> = listOf(),
  @Schema(description = "Staff involved in the incident")
  val staffParties: List<UpsertStaffPartyRequest> = listOf(),
  @Schema(description = "Questions asked for the incident")
  val questions: List<UpsertIncidentQuestionRequest> = listOf(),
  @Schema(description = "Historical questionnaire details for the incident")
  val history: List<UpsertIncidentHistoryRequest> = listOf(),
)

data class UpsertDescriptionAmendmentRequest(
  @Schema(description = "When addendum was added", example = "2024-04-29T12:34:56.789012")
  val createdDateTime: LocalDateTime,
  @Schema(description = "First name of person that added this addendum", example = "John")
  val firstName: String,
  @Schema(description = "Last name of person that added this addendum", example = "Doe")
  val lastName: String,
  @Schema(description = "Addendum text")
  val text: String,
)

data class UpsertIncidentRequirementRequest(
  @Schema(description = "The update required to the incident report")
  val comment: String?,
  @Schema(description = "Date the requirement was recorded")
  val date: LocalDateTime,
  @Schema(description = "The staff member who made the requirement request")
  val username: String,
  @Schema(description = "The reporting agency of the staff")
  val location: String,
)

data class UpsertOffenderPartyRequest(
  @Schema(description = "Offender involved in the incident")
  val prisonNumber: String,
  @Schema(description = "Offender role in the incident")
  val role: String,
  @Schema(description = "The outcome of the incident")
  val outcome: String?,
  @Schema(description = "General information about the incident")
  val comment: String?,
)

data class UpsertStaffPartyRequest(
  @Schema(description = "Staff involved in the incident")
  val username: String,
  @Schema(description = "Staff role in the incident")
  val role: String,
  @Schema(description = "The outcome of the incident")
  val outcome: String?,
  @Schema(description = "General information about the incident")
  val comment: String?,
)

data class UpsertIncidentQuestionRequest(
  @Schema(description = "The questionnaire question id")
  val questionId: Long,
  @Schema(description = "List of Responses to this question")
  val responses: List<UpsertIncidentResponseRequest> = listOf(),
)

data class UpsertIncidentResponseRequest(
  @Schema(description = "The questionnaire answer id")
  val answerId: Long,
  @Schema(description = "Comment added to the response by recording staff")
  val comment: String?,
  @Schema(description = "Response date added to the response by recording staff")
  val responseDate: LocalDate?,
  @Schema(description = "Recording staff")
  val recordingUsername: String,
  @Schema(description = "Sequence number across all responses for an incident")
  val sequence: Int,
)

data class UpsertIncidentHistoryRequest(
  @Schema(description = "The incident questionnaire type")
  val typeCode: String,
  @Schema(description = "When the questionnaire was changed")
  val incidentChangeDateTime: LocalDateTime,
  @Schema(description = "Who changed the questionnaire")
  val incidentChangeUsername: String,
  @Schema(description = "Questions asked for the questionnaire")
  val questions: List<UpsertIncidentQuestionRequest>,
)

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
  @Schema(description = "Agency where the incident occurred")
  val agency: CodeDescription,

  @Schema(description = "Status details")
  val status: IncidentStatus,
  @Schema(description = "The incident questionnaire type")
  val type: String,

  @Schema(description = "If the response is locked ie if the response is completed")
  val lockedResponse: Boolean,

  @Schema(description = "The date and time of the incident")
  val incidentDateTime: LocalDateTime,

  @Schema(description = "The date and time the incident was created")
  val createDateTime: LocalDateTime,
  @Schema(description = "The username of the person who created the incident")
  val createdBy: String,
  @Schema(description = "The date and time the incident was last updated")
  val lastModifiedDateTime: LocalDateTime?,
  @Schema(description = "The username of the person who last updated the incident")
  val lastModifiedBy: String?,

  @Schema(description = "The follow up date for the incident")
  val followUpDate: LocalDate?,

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
  @Schema(description = "The sequence number of the staff party for this incident")
  val sequence: Int,
  @Schema(description = "Staff role in the incident")
  val role: CodeDescription,
  @Schema(description = "General information about the incident")
  val comment: String?,
  @Schema(description = "The date and time the staff party was created")
  val createDateTime: LocalDateTime,
  @Schema(description = "The username of the person who created the staff party")
  val createdBy: String,
  @Schema(description = "The date and time the staff party was last updated")
  val lastModifiedDateTime: LocalDateTime?,
  @Schema(description = "The username of the person who last updated the staff party")
  val lastModifiedBy: String?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Offender(
  @Schema(description = "NOMIS id")
  val offenderNo: String,
  @Schema(description = "First name of staff member")
  val firstName: String,
  @Schema(description = "Last name of staff member")
  val lastName: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OffenderParty(
  @Schema(description = "Offender involved in the incident")
  val offender: Offender,
  @Schema(description = "The sequence number of the offender party for this incident")
  val sequence: Int,
  @Schema(description = "Offender role in the incident")
  val role: CodeDescription,
  @Schema(description = "The outcome of the incident")
  val outcome: CodeDescription?,
  @Schema(description = "General information about the incident")
  val comment: String?,
  @Schema(description = "The date and time the offender party was created")
  val createDateTime: LocalDateTime,
  @Schema(description = "The username of the person who created the offender party")
  val createdBy: String,
  @Schema(description = "The date and time the offender party was last updated")
  val lastModifiedDateTime: LocalDateTime?,
  @Schema(description = "The username of the person who last updated the offender party")
  val lastModifiedBy: String?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Requirement(
  @Schema(description = "The update required to the incident report")
  val comment: String?,
  @Schema(description = "The sequence number of the requirement for this incident")
  val sequence: Int,
  @Schema(description = "Date and time the requirement was recorded")
  val recordedDate: LocalDateTime,
  @Schema(description = "The staff member who made the requirement request")
  val staff: Staff,
  @Schema(description = "The reporting agency of the staff")
  val agencyId: String,
  @Schema(description = "The date and time the requirement was created")
  val createDateTime: LocalDateTime,
  @Schema(description = "The username of the person who created the requirement")
  val createdBy: String,
  @Schema(description = "The date and time the requirement was last updated")
  val lastModifiedDateTime: LocalDateTime?,
  @Schema(description = "The username of the person who last updated the requirement")
  val lastModifiedBy: String?,
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
  @Schema(description = "The date and time the question was created")
  val createDateTime: LocalDateTime,
  @Schema(description = "The username of the person who created the question")
  val createdBy: String,
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
  @Schema(description = "Response date added to the response by recording staff")
  val responseDate: LocalDate?,
  @Schema(description = "Recording staff")
  val recordingStaff: Staff,
  @Schema(description = "The date and time the response was created")
  val createDateTime: LocalDateTime,
  @Schema(description = "The username of the person who created the response")
  val createdBy: String,
  @Schema(description = "The date and time the response was last updated")
  val lastModifiedDateTime: LocalDateTime?,
  @Schema(description = "The username of the person who last updated the response")
  val lastModifiedBy: String?,
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
  val incidentChangeDateTime: LocalDateTime,
  @Schema(description = "Who changed the questionnaire")
  val incidentChangeStaff: Staff,
  @Schema(description = "The date and time the historical incident questionnaire was created")
  val createDateTime: LocalDateTime,
  @Schema(description = "The username of the person who created the historical incident questionnaire")
  val createdBy: String,
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
  @Schema(description = "Response date added to the response by recording staff")
  val responseDate: LocalDate?,
  @Schema(description = "Recording staff")
  val recordingStaff: Staff,
)

@Schema(description = "Incident Agency Id")
data class IncidentAgencyId(
  @Schema(description = "The agency id", example = "BXI")
  val agencyId: String,
)

@Schema(description = "Incidents reconciliation count response")
data class IncidentsReconciliationResponse(
  @Schema(description = "The agency we checked the incidents for", example = "BXI")
  val agencyId: String,
  @Schema(description = "All open and closed incidents counts")
  val incidentCount: IncidentsCount,
)

@Schema(description = "A count for incidents at an agency")
data class IncidentsCount(
  @Schema(description = "A count for the number of open incidents i.e. all incidents that are not closed or duplicates", example = "4")
  val openIncidents: Long,
  @Schema(description = "A count for the number of closed or duplicate incidents", example = "2")
  val closedIncidents: Long,
)
