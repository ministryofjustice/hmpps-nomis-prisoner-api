package uk.gov.justice.digital.hmpps.nomisprisonerapi.property

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PropertyContainerCode
import java.time.LocalDate
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Prisoner property container")
data class PropertyContainerGetResponse(
  @Schema(description = "The property container id")
  val containerId: Long,

  @Schema(description = "The NOMIS reference of the owner", example = "A1234AA")
  val offenderNo: String,

  @Schema(description = "The offender booking id", example = "1234567")
  val bookingId: Long,

  @Schema(description = "The property box id for the container", example = "1234567")
  val internalLocationId: Long? = null,

  @Schema(description = "The container is in this prison", example = "LEI")
  val prisonId: String,

  @Schema(description = "Whether the container is active", example = "true")
  val active: Boolean,

  @Schema(description = "The container's seal number", example = "1234")
  val sealMark: String? = null,

  @Schema(
    description = "The container's code",
    example = "DES",
    // allowableValues = ["BRA", "BULK", "CO", "DES", "VALU"],
    enumAsRef = true,
  )
  val containerCode: PropertyContainerCode,

  @Schema(description = "Date the container is no longer active")
  val expiryDate: LocalDate? = null,

  @Schema(description = "Date the container will be disposed of")
  val proposedDisposalDate: LocalDate? = null,

  @Schema(description = "Timestamp for when the container was created", example = "2025-12-06T12:34:56")
  val createdDateTime: LocalDateTime,

  @Schema(description = "Username of the staff creating the container", example = "AJONES")
  val createdBy: String,

  @Schema(description = "Timestamp for when the container was modified", example = "2025-12-06T12:34:56")
  val updatedDateTime: LocalDateTime? = null,

  @Schema(description = "Username of the staff modifying the container", example = "AJONES")
  val updatedBy: String? = null,
)
