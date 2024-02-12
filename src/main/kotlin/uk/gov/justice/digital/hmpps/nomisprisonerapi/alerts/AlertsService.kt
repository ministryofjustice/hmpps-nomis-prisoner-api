package uk.gov.justice.digital.hmpps.nomisprisonerapi.alerts

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AlertStatus.ACTIVE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderAlertRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository

@Service
class AlertsService(
  val offenderBookingRepository: OffenderBookingRepository,
  val offenderAlertRepository: OffenderAlertRepository,
) {
  fun getAlert(bookingId: Long, alertSequence: Long): AlertResponse =
    offenderBookingRepository.findByIdOrNull(bookingId)?.let { booking ->
      offenderAlertRepository.findById_OffenderBookingAndId_Sequence(booking, alertSequence)?.let {
        AlertResponse(
          bookingId = bookingId,
          alertSequence = alertSequence,
          alertCode = it.alertCode.toCodeDescription(),
          type = it.alertType.toCodeDescription(),
          date = it.alertDate,
          expiryDate = it.expiryDate,
          isActive = it.alertStatus == ACTIVE,
          isVerified = it.verifiedFlag,
          authorisedBy = it.authorizePersonText,
          comment = it.commentText,
          audit = NomisAudit(
            createDatetime = it.createDatetime,
            createUsername = it.createUsername,
            modifyDatetime = it.modifyDatetime,
            modifyUserId = it.modifyUserId,
            auditUserId = it.auditUserId,
            auditTimestamp = it.auditTimestamp,
            auditModuleName = it.auditModuleName,
            auditAdditionalInfo = it.auditAdditionalInfo,
            auditClientIpAddress = it.auditClientIpAddress,
            auditClientUserId = it.auditClientUserId,
            auditClientWorkstationName = it.auditClientWorkstationName,
          ),
        )
      } ?: throw NotFoundException("Prisoner alert not found for alertSequence=$alertSequence")
    } ?: throw NotFoundException("Prisoner booking not found for bookingId=$bookingId")
}
