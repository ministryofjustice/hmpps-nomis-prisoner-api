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
annotation class IncentiveDslMarker

@NomisDataDslMarker
interface IncentiveDsl

interface IncentiveDslApi {
  @IncentiveDslMarker
  fun incentive(
    iepLevelCode: String = "ENT",
    userId: String? = null,
    sequence: Long = 1,
    commentText: String = "comment",
    auditModuleName: String? = null,
    iepDateTime: LocalDateTime = LocalDateTime.now(),
  ): Incentive
}

@Component
class IncentiveBuilderRepository(
  private val incentiveRepository: IncentiveRepository,
  val iepLevelRepository: ReferenceCodeRepository<IEPLevel>,
) {
  fun save(incentive: Incentive): Incentive = incentiveRepository.save(incentive)
  fun lookupIepLevel(code: String): IEPLevel = iepLevelRepository.findByIdOrNull(ReferenceCode.Pk(IEPLevel.IEP_LEVEL, code))!!
}

@Component
class IncentiveBuilderFactory(private val repository: IncentiveBuilderRepository) {
  fun builder() = IncentiveBuilder(repository)
}

class IncentiveBuilder(private val repository: IncentiveBuilderRepository) :
  IncentiveDsl {
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
