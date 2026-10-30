package uk.gov.justice.digital.hmpps.nomisprisonerapi.bedassignments

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import java.time.LocalDateTime

class BedAssignmentHistoryResourceIntTest : IntegrationTestBase() {
  private lateinit var booking: OffenderBooking

  private val firstAssigned = LocalDateTime.parse("2023-05-01T09:15:00")
  private val moved = LocalDateTime.parse("2023-06-10T14:30:00")

  @AfterEach
  internal fun deleteData() {
    repository.deleteOffenders()
  }

  @DisplayName("GET /prisoners/booking-id/{bookingId}/bed-assignments/{sequence}")
  @Nested
  inner class GetBedAssignment {
    @BeforeEach
    fun init() {
      nomisDataBuilder.build {
        offender(nomsId = "A1111AA") {
          booking = booking {
            bedAssignmentHistory(
              sequence = 1,
              livingUnitId = -3009,
              assignmentDateTime = firstAssigned,
              assignmentReasonCode = "ADM",
              assignmentEndDateTime = moved,
            )
            bedAssignmentHistory(sequence = 2, livingUnitId = -3010, assignmentDateTime = moved, assignmentReasonCode = "BEH")
            bedAssignmentHistory(sequence = 3, livingUnitId = 999999, assignmentReasonCode = "UNKNOWN")
          }
        }
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/prisoners/booking-id/${booking.bookingId}/bed-assignments/1")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/prisoners/booking-id/${booking.bookingId}/bed-assignments/1")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/prisoners/booking-id/${booking.bookingId}/bed-assignments/1")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `booking not found`() {
        webTestClient.get().uri("/prisoners/booking-id/9999/bed-assignments/1")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("userMessage").value<String> {
            assertThat(it).contains("Booking with id 9999 not found")
          }
      }

      @Test
      fun `sequence not found`() {
        webTestClient.get().uri("/prisoners/booking-id/${booking.bookingId}/bed-assignments/4")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("userMessage").value<String> {
            assertThat(it).contains("Bed assignment for booking ${booking.bookingId} and sequence 4 not found")
          }
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `can get an ended bed assignment`() {
        webTestClient.get().uri("/prisoners/booking-id/${booking.bookingId}/bed-assignments/1")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody<BedAssignmentHistoryResponse>()
          .returnResult()
          .responseBody!!
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            assertThat(sequence).isEqualTo(1)
            assertThat(livingUnitId).isEqualTo(-3009)
            assertThat(livingUnitDescription).isEqualTo("BXI-A-1-016")
            assertThat(prisonId).isEqualTo("BXI")
            assertThat(assignmentDateTime).isEqualTo(firstAssigned)
            assertThat(assignmentEndDateTime).isEqualTo(moved)
            assertThat(assignmentReasonCode).isEqualTo("ADM")
            assertThat(assignmentReasonDescription).isEqualTo("Administrative")
            assertThat(audit.createUsername).isNotBlank()
            assertThat(audit.createDatetime).isNotNull()
          }
      }

      @Test
      fun `can get the current bed assignment`() {
        webTestClient.get().uri("/prisoners/booking-id/${booking.bookingId}/bed-assignments/2")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody<BedAssignmentHistoryResponse>()
          .returnResult()
          .responseBody!!
          .apply {
            assertThat(livingUnitDescription).isEqualTo("BXI-A-1-017")
            assertThat(assignmentDateTime).isEqualTo(moved)
            assertThat(assignmentEndDateTime).isNull()
            assertThat(assignmentReasonDescription).isEqualTo("Behaviour")
          }
      }

      @Test
      fun `returns the raw values when the living unit and reason are not in reference data`() {
        webTestClient.get().uri("/prisoners/booking-id/${booking.bookingId}/bed-assignments/3")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody<BedAssignmentHistoryResponse>()
          .returnResult()
          .responseBody!!
          .apply {
            assertThat(livingUnitId).isEqualTo(999999)
            assertThat(livingUnitDescription).isNull()
            assertThat(prisonId).isNull()
            assertThat(assignmentReasonCode).isEqualTo("UNKNOWN")
            assertThat(assignmentReasonDescription).isNull()
          }
      }
    }
  }

  @DisplayName("GET /prisoners/{offenderNo}/bed-assignments")
  @Nested
  inner class GetBedAssignmentsForPrisoner {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/prisoners/A1234AA/bed-assignments")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/prisoners/A1234AA/bed-assignments")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/prisoners/A1234AA/bed-assignments")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `prisoner not found`() {
        webTestClient.get().uri("/prisoners/DOESNOTEXIST/bed-assignments")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("userMessage").value<String> {
            assertThat(it).contains("Prisoner DOESNOTEXIST not found")
          }
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `can get history across all bookings in booking then sequence order`() {
        lateinit var oldBooking: OffenderBooking
        lateinit var latestBooking: OffenderBooking
        nomisDataBuilder.build {
          offender(nomsId = "A2222BB") {
            oldBooking = booking {
              bedAssignmentHistory(sequence = 2, livingUnitId = -41, assignmentDateTime = moved)
              bedAssignmentHistory(sequence = 1, livingUnitId = -3009, assignmentDateTime = firstAssigned, assignmentEndDateTime = moved)
              release()
            }
            latestBooking = booking {
              bedAssignmentHistory(sequence = 1, livingUnitId = -3010)
            }
          }
          offender(nomsId = "OTHER") {
            booking {
              bedAssignmentHistory(sequence = 1, livingUnitId = -3009)
            }
          }
        }

        webTestClient.get().uri("/prisoners/A2222BB/bed-assignments")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody<PrisonerBedAssignmentHistoryResponse>()
          .returnResult()
          .responseBody!!
          .apply {
            assertThat(bedAssignments.map { it.bookingId to it.sequence }).containsExactly(
              oldBooking.bookingId to 1,
              oldBooking.bookingId to 2,
              latestBooking.bookingId to 1,
            )
            assertThat(bedAssignments.map { it.prisonId }).containsExactly("BXI", "MDI", "BXI")
            assertThat(bedAssignments.map { it.livingUnitDescription }).containsExactly("BXI-A-1-016", "MDI-1-1-001", "BXI-A-1-017")
          }
      }

      @Test
      fun `returns an empty list when the prisoner has no bed history`() {
        nomisDataBuilder.build {
          offender(nomsId = "A3333CC") {
            booking()
          }
        }

        webTestClient.get().uri("/prisoners/A3333CC/bed-assignments")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody<PrisonerBedAssignmentHistoryResponse>()
          .returnResult()
          .responseBody!!
          .apply {
            assertThat(bedAssignments).isEmpty()
          }
      }
    }
  }
}
