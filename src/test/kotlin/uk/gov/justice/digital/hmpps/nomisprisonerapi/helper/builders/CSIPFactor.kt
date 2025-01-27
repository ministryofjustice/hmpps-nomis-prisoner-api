package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPFactor
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPFactorType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPReport
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository

@DslMarker
annotation class CSIPFactorDslMarker

@NomisDataDslMarker
interface CSIPFactorDsl

@Component
class CSIPFactorBuilderFactory(
  private val repository: CSIPFactorBuilderRepository,
) {
  fun builder() = CSIPFactorBuilder(repository)
}

@Component
class CSIPFactorBuilderRepository(
  val repository: ReferenceCodeRepository<CSIPFactorType>,
) {
  fun lookupFactorType(code: String) = repository.findByIdOrNull(CSIPFactorType.pk(code))!!
}

class CSIPFactorBuilder(
  private val repository: CSIPFactorBuilderRepository,
) : CSIPFactorDsl {
  fun build(
    csipReport: CSIPReport,
    type: String,
    comment: String?,
  ): CSIPFactor = CSIPFactor(
    csipReport = csipReport,
    type = repository.lookupFactorType(type),
    comment = comment,
  )
}
