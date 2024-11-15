package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.time.LocalDateTime

@Entity
@DiscriminatorValue("OFF_BKG")
class OffenderBookingImage(
  id: Long,
  captureDateTime: LocalDateTime,
  fullSizeImage: ByteArray,
  thumbnailImage: ByteArray,
  imageObjectSequence: Long? = null,
  active: Boolean,
  imageSource: ImageSource,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,
) : OffenderImage(
  id,
  captureDateTime,
  fullSizeImage,
  thumbnailImage,
  imageObjectSequence,
  active,
  imageSource,
  // This is hardcoded in NOMIS so do the same here
  "FRONT",
  // This is hardcoded in NOMIS so do the same here
  "FACE",
)
