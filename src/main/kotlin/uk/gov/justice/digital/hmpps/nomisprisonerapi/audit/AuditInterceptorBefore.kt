package uk.gov.justice.digital.hmpps.nomisprisonerapi.audit

import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronizationManager
import uk.gov.justice.digital.hmpps.nomisprisonerapi.repository.StoredProcedureRepository

@Component
@Aspect
@Order(1) // this runs before transaction starts
class AuditInterceptorBefore(private val storedProcedureRepository: StoredProcedureRepository) {
  @Before("@annotation(Audit)")
  fun annotatedEndpoint() {
    storedProcedureRepository.audit("DPS_SYNCHRONISATION")
    log.debug("Before advice: Set audit. Transaction is ${if (TransactionSynchronizationManager.isActualTransactionActive()) "" else "not "}active")
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
