package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import java.time.LocalDateTime

@Entity
@DiscriminatorValue("OFF_BKG")
class OffenderImageBooking(
  id: Long,
  offenderBooking: OffenderBooking,
  captureDateTime: LocalDateTime,
  fullSizeImage: ByteArray,
  thumbnailImage: ByteArray,
  imageObjectId: Long? = null,
  imageObjectSequence: Long? = null,
  active: Boolean,
  imageSource: ImageSource,
) : OffenderImage(
  id,
  offenderBooking,
  captureDateTime,
  fullSizeImage,
  thumbnailImage,
  imageObjectId,
  imageObjectSequence,
  active,
  imageSource,
  // This might come from REFERENCE_DOMAIN=IMAGE_VIEW, but in the database it's always "FRONT" so just hardcoding for now
  "FRONT",
  // This might come from REFERENCE_DOMAIN=BODY_PART or PART_ORIENT, but in the database it's always "FACE" so just hardcoding for now
  "FACE",
)
