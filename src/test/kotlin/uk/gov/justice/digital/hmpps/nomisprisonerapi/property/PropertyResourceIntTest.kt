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
import org.springframework.web.reactive.function.BodyInserters.fromValue
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.expectBodyResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderPropertyContainer
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PropertyContainerCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderPropertyContainerRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.SECONDS

private const val OFFENDER_NO = "A1111AA"

class PropertyResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var offenderPropertyContainerRepository: OffenderPropertyContainerRepository

  private lateinit var location1: AgencyInternalLocation
  private lateinit var location2: AgencyInternalLocation
  private lateinit var booking: OffenderBooking
  private lateinit var container1: OffenderPropertyContainer
  private lateinit var container2: OffenderPropertyContainer
  private lateinit var container3: OffenderPropertyContainer

  @AfterEach
  fun deleteData() {
    repository.deleteAllPrisonerProperty()
    repository.deleteOffenders()
    repository.deleteAgencyInternalLocationById(location1.locationId)
  }

  @DisplayName("POST /property-containers")
  @Nested
  inner class CreatePropertyContainer {
    @BeforeEach
    fun init() {
      nomisDataBuilder.build {
        location1 = agencyInternalLocation(
          locationCode = "SYI-001",
          locationType = "BOX",
          prisonId = "SYI",
        )
        offender(nomsId = OFFENDER_NO) { booking = booking() }
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/property-containers")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(fromValue(validMinimalCreateJsonRequest(OFFENDER_NO)))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/property-containers")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(fromValue(validMinimalCreateJsonRequest(OFFENDER_NO)))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/property-containers")
          .contentType(MediaType.APPLICATION_JSON)
          .body(fromValue(validMinimalCreateJsonRequest(OFFENDER_NO)))
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `offender not found`() {
        webTestClient.post().uri("/property-containers")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(fromValue(validMinimalCreateJsonRequest("A9999ZZ")))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("userMessage").isEqualTo("Bad request: Latest booking id for A9999ZZ not found")
      }

      @Test
      fun `location not found`() {
        webTestClient.post().uri("/property-containers")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(fromValue(validFullCreateJsonRequest(OFFENDER_NO, 9999)))
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
          .body(fromValue("""{ ${requiredFields(OFFENDER_NO, prisonId = "DUFF")}" }"""))
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
                "offenderNo": "$OFFENDER_NO",
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
          .body(fromValue(validFullCreateJsonRequest(OFFENDER_NO, location1.locationId)))
          .exchange()
          .expectBodyResponse<CreatePropertyResponse>()

        nomisDataBuilder.runInTransaction {
          offenderPropertyContainerRepository.findByIdOrNull(created.propertyContainerId)!!
            .apply {
              assertThat(offenderBooking.bookingId).isEqualTo(booking.bookingId)
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
          .body(fromValue(validMinimalCreateJsonRequest(OFFENDER_NO)))
          .exchange()
          .expectBodyResponse<CreatePropertyResponse>()

        nomisDataBuilder.runInTransaction {
          offenderPropertyContainerRepository.findByIdOrNull(created.propertyContainerId)!!
            .apply {
              assertThat(offenderBooking.bookingId).isEqualTo(booking.bookingId)
              assertThat(agencyLocation.id).isEqualTo("SYI")
              assertThat(active).isTrue()
              assertThat(sealMark).isEqualTo("S12345")
              assertThat(containerCode).isEqualTo(PropertyContainerCode.CO)
            }
        }
      }
    }
  }

  @DisplayName("PUT /property-containers/{id}")
  @Nested
  inner class UpdatePropertyContainer {
    @BeforeEach
    fun init() {
      nomisDataBuilder.build {
        location1 = agencyInternalLocation(
          locationCode = "SYI-001",
          locationType = "BOX",
          prisonId = "SYI",
        )
        location2 = agencyInternalLocation(
          locationCode = "SYI-002",
          locationType = "BOX",
          prisonId = "SYI",
        )
        offender(nomsId = OFFENDER_NO) {
          booking = booking {
            container1 = property()
          }
        }
      }
    }

    @AfterEach
    fun deleteData() {
      repository.deleteAgencyInternalLocationById(location2.locationId)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.put().uri("/property-containers/99")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(fromValue(validUpdateJsonRequest(1234)))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put().uri("/property-containers/99")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(fromValue(validUpdateJsonRequest(1234)))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put().uri("/property-containers/99")
          .contentType(MediaType.APPLICATION_JSON)
          .body(fromValue(validUpdateJsonRequest(1234)))
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `location not found`() {
        webTestClient.put().uri("/property-containers/${container1.propertyContainerId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(fromValue(validUpdateJsonRequest(99)))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("userMessage").value<String> {
            assertThat(it).contains("No location with id 99 found")
          }
      }

      @Test
      fun `invalid container code`() {
        webTestClient.put().uri("/property-containers/${container1.propertyContainerId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(fromValue("""{ "containerCode": "INVALID" }"""))
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
      fun `can update a property container`() {
        webTestClient.put().uri("/property-containers/${container1.propertyContainerId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(fromValue(validUpdateJsonRequest(location2.locationId)))
          .exchange()
          .expectStatus().isOk

        nomisDataBuilder.runInTransaction {
          offenderPropertyContainerRepository.findByIdOrNull(container1.propertyContainerId)!!
            .apply {
              assertThat(agencyInternalLocation?.locationId).isEqualTo(location2.locationId)
              assertThat(sealMark).isEqualTo("SEAL4567")
              assertThat(containerCode).isEqualTo(PropertyContainerCode.BRA)
              assertThat(proposedDisposalDate).isEqualTo("2027-11-02")
            }
        }
      }
    }
  }

  @DisplayName("GET /property-containers/{id}")
  @Nested
  inner class GetPropertyContainer {

    @BeforeEach
    fun init() {
      nomisDataBuilder.build {
        location1 = agencyInternalLocation(
          locationCode = "SYI-001",
          locationType = "BOX",
          prisonId = "SYI",
        )
        offender(nomsId = OFFENDER_NO) {
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
      repository.deleteAgencyInternalLocationById(location1.locationId)
    }

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
        webTestClient.get().uri("/property-containers/${container2.propertyContainerId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse<PropertyContainerGetResponse>()
          .apply {
            assertThat(containerId).isEqualTo(container2.propertyContainerId)
            assertThat(offenderNo).isEqualTo(OFFENDER_NO)
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
            assertThat(updatedDateTime).isNull()
            assertThat(updatedBy).isNull()
          }
      }
    }
  }

  @Nested
  inner class GetPropertyContainerIdsByFilterRequest {

    @BeforeEach
    fun init() {
      nomisDataBuilder.build {
        location1 = agencyInternalLocation(
          locationCode = "SYI-001",
          locationType = "BOX",
          prisonId = "SYI",
        )
        offender(nomsId = OFFENDER_NO) {
          booking = booking(agencyLocationId = "BXI") {
            container1 = property()
            container2 = property(
              prisonId = "SYI",
              internalLocationId = location1.locationId,
              expiryDate = LocalDate.parse("2026-06-01"),
              proposedDisposalDate = LocalDate.parse("2026-10-01"),
            )
            container3 = property()
          }
        }
        offender(nomsId = OFFENDER_NO) {
          booking = booking(agencyLocationId = "LEI") {
            property()
          }
        }
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `get property-containers prevents access without appropriate role`() {
        assertThat(
          webTestClient.get().uri {
            it.path("/property-containers/ids")
              .queryParam("prisonIds", "MDI")
              .build()
          }
            .headers(setAuthorisation(roles = listOf("ROLE_BLA")))
            .exchange()
            .expectStatus().isForbidden,
        )
      }

      @Test
      fun `get property-containers prevents access without authorization`() {
        assertThat(
          webTestClient.get().uri("/property-containers/ids")
            .exchange()
            .expectStatus().isUnauthorized,
        )
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `get all ids - prisons not specified`() {
        webTestClient.get()
          .uri("/property-containers/ids")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.numberOfElements").isEqualTo(4)
      }

      @Test
      fun `get property-containers issued within given prisons`() {
        webTestClient.get().uri {
          it.path("/property-containers/ids")
            .queryParam("prisonIds", "MDI", "SYI", "BXI")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.content[0].containerId").isEqualTo(container1.propertyContainerId)
          .jsonPath("$.content[1].containerId").isEqualTo(container2.propertyContainerId)
          .jsonPath("$.content[2].containerId").isEqualTo(container3.propertyContainerId)
          .jsonPath("$.numberOfElements").isEqualTo(3)
      }

      @Test
      fun `can request a different page size`() {
        webTestClient.get().uri {
          it.path("/property-containers/ids")
            .queryParam("prisonIds", "MDI", "SYI", "BXI")
            .queryParam("size", "2")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(3)
          .jsonPath("numberOfElements").isEqualTo(2)
          .jsonPath("number").isEqualTo(0)
          .jsonPath("totalPages").isEqualTo(2)
          .jsonPath("size").isEqualTo(2)
          .jsonPath("$.content[0].containerId").isEqualTo(container1.propertyContainerId)
          .jsonPath("$.content[1].containerId").isEqualTo(container2.propertyContainerId)
      }

      @Test
      fun `can request a different page`() {
        webTestClient.get().uri {
          it.path("/property-containers/ids")
            .queryParam("prisonIds", "MDI", "SYI", "BXI")
            .queryParam("size", "2")
            .queryParam("page", "1")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(3)
          .jsonPath("numberOfElements").isEqualTo(1)
          .jsonPath("number").isEqualTo(1)
          .jsonPath("totalPages").isEqualTo(2)
          .jsonPath("size").isEqualTo(2)
          .jsonPath("$.content[0].containerId").isEqualTo(container3.propertyContainerId)
      }
    }
  }
}

fun requiredFields(offenderNo: String, prisonId: String = "SYI") =
  """
    "offenderNo": "$offenderNo",
    "prisonId": "$prisonId",
    "active": true,
    "sealMark": "S12345",
    "containerCode": "CO"
  """.trimIndent()

fun validMinimalCreateJsonRequest(offenderNo: String): String = "{ ${requiredFields(offenderNo)} }"

fun validFullCreateJsonRequest(offenderNo: String, internalLocationId: Long): String =
  """
   { ${requiredFields(offenderNo)},
    "internalLocationId": $internalLocationId,
    "proposedDisposalDate": "2026-10-01",
    "expiryDate": "2026-06-01"
   }
  """.trimIndent()

fun validUpdateJsonRequest(internalLocationId: Long): String =
  """
   {
     "internalLocationId": $internalLocationId,
     "proposedDisposalDate": "2027-11-02",
     "sealMark": "SEAL4567",
     "containerCode": "BRA"
   }
  """.trimIndent()
