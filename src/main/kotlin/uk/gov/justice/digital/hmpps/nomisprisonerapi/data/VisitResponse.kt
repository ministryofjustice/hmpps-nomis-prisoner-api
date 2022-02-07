package uk.gov.justice.digital.hmpps.nomisprisonerapi.data

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Visit
import java.time.LocalDateTime
import javax.validation.constraints.NotEmpty

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Visit information")
data class VisitResponse(
  @Schema(description = "The visit id", required = true)
  val visitId: Long,

  @Schema(description = "The offender number, aka nomsId, prisonerId", required = true)
  val offenderNo: String,

  @Schema(description = "Visit start date and time", required = true)
  val startDateTime: LocalDateTime,

  @Schema(description = "Visit end date and time", required = true)
  val endDateTime: LocalDateTime,

  @Schema(description = "Prison where the visit is to occur", required = true)
  val prisonId: String,

  @Schema(description = "Visitors", required = true)
  val visitors: List<Visitor>,

  @Schema(description = "Visit type, whether social or official", allowableValues = ["SCON", "OFFI"], required = true)
  @NotEmpty
  val visitType: CodeDescription,

  @Schema(
    description = "The status of the visit",
    allowableValues = [
      "CANC",
      "COMP",
      "EXP",
      "HMPOP",
      "NORM",
      "OFFEND",
      "SCH",
      "VISITOR"
    ],
    required = true
  )
  val visitStatus: CodeDescription,

  @Schema(description = "NOMIS room", required = true)
  val agencyInternalLocation: CodeDescription? = null,

  @Schema(
    description = "Visit comments"
  )
  val commentText: String? = null,

  @Schema(
    description = "Visitor concerns text"
  )
  val visitorConcernText: String? = null,
) {
  data class Visitor(
    @Schema(
      description = "visitor NOMIS person Id"
    )
    val personId: Long,
    @Schema(
      description = "Indicates lead visitor for the visit"
    )
    val leadVisitor: Boolean
  )

  constructor(visitEntity: Visit) : this(
    visitId = visitEntity.id,
    offenderNo = visitEntity.offenderBooking.offender.nomsId,
    prisonId = visitEntity.location.id,
    startDateTime = visitEntity.startDateTime,
    endDateTime = visitEntity.endDateTime,
    visitType = CodeDescription(visitEntity.visitType.code, visitEntity.visitType.description),
    visitStatus = CodeDescription(visitEntity.visitStatus.code, visitEntity.visitStatus.description),
    agencyInternalLocation = visitEntity.agencyInternalLocation?.let { CodeDescription(it.locationCode, it.description) },
    commentText = visitEntity.commentText,
    visitorConcernText = visitEntity.visitorConcernText,
    visitors = visitEntity.visitors.filter { visitor -> visitor.person != null }.map { visitor -> Visitor(visitor.person!!.id, visitor.groupLeader) }

  )

  data class CodeDescription(val code: String, val description: String)
}
