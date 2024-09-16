package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ContactType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderContactPerson
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ReferenceCode.Pk
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.RelationshipType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderContactPersonRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository

@DslMarker
annotation class OffenderContactPersonDslMarker

@NomisDataDslMarker
interface OffenderContactPersonDsl

@Component
class OffenderContactPersonBuilderRepository(
  private val offenderContactPersonRepository: OffenderContactPersonRepository,
  private val relationshipTypeRepository: ReferenceCodeRepository<RelationshipType>,
  private val contactTypeRepository: ReferenceCodeRepository<ContactType>,
) {
  fun save(contact: OffenderContactPerson): OffenderContactPerson = offenderContactPersonRepository.save(contact)
  fun lookupRelationshipType(code: String): RelationshipType = relationshipTypeRepository.findByIdOrNull(Pk(RelationshipType.RELATIONSHIP, code))!!
  fun lookupContactType(code: String): ContactType = contactTypeRepository.findByIdOrNull(Pk(ContactType.CONTACTS, code))!!
}

@Component
class OffenderContactPersonBuilderFactory(
  private val repository: OffenderContactPersonBuilderRepository,
) {
  fun builder() = OffenderContactPersonBuilderRepositoryBuilder(repository)
}

class OffenderContactPersonBuilderRepositoryBuilder(private val repository: OffenderContactPersonBuilderRepository) : OffenderContactPersonDsl {
  fun build(
    offenderBooking: OffenderBooking,
    person: Person,
    relationshipType: String,
    contactType: String,
    active: Boolean,
    nextOfKin: Boolean,
    emergencyContact: Boolean,
    approvedVisitor: Boolean,
    comment: String?,
  ): OffenderContactPerson =
    OffenderContactPerson(
      offenderBooking = offenderBooking,
      person = person,
      relationshipType = repository.lookupRelationshipType(relationshipType),
      contactType = repository.lookupContactType(contactType),
      active = active,
      nextOfKin = nextOfKin,
      emergencyContact = emergencyContact,
      approvedVisitor = approvedVisitor,
      comment = comment,
    )
      .let { repository.save(it) }
}
