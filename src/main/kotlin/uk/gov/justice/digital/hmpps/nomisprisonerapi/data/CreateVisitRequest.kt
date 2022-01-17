package uk.gov.justice.digital.hmpps.nomisprisonerapi.data

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Visit creation request")
data class CreateVisitRequest(
  @Schema(description = "Offender Noms Id", example = "A1234ZZ", required = true)
  @field:Size(
    max = 7,
    min = 7,
    message = "Noms Id must be 7 chars long"
  )
  @NotBlank
  val offenderNo: String,

  @Schema(description = "Visit start date and time", required = true)
  @NotNull
  val startTime: LocalDateTime,

  @Schema(description = "Visit end time", required = true)
  @NotNull
  val endTime: LocalTime,

  @Schema(description = "Prison where the visit is to occur", required = true)
  @NotBlank
  val prisonId: String,

  @Schema(description = "Visitors", required = true)
  @NotEmpty
  val visitorPersonIds: List<Long>,

  @Schema(description = "Whether to handle balance or remaining visits", required = true)
  @NotNull
  val decrementBalances: Boolean,

  @Schema(description = "Whether visit is a privileged one")
  val privileged: Boolean = false,

  @Schema(description = "Visit type, whether social or official", allowableValues = ["SCON", "OFFI"], required = true)
  @NotEmpty
  val visitType: String,

  // Probably not required
//  @Schema(description = "Staff who created the visit booking") //, required = true)
//  // @NotNull
//  val staffId: Long? = null,
//
  @Schema(description = "Issue date", required = true)
  @NotNull
  val issueDate: LocalDate? = null,
)
