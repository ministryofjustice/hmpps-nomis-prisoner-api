package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.transfers

import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.expectBodyResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransferMovementOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransferScheduleOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.transfers.movement.TransferMovementOut
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class TransferMovementResourceIntTest(
  @Autowired private val entityManager: EntityManager,
) : IntegrationTestBase() {

  private val offenderNo = "B7463BB"
  private lateinit var offender: Offender
  private lateinit var booking: OffenderBooking
  private lateinit var movementOut: OffenderTransferMovementOut
  private lateinit var scheduleOut: OffenderTransferScheduleOut

  @AfterEach
  fun tearDown() {
    repository.deleteOffenders()
  }

  @Nested
  @DisplayName("GET /movements/{offenderNo}/transfer/movement/out/{bookingId}/{movementSeq}")
  inner class GetTransferMovementOut {

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          booking = booking {
            scheduleOut = transferScheduleOut(fromPrison = "BXI", toPrison = "LEI")
            movementOut = transferMovementOut(
              date = LocalDateTime.parse("2023-05-01T09:15:00"),
              fromPrison = "BXI",
              toPrison = "LEI",
              movementReason = "28",
              escort = "U",
              comment = "Some comment",
              transferScheduleOutId = scheduleOut.eventId,
            )
          }
        }
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `should get all transfer movement details`() {
        webTestClient.getTransferMovementOutOk().apply {
          Assertions.assertThat(bookingId).isEqualTo(booking.bookingId)
          Assertions.assertThat(sequence).isEqualTo(movementOut.id.sequence)
          Assertions.assertThat(eventId).isEqualTo(scheduleOut.eventId)
          Assertions.assertThat(transferScheduleOutId).isEqualTo(scheduleOut.eventId)
          Assertions.assertThat(movementTime.toLocalDate()).isEqualTo(movementOut.movementDate)
          Assertions.assertThat(movementTime.toLocalTime()).isEqualTo(movementOut.getMovementDateAndTime().toLocalTime())
          Assertions.assertThat(movementReason).isEqualTo(movementOut.movementReason.id.reasonCode)
          Assertions.assertThat(escort).isEqualTo("U")
          Assertions.assertThat(fromPrison).isEqualTo(movementOut.fromAgency!!.id)
          Assertions.assertThat(toPrison).isEqualTo(movementOut.toAgency!!.id)
          Assertions.assertThat(active).isEqualTo(movementOut.active)
          Assertions.assertThat(commentText).isEqualTo(movementOut.commentText)
          Assertions.assertThat(audit.createUsername).isNotBlank()
          Assertions.assertThat(audit.createDatetime)
            .isCloseTo(movementOut.createDatetime, Assertions.within(10, ChronoUnit.SECONDS))
          Assertions.assertThat(userActiveCaseloadId).isEqualTo("CADM_I")
        }
      }

      @Test
      fun `should get unscheduled transfer movement details`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              movementOut = transferMovementOut(fromPrison = "BXI", toPrison = "LEI")
            }
          }
        }

        webTestClient.getTransferMovementOutOk().apply {
          Assertions.assertThat(bookingId).isEqualTo(booking.bookingId)
          Assertions.assertThat(sequence).isEqualTo(movementOut.id.sequence)
          Assertions.assertThat(eventId).isNull()
          Assertions.assertThat(transferScheduleOutId).isNull()
        }
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return not found if offender unknown`() {
        webTestClient.getTransferMovementOut(offenderNo = "UNKNOWN")
          .expectStatus().isNotFound
      }

      @Test
      fun `should return not found if booking doesn't exist`() {
        webTestClient.getTransferMovementOut(offenderNo = offender.nomsId, bookingId = 9999, sequence = 1)
          .expectStatus().isNotFound
      }

      @Test
      fun `should return not found if transfer movement doesn't exist`() {
        webTestClient.getTransferMovementOut(offenderNo = offender.nomsId, bookingId = booking.bookingId, sequence = 9999)
          .expectStatus().isNotFound
      }

      @Test
      fun `should return not found if transfer movement belongs to a different offender`() {
        lateinit var otherOffender: Offender
        nomisDataBuilder.build {
          otherOffender = offender(nomsId = "A1234BC") {
            booking()
          }
        }

        webTestClient.getTransferMovementOut(offenderNo = otherOffender.nomsId, bookingId = booking.bookingId, sequence = movementOut.id.sequence)
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class BadData {
      @Test
      fun `should return not found for a corrupt movement with a null from prison`() {
        repository.runInTransaction {
          entityManager.createNativeQuery(
            """
              update OFFENDER_EXTERNAL_MOVEMENTS set FROM_AGY_LOC_ID = null 
              where OFFENDER_BOOK_ID = ${movementOut.id.offenderBooking.bookingId} 
              and MOVEMENT_SEQ = ${movementOut.id.sequence}
            """.trimIndent(),
          ).executeUpdate()
        }

        webTestClient.getTransferMovementOut()
          .expectStatus().isNotFound
      }

      @Test
      fun `should handle deleted transfer schedule`() {
        repository.runInTransaction {
          entityManager.createNativeQuery(
            """
              delete from OFFENDER_IND_SCHEDULES where EVENT_ID = ${scheduleOut.eventId}
            """.trimIndent(),
          ).executeUpdate()
        }

        webTestClient.getTransferMovementOutOk().apply {
          Assertions.assertThat(bookingId).isEqualTo(booking.bookingId)
          Assertions.assertThat(sequence).isEqualTo(movementOut.id.sequence)
          Assertions.assertThat(eventId).isEqualTo(scheduleOut.eventId)
          Assertions.assertThat(transferScheduleOutId).isNull()
        }
      }

      @Test
      fun `should treat a transfer schedule on a different booking as orphaned`() {
        lateinit var otherBookingScheduleOut: OffenderTransferScheduleOut
        nomisDataBuilder.build {
          offender(nomsId = "A1234BC") {
            booking {
              otherBookingScheduleOut = transferScheduleOut(fromPrison = "BXI", toPrison = "LEI")
            }
          }
        }

        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              movementOut = transferMovementOut(
                fromPrison = "BXI",
                toPrison = "LEI",
                transferScheduleOutId = otherBookingScheduleOut.eventId,
              )
            }
          }
        }

        webTestClient.getTransferMovementOutOk().apply {
          Assertions.assertThat(bookingId).isEqualTo(booking.bookingId)
          Assertions.assertThat(sequence).isEqualTo(movementOut.id.sequence)
          Assertions.assertThat(eventId).isEqualTo(otherBookingScheduleOut.eventId)
          Assertions.assertThat(transferScheduleOutId).isNull()
        }
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `should return unauthorised for missing token`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/transfer/movement/out/12345/1")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/transfer/movement/out/12345/1")
          .headers(setAuthorisation())
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/transfer/movement/out/12345/1")
          .headers(setAuthorisation("ROLE_INVALID"))
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }

  private fun WebTestClient.getTransferMovementOut(offenderNo: String = offender.nomsId, bookingId: Long = movementOut.id.offenderBooking.bookingId, sequence: Int = movementOut.id.sequence) = get()
    .uri("/movements/$offenderNo/transfer/movement/out/$bookingId/$sequence")
    .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
    .exchange()

  private fun WebTestClient.getTransferMovementOutOk(offenderNo: String = offender.nomsId, bookingId: Long = movementOut.id.offenderBooking.bookingId, sequence: Int = movementOut.id.sequence) = getTransferMovementOut(offenderNo, bookingId, sequence)
    .expectStatus().isOk
    .expectBodyResponse<TransferMovementOut>()
}
