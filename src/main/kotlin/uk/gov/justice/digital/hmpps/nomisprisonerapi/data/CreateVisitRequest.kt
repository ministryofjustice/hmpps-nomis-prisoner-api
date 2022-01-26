package uk.gov.justice.digital.hmpps.nomisprisonerapi.data

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Visit creation request")
data class CreateVisitRequest(

  @Schema(description = "Visit start date and time", required = true)
  @NotNull
  val startDateTime: LocalDateTime,

  @Schema(description = "Visit end time", required = true)
  @NotNull
  val endTime: LocalTime,

  @Schema(description = "Prison where the visit is to occur", required = true)
  @NotBlank
  val prisonId: String,

  @Schema(description = "Visitors", required = true)
  @NotEmpty
  val visitorPersonIds: List<Long>,

  @Schema(description = "Whether visit is a privileged one")
  val privileged: Boolean = false,

  @Schema(description = "Visit type, whether social or official", allowableValues = ["SCON", "OFFI"], required = true)
  @NotEmpty
  val visitType: String,

  // Probably not required
//  @Schema(description = "Staff who created the visit booking") //, required = true)
//  // @NotNull
//  val staffId: Long? = null,

  @Schema(description = "Issue date", required = true)
  @NotNull
  val issueDate: LocalDate,

//  @Schema(description = "Location of visit, an agency internal location code", required = true)
//  @NotBlank
//  val visitRoomId: String,

  @Schema(description = "VSIP visit id to allow mapping of nomis to VSIP visits", required = true)
  @NotBlank
  val vsipVisitId: String,
)
