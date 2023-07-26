package uk.gov.justice.digital.hmpps.nomisprisonerapi.appointments

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.LocalTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Offender individual schedule update request")
data class UpdateAppointmentRequest(

  @Schema(description = "Appointment date", required = true, example = "2022-08-12")
  val eventDate: LocalDate,

  @Schema(description = "Appointment start time", required = true, example = "09:45")
  val startTime: LocalTime,

  @Schema(description = "Activity end time", required = true, example = "15:20")
  val endTime: LocalTime?,

  @Schema(description = "Room where the appointment is to occur (in cell if null)", example = "112233")
  val internalLocationId: Long? = null,

  @Schema(description = "Appointment event sub-type", required = true, example = "MEOT")
  val eventSubType: String,

  @Schema(description = "Comment", example = "Some comment")
  @field:Size(max = 4000, message = "Comment is too long (max allowed 4000 characters)")
  val comment: String? = null,
)
