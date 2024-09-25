package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.profiledetails

import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
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

  // TODO SDIT-2023 Remove this after switching to calling the upsert API
  @Autowired
  private lateinit var service: ProfileDetailsService

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
      fun `should return null profile details`() {
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

  @Nested
  @DisplayName("PUT /prisoners/{offenderNo}/profile-details")
  // TODO SDIT-2023 switch to calling the API once it's available
  inner class UpsertProfileDetails {
    private lateinit var booking: OffenderBooking

    @Nested
    inner class HappyPath {
      @Test
      fun `should create profile details`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingBeginDate = yesterday)
          }
        }

        repository.runInTransaction {
          service.upsertProfileDetails("A1234AA", UpsertProfileDetailsRequest("BUILD", "SMALL"))
            .also {
              assertThat(it.bookingId).isEqualTo(booking.bookingId)
              assertThat(it.created).isTrue()
            }
        }

        repository.runInTransaction {
          with(findBooking().profiles.first()) {
            assertThat(id.sequence).isEqualTo(1)
            assertThat(profileDetails.size).isEqualTo(1)
            with(profileDetails.first()) {
              assertThat(id.profileType.type).isEqualTo("BUILD")
              assertThat(profileCodeId).isEqualTo("SMALL")
            }
          }
        }
      }

      @Test
      fun `should update profile details`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingBeginDate = yesterday) {
              profile {
                detail(profileType = "BUILD", profileCode = "MEDIUM")
              }
            }
          }
        }

        repository.runInTransaction {
          service.upsertProfileDetails("A1234AA", UpsertProfileDetailsRequest("BUILD", "SMALL"))
            .also {
              assertThat(it.bookingId).isEqualTo(booking.bookingId)
              assertThat(it.created).isFalse()
            }
        }

        repository.runInTransaction {
          with(findBooking().profiles.first()) {
            assertThat(id.sequence).isEqualTo(1)
            assertThat(profileDetails.size).isEqualTo(1)
            with(profileDetails.first()) {
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

        service.upsertProfileDetails("A1234AA", UpsertProfileDetailsRequest("BUILD", "SMALL"))

        verify(telemetryClient).trackEvent(
          "profile-details-physical-attributes-created",
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
              profile {
                detail(profileType = "BUILD", profileCode = "MEDIUM")
              }
            }
          }
        }

        service.upsertProfileDetails("A1234AA", UpsertProfileDetailsRequest("BUILD", "SMALL"))

        verify(telemetryClient).trackEvent(
          "profile-details-physical-attributes-updated",
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
              profile {
                detail(profileType = "BUILD", profileCode = "MEDIUM")
              }
            }
            oldBooking = booking(bookingSequence = 2, bookingBeginDate = yesterday, bookingEndDate = yesterday.toLocalDate()) {
              profile {
                detail(profileType = "BUILD", profileCode = "HEAVY")
              }
            }
          }
        }

        repository.runInTransaction {
          service.upsertProfileDetails("A1234AA", UpsertProfileDetailsRequest("BUILD", "SMALL"))
        }

        repository.runInTransaction {
          // The new booking was updated
          with(findBooking().profiles.first()) {
            with(profileDetails.first()) {
              assertThat(id.profileType.type).isEqualTo("BUILD")
              assertThat(profileCodeId).isEqualTo("SMALL")
            }
          }
          // The old booking wasn't updated
          with(findBooking(oldBooking.bookingId).profiles.first()) {
            with(profileDetails.first()) {
              assertThat(id.profileType.type).isEqualTo("BUILD")
              assertThat(profileCodeId).isEqualTo("HEAVY")
            }
          }
        }
      }

      @Test
      fun `should ignore profile sequence greater than 1`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingSequence = 1, bookingBeginDate = yesterday) {
              profile(sequence = 1) {
                detail(profileType = "BUILD", profileCode = "MEDIUM")
              }
              profile(sequence = 2) {
                detail(profileType = "BUILD", profileCode = "HEAVY")
              }
            }
          }
        }

        repository.runInTransaction {
          service.upsertProfileDetails("A1234AA", UpsertProfileDetailsRequest("BUILD", "SMALL"))
        }

        repository.runInTransaction {
          val booking = findBooking()
          // The new profile with sequence 1 was updated
          with(booking.profiles.find { it.id.sequence == 1L }!!.profileDetails.first()) {
            assertThat(id.profileType.type).isEqualTo("BUILD")
            assertThat(profileCodeId).isEqualTo("SMALL")
          }
          // The profile with sequence 2 wasn't updated
          with(booking.profiles.find { it.id.sequence == 2L }!!.profileDetails.first()) {
            assertThat(id.profileType.type).isEqualTo("BUILD")
            assertThat(profileCodeId).isEqualTo("HEAVY")
          }
        }
      }
    }

    @Test
    fun `should only update requested profile type`() {
      nomisDataBuilder.build {
        offender(nomsId = "A1234AA") {
          booking = booking(bookingSequence = 1, bookingBeginDate = yesterday) {
            profile {
              detail(profileType = "BUILD", profileCode = "MEDIUM")
              detail(profileType = "SHOESIZE", profileCode = "8.5")
            }
          }
        }
      }

      repository.runInTransaction {
        service.upsertProfileDetails("A1234AA", UpsertProfileDetailsRequest("BUILD", "SMALL"))
      }

      repository.runInTransaction {
        with(findBooking().profiles.first()) {
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

      repository.runInTransaction {
        service.upsertProfileDetails("A1234AA", UpsertProfileDetailsRequest("BUILD", null))
      }

      repository.runInTransaction {
        val profiles = findBooking().profiles.first()
        with(profiles.profileDetails.first()) {
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
            profile {
              detail(profileType = "BUILD", profileCode = "MEDIUM")
            }
          }
        }

        repository.runInTransaction {
          service.upsertProfileDetails("A1234AA", UpsertProfileDetailsRequest("BUILD", null))
        }

        val profiles = findBooking().profiles.first()
        with(profiles.profileDetails.first()) {
          assertThat(id.profileType.type).isEqualTo("BUILD")
          assertThat(profileCode).isNull()
          assertThat(profileCodeId).isNull()
        }
      }
    }

    @Nested
    inner class Errors {
      @Test
      fun `should reject if no offender`() {
        assertThrows<NotFoundException> {
          service.upsertProfileDetails("A1234AA", UpsertProfileDetailsRequest("BUILD", null))
        }
      }

      @Test
      fun `should reject if no bookings`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA")
        }

        assertThrows<NotFoundException> {
          service.upsertProfileDetails("A1234AA", UpsertProfileDetailsRequest("BUILD", null))
        }.also {
          assertThat(it.message).contains("A1234AA")
        }
      }

      @Test
      fun `should reject if unknown profile type`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking()
          }
        }

        assertThrows<BadDataException> {
          repository.runInTransaction {
            service.upsertProfileDetails("A1234AA", UpsertProfileDetailsRequest("UNKNOWN_TYPE", "UNKNOWN_CODE"))
          }
        }.also {
          assertThat(it.message).contains("UNKNOWN_TYPE")
        }
      }

      @Test
      fun `should reject if unknown profile code`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking()
          }
        }

        assertThrows<BadDataException> {
          service.upsertProfileDetails("A1234AA", UpsertProfileDetailsRequest("BUILD", "UNKNOWN_CODE"))
        }.also {
          assertThat(it.message).contains("UNKNOWN_CODE")
        }
      }
    }

    private fun findBooking(bookingId: Long = booking.bookingId) = offenderBookingRepository.findByIdOrNull(bookingId)!!
  }
}
