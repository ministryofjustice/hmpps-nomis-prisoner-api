package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.AuditorAwareImpl
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBookingDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTestSelection
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTestSelectionId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.RandomTestingProgram
import uk.gov.justice.hmpps.kotlin.auth.HmppsAuthenticationHolder
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser
import java.time.LocalDate

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(HmppsAuthenticationHolder::class, AuditorAwareImpl::class, Repository::class, NomisDataBuilder::class)
@WithMockAuthUser
class RandomTestingProgramRepositoryTest(
  @Autowired private val repository: RandomTestingProgramRepository,
  @Autowired private val builderRepository: Repository,
) {

  @Test
  fun `save and find random testing program`() {
    val saved = repository.save(
      RandomTestingProgram(
        id = 12345,
        caseloadId = "MDI",
        rtpDate = LocalDate.parse("2024-01-02"),
        mainPercentage = 10,
        reservePercentage = 5,
        selectionsCount = 12,
        eligibleCount = 30,
        reserveCount = 3,
      ),
    )

    val found = repository.findById(saved.id).orElseThrow()

    assertThat(found.id).isEqualTo(12345)
    assertThat(found.caseloadId).isEqualTo("MDI")
    assertThat(found.rtpDate).isEqualTo(LocalDate.parse("2024-01-02"))
    assertThat(found.mainPercentage).isEqualTo(10)
    assertThat(found.reservePercentage).isEqualTo(5)
    assertThat(found.selectionsCount).isEqualTo(12)
    assertThat(found.eligibleCount).isEqualTo(30)
    assertThat(found.reserveCount).isEqualTo(3)
    assertThat(found.offenderTestSelection).isEmpty()
    assertThat(found).isEqualTo(saved)

    repository.delete(found)
  }

  @Test
  fun `find random testing program with offenders`() {
    val savedOffender = builderRepository.save(OffenderDataBuilder().withBooking(OffenderBookingDataBuilder()))
    val saved = repository.save(
      RandomTestingProgram(
        id = 22345,
        caseloadId = "MDI",
        rtpDate = LocalDate.parse("2024-01-03"),
        mainPercentage = 15,
        reservePercentage = 7,
      ).also {
        it.offenderTestSelection.add(
          OffenderTestSelection(
            id = OffenderTestSelectionId(
              offenderBooking = savedOffender.allBookings.first(),
              randomTestingProgram = it,
            ),
            testSelectionType = "R",
            testSelectionNo = 1,
            testedFlag = "N",
            reasonNotTested = "NOT_TAKEN",
            notes = "selected in test",
          ),
        )
      },
    )

    val found = repository.findRandomTestingProgramWithOffenders(saved.id)!!

    assertThat(found.id).isEqualTo(22345)
    assertThat(found.offenderTestSelection).hasSize(1)
    assertThat(found.offenderTestSelection.first().id?.offenderBooking).isEqualTo(savedOffender.allBookings.first())
    assertThat(found.offenderTestSelection.first().testSelectionType).isEqualTo("R")
    assertThat(found.offenderTestSelection.first().testSelectionNo).isEqualTo(1)
    assertThat(found.offenderTestSelection.first().testedFlag).isEqualTo("N")
    assertThat(found.offenderTestSelection.first().reasonNotTested).isEqualTo("NOT_TAKEN")
    assertThat(found.offenderTestSelection.first().notes).isEqualTo("selected in test")

    repository.delete(found)
  }

  @Test
  fun `find random testing program with offenders when none exist`() {
    val saved = repository.save(
      RandomTestingProgram(
        id = 32345,
        caseloadId = "MDI",
        rtpDate = LocalDate.parse("2024-01-04"),
        mainPercentage = 20,
        reservePercentage = 8,
      ),
    )

    repository.findRandomTestingProgramWithOffenders(saved.id)
  }

  @Test
  fun `find random testing program with offenders for invalid id`() {
    assertThat(repository.findRandomTestingProgramWithOffenders(9999)).isNull()
  }
}
