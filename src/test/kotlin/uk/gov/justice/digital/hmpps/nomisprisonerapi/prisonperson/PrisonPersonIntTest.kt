package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson

import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderPhysicalAttributeId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderPhysicalAttributesRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.api.PrisonerPhysicalAttributesResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.api.UpsertPhysicalAttributesRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.api.UpsertPhysicalAttributesResponse
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class PrisonPersonIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var repository: Repository

  @Autowired
  private lateinit var physicalAttributesRepository: OffenderPhysicalAttributesRepository

  @AfterEach
  fun cleanUp() {
    repository.deleteOffenders()
  }

  @Nested
  @DisplayName("GET /prisoners/{offenderNo}/physical-attributes")
  inner class GetPhysicalAttributes {
    lateinit var booking: OffenderBooking

    @Nested
    inner class Security {
      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/prisoners/A1234AA/physical-attributes")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/prisoners/A1234AA/physical-attributes")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/prisoners/A1234AA/physical-attributes")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `not found if prisoner does not exist`() {
        webTestClient.get().uri("/prisoners/A1234AA/physical-attributes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISON_PERSON")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      // The DB column is a DATE type so truncates milliseconds, but bizarrely H2 uses half-up rounding so I have to emulate here or tests fail
      val today = LocalDateTime.now().roundToNearestSecond()
      val yesterday = today.minusDays(1)

      @Test
      fun `should return physical attributes`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingBeginDate = yesterday) {
              physicalAttributes(
                heightCentimetres = 180,
                weightKilograms = 81,
              )
            }
          }
        }

        webTestClient.getPhysicalAttributesOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(offenderNo).isEqualTo("A1234AA")
              assertThat(bookings).extracting("bookingId", "startDateTime", "endDateTime", "latestBooking")
                .containsExactly(tuple(booking.bookingId, booking.bookingBeginDate, booking.bookingEndDate, true))
              assertThat(bookings[0].physicalAttributes)
                .extracting("attributeSequence", "heightCentimetres", "weightKilograms")
                .containsExactly(tuple(1L, 180, 81))
            }
          }
      }

      @Test
      fun `should return empty list if no bookings`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
          }
        }

        webTestClient.getPhysicalAttributesOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(offenderNo).isEqualTo("A1234AA")
              assertThat(bookings).isEmpty()
            }
          }
      }

      @Test
      fun `should return empty list if no physical attributes`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking()
          }
        }

        webTestClient.getPhysicalAttributesOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(offenderNo).isEqualTo("A1234AA")
              assertThat(bookings).isEmpty()
            }
          }
      }

      @Test
      fun `should return multiple physical attributes`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking {
              physicalAttributes(180, null, null, weightKilograms = 80, null)
              physicalAttributes(170, null, null, weightKilograms = 70, null)
            }
          }
        }

        webTestClient.getPhysicalAttributesOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(bookings[0].physicalAttributes)
                .extracting("attributeSequence", "heightCentimetres", "weightKilograms")
                .containsExactly(
                  tuple(1L, 180, 80),
                  tuple(2L, 170, 70),
                )
            }
          }
      }

      @Test
      fun `should return physical attributes from old bookings`() {
        lateinit var oldBooking: OffenderBooking
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingSequence = 1, bookingBeginDate = today) {
              physicalAttributes(170, null, null, 70, null)
            }
            oldBooking = booking(bookingSequence = 2, bookingBeginDate = today.minusDays(2)) {
              physicalAttributes(180, null, null, 80, null)
              release(date = yesterday)
            }
          }
        }

        webTestClient.getPhysicalAttributesOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(bookings).extracting("bookingId", "startDateTime", "endDateTime", "latestBooking")
                .containsExactly(
                  tuple(booking.bookingId, today, null, true),
                  tuple(oldBooking.bookingId, today.minusDays(2), yesterday, false),
                )
              assertThat(bookings[0].physicalAttributes)
                .extracting("attributeSequence", "heightCentimetres", "weightKilograms")
                .containsExactly(tuple(1L, 170, 70))
              assertThat(bookings[1].physicalAttributes)
                .extracting("attributeSequence", "heightCentimetres", "weightKilograms")
                .containsExactly(tuple(1L, 180, 80))
            }
          }
      }

      @Test
      fun `should return end date from last release movement`() {
        lateinit var oldBooking: OffenderBooking
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingSequence = 1, bookingBeginDate = today) {
              physicalAttributes()
            }
            oldBooking =
              booking(bookingSequence = 2, bookingBeginDate = today.minusDays(3), bookingEndDate = today.minusDays(2).toLocalDate()) {
                physicalAttributes()
                // Note the release time is after the bookingEndDate - an edge case seen in production data
                release(date = today.minusDays(1))
              }
          }
        }

        webTestClient.getPhysicalAttributesOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(bookings).extracting("bookingId", "endDateTime")
                .containsExactly(
                  tuple(booking.bookingId, null),
                  // For the old booking we return the latest release time rather than the booking end date - because it includes the time but booking end date doesn't
                  tuple(oldBooking.bookingId, today.minusDays(1)),
                )
            }
          }
      }

      @Test
      fun `should return booking end date if can't find a release movement`() {
        lateinit var oldBooking: OffenderBooking
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingSequence = 1, bookingBeginDate = today) {
              physicalAttributes()
            }
            oldBooking =
              booking(bookingSequence = 2, bookingBeginDate = today.minusDays(3), bookingEndDate = today.minusDays(2).toLocalDate()) {
                // Note there is no release movement added here - an edge case seen in production data
                physicalAttributes()
              }
          }
        }

        webTestClient.getPhysicalAttributesOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(bookings).extracting("bookingId", "endDateTime")
                .containsExactly(
                  tuple(booking.bookingId, null),
                  // For the old booking without a release movement we return the booking end date
                  tuple(oldBooking.bookingId, today.minusDays(2).truncatedTo(ChronoUnit.DAYS)),
                )
            }
          }
      }

      @Test
      fun `should return physical attributes from aliases`() {
        lateinit var aliasBooking: OffenderBooking
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingSequence = 1, bookingBeginDate = today) {
              physicalAttributes(170, null, null, 70, null)
            }
            alias {
              aliasBooking = booking(bookingSequence = 2, bookingBeginDate = today.minusDays(2)) {
                physicalAttributes(180, null, null, 80, null)
                release(date = yesterday)
              }
            }
          }
        }

        webTestClient.getPhysicalAttributesOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(bookings).extracting("bookingId", "startDateTime", "endDateTime", "latestBooking")
                .containsExactly(
                  tuple(booking.bookingId, booking.bookingBeginDate, null, true),
                  tuple(aliasBooking.bookingId, today.minusDays(2), yesterday, false),
                )
              assertThat(bookings[0].physicalAttributes)
                .extracting("heightCentimetres", "weightKilograms")
                .containsExactly(tuple(170, 70))
              assertThat(bookings[1].physicalAttributes)
                .extracting("heightCentimetres", "weightKilograms")
                .containsExactly(tuple(180, 80))
            }
          }
      }

      @Test
      fun `should return physical attributes for multiple physical attributes from multiple bookings`() {
        lateinit var oldBooking: OffenderBooking
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingSequence = 1, bookingBeginDate = yesterday) {
              physicalAttributes(180, null, null, 80, null)
              physicalAttributes(181, null, null, 81, null)
              release(date = today)
            }
            oldBooking = booking(bookingSequence = 2, bookingBeginDate = today.minusDays(2)) {
              physicalAttributes(170, null, null, 70, null)
              physicalAttributes(171, null, null, 71, null)
              release(date = yesterday)
            }
          }
        }

        webTestClient.getPhysicalAttributesOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(bookings).extracting("bookingId", "startDateTime", "endDateTime", "latestBooking")
                .containsExactly(
                  tuple(booking.bookingId, yesterday, today, true),
                  tuple(oldBooking.bookingId, today.minusDays(2), yesterday, false),
                )
              assertThat(bookings[0].physicalAttributes)
                .extracting("attributeSequence", "heightCentimetres", "weightKilograms")
                .containsExactly(tuple(1L, 180, 80), tuple(2L, 181, 81))
              assertThat(bookings[1].physicalAttributes)
                .extracting("attributeSequence", "heightCentimetres", "weightKilograms")
                .containsExactly(tuple(1L, 170, 70), tuple(2L, 171, 71))
            }
          }
      }

      @Test
      fun `should return null if empty attributes`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking {
              physicalAttributes(
                heightCentimetres = null,
                weightKilograms = null,
              )
            }
          }
        }

        webTestClient.getPhysicalAttributesOk("A1234AA")
          .consumeWith {
            assertThat(it.responseBody!!.bookings[0].physicalAttributes)
              .extracting("heightCentimetres", "weightKilograms")
              .containsExactly(tuple(null, null))
          }
      }

      @Test
      fun `should return metric measures if imperial measures are empty`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking {
              physicalAttributes(
                heightCentimetres = 180,
                weightKilograms = 80,
              )
            }
          }
        }

        webTestClient.getPhysicalAttributesOk("A1234AA")
          .consumeWith {
            assertThat(it.responseBody!!.bookings[0].physicalAttributes)
              .extracting("heightCentimetres", "weightKilograms")
              .containsExactly(tuple(180, 80))
          }
      }

      @Test
      fun `should convert from imperial to metric if metric measures are empty`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking {
              physicalAttributes(
                heightFeet = 5,
                heightInches = 10,
                weightPounds = 180,
              )
            }
          }
        }

        webTestClient.getPhysicalAttributesOk("A1234AA")
          .consumeWith {
            assertThat(it.responseBody!!.bookings[0].physicalAttributes)
              .extracting("heightCentimetres", "weightKilograms")
              .containsExactly(tuple(180, 82))
          }
      }

      @Test
      fun `should return metric height if both imperial and metric measures are present`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking {
              physicalAttributes(
                heightCentimetres = 180,
                heightFeet = 5,
                heightInches = 10,
              )
            }
          }
        }

        webTestClient.getPhysicalAttributesOk("A1234AA")
          .consumeWith {
            assertThat(it.responseBody!!.bookings[0].physicalAttributes[0].heightCentimetres)
              .isEqualTo(180)
          }
      }

      @Test
      fun `should convert from imperial weight if both imperial and metric measures are present`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking {
              physicalAttributes(
                weightKilograms = 80,
                weightPounds = 180,
              )
            }
          }
        }

        /*
         * Note this result is different to the weightKilograms on the NOMIS record (80).
         * This is because we know the user entered weightPounds, so we convert that to kilograms.
         * We know the user entered weightPounds because had they entered weightKilograms, weightPounds would be 80/0.45359=176.37, clearly not 180.
         */
        webTestClient.getPhysicalAttributesOk("A1234AA")
          .consumeWith {
            assertThat(it.responseBody!!.bookings[0].physicalAttributes[0].weightKilograms)
              .isEqualTo(82)
          }
      }
    }
  }

  fun WebTestClient.getPhysicalAttributesOk(offenderNo: String) =
    this.get().uri("/prisoners/$offenderNo/physical-attributes")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISON_PERSON")))
      .exchange()
      .expectStatus().isOk
      .expectBody<PrisonerPhysicalAttributesResponse>()

  private fun LocalDateTime.roundToNearestSecond(): LocalDateTime {
    val secondsOnly = this.truncatedTo(ChronoUnit.SECONDS)
    val nanosOnly = this.nano
    val nanosRounded = if (nanosOnly >= 500_000_000) 1 else 0
    return secondsOnly.plusSeconds(nanosRounded.toLong())
  }

  // TODO SDIT-1826 Switch to using the API when it has been written
  @Nested
  @DisplayName("PUT /prisoners/{offenderNo}/physical-attributes")
  inner class UpsertPhysicalAttributes {
    private lateinit var booking: OffenderBooking
    private lateinit var oldBooking: OffenderBooking

    @Nested
    inner class Security {
      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put().uri("/prisoners/A1234AA/physical-attributes")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/prisoners/A1234AA/physical-attributes")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/prisoners/A1234AA/physical-attributes")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `not found if prisoner does not exist`() {
        webTestClient.get().uri("/prisoners/A1234AA/physical-attributes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISON_PERSON")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class Upserts {
      @Test
      fun `should create physical attributes`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking()
          }
        }

        webTestClient.upsertPhysicalAttributesOk("A1234AA", 180, 80)
          .consumeWith {
            assertThat(it.responseBody!!.created).isEqualTo(true)
            assertThat(it.responseBody!!.bookingId).isEqualTo(booking.bookingId)
          }

        with(physicalAttributesRepository.findByIdOrNull(OffenderPhysicalAttributeId(booking, 1))!!) {
          assertThat(heightCentimetres).isEqualTo(180)
          assertThat(heightFeet).isEqualTo(5)
          assertThat(heightInches).isEqualTo(11)
          assertThat(weightKilograms).isEqualTo(80)
          assertThat(weightPounds).isEqualTo(176)
        }
      }

      @Test
      fun `should update physical attributes`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking {
              physicalAttributes(heightCentimetres = 170, weightKilograms = 70)
            }
          }
        }

        webTestClient.upsertPhysicalAttributesOk("A1234AA", 180, 80)
          .consumeWith {
            assertThat(it.responseBody!!.created).isEqualTo(false)
            assertThat(it.responseBody!!.bookingId).isEqualTo(booking.bookingId)
          }

        with(physicalAttributesRepository.findByIdOrNull(OffenderPhysicalAttributeId(booking, 1))!!) {
          assertThat(heightCentimetres).isEqualTo(180)
          assertThat(weightKilograms).isEqualTo(80)
        }
      }

      @Test
      fun `should only update active booking`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            oldBooking = booking(bookingSequence = 2) {
              physicalAttributes(heightCentimetres = 160, weightKilograms = 60)
              release(date = LocalDateTime.now().minusDays(1))
            }
            booking = booking(bookingSequence = 1, bookingBeginDate = LocalDateTime.now()) {
              physicalAttributes(heightCentimetres = 170, weightKilograms = 70)
            }
          }
        }

        webTestClient.upsertPhysicalAttributesOk("A1234AA", 180, 80)
          .consumeWith {
            assertThat(it.responseBody!!.created).isEqualTo(false)
            assertThat(it.responseBody!!.bookingId).isEqualTo(booking.bookingId)
          }

        // the new booking should have been updated
        with(physicalAttributesRepository.findByIdOrNull(OffenderPhysicalAttributeId(booking, 1))!!) {
          assertThat(heightCentimetres).isEqualTo(180)
          assertThat(weightKilograms).isEqualTo(80)
        }
        // the old booking should not have changed
        with(physicalAttributesRepository.findByIdOrNull(OffenderPhysicalAttributeId(oldBooking, 1))!!) {
          assertThat(heightCentimetres).isEqualTo(160)
          assertThat(weightKilograms).isEqualTo(60)
        }
      }

      @Test
      fun `should only update latest booking`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            oldBooking = booking(bookingSequence = 2) {
              physicalAttributes(heightCentimetres = 160, weightKilograms = 60)
              release(date = LocalDateTime.now().minusDays(2))
            }
            booking = booking(bookingSequence = 1, bookingBeginDate = LocalDateTime.now().minusDays(1)) {
              physicalAttributes(heightCentimetres = 170, weightKilograms = 70)
              release(date = LocalDateTime.now())
            }
          }
        }

        webTestClient.upsertPhysicalAttributesOk("A1234AA", 180, 80)
          .consumeWith {
            assertThat(it.responseBody!!.created).isEqualTo(false)
            assertThat(it.responseBody!!.bookingId).isEqualTo(booking.bookingId)
          }

        // the latest booking should have been updated
        with(physicalAttributesRepository.findByIdOrNull(OffenderPhysicalAttributeId(booking, 1))!!) {
          assertThat(heightCentimetres).isEqualTo(180)
          assertThat(weightKilograms).isEqualTo(80)
        }
        // the old booking should not have changed
        with(physicalAttributesRepository.findByIdOrNull(OffenderPhysicalAttributeId(oldBooking, 1))!!) {
          assertThat(heightCentimetres).isEqualTo(160)
          assertThat(weightKilograms).isEqualTo(60)
        }
      }

      @Test
      fun `should reject if no offender`() {
        webTestClient.put().uri("/prisoners/A1234AA/physical-attributes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISON_PERSON")))
          .bodyValue(UpsertPhysicalAttributesRequest(180, 80))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `should reject if no bookings`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA")
        }

        webTestClient.put().uri("/prisoners/A1234AA/physical-attributes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISON_PERSON")))
          .bodyValue(UpsertPhysicalAttributesRequest(180, 80))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `should create physical attributes with null values`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking()
          }
        }

        webTestClient.upsertPhysicalAttributesOk("A1234AA", null, null)
          .consumeWith {
            assertThat(it.responseBody!!.created).isEqualTo(true)
            assertThat(it.responseBody!!.bookingId).isEqualTo(booking.bookingId)
          }

        with(physicalAttributesRepository.findByIdOrNull(OffenderPhysicalAttributeId(booking, 1))!!) {
          assertThat(heightCentimetres).isNull()
          assertThat(heightFeet).isNull()
          assertThat(heightInches).isNull()
          assertThat(weightKilograms).isNull()
          assertThat(weightPounds).isNull()
        }
      }

      @Test
      fun `should update physical attributes with null values`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking {
              physicalAttributes(
                heightCentimetres = 170,
                heightFeet = 5,
                heightInches = 6,
                weightKilograms = 70,
                weightPounds = 160,
              )
            }
          }
        }

        webTestClient.upsertPhysicalAttributesOk("A1234AA", null, null)
          .consumeWith {
            assertThat(it.responseBody!!.created).isEqualTo(false)
            assertThat(it.responseBody!!.bookingId).isEqualTo(booking.bookingId)
          }

        with(physicalAttributesRepository.findByIdOrNull(OffenderPhysicalAttributeId(booking, 1))!!) {
          assertThat(heightCentimetres).isNull()
          assertThat(heightFeet).isNull()
          assertThat(heightInches).isNull()
          assertThat(weightKilograms).isNull()
          assertThat(weightPounds).isNull()
        }
      }
    }

    fun WebTestClient.upsertPhysicalAttributesOk(offenderNo: String, height: Int?, weight: Int?) =
      this.put().uri("/prisoners/$offenderNo/physical-attributes")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISON_PERSON")))
        .bodyValue(UpsertPhysicalAttributesRequest(height, weight))
        .exchange()
        .expectStatus().isOk
        .expectBody<UpsertPhysicalAttributesResponse>()
  }
}
