package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
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
import java.time.LocalDateTime

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
    whenCreated: LocalDateTime? = null,
    whoCreated: String? = null,
    dsl: OffenderPersonRestrictsDsl.() -> Unit = {},
  ): OffenderPersonRestrict
}

@Component
class OffenderContactPersonBuilderRepository(
  private val offenderContactPersonRepository: OffenderContactPersonRepository,
  private val relationshipTypeRepository: ReferenceCodeRepository<RelationshipType>,
  private val contactTypeRepository: ReferenceCodeRepository<ContactType>,
  private val jdbcTemplate: JdbcTemplate,
) {
  fun save(contact: OffenderContactPerson): OffenderContactPerson = offenderContactPersonRepository.saveAndFlush(contact)
  fun lookupRelationshipType(code: String): RelationshipType = relationshipTypeRepository.findByIdOrNull(Pk(RelationshipType.RELATIONSHIP, code))!!
  fun lookupContactType(code: String): ContactType = contactTypeRepository.findByIdOrNull(Pk(ContactType.CONTACTS, code))!!
  fun updateCreateDatetime(contact: OffenderContactPerson, whenCreated: LocalDateTime) {
    jdbcTemplate.update("update OFFENDER_CONTACT_PERSONS set CREATE_DATETIME = ? where OFFENDER_CONTACT_PERSON_ID = ?", whenCreated, contact.id)
  }
  fun updateCreateUsername(contact: OffenderContactPerson, whoCreated: String) {
    jdbcTemplate.update("update OFFENDER_CONTACT_PERSONS set CREATE_USER_ID = ? where OFFENDER_CONTACT_PERSON_ID = ?", whoCreated, contact.id)
  }
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
    whenCreated: LocalDateTime?,
    whoCreated: String?,
  ): OffenderContactPerson = OffenderContactPerson(
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
    .also {
      if (whenCreated != null) {
        repository.updateCreateDatetime(it, whenCreated)
      }
      if (whoCreated != null) {
        repository.updateCreateUsername(it, whoCreated)
      }
    }
    .also { contact = it }

  override fun restriction(
    restrictionType: String,
    enteredStaff: Staff,
    comment: String?,
    effectiveDate: String,
    expiryDate: String?,
    whenCreated: LocalDateTime?,
    whoCreated: String?,
    dsl: OffenderPersonRestrictsDsl.() -> Unit,
  ): OffenderPersonRestrict = offenderPersonRestrictsBuilderFactory.builder().let { builder ->
    builder.build(
      contactPerson = contact,
      restrictionType = restrictionType,
      enteredStaff = enteredStaff,
      comment = comment,
      effectiveDate = LocalDate.parse(effectiveDate),
      expiryDate = expiryDate?.let { LocalDate.parse(it) },
      whenCreated = whenCreated,
      whoCreated = whoCreated,
    )
      .also { contact.restrictions += it }
      .also { builder.apply(dsl) }
  }
}
