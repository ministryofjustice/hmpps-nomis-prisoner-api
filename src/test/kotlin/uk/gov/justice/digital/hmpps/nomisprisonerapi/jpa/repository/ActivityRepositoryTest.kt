package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.AuditorAwareImpl
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivityPayRate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PayPerSession
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProgramService
import java.math.BigDecimal
import java.time.LocalDate

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AuthenticationFacade::class, AuditorAwareImpl::class, Repository::class)
@WithMockUser
class ActivityRepositoryTest {
  @Autowired
  lateinit var builderRepository: Repository

  @Autowired
  lateinit var repository: ActivityRepository

  @Autowired
  lateinit var entityManager: TestEntityManager

  @Test
  fun saveActivity() {
    val seedProgramService = builderRepository.save(
      ProgramService(
        programId = 10,
        programCode = "TESTPS",
        description = "test description",
        active = true,
      )
    )

    val seedPrison = builderRepository.lookupAgency("LEI")

    val seedRoom = builderRepository.lookupAgencyInternalLocationByDescription("LEI-A-1-7")

    val seedIep = builderRepository.lookupIepLevel("STD")

    repository.save(
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
      )
    ).apply {
      payRates = listOf(
        CourseActivityPayRate(
          courseActivity = this,
          iepLevelCode = seedIep.code,
          payBandCode = "4",
          startDate = LocalDate.parse("2022-12-01"),
          endDate = LocalDate.parse("2022-12-02"),
          halfDayRate = BigDecimal(0.6),
        )
      )
    }
    entityManager.flush()

    val persistedRecord = repository.findAll().first()
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
    assertThat(persistedRecord?.holiday).isFalse()
    assertThat(persistedRecord?.payPerSession).isEqualTo(PayPerSession.F)
    val rate = persistedRecord?.payRates?.first()
    assertThat(rate?.courseActivity?.courseActivityId).isEqualTo(persistedRecord?.courseActivityId)
    assertThat(rate?.iepLevelCode).isEqualTo("STD")
    assertThat(rate?.payBandCode).isEqualTo("4")
    assertThat(rate?.startDate).isEqualTo(LocalDate.parse("2022-12-01"))
    assertThat(rate?.endDate).isEqualTo(LocalDate.parse("2022-12-02"))
    assertThat(rate?.halfDayRate).isEqualTo(BigDecimal(0.6))
  }
}
