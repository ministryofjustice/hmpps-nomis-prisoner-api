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
            offenderBooking = offender.allBookings.first(),
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
      randomTestingProgramRepository.save(
        RandomTestingProgram(
          id = 32345,
          caseloadId = "MDI",
          rtpDate = LocalDate.parse("2024-01-04"),
          mainPercentage = 20,
          reservePercentage = 8,
        ),
      )
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
            assertThat(offenderTestSelection.first().prisonNumber).isEqualTo(offender.nomsId)
          }
      }

      @Test
      fun `get random testing program when no offenders`() {
        webTestClient.get().uri("/drug-testing/32345")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse<RandomTestingProgramResponse>()
          .apply {
            assertThat(rtpId).isEqualTo(32345)
            assertThat(caseloadId).isEqualTo("MDI")
            assertThat(rtpDate).isEqualTo(LocalDate.parse("2024-01-04"))
            assertThat(mainPercentage).isEqualTo(20)
            assertThat(reservePercentage).isEqualTo(8)
            assertThat(selectionsCount).isNull()
            assertThat(eligibleCount).isNull()
            assertThat(reserveCount).isNull()
            assertThat(offenderTestSelection).hasSize(0)
          }
      }
    }
  }

  @DisplayName("GET /drug-testing/id-ranges")
  @Nested
  @TestInstance(PER_CLASS)
  inner class GetDrugTestingIdRanges {
    @BeforeAll
    internal fun init() {
      randomTestingProgramRepository.saveAll(
        listOf(
          RandomTestingProgram(
            id = 1,
            caseloadId = "MDI",
            rtpDate = LocalDate.parse("2024-01-01"),
            mainPercentage = 10,
            reservePercentage = 5,
          ),
          RandomTestingProgram(
            id = 2,
            caseloadId = "LEI",
            rtpDate = LocalDate.parse("2024-01-02"),
            mainPercentage = 10,
            reservePercentage = 5,
          ),
          RandomTestingProgram(
            id = 3,
            caseloadId = "BXI",
            rtpDate = LocalDate.parse("2024-01-03"),
            mainPercentage = 10,
            reservePercentage = 5,
          ),
        ),
      )
    }

    @AfterAll
    internal fun deleteData() {
      randomTestingProgramRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/drug-testing/id-ranges?pageSize=2")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/drug-testing/id-ranges?pageSize=2")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/drug-testing/id-ranges?pageSize=2")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `page size must be at least one`() {
        webTestClient.get().uri("/drug-testing/id-ranges?pageSize=0")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `get id ranges`() {
        webTestClient.get().uri("/drug-testing/id-ranges?pageSize=2")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .json("""[2]""")
      }

      @Test
      fun `get id ranges filtered by included prison ids`() {
        webTestClient.get().uri("/drug-testing/id-ranges?pageSize=1&includedPrisonIds=LEI&includedPrisonIds=BXI")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .json("""[2,3]""")
      }

      @Test
      fun `get id ranges filtered by excluded prison ids`() {
        webTestClient.get().uri("/drug-testing/id-ranges?pageSize=1&excludedPrisonIds=LEI")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .json("""[1,3]""")
      }
    }
  }

  @DisplayName("GET /drug-testing/ids-in-range")
  @Nested
  @TestInstance(PER_CLASS)
  inner class GetDrugTestingIdsInRange {
    @BeforeAll
    internal fun init() {
      randomTestingProgramRepository.saveAll(
        listOf(
          RandomTestingProgram(
            id = 10,
            caseloadId = "MDI",
            rtpDate = LocalDate.parse("2024-01-01"),
            mainPercentage = 10,
            reservePercentage = 5,
          ),
          RandomTestingProgram(
            id = 20,
            caseloadId = "LEI",
            rtpDate = LocalDate.parse("2024-01-02"),
            mainPercentage = 10,
            reservePercentage = 5,
          ),
          RandomTestingProgram(
            id = 30,
            caseloadId = "BXI",
            rtpDate = LocalDate.parse("2024-01-03"),
            mainPercentage = 10,
            reservePercentage = 5,
          ),
        ),
      )
    }

    @AfterAll
    internal fun deleteData() {
      randomTestingProgramRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/drug-testing/ids-in-range?fromId=0&toId=20")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/drug-testing/ids-in-range?fromId=0&toId=20")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/drug-testing/ids-in-range?fromId=0&toId=20")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `fromId is required`() {
        webTestClient.get().uri("/drug-testing/ids-in-range?toId=20")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `toId is required`() {
        webTestClient.get().uri("/drug-testing/ids-in-range?fromId=0")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `get ids in range`() {
        webTestClient.get().uri("/drug-testing/ids-in-range?fromId=0&toId=20")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .json("""[10,20]""")
      }

      @Test
      fun `get ids in range filtered by included prison ids`() {
        webTestClient.get().uri("/drug-testing/ids-in-range?fromId=0&toId=30&includedPrisonIds=LEI&includedPrisonIds=BXI")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .json("""[20,30]""")
      }

      @Test
      fun `get ids in range filtered by excluded prison ids`() {
        webTestClient.get().uri("/drug-testing/ids-in-range?fromId=0&toId=30&excludedPrisonIds=LEI")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .json("""[10,30]""")
      }
    }
  }
}
