package uk.gov.justice.digital.hmpps.nomisprisonerapi.audit

import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.AfterThrowing
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
class AuditInterceptorAfter(
  private val storedProcedureRepository: StoredProcedureRepository,
) {
  /**
   * After the annotated method returns successfully, reset the audit
   */
  @AfterReturning("@annotation(audit)")
  fun afterReturningAnnotatedMethod(audit: Audit) {
    // TODO can add try catch here to catch any reset fails
    log.debug("AfterReturning advice: resetting audit. Transaction is ${if (TransactionSynchronizationManager.isActualTransactionActive()) "" else "not "}active")
    storedProcedureRepository.resetAudit()
    log.info("AfterReturning advice: audit reset after successful execution.")
  }

  /**
   *
   * handle failures explicitly in AuditInterceptorBefore
   */
  @AfterThrowing(pointcut = "@annotation(audit)", throwing = "ex")
  fun afterThrowingAnnotatedMethod(audit: Audit, ex: Throwable) {
    log.warn("AfterThrowing advice: method with @Audit(${audit.auditModule}) threw exception: ${ex.message}")
    storedProcedureRepository.resetAudit()
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
