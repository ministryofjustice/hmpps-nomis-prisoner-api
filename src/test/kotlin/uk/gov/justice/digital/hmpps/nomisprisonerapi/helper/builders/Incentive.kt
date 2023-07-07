package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IEPLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Incentive
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncentiveId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ReferenceCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.IncentiveRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDateTime

@DslMarker
annotation class NewIncentiveDslMarker

@NomisDataDslMarker
interface NewIncentiveDsl

@Component
class NewIncentiveBuilderRepository(
  private val incentiveRepository: IncentiveRepository,
  val iepLevelRepository: ReferenceCodeRepository<IEPLevel>,
) {
  fun save(incentive: Incentive) = incentiveRepository.save(incentive)
  fun lookupIepLevel(code: String): IEPLevel = iepLevelRepository.findByIdOrNull(ReferenceCode.Pk(IEPLevel.IEP_LEVEL, code))!!
}

@Component
class NewIncentiveBuilderFactory(private val repository: NewIncentiveBuilderRepository) {
  fun builder() = NewIncentiveBuilder(repository)
}

class NewIncentiveBuilder(private val repository: NewIncentiveBuilderRepository) : NewIncentiveDsl {
  fun build(
    offenderBooking: OffenderBooking,
    iepLevel: String,
    userId: String?,
    sequence: Long,
    commentText: String,
    auditModuleName: String?,
    iepDateTime: LocalDateTime,
  ): Incentive =
    Incentive(
      id = IncentiveId(offenderBooking = offenderBooking, sequence = sequence),
      commentText = commentText,
      iepDate = iepDateTime.toLocalDate(),
      iepTime = iepDateTime,
      location = offenderBooking.location!!,
      iepLevel = repository.lookupIepLevel(iepLevel),
      userId = userId,
      auditModuleName = auditModuleName,
    )
      .let { repository.save(it) }
}
