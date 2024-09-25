package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Corporate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CorporateRepository
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class CorporateDslMarker

@NomisDataDslMarker
interface CorporateDsl

@Component
class CorporateBuilderFactory(
  private val repository: CorporateBuilderRepository,
) {
  fun builder(): CorporateBuilder = CorporateBuilder(repository)
}

@Component
class CorporateBuilderRepository(
  private val corporateRepository: CorporateRepository,
) {
  fun save(corporate: Corporate): Corporate = corporateRepository.save(corporate)
}

class CorporateBuilder(
  private val repository: CorporateBuilderRepository,
) : CorporateDsl {
  private lateinit var corporate: Corporate

  fun build(
    corporateName: String,
    caseloadId: String?,
    createdDate: LocalDateTime,
    commentText: String?,
    suspended: Boolean,
    feiNumber: String?,
    active: Boolean,
    expiryDate: LocalDate?,
    taxNo: String?,
  ): Corporate = Corporate(
    corporateName = corporateName,
    caseloadId = caseloadId,
    createdDate = createdDate,
    commentText = commentText,
    suspended = suspended,
    feiNumber = feiNumber,
    active = active,
    expiryDate = expiryDate,
    taxNo = taxNo,
  )
    .let { repository.save(it) }
    .also { corporate = it }
}
