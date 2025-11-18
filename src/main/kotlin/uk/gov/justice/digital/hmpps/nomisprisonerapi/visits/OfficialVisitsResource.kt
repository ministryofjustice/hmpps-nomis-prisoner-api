package uk.gov.justice.digital.hmpps.nomisprisonerapi.visits

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
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
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.NomisAudit
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
    @PageableDefault(size = 20, sort = ["id" ], direction = Sort.Direction.ASC)
    @ParameterObject
    pageRequest: Pageable,
  ): PagedModel<VisitIdResponse> = PagedModel(officialVisitsService.getVisitIds(pageRequest = pageRequest))

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
  fun getVisitTimeSlot(
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
  @Schema(description = "The status of the visit")
  val visitStatus: CodeDescription,
  @Schema(description = "The outcome of the visit")
  val visitOutcome: CodeDescription?,
  @Schema(description = "The outcome reason of the visit")
  val outcomeReason: CodeDescription?,
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
    @Schema(description = "Indicates lead visitor for the visit")
    val leadVisitor: Boolean,
    @Schema(description = "Indicates visitor requires assistance")
    val assistedVisit: Boolean,
    @Schema(description = "The outcome of the visit")
    val visitOutcome: CodeDescription?,
    @Schema(description = "The outcome reason of the visit")
    val outcomeReason: CodeDescription?,
    @Schema(description = "The status of visitor")
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
      @Schema(description = "The relationship type")
      val relationshipType: CodeDescription,
      @Schema(description = "Audit information")
      val audit: NomisAudit,
    )
  }
}
