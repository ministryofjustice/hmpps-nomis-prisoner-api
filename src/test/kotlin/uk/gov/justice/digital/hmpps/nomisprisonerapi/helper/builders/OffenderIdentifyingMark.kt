package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.BodyPart
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MarkType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderIdentifyingMark
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderIdentifyingMarkId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderIdentifyingMarkImage
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PartOrientation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Side
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderIdentifyingMarkRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDateTime

@DslMarker
annotation class OffenderIdentifyingMarkDslMarker

@NomisDataDslMarker
interface OffenderIdentifyingMarkDsl {

  @OffenderIdentifyingMarkImageDslMarker
  fun image(
    captureDateTime: LocalDateTime = LocalDateTime.now(),
    fullSizeImage: ByteArray = byteArrayOf(1, 2, 3),
    thumbnailImage: ByteArray = byteArrayOf(4, 5, 6),
    active: Boolean = true,
    imageSourceCode: String = "FILE",
    orientationTypeCode: String = "HEAD",
    imageViewTypeCode: String = "TAT",
    dsl: OffenderIdentifyingMarkImageDsl.() -> Unit = {},
  ): OffenderIdentifyingMarkImage
}

@Component
class OffenderIdentifyingMarkBuilderFactory(
  private val repository: OffenderIdentifyingMarkRepository,
  private val bodyPartRepository: ReferenceCodeRepository<BodyPart>,
  private val markTypeRepository: ReferenceCodeRepository<MarkType>,
  private val sideRepository: ReferenceCodeRepository<Side>,
  private val partOrientationRepository: ReferenceCodeRepository<PartOrientation>,
  private val imageBuilderFactory: OffenderIdentifyingMarkImageBuilderFactory,
) {
  fun builder(): OffenderIdentifyingMarkBuilder = OffenderIdentifyingMarkBuilder(
    repository,
    bodyPartRepository,
    markTypeRepository,
    sideRepository,
    partOrientationRepository,
    imageBuilderFactory,
  )
}

class OffenderIdentifyingMarkBuilder(
  private val repository: OffenderIdentifyingMarkRepository,
  private val bodyPartRepository: ReferenceCodeRepository<BodyPart>,
  private val markTypeRepository: ReferenceCodeRepository<MarkType>,
  private val sideRepository: ReferenceCodeRepository<Side>,
  private val partOrientationRepository: ReferenceCodeRepository<PartOrientation>,
  private val imageBuilderFactory: OffenderIdentifyingMarkImageBuilderFactory,
) : OffenderIdentifyingMarkDsl {
  private lateinit var identifyingMark: OffenderIdentifyingMark

  private fun findBodyPart(code: String) = bodyPartRepository.findByIdOrNull(BodyPart.pk(code))!!
  private fun findMarkType(code: String) = markTypeRepository.findByIdOrNull(MarkType.pk(code))!!
  private fun findSide(code: String) = sideRepository.findByIdOrNull(Side.pk(code))!!
  private fun findPartOrientation(code: String) = partOrientationRepository.findByIdOrNull(PartOrientation.pk(code))!!

  fun build(
    offenderBooking: OffenderBooking,
    sequence: Long,
    bodyPartCode: String,
    markTypeCode: String,
    sideCode: String?,
    partOrientationCode: String?,
    commentText: String?,
  ): OffenderIdentifyingMark =
    OffenderIdentifyingMark(
      id = OffenderIdentifyingMarkId(offenderBooking, sequence),
      bodyPart = findBodyPart(bodyPartCode),
      markType = findMarkType(markTypeCode),
      side = sideCode?.let { findSide(sideCode) },
      partOrientation = partOrientationCode?.let { findPartOrientation(partOrientationCode) },
      commentText = commentText,
    )
      .let { repository.save(it) }
      .also { identifyingMark = it }

  override fun image(
    captureDateTime: LocalDateTime,
    fullSizeImage: ByteArray,
    thumbnailImage: ByteArray,
    active: Boolean,
    imageSourceCode: String,
    orientationTypeCode: String,
    imageViewTypeCode: String,
    dsl: OffenderIdentifyingMarkImageDsl.() -> Unit,
  ): OffenderIdentifyingMarkImage =
    imageBuilderFactory.builder().let { builder ->
      builder.build(
        identifyingMark = identifyingMark,
        captureDateTime = captureDateTime,
        fullSizeImage = fullSizeImage,
        thumbnailImage = thumbnailImage,
        active = active,
        imageSourceCode = imageSourceCode,
        orientationTypeCode = orientationTypeCode,
        imageViewType = imageViewTypeCode,
      )
        .also { identifyingMark.images += it }
        .also { builder.apply(dsl) }
    }
}
