package uk.gov.justice.digital.hmpps.nomisprisonerapi.nonassociations

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Offender individual schedule creation request")
data class CreateNonAssociationRequest(

  @Schema(description = "Noms id of the prisoner", required = true, example = "A1234DF")
  val offenderNo: String,

  @Schema(description = "Noms id of the other prisoner", required = true, example = "A1234EG")
  val nsOffenderNo: String,

  @Schema(description = "Reason code of the first prisoner, domain NON_ASSO_RSN", required = true, example = "VIC")
  val reason: String,

  @Schema(description = "Reason code of the other prisoner, domain NON_ASSO_RSN", required = true, example = "PER")
  val recipReason: String,

  @Schema(description = "Type code, domain NON_ASSO_TYP", required = true, example = "WING")
  val type: String,

  val authorisedBy: String? = null,

  @Schema(description = "Effective date", required = true, example = "2022-08-12")
  val effectiveDate: LocalDate,

  @Schema(description = "Comment", example = "Some comment")
  @field:Size(max = 240, message = "Comment is too long (max allowed 240 characters)")
  val comment: String? = null,
)
