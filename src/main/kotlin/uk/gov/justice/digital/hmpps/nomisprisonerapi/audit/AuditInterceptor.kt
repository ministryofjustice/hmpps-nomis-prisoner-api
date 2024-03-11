package uk.gov.justice.digital.hmpps.nomisprisonerapi.audit

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
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
@Order(Ordered.LOWEST_PRECEDENCE)
@DependsOn("transactionManager") // Ensure that this runs after the transaction manager
class AuditInterceptor(private val storedProcedureRepository: StoredProcedureRepository) {
  @Around("@annotation(Audit)")
  fun annotatedEndpoint(proceedingJoinPoint: ProceedingJoinPoint): Any? {
    storedProcedureRepository.audit("DPS_SYNCHRONISATION")
    try {
      return proceedingJoinPoint.proceed()
    } finally {
      storedProcedureRepository.clearAudit()
      log.debug("annotatedEndpoint() : Cleared audit. Transaction is ${if (TransactionSynchronizationManager.isActualTransactionActive()) "" else "not "}active")
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
