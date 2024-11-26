package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.identifyingmarks

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.getReleaseTime

@Service
@Transactional(readOnly = true)
class IdentifyingMarksService(
  private val bookingRepository: OffenderBookingRepository,
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

  // TODO SDIT-2212 implement this service
  fun getIdentifyingMarksImage(imageId: Long): IdentifyingMarkImageDetailsResponse = throw NotFoundException("Image not found: $imageId")
}
