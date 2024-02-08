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

    private var createdLocation: AgencyInternalLocation? = null

    private val createLocationRequest: () -> CreateLocationRequest = {
      CreateLocationRequest(
        certified = true,
        locationType = "LAND",
        prisonId = "MDI",
        locationCode = "5",
        parentLocationId = -1L,
      )
    }

    @AfterEach
    internal fun deleteData() {
      createdLocation?.apply { repository.delete(this) }
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
    fun `invalid location type`() {
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
    fun `invalid housing unit type`() {
      webTestClient.post().uri("/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_LOCATIONS")))
        .body(BodyInserters.fromValue(createLocationRequest().copy(unitType = "invalid")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Housing unit type with id=invalid does not exist")
        }
    }

    @Test
    fun `userDescription too long`() {
      webTestClient.post().uri("/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_LOCATIONS")))
        .body(BodyInserters.fromValue(createLocationRequest().copy(userDescription = "x".repeat(41))))
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
                "locationCode"        : "CLASS7",
                "locationType"        : "CLAS",
                "prisonId"            : "MDI",
                "comment"             : "this is a test!",
                "capacity"            : 30,
                "operationalCapacity" : 25,
                "cnaCapacity"         : 20,
                "userDescription"     : "user description",
                "listSequence"        : 1,
                "certified"           : true
               }
            """.trimIndent(),
          ),
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody(LocationIdResponse::class.java)
        .returnResult().responseBody!!

      // Check the database
      createdLocation = repository.lookupAgencyInternalLocation(result.locationId)!!.apply {
        assertThat(locationCode).isEqualTo("CLASS7")
        assertThat(description).isEqualTo("CLASS7")
        assertThat(locationType.code).isEqualTo("CLAS")
        assertThat(agency.id).isEqualTo("MDI")
        assertThat(comment).isEqualTo("this is a test!")
        assertThat(capacity).isEqualTo(30)
        assertThat(operationalCapacity).isEqualTo(25)
        assertThat(cnaCapacity).isEqualTo(20)
        assertThat(userDescription).isEqualTo("user description")
        assertThat(listSequence).isEqualTo(1)
        assertThat(certified).isTrue
      }
    }

    @Test
    fun `will create location with parent`() {
      val result = webTestClient.post().uri("/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_LOCATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
                "locationCode"     : "005",
                "locationType"     : "CELL",
                "unitType"         : "NA",
                "prisonId"         : "BXI",
                "parentLocationId" : -3008,
                "comment"          : "this is a cell!",
                "capacity"         : 1,
                "userDescription"  : "user description",
                "listSequence"     : 5,
                "certified"        : true
               }
            """.trimIndent(),
          ),
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody(LocationIdResponse::class.java)
        .returnResult().responseBody!!

      // Check the database
      createdLocation = repository.lookupAgencyInternalLocation(result.locationId)!!.apply {
        assertThat(locationCode).isEqualTo("005")
        assertThat(description).isEqualTo("BXI-A-1-005")
        assertThat(locationType.code).isEqualTo("CELL")
        assertThat(unitType?.code).isEqualTo("NA")
        assertThat(agency.id).isEqualTo("BXI")
        assertThat(comment).isEqualTo("this is a cell!")
        assertThat(capacity).isEqualTo(1)
        assertThat(operationalCapacity).isNull()
        assertThat(cnaCapacity).isNull()
        assertThat(userDescription).isEqualTo("user description")
        assertThat(listSequence).isEqualTo(5)
        assertThat(certified).isTrue
      }
    }
  }

  @Nested
  inner class GetLocationById {

    lateinit var location1: AgencyInternalLocation

    @BeforeEach
    internal fun createLocations() {
      nomisDataBuilder.build {
        location1 = agencyInternalLocation(
          locationCode = "100",
          locationType = "CELL",
          prisonId = "MDI",
          parentAgencyInternalLocationId = -2L,
          capacity = 30,
          operationalCapacity = 25,
          cnaCapacity = 20,
          userDescription = "user description",
          listSequence = 100,
          comment = "this is a GET test!",
        )
      }
    }

    @AfterEach
    internal fun deleteData() {
      repository.delete(location1)
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
        .jsonPath("$.description").isEqualTo("LEI-A-1-100")
        .jsonPath("$.userDescription").isEqualTo("user description")
        .jsonPath("$.locationCode").isEqualTo("100")
        .jsonPath("$.capacity").isEqualTo(30)
        .jsonPath("$.listSequence").isEqualTo(100)
        .jsonPath("$.comment").isEqualTo("this is a GET test!")
    }

    @Test
    fun `get location by id not found`() {
      webTestClient
        .get().uri("/locations/-9999")
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
      webTestClient.get().uri("/locations/{id}", location1.locationId)
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/locations/{id}", location1.locationId)
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }
  }

  @Nested
  inner class GetLocationByBusinessKey {

    lateinit var location1: AgencyInternalLocation

    @BeforeEach
    internal fun createLocations() {
      nomisDataBuilder.build {
        location1 = agencyInternalLocation(
          locationCode = "100",
          locationType = "CELL",
          prisonId = "MDI",
          parentAgencyInternalLocationId = -2L,
          capacity = 30,
          operationalCapacity = 25,
          cnaCapacity = 20,
          userDescription = "user description",
          listSequence = 100,
          comment = "this is a key GET test!",
        )
      }
    }

    @AfterEach
    internal fun deleteData() {
      repository.delete(location1)
    }

    @Test
    fun `get location by id`() {
      webTestClient
        .get().uri("/locations/key/{key}", location1.description)
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
        .jsonPath("$.description").isEqualTo("LEI-A-1-100")
        .jsonPath("$.userDescription").isEqualTo("user description")
        .jsonPath("$.locationCode").isEqualTo("100")
        .jsonPath("$.capacity").isEqualTo(30)
        .jsonPath("$.listSequence").isEqualTo(100)
        .jsonPath("$.comment").isEqualTo("this is a key GET test!")
    }

    @Test
    fun `get location by id not found`() {
      webTestClient
        .get().uri("/locations/key/doesnt-exist")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_LOCATIONS")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `get locations prevents access without appropriate role`() {
      assertThat(
        webTestClient.get()
          .uri("/locations/key/{key}", location1.description)
          .headers(setAuthorisation(roles = listOf("ROLE_BLA")))
          .exchange()
          .expectStatus().isForbidden,
      )
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/locations/key/{key}", location1.description)
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/locations/key/{key}", location1.description)
        .headers(setAuthorisation(roles = emptyList()))
        .exchange()
        .expectStatus().isForbidden
    }
  }

  @Nested
  inner class GetLocationIdsByFilterRequest {

    @Test
    fun `get all ids`() {
      webTestClient.get().uri("/locations/ids")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_LOCATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.numberOfElements").isEqualTo(10)
        .jsonPath("$.content[0].locationId").isEqualTo(-3010)
        .jsonPath("$.content[1].locationId").isEqualTo(-3009)
        .jsonPath("$.content[2].locationId").isEqualTo(-3008)
    }

    @Test
    fun `can request a different page size`() {
      webTestClient.get().uri {
        it.path("/locations/ids")
          .queryParam("size", "5")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_LOCATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(309)
        .jsonPath("numberOfElements").isEqualTo(5)
        .jsonPath("number").isEqualTo(0)
        .jsonPath("totalPages").isEqualTo(310 / 5)
        .jsonPath("size").isEqualTo(5)
        .jsonPath("$.content[0].locationId").isEqualTo(-3010)
        .jsonPath("$.content[1].locationId").isEqualTo(-3009)
        .jsonPath("$.content[2].locationId").isEqualTo(-3008)
        .jsonPath("$.content[3].locationId").isEqualTo(-3007)
        .jsonPath("$.content[4].locationId").isEqualTo(-3006)
    }

    @Test
    fun `can request a different page`() {
      webTestClient.get().uri {
        it.path("/locations/ids")
          .queryParam("size", "5")
          .queryParam("page", "1")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_LOCATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(309)
        .jsonPath("numberOfElements").isEqualTo(5)
        .jsonPath("number").isEqualTo(1)
        .jsonPath("totalPages").isEqualTo(310 / 5)
        .jsonPath("size").isEqualTo(5)
        .jsonPath("$.content[0].locationId").isEqualTo(-3005)
        .jsonPath("$.content[1].locationId").isEqualTo(-3004)
        .jsonPath("$.content[2].locationId").isEqualTo(-3003)
        .jsonPath("$.content[3].locationId").isEqualTo(-3002)
        .jsonPath("$.content[4].locationId").isEqualTo(-3001)
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
