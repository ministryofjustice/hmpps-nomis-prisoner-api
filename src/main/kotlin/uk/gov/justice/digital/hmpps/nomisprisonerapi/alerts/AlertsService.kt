package uk.gov.justice.digital.hmpps.nomisprisonerapi.alerts

import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.ConflictException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AlertCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AlertStatus.ACTIVE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AlertStatus.INACTIVE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AlertType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAlert
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAlertId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.WorkFlowAction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderAlertRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository

@Service
@Transactional
class AlertsService(
  val offenderBookingRepository: OffenderBookingRepository,
  val offenderAlertRepository: OffenderAlertRepository,
  val alertCodeRepository: ReferenceCodeRepository<AlertCode>,
  val alertTypeRepository: ReferenceCodeRepository<AlertType>,
  val workFlowActionRepository: ReferenceCodeRepository<WorkFlowAction>,
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

  fun createAlert(offenderNo: String, request: CreateAlertRequest): CreateAlertResponse {
    val offenderBooking = offenderBookingRepository.findLatestByOffenderNomsId(offenderNo)
      ?: throw NotFoundException("Prisoner $offenderNo not found with a booking")

    val alertCode = alertCodeRepository.findByIdOrNull(AlertCode.pk(request.alertCode))
      ?: throw BadDataException("Alert code ${request.alertCode} is not valid")

    if (request.isActive && offenderBooking.hasActiveAlertOfCode(request.alertCode)) {
      throw ConflictException("Alert code ${request.alertCode} is already active on booking ${offenderBooking.bookingId} ")
    }

    val workActionCode = workFlowActionRepository.findByIdOrNull(WorkFlowAction.pk(WorkFlowAction.DATA_ENTRY))!!

    val alert = OffenderAlert(
      id = OffenderAlertId(offenderBooking, sequence = offenderAlertRepository.getNextSequence(offenderBooking)),
      alertDate = request.date,
      expiryDate = request.expiryDate,
      alertType = alertTypeRepository.findByIdOrNull(AlertType.pk(alertCode.parentCode!!))!!,
      alertCode = alertCode,
      authorizePersonText = request.authorisedBy,
      alertStatus = if (request.isActive) ACTIVE else INACTIVE,
      verifiedFlag = false,
      commentText = request.comment,
      createUsername = request.createUsername,
    )
    alert.addWorkFlowLog(workActionCode = workActionCode)

    offenderBooking.alerts.add(alert)

    return CreateAlertResponse(
      bookingId = alert.id.offenderBooking.bookingId,
      alertSequence = alert.id.sequence,
      alertCode = alert.alertCode.toCodeDescription(),
      type = alert.alertType.toCodeDescription(),
    )
  }
}

private fun OffenderBooking.hasActiveAlertOfCode(alertCode: String) =
  this.alerts.firstOrNull { it.alertCode.code == alertCode && it.alertStatus == ACTIVE } != null
