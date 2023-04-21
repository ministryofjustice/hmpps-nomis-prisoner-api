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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AttendanceOutcome
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ContactType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseSchedule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventSubType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Gender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IEPLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCourseAttendance
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderIndividualSchedule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PayBand
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProgramService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProgramServiceEndReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ReferenceCode.Pk
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.RelationshipType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceCalculationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceCalculationTypeId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Visit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyVisitDayRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyVisitSlotRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyVisitTimeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourseScheduleRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderCourseAttendanceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderIndividualScheduleRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderProgramProfileRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ProgramServiceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.SentenceAdjustmentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.SentenceCalculationTypeRepository
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
  val activityRepository: ActivityRepository,
  val programServiceRepository: ProgramServiceRepository,
  val sentenceCalculationTypeRepository: SentenceCalculationTypeRepository,
  val sentenceAdjustmentRepository: SentenceAdjustmentRepository,
  val offenderProgramProfileRepository: OffenderProgramProfileRepository,
  val payBandRepository: ReferenceCodeRepository<PayBand>,
  val programStatusRepository: ReferenceCodeRepository<OffenderProgramStatus>,
  val programEndReasonRepository: ReferenceCodeRepository<ProgramServiceEndReason>,
  val offenderIndividualScheduleRepository: OffenderIndividualScheduleRepository,
  val attendanceOutcomeRepository: ReferenceCodeRepository<AttendanceOutcome>,
  val eventStatusRepository: ReferenceCodeRepository<EventStatus>,
  val eventSubTypeRepository: ReferenceCodeRepository<EventSubType>,
  val offenderCourseAttendanceRepository: OffenderCourseAttendanceRepository,
  val courseScheduleRepository: CourseScheduleRepository,
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
          },
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
                  this,
                )
              },
            )
            visit.visitors.addAll(
              visitBuilder.visitors.map {
                it.build(it.person, leadVisitor = it.leadVisitor, visit)
              } + visitBuilder.visitOutcome.build(visit),
            )
            visit
          },
        )

        booking
      },
    )

    offenderRepository.saveAndFlush(offender)

    // children that require a flushed booking
    offender.bookings.forEachIndexed { bookingIndex, booking ->
      booking.incentives.addAll(
        offenderBuilder.bookingBuilders[bookingIndex].incentives.map {
          it.build(booking, lookupIepLevel(it.iepLevel))
        },
      )
      booking.sentences.addAll(
        offenderBuilder.bookingBuilders[bookingIndex].sentences.mapIndexed { sentenceIndex, sentenceBuilder ->
          val sentence = sentenceBuilder.build(
            booking,
            sentenceIndex.toLong() + 1,
            lookupSentenceCalculationType(sentenceBuilder.calculationType, sentenceBuilder.category),
          )
          sentence.adjustments.addAll(
            sentenceBuilder.adjustments.map {
              it.build(lookupSentenceAdjustment(it.adjustmentTypeCode), sentence)
            },
          )
          sentence
        },
      )
      booking.keyDateAdjustments.addAll(
        offenderBuilder.bookingBuilders[bookingIndex].keyDateAdjustments.map {
          it.build(
            booking,
            lookupSentenceAdjustment(it.adjustmentTypeCode),
          )
        },
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

  fun lookupSentenceCalculationType(calculationType: String, category: String): SentenceCalculationType =
    sentenceCalculationTypeRepository.findByIdOrNull(SentenceCalculationTypeId(calculationType, category))!!

  fun lookupSentenceAdjustment(code: String): SentenceAdjustment =
    sentenceAdjustmentRepository.findByIdOrNull(code)!!

  fun lookupVisitStatus(code: String): VisitStatus =
    visitStatusRepository.findByIdOrNull(Pk(VisitStatus.VISIT_STATUS, code))!!

  fun lookupRelationshipType(code: String): RelationshipType =
    relationshipTypeRepository.findByIdOrNull(Pk(RelationshipType.RELATIONSHIP, code))!!

  fun lookupAgency(id: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(id)!!
  fun lookupAgencyInternalLocationByDescription(description: String): AgencyInternalLocation =
    agencyInternalLocationRepository.findOneByDescription(description).map { it }.orElse(null)

  fun lookupAgencyInternalLocation(locationId: Long): AgencyInternalLocation? =
    agencyInternalLocationRepository.findByIdOrNull(locationId)

  fun lookupAppointment(id: Long): OffenderIndividualSchedule? =
    offenderIndividualScheduleRepository.findByIdOrNull(id)

  fun delete(offender: Offender) = offenderRepository.deleteById(offender.id)
  fun deleteOffenders() = offenderRepository.deleteAll()
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
    val sql = "UPDATE offender_visits SET CREATE_DATETIME = START_TIME"
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

  fun lookupProgramService(id: Long): ProgramService = programServiceRepository.findByIdOrNull(id)!!
  fun lookupOffenderProgramProfile(id: Long): OffenderProgramProfile =
    offenderProgramProfileRepository.findByIdOrNull(id)!!.also {
      it.payBands.size
    }

  fun lookupActivity(id: Long): CourseActivity = activityRepository.findByIdOrNull(id)!!.also {
    it.payRates.size
    it.courseSchedules.size
    it.courseScheduleRules.size
  }

  fun lookupSchedule(id: Long): CourseSchedule = courseScheduleRepository.findByIdOrNull(id)!!

  fun lookupAttendance(eventId: Long): OffenderCourseAttendance = offenderCourseAttendanceRepository.findByIdOrNull(eventId)!!

  fun <T> runInTransaction(block: () -> T) = block()

  fun deleteProgramServices() = programServiceRepository.deleteAll()
  fun deleteActivities() = activityRepository.deleteAll()
  fun deleteAttendances() = offenderCourseAttendanceRepository.deleteAll()

  fun save(programServiceBuilder: ProgramServiceBuilder): ProgramService =
    programServiceRepository.save(programServiceBuilder.build())

  fun save(courseActivityBuilder: CourseActivityBuilder): CourseActivity =
    courseActivityBuilder.build()
      .let { activityRepository.saveAndFlush(it) }

  fun save(
    offenderCourseAttendanceBuilder: OffenderCourseAttendanceBuilder,
    courseSchedule: CourseSchedule,
    offenderProgramProfile: OffenderProgramProfile,
  ): OffenderCourseAttendance =
    offenderCourseAttendanceBuilder.build(courseSchedule, offenderProgramProfile)
      .let { offenderCourseAttendanceRepository.saveAndFlush(it) }

  fun lookupPayBandCode(code: String): PayBand = payBandRepository.findByIdOrNull(PayBand.pk(code))!!

  fun lookupAttendanceOutcomeCode(code: String): AttendanceOutcome = attendanceOutcomeRepository.findByIdOrNull(AttendanceOutcome.pk(code))!!

  fun lookupEventStatusCode(code: String): EventStatus = eventStatusRepository.findByIdOrNull(EventStatus.pk(code))!!
  fun lookupEventSubtype(code: String): EventSubType = eventSubTypeRepository.findByIdOrNull(EventSubType.pk(code))!!

  fun lookupProgramStatus(code: String): OffenderProgramStatus =
    programStatusRepository.findByIdOrNull(OffenderProgramStatus.pk(code))!!

  fun lookupProgramEndReason(code: String): ProgramServiceEndReason =
    programEndReasonRepository.findByIdOrNull(ProgramServiceEndReason.pk(code))!!

  fun save(
    offenderProgramProfileBuilder: OffenderProgramProfileBuilder,
    offenderBooking: OffenderBooking,
    courseActivity: CourseActivity,
  ): OffenderProgramProfile =
    offenderProgramProfileBuilder.build(offenderBooking, courseActivity)
      .let { offenderProgramProfileRepository.save(it) }

  fun save(offenderIndividualSchedule: OffenderIndividualSchedule): OffenderIndividualSchedule =
    offenderIndividualScheduleRepository.save(offenderIndividualSchedule)

  fun delete(offenderIndividualSchedule: OffenderIndividualSchedule) = offenderRepository.deleteById(offenderIndividualSchedule.eventId)
}
