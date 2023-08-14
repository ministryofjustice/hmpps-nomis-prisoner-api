package uk.gov.justice.digital.hmpps.nomisprisonerapi.nonassociations

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDate

@Schema(description = "Appointment information")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class NonAssociationResponse(
  @Schema(description = "Noms id of the prisoner", required = true, example = "A1234DF")
  val offenderNo: String,

  @Schema(description = "Noms id of the other prisoner", required = true, example = "A1234EG")
  val nsOffenderNo: String,

  @Schema(description = "Reason code of the first prisoner, domain NON_ASSO_RSN", required = true, example = "VIC")
  val reason: String? = null,

  @Schema(description = "Reason code of the other prisoner, domain NON_ASSO_RSN", example = "PER")
  val recipReason: String? = null,

  @Schema(description = "Type code, domain NON_ASSO_TYP", required = true, example = "WING")
  val type: String? = null,

  val authorisedBy: String? = null,

  @Schema(description = "Effective date", required = true, example = "2022-08-12")
  val effectiveDate: LocalDate? = null,

  @Schema(description = "Expiry date, open if null", example = "2022-08-12")
  val expiryDate: LocalDate? = null,

  @Schema(description = "Comment", example = "Some comment")
  @field:Size(max = 240, message = "Comment is too long (max allowed 240 characters)")
  val comment: String? = null,
)