package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.identifyingmarks

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Image details, not including the image itself")
@JsonInclude(JsonInclude.Include.NON_NULL)
class IdentifyingMarkImageDetailsResponse(
  @Schema(description = "The unique image identifier", example = "1234567")
  val imageId: Long,
  @Schema(description = "The booking's unique identifier", example = "1234567")
  val bookingId: Long,
  @Schema(description = "The identifying mark sequence, part of the identifying mark unique key", example = "1")
  val idMarksSeq: Long,
  @Schema(description = "The time the image was captured", example = "2024-11-26T10:52:54")
  val captureDateTime: LocalDateTime,
  @Schema(description = "The body part", example = "ARM")
  val bodyPartCode: String,
  @Schema(description = "The type of identifying mark", example = "TAT")
  val markTypeCode: String,
  @Schema(description = "Whether this is the default image for the bookingId/idMarksSeq", example = "true")
  val default: Boolean,
  @Schema(description = "Whether image data exists yet. Image records are created prior to the actual image being added to them (which sometimes never happens).", example = "true")
  val imageExists: Boolean,
  @Schema(description = "The source of the image", example = "FILE")
  val imageSourceCode: String,
  @Schema(description = "The time the image record was created. Note records are created without an image which is added later.", example = "2020-07-17T12:34:56")
  val createDateTime: LocalDateTime,
  @Schema(description = "The user who created the image record", example = "AQ425D")
  val createdBy: String,
  @Schema(description = "The time the image record was last changed", example = "2021-07-16T12:34:56")
  val modifiedDateTime: LocalDateTime?,
  @Schema(description = "The user who modified the image record", example = "AQ425D")
  val modifiedBy: String?,
  @Schema(description = "The name of the module that last changed the image, indicates if this was NOMIS or the synchronisation service", example = "DPS_SYNCHRONISATION")
  val auditModuleName: String?,
)
