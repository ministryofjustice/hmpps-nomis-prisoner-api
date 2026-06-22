package uk.gov.justice.digital.hmpps.nomisprisonerapi.property

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters.fromValue
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderPropertyContainer
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PropertyContainerCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderPropertyContainerRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.SECONDS

class PropertyResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var offenderPropertyContainerRepository: OffenderPropertyContainerRepository

  private lateinit var location1: AgencyInternalLocation
  private lateinit var booking: OffenderBooking
  private lateinit var container1: OffenderPropertyContainer
  private lateinit var container2: OffenderPropertyContainer

  @BeforeEach
  fun init() {
    nomisDataBuilder.build {
      location1 = agencyInternalLocation(
        locationCode = "SYI-001",
        locationType = "BOX",
        prisonId = "SYI",
      )
      offender(nomsId = "A1111AA") {
        booking = booking {
          container1 = property()
          container2 = property(
            prisonId = "SYI",
            internalLocationId = location1.locationId,
            expiryDate = LocalDate.parse("2026-06-01"),
            proposedDisposalDate = LocalDate.parse("2026-10-01"),
          )
        }
      }
    }
  }

  @AfterEach
  internal fun deleteData() {
    repository.deleteAllPrisonerProperty()
    repository.deleteOffenders()
    repository.deleteAgencyInternalLocationById(location1.locationId)
  }

  @DisplayName("POST /property-containers")
  @Nested
  inner class CreatePropertyContainer {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/property-containers")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(fromValue(validMinimalCreateJsonRequest(1234)))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/property-containers")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(fromValue(validMinimalCreateJsonRequest(1234)))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/property-containers")
          .contentType(MediaType.APPLICATION_JSON)
          .body(fromValue(validMinimalCreateJsonRequest(1234)))
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `no booking`() {
        webTestClient.post().uri("/property-containers")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(fromValue(validMinimalCreateJsonRequest(999)))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("userMessage").isEqualTo("Bad request: Booking id 999 not found")
      }

      @Test
      fun `location not found`() {
        webTestClient.post().uri("/property-containers")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            fromValue(validFullCreateJsonRequest(booking.bookingId, 9999)),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("userMessage").value<String> {
            assertThat(it).contains("No location with id 9999 found")
          }
      }

      @Test
      fun `invalid prison`() {
        webTestClient.post().uri("/property-containers")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            fromValue(
              """{ ${requiredFields(bookingId = booking.bookingId, prisonId = "DUFF")}" }""",
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("userMessage").value<String> { it.contains("Prison DUFF not found") }
      }

      @Test
      fun `invalid container code`() {
        webTestClient.post().uri("/property-containers")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            fromValue(
              """{
                "bookingId": ${booking.bookingId},
                "prisonId": "SYI",
                "active": true,
                "sealMark": "S12345",
                "containerCode": "INVALID",
                }
              """,
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("userMessage").value<String> {
            assertThat(it).contains(
              "Cannot deserialize value of type `uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PropertyContainerCode` from String \"INVALID\": not one of the values accepted for Enum class",
            )
          }
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `can create a property container with full data`() {
        val created = webTestClient.post().uri("/property-containers")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(fromValue(validFullCreateJsonRequest(booking.bookingId, location1.locationId)))
          .exchange()
          .expectStatus().isOk
          .expectBody<CreatePropertyResponse>()
          .returnResult()
          .responseBody!!

        nomisDataBuilder.runInTransaction {
          val data = offenderPropertyContainerRepository.findByIdOrNull(created.propertyContainerId)

          with(data!!) {
            assertThat(this.offenderBooking.bookingId).isEqualTo(booking.bookingId)
            assertThat(agencyInternalLocation?.locationId).isEqualTo(location1.locationId)
            assertThat(agencyLocation.id).isEqualTo("SYI")
            assertThat(active).isTrue()
            assertThat(sealMark).isEqualTo("S12345")
            assertThat(containerCode).isEqualTo(PropertyContainerCode.CO)
            assertThat(proposedDisposalDate).isEqualTo("2026-10-01")
            assertThat(expiryDate).isEqualTo("2026-06-01")
          }
        }
      }

      @Test
      fun `can create a property container with minimal data`() {
        val created = webTestClient.post().uri("/property-containers")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(fromValue(validMinimalCreateJsonRequest(booking.bookingId)))
          .exchange()
          .expectStatus().isOk
          .expectBody<CreatePropertyResponse>()
          .returnResult()
          .responseBody!!

        nomisDataBuilder.runInTransaction {
          val data = offenderPropertyContainerRepository.findByIdOrNull(
            created.propertyContainerId,
          )

          with(data!!) {
            assertThat(this.offenderBooking.bookingId).isEqualTo(booking.bookingId)
            assertThat(agencyLocation.id).isEqualTo("SYI")
            assertThat(active).isTrue()
            assertThat(sealMark).isEqualTo("S12345")
            assertThat(containerCode).isEqualTo(PropertyContainerCode.CO)
          }
        }
      }
    }
  }

  @DisplayName("GET /property-containers/{id}")
  @Nested
  inner class GetPropertyContainer {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/property-containers/1")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/property-containers/1")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/property-containers/1")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `id doesnt exist`() {
        webTestClient.get().uri("/property-containers/9999")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("userMessage").value<String> {
            assertThat(it).contains("No property container with id 9999")
          }
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `can get a property container with full data`() {
        val data = webTestClient.get().uri("/property-containers/${container2.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody<PropertyContainerGetDto>()
          .returnResult()
          .responseBody!!

        with(data) {
          assertThat(containerId).isEqualTo(container2.id)
          assertThat(offenderNo).isEqualTo("A1111AA")
          assertThat(bookingId).isEqualTo(booking.bookingId)
          assertThat(internalLocationId).isEqualTo(location1.locationId)
          assertThat(prisonId).isEqualTo("SYI")
          assertThat(active).isTrue()
          assertThat(sealMark).isEqualTo("SEAL1234")
          assertThat(containerCode).isEqualTo(PropertyContainerCode.BRA)
          assertThat(proposedDisposalDate).isEqualTo("2026-10-01")
          assertThat(expiryDate).isEqualTo("2026-06-01")
          assertThat(createdDateTime).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
          assertThat(createdBy).isEqualTo("SA")
        }
      }
    }
  }
}

fun requiredFields(bookingId: Long, prisonId: String = "SYI") =
  """
    "bookingId": $bookingId,
    "prisonId": "$prisonId",
    "active": true,
    "sealMark": "S12345",
    "containerCode": "CO"
  """.trimIndent()

fun validMinimalCreateJsonRequest(bookingId: Long): String = "{ ${requiredFields(bookingId)} }"

fun validFullCreateJsonRequest(bookingId: Long, internalLocationId: Long): String =
  """
   { ${requiredFields(bookingId) },
    "internalLocationId": $internalLocationId,
    "proposedDisposalDate": "2026-10-01",
    "expiryDate": "2026-06-01"
   }
  """.trimIndent()
