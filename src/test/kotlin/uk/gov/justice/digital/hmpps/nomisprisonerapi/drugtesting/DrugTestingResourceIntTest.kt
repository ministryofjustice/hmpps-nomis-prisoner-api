package uk.gov.justice.digital.hmpps.nomisprisonerapi.drugtesting

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBookingDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.expectBodyResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTestSelection
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTestSelectionId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.RandomTestingProgram
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.RandomTestingProgramRepository
import java.time.LocalDate

class DrugTestingResourceIntTest(
  @Autowired
  private val randomTestingProgramRepository: RandomTestingProgramRepository,
  @Autowired
  private val builderRepository: Repository,
) : IntegrationTestBase() {
  @DisplayName("GET /drug-testing/{rtpId}")
  @Nested
  @TestInstance(PER_CLASS)
  inner class GetDrugTesting {
    private lateinit var offender: Offender

    @BeforeAll
    internal fun init() {
      offender = builderRepository.save(OffenderDataBuilder().withBooking(OffenderBookingDataBuilder()))
      val program = randomTestingProgramRepository.save(
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
      program.offenderTestSelection.add(
        OffenderTestSelection(
          id = OffenderTestSelectionId(
            offenderBookId = offender.allBookings.first().bookingId,
            randomTestingProgram = program,
          ),
          testSelectionType = "R",
          testSelectionNo = 1,
          testedFlag = "N",
          reasonNotTested = "NOT_TAKEN",
          notes = "selected in test",
        ),
      )
      randomTestingProgramRepository.save(program)
    }

    @AfterAll
    internal fun deleteData() {
      randomTestingProgramRepository.deleteAll()
      repository.delete(offender)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/drug-testing/1")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/drug-testing/1")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/drug-testing/1")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `id doesn't exist`() {
        webTestClient.get().uri("/drug-testing/9999")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("userMessage").value<String> {
            assertThat(it).contains("Random testing program with id 9999 not found")
          }
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `get random testing program with offenders`() {
        webTestClient.get().uri("/drug-testing/12345")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse<RandomTestingProgramResponse>()
          .apply {
            assertThat(rtpId).isEqualTo(12345)
            assertThat(caseloadId).isEqualTo("MDI")
            assertThat(rtpDate).isEqualTo(LocalDate.parse("2024-01-02"))
            assertThat(mainPercentage).isEqualTo(10)
            assertThat(reservePercentage).isEqualTo(5)
            assertThat(selectionsCount).isEqualTo(12)
            assertThat(eligibleCount).isEqualTo(30)
            assertThat(reserveCount).isEqualTo(3)
            assertThat(offenderTestSelection).hasSize(1)
            assertThat(offenderTestSelection.first().offenderBookId).isEqualTo(offender.allBookings.first().bookingId)
          }
      }
    }
  }
}
