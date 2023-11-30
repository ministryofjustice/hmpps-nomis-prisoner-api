package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtOrder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SeriousnessLevelType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate

@DslMarker
annotation class CourtOrderDslMarker

@NomisDataDslMarker
interface CourtOrderDsl

@Component
class CourtOrderBuilderFactory(
  private val repository: CourtOrderBuilderRepository,
) {
  fun builder(): CourtOrderBuilder {
    return CourtOrderBuilder(
      repository,
    )
  }
}

@Component
class CourtOrderBuilderRepository(
  val agencyLocationRepository: AgencyLocationRepository,
  val seriousnessLevelTypeRepository: ReferenceCodeRepository<SeriousnessLevelType>,
) {
  fun lookupAgency(id: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(id)!!

  fun lookupSeriousnessType(code: String): SeriousnessLevelType = seriousnessLevelTypeRepository.findByIdOrNull(
    SeriousnessLevelType.pk(code),
  )!!
}

class CourtOrderBuilder(
  private val repository: CourtOrderBuilderRepository,
) : CourtOrderDsl {
  private lateinit var courtOrder: CourtOrder

  fun build(
    courtEvent: CourtEvent,
    courtDate: LocalDate,
    issuingCourt: String,
    orderType: String,
    orderStatus: String,
    requestDate: LocalDate?,
    dueDate: LocalDate?,
    courtInfoId: String?,
    seriousnessLevel: String?,
    commentText: String?,
    nonReportFlag: Boolean,
  ): CourtOrder = CourtOrder(
    courtDate = courtDate,
    issuingCourt = repository.lookupAgency(issuingCourt),
    orderType = orderType,
    orderStatus = orderStatus,
    requestDate = requestDate,
    dueDate = dueDate,
    courtInfoId = courtInfoId,
    seriousnessLevel = seriousnessLevel?.let { repository.lookupSeriousnessType(it) },
    commentText = commentText,
    nonReportFlag = nonReportFlag,
    offenderBooking = courtEvent.offenderBooking,
    courtCase = courtEvent.courtCase!!, // always a court case in the context of this test data
    courtEvent = courtEvent,
  )
    .also { courtOrder = it }
}
