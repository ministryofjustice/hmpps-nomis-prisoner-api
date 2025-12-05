package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ReferenceCode.Pk
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrderType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitOrderRepository
import java.time.LocalDate

@DslMarker
annotation class VisitOrderDslMarker

@NomisDataDslMarker
interface VisitOrderDsl

@Component
class VisitOrderBuilderRepository(
  private val visitOrderRepository: VisitOrderRepository,
  private val visitOrderTypeRepository: ReferenceCodeRepository<VisitOrderType>,
  private val visitStatusRepository: ReferenceCodeRepository<VisitStatus>,
) {
  fun save(visitOrder: VisitOrder): VisitOrder = visitOrderRepository.saveAndFlush(visitOrder)

  fun lookupVisitOrderType(code: String): VisitOrderType = visitOrderTypeRepository.findByIdOrNull(Pk(VisitOrderType.VISIT_ORDER_TYPE, code))!!
  fun lookupVisitStatus(code: String): VisitStatus = visitStatusRepository.findByIdOrNull(Pk(VisitStatus.VISIT_STATUS, code))!!
}

@Component
class VisitOrderBuilderFactory(
  private val repository: VisitOrderBuilderRepository,
) {
  fun builder() = VisitOrderBuilderRepositoryBuilder(repository)
}

class VisitOrderBuilderRepositoryBuilder(
  private val repository: VisitOrderBuilderRepository,
) : VisitOrderDsl {

  private lateinit var visitOrder: VisitOrder

  fun build(
    offenderBooking: OffenderBooking,
    visitOrderTypeCode: String,
    visitStatusCode: String,
    orderNumber: Long,
    issueDate: LocalDate,
  ): VisitOrder = VisitOrder(
    offenderBooking = offenderBooking,
    visitOrderNumber = orderNumber,
    issueDate = issueDate,
    visitOrderType = repository.lookupVisitOrderType(visitOrderTypeCode),
    status = repository.lookupVisitStatus(visitStatusCode),
  )
    .let { repository.save(it) }
    .also { visitOrder = it }
}
