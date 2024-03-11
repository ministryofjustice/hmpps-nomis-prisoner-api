package uk.gov.justice.digital.hmpps.nomisprisonerapi.alerts

import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.audit.Audit
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
import java.time.LocalDateTime

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
      offenderAlertRepository.findById_OffenderBookingAndId_Sequence(booking, alertSequence)?.toAlertResponse()
        ?: throw NotFoundException("Prisoner alert not found for alertSequence=$alertSequence")
    } ?: throw NotFoundException("Prisoner booking not found for bookingId=$bookingId")

  @Audit
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

  fun updateAlert(bookingId: Long, alertSequence: Long, request: UpdateAlertRequest): AlertResponse {
    val offenderBooking = offenderBookingRepository.findByIdOrNull(bookingId)
      ?: throw NotFoundException("Booking $bookingId not found")
    val alert = offenderAlertRepository.findByIdOrNull(OffenderAlertId(offenderBooking, alertSequence))
      ?: throw NotFoundException("Alert $alertSequence on $bookingId not found")
    val workActionCode = workFlowActionRepository.findByIdOrNull(WorkFlowAction.pk(WorkFlowAction.MODIFIED))!!

    alert.expiryDate = request.expiryDate
    alert.alertStatus = if (request.isActive) ACTIVE else INACTIVE
    alert.commentText = request.comment
    alert.modifyUserId = request.updateUsername
    alert.modifyDatetime = LocalDateTime.now()
    // TODO I suspect DPS won't update these
    alert.alertDate = request.date
    alert.authorizePersonText = request.authorisedBy

    alert.addWorkFlowLog(workActionCode = workActionCode, createUsername = request.updateUsername)

    return offenderAlertRepository.save(alert).toAlertResponse()
  }

  private fun OffenderAlert.toAlertResponse() = AlertResponse(
    bookingId = id.offenderBooking.bookingId,
    alertSequence = id.sequence,
    alertCode = alertCode.toCodeDescription(),
    type = alertType.toCodeDescription(),
    date = alertDate,
    expiryDate = expiryDate,
    isActive = alertStatus == ACTIVE,
    isVerified = verifiedFlag,
    authorisedBy = authorizePersonText,
    comment = commentText,
    audit = NomisAudit(
      createDatetime = createDatetime,
      createUsername = createUsername,
      modifyDatetime = modifyDatetime,
      modifyUserId = modifyUserId,
      auditUserId = auditUserId,
      auditTimestamp = auditTimestamp,
      auditModuleName = auditModuleName,
      auditAdditionalInfo = auditAdditionalInfo,
      auditClientIpAddress = auditClientIpAddress,
      auditClientUserId = auditClientUserId,
      auditClientWorkstationName = auditClientWorkstationName,
    ),
  )
}

private fun OffenderBooking.hasActiveAlertOfCode(alertCode: String) =
  this.alerts.firstOrNull { it.alertCode.code == alertCode && it.alertStatus == ACTIVE } != null
