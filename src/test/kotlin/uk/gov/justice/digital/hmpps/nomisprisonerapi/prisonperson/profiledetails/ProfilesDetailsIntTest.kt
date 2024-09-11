package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.profiledetails

import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.roundToNearestSecond
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class ProfilesDetailsIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var repository: Repository

  @AfterEach
  fun cleanUp() {
    repository.deleteOffenders()
  }

  @Nested
  @DisplayName("GET /prisoners/{offenderNo}/profile-details")
  inner class GetProfileDetails {
    private lateinit var booking: OffenderBooking

    // The DB column is a DATE type so truncates milliseconds, but bizarrely H2 uses half-up rounding so I have to emulate here or tests fail
    val today = LocalDateTime.now().roundToNearestSecond()
    val yesterday = today.minusDays(1)

    @Nested
    inner class Security {
      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/prisoners/A1234AA/profile-details")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/prisoners/A1234AA/profile-details")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/prisoners/A1234AA/profile-details")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `not found if prisoner does not exist`() {
        webTestClient.get().uri("/prisoners/A1234AA/profile-details")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISON_PERSON")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `should return profile details`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingBeginDate = yesterday) {
              profile {
                detail(profileType = "L_EYE_C", profileCode = "RED")
                detail(profileType = "SHOESIZE", profileCode = "8.5")
              }
            }
          }
        }

        webTestClient.getProfileDetailsOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(offenderNo).isEqualTo("A1234AA")
              assertThat(bookings).extracting("bookingId", "startDateTime", "endDateTime", "latestBooking")
                .containsExactly(tuple(booking.bookingId, booking.bookingBeginDate, booking.bookingEndDate, true))
              assertThat(bookings[0].profileDetails).extracting("type", "code")
                .containsExactlyInAnyOrder(
                  tuple("L_EYE_C", "RED"),
                  tuple("SHOESIZE", "8.5"),
                )
              assertThat(bookings[0].profileDetails[0].createDateTime).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
              assertThat(bookings[0].profileDetails[0].createdBy).isEqualTo("SA")
            }
          }
      }

      @Test
      fun `should ignore null profile details`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking {
              profile {
                detail(profileType = "L_EYE_C", profileCode = "RED")
                detail(profileType = "SHOESIZE", profileCode = null)
              }
            }
          }
        }

        webTestClient.getProfileDetailsOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(bookings[0].profileDetails).extracting("type", "code")
                .containsExactlyInAnyOrder(
                  tuple("L_EYE_C", "RED"),
                )
            }
          }
      }

      @Test
      fun `should return empty list if no bookings`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA")
        }

        webTestClient.getProfileDetailsOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(bookings).isEmpty()
            }
          }
      }

      @Test
      fun `should return empty list if no profile details`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking()
          }
        }

        webTestClient.getProfileDetailsOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(bookings).isEmpty()
            }
          }
      }

      @Test
      fun `should return empty list if all profile details are null`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking {
              profile {
                detail(profileType = "L_EYE_C", profileCode = null)
                detail(profileType = "SHOESIZE", profileCode = null)
              }
            }
          }
        }

        webTestClient.getProfileDetailsOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(bookings).isEmpty()
            }
          }
      }

      // There are only 5 bookings in production with multiple profile details. We'll handle these manually rather than complicate the migration/sync.
      @Test
      fun `should only return the first profile details`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking {
              profile(sequence = 1) {
                detail(profileType = "L_EYE_C", profileCode = "RED")
                detail(profileType = "SHOESIZE", profileCode = "8.5")
              }
              profile(sequence = 2) {
                detail(profileType = "R_EYE_C", profileCode = "BLUE")
                detail(profileType = "BUILD", profileCode = "SLIM")
              }
            }
          }
        }

        webTestClient.getProfileDetailsOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(bookings[0].profileDetails).extracting("type", "code")
                .containsExactlyInAnyOrder(
                  tuple("L_EYE_C", "RED"),
                  tuple("SHOESIZE", "8.5"),
                )
            }
          }
      }

      @Test
      fun `should return profile details from old bookings`() {
        lateinit var oldBooking: OffenderBooking
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingSequence = 1, bookingBeginDate = today) {
              profile {
                detail(profileType = "HAIR", profileCode = "BROWN")
                detail(profileType = "FACIAL_HAIR", profileCode = "CLEAN SHAVEN")
              }
            }
            oldBooking = booking(bookingSequence = 2, bookingBeginDate = today.minusDays(2)) {
              profile {
                detail(profileType = "HAIR", profileCode = "BALD")
                detail(profileType = "FACIAL_HAIR", profileCode = "BEARDED")
              }
            }
          }
        }

        webTestClient.getProfileDetailsOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(bookings).extracting("bookingId", "startDateTime", "endDateTime", "latestBooking")
                .containsExactly(
                  tuple(booking.bookingId, booking.bookingBeginDate, booking.bookingEndDate, true),
                  tuple(oldBooking.bookingId, oldBooking.bookingBeginDate, oldBooking.bookingEndDate, false),
                )
              assertThat(bookings[0].profileDetails).extracting("type", "code")
                .containsExactlyInAnyOrder(
                  tuple("HAIR", "BROWN"),
                  tuple("FACIAL_HAIR", "CLEAN SHAVEN"),
                )
              assertThat(bookings[1].profileDetails).extracting("type", "code")
                .containsExactlyInAnyOrder(
                  tuple("HAIR", "BALD"),
                  tuple("FACIAL_HAIR", "BEARDED"),
                )
            }
          }
      }

      @Test
      fun `should return profile details from aliases`() {
        lateinit var aliasBooking: OffenderBooking
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingSequence = 1, bookingBeginDate = today) {
              profile {
                detail(profileType = "HAIR", profileCode = "BROWN")
                detail(profileType = "FACIAL_HAIR", profileCode = "CLEAN SHAVEN")
              }
            }
            alias {
              aliasBooking = booking(bookingSequence = 2, bookingBeginDate = today.minusDays(2)) {
                profile {
                  detail(profileType = "HAIR", profileCode = "BALD")
                  detail(profileType = "FACIAL_HAIR", profileCode = "BEARDED")
                }
                release(date = yesterday)
              }
            }
          }
        }

        webTestClient.getProfileDetailsOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(bookings).extracting("bookingId", "startDateTime", "endDateTime", "latestBooking")
                .containsExactly(
                  tuple(booking.bookingId, booking.bookingBeginDate, null, true),
                  tuple(aliasBooking.bookingId, today.minusDays(2), yesterday, false),
                )
              assertThat(bookings[0].profileDetails).extracting("type", "code")
                .containsExactlyInAnyOrder(
                  tuple("HAIR", "BROWN"),
                  tuple("FACIAL_HAIR", "CLEAN SHAVEN"),
                )
              assertThat(bookings[1].profileDetails).extracting("type", "code")
                .containsExactlyInAnyOrder(
                  tuple("HAIR", "BALD"),
                  tuple("FACIAL_HAIR", "BEARDED"),
                )
            }
          }
      }
    }

    fun WebTestClient.getProfileDetailsOk(offenderNo: String) =
      this.get().uri("/prisoners/$offenderNo/profile-details")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISON_PERSON")))
        .exchange()
        .expectStatus().isOk
        .expectBody<PrisonerProfileDetailsResponse>()
  }
}
