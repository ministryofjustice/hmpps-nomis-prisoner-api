package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonPhone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PhoneUsage
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository

@DslMarker
annotation class PersonPhoneDslMarker

@NomisDataDslMarker
interface PersonPhoneDsl

@Component
class PersonPhoneBuilderFactory(private val personPhoneBuilderRepository: PersonPhoneBuilderRepository) {
  fun builder() = PersonPhoneBuilder(personPhoneBuilderRepository = personPhoneBuilderRepository)
}

@Component
class PersonPhoneBuilderRepository(
  private val phoneUsageRepository: ReferenceCodeRepository<PhoneUsage>,

) {
  fun phoneUsageOf(code: String): PhoneUsage = phoneUsageRepository.findByIdOrNull(PhoneUsage.pk(code))!!
}

class PersonPhoneBuilder(private val personPhoneBuilderRepository: PersonPhoneBuilderRepository) : PersonPhoneDsl {

  fun build(
    person: Person,
    phoneType: String,
    phoneNo: String,
    extNo: String?,
  ): PersonPhone =
    PersonPhone(
      person = person,
      phoneType = personPhoneBuilderRepository.phoneUsageOf(phoneType),
      phoneNo = phoneNo,
      extNo = extNo,
    )
}
