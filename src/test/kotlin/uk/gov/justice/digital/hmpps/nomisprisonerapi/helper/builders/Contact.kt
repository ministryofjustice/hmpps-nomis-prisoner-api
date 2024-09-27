package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ContactType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderContactPerson
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderPersonRestrict
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ReferenceCode.Pk
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.RelationshipType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderContactPersonRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate

@DslMarker
annotation class OffenderContactPersonDslMarker

@NomisDataDslMarker
interface OffenderContactPersonDsl {

  @OffenderPersonRestrictsDslMarker
  fun restriction(
    restrictionType: String = "BAN",
    enteredStaff: Staff,
    comment: String? = null,
    effectiveDate: String = LocalDate.now().toString(),
    expiryDate: String? = null,
    dsl: OffenderPersonRestrictsDsl.() -> Unit = {},
  ): OffenderPersonRestrict
}

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
  private val offenderPersonRestrictsBuilderFactory: OffenderPersonRestrictsBuilderFactory,
) {
  fun builder() = OffenderContactPersonBuilderRepositoryBuilder(repository, offenderPersonRestrictsBuilderFactory)
}

class OffenderContactPersonBuilderRepositoryBuilder(
  private val repository: OffenderContactPersonBuilderRepository,
  private val offenderPersonRestrictsBuilderFactory: OffenderPersonRestrictsBuilderFactory,
) : OffenderContactPersonDsl {
  private lateinit var contact: OffenderContactPerson

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
    expiryDate: LocalDate?,
  ): OffenderContactPerson =
    OffenderContactPerson(
      offenderBooking = offenderBooking,
      person = person,
      rootOffender = null,
      relationshipType = repository.lookupRelationshipType(relationshipType),
      contactType = repository.lookupContactType(contactType),
      active = active,
      nextOfKin = nextOfKin,
      emergencyContact = emergencyContact,
      approvedVisitor = approvedVisitor,
      comment = comment,
      expiryDate = expiryDate,
    )
      .let { repository.save(it) }
      .also { contact = it }

  override fun restriction(
    restrictionType: String,
    enteredStaff: Staff,
    comment: String?,
    effectiveDate: String,
    expiryDate: String?,
    dsl: OffenderPersonRestrictsDsl.() -> Unit,
  ): OffenderPersonRestrict =
    offenderPersonRestrictsBuilderFactory.builder().let { builder ->
      builder.build(
        contactPerson = contact,
        restrictionType = restrictionType,
        enteredStaff = enteredStaff,
        comment = comment,
        effectiveDate = LocalDate.parse(effectiveDate),
        expiryDate = expiryDate?.let { LocalDate.parse(it) },
      )
        .also { contact.restrictions += it }
        .also { builder.apply(dsl) }
    }
}
