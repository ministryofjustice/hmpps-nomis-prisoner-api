package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtOrder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentencePurpose
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SeriousnessLevelType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourtOrderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate

@DslMarker
annotation class CourtOrderDslMarker

@NomisDataDslMarker
interface CourtOrderDsl {
  @SentencePurposeDslMarker
  fun sentencePurpose(
    purposeCode: String = "REPAIR",
    orderPartyCode: String = "CRT",
    dsl: SentencePurposeDsl.() -> Unit = {},
  ): SentencePurpose
}

@Component
class CourtOrderBuilderFactory(
  private val repository: CourtOrderBuilderRepository,
  private val sentencePurposeBuilderFactory: SentencePurposeBuilderFactory,
) {
  fun builder(): CourtOrderBuilder {
    return CourtOrderBuilder(
      repository,
      sentencePurposeBuilderFactory,
    )
  }
}

@Component
class CourtOrderBuilderRepository(
  val repository: CourtOrderRepository,
  val agencyLocationRepository: AgencyLocationRepository,
  val seriousnessLevelTypeRepository: ReferenceCodeRepository<SeriousnessLevelType>,
) {
  fun save(courtOrder: CourtOrder): CourtOrder =
    repository.save(courtOrder)

  fun lookupAgency(id: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(id)!!

  fun lookupSeriousnessType(code: String): SeriousnessLevelType = seriousnessLevelTypeRepository.findByIdOrNull(
    SeriousnessLevelType.pk(code),
  )!!
}

class CourtOrderBuilder(
  private val repository: CourtOrderBuilderRepository,
  private val sentencePurposeBuilderFactory: SentencePurposeBuilderFactory,
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
    .let { repository.save(it) }
    .also { courtOrder = it }

  override fun sentencePurpose(
    purposeCode: String,
    orderPartyCode: String,
    dsl: SentencePurposeDsl.() -> Unit,
  ) =
    sentencePurposeBuilderFactory.builder().let { builder ->
      builder.build(
        purposeCode = purposeCode,
        orderPartyCode = orderPartyCode,
        courtOrder = courtOrder,
      )
        .also { courtOrder.sentencePurposes += it }
        .also { builder.apply(dsl) }
    }
}
