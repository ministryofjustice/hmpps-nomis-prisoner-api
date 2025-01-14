package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Corporate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateOrganisationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateOrganisationTypePK
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository

@DslMarker
annotation class CorporateTypeDslMarker

@NomisDataDslMarker
interface CorporateTypeDsl

@Component
class CorporateTypeBuilderFactory(
  private val repository: CorporateTypeBuilderRepository,
) {
  fun builder(): CorporateTypeBuilder = CorporateTypeBuilder(repository)
}

@Component
class CorporateTypeBuilderRepository(
  private val corporateOrganisationTypeRepository: ReferenceCodeRepository<CorporateOrganisationType>,
) {
  fun corporateOrganisationTypeOf(code: String): CorporateOrganisationType = corporateOrganisationTypeRepository.findByIdOrNull(CorporateOrganisationType.pk(code))!!
}

class CorporateTypeBuilder(
  private val repository: CorporateTypeBuilderRepository,
) : CorporateTypeDsl {
  private lateinit var corporateType: CorporateType

  fun build(
    corporate: Corporate,
    typeCode: String,
  ): CorporateType = CorporateType(
    id = CorporateOrganisationTypePK(corporate = corporate, typeCode = typeCode),
    type = repository.corporateOrganisationTypeOf(typeCode),
  )
    .also { corporateType = it }
}
