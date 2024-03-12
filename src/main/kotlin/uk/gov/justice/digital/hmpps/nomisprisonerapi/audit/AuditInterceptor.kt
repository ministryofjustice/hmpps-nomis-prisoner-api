package uk.gov.justice.digital.hmpps.nomisprisonerapi.audit

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.repository.StoredProcedureRepository

@Component
@Aspect
@Order(1)
class AuditInterceptor(private val storedProcedureRepository: StoredProcedureRepository) {
  @Around("@annotation(Audit)")
  fun annotatedEndpoint(proceedingJoinPoint: ProceedingJoinPoint): Any? {
    storedProcedureRepository.audit("DPS_SYNCHRONISATION")
    try {
      return proceedingJoinPoint.proceed()
    } finally {
      storedProcedureRepository.clearAudit()
    }
  }
}
