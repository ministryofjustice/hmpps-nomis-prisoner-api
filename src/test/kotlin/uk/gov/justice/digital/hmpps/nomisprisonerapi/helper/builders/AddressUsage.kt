package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Address
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AddressUsage
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AddressUsageId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AddressUsageType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository

@DslMarker
annotation class AddressUsageDslMarker

@NomisDataDslMarker
interface AddressUsageDsl

@Component
class AddressUsageBuilderFactory(
  private val repository: AddressUsageBuilderRepository,
) {
  fun builder(): AddressUsageBuilder = AddressUsageBuilder(repository)
}

@Component
class AddressUsageBuilderRepository(
  private val addressUsageTypeRepository: ReferenceCodeRepository<AddressUsageType>,
) {
  fun addressUsageTypeOf(code: String): AddressUsageType? = addressUsageTypeRepository.findByIdOrNull(AddressUsageType.pk(code))
}

class AddressUsageBuilder(
  private val repository: AddressUsageBuilderRepository,
) : AddressUsageDsl {
  private lateinit var addressUsage: AddressUsage

  fun build(
    address: Address,
    usageCode: String,
    active: Boolean,
  ): AddressUsage = AddressUsage(
    id = AddressUsageId(address = address, usageCode = usageCode),
    addressUsage = repository.addressUsageTypeOf(usageCode),
    active = active,
  )
    .also { addressUsage = it }
}
