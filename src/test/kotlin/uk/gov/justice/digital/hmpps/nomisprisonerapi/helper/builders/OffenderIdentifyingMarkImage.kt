package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.BodyPart
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ImageSource
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MarkType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderIdentifyingMark
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderIdentifyingMarkImage
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderIdentifyingMarkImageRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDateTime

@DslMarker
annotation class OffenderIdentifyingMarkImageDslMarker

@NomisDataDslMarker
interface OffenderIdentifyingMarkImageDsl

@Component
class OffenderIdentifyingMarkImageBuilderFactory(
  private val repository: OffenderIdentifyingMarkImageRepository,
  private val imageSourceRepository: ReferenceCodeRepository<ImageSource>,
  private val bodyPartRepository: ReferenceCodeRepository<BodyPart>,
  private val markTypeRepository: ReferenceCodeRepository<MarkType>,
) {
  fun builder(): OffenderIdentifyingMarkImageBuilder =
    OffenderIdentifyingMarkImageBuilder(repository, imageSourceRepository, bodyPartRepository, markTypeRepository)
}

class OffenderIdentifyingMarkImageBuilder(
  private val repository: OffenderIdentifyingMarkImageRepository,
  private val imageSourceRepository: ReferenceCodeRepository<ImageSource>,
  private val bodyPartRepository: ReferenceCodeRepository<BodyPart>,
  private val markTypeRepository: ReferenceCodeRepository<MarkType>,
) : OffenderIdentifyingMarkImageDsl {

  fun build(
    identifyingMark: OffenderIdentifyingMark,
    captureDateTime: LocalDateTime,
    fullSizeImage: ByteArray,
    thumbnailImage: ByteArray,
    active: Boolean,
    imageSourceCode: String,
    orientationTypeCode: String,
    imageViewType: String,
  ): OffenderIdentifyingMarkImage =
    OffenderIdentifyingMarkImage(
      identifyingMark = identifyingMark,
      captureDateTime = captureDateTime,
      fullSizeImage = fullSizeImage,
      thumbnailImage = thumbnailImage,
      active = active,
      imageSource = imageSourceRepository.findByIdOrNull(ImageSource.pk(imageSourceCode))!!,
      orientationType = bodyPartRepository.findByIdOrNull(BodyPart.pk(orientationTypeCode))!!,
      imageViewType = markTypeRepository.findByIdOrNull(MarkType.pk(imageViewType))!!,
    )
      .let { repository.save(it) }
}
