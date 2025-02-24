package uk.gov.justice.digital.hmpps.nomisprisonerapi.profiledetails

import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.roundToNearestSecond
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class ProfilesDetailsIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var repository: Repository

  @Autowired
  private lateinit var offenderBookingRepository: OffenderBookingRepository

  @AfterEach
  fun cleanUp() {
    repository.deleteOffenders()
  }

  // The DB column is a DATE type so truncates milliseconds, but bizarrely H2 uses half-up rounding so I have to emulate here or tests fail
  val today = LocalDateTime.now().roundToNearestSecond()
  val yesterday = today.minusDays(1)

  @Nested
  @DisplayName("GET /prisoners/{offenderNo}/profile-details")
  inner class GetProfileDetails {
    private lateinit var booking: OffenderBooking

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
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
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
              profile()
              profileDetail(profileType = "L_EYE_C", profileCode = "RED")
              profileDetail(profileType = "SHOESIZE", profileCode = "8.5")
            }
          }
        }

        webTestClient.getProfileDetailsOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(offenderNo).isEqualTo("A1234AA")
              assertThat(bookings).extracting("bookingId", "latestBooking")
                .containsExactly(tuple(booking.bookingId, true))
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
      fun `should return profile details where there is no profile`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking {
              profileDetail(profileType = "L_EYE_C", profileCode = "RED")
              profileDetail(profileType = "SHOESIZE", profileCode = "8.5")
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
      fun `should return null profile details`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking {
              profile()
              profileDetail(profileType = "L_EYE_C", profileCode = "RED")
              profileDetail(profileType = "SHOESIZE", profileCode = null)
            }
          }
        }

        webTestClient.getProfileDetailsOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(bookings[0].profileDetails).extracting("type", "code")
                .containsExactlyInAnyOrder(
                  tuple("L_EYE_C", "RED"),
                  tuple("SHOESIZE", null),
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
      fun `should not return empty list if all profile details are null`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking {
              profile()
              profileDetail(profileType = "L_EYE_C", profileCode = null)
              profileDetail(profileType = "SHOESIZE", profileCode = null)
            }
          }
        }

        webTestClient.getProfileDetailsOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(bookings[0].profileDetails).extracting("type", "code")
                .containsExactlyInAnyOrder(
                  tuple("L_EYE_C", null),
                  tuple("SHOESIZE", null),
                )
            }
          }
      }

      // There are only 5 bookings in production with multiple profile details. We'll handle these manually rather than complicate the migration/sync.
      @Test
      fun `should only return the first profile details`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking {
              profile(sequence = 1L)
              profileDetail(sequence = 1L, profileType = "L_EYE_C", profileCode = "RED")
              profileDetail(sequence = 1L, profileType = "SHOESIZE", profileCode = "8.5")
              profile(sequence = 2L)
              profileDetail(sequence = 2L, profileType = "R_EYE_C", profileCode = "BLUE")
              profileDetail(sequence = 2L, profileType = "BUILD", profileCode = "SLIM")
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
      fun `should only return the first profile details where there are no profile records`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking {
              profileDetail(sequence = 1L, profileType = "L_EYE_C", profileCode = "RED")
              profileDetail(sequence = 1L, profileType = "SHOESIZE", profileCode = "8.5")
              profileDetail(sequence = 2L, profileType = "R_EYE_C", profileCode = "BLUE")
              profileDetail(sequence = 2L, profileType = "BUILD", profileCode = "SLIM")
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
              profile()
              profileDetail(profileType = "HAIR", profileCode = "BROWN")
              profileDetail(profileType = "FACIAL_HAIR", profileCode = "CLEAN SHAVEN")
            }
            oldBooking = booking(bookingSequence = 2, bookingBeginDate = today.minusDays(2)) {
              profile()
              profileDetail(profileType = "HAIR", profileCode = "BALD")
              profileDetail(profileType = "FACIAL_HAIR", profileCode = "BEARDED")
            }
          }
        }

        webTestClient.getProfileDetailsOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(bookings).extracting("bookingId", "latestBooking", "startDateTime")
                .containsExactly(
                  tuple(booking.bookingId, true, booking.bookingBeginDate),
                  tuple(oldBooking.bookingId, false, oldBooking.bookingBeginDate),
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
      fun `should return profile details from old bookings where there are no profile records`() {
        lateinit var oldBooking: OffenderBooking
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingSequence = 1, bookingBeginDate = today) {
              profileDetail(profileType = "HAIR", profileCode = "BROWN")
              profileDetail(profileType = "FACIAL_HAIR", profileCode = "CLEAN SHAVEN")
            }
            oldBooking = booking(bookingSequence = 2, bookingBeginDate = today.minusDays(2)) {
              profileDetail(profileType = "HAIR", profileCode = "BALD")
              profileDetail(profileType = "FACIAL_HAIR", profileCode = "BEARDED")
            }
          }
        }

        webTestClient.getProfileDetailsOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(bookings).extracting("bookingId", "latestBooking", "startDateTime")
                .containsExactly(
                  tuple(booking.bookingId, true, booking.bookingBeginDate),
                  tuple(oldBooking.bookingId, false, oldBooking.bookingBeginDate),
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
              profile()
              profileDetail(profileType = "HAIR", profileCode = "BROWN")
              profileDetail(profileType = "FACIAL_HAIR", profileCode = "CLEAN SHAVEN")
            }
            alias {
              aliasBooking = booking(bookingSequence = 2, bookingBeginDate = today.minusDays(2)) {
                profile()
                profileDetail(profileType = "HAIR", profileCode = "BALD")
                profileDetail(profileType = "FACIAL_HAIR", profileCode = "BEARDED")
                release(date = yesterday)
              }
            }
          }
        }

        webTestClient.getProfileDetailsOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(bookings).extracting("bookingId", "latestBooking", "startDateTime")
                .containsExactly(
                  tuple(booking.bookingId, true, booking.bookingBeginDate),
                  tuple(aliasBooking.bookingId, false, aliasBooking.bookingBeginDate),
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
      fun `should return profile details for requested profile types`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingSequence = 1, bookingBeginDate = today) {
              profile()
              profileDetail(profileType = "HAIR", profileCode = "BROWN")
              profileDetail(profileType = "FACIAL_HAIR", profileCode = "CLEAN SHAVEN")
              profileDetail(profileType = "SHOESIZE", profileCode = "8.5")
            }
          }
        }

        webTestClient.getProfileDetailsOk("A1234AA", profileTypes = listOf("HAIR", "SHOESIZE"))
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(bookings).extracting("bookingId", "latestBooking", "startDateTime")
                .containsExactly(tuple(booking.bookingId, true, booking.bookingBeginDate))
              assertThat(bookings[0].profileDetails).extracting("type", "code")
                .containsExactlyInAnyOrder(
                  tuple("HAIR", "BROWN"),
                  tuple("SHOESIZE", "8.5"),
                )
            }
          }
      }

      @Test
      fun `should return profile details for requested booking ID`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingSequence = 1, bookingBeginDate = today) {
              profile()
              profileDetail(profileType = "HAIR", profileCode = "BROWN")
              profileDetail(profileType = "SHOESIZE", profileCode = "8.5")
            }
            booking(bookingSequence = 2, bookingBeginDate = yesterday, bookingEndDate = today.toLocalDate()) {
              profile()
              profileDetail(profileType = "HAIR", profileCode = "BLACK")
              profileDetail(profileType = "SHOESIZE", profileCode = "9")
            }
          }
        }

        webTestClient.getProfileDetailsOk("A1234AA", bookingId = booking.bookingId)
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(bookings).extracting("bookingId", "latestBooking", "startDateTime")
                .containsExactly(tuple(booking.bookingId, true, booking.bookingBeginDate))
              assertThat(bookings[0].profileDetails).extracting("type", "code")
                .containsExactlyInAnyOrder(
                  tuple("HAIR", "BROWN"),
                  tuple("SHOESIZE", "8.5"),
                )
            }
          }
      }

      @Test
      fun `should return profile details for requested booking ID and profile type`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingSequence = 1, bookingBeginDate = today) {
              profile()
              profileDetail(profileType = "HAIR", profileCode = "BROWN")
              profileDetail(profileType = "SHOESIZE", profileCode = "8.5")
            }
            booking(bookingSequence = 2, bookingBeginDate = yesterday, bookingEndDate = today.toLocalDate()) {
              profile()
              profileDetail(profileType = "HAIR", profileCode = "BLACK")
              profileDetail(profileType = "SHOESIZE", profileCode = "9")
            }
          }
        }

        webTestClient.getProfileDetailsOk("A1234AA", profileTypes = listOf("HAIR"), bookingId = booking.bookingId)
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(bookings).extracting("bookingId", "latestBooking", "startDateTime")
                .containsExactly(tuple(booking.bookingId, true, booking.bookingBeginDate))
              assertThat(bookings[0].profileDetails).extracting("type", "code")
                .containsExactlyInAnyOrder(
                  tuple("HAIR", "BROWN"),
                )
            }
          }
      }
    }

    fun WebTestClient.getProfileDetailsOk(offenderNo: String, profileTypes: List<String> = listOf(), bookingId: Long? = null) = this.get()
      .uri {
        it.path("/prisoners/$offenderNo/profile-details")
          .queryParam("profileTypes", profileTypes)
          .apply { bookingId?.let { queryParam("bookingId", bookingId) } }
          .build()
      }
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
      .exchange()
      .expectStatus().isOk
      .expectBody<PrisonerProfileDetailsResponse>()
  }

  @Nested
  @DisplayName("PUT /prisoners/{offenderNo}/profile-details")
  inner class UpsertProfileDetails {
    private lateinit var booking: OffenderBooking

    @Nested
    inner class Security {
      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put().uri("/prisoners/A1234AA/profile-details")
          .bodyValue(UpsertProfileDetailsRequest("HAIR", "BROWN"))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put().uri("/prisoners/A1234AA/profile-details")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(UpsertProfileDetailsRequest("HAIR", "BROWN"))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put().uri("/prisoners/A1234AA/profile-details")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .bodyValue(UpsertProfileDetailsRequest("HAIR", "BROWN"))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `not found if prisoner does not exist`() {
        webTestClient.put().uri("/prisoners/A1234AA/profile-details")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
          .bodyValue(UpsertProfileDetailsRequest("HAIR", "BROWN"))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `should create profile details`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingBeginDate = yesterday)
          }
        }

        webTestClient.upsertProfileDetailsOk("A1234AA", "BUILD", "SMALL")
          .consumeWith {
            assertThat(it.responseBody!!.bookingId).isEqualTo(booking.bookingId)
            assertThat(it.responseBody!!.created).isTrue()
          }

        repository.runInTransaction {
          with(findBooking().profileDetails) {
            assertThat(size).isEqualTo(1)
            with(first()) {
              assertThat(id.profileType.type).isEqualTo("BUILD")
              assertThat(profileCodeId).isEqualTo("SMALL")
              assertThat(offenderProfile?.id?.sequence).isEqualTo(1)
            }
          }
        }
      }

      @Test
      fun `should update profile details`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingBeginDate = yesterday) {
              profile()
              profileDetail(profileType = "BUILD", profileCode = "MEDIUM")
            }
          }
        }

        webTestClient.upsertProfileDetailsOk("A1234AA", "BUILD", "SMALL")
          .consumeWith {
            assertThat(it.responseBody!!.bookingId).isEqualTo(booking.bookingId)
            assertThat(it.responseBody!!.created).isFalse()
          }

        repository.runInTransaction {
          with(findBooking().profileDetails) {
            assertThat(size).isEqualTo(1)
            with(first()) {
              assertThat(id.profileType.type).isEqualTo("BUILD")
              assertThat(profileCodeId).isEqualTo("SMALL")
            }
          }
        }
      }

      @Test
      fun `should update profile details where there is no profile record`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingBeginDate = yesterday) {
              profileDetail(profileType = "BUILD", profileCode = "MEDIUM")
            }
          }
        }

        webTestClient.upsertProfileDetailsOk("A1234AA", "BUILD", "SMALL")
          .consumeWith {
            assertThat(it.responseBody!!.bookingId).isEqualTo(booking.bookingId)
            assertThat(it.responseBody!!.created).isFalse()
          }

        repository.runInTransaction {
          with(findBooking().profileDetails) {
            assertThat(size).isEqualTo(1)
            with(first()) {
              assertThat(id.profileType.type).isEqualTo("BUILD")
              assertThat(profileCodeId).isEqualTo("SMALL")
            }
          }
        }
      }

      @Test
      fun `should publish telemetry for creating profile details`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingBeginDate = yesterday)
          }
        }

        webTestClient.upsertProfileDetailsOk("A1234AA", "BUILD", "SMALL")

        verify(telemetryClient).trackEvent(
          "physical-attributes-profile-details-created",
          mapOf(
            "offenderNo" to "A1234AA",
            "bookingId" to booking.bookingId.toString(),
            "profileType" to "BUILD",
          ),
          null,
        )
      }

      @Test
      fun `should publish telemetry for updating profile details`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingBeginDate = yesterday) {
              profile()
              profileDetail(profileType = "BUILD", profileCode = "MEDIUM")
            }
          }
        }

        webTestClient.upsertProfileDetailsOk("A1234AA", "BUILD", "SMALL")

        verify(telemetryClient).trackEvent(
          "physical-attributes-profile-details-updated",
          mapOf(
            "offenderNo" to "A1234AA",
            "bookingId" to booking.bookingId.toString(),
            "profileType" to "BUILD",
          ),
          null,
        )
      }

      @Test
      fun `should only update latest booking`() {
        lateinit var oldBooking: OffenderBooking
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingSequence = 1, bookingBeginDate = yesterday) {
              profile()
              profileDetail(profileType = "BUILD", profileCode = "MEDIUM")
            }
            oldBooking =
              booking(bookingSequence = 2, bookingBeginDate = yesterday, bookingEndDate = yesterday.toLocalDate()) {
                profile()
                profileDetail(profileType = "BUILD", profileCode = "HEAVY")
              }
          }
        }

        webTestClient.upsertProfileDetailsOk("A1234AA", "BUILD", "SMALL")

        repository.runInTransaction {
          // The new booking was updated
          with(findBooking().profileDetails) {
            with(first()) {
              assertThat(id.profileType.type).isEqualTo("BUILD")
              assertThat(profileCodeId).isEqualTo("SMALL")
            }
          }
          // The old booking wasn't updated
          with(findBooking(oldBooking.bookingId).profileDetails.first()) {
            assertThat(id.profileType.type).isEqualTo("BUILD")
            assertThat(profileCodeId).isEqualTo("HEAVY")
          }
        }
      }

      @Test
      fun `should ignore profile sequence greater than 1`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingSequence = 1, bookingBeginDate = yesterday) {
              profile(sequence = 1L)
              profileDetail(sequence = 1L, profileType = "BUILD", profileCode = "MEDIUM")
              profile(sequence = 2L)
              profileDetail(sequence = 2L, profileType = "BUILD", profileCode = "HEAVY")
            }
          }
        }

        webTestClient.upsertProfileDetailsOk("A1234AA", "BUILD", "SMALL")

        repository.runInTransaction {
          val booking = findBooking()
          // The new profile with sequence 1 was updated
          with(booking.profileDetails.first { it.id.sequence == 1L }) {
            assertThat(id.profileType.type).isEqualTo("BUILD")
            assertThat(profileCodeId).isEqualTo("SMALL")
          }
          // The profile with sequence 2 wasn't updated
          with(booking.profileDetails.first { it.id.sequence == 2L }) {
            assertThat(id.profileType.type).isEqualTo("BUILD")
            assertThat(profileCodeId).isEqualTo("HEAVY")
          }
        }
      }

      @Test
      fun `should ignore profile sequence greater than 1 where there are no profile records`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingSequence = 1, bookingBeginDate = yesterday) {
              profileDetail(sequence = 1L, profileType = "BUILD", profileCode = "MEDIUM")
              profileDetail(sequence = 2L, profileType = "BUILD", profileCode = "HEAVY")
            }
          }
        }

        webTestClient.upsertProfileDetailsOk("A1234AA", "BUILD", "SMALL")

        repository.runInTransaction {
          val booking = findBooking()
          // The new profile with sequence 1 was updated
          with(booking.profileDetails.first { it.id.sequence == 1L }) {
            assertThat(id.profileType.type).isEqualTo("BUILD")
            assertThat(profileCodeId).isEqualTo("SMALL")
          }
          // The profile with sequence 2 wasn't updated
          with(booking.profileDetails.first { it.id.sequence == 2L }) {
            assertThat(id.profileType.type).isEqualTo("BUILD")
            assertThat(profileCodeId).isEqualTo("HEAVY")
          }
        }
      }

      @Test
      fun `should only update requested profile type`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingSequence = 1, bookingBeginDate = yesterday) {
              profile()
              profileDetail(profileType = "BUILD", profileCode = "MEDIUM")
              profileDetail(profileType = "SHOESIZE", profileCode = "8.5")
            }
          }
        }

        webTestClient.upsertProfileDetailsOk("A1234AA", "BUILD", "SMALL")

        repository.runInTransaction {
          with(findBooking()) {
            profileDetails.find { it.id.profileType.type == "SHOESIZE" }!!
              .also {
                assertThat(it.profileCodeId).isEqualTo("8.5")
              }
          }
        }
      }

      @Test
      fun `should allow create with null value`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingSequence = 1, bookingBeginDate = yesterday)
          }
        }

        webTestClient.upsertProfileDetailsOk("A1234AA", "BUILD", null)

        repository.runInTransaction {
          with(findBooking().profileDetails.first()) {
            assertThat(id.profileType.type).isEqualTo("BUILD")
            assertThat(profileCode).isNull()
            assertThat(profileCodeId).isNull()
          }
        }
      }

      @Test
      fun `should allow updates with null value`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingSequence = 1, bookingBeginDate = yesterday) {
              profile()
              profileDetail(profileType = "BUILD", profileCode = "MEDIUM")
            }
          }
        }

        webTestClient.upsertProfileDetailsOk("A1234AA", "BUILD", null)

        repository.runInTransaction {
          with(findBooking().profileDetails.first()) {
            assertThat(id.profileType.type).isEqualTo("BUILD")
            assertThat(profileCode).isNull()
            assertThat(profileCodeId).isNull()
          }
        }
      }

      @Test
      fun `should allow update of free text profile codes`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingSequence = 1, bookingBeginDate = yesterday)
          }
        }

        webTestClient.upsertProfileDetailsOk("A1234AA", "SHOESIZE", "8.5")

        repository.runInTransaction {
          with(findBooking().profileDetails.first()) {
            assertThat(id.profileType.type).isEqualTo("SHOESIZE")
            assertThat(profileCode).isNull()
            assertThat(profileCodeId).isEqualTo("8.5")
          }
        }
      }
    }

    @Nested
    inner class Errors {
      @Test
      fun `should reject if no offender`() {
        webTestClient.upsertProfileDetails("A1234AA", "BUILD", "SMALL")
          .expectStatus().isNotFound
          .expectErrorContaining("A1234AA")
      }

      @Test
      fun `should reject if no bookings`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA")
        }

        webTestClient.upsertProfileDetails("A1234AA", "BUILD", "SMALL")
          .expectStatus().isNotFound
          .expectErrorContaining("A1234AA")
      }

      @Test
      fun `should reject if unknown profile type`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking()
          }
        }

        webTestClient.upsertProfileDetails("A1234AA", "UNKNOWN_TYPE", "UNKNOWN_CODE")
          .expectStatus().isBadRequest
          .expectErrorContaining("UNKNOWN_TYPE")
      }

      @Test
      fun `should reject if unknown profile code`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking()
          }
        }

        webTestClient.upsertProfileDetails("A1234AA", "BUILD", "UNKNOWN_CODE")
          .expectStatus().isBadRequest
          .expectErrorContaining("UNKNOWN_CODE")
      }
    }

    private fun findBooking(bookingId: Long = booking.bookingId) = offenderBookingRepository.findByIdOrNull(bookingId)!!

    fun WebTestClient.upsertProfileDetailsOk(offenderNo: String, profileType: String, profileCode: String?) = upsertProfileDetails(offenderNo, profileType, profileCode)
      .expectStatus().isOk
      .expectBody<UpsertProfileDetailsResponse>()

    fun WebTestClient.upsertProfileDetails(offenderNo: String, profileType: String, profileCode: String?) = put().uri("/prisoners/$offenderNo/profile-details")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
      .bodyValue(UpsertProfileDetailsRequest(profileType, profileCode))
      .exchange()

    fun WebTestClient.ResponseSpec.expectErrorContaining(partialMessage: String) = expectBody<ErrorResponse>()
      .consumeWith {
        assertThat(it.responseBody!!.userMessage).contains(partialMessage)
      }
  }
}
