package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.identifyingmarks

import org.assertj.core.api.Assertions.assertThat
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBookingImage
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderIdentifyingMark
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderIdentifyingMarkImage
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.roundToNearestSecond
import java.time.LocalDateTime

class IdentifyingMarkImagesIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var repository: Repository

  @AfterEach
  fun cleanup() {
    repository.offenderRepository.deleteAll()
  }

  @DisplayName("GET /identifying-marks/images/{imageId}/details")
  @Nested
  inner class GetIdentifyingMarkImageDetails {
    @Nested
    inner class Security {
      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/identifying-marks/images/123456/details")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/identifying-marks/images/123456/details")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/identifying-marks/images/123456/details")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `not found if image does not exist`() {
        webTestClient.get().uri("/identifying-marks/images/123456/details")
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
      fun `should return an image`() {
        lateinit var booking: OffenderBooking
        lateinit var identifyingMark: OffenderIdentifyingMark
        lateinit var image: OffenderIdentifyingMarkImage
        nomisDataBuilder.build {
          offender {
            booking = booking {
              identifyingMark = identifyingMark {
                image = image(
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

        webTestClient.getIdentifyingMarkImageDetailsOk(image.id)
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(imageId).isEqualTo(image.id)
              assertThat(bookingId).isEqualTo(booking.bookingId)
              assertThat(idMarksSeq).isEqualTo(identifyingMark.id.idMarkSequence)
              assertThat(captureDateTime.toLocalDate()).isEqualTo(today.toLocalDate())
              assertThat(bodyPartCode).isEqualTo("HEAD")
              assertThat(markTypeCode).isEqualTo("TAT")
              assertThat(default).isTrue
              assertThat(imageExists).isTrue
              assertThat(imageSourceCode).isEqualTo("FILE")
              assertThat(createDateTime.toLocalDate()).isEqualTo(today.toLocalDate())
              assertThat(createdBy).isEqualTo("SA")
              assertThat(modifiedDateTime).isNull()
              assertThat(modifiedBy).isNull()
            }
          }
      }

      @Test
      fun `should return correct image where there are multiple for the identifying mark`() {
        lateinit var image: OffenderIdentifyingMarkImage
        nomisDataBuilder.build {
          offender {
            booking {
              identifyingMark {
                image(captureDateTime = today, active = true)
                image = image(captureDateTime = today.minusDays(1), active = false)
                image(captureDateTime = today.minusDays(2), active = false)
              }
            }
          }
        }

        webTestClient.getIdentifyingMarkImageDetailsOk(image.id)
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(captureDateTime.toLocalDate()).isEqualTo(today.minusDays(1).toLocalDate())
              assertThat(default).isFalse()
            }
          }
      }

      @Test
      fun `should indicate if the image data doesn't exist`() {
        lateinit var image: OffenderIdentifyingMarkImage
        nomisDataBuilder.build {
          offender {
            booking {
              identifyingMark {
                image = image(fullSizeImage = null)
              }
            }
          }
        }

        webTestClient.getIdentifyingMarkImageDetailsOk(image.id)
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(imageExists).isFalse()
            }
          }
      }
    }

    @Nested
    inner class Errors {
      @Test
      fun `should return not found if the image is not for an identifying mark`() {
        lateinit var image: OffenderBookingImage
        nomisDataBuilder.build {
          offender {
            booking {
              image = image()
            }
          }
        }

        webTestClient.get().uri("/identifying-marks/images/${image.id}/details")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISON_PERSON")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    fun WebTestClient.getIdentifyingMarkImageDetailsOk(imageId: Long) =
      this.get().uri("/identifying-marks/images/{imageId}/details", imageId)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISON_PERSON")))
        .exchange()
        .expectStatus().isOk
        .expectBody<IdentifyingMarkImageDetailsResponse>()
  }
}
