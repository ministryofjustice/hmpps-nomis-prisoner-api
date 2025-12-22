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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPReport
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseSchedule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtCase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Gender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IEPLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Incident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.InternalScheduleReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAppointment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCaseNote
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCourseAttendance
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderNonAssociation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderNonAssociationId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentenceAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PayBand
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Questionnaire
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ReferenceCode.Pk
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.StaffUserAccount
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Visit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.WeekDay
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AdjudicationHearingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AdjudicationIncidentPartyRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AdjudicationIncidentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyVisitDayRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyVisitSlotRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyVisitTimeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CSIPReportRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CaseloadCurrentAccountsBaseRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CaseloadCurrentAccountsTxnRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourseScheduleRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourtEventRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.GeneralLedgerTransactionRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.IWPTemplateRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.IncidentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.LinkCaseTxnRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.MergeTransactionRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderAppointmentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBeliefRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderCaseNoteRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderChargeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderCourseAttendanceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderNonAssociationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderProgramProfileRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderSentenceAdjustmentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderSentenceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTransactionRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTrustAccountRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ProgramServiceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.QuestionnaireRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.SplashScreenRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.StaffRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitRepository
import kotlin.jvm.optionals.getOrNull

@Repository
@Transactional
class Repository(
  val genderRepository: ReferenceCodeRepository<Gender>,
  val offenderRepository: OffenderRepository,
  val agencyLocationRepository: AgencyLocationRepository,
  val agencyInternalLocationRepository: AgencyInternalLocationRepository,
  val personRepository: PersonRepository,
  val visitRepository: VisitRepository,
  val visitStatusRepository: ReferenceCodeRepository<VisitStatus>,
  val iepLevelRepository: ReferenceCodeRepository<IEPLevel>,
  val agencyVisitSlotRepository: AgencyVisitSlotRepository,
  val agencyVisitDayRepository: AgencyVisitDayRepository,
  val agencyVisitTimeRepository: AgencyVisitTimeRepository,
  val activityRepository: ActivityRepository,
  val programServiceRepository: ProgramServiceRepository,
  val offenderProgramProfileRepository: OffenderProgramProfileRepository,
  val payBandRepository: ReferenceCodeRepository<PayBand>,
  val offenderAppointmentRepository: OffenderAppointmentRepository,
  val eventStatusRepository: ReferenceCodeRepository<EventStatus>,
  val internalScheduleReasonRepository: ReferenceCodeRepository<InternalScheduleReason>,
  val offenderCourseAttendanceRepository: OffenderCourseAttendanceRepository,
  val courseScheduleRepository: CourseScheduleRepository,
  val adjudicationIncidentRepository: AdjudicationIncidentRepository,
  val adjudicationIncidentPartyRepository: AdjudicationIncidentPartyRepository,
  val staffRepository: StaffRepository,
  val adjudicationHearingRepository: AdjudicationHearingRepository,
  val offenderNonAssociationRepository: OffenderNonAssociationRepository,
  val courtCaseRepository: CourtCaseRepository,
  val courtEventRepository: CourtEventRepository,
  val offenderSentenceRepository: OffenderSentenceRepository,
  val offenderSentenceAdjustmentRepository: OffenderSentenceAdjustmentRepository,
  val offenderChargeRepository: OffenderChargeRepository,
  val questionnaireRepository: QuestionnaireRepository,
  val splashScreenRepository: SplashScreenRepository,
  val incidentRepository: IncidentRepository,
  val mergeTransactionRepository: MergeTransactionRepository,
  val csipReportRepository: CSIPReportRepository,
  val iwpTemplateRepository: IWPTemplateRepository,
  val offenderCaseNoteRepository: OffenderCaseNoteRepository,
  val offenderTransactionRepository: OffenderTransactionRepository,
  val generalLedgerTransactionRepository: GeneralLedgerTransactionRepository,
  val offenderTrustAccountRepository: OffenderTrustAccountRepository,
  val linkCaseTxnRepository: LinkCaseTxnRepository,
  val caseloadCurrentAccountsBaseRepository: CaseloadCurrentAccountsBaseRepository,
  val caseloadCurrentAccountsTxnRepository: CaseloadCurrentAccountsTxnRepository,
  val offenderBeliefRepository: OffenderBeliefRepository,
) {
  @Autowired
  lateinit var jdbcTemplate: JdbcTemplate

  // Entity builders
  fun save(offenderBuilder: OffenderDataBuilder): Offender {
    val gender = lookupGender(offenderBuilder.genderCode)

    val offender = offenderRepository.saveAndFlush(offenderBuilder.build(gender)).apply {
      rootOffenderId = id
      rootOffender = this
    }

    offenderBuilder.bookingBuilders.forEachIndexed { index, bookingBuilder ->
      bookingBuilder.build(offender, index + 1, lookupAgency(bookingBuilder.agencyLocationId))
    }

    offenderRepository.saveAndFlush(offender)
    return offender
  }

  fun delete(offender: Offender) = offenderRepository.deleteById(offender.id)
  fun delete(vararg offender: Offender) = offender.forEach { offenderRepository.deleteById(it.id) }
  fun deleteOffenders() = offenderRepository.deleteAll()
  fun deleteAllCSIPReports() = csipReportRepository.deleteAll()
  fun deleteMergeTransactions() = mergeTransactionRepository.deleteAll()
  fun deleteAllIncidents() = incidentRepository.deleteAll()
  fun deleteAllQuestionnaires() = questionnaireRepository.deleteAll()
  fun deleteAllSplashScreens() = splashScreenRepository.deleteAll()
  fun delete(people: Collection<Person>) = personRepository.deleteAllById(people.map { it.id })

  fun deleteProgramServices() = programServiceRepository.deleteAll()

  fun deleteActivities() = activityRepository.deleteAll()

  fun deleteAttendances() = offenderCourseAttendanceRepository.deleteAll()

  fun delete(staffMember: Staff) = staffRepository.deleteById(staffMember.id)
  fun deleteStaffByAccount(vararg staffUserAccount: StaffUserAccount) = staffUserAccount.map { it.staff }.forEach { staffRepository.delete(it) }
  fun deleteStaff() = staffRepository.deleteAll()

  fun save(staff: Staff): Staff = staffRepository.save(staff)

  fun save(offenderAppointment: OffenderAppointment): OffenderAppointment = offenderAppointmentRepository.save(offenderAppointment)

  fun delete(offenderAppointment: OffenderAppointment) = offenderRepository.deleteById(offenderAppointment.eventId)

  fun delete(incident: AdjudicationIncident) = adjudicationIncidentRepository.deleteById(incident.id)
  fun deleteHearingByAdjudicationNumber(adjudicationNumber: Long) = adjudicationHearingRepository.deleteByAdjudicationNumber(adjudicationNumber)

  fun delete(courtCase: CourtCase) = courtCaseRepository.deleteById(courtCase.id)
  fun delete(courtEvent: CourtEvent) = courtEventRepository.deleteById(courtEvent.id)

  fun deleteOffenderChargeByBooking(bookingId: Long) = offenderChargeRepository.deleteByOffenderBookingBookingId(bookingId = bookingId)
  fun deleteAllOffenderLinkedTransactions() = linkCaseTxnRepository.deleteAll()

  fun delete(sentence: OffenderSentence) = offenderSentenceRepository.deleteById(sentence.id)

  fun delete(questionnaire: Questionnaire) = questionnaireRepository.deleteById(questionnaire.id)
  fun delete(incident: Incident) = incidentRepository.deleteById(incident.id)
  fun delete(csipReport: CSIPReport) = csipReportRepository.deleteById(csipReport.id)
  fun deleteTemplates() = iwpTemplateRepository.deleteAll()

  // Builder lookups
  fun lookupGender(code: String): Gender = genderRepository.findByIdOrNull(Pk(Gender.SEX, code))!!

  fun lookupIepLevel(code: String): IEPLevel = iepLevelRepository.findByIdOrNull(Pk(IEPLevel.IEP_LEVEL, code))!!

  fun lookupAgency(id: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(id)!!
  fun lookupAgencyInternalLocationByDescription(description: String): AgencyInternalLocation? = agencyInternalLocationRepository.findOneByDescription(description).getOrNull()
    ?.also { it.toString() } // hydrate

  fun lookupAgencyInternalLocation(locationId: Long): AgencyInternalLocation? = agencyInternalLocationRepository.findByIdOrNull(locationId).also {
    it?.profiles?.size // hydrate
    it?.usages?.map { m -> m.toString() }
  }

  fun lookupPayBandCode(code: String): PayBand = payBandRepository.findByIdOrNull(PayBand.pk(code))!!

  fun lookupEventStatusCode(code: String): EventStatus = eventStatusRepository.findByIdOrNull(EventStatus.pk(code))!!
  fun lookupEventSubtype(code: String): InternalScheduleReason = internalScheduleReasonRepository.findByIdOrNull(InternalScheduleReason.pk(code))!!

  // Test Helpers

  fun getOffender(nomsId: String): Offender? {
    val offender = offenderRepository.findByNomsId(nomsId).firstOrNull()
    offender?.getAllBookings()?.size // hydrate
    offender?.getAllBookings()?.firstOrNull()?.incentives?.size // hydrate
    return offender
  }

  fun getOffender(offenderId: Long): Offender? {
    val offender = offenderRepository.findByIdOrNull(offenderId)
    offender?.getAllBookings()?.size // hydrate
    return offender
  }

  fun getVisit(visitId: Long?): Visit {
    val visit = visitRepository.findById(visitId!!).orElseThrow()
    visit.visitors.size // hydrate
    visit.visitOrder?.visitors?.size
    return visit
  }
  fun getIncident(incidentId: Long): Incident = incidentRepository.findByIdOrNull(incidentId)!!

  fun getAttendance(eventId: Long): OffenderCourseAttendance = offenderCourseAttendanceRepository.findByIdOrNull(eventId)!!

  fun getSchedule(id: Long): CourseSchedule = courseScheduleRepository.findByIdOrNull(id)!!

  fun getActivity(id: Long): CourseActivity = activityRepository.findByIdOrNull(id)!!.also {
    it.payRates.size
    it.courseSchedules.size
    it.courseScheduleRules.size
  }

  fun getOffenderProgramProfiles(
    courseActivity: CourseActivity,
    booking: OffenderBooking,
  ): List<OffenderProgramProfile> = offenderProgramProfileRepository.findByCourseActivityAndOffenderBooking(courseActivity, booking)
    .onEach { it.payBands.size }

  fun getOffenderProgramProfile(id: Long): OffenderProgramProfile = offenderProgramProfileRepository.findByIdOrNull(id)!!.also {
    it.payBands.size
    it.offenderExclusions.size
  }

  fun getAppointment(id: Long): OffenderAppointment? = offenderAppointmentRepository.findByIdOrNull(id)
    ?.also {
      it.toString() // hydrate
      it.internalLocation?.toString()
    }

  fun updateVisitStatus(visitId: Long?) {
    val visit = visitRepository.findById(visitId!!).orElseThrow()
    visit.visitStatus = visitStatusRepository.findById(VisitStatus.NORM).orElseThrow()
  }

  @Suppress("SqlWithoutWhere")
  fun updateCreatedToMatchVisitStart() {
    val sql = "UPDATE offender_visits SET CREATE_DATETIME = START_TIME"
    jdbcTemplate.execute(sql)
  }

  fun getAllAgencyVisitSlots(prisonId: String): List<AgencyVisitSlot> = agencyVisitSlotRepository.findByLocationId(prisonId)

  fun getAllAgencyVisitTimes(prisonId: String): List<AgencyVisitTime> = agencyVisitTimeRepository.findByAgencyVisitTimesIdLocationId(prisonId)

  fun getAgencyVisitDays(weekDay: WeekDay, prisonId: String): AgencyVisitDay? = agencyVisitDayRepository.findByAgencyVisitDayIdWeekDayAndAgencyVisitDayIdLocationId(weekDay, prisonId)

  fun getAdjudicationIncidentByAdjudicationNumber(adjudicationNumber: Long): AdjudicationIncident? = adjudicationIncidentPartyRepository.findByAdjudicationNumber(adjudicationNumber)?.incident

  fun getInternalLocationByDescription(locationDescription: String, prisonId: String): AgencyInternalLocation = agencyInternalLocationRepository.findByDescriptionAndAgencyId(locationDescription, prisonId)!!

  fun deleteAllVisitSlots() = agencyVisitSlotRepository.deleteAll()
  fun deleteAllVisitDays() = agencyVisitDayRepository.deleteAll()
  fun deleteAllVisitTimes() = agencyVisitTimeRepository.deleteAll()
  fun <T> runInTransaction(block: () -> T) = block()

  fun getNonAssociation(first: Long, second: Long): OffenderNonAssociation = offenderNonAssociationRepository.findById(OffenderNonAssociationId(first, second)).orElseThrow()
    .also {
      it.toString() // hydrate
    }

  fun getNonAssociationOrNull(first: Long, second: Long): OffenderNonAssociation? = offenderNonAssociationRepository.findByIdOrNull(OffenderNonAssociationId(first, second))
    ?.also {
      it.toString() // hydrate
    }

  fun deleteAllNonAssociations() = offenderNonAssociationRepository.deleteAll()

  fun delete(adjustment: OffenderSentenceAdjustment) = offenderSentenceAdjustmentRepository.delete(adjustment)

  fun delete(agencyInternalLocation: AgencyInternalLocation) = agencyInternalLocationRepository.delete(agencyInternalLocation)

  fun deleteAgencyInternalLocationById(id: Long) = agencyInternalLocationRepository.deleteById(id)

  fun save(offenderCaseNote: OffenderCaseNote): OffenderCaseNote = offenderCaseNoteRepository.save(offenderCaseNote)
  fun delete(offenderCaseNote: OffenderCaseNote) = offenderCaseNoteRepository.delete(offenderCaseNote)
  fun deleteCaseNotes() = offenderCaseNoteRepository.deleteAll()
  fun addSentenceCaseNoteLink(caseNoteId: Long, sentenceSequence: Long) {
    val cn = offenderCaseNoteRepository.findById(caseNoteId).get()
    val sentence = offenderSentenceRepository.findById(SentenceId(cn.offenderBooking, sentenceSequence)).get()
    cn.sentences.add(sentence)
  }

  fun deleteAllTransactions() = generalLedgerTransactionRepository
    .deleteAll().also {
      offenderTransactionRepository.deleteAll()
      offenderTrustAccountRepository.deleteAll()
    }

  fun deleteAllPrisonBalances() = caseloadCurrentAccountsBaseRepository.deleteAll().also {
    caseloadCurrentAccountsTxnRepository.deleteAll()
  }

  fun deleteAllBeliefs() = offenderBeliefRepository.deleteAll()
}
