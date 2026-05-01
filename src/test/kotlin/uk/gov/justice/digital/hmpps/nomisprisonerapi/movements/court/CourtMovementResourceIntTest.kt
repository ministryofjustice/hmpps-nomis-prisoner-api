package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.court

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourtEventRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderCourtMovementInRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderCourtMovementOutRepository

class CourtMovementResourceIntTest(
  @Autowired private val courtEventRepository: CourtEventRepository,
  @Autowired private val courtMovementOutRepository: OffenderCourtMovementOutRepository,
  @Autowired private val courtMovementInRepository: OffenderCourtMovementInRepository,
) : IntegrationTestBase() {

  private val offenderNo = "B7463BB"
  private lateinit var offender: Offender
  private lateinit var booking: OffenderBooking
  private lateinit var staff: Staff

  @Nested
  @DisplayName("GET /movements/{offenderNo}/court/movement/out/{bookingId}/{movementSeq}")
  inner class GetCourtMovementOut {

    @Nested
    inner class HappyPath {
      // TODO remove this when we have some real tests
      @Test
      fun `check court movement test data builders work`() {
        nomisDataBuilder.build {
          staff = staff {
            account()
          }
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              courtMovementOut()
              courtMovementIn()
              courtScheduleOut {
                courtMovementOut()
                courtMovementIn()
              }
              courtCase(reportingStaff = staff) {
                courtEvent {
                  courtMovementOut()
                  courtMovementIn()
                }
              }
            }
          }
        }

        courtEventRepository.findAllByOffenderBooking_BookingId(booking.bookingId).apply {
          assertThat(size).isEqualTo(2)
          assertThat(filter { it.courtCase == null }).hasSize(1)
          assertThat(filter { it.courtCase != null }).hasSize(1)
        }

        courtMovementOutRepository.findAllByOffenderBooking_BookingId(booking.bookingId).apply {
          assertThat(size).isEqualTo(3)
          assertThat(filter { it.courtScheduleOut == null }).hasSize(1)
          assertThat(filter { it.courtScheduleOut != null && it.courtScheduleOut!!.courtCase != null }).hasSize(1)
          assertThat(filter { it.courtScheduleOut != null && it.courtScheduleOut!!.courtCase == null }).hasSize(1)
        }

        courtMovementInRepository.findAllByOffenderBooking_BookingId(booking.bookingId).apply {
          assertThat(size).isEqualTo(3)
          assertThat(filter { it.courtScheduleOut == null }).hasSize(1)
          assertThat(filter { it.courtScheduleOut != null && it.courtScheduleOut!!.courtCase != null }).hasSize(1)
          assertThat(filter { it.courtScheduleOut != null && it.courtScheduleOut!!.courtCase == null }).hasSize(1)
        }
      }
    }

    @Nested
    inner class Security {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking()
          }
        }
      }

      @Test
      fun `should return unauthorised for missing token`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/court/movement/out/12345/1")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/court/movement/out/12345/1")
          .headers(setAuthorisation())
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/court/movement/out/12345/1")
          .headers(setAuthorisation("ROLE_INVALID"))
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }

  @Nested
  @DisplayName("GET /movements/{offenderNo}/court/movement/in/{bookingId}/{movementSeq}")
  inner class GetCourtMovementIn {

    @Nested
    inner class Security {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking()
          }
        }
      }

      @Test
      fun `should return unauthorised for missing token`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/court/movement/in/12345/1")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/court/movement/in/12345/1")
          .headers(setAuthorisation())
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/court/movement/in/12345/1")
          .headers(setAuthorisation("ROLE_INVALID"))
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }
}
