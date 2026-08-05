package uk.gov.justice.digital.hmpps.nomisprisonerapi.property

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PropertyContainerCode
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Prisoner property field which are mutable")
data class PropertyContainerUpdateRequest(
  @Schema(description = "The property box location id for the container", example = "1234567")
  val internalLocationId: Long? = null,

  @Schema(description = "The container's seal number", example = "1234")
  val sealMark: String? = null,

  @Schema(
    description = "The container's code",
    example = "BULK",
    enumAsRef = true,
  )
  val containerCode: PropertyContainerCode,

  @Schema(description = "Date the container will be disposed of")
  val proposedDisposalDate: LocalDate? = null,

  @Schema(description = "Whether the container is active")
  val active: Boolean,

  @Schema(description = "Date the container is no longer active", example = "2027-01-01")
  val expiryDate: LocalDate? = null,
)
