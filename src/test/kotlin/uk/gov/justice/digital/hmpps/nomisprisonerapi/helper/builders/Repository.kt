package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitDay
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitSlot
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitTime
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ContactType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseSchedule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtCase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventSubType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Gender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IEPLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCourseAttendance
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderIndividualSchedule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderNonAssociation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderNonAssociationId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentenceAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PayBand
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Questionnaire
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ReferenceCode.Pk
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.RelationshipType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceCalculationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceCalculationTypeId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceCategoryType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.StaffUserAccount
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Visit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AdjudicationHearingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AdjudicationIncidentPartyRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AdjudicationIncidentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyVisitDayRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyVisitSlotRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyVisitTimeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourseScheduleRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderChargeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderCourseAttendanceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderIndividualScheduleRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderNonAssociationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderProgramProfileRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderSentenceAdjustmentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderSentenceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ProgramServiceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.QuestionnaireRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.SentenceAdjustmentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.SentenceCalculationTypeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.StaffRepository
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
  val offenderIndividualScheduleRepository: OffenderIndividualScheduleRepository,
  val eventStatusRepository: ReferenceCodeRepository<EventStatus>,
  val eventSubTypeRepository: ReferenceCodeRepository<EventSubType>,
  val offenderCourseAttendanceRepository: OffenderCourseAttendanceRepository,
  val courseScheduleRepository: CourseScheduleRepository,
  val adjudicationIncidentRepository: AdjudicationIncidentRepository,
  val adjudicationIncidentPartyRepository: AdjudicationIncidentPartyRepository,
  val staffRepository: StaffRepository,
  val adjudicationHearingRepository: AdjudicationHearingRepository,
  val offenderNonAssociationRepository: OffenderNonAssociationRepository,
  val courtCaseRepository: CourtCaseRepository,
  val offenderSentenceRepository: OffenderSentenceRepository,
  val offenderSentenceAdjustmentRepository: OffenderSentenceAdjustmentRepository,
  val sentenceCategoryTypeRepository: ReferenceCodeRepository<SentenceCategoryType>,
  val offenderChargeRepository: OffenderChargeRepository,
  val questionnaireRepository: QuestionnaireRepository,
) {
  @Autowired
  lateinit var jdbcTemplate: JdbcTemplate

  // Entity builders
  fun save(offenderBuilder: LegacyOffenderBuilder): Offender {
    val gender = lookupGender(offenderBuilder.genderCode)

    val offender = offenderRepository.saveAndFlush(offenderBuilder.build(gender)).apply {
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
            offenderBooking = booking,
            sequence = sentenceIndex.toLong() + 1,
            calculationType = lookupSentenceCalculationType(sentenceBuilder.calculationType, sentenceBuilder.category),
            category = lookupSentenceCategory(sentenceBuilder.category),
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

    offenderRepository.flush()
    return offender
  }

  fun delete(offender: Offender) = offenderRepository.deleteById(offender.id)
  fun delete(vararg offender: Offender) = offender.forEach { offenderRepository.deleteById(it.id) }
  fun deleteOffenders() = offenderRepository.deleteAll()

  fun save(personBuilder: PersonBuilder): Person = personRepository.save(personBuilder.build())
  fun delete(people: Collection<Person>) = personRepository.deleteAllById(people.map { it.id })

  fun deleteProgramServices() = programServiceRepository.deleteAll()

  fun deleteActivities() = activityRepository.deleteAll()

  fun deleteAttendances() = offenderCourseAttendanceRepository.deleteAll()

  fun delete(staffMember: Staff) = staffRepository.deleteById(staffMember.id)
  fun deleteStaffByAccount(vararg staffUserAccount: StaffUserAccount) =
    staffUserAccount.map { it.staff }.forEach { staffRepository.delete(it) }

  fun save(offenderIndividualSchedule: OffenderIndividualSchedule): OffenderIndividualSchedule =
    offenderIndividualScheduleRepository.save(offenderIndividualSchedule)

  fun delete(offenderIndividualSchedule: OffenderIndividualSchedule) =
    offenderRepository.deleteById(offenderIndividualSchedule.eventId)

  fun delete(incident: AdjudicationIncident) = adjudicationIncidentRepository.deleteById(incident.id)
  fun deleteHearingByAdjudicationNumber(adjudicationNumber: Long) =
    adjudicationHearingRepository.deleteByAdjudicationNumber(adjudicationNumber)

  fun delete(courtCase: CourtCase) = courtCaseRepository.deleteById(courtCase.id)
  fun deleteOffenderChargeByBooking(bookingId: Long) = offenderChargeRepository.deleteByOffenderBookingBookingId(bookingId = bookingId)
  fun delete(sentence: OffenderSentence) = offenderSentenceRepository.deleteById(sentence.id)

  fun delete(questionnaire: Questionnaire) = questionnaireRepository.deleteById(questionnaire.id)

  // Builder lookups
  fun lookupGender(code: String): Gender = genderRepository.findByIdOrNull(Pk(Gender.SEX, code))!!
  fun lookupContactType(code: String): ContactType =
    contactTypeRepository.findByIdOrNull(Pk(ContactType.CONTACTS, code))!!

  fun lookupVisitType(code: String): VisitType = visitTypeRepository.findByIdOrNull(Pk(VisitType.VISIT_TYPE, code))!!

  fun lookupIepLevel(code: String): IEPLevel = iepLevelRepository.findByIdOrNull(Pk(IEPLevel.IEP_LEVEL, code))!!

  fun lookupSentenceCalculationType(calculationType: String, category: String): SentenceCalculationType =
    sentenceCalculationTypeRepository.findByIdOrNull(SentenceCalculationTypeId(calculationType, category))!!

  fun lookupSentenceCategory(code: String): SentenceCategoryType = sentenceCategoryTypeRepository.findByIdOrNull(Pk(SentenceCategoryType.CATEGORY, code))!!

  fun lookupSentenceAdjustment(code: String): SentenceAdjustment = sentenceAdjustmentRepository.findByIdOrNull(code)!!

  fun lookupVisitStatus(code: String): VisitStatus =
    visitStatusRepository.findByIdOrNull(Pk(VisitStatus.VISIT_STATUS, code))!!

  fun lookupRelationshipType(code: String): RelationshipType =
    relationshipTypeRepository.findByIdOrNull(Pk(RelationshipType.RELATIONSHIP, code))!!

  fun lookupAgency(id: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(id)!!
  fun lookupAgencyInternalLocationByDescription(description: String): AgencyInternalLocation =
    agencyInternalLocationRepository.findOneByDescription(description).map { it }.orElse(null)

  fun lookupAgencyInternalLocation(locationId: Long): AgencyInternalLocation? =
    agencyInternalLocationRepository.findByIdOrNull(locationId)

  fun lookupPayBandCode(code: String): PayBand = payBandRepository.findByIdOrNull(PayBand.pk(code))!!

  fun lookupEventStatusCode(code: String): EventStatus = eventStatusRepository.findByIdOrNull(EventStatus.pk(code))!!
  fun lookupEventSubtype(code: String): EventSubType = eventSubTypeRepository.findByIdOrNull(EventSubType.pk(code))!!

  // Test Helpers

  fun getOffender(nomsId: String): Offender? {
    val offender = offenderRepository.findByNomsId(nomsId).firstOrNull()
    offender?.bookings?.firstOrNull()?.incentives?.size // hydrate
    return offender
  }

  fun getVisit(visitId: Long?): Visit {
    val visit = visitRepository.findById(visitId!!).orElseThrow()
    visit.visitors.size // hydrate
    visit.visitOrder?.visitors?.size
    return visit
  }

  fun getAttendance(eventId: Long): OffenderCourseAttendance =
    offenderCourseAttendanceRepository.findByIdOrNull(eventId)!!

  fun getSchedule(id: Long): CourseSchedule = courseScheduleRepository.findByIdOrNull(id)!!

  fun getActivity(id: Long): CourseActivity = activityRepository.findByIdOrNull(id)!!.also {
    it.payRates.size
    it.courseSchedules.size
    it.courseScheduleRules.size
  }

  fun getOffenderProgramProfiles(
    courseActivity: CourseActivity,
    booking: OffenderBooking,
  ): List<OffenderProgramProfile> =
    offenderProgramProfileRepository.findByCourseActivityAndOffenderBooking(courseActivity, booking)
      .onEach { it.payBands.size }

  fun getOffenderProgramProfile(id: Long): OffenderProgramProfile =
    offenderProgramProfileRepository.findByIdOrNull(id)!!.also {
      it.payBands.size
      it.offenderExclusions.size
    }

  fun getAppointment(id: Long): OffenderIndividualSchedule? = offenderIndividualScheduleRepository.findByIdOrNull(id)

  fun updateVisitStatus(visitId: Long?) {
    val visit = visitRepository.findById(visitId!!).orElseThrow()
    visit.visitStatus = visitStatusRepository.findById(VisitStatus.NORM).orElseThrow()
  }

  @Suppress("SqlWithoutWhere")
  fun updateCreatedToMatchVisitStart() {
    val sql = "UPDATE offender_visits SET CREATE_DATETIME = START_TIME"
    jdbcTemplate.execute(sql)
  }

  fun getAllAgencyVisitSlots(prisonId: String): List<AgencyVisitSlot> =
    agencyVisitSlotRepository.findByLocationId(prisonId)

  fun getAllAgencyVisitTimes(prisonId: String): List<AgencyVisitTime> =
    agencyVisitTimeRepository.findByAgencyVisitTimesIdLocationId(prisonId)

  fun getAgencyVisitDays(weekDay: String, prisonId: String): AgencyVisitDay? =
    agencyVisitDayRepository.findByAgencyVisitDayIdWeekDayAndAgencyVisitDayIdLocationId(weekDay, prisonId)

  fun getAdjudicationIncidentByAdjudicationNumber(adjudicationNumber: Long): AdjudicationIncident? =
    adjudicationIncidentPartyRepository.findByAdjudicationNumber(adjudicationNumber)?.incident

  fun getInternalLocationByDescription(locationDescription: String, prisonId: String): AgencyInternalLocation =
    agencyInternalLocationRepository.findByDescriptionAndAgencyId(locationDescription, prisonId)!!

  fun deleteAllVisitSlots() = agencyVisitSlotRepository.deleteAll()
  fun deleteAllVisitDays() = agencyVisitDayRepository.deleteAll()
  fun deleteAllVisitTimes() = agencyVisitTimeRepository.deleteAll()
  fun <T> runInTransaction(block: () -> T) = block()

  fun getNonAssociation(first: Long, second: Long): OffenderNonAssociation =
    offenderNonAssociationRepository.findById(OffenderNonAssociationId(first, second)).orElseThrow()
      .also {
        it.toString() // hydrate
      }

  fun getNonAssociationOrNull(first: Long, second: Long): OffenderNonAssociation? =
    offenderNonAssociationRepository.findByIdOrNull(OffenderNonAssociationId(first, second))
      ?.also {
        it.toString() // hydrate
      }

  fun deleteAllNonAssociations() = offenderNonAssociationRepository.deleteAll()
  fun delete(adjustment: OffenderSentenceAdjustment) = offenderSentenceAdjustmentRepository.delete(adjustment)
}
