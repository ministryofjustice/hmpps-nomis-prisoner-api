package uk.gov.justice.digital.hmpps.nomisprisonerapi.visits

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.PagedModel
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.NomisAudit
import java.time.LocalDate
import java.time.LocalDateTime

@RestController
@Validated
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasRole('ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW')")
class OfficialVisitsResource(private val officialVisitsService: OfficialVisitsService) {
  @GetMapping("/official-visits/ids")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get all official visit Ids",
    description = "Typically for a migration. Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Page of visit Ids",
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
  fun getOfficialVisitIds(
    @PageableDefault(size = 20, sort = ["id"], direction = Sort.Direction.ASC)
    @ParameterObject
    pageRequest: Pageable,
    @RequestParam(value = "prisonIds")
    @Parameter(description = "Filter results by prison ids (returns all prisons if not specified)", example = "['MDI','LEI']")
    prisonIds: List<String> = emptyList(),
    @RequestParam(value = "fromDate")
    @Parameter(description = "Filter results by visits that were created on or after the given date", example = "2024-11-03")
    fromDate: LocalDate?,
    @RequestParam(value = "toDate")
    @Parameter(description = "Filter results by visits that were created on or before the given date", example = "2025-11-03")
    toDate: LocalDate?,
  ): PagedModel<VisitIdResponse> = PagedModel(
    officialVisitsService.getVisitIds(
      pageRequest = pageRequest,
      prisonIds = prisonIds,
      fromDate = fromDate,
      toDate = toDate,
    ),
  )

  @GetMapping("/official-visits/ids/all-from-id")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get all official visit Ids",
    description = "Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Page of visit Ids",
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
  fun getOfficialVisitIdsFromIds(
    @Schema(description = "If supplied get visit starting after this id", required = false, example = "1555999")
    @RequestParam(value = "visitId", defaultValue = "0")
    visitId: Long,
    @Schema(description = "Number of visit ids to get", required = false, defaultValue = "20")
    @RequestParam(value = "size", defaultValue = "20")
    size: Int,
    @RequestParam(value = "prisonIds")
    @Parameter(description = "Filter results by prison ids (returns all prisons if not specified)", example = "['MDI','LEI']")
    prisonIds: List<String> = emptyList(),
    @RequestParam(value = "fromDate")
    @Parameter(description = "Filter results by visits that were created on or after the given date", example = "2024-11-03")
    fromDate: LocalDate?,
    @RequestParam(value = "toDate")
    @Parameter(description = "Filter results by visits that were created on or before the given date", example = "2025-11-03")
    toDate: LocalDate?,
  ): VisitIdsPage = officialVisitsService.getVisitIds(
    visitId = visitId,
    pageSize = size,
    prisonIds = prisonIds,
    fromDate = fromDate,
    toDate = toDate,
  )

  @GetMapping("/official-visits/{visitId}")
  @Operation(
    summary = "Get an official visit",
    description = "Requires ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The official visit",
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
        responseCode = "400",
        description = "Visit is not an official visit",
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
        description = "Visit not found",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getOfficialVisit(
    @PathVariable visitId: Long,
  ): OfficialVisitResponse = officialVisitsService.getVisit(visitId = visitId)
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Official Visit information")
data class OfficialVisitResponse(
  @Schema(description = "The visit id")
  val visitId: Long,
  @Schema(description = "The visit slot id")
  val visitSlotId: Long,
  @Schema(description = "Prison where the visit is to occur")
  val prisonId: String,
  @Schema(description = "The offender number, aka nomsId, prisonerId")
  val offenderNo: String,
  @Schema(description = "The offender booking id")
  val bookingId: Long,
  @Schema(description = "true if the related booking is current")
  val currentTerm: Boolean,
  @Schema(description = "Visit start date and time")
  val startDateTime: LocalDateTime,
  @Schema(description = "Visit end date and time")
  val endDateTime: LocalDateTime,
  @Schema(description = "The room where the visit will take place")
  val internalLocationId: Long,
  @Schema(description = "The status of the visit; Scheduled, Normal, Cancelled")
  val visitStatus: CodeDescription,
  @Schema(description = "The outcome of the visit; Completed, Cancelled, Scheduled, Expired")
  val visitOutcome: CodeDescription?,
  @Schema(description = "The reason of the visit cancellation")
  val cancellationReason: CodeDescription?,
  @Schema(description = "The status of prisoner, Attended or Absent")
  val prisonerAttendanceOutcome: CodeDescription?,
  @Schema(description = "The type of search to apply to prisoner")
  val prisonerSearchType: CodeDescription?,
  @Schema(description = "Visitor concerns text")
  val visitorConcernText: String? = null,
  @Schema(description = "Visit comments")
  val commentText: String? = null,
  @Schema(description = "A username associated with the staff user who override ban")
  val overrideBanStaffUsername: String? = null,
  @Schema(description = "Visitors")
  val visitors: List<OfficialVisitor>,
  @Schema(description = "Details about any related visitor order")
  val visitOrder: VisitOrder?,
  @Schema(description = "Audit information")
  val audit: NomisAudit,
) {
  @JsonInclude(JsonInclude.Include.NON_NULL)
  data class OfficialVisitor(
    @Schema(description = "ID of the visitor")
    val id: Long,
    @Schema(description = "visitor NOMIS person Id")
    val personId: Long,
    @Schema(description = "First name of the person")
    val firstName: String,
    @Schema(description = "Surname name of the person")
    val lastName: String,
    @Schema(description = "Date of birth name of the person")
    val dateOfBirth: LocalDate?,
    @Schema(description = "Indicates lead visitor for the visit")
    val leadVisitor: Boolean,
    @Schema(description = "Indicates visitor requires assistance")
    val assistedVisit: Boolean,
    @Schema(description = "The status of visitor, Attended or Absent")
    val visitorAttendanceOutcome: CodeDescription?,
    @Schema(description = "The reason of the visit cancellation - typically matches the overall cancellation reason")
    val cancellationReason: CodeDescription?,
    @Schema(description = "The status of the visit; Scheduled, Normal, Cancelled")
    val eventStatus: CodeDescription?,
    @Schema(description = "Visitor comments")
    val commentText: String? = null,
    @Schema(description = "List of visitor contact relationships")
    val relationships: List<ContactRelationship> = emptyList(),
    @Schema(description = "Audit information")
    val audit: NomisAudit,
  ) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class ContactRelationship(
      @Schema(description = "The relationship type, e.g. police")
      val relationshipType: CodeDescription,
      @Schema(description = "The contact type, e.g. social or official")
      val contactType: CodeDescription,
    )
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  data class VisitOrder(
    @Schema(description = "The visit order number as displayed in NOMIS", example = "123456789")
    val number: Long,
  )
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class VisitIdsPage(
  @Schema(description = "Page of visit IDs")
  val ids: List<VisitIdResponse>,
)
