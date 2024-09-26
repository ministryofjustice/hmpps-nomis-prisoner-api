package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IdentifierType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonIdentifier
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonIdentifierPK
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository

@DslMarker
annotation class PersonIdentifierDslMarker

@NomisDataDslMarker
interface PersonIdentifierDsl

@Component
class PersonIdentifierBuilderFactory(
  private val personIdentifierBuilderRepository: PersonIdentifierBuilderRepository,

) {
  fun builder() = PersonIdentifierBuilder(
    personIdentifierBuilderRepository = personIdentifierBuilderRepository,
  )
}

@Component
class PersonIdentifierBuilderRepository(
  private val identifierTypeRepository: ReferenceCodeRepository<IdentifierType>,
) {
  fun identifierTypeOf(code: String): IdentifierType = identifierTypeRepository.findByIdOrNull(IdentifierType.pk(code))!!
}

class PersonIdentifierBuilder(
  private val personIdentifierBuilderRepository: PersonIdentifierBuilderRepository,
) : PersonIdentifierDsl {

  private lateinit var personIdentifier: PersonIdentifier

  fun build(
    person: Person,
    sequence: Long,
    type: String,
    identifier: String,
    issuedAuthority: String?,
  ): PersonIdentifier =
    PersonIdentifier(
      id = PersonIdentifierPK(person, sequence),
      identifierType = personIdentifierBuilderRepository.identifierTypeOf(type),
      identifier = identifier,
      issuedAuthority = issuedAuthority,
    )
      .also { personIdentifier = it }
}
