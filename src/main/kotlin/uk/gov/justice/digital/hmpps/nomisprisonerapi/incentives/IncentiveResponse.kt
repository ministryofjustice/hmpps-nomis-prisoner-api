package uk.gov.justice.digital.hmpps.nomisprisonerapi.incentives

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import java.time.LocalDateTime

@Schema(description = "Incentive information")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class IncentiveResponse(
  @Schema(description = "The offender number, aka nomsId, prisonerId", required = true)
  val offenderNo: String,
  @Schema(description = "The booking id", required = true)
  val bookingId: Long,
  @Schema(description = "The sequence of the incentive within this booking", required = true)
  val incentiveSequence: Long,
  @Schema(description = "Comment for Incentive level", required = false)
  val commentText: String? = null,
  @Schema(description = "Date and time of Incentive level creation", required = false)
  val iepDateTime: LocalDateTime,
  @Schema(description = "Prison where the Incentive level was created", required = true)
  val prisonId: String,
  @Schema(description = "IEP level code and description", required = true)
  val iepLevel: CodeDescription,
  @Schema(description = "User id of user creating prisoner incentive level", required = false)
  val userId: String? = null,
  @Schema(description = "Is this IEP the current IEP for the booking?", required = true)
  val currentIep: Boolean,
  @Schema(
    description = "The NOMIS module that created this IEP",
    required = true,
    allowableValues = ["OCUWARNG", "PRISON_API", "OIDADMIS", "MERGE", "OIDOIEPS", "OIDITRAN", "OSIOSEAR"],
  )
  val auditModule: String? = null,
  @Schema(description = "date and time of creation")
  val whenCreated: LocalDateTime,
  @Schema(description = "date and time of last update")
  val whenUpdated: LocalDateTime? = null,
)
