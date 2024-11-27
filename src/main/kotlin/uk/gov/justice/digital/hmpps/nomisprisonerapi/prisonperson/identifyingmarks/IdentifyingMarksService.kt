package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.identifyingmarks

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.ImageBadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.ImageNotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderIdentifyingMarkImageRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.getReleaseTime

@Service
@Transactional(readOnly = true)
class IdentifyingMarksService(
  private val bookingRepository: OffenderBookingRepository,
  private val imageRepository: OffenderIdentifyingMarkImageRepository,
) {

  fun getIdentifyingMarks(bookingId: Long): BookingIdentifyingMarksResponse =
    bookingRepository.findByIdOrNull(bookingId)
      ?.let { booking ->
        BookingIdentifyingMarksResponse(
          bookingId = booking.bookingId,
          startDateTime = booking.bookingBeginDate,
          endDateTime = booking.getReleaseTime(),
          latestBooking = booking.bookingSequence == 1,
          identifyingMarks = booking.identifyingMarks.map { mark ->
            IdentifyingMarksResponse(
              idMarksSeq = mark.id.idMarkSequence,
              bodyPartCode = mark.bodyPart.code,
              markTypeCode = mark.markType.code,
              sideCode = mark.side?.code,
              partOrientationCode = mark.partOrientation?.code,
              commentText = mark.commentText,
              imageIds = mark.images.map { image -> image.id },
              createDateTime = mark.createDatetime,
              createdBy = mark.createUserId,
              modifiedDateTime = mark.modifyDatetime,
              modifiedBy = mark.modifyUserId,
              auditModuleName = mark.auditModuleName,
            )
          },
        )
      }
      ?: throw NotFoundException("Booking not found: $bookingId")

  fun getIdentifyingMarksImageDetails(imageId: Long): IdentifyingMarkImageDetailsResponse =
    imageRepository.findByIdOrNull(imageId)
      ?.let {
        IdentifyingMarkImageDetailsResponse(
          imageId = it.id,
          bookingId = it.identifyingMark.id.offenderBooking.bookingId,
          idMarksSeq = it.identifyingMark.id.idMarkSequence,
          captureDateTime = it.captureDateTime,
          bodyPartCode = it.orientationType.code,
          markTypeCode = it.imageViewType.code,
          default = it.active,
          imageExists = it.fullSizeImage != null,
          imageSourceCode = it.imageSource.code,
          createDateTime = it.createDatetime,
          createdBy = it.createUserId,
          modifiedDateTime = it.modifyDatetime,
          modifiedBy = it.modifyUserId,
          auditModuleName = it.auditModuleName,
        )
      }
      ?: throw NotFoundException("Image not found: $imageId")

  fun getIdentifyingMarksImageData(imageId: Long): ByteArray =
    imageRepository.findByIdOrNull(imageId)
      ?.also { if (it.fullSizeImage == null) throw ImageNotFoundException("Image not found: $imageId") }
      ?.fullSizeImage
      ?: throw ImageBadDataException("Image record does not exist: $imageId")
}
