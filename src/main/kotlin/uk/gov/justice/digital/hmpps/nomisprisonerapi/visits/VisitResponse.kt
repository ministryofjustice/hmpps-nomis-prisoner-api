package uk.gov.justice.digital.hmpps.nomisprisonerapi.visits

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Phone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Visit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitVisitor
import java.time.LocalDateTime

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

  @Schema(description = "the lead visitor")
  val leadVisitor: LeadVisitor?,

  @Schema(description = "Visit type, whether social or official", allowableValues = ["SCON", "OFFI"], required = true)
  @NotEmpty
  val visitType: CodeDescription,

  @Schema(
    description = "The status of the visit",
    allowableValues = [
      "CANC",
      "EXP",
      "HMPOP",
      "NORM",
      "OFFEND",
      "SCH",
      "VISITOR",
      "VDE",
    ],
    required = true,
  )
  val visitStatus: CodeDescription,

  @Schema(
    description = "The outcome of the visit",
    allowableValues = [
      "ADMIN",
      "HMP",
      "NO_ID",
      "NO_VO",
      "NSHOW",
      "OFFCANC",
      "REFUSED",
      "VISCANC",
      "VO_CANCEL",
      "BATCH_CANC",
      "ADMIN_CANCEL",
    ],
    required = true,
  )
  val visitOutcome: CodeDescription?,

  @Schema(description = "NOMIS room", required = true)
  val agencyInternalLocation: CodeDescription? = null,

  @Schema(
    description = "Visit comments",
  )
  val commentText: String? = null,

  @Schema(
    description = "Visitor concerns text",
  )
  val visitorConcernText: String? = null,

  @Schema(
    description = "date and time of creation",
  )
  val whenCreated: LocalDateTime,

  @Schema(
    description = "date and time of last update",
  )
  val whenUpdated: LocalDateTime? = null,

  @Schema(
    description = "User id for visit creation",
  )
  val createUserId: String,

  @Schema(
    description = "User id for last visit update",
  )
  val modifyUserId: String? = null,
) {
  data class Visitor(
    @Schema(
      description = "visitor NOMIS person Id",
    )
    val personId: Long,
    @Schema(
      description = "Indicates lead visitor for the visit",
    )
    val leadVisitor: Boolean,
  )

  data class LeadVisitor(
    @Schema(
      description = "visitor NOMIS person Id",
    )
    val personId: Long,
    @Schema(
      description = "full name of visitor",
    )
    val fullName: String,
    @Schema(
      description = "Ordered list of telephone numbers for contact with latest first",
    )
    val telephones: List<String>,
  )

  constructor(visitEntity: Visit) : this(
    visitId = visitEntity.id,
    offenderNo = visitEntity.offenderBooking.offender.nomsId,
    prisonId = visitEntity.location.id,
    startDateTime = visitEntity.startDateTime,
    endDateTime = visitEntity.endDateTime,
    visitType = CodeDescription(visitEntity.visitType.code, visitEntity.visitType.description),
    visitStatus = CodeDescription(visitEntity.visitStatus.code, visitEntity.visitStatus.description),
    agencyInternalLocation = visitEntity.agencyInternalLocation?.let {
      CodeDescription(
        it.locationCode,
        it.description,
      )
    },
    commentText = visitEntity.commentText,
    visitorConcernText = visitEntity.visitorConcernText,
    visitors = visitEntity.visitors.filter { visitor -> visitor.person != null }
      .map { visitor -> Visitor(visitor.person!!.id, visitor.groupLeader) },
    visitOutcome = visitEntity.outcomeVisitor()?.outcomeReason
      ?.let { CodeDescription(it.code, it.description) }
      ?: visitEntity.outcomeVisitor()?.outcomeReasonCode?.let { CodeDescription(it, it) },
    leadVisitor = visitEntity.visitors.find { visitor -> visitor.groupLeader }?.person?.let {
      LeadVisitor(
        personId = it.id,
        fullName = "${it.firstName} ${it.lastName}",
        telephones = (it.phones + it.addresses.flatMap { address -> address.phones }).sortedByDescending { phone -> phone.lastChanged }
          .toTelephoneList(),
      )
    },
    modifyUserId = visitEntity.modifyUserId,
    whenUpdated = visitEntity.whenUpdated,
    createUserId = visitEntity.createUserId,
    whenCreated = visitEntity.whenCreated,
  )
}

fun List<Phone>.toTelephoneList(): List<String> = this.map { phone ->
  phone.phoneNo + if (phone.extNo.isNullOrBlank()) {
    ""
  } else {
    " ${phone.extNo}"
  }
}

private val Phone.lastChanged: LocalDateTime
  get() {
    return modifyDatetime ?: createDatetime
  }

fun Visit.outcomeVisitor(): VisitVisitor? = this.visitors.find { it.person == null }
