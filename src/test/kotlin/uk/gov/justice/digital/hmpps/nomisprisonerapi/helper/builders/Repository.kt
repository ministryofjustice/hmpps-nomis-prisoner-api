package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitDay
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitSlot
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitTime
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ContactType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Gender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IEPLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ReferenceCode.Pk
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.RelationshipType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Visit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyVisitDayRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyVisitSlotRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyVisitTimeRepository
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
  val agencyInternalLocationRepository: AgencyInternalLocationRepository,
  val personRepository: PersonRepository,
  val visitRepository: VisitRepository,
  val visitStatusRepository: ReferenceCodeRepository<VisitStatus>,
  val visitTypeRepository: ReferenceCodeRepository<VisitType>,
  val iepLevelRepository: ReferenceCodeRepository<IEPLevel>,
  val agencyVisitSlotRepository: AgencyVisitSlotRepository,
  val agencyVisitDayRepository: AgencyVisitDayRepository,
  val agencyVisitTimeRepository: AgencyVisitTimeRepository,
) {
  @Autowired
  lateinit var jdbcTemplate: JdbcTemplate
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
              agencyLocation = lookupAgency(visitBuilder.agyLocId),
              agencyInternalLocation = visitBuilder.agencyInternalLocationDescription?.run {
                lookupAgencyInternalLocationByDescription(
                  this
                )
              }
            )
            visit.visitors.addAll(
              visitBuilder.visitors.map {
                it.build(it.person, leadVisitor = it.leadVisitor, visit)
              } + visitBuilder.visitOutcome.build(visit)
            )
            visit
          }
        )

        booking
      }
    )

    offenderRepository.saveAndFlush(offender)

    // children that require a flushed booking
    offender.bookings.forEachIndexed { index, booking ->
      booking.incentives.addAll(
        offenderBuilder.bookingBuilders[index].incentives.map {
          it.build(booking, lookupIepLevel(it.iepLevel))
        }
      )
    }
    return offender
  }

  fun save(personBuilder: PersonBuilder): Person = personRepository.save(personBuilder.build())
  fun save(offender: Offender): Offender = offenderRepository.saveAndFlush(offender)

  fun lookupGender(code: String): Gender = genderRepository.findByIdOrNull(Pk(Gender.SEX, code))!!
  fun lookupContactType(code: String): ContactType =
    contactTypeRepository.findByIdOrNull(Pk(ContactType.CONTACTS, code))!!

  fun lookupVisitType(code: String): VisitType =
    visitTypeRepository.findByIdOrNull(Pk(VisitType.VISIT_TYPE, code))!!

  fun lookupIepLevel(code: String): IEPLevel =
    iepLevelRepository.findByIdOrNull(Pk(IEPLevel.IEP_LEVEL, code))!!

  fun lookupVisitStatus(code: String): VisitStatus =
    visitStatusRepository.findByIdOrNull(Pk(VisitStatus.VISIT_STATUS, code))!!

  fun lookupRelationshipType(code: String): RelationshipType =
    relationshipTypeRepository.findByIdOrNull(Pk(RelationshipType.RELATIONSHIP, code))!!

  fun lookupAgency(id: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(id)!!
  fun lookupAgencyInternalLocationByDescription(description: String): AgencyInternalLocation =
    agencyInternalLocationRepository.findOneByDescription(description).map { it }.orElse(null)

  fun delete(offender: Offender) = offenderRepository.deleteById(offender.id)
  fun delete(people: Collection<Person>) = personRepository.deleteAllById(people.map { it.id })

  fun deleteAllVisitSlots() = agencyVisitSlotRepository.deleteAll()
  fun deleteAllVisitDays() = agencyVisitDayRepository.deleteAll()
  fun deleteAllVisitTimes() = agencyVisitTimeRepository.deleteAll()

  fun lookupVisit(visitId: Long?): Visit {
    val visit = visitRepository.findById(visitId!!).orElseThrow()
    visit.visitors.size // hydrate
    visit.visitOrder?.visitors?.size
    return visit
  }

  fun changeVisitStatus(visitId: Long?) {
    val visit = visitRepository.findById(visitId!!).orElseThrow()
    visit.visitStatus = visitStatusRepository.findById(VisitStatus.NORM).orElseThrow()
  }

  fun updateCreatedToMatchVisitStart() {
    val sql =
      "UPDATE offender_visits SET CREATE_DATETIME = START_TIME"
    jdbcTemplate.execute(sql)
  }

  fun findAllAgencyVisitSlots(prisonId: String): List<AgencyVisitSlot> =
    agencyVisitSlotRepository.findByLocation_Id(prisonId)

  fun findAllAgencyVisitTimes(prisonId: String): List<AgencyVisitTime> =
    agencyVisitTimeRepository.findByAgencyVisitTimesId_Location_Id(prisonId)

  fun findAllAgencyVisitDays(weekDay: String, prisonId: String): AgencyVisitDay? =
    agencyVisitDayRepository.findByAgencyVisitDayId_WeekDayAndAgencyVisitDayId_Location_Id(weekDay, prisonId)

  fun lookupOffender(nomsId: String): Offender? {
    val offender = offenderRepository.findByNomsId(nomsId).firstOrNull()
    offender?.bookings?.firstOrNull()?.incentives?.size // hydrate
    return offender
  }
}
