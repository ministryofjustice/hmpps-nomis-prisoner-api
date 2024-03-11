package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AlertCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AlertStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AlertType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAlert
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAlertId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.WorkFlow
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.WorkFlowAction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.WorkFlowLog
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.WorkFlowStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderAlertRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class OffenderAlertDslMarker

@DslMarker
annotation class OffenderAlertAuditDslMarker

@NomisDataDslMarker
interface OffenderAlertDsl {
  @WorkFlowLogDslMarker
  fun workFlowLog(
    workActionCode: String,
    workFlowStatus: WorkFlowStatus,
    dsl: WorkFlowLogDsl.() -> Unit = {},
  ): WorkFlowLog

  @OffenderAlertAuditDslMarker
  fun audit(
    createDatetime: LocalDateTime = LocalDateTime.now(),
    createUsername: String = "SARAH.BEEKS",
    modifyUserId: String? = null,
    modifyDatetime: LocalDateTime? = null,
    auditTimestamp: LocalDateTime? = LocalDateTime.now(),
    auditUserId: String? = "SARAH.BEEKS",
    auditModuleName: String? = "OCDALERT",
    auditClientUserId: String? = "sarah.beeks",
    auditClientIpAddress: String? = "10.1.1.23",
    auditClientWorkstationName: String? = "APP",
    auditAdditionalInfo: String? = null,
  )
}

@Component
class OffenderAlertBuilderFactory(
  private val repository: OffenderAlertBuilderRepository,
  private val workFlowLogBuilderFactory: WorkFlowLogBuilderFactory,
) {
  fun builder(): OffenderAlertBuilder {
    return OffenderAlertBuilder(repository, workFlowLogBuilderFactory)
  }
}

@Component
class OffenderAlertBuilderRepository(
  private val repository: OffenderAlertRepository,
  private val alertCodeRepository: ReferenceCodeRepository<AlertCode>,
  private val alertTypeRepository: ReferenceCodeRepository<AlertType>,
  private val workFlowActionRepository: ReferenceCodeRepository<WorkFlowAction>,
  private val jdbcTemplate: NamedParameterJdbcTemplate,
) {
  fun save(alert: OffenderAlert): OffenderAlert =
    repository.saveAndFlush(alert)

  fun updateAudit(
    id: OffenderAlertId,
    createDatetime: LocalDateTime,
    createUsername: String,
    modifyUserId: String?,
    modifyDatetime: LocalDateTime?,
    auditTimestamp: LocalDateTime?,
    auditUserId: String?,
    auditModuleName: String?,
    auditClientUserId: String?,
    auditClientIpAddress: String?,
    auditClientWorkstationName: String?,
    auditAdditionalInfo: String?,
  ) {
    jdbcTemplate.update(
      """
      UPDATE OFFENDER_ALERTS 
      SET 
        CREATE_USER_ID = :createUsername, 
        CREATE_DATETIME = :createDatetime,
        MODIFY_USER_ID = :modifyUserId,
        MODIFY_DATETIME = :modifyDatetime,
        AUDIT_TIMESTAMP = :auditTimestamp,
        AUDIT_USER_ID = :auditUserId,
        AUDIT_MODULE_NAME = :auditModuleName,
        AUDIT_CLIENT_USER_ID = :auditClientUserId,
        AUDIT_CLIENT_IP_ADDRESS = :auditClientIpAddress,
        AUDIT_CLIENT_WORKSTATION_NAME = :auditClientWorkstationName,
        AUDIT_ADDITIONAL_INFO = :auditAdditionalInfo
      WHERE OFFENDER_BOOK_ID = :bookingId 
      AND ALERT_SEQ = :alertSequence
      """,
      mapOf(
        "createUsername" to createUsername,
        "createDatetime" to createDatetime,
        "modifyUserId" to modifyUserId,
        "modifyDatetime" to modifyDatetime,
        "auditTimestamp" to auditTimestamp,
        "auditUserId" to auditUserId,
        "auditModuleName" to auditModuleName,
        "auditClientUserId" to auditClientUserId,
        "auditClientIpAddress" to auditClientIpAddress,
        "auditClientWorkstationName" to auditClientWorkstationName,
        "auditAdditionalInfo" to auditAdditionalInfo,
        "bookingId" to id.offenderBooking.bookingId,
        "alertSequence" to id.sequence,
      ),
    )
  }

  fun lookupAlertCode(code: String): AlertCode =
    alertCodeRepository.findByIdOrNull(AlertCode.pk(code))!!

  fun lookupAlertType(code: String): AlertType =
    alertTypeRepository.findByIdOrNull(AlertType.pk(code))!!

  fun lookupWorkFLowAction(code: String): WorkFlowAction =
    workFlowActionRepository.findByIdOrNull(WorkFlowAction.pk(code))!!
}

class OffenderAlertBuilder(
  private val repository: OffenderAlertBuilderRepository,
  private val workFlowLogBuilderFactory: WorkFlowLogBuilderFactory,
) : OffenderAlertDsl {
  private lateinit var workFlow: WorkFlow
  private lateinit var alert: OffenderAlert

  fun build(
    offenderBooking: OffenderBooking,
    sequence: Long?,
    alertCode: String,
    typeCode: String,
    date: LocalDate,
    expiryDate: LocalDate?,
    authorizePersonText: String?,
    status: AlertStatus,
    commentText: String?,
    verifiedFlag: Boolean,
    createUsername: String,
  ): OffenderAlert = OffenderAlert(
    id = OffenderAlertId(
      offenderBooking = offenderBooking,
      sequence = sequence ?: ((offenderBooking.alerts.maxByOrNull { it.id.sequence }?.id?.sequence ?: 0) + 1),
    ),
    alertCode = repository.lookupAlertCode(alertCode),
    alertType = repository.lookupAlertType(typeCode),
    alertDate = date,
    expiryDate = expiryDate,
    authorizePersonText = authorizePersonText,
    alertStatus = status,
    commentText = commentText,
    verifiedFlag = verifiedFlag,
    createUsername = createUsername,
  ).apply {
    addWorkFlowLog(workActionCode = repository.lookupWorkFLowAction(WorkFlowAction.DATA_ENTRY))
  }
    .let { repository.save(it) }
    .also { workFlow = it.workFlows.first() }
    .also { alert = it }

  override fun workFlowLog(
    workActionCode: String,
    workFlowStatus: WorkFlowStatus,
    dsl: WorkFlowLogDsl.() -> Unit,
  ): WorkFlowLog = workFlowLogBuilderFactory.builder().let { builder ->
    builder.build(
      workFlow = workFlow,
      workActionCode = workActionCode,
      workFlowStatus = workFlowStatus,
    )
      .also { workFlow.logs += it }
      .also { builder.apply(dsl) }
  }

  override fun audit(
    createDatetime: LocalDateTime,
    createUsername: String,
    modifyUserId: String?,
    modifyDatetime: LocalDateTime?,
    auditTimestamp: LocalDateTime?,
    auditUserId: String?,
    auditModuleName: String?,
    auditClientUserId: String?,
    auditClientIpAddress: String?,
    auditClientWorkstationName: String?,
    auditAdditionalInfo: String?,
  ) = repository.updateAudit(
    id = alert.id,
    createDatetime = createDatetime,
    createUsername = createUsername,
    modifyUserId = modifyUserId,
    modifyDatetime = modifyDatetime,
    auditTimestamp = auditTimestamp,
    auditUserId = auditUserId,
    auditModuleName = auditModuleName,
    auditClientUserId = auditClientUserId,
    auditClientIpAddress = auditClientIpAddress,
    auditClientWorkstationName = auditClientWorkstationName,
    auditAdditionalInfo = auditAdditionalInfo,
  )
}
