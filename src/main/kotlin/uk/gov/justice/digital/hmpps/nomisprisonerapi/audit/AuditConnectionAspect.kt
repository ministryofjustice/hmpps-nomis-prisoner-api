package uk.gov.justice.digital.hmpps.nomisprisonerapi.audit

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.jdbc.datasource.DataSourceUtils
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
class AuditConnectionAspect(
  private val auditHandler: AuditConnectionHandler,
  private val dataSource: DataSource,
) {
  @Around(
    """
      @within(org.springframework.transaction.annotation.Transactional) ||  
      @annotation(org.springframework.transaction.annotation.Transactional) || 
      @within(jakarta.transaction.Transactional) || 
      @annotation(jakarta.transaction.Transactional)
      """,
  )
  fun applyAuditModuleAround(joinPoint: ProceedingJoinPoint): Any? {
    val conn = DataSourceUtils.getConnection(dataSource)

    try {
      auditHandler.applyAuditModule(conn)
      return joinPoint.proceed()
    } finally {
      DataSourceUtils.releaseConnection(conn, dataSource)
    }
  }
}
