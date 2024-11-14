package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.time.LocalDateTime

const val OFFENDER_IMAGE_BOOKINGS = "OFF_BKG"

@EntityOpen
@Entity
@DiscriminatorValue(OFFENDER_IMAGE_BOOKINGS)
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

  @Column(name = "IMAGE_OBJECT_TYPE")
  val imageObjectType: String = OFFENDER_IMAGE_BOOKINGS,
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
