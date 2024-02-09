package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
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

@DslMarker
annotation class OffenderAlertDslMarker

@NomisDataDslMarker
interface OffenderAlertDsl {
  @WorkFlowLogDslMarker
  fun workFlowLog(
    workActionCode: String,
    workFlowStatus: WorkFlowStatus,
    dsl: WorkFlowLogDsl.() -> Unit = {},
  ): WorkFlowLog
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
) {
  fun save(alert: OffenderAlert): OffenderAlert =
    repository.save(alert)

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
  ).apply {
    workFlow = addWorkFlowLog(workActionCode = repository.lookupWorkFLowAction(WorkFlowAction.DATA_ENTRY))
  }
    .let { repository.save(it) }
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
}
