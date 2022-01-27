package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ContactType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Gender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ReferenceCode.Pk
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.RelationshipType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Visit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitRepository

@Repository
@Transactional
class Repository(
  val genderRepository: ReferenceCodeRepository<Gender>,
  val contactTypeRepository: ReferenceCodeRepository<ContactType>,
  val relationshipTypeRepository: ReferenceCodeRepository<RelationshipType>,
  val offenderRepository: OffenderRepository,
  val agencyLocationRepository: AgencyLocationRepository,
  val personRepository: PersonRepository,
  val visitRepository: VisitRepository,
  val visitStatusRepository: ReferenceCodeRepository<VisitStatus>,
) {
  fun save(offenderBuilder: OffenderBuilder): Offender {
    val gender = lookupGender(offenderBuilder.genderCode)

    val offender = save(offenderBuilder.build(gender)).apply {
      rootOffenderId = id
    }

    offender.bookings.addAll(
      offenderBuilder.bookingBuilders.mapIndexed { index, bookingBuilder ->
        val booking = bookingBuilder.build(offender, index, lookupAgency(bookingBuilder.agencyLocationId))
        bookingBuilder.visitBalanceBuilder?.run {
          booking.visitBalance = this.build(booking)
        }
        booking.contacts.addAll(
          bookingBuilder.contacts.map {
            it.build(booking, lookupContactType(it.contactType), lookupRelationshipType(it.relationshipType))
          }
        )
        booking
      }
    )

    offenderRepository.saveAndFlush(offender)
    return offender
  }

  fun save(personBuilder: PersonBuilder): Person = personRepository.save(personBuilder.build())
  fun save(offender: Offender): Offender = offenderRepository.saveAndFlush(offender)

  fun lookupGender(code: String): Gender = genderRepository.findByIdOrNull(Pk(Gender.SEX, code))!!
  fun lookupContactType(code: String): ContactType =
    contactTypeRepository.findByIdOrNull(Pk(ContactType.CONTACTS, code))!!

  fun lookupRelationshipType(code: String): RelationshipType =
    relationshipTypeRepository.findByIdOrNull(Pk(RelationshipType.RELATIONSHIP, code))!!

  fun lookupAgency(id: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(id)!!
  fun delete(offender: Offender) = offenderRepository.deleteById(offender.id)
  fun delete(people: Collection<Person>) = personRepository.deleteAllById(people.map { it.id })

  fun lookupVisit(visitId: Long?): Visit {
    val visit = visitRepository.findById(visitId!!).orElseThrow()
    visit.visitors.size // hydrate
    return visit
  }

  fun changeVisitStatus(visitId: Long?) {
      val visit = visitRepository.findById(visitId!!).orElseThrow()
      visit.visitStatus = visitStatusRepository.findById(VisitStatus.NORM).orElseThrow()
  }
}
