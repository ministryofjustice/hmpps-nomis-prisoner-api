package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.AuditorAwareImpl
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBookingBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.ProgramServiceBuilderFactory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivityPayRate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivityPayRateId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PayPerSession
import java.math.BigDecimal
import java.time.LocalDate

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AuthenticationFacade::class, AuditorAwareImpl::class, Repository::class, ProgramServiceBuilderFactory::class)
@WithMockUser
class ActivityRepositoryTest {
  @Autowired
  lateinit var builderRepository: Repository

  @Autowired
  lateinit var activityRepository: ActivityRepository

  @Autowired
  lateinit var offenderProgramProfileRepository: OffenderProgramProfileRepository

  @Autowired
  lateinit var offenderProgramStatusRepository: ReferenceCodeRepository<OffenderProgramStatus>

  @Autowired
  lateinit var programServiceBuilderFactory: ProgramServiceBuilderFactory

  @Autowired
  lateinit var programServiceRepository: ProgramServiceRepository

  @Test
  fun saveActivity() {
    val seedProgramService = programServiceRepository.save(programServiceBuilderFactory.builder(programId = 10, programCode = "TESTPS", active = true, description = "Test").build())

    val seedPrison = builderRepository.lookupAgency("LEI")

    val seedRoom = builderRepository.lookupAgencyInternalLocationByDescription("LEI-A-1-7")

    val seedIep = builderRepository.lookupIepLevel("STD")

    val seedPayBand = builderRepository.lookupPayBandCode("4")

    activityRepository.saveAndFlush(
      CourseActivity(
        code = "CA",
        program = seedProgramService,
        caseloadId = "LEI",
        prison = seedPrison,
        description = "test description",
        capacity = 23,
        active = true,
        scheduleStartDate = LocalDate.parse("2022-10-31"),
        scheduleEndDate = LocalDate.parse("2022-11-30"),
        iepLevel = seedIep,
        internalLocation = seedRoom,
        payPerSession = PayPerSession.F,
      ),
    ).apply {
      payRates.add(
        CourseActivityPayRate(
          id = CourseActivityPayRateId(
            courseActivity = this,
            iepLevelCode = seedIep.code,
            payBandCode = seedPayBand.code,
            startDate = LocalDate.parse("2022-12-01"),
          ),
          payBand = seedPayBand,
          iepLevel = seedIep,
          endDate = LocalDate.parse("2022-12-02"),
          halfDayRate = BigDecimal(0.6),
        ),
      )
    }

    val persistedRecord = activityRepository.findAll().first()
    assertThat(persistedRecord).isNotNull

    assertThat(persistedRecord?.courseActivityId).isGreaterThan(0)
    assertThat(persistedRecord?.code).isEqualTo("CA")
    assertThat(persistedRecord?.program?.programCode).isEqualTo("TESTPS")
    assertThat(persistedRecord?.description).isEqualTo("test description")
    assertThat(persistedRecord?.capacity).isEqualTo(23)
    assertThat(persistedRecord?.active).isTrue()
    assertThat(persistedRecord?.scheduleStartDate).isEqualTo(LocalDate.parse("2022-10-31"))
    assertThat(persistedRecord?.scheduleEndDate).isEqualTo(LocalDate.parse("2022-11-30"))
    assertThat(persistedRecord?.caseloadId).isEqualTo("LEI")
    assertThat(persistedRecord?.prison?.id).isEqualTo("LEI")
    assertThat(persistedRecord?.caseloadType).isEqualTo("INST")
    assertThat(persistedRecord?.providerPartyCode).isEqualTo("LEI")
    assertThat(persistedRecord?.iepLevel?.description).isEqualTo("Standard")
    assertThat(persistedRecord?.internalLocation?.locationId).isEqualTo(-9)
    assertThat(persistedRecord?.excludeBankHolidays).isFalse()
    assertThat(persistedRecord?.payPerSession).isEqualTo(PayPerSession.F)
    val rate = persistedRecord?.payRates?.first()
    assertThat(rate?.id?.courseActivity?.courseActivityId).isEqualTo(persistedRecord?.courseActivityId)
    assertThat(rate?.id?.iepLevelCode).isEqualTo("STD")
    assertThat(rate?.id?.payBandCode).isEqualTo("4")
    assertThat(rate?.id?.startDate).isEqualTo(LocalDate.parse("2022-12-01"))
    assertThat(rate?.endDate).isEqualTo(LocalDate.parse("2022-12-02"))
    assertThat(rate?.halfDayRate).isEqualTo(BigDecimal(0.6))
  }

  @Test
  fun saveOffenderProgramProfile() {
    val seedProgramService = programServiceRepository.save(programServiceBuilderFactory.builder(programId = 10, programCode = "TESTPS", active = true, description = "Test").build())
    val seedPrison = builderRepository.lookupAgency("LEI")
    val seedRoom = builderRepository.lookupAgencyInternalLocationByDescription("LEI-A-1-7")
    val seedIep = builderRepository.lookupIepLevel("STD")

    val seedCourseActivity = activityRepository.save(
      CourseActivity(
        program = seedProgramService,
        scheduleStartDate = LocalDate.parse("2022-10-31"),
        prison = seedPrison,
        active = true,
        iepLevel = seedIep,
        internalLocation = seedRoom,
      ),
    )

    val seedOffenderBooking = builderRepository.save(
      OffenderBuilder()
        .withBooking(OffenderBookingBuilder()),
    ).bookings.first()

    offenderProgramProfileRepository.saveAndFlush(
      OffenderProgramProfile(
        offenderBooking = seedOffenderBooking,
        program = seedProgramService,
        startDate = LocalDate.parse("2023-01-10"),
        programStatus = offenderProgramStatusRepository.findByIdOrNull(OffenderProgramStatus.pk("ALLOC"))!!,
        courseActivity = seedCourseActivity,
        prison = seedPrison,
        endDate = LocalDate.parse("2023-01-11"),
      ),
    )

    val persistedRecord = offenderProgramProfileRepository.findAll().first()
    assertThat(persistedRecord).isNotNull
    with(persistedRecord) {
      assertThat(offenderProgramReferenceId).isGreaterThan(0)
      assertThat(offenderBooking.bookingId).isEqualTo(seedOffenderBooking.bookingId)
      assertThat(program.programCode).isEqualTo("TESTPS")
      assertThat(startDate).isEqualTo(LocalDate.parse("2023-01-10"))
      assertThat(programStatus.code).isEqualTo("ALLOC")
      assertThat(courseActivity?.courseActivityId).isEqualTo(seedCourseActivity.courseActivityId)
      assertThat(prison?.id).isEqualTo(seedPrison.id)
      assertThat(endDate).isEqualTo(LocalDate.parse("2023-01-11"))
    }
  }
}
