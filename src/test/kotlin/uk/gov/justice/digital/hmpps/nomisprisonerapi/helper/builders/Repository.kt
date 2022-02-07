package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitType
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
  val visitTypeRepository: ReferenceCodeRepository<VisitType>,
) {
  @Autowired
  var jdbcTemplate: JdbcTemplate? = null
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
            it.build(booking, lookupContactType(it.contactTypeCode), lookupRelationshipType(it.relationshipTypeCode))
          }
        )
        booking.visits.addAll(
          bookingBuilder.visits.map { visitBuilder ->
            val visit = visitBuilder.build(
              offenderBooking = booking,
              visitType = lookupVisitType(visitBuilder.visitTypeCode),
              visitStatus = lookupVisitStatus(visitBuilder.visitStatusCode),
              agencyLocation = lookupAgency(visitBuilder.agyLocId)
            )
            visit.visitors.addAll(
              visitBuilder.visitors.map {
                it.build(it.person, leadVisitor = it.leadVisitor, visit)
              }
            )
            visit
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

  fun lookupVisitType(code: String): VisitType =
    visitTypeRepository.findByIdOrNull(Pk(VisitType.VISIT_TYPE, code))!!

  fun lookupVisitStatus(code: String): VisitStatus =
    visitStatusRepository.findByIdOrNull(Pk(VisitStatus.VISIT_STATUS, code))!!

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

  fun updateCreatedToMatchVisitStart() {
    val sql =
      "UPDATE offender_visits SET CREATE_DATETIME = START_TIME"
    jdbcTemplate!!.execute(sql)
  }
}
