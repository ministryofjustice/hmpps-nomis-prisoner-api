package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.WorkFlow
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.WorkFlowAction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.WorkFlowLog
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.WorkFlowLogId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.WorkFlowStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.WorkFlowLogRepository

@DslMarker
annotation class WorkFlowLogDslMarker

@NomisDataDslMarker
interface WorkFlowLogDsl

@Component
class WorkFlowLogBuilderFactory(
  private val repository: WorkFlowLogBuilderRepository,
) {
  fun builder(): WorkFlowLogBuilder {
    return WorkFlowLogBuilder(repository)
  }
}

@Component
class WorkFlowLogBuilderRepository(
  private val workFlowActionRepository: ReferenceCodeRepository<WorkFlowAction>,
  private val workFlowLogRepository: WorkFlowLogRepository,
) {
  fun lookupWorkFlowAction(code: String): WorkFlowAction =
    workFlowActionRepository.findByIdOrNull(WorkFlowAction.pk(code))!!

  fun save(workFlowLog: WorkFlowLog): WorkFlowLog =
    workFlowLogRepository.save(workFlowLog)
}

class WorkFlowLogBuilder(
  private val repository: WorkFlowLogBuilderRepository,
) : WorkFlowLogDsl {
  private lateinit var workFlowLog: WorkFlowLog

  fun build(
    workFlow: WorkFlow,
    workActionCode: String,
    workFlowStatus: WorkFlowStatus,
  ): WorkFlowLog = WorkFlowLog(
    id = WorkFlowLogId(workFlow, workFlow.nextSequence()),
    workActionCode = repository.lookupWorkFlowAction(workActionCode),
    workFlowStatus = workFlowStatus,
  )
    .let { repository.save(it) }
    .also { workFlowLog = it }
}
