package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Upsert temporary absence address")
data class UpsertTemporaryAbsenceAddress(
  @Schema(description = "Address ID - if entered then this is a known NOMIS address, if not a new address is required based on the other properties")
  val id: Long? = null,

  @Schema(description = "The name of a corporation or agency")
  val name: String? = null,

  @Schema(description = "The full address text")
  val addressText: String? = null,

  @Schema(description = "The postal code")
  val postalCode: String? = null,
)
