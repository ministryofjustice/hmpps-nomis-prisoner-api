package uk.gov.justice.digital.hmpps.nomisprisonerapi.audit

import org.aspectj.lang.annotation.After
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.DependsOn
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronizationManager
import uk.gov.justice.digital.hmpps.nomisprisonerapi.repository.StoredProcedureRepository

@Component
@Aspect
@Order(Ordered.LOWEST_PRECEDENCE) // this runs within transaction, before it commits
@DependsOn("transactionManager")
class AuditInterceptorAfter(private val storedProcedureRepository: StoredProcedureRepository) {
  @After("@annotation(Audit)")
  fun annotatedEndpoint() {
    log.debug("After advice: Clearing audit. Transaction is ${if (TransactionSynchronizationManager.isActualTransactionActive()) "" else "not "}active")
    storedProcedureRepository.clearAudit()
    log.debug("After advice: Cleared audit.")
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
