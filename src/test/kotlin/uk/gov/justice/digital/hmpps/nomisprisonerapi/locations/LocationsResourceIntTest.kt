package uk.gov.justice.digital.hmpps.nomisprisonerapi.locations

import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation

class LocationsResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var repository: Repository

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder


  @Nested
  inner class CreateLocation {

    private val createLocationRequest: () -> CreateLocationRequest = {
      CreateLocationRequest(
        certified = true,
        locationType = "LAND",
        prisonId = "MDI",
        parentLocationId = -1L,
        operationalCapacity = 25,
        cnaCapacity = 20,
        userDescription = "A new landing on 'A' Wing",
        locationCode = "5",
        capacity = 30,
        listSequence = 1,
        comment = "this is a test!",
      )
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/locations")
        .body(BodyInserters.fromValue(createLocationRequest()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/locations")
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(createLocationRequest()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.post().uri("/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .body(BodyInserters.fromValue(createLocationRequest()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `agency not found`() {
      webTestClient.post().uri("/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_LOCATIONS")))
        .body(BodyInserters.fromValue(createLocationRequest().copy(prisonId = "XXX")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Agency with id=XXX does not exist")
        }
    }

    @Test
    fun `parent location not found`() {
      webTestClient.post().uri("/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_LOCATIONS")))
        .body(BodyInserters.fromValue(createLocationRequest().copy(parentLocationId = -9999)))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Parent location with id=-9999 does not exist")
        }
    }

    @Test
    fun `invalid type`() {
      webTestClient.post().uri("/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_LOCATIONS")))
        .body(BodyInserters.fromValue(createLocationRequest().copy(locationType = "invalid")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Location type with id=invalid does not exist")
        }
    }

    @Test
    fun `userDescription too long`() {
      webTestClient.post().uri("/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_LOCATIONS")))
        .body(BodyInserters.fromValue(createLocationRequest().copy(comment = "x".repeat(41))))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("userDescription is too long (max allowed 40 characters)")
        }
    }

    @Test
    fun `comment too long`() {
      webTestClient.post().uri("/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_LOCATIONS")))
        .body(BodyInserters.fromValue(createLocationRequest().copy(comment = "x".repeat(241))))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Comment is too long (max allowed 240 characters)")
        }
    }

    @Test
    fun `will create location with correct details`() {
      val result = webTestClient.post().uri("/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_LOCATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
                "locationCode" : "099",
                "locationType" : "CELL",
                "prisonId"     : "MDI",
                "comment"      : "this is a test!"
               }
            """.trimIndent(),
          ),
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody(LocationIdResponse::class.java)
        .returnResult().responseBody!!

      // Check the database
      repository.lookupAgencyInternalLocation(result.locationId)!!.apply {
        assertThat(locationCode).isEqualTo("099")
        assertThat(locationType.code).isEqualTo("CELL")
        assertThat(agency.id).isEqualTo("MDI")
        assertThat(comment).isEqualTo("this is a test!")
      }
    }
  }

  @Nested
  inner class GetSpecificLocationById {

    lateinit var location1: AgencyInternalLocation


    @AfterEach
    internal fun deleteData() {
      repository.delete(location1)
    }

    @BeforeEach
    internal fun createLocations() {
      nomisDataBuilder.build {
        location1 = agencyInternalLocation(
          locationCode = "100",
          locationType = "CELL",
          prisonId = "MDI",
          parentAgencyInternalLocationId = -2L,
//          capacity = 30,
//          operationalCapacity = 25,
//          cnaCapacity = 20,
//          userDescription = "A-1-1",
//          listSequence = 1,
//          comment = "this is a test!",
        )
      }
    }

    @Test
    fun `get location by id`() {
      webTestClient
        .get().uri("/locations/{id}", location1.locationId)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_LOCATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.certified").isEqualTo(false)
        .jsonPath("$.locationType").isEqualTo("CELL")
        .jsonPath("$.prisonId").isEqualTo("MDI")
        .jsonPath("$.parentLocationId").isEqualTo(-2L)
        .jsonPath("$.operationalCapacity").isEqualTo(25)
        .jsonPath("$.cnaCapacity").isEqualTo(20)
        .jsonPath("$.userDescription").isEqualTo("A-1-1")
        .jsonPath("$.locationCode").isEqualTo("100")
        .jsonPath("$.capacity").isEqualTo(30)
        .jsonPath("$.listSequence").isEqualTo(1)
        .jsonPath("$.comment").isEqualTo("this is a GET test!")
    }

    @Test
    fun `get location by id not found`() {
      webTestClient
        .get().uri(
              "/locations/-9999",
          )
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_LOCATIONS")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `get locations prevents access without appropriate role`() {
      assertThat(
        webTestClient.get()
          .uri("/locations/{id}", location1.locationId)
          .headers(setAuthorisation(roles = listOf("ROLE_BLA")))
          .exchange()
          .expectStatus().isForbidden,
      )
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/locations/{id}", location1.locationId)
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/locations/{id}", location1.locationId)
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }
  }

  @Nested
  inner class GetLocationIdsByFilterRequest {

    lateinit var location1: AgencyInternalLocation
    lateinit var location2: AgencyInternalLocation
    lateinit var location3: AgencyInternalLocation

    @BeforeEach
    internal fun createLocations() {
      nomisDataBuilder.build {
        location1 = agencyInternalLocation(
          locationCode = "X",
          locationType = "WING",
          prisonId = "MDI",
        )
        location2 = agencyInternalLocation(
          locationCode = "Y",
          locationType = "WING",
          prisonId = "MDI",
        )
        location3 = agencyInternalLocation(
          locationCode = "Z",
          locationType = "WING",
          prisonId = "MDI",
        )
      }
    }

    @Test
    fun `get all ids`() {
      webTestClient.get().uri("/locations/ids")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_LOCATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.numberOfElements").isEqualTo(3)
        .jsonPath("$.content[0].locationId").isEqualTo(location1)
        .jsonPath("$.content[1].locationId").isEqualTo(location2)
        .jsonPath("$.content[2].locationId").isEqualTo(location3)
    }

    @Test
    fun `can request a different page size`() {
      webTestClient.get().uri {
        it.path("/locations/ids")
          .queryParam("size", "2")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_LOCATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(3)
        .jsonPath("numberOfElements").isEqualTo(2)
        .jsonPath("number").isEqualTo(0)
        .jsonPath("totalPages").isEqualTo(2)
        .jsonPath("size").isEqualTo(2)
    }

    @Test
    fun `can request a different page`() {
      webTestClient.get().uri {
        it.path("/locations/ids")
          .queryParam("size", "2")
          .queryParam("page", "1")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_LOCATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(3)
        .jsonPath("numberOfElements").isEqualTo(1)
        .jsonPath("number").isEqualTo(1)
        .jsonPath("totalPages").isEqualTo(2)
        .jsonPath("size").isEqualTo(2)
    }

    @Test
    fun `get locations prevents access without appropriate role`() {
      assertThat(
        webTestClient.get().uri("/locations/ids")
          .headers(setAuthorisation(roles = listOf("ROLE_BLA")))
          .exchange()
          .expectStatus().isForbidden,
      )
    }

    @Test
    fun `get locations prevents access without authorization`() {
      assertThat(
        webTestClient.get().uri("/locations/ids")
          .exchange()
          .expectStatus().isUnauthorized,
      )
    }
  }
}
