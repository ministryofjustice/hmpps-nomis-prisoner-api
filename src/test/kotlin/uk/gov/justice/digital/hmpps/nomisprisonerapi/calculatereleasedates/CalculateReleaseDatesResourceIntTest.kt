package uk.gov.justice.digital.hmpps.nomisprisonerapi.calculatereleasedates

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.expectBodyResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentenceCalculation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import java.time.LocalDate
import java.time.LocalDateTime

class CalculateReleaseDatesResourceIntTest : IntegrationTestBase() {
  private lateinit var staff: Staff
  private lateinit var booking: OffenderBooking
  private lateinit var calculation: OffenderSentenceCalculation
  private lateinit var minimalCalculation: OffenderSentenceCalculation
  private val expectedCalculationDate = LocalDateTime.parse("2024-03-04T10:15:30")
  private val recordDate = LocalDateTime.parse("2024-03-04T10:16:00")

  @BeforeEach
  fun setUp() {
    nomisDataBuilder.build {
      staff = staff(firstName = "JANE", lastName = "SMITH")
      offender(nomsId = "A1234TT") {
        booking = booking {
          calculation = sentenceCalculation(
            calculationDate = expectedCalculationDate,
            staff = staff,
            calculationReasonCode = "NEW",
            commentText = "A comment",
            effectiveSentenceEndDate = LocalDate.parse("2030-01-01"),
            effectiveSentenceLength = "05/06/07",
            judiciallyImposedSentenceLength = "08/09/10",
            workflowHistoryId = 12345,
            recordDatetime = recordDate,
            recordUserId = "REC_USER",
            hdcedCalculatedDate = LocalDate.parse("2025-01-01"),
            hdcedOverridedDate = LocalDate.parse("2025-01-02"),
            hdcadCalculatedDate = LocalDate.parse("2025-01-03"),
            hdcadOverridedDate = LocalDate.parse("2025-01-04"),
            etdCalculatedDate = LocalDate.parse("2025-02-01"),
            etdOverridedDate = LocalDate.parse("2025-02-02"),
            mtdCalculatedDate = LocalDate.parse("2025-02-03"),
            mtdOverridedDate = LocalDate.parse("2025-02-04"),
            ltdCalculatedDate = LocalDate.parse("2025-02-05"),
            ltdOverridedDate = LocalDate.parse("2025-02-06"),
            ardCalculatedDate = LocalDate.parse("2025-03-01"),
            ardOverridedDate = LocalDate.parse("2025-03-02"),
            crdCalculatedDate = LocalDate.parse("2025-03-03"),
            crdOverridedDate = LocalDate.parse("2025-03-04"),
            pedCalculatedDate = LocalDate.parse("2025-03-05"),
            pedOverridedDate = LocalDate.parse("2025-03-06"),
            apdCalculatedDate = LocalDate.parse("2025-04-01"),
            apdOverridedDate = LocalDate.parse("2025-04-02"),
            npdCalculatedDate = LocalDate.parse("2025-04-03"),
            npdOverridedDate = LocalDate.parse("2025-04-04"),
            ledCalculatedDate = LocalDate.parse("2025-04-05"),
            ledOverridedDate = LocalDate.parse("2025-04-06"),
            sedCalculatedDate = LocalDate.parse("2025-05-01"),
            sedOverridedDate = LocalDate.parse("2025-05-02"),
            prrdCalculatedDate = LocalDate.parse("2025-05-03"),
            prrdOverridedDate = LocalDate.parse("2025-05-04"),
            tariffCalculatedDate = LocalDate.parse("2025-05-05"),
            tariffOverridedDate = LocalDate.parse("2025-05-06"),
            dprrdCalculatedDate = LocalDate.parse("2025-06-01"),
            dprrdOverridedDate = LocalDate.parse("2025-06-02"),
            tusedCalculatedDate = LocalDate.parse("2025-06-03"),
            tusedOverridedDate = LocalDate.parse("2025-06-04"),
            ersedOverridedDate = LocalDate.parse("2025-06-05"),
            tersedOverridedDate = LocalDate.parse("2025-06-06"),
            rotlOverridedDate = LocalDate.parse("2025-06-07"),
          )
          minimalCalculation = sentenceCalculation()
        }
      }
    }
  }

  @AfterEach
  fun tearDown() {
    repository.deleteOffenders()
    repository.deleteStaff(staff.id)
  }

  @DisplayName("GET /sentence-calculations/{offenderSentenceCalculationId}")
  @Nested
  inner class GetOffenderSentenceCalculation {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/sentence-calculations/${calculation.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/sentence-calculations/${calculation.id}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/sentence-calculations/${calculation.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `returns 404 when calculation does not exist`() {
        webTestClient.get().uri("/sentence-calculations/999999")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("userMessage").value<String> {
            assertThat(it).contains("Offender sentence calculation with id 999999 not found")
          }
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `returns all the data passed in via the DSL`() {
        webTestClient.get().uri("/sentence-calculations/${calculation.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse<OffenderSentenceCalculationResponse>()
          .apply {
            assertThat(id).isEqualTo(calculation.id)
            assertThat(bookingId).isEqualTo(booking.bookingId)
            assertThat(offenderNo).isEqualTo("A1234TT")
            assertThat(calculationDate).isEqualTo(expectedCalculationDate)
            assertThat(staffId).isEqualTo(staff.id)
            assertThat(calculationReasonCode).isEqualTo("NEW")
            assertThat(commentText).isEqualTo("A comment")
            assertThat(effectiveSentenceEndDate).isEqualTo(LocalDate.parse("2030-01-01"))
            assertThat(effectiveSentenceLength).isEqualTo("05/06/07")
            assertThat(judiciallyImposedSentenceLength).isEqualTo("08/09/10")
            assertThat(workflowHistoryId).isEqualTo(12345)
            assertThat(recordDatetime).isEqualTo(recordDate)
            assertThat(recordUserId).isEqualTo("REC_USER")
            assertThat(hdcedCalculatedDate).isEqualTo(LocalDate.parse("2025-01-01"))
            assertThat(hdcedOverridedDate).isEqualTo(LocalDate.parse("2025-01-02"))
            assertThat(hdcadCalculatedDate).isEqualTo(LocalDate.parse("2025-01-03"))
            assertThat(hdcadOverridedDate).isEqualTo(LocalDate.parse("2025-01-04"))
            assertThat(etdCalculatedDate).isEqualTo(LocalDate.parse("2025-02-01"))
            assertThat(etdOverridedDate).isEqualTo(LocalDate.parse("2025-02-02"))
            assertThat(mtdCalculatedDate).isEqualTo(LocalDate.parse("2025-02-03"))
            assertThat(mtdOverridedDate).isEqualTo(LocalDate.parse("2025-02-04"))
            assertThat(ltdCalculatedDate).isEqualTo(LocalDate.parse("2025-02-05"))
            assertThat(ltdOverridedDate).isEqualTo(LocalDate.parse("2025-02-06"))
            assertThat(ardCalculatedDate).isEqualTo(LocalDate.parse("2025-03-01"))
            assertThat(ardOverridedDate).isEqualTo(LocalDate.parse("2025-03-02"))
            assertThat(crdCalculatedDate).isEqualTo(LocalDate.parse("2025-03-03"))
            assertThat(crdOverridedDate).isEqualTo(LocalDate.parse("2025-03-04"))
            assertThat(pedCalculatedDate).isEqualTo(LocalDate.parse("2025-03-05"))
            assertThat(pedOverridedDate).isEqualTo(LocalDate.parse("2025-03-06"))
            assertThat(apdCalculatedDate).isEqualTo(LocalDate.parse("2025-04-01"))
            assertThat(apdOverridedDate).isEqualTo(LocalDate.parse("2025-04-02"))
            assertThat(npdCalculatedDate).isEqualTo(LocalDate.parse("2025-04-03"))
            assertThat(npdOverridedDate).isEqualTo(LocalDate.parse("2025-04-04"))
            assertThat(ledCalculatedDate).isEqualTo(LocalDate.parse("2025-04-05"))
            assertThat(ledOverridedDate).isEqualTo(LocalDate.parse("2025-04-06"))
            assertThat(sedCalculatedDate).isEqualTo(LocalDate.parse("2025-05-01"))
            assertThat(sedOverridedDate).isEqualTo(LocalDate.parse("2025-05-02"))
            assertThat(prrdCalculatedDate).isEqualTo(LocalDate.parse("2025-05-03"))
            assertThat(prrdOverridedDate).isEqualTo(LocalDate.parse("2025-05-04"))
            assertThat(tariffCalculatedDate).isEqualTo(LocalDate.parse("2025-05-05"))
            assertThat(tariffOverridedDate).isEqualTo(LocalDate.parse("2025-05-06"))
            assertThat(dprrdCalculatedDate).isEqualTo(LocalDate.parse("2025-06-01"))
            assertThat(dprrdOverridedDate).isEqualTo(LocalDate.parse("2025-06-02"))
            assertThat(tusedCalculatedDate).isEqualTo(LocalDate.parse("2025-06-03"))
            assertThat(tusedOverridedDate).isEqualTo(LocalDate.parse("2025-06-04"))
            assertThat(ersedOverridedDate).isEqualTo(LocalDate.parse("2025-06-05"))
            assertThat(tersedOverridedDate).isEqualTo(LocalDate.parse("2025-06-06"))
            assertThat(rotlOverridedDate).isEqualTo(LocalDate.parse("2025-06-07"))
          }
      }

      @Test
      fun `returns nulls for optional data not supplied`() {
        webTestClient.get().uri("/sentence-calculations/${minimalCalculation.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse<OffenderSentenceCalculationResponse>()
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            assertThat(staffId).isNull()
            assertThat(commentText).isNull()
            assertThat(effectiveSentenceEndDate).isNull()
            assertThat(hdcedCalculatedDate).isNull()
            assertThat(rotlOverridedDate).isNull()
          }
      }
    }
  }
}
