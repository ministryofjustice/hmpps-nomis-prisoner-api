package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
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
import java.time.LocalDateTime

@DslMarker
annotation class CourtOrderDslMarker

@CourtOrderDslMarker
interface CourtOrderDsl {
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
  fun builder(): CourtOrderBuilder = CourtOrderBuilder(
    repository,
    sentencePurposeBuilderFactory,
  )
}

@Component
class CourtOrderBuilderRepository(
  val repository: CourtOrderRepository,
  val agencyLocationRepository: AgencyLocationRepository,
  val seriousnessLevelTypeRepository: ReferenceCodeRepository<SeriousnessLevelType>,
  private val jdbcTemplate: JdbcTemplate,
) {
  fun save(courtOrder: CourtOrder): CourtOrder = repository.saveAndFlush(courtOrder)

  fun lookupAgency(id: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(id)!!

  fun lookupSeriousnessType(code: String): SeriousnessLevelType = seriousnessLevelTypeRepository.findByIdOrNull(
    SeriousnessLevelType.pk(code),
  )!!

  fun updateCreateDatetime(courtOrder: CourtOrder, whenCreated: LocalDateTime) {
    jdbcTemplate.update("update ORDERS set CREATE_DATETIME = ? where ORDER_ID = ?", whenCreated, courtOrder.id)
  }
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
    whenCreated: LocalDateTime?,
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
    // always a court case in the context of this test data
    courtCase = courtEvent.courtCase!!,
    courtEvent = courtEvent,
  )
    .let { repository.save(it) }
    .also { whenCreated?.run { repository.updateCreateDatetime(it, whenCreated) } }
    .also { courtOrder = it }

  override fun sentencePurpose(
    purposeCode: String,
    orderPartyCode: String,
    dsl: SentencePurposeDsl.() -> Unit,
  ) = sentencePurposeBuilderFactory.builder().let { builder ->
    builder.build(
      purposeCode = purposeCode,
      orderPartyCode = orderPartyCode,
      courtOrder = courtOrder,
    )
      .also { courtOrder.sentencePurposes += it }
      .also { builder.apply(dsl) }
  }
}
