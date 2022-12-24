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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IEPLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PayPerSession
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProgramService
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

    val record = CourseActivity(
      code = "CA",
      program = seedProgramService,
      caseloadId = "LEI",
      prison = seedPrison,
      description = "test description",
      capacity = 23,
      active = true,
      scheduleStartDate = LocalDate.parse("2022-10-31"),
      scheduleEndDate = LocalDate.parse("2022-11-30"),
      iepLevel = "Standard",
      internalLocation = seedRoom,
      payPerSession = PayPerSession.F,
    )

    repository.save(record)
    entityManager.flush()

    val persistedRecord = repository.findAll().first()
    assertThat(persistedRecord).isNotNull

    assertThat(persistedRecord?.code).isEqualTo("CA")
    assertThat(persistedRecord?.program?.programCode).isEqualTo("TESTPS")
    assertThat(persistedRecord?.scheduleStartDate).isEqualTo(LocalDate.parse("2022-10-31"))
    assertThat(persistedRecord?.scheduleEndDate).isEqualTo(LocalDate.parse("2022-11-30"))
    assertThat(persistedRecord?.caseloadId).isEqualTo("LEI")
    assertThat(persistedRecord?.description).isEqualTo("test description")
    assertThat(persistedRecord?.prison?.id).isEqualTo("LEI")
    assertThat(persistedRecord?.iepLevel).isEqualTo("Standard")
    assertThat(persistedRecord?.internalLocation?.locationId).isEqualTo(-9)
  }
}
