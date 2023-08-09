package uk.gov.justice.digital.hmpps.nomisprisonerapi.appointments

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.hibernate.validator.constraints.Length
import java.time.LocalDateTime

@Schema(description = "Appointment information")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AppointmentResponse(
  @Schema(description = "The booking id", required = true)
  val bookingId: Long,
  @Schema(description = "The offender number, aka nomsId, prisonerId", required = true)
  val offenderNo: String,
  @Schema(description = "Prison where the appointment occurs", required = true)
  val prisonId: String? = null,
  @Schema(description = "NOMIS room id", required = true)
  val internalLocation: Long? = null,
  @Schema(description = "Start date and time", required = true)
  val startDateTime: LocalDateTime? = null,
  @Schema(description = "End date and time", required = true)
  val endDateTime: LocalDateTime? = null,
  @Schema(description = "Comment")
  @field:Length(max = 4000)
  val comment: String? = null,
  @Schema(description = "Event subtype")
  val subtype: String,
  @Schema(description = "Status")
  val status: String,
  @Schema(description = "Record created date", required = true)
  val createdDate: LocalDateTime,
  @Schema(description = "Record created by", required = true)
  val createdBy: String,
  @Schema(description = "Record modified date")
  val modifiedDate: LocalDateTime? = null,
  @Schema(description = "Record modified by")
  val modifiedBy: String? = null,
)
