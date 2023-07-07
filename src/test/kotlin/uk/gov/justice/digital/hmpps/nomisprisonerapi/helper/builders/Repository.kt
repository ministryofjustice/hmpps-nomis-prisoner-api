package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationEvidenceType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationFindingType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentOffence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationPleaFindingType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationRepairType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationSanctionStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationSanctionType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitDay
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitSlot
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitTime
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ContactType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseSchedule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventSubType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Gender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IEPLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentDecisionAction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCourseAttendance
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderIndividualSchedule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PayBand
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ReferenceCode.Pk
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.RelationshipType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceCalculationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceCalculationTypeId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Visit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AdjudicationHearingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AdjudicationIncidentOffenceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AdjudicationIncidentRepository
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
  val staffRepository: StaffRepository,
  val adjudicationIncidentTypeRepository: ReferenceCodeRepository<AdjudicationIncidentType>,
  val incidentDecisionActionRepository: ReferenceCodeRepository<IncidentDecisionAction>,
  val repairTypeRepository: ReferenceCodeRepository<AdjudicationRepairType>,
  val evidenceTypeRepository: ReferenceCodeRepository<AdjudicationEvidenceType>,
  val adjudicationIncidentOffenceRepository: AdjudicationIncidentOffenceRepository,
  val adjudicationHearingRepository: AdjudicationHearingRepository,
  val hearingTypeRepository: ReferenceCodeRepository<AdjudicationHearingType>,
  val pleaFindingTypeRepository: ReferenceCodeRepository<AdjudicationPleaFindingType>,
  val findingTypeRepository: ReferenceCodeRepository<AdjudicationFindingType>,
  val sanctionStatusRepository: ReferenceCodeRepository<AdjudicationSanctionStatus>,
  val sanctionTypeRepository: ReferenceCodeRepository<AdjudicationSanctionType>,
) {
  @Autowired
  lateinit var jdbcTemplate: JdbcTemplate

  // Entity builders
  fun save(offenderBuilder: OffenderBuilder): Offender {
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
      var chargeSequence = 0
      booking.adjudicationParties.addAll(
        offenderBuilder.bookingBuilders[bookingIndex].adjudications.mapIndexed { adjudicationIndex, adjudicationPartyPair ->
          val party = adjudicationPartyPair.second.build(
            repository = this,
            incident = adjudicationPartyPair.first,
            offenderBooking = booking,
            index = adjudicationPartyPair.first.parties.size + 1,
          )
          party.charges.addAll(
            offenderBuilder.bookingBuilders[bookingIndex].adjudications[adjudicationIndex].second.charges.map {
              chargeSequence += 1
              it.build(
                repository = this,
                incidentParty = party,
                chargeSequence = chargeSequence,
              )
            },
          )
          party.investigations.addAll(
            offenderBuilder.bookingBuilders[bookingIndex].adjudications[adjudicationIndex].second.investigations.map { builder ->
              builder.build(
                incidentParty = party,
              ).also { investigation ->
                investigation.evidence.addAll(
                  builder.evidence.map {
                    it.build(this, investigation)
                  },
                )
              }
            },
          )
          party
        },
      )
    }

    offenderRepository.flush()

    // children that require a flushed party
    offender.bookings.forEachIndexed { bookingIndex, booking ->
      offenderBuilder.bookingBuilders[bookingIndex].adjudications.mapIndexed { adjudicationIndex, adjudicationPartyPair ->
        offenderBuilder.bookingBuilders[bookingIndex].adjudications[adjudicationIndex].second.hearings.mapIndexed { hearingIndex, builder ->
          val party = booking.adjudicationParties[adjudicationIndex]

          adjudicationHearingRepository.save(
            builder.build(
              repository = this,
              incidentParty = party,
            ),
          ).also { hearing ->
            hearing.hearingResults.addAll(
              offenderBuilder.bookingBuilders[bookingIndex].adjudications[adjudicationIndex].second.hearings[hearingIndex].results.mapIndexed { resultIndex, resultBuilder ->
                resultBuilder.build(
                  repository = this,
                  hearing = hearing,
                  index = resultIndex,
                ).also { result ->
                  result.resultAwards.addAll(
                    offenderBuilder.bookingBuilders[bookingIndex].adjudications[adjudicationIndex].second.hearings[hearingIndex].results[resultIndex].awards.mapIndexed { awardIndex, awardBuilder ->
                      awardBuilder.build(
                        repository = this,
                        sanctionIndex = awardIndex,
                        result = result,
                        party = party,
                      )
                    },

                  ).also {
                    offenderBuilder.bookingBuilders[bookingIndex].adjudications[adjudicationIndex].second.hearings[hearingIndex].results[resultIndex].awards.mapIndexed { awardIndex, awardBuilder ->
                      awardBuilder.consecutiveSanctionIndex?.let {
                        result.resultAwards[awardIndex].consecutiveHearingResultAward =
                          result.resultAwards[awardBuilder.consecutiveSanctionIndex!!]
                      }
                    }
                  }
                }
              },
            )
          }
        }
      }
    }

    return offender
  }

  fun delete(offender: Offender) = offenderRepository.deleteById(offender.id)
  fun deleteOffenders() = offenderRepository.deleteAll()

  fun save(personBuilder: PersonBuilder): Person = personRepository.save(personBuilder.build())
  fun delete(people: Collection<Person>) = personRepository.deleteAllById(people.map { it.id })

  fun deleteProgramServices() = programServiceRepository.deleteAll()

  fun deleteActivities() = activityRepository.deleteAll()

  fun deleteAttendances() = offenderCourseAttendanceRepository.deleteAll()

  fun save(staffBuilder: StaffBuilder): Staff = staffRepository.save(staffBuilder.build())

  fun delete(staffMember: Staff) = staffRepository.deleteById(staffMember.id)

  fun save(offenderIndividualSchedule: OffenderIndividualSchedule): OffenderIndividualSchedule =
    offenderIndividualScheduleRepository.save(offenderIndividualSchedule)

  fun delete(offenderIndividualSchedule: OffenderIndividualSchedule) =
    offenderRepository.deleteById(offenderIndividualSchedule.eventId)

  fun save(adjudicationIncidentBuilder: AdjudicationIncidentBuilder): AdjudicationIncident =
    adjudicationIncidentRepository.save(
      adjudicationIncidentBuilder.build(
        repository = this,
      ),
    ).also { incident ->
      incident.repairs.addAll(
        adjudicationIncidentBuilder.repairs.mapIndexed { index, repair ->
          repair.build(repository = this, incident, repairSequence = index + 1)
        },
      )
      incident.parties.addAll(
        adjudicationIncidentBuilder.parties.mapIndexed { index, party ->
          party.build(repository = this, incident, index + 1, adjudicationIncidentBuilder.whenCreated)
        },
      )
    }

  fun delete(incident: AdjudicationIncident) = adjudicationIncidentRepository.deleteById(incident.id)
  fun deleteHearingByAdjudicationNumber(adjudicationNumber: Long) =
    adjudicationHearingRepository.deleteByAdjudicationNumber(adjudicationNumber)

  // Builder lookups
  fun lookupGender(code: String): Gender = genderRepository.findByIdOrNull(Pk(Gender.SEX, code))!!
  fun lookupContactType(code: String): ContactType =
    contactTypeRepository.findByIdOrNull(Pk(ContactType.CONTACTS, code))!!

  fun lookupVisitType(code: String): VisitType = visitTypeRepository.findByIdOrNull(Pk(VisitType.VISIT_TYPE, code))!!

  fun lookupIepLevel(code: String): IEPLevel = iepLevelRepository.findByIdOrNull(Pk(IEPLevel.IEP_LEVEL, code))!!

  fun lookupSentenceCalculationType(calculationType: String, category: String): SentenceCalculationType =
    sentenceCalculationTypeRepository.findByIdOrNull(SentenceCalculationTypeId(calculationType, category))!!

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

  fun lookupAdjudicationEvidenceType(code: String): AdjudicationEvidenceType =
    evidenceTypeRepository.findByIdOrNull(Pk(AdjudicationEvidenceType.OIC_STMT_TYP, code))!!

  fun lookupPayBandCode(code: String): PayBand = payBandRepository.findByIdOrNull(PayBand.pk(code))!!

  fun lookupEventStatusCode(code: String): EventStatus = eventStatusRepository.findByIdOrNull(EventStatus.pk(code))!!
  fun lookupEventSubtype(code: String): EventSubType = eventSubTypeRepository.findByIdOrNull(EventSubType.pk(code))!!

  fun lookupHearingType(code: String): AdjudicationHearingType =
    hearingTypeRepository.findByIdOrNull(AdjudicationHearingType.pk(code))!!

  fun lookupHearingResultPleaType(code: String): AdjudicationPleaFindingType =
    pleaFindingTypeRepository.findByIdOrNull(AdjudicationPleaFindingType.pk(code))!!

  fun lookupSanctionStatus(code: String): AdjudicationSanctionStatus =
    sanctionStatusRepository.findByIdOrNull(AdjudicationSanctionStatus.pk(code))!!

  fun lookupSanctionType(code: String): AdjudicationSanctionType =
    sanctionTypeRepository.findByIdOrNull(AdjudicationSanctionType.pk(code))!!

  fun lookupHearingResultFindingType(code: String): AdjudicationFindingType =
    findingTypeRepository.findByIdOrNull(AdjudicationFindingType.pk(code))!!

  fun lookupIncidentType(): AdjudicationIncidentType =
    adjudicationIncidentTypeRepository.findByIdOrNull(AdjudicationIncidentType.pk(AdjudicationIncidentType.GOVERNORS_REPORT))!!

  fun lookupActionDecision(code: String = IncidentDecisionAction.PLACED_ON_REPORT_ACTION_CODE): IncidentDecisionAction =
    incidentDecisionActionRepository.findByIdOrNull(IncidentDecisionAction.pk(code))!!

  fun lookupAdjudicationOffence(code: String): AdjudicationIncidentOffence =
    adjudicationIncidentOffenceRepository.findByCode(code)!!

  fun lookupRepairType(code: String): AdjudicationRepairType =
    repairTypeRepository.findByIdOrNull(AdjudicationRepairType.pk(code))!!

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
    agencyVisitSlotRepository.findByLocation_Id(prisonId)

  fun getAllAgencyVisitTimes(prisonId: String): List<AgencyVisitTime> =
    agencyVisitTimeRepository.findByAgencyVisitTimesId_Location_Id(prisonId)

  fun getAgencyVisitDays(weekDay: String, prisonId: String): AgencyVisitDay? =
    agencyVisitDayRepository.findByAgencyVisitDayId_WeekDayAndAgencyVisitDayId_Location_Id(weekDay, prisonId)

  fun deleteAllVisitSlots() = agencyVisitSlotRepository.deleteAll()
  fun deleteAllVisitDays() = agencyVisitDayRepository.deleteAll()
  fun deleteAllVisitTimes() = agencyVisitTimeRepository.deleteAll()
  fun <T> runInTransaction(block: () -> T) = block()
}
