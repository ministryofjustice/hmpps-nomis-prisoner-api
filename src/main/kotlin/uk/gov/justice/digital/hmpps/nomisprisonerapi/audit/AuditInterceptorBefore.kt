
package uk.gov.justice.digital.hmpps.nomisprisonerapi.audit

import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronizationManager
import uk.gov.justice.digital.hmpps.nomisprisonerapi.repository.StoredProcedureRepository

// @Order(1) // this runs before transaction starts
@Aspect
@Component
class AuditInterceptorBefore(private val storedProcedureRepository: StoredProcedureRepository) {

  @Before("@annotation(audit)")
  fun beforeAnnotatedMethod(joinPoint: JoinPoint, audit: Audit) {
    val auditValue = audit.auditModule
    storedProcedureRepository.audit(auditValue)
    log.debug("Before advice: Set audit to $auditValue. Transaction is ${if (TransactionSynchronizationManager.isActualTransactionActive()) "" else "not "}active")
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
