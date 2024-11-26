package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.identifyingmarks

import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import org.assertj.core.api.Assertions.tuple
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderExternalMovement
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.roundToNearestSecond
import java.time.LocalDate
import java.time.LocalDateTime

class OffenderIdentifyingMarksIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var offenderBookingRepository: OffenderBookingRepository

  @Autowired
  private lateinit var repository: Repository

  @AfterEach
  fun cleanup() {
    repository.offenderRepository.deleteAll()
  }

  // Temporary tests to check the JPA mappings - to be removed when real tests are available
  @DisplayName("Offender Images JPA tests ")
  @Nested
  inner class TestJpa {
    @Test
    fun `should save and load identifying marks offender images`() {
      lateinit var booking: OffenderBooking
      nomisDataBuilder.build {
        offender {
          booking = booking {
            identifyingMark(
              bodyPartCode = "HEAD",
              markTypeCode = "TAT",
              sideCode = "L",
              partOrientationCode = "FACE",
              commentText = "Head tattoo left facing",
            ) {
              image(
                captureDateTime = LocalDateTime.now(),
                fullSizeImage = byteArrayOf(1, 2, 3),
                thumbnailImage = byteArrayOf(4, 5, 6),
                active = true,
                imageSourceCode = "FILE",
                orientationTypeCode = "HEAD",
                imageViewTypeCode = "TAT",
              )
            }
          }
        }
      }

      repository.runInTransaction {
        val saved = offenderBookingRepository.findByIdOrNull(booking.bookingId)!!
        with(saved.identifyingMarks.first()) {
          assertThat(id.offenderBooking.bookingId).isEqualTo(booking.bookingId)
          assertThat(id.idMarkSequence).isEqualTo(1)
          assertThat(bodyPart.code).isEqualTo("HEAD")
          assertThat(markType.code).isEqualTo("TAT")
          assertThat(side?.code).isEqualTo("L")
          assertThat(partOrientation?.code).isEqualTo("FACE")
          assertThat(commentText).isEqualTo("Head tattoo left facing")
          assertThat(images).hasSize(1)
        }
        with(saved.identifyingMarks.first().images.first()) {
          assertThat(captureDateTime.toLocalDate()).isEqualTo(LocalDate.now())
          assertThat(fullSizeImage).containsExactly(1, 2, 3)
          assertThat(thumbnailImage).containsExactly(4, 5, 6)
          assertThat(active).isTrue()
          assertThat(imageSource.code).isEqualTo("FILE")
          assertThat(orientationType.code).isEqualTo("HEAD")
          assertThat(imageViewType.code).isEqualTo("TAT")
          assertThat(createDatetime.toLocalDate()).isEqualTo(LocalDate.now())
          assertThat(createUserId).isEqualTo("SA")
        }
      }
    }

    @Test
    fun `should save and load offender booking offender images`() {
      lateinit var booking: OffenderBooking
      nomisDataBuilder.build {
        offender {
          booking = booking {
            image(
              captureDateTime = LocalDateTime.now(),
              fullSizeImage = byteArrayOf(7, 8, 9),
              thumbnailImage = byteArrayOf(10, 11, 12),
              active = false,
              imageSourceCode = "WEB",
            )
          }
        }
      }

      repository.runInTransaction {
        val saved = offenderBookingRepository.findByIdOrNull(booking.bookingId)!!
        with(saved.images.first()) {
          assertThat(captureDateTime.toLocalDate()).isEqualTo(LocalDate.now())
          assertThat(fullSizeImage).containsExactly(7, 8, 9)
          assertThat(thumbnailImage).containsExactly(10, 11, 12)
          assertThat(active).isFalse()
          assertThat(imageSource.code).isEqualTo("WEB")
          assertThat(orientationTypeCode).isEqualTo("FRONT")
          assertThat(imageViewTypeCode).isEqualTo("FACE")
          assertThat(createDatetime.toLocalDate()).isEqualTo(LocalDate.now())
          assertThat(createUserId).isEqualTo("SA")
        }
      }
    }
  }

  @DisplayName("GET /bookings/{bookingId}/identifying-marks")
  @Nested
  inner class GetBookingIdentifyingMarks {
    lateinit var booking: OffenderBooking

    @Nested
    inner class Security {
      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/bookings/123456/identifying-marks")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/bookings/123456/identifying-marks")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/bookings/123456/identifying-marks")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `not found if booking does not exist`() {
        webTestClient.get().uri("/bookings/123456/identifying-marks")
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
      fun `should return identifying marks`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingBeginDate = yesterday) {
              identifyingMark()
            }
          }
        }

        webTestClient.getIdentifyingMarksOk(booking.bookingId)
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(bookingId).isEqualTo(booking.bookingId)
              assertThat(startDateTime).isEqualTo(yesterday)
              assertThat(endDateTime).isNull()
              assertThat(latestBooking).isTrue()
              assertThat(identifyingMarks.size).isEqualTo(1)
              with(identifyingMarks.first()) {
                assertThat(idMarksSeq).isEqualTo(1)
                assertThat(bodyPartCode).isEqualTo("HEAD")
                assertThat(markTypeCode).isEqualTo("TAT")
                assertThat(sideCode).isEqualTo("L")
                assertThat(partOrientationCode).isEqualTo("FACE")
                assertThat(commentText).isEqualTo("head tattoo left front")
                assertThat(imageIds).isEmpty()
                assertThat(createDateTime.toLocalDate()).isEqualTo(today.toLocalDate())
                assertThat(createdBy).isEqualTo("SA")
                assertThat(modifiedDateTime).isNull()
                assertThat(modifiedBy).isNull()
              }
            }
          }
      }

      @Test
      fun `should return empty list if no marks`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingBeginDate = yesterday)
          }
        }

        webTestClient.getIdentifyingMarksOk(booking.bookingId)
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(identifyingMarks.size).isEqualTo(0)
            }
          }
      }

      @Test
      fun `should return image ids`() {
        val savedImageIds = mutableListOf<Long>()
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingBeginDate = yesterday) {
              identifyingMark {
                savedImageIds += image(active = true).id
                savedImageIds += image(active = false).id
              }
            }
          }
        }

        webTestClient.getIdentifyingMarksOk(booking.bookingId)
          .consumeWith {
            with(it.responseBody!!.identifyingMarks.first()) {
              assertThat(this.imageIds).containsExactlyInAnyOrder(*savedImageIds.toTypedArray())
            }
          }
      }

      @Test
      fun `should return multiple marks`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingBeginDate = yesterday) {
              identifyingMark(sequence = 1, bodyPartCode = "HEAD")
              identifyingMark(sequence = 2, bodyPartCode = "FACE")
            }
          }
        }

        webTestClient.getIdentifyingMarksOk(booking.bookingId)
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(identifyingMarks)
                .extracting("idMarksSeq", "bodyPartCode")
                .containsExactlyInAnyOrder(
                  tuple(1L, "HEAD"),
                  tuple(2L, "FACE"),
                )
            }
          }
      }

      @Test
      fun `should return end date from last release movement`() {
        lateinit var releaseMovement: OffenderExternalMovement
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(
              active = false,
              bookingSequence = 2,
              bookingBeginDate = today.minusDays(3),
              bookingEndDate = today.minusDays(2).toLocalDate(),
            ) {
              identifyingMark()
              releaseMovement = release(date = today.minusDays(2))
            }
          }
        }

        webTestClient.getIdentifyingMarksOk(booking.bookingId)
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(this.startDateTime).isEqualTo(booking.bookingBeginDate)
              assertThat(this.endDateTime).isEqualTo(releaseMovement.movementTime)
            }
          }
      }

      @Test
      fun `should return end date from last release movement if booking end date is null and booking inactive`() {
        lateinit var releaseMovement: OffenderExternalMovement
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(
              active = false,
              bookingSequence = 2,
              bookingBeginDate = today.minusDays(3),
            ) {
              identifyingMark()
              releaseMovement = release(date = today.minusDays(1))
            }.apply {
              bookingEndDate = null
            }
          }
        }

        webTestClient.getIdentifyingMarksOk(booking.bookingId)
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(this.startDateTime).isEqualTo(booking.bookingBeginDate)
              assertThat(this.endDateTime).isEqualTo(releaseMovement.movementTime)
            }
          }
      }
    }

    fun WebTestClient.getIdentifyingMarksOk(bookingId: Long) =
      this.get().uri("/bookings/$bookingId/identifying-marks")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISON_PERSON")))
        .exchange()
        .expectStatus().isOk
        .expectBody<BookingIdentifyingMarksResponse>()
  }
}
