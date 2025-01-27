package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ImageSource
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBookingImage
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingImageRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDateTime

@DslMarker
annotation class OffenderBookingImageDslMarker

@NomisDataDslMarker
interface OffenderBookingImageDsl

@Component
class OffenderBookingImageBuilderFactory(
  private val repository: OffenderBookingImageRepository,
  private val imageSourceRepository: ReferenceCodeRepository<ImageSource>,
) {
  fun builder(): OffenderBookingImageBuilder = OffenderBookingImageBuilder(repository, imageSourceRepository)
}

class OffenderBookingImageBuilder(
  private val repository: OffenderBookingImageRepository,
  private val imageSourceRepository: ReferenceCodeRepository<ImageSource>,
) : OffenderBookingImageDsl {

  private fun findImageSource(code: String) = imageSourceRepository.findByIdOrNull(ImageSource.pk(code))!!

  fun build(
    offenderBooking: OffenderBooking,
    captureDateTime: LocalDateTime,
    fullSizeImage: ByteArray,
    thumbnailImage: ByteArray,
    active: Boolean,
    imageSourceCode: String,
  ): OffenderBookingImage = OffenderBookingImage(
    offenderBooking = offenderBooking,
    captureDateTime = captureDateTime,
    fullSizeImage = fullSizeImage,
    thumbnailImage = thumbnailImage,
    active = active,
    imageSource = findImageSource(imageSourceCode),
  )
    .let { repository.save(it) }
}
