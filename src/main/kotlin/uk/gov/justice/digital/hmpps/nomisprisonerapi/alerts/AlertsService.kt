package uk.gov.justice.digital.hmpps.nomisprisonerapi.alerts

import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.ConflictException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.NomisAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.truncateToUtf8Length
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AlertCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AlertStatus.ACTIVE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AlertStatus.INACTIVE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AlertType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAlert
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAlertId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
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
  fun getAlert(bookingId: Long, alertSequence: Long): AlertResponse = offenderBookingRepository.findByIdOrNull(bookingId)?.let { booking ->
    offenderAlertRepository.findById_OffenderBookingAndId_Sequence(booking, alertSequence)?.toAlertResponse()
      ?: throw NotFoundException("Prisoner alert not found for alertSequence=$alertSequence")
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
      rootOffender = offenderBooking.rootOffender,
      alertDate = request.date,
      expiryDate = request.expiryDate,
      alertType = alertTypeRepository.findByIdOrNull(AlertType.pk(alertCode.parentCode!!))!!,
      alertCode = alertCode,
      authorizePersonText = request.authorisedBy,
      alertStatus = if (request.isActive) ACTIVE else INACTIVE,
      verifiedFlag = false,
      commentText = request.comment?.truncateToUtf8Length(4000),
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

  fun resynchroniseAlerts(offenderNo: String, requests: List<CreateAlertRequest>): List<CreateAlertResponse> {
    val offenderBooking = offenderBookingRepository.findLatestByOffenderNomsId(offenderNo)
      ?: throw NotFoundException("Prisoner $offenderNo not found with a booking")

    // ensure the new alerts get a new primary key that doesn't match the ones being deleted
    // else they will all get deleted from DPS
    val nextSequence = offenderAlertRepository.getNextSequence(offenderBooking)
    offenderBooking.alerts.clear()
    requests.forEachIndexed { index, request ->
      val alertCode = alertCodeRepository.findByIdOrNull(AlertCode.pk(request.alertCode))
        ?: throw BadDataException("Alert code ${request.alertCode} is not valid")

      val workActionCode = workFlowActionRepository.findByIdOrNull(WorkFlowAction.pk(WorkFlowAction.DATA_ENTRY))!!

      offenderBooking.alerts.add(
        OffenderAlert(
          id = OffenderAlertId(offenderBooking, sequence = nextSequence + index),
          rootOffender = offenderBooking.rootOffender,
          alertDate = request.date,
          expiryDate = request.expiryDate,
          alertType = alertTypeRepository.findByIdOrNull(AlertType.pk(alertCode.parentCode!!))!!,
          alertCode = alertCode,
          authorizePersonText = request.authorisedBy,
          alertStatus = if (request.isActive) ACTIVE else INACTIVE,
          verifiedFlag = false,
          commentText = request.comment?.truncateToUtf8Length(4000),
          createUsername = request.createUsername,
        ).also {
          it.addWorkFlowLog(workActionCode = workActionCode)
        },
      )
    }

    return offenderBooking.alerts.map { alert ->
      CreateAlertResponse(
        bookingId = alert.id.offenderBooking.bookingId,
        alertSequence = alert.id.sequence,
        alertCode = alert.alertCode.toCodeDescription(),
        type = alert.alertType.toCodeDescription(),
      )
    }
  }

  fun updateAlert(bookingId: Long, alertSequence: Long, request: UpdateAlertRequest): AlertResponse {
    val offenderBooking = offenderBookingRepository.findByIdOrNull(bookingId)
      ?: throw NotFoundException("Booking $bookingId not found")
    val alert = offenderAlertRepository.findByIdOrNull(OffenderAlertId(offenderBooking, alertSequence))
      ?: throw NotFoundException("Alert $alertSequence on $bookingId not found")
    val workActionCode = workFlowActionRepository.findByIdOrNull(WorkFlowAction.pk(WorkFlowAction.MODIFIED))!!

    alert.expiryDate = request.expiryDate
    alert.alertStatus = if (request.isActive) ACTIVE else INACTIVE
    alert.commentText = request.comment?.truncateToUtf8Length(4000)
    alert.modifyUserId = request.updateUsername
    alert.modifyDatetime = LocalDateTime.now()
    alert.alertDate = request.date
    alert.authorizePersonText = request.authorisedBy

    alert.addWorkFlowLog(workActionCode = workActionCode, createUsername = request.updateUsername)

    return offenderAlertRepository.save(alert).toAlertResponse()
  }

  private fun OffenderAlert.toAlertResponse() = AlertResponse(
    bookingId = id.offenderBooking.bookingId,
    bookingSequence = id.offenderBooking.bookingSequence.toLong(),
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
      createDisplayName = this.createStaffUserAccount?.staff.asDisplayName(),
      modifyDatetime = modifyDatetime,
      modifyUserId = modifyUserId,
      modifyDisplayName = this.modifyStaffUserAccount?.staff.asDisplayName(),
      auditUserId = auditUserId,
      auditTimestamp = auditTimestamp,
      auditModuleName = auditModuleName,
      auditAdditionalInfo = auditAdditionalInfo,
      auditClientIpAddress = auditClientIpAddress,
      auditClientUserId = auditClientUserId,
      auditClientWorkstationName = auditClientWorkstationName,
    ),
  )

  fun deleteAlert(bookingId: Long, alertSequence: Long) {
    offenderBookingRepository.findByIdOrNull(bookingId)?.also {
      offenderAlertRepository.deleteById(OffenderAlertId(it, alertSequence))
    }
  }

  fun getAlerts(offenderNo: String): PrisonerAlertsResponse = offenderBookingRepository.findAllByOffenderNomsId(offenderNo).takeIf { it.isNotEmpty() }
    ?.let { bookings ->
      val latestBooking = bookings.first { it.bookingSequence == 1 }
      return PrisonerAlertsResponse(
        latestBookingAlerts = latestBooking.alerts.map { it.toAlertResponse() }.sortedBy { it.alertSequence },
      )
    } ?: throw NotFoundException("Prisoner with offender $offenderNo not found with any bookings")

  fun getActiveAlertsForReconciliation(offenderNo: String): PrisonerAlertsResponse = PrisonerAlertsResponse(
    latestBookingAlerts = getAlerts(offenderNo).latestBookingAlerts.filter { it.isActive },
  )

  fun getAlerts(bookingId: Long): BookingAlertsResponse = offenderBookingRepository.findByIdOrNull(bookingId)
    ?.let { booking -> offenderAlertRepository.findAllById_OffenderBooking(booking).map { it.toAlertResponse() } }
    ?.let { BookingAlertsResponse(it) }
    ?: throw NotFoundException("Prisoner with booking $bookingId not found")
}

private fun Staff?.asDisplayName(): String? = this?.let { "${it.firstName} ${it.lastName}" }

private fun OffenderBooking.hasActiveAlertOfCode(alertCode: String) = this.alerts.firstOrNull { it.alertCode.code == alertCode && it.alertStatus == ACTIVE } != null
