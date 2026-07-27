package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ImprisonmentStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderImprisonmentStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderImprisonmentStatusId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ImprisonmentStatusRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderImprisonmentStatusRepository
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class OffenderImprisonmentStatusDslMarker

@OffenderImprisonmentStatusDslMarker
interface OffenderImprisonmentStatusDsl

@Component
class OffenderImprisonmentStatusBuilderRepository(
  private val offenderImprisonmentStatusRepository: OffenderImprisonmentStatusRepository,
  private val imprisonmentStatusRepository: ImprisonmentStatusRepository,
) {
  fun save(offenderImprisonmentStatus: OffenderImprisonmentStatus): OffenderImprisonmentStatus = offenderImprisonmentStatusRepository
    .saveAndFlush(offenderImprisonmentStatus)

  fun lookupImprisonmentStatus(code: String): ImprisonmentStatus = imprisonmentStatusRepository.findByCode(code)
    ?: throw BadDataException("ImprisonmentStatus $code not found")
}

@Component
class OffenderImprisonmentStatusBuilderFactory(
  val repository: OffenderImprisonmentStatusBuilderRepository,
) {
  fun builder() = OffenderImprisonmentStatusBuilder(repository)
}

class OffenderImprisonmentStatusBuilder(
  private val repository: OffenderImprisonmentStatusBuilderRepository,
) : OffenderImprisonmentStatusDsl {

  fun build(
    offenderBooking: OffenderBooking,
    sequence: Long? = null,
    statusCode: String,
    effectiveDateTime: LocalDateTime,
    expiryDate: LocalDate?,
    createDate: LocalDate,
    latestStatus: Boolean,
  ): OffenderImprisonmentStatus = OffenderImprisonmentStatus(
    id = OffenderImprisonmentStatusId(offenderBooking, sequence = sequence ?: ((offenderBooking.imprisonmentStatuses.maxByOrNull { it.id.sequence }?.id?.sequence ?: 0) + 1)),
    statusCode = statusCode,
    status = repository.lookupImprisonmentStatus(statusCode),
    effectiveDate = effectiveDateTime.toLocalDate(),
    effectiveTime = effectiveDateTime,
    expiryDate = expiryDate,
    createDate = createDate,
    prison = offenderBooking.location,
    latestStatus = latestStatus,
  )
    .let {
      repository.save(it)
    }
}
