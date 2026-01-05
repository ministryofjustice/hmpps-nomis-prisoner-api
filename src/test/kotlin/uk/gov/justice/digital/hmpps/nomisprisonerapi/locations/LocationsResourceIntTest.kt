package uk.gov.justice.digital.hmpps.nomisprisonerapi.locations

import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import java.time.LocalDate
import java.time.LocalDateTime

class LocationsResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var repository: Repository

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  private val createLocationRequest: () -> CreateLocationRequest = {
    CreateLocationRequest(
      certified = true,
      locationType = "LAND",
      prisonId = "MDI",
      locationCode = "5",
      description = "LEI-A-2-TEST",
      parentLocationId = -1L,
      tracking = false,
    )
  }

  private fun webOperation(uri: Pair<String, String>): WebTestClient.RequestHeadersSpec<*> = when (uri.first) {
    "POST" -> webTestClient.post()
      .uri(uri.second, -1)
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(createLocationRequest()))

    "PUT" -> webTestClient.put()
      .uri(uri.second, -99999)
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(createLocationRequest().copy(cnaCapacity = 2)))

    "DELETE" -> webTestClient.delete()
      .uri(uri.second, -1)

    "GET" -> webTestClient.get()
      .uri(uri.second, -99999)

    else -> throw IllegalArgumentException("Invalid method ${uri.first}")
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class SecureEndpoints {
    private fun secureGetEndpoints() = listOf(
      "POST" to "/locations",
      "PUT" to "/locations/{locationId}",
      "PUT" to "/locations/{locationId}/deactivate",
      "PUT" to "/locations/{locationId}/reactivate",
      "PUT" to "/locations/{locationId}/capacity",
      "PUT" to "/locations/{locationId}/certification",
      "GET" to "/locations/{locationId}",
      "GET" to "/locations/key/{key}",
      "GET" to "/locations/ids",
      "GET" to "/locations/{locationId}",
    )

    @ParameterizedTest
    @MethodSource("secureGetEndpoints")
    fun `requires a valid authentication token`(uri: Pair<String, String>) {
      webOperation(uri)
        .exchange()
        .expectStatus().isUnauthorized
    }

    @ParameterizedTest
    @MethodSource("secureGetEndpoints")
    fun `requires a role`(uri: Pair<String, String>) {
      webOperation(uri)
        .headers(setAuthorisation(roles = emptyList()))
        .exchange()
        .expectStatus().isForbidden
    }

    @ParameterizedTest
    @MethodSource("secureGetEndpoints")
    fun `requires the correct role`(uri: Pair<String, String>) {
      webOperation(uri)
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @ParameterizedTest
    @MethodSource("secureGetEndpoints")
    fun `returns 404 if location not found`(uri: Pair<String, String>) {
      if (!uri.first.equals("POST") && !uri.second.equals("/locations/ids")) { // skip the 'create' and ids endpoints
        webOperation(uri)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }

  @Nested
  inner class CreateLocation {

    private var createdLocation: AgencyInternalLocation? = null

    @AfterEach
    internal fun deleteData() {
      createdLocation?.apply { repository.delete(this) }
    }

    @Test
    fun `agency not found`() {
      webTestClient.post().uri("/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .body(BodyInserters.fromValue(createLocationRequest().copy(prisonId = "XXX")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Prison with id=XXX does not exist")
        }
    }

    @Test
    fun `parent location not found`() {
      webTestClient.post().uri("/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .body(BodyInserters.fromValue(createLocationRequest().copy(parentLocationId = -9999)))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Parent location with id=-9999 does not exist")
        }
    }

    @Test
    fun `invalid housing unit type`() {
      webTestClient.post().uri("/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
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
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
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
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
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
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
                "locationCode"        : "CLASS7",
                "description"        :  "LEI-CLASS7",
                "locationType"        : "CLAS",
                "prisonId"            : "LEI",
                "comment"             : "this is a test!",
                "capacity"            : 30,
                "operationalCapacity" : 25,
                "cnaCapacity"         : 20,
                "userDescription"     : "user description",
                "listSequence"        : 1,
                "certified"           : true,
                "tracking"            : false,
                "profiles"            : [
                  {
                    "profileType"     : "HOU_UNIT_ATT",
                    "profileCode"     : "NSMC"
                  },
                  {
                    "profileType"     : "SUP_LVL_TYPE",
                    "profileCode"     : "EL"
                  }
                ],
                "usages"              : [
                  {
                    "internalLocationUsageType" : "APP",
                    "capacity"   : 41,
                    "sequence"   : 2
                  },
                  {
                    "internalLocationUsageType" : "OTH",
                    "capacity"   : 42,
                    "sequence"   : 3
                  }
                ]
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
        assertThat(description).isEqualTo("LEI-CLASS7")
        assertThat(locationType).isEqualTo("CLAS")
        assertThat(agency.id).isEqualTo("LEI")
        assertThat(comment).isEqualTo("this is a test!")
        assertThat(capacity).isEqualTo(30)
        assertThat(operationalCapacity).isEqualTo(25)
        assertThat(cnaCapacity).isEqualTo(20)
        assertThat(userDescription).isEqualTo("user description")
        assertThat(listSequence).isEqualTo(1)
        assertThat(certified).isTrue
        assertThat(tracking).isFalse
        with(profiles) {
          assertThat(this).hasSize(2)
          assertThat(this[0].id.profileType).isEqualTo("HOU_UNIT_ATT")
          assertThat(this[0].id.profileCode).isEqualTo("NSMC")
          assertThat(this[1].id.profileType).isEqualTo("SUP_LVL_TYPE")
          assertThat(this[1].id.profileCode).isEqualTo("EL")
        }
        with(usages) {
          assertThat(this).hasSize(2)
          assertThat(this[0].internalLocationUsage.internalLocationUsage).isEqualTo("APP")
          assertThat(this[0].capacity).isEqualTo(41)
          assertThat(this[0].listSequence).isEqualTo(2)
          assertThat(this[1].internalLocationUsage.internalLocationUsage).isEqualTo("OTH")
          assertThat(this[1].capacity).isEqualTo(42)
          assertThat(this[1].listSequence).isEqualTo(3)
        }
      }
    }

    @Test
    fun `will create location with parent`() {
      val result = webTestClient.post().uri("/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
                "locationCode"     : "005",
                "description"      : "BXI-A-1-005",
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
        assertThat(locationType).isEqualTo("CELL")
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
  inner class UpdateLocation {

    private lateinit var location1: AgencyInternalLocation

    @BeforeEach
    internal fun setupLocations() {
      nomisDataBuilder.build {
        location1 = agencyInternalLocation(
          locationCode = "100",
          locationType = "CELL",
          prisonId = "LEI",
          parentAgencyInternalLocationId = -2L,
          capacity = 3,
          operationalCapacity = 1,
          cnaCapacity = 2,
          userDescription = "user description",
          listSequence = 100,
          comment = "this is an UPDATE test!",
        ) {
          attributes("HOU_UNIT_ATT", "OTH")
          usages(-3L, 41, "APP")
        }
      }
    }

    @AfterEach
    internal fun deleteData() {
      repository.deleteAgencyInternalLocationById(location1.locationId)
    }

    @Test
    fun `parent location not found`() {
      webTestClient.put().uri("/locations/{locationId}", location1.locationId)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .body(BodyInserters.fromValue(createLocationRequest().copy(parentLocationId = -9999)))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Parent location with id=-9999 does not exist")
        }
    }

    @Test
    fun `invalid housing unit type`() {
      webTestClient.put().uri("/locations/{locationId}", location1.locationId)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .body(BodyInserters.fromValue(createLocationRequest().copy(unitType = "invalid")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Housing unit type with id=invalid does not exist")
        }
    }

    @Test
    fun `userDescription too long`() {
      webTestClient.put().uri("/locations/{locationId}", location1.locationId)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .body(BodyInserters.fromValue(createLocationRequest().copy(userDescription = "x".repeat(41))))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("userDescription is too long (max allowed 40 characters)")
        }
    }

    @Test
    fun `comment too long`() {
      webTestClient.put().uri("/locations/{locationId}", location1.locationId)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .body(BodyInserters.fromValue(createLocationRequest().copy(comment = "x".repeat(241))))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Comment is too long (max allowed 240 characters)")
        }
    }

    @Test
    fun `will update location with correct details`() {
      webTestClient.put().uri("/locations/{locationId}", location1.locationId)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
                "locationCode"        : "CLASS7",
                "description"        :  "LEI-CLASS7",
                "locationType"        : "CLAS",
                "comment"             : "this is a test!",
                "userDescription"     : "new description",
                "listSequence"        : 1,
                "unitType"            : "NA",
                "tracking"            : true,
                "profiles"            : [
                  {
                    "profileType"     : "HOU_UNIT_ATT",
                    "profileCode"     : "NSMC"
                  },
                  {
                    "profileType"     : "SUP_LVL_TYPE",
                    "profileCode"     : "EL"
                  }
                ],
                "usages"              : [
                  {
                    "internalLocationUsageType" : "APP",
                    "capacity"   : 41,
                    "sequence"   : 2
                  },
                  {
                    "internalLocationUsageType" : "OTH",
                    "capacity"   : 42,
                    "sequence"   : 3
                  }
                ]
               }
            """.trimIndent(),
          ),
        )
        .exchange()
        .expectStatus().isOk

      // Check the database
      repository.lookupAgencyInternalLocation(location1.locationId)!!.apply {
        assertThat(locationCode).isEqualTo("CLASS7")
        assertThat(description).isEqualTo("LEI-CLASS7")
        assertThat(locationType).isEqualTo("CLAS")
        assertThat(comment).isEqualTo("this is a test!")
        assertThat(userDescription).isEqualTo("new description")
        assertThat(listSequence).isEqualTo(1)
        assertThat(unitType?.code).isEqualTo("NA")
        assertThat(tracking).isTrue
        with(profiles) {
          assertThat(this).hasSize(2)
          assertThat(this[0].id.profileType).isEqualTo("HOU_UNIT_ATT")
          assertThat(this[0].id.profileCode).isEqualTo("NSMC")
          assertThat(this[1].id.profileType).isEqualTo("SUP_LVL_TYPE")
          assertThat(this[1].id.profileCode).isEqualTo("EL")
        }
        with(usages) {
          assertThat(this).hasSize(2)
          assertThat(this[0].internalLocationUsage.internalLocationUsage).isEqualTo("APP")
          assertThat(this[0].capacity).isEqualTo(41)
          assertThat(this[0].listSequence).isEqualTo(2)
          assertThat(this[1].internalLocationUsage.internalLocationUsage).isEqualTo("OTH")
          assertThat(this[1].capacity).isEqualTo(42)
          assertThat(this[1].listSequence).isEqualTo(3)
        }
      }
    }

    @Test
    fun `will update location with parent`() {
      webTestClient.put().uri("/locations/{locationId}", location1.locationId)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
                "locationCode"     : "005",
                "description"      : "MDI-A-1-005",
                "locationType"     : "CELL",
                "unitType"         : "NA",
                "parentLocationId" : -3008,
                "comment"          : "this is a cell!",
                "userDescription"  : "new description",
                "listSequence"     : 5
               }
            """.trimIndent(),
          ),
        )
        .exchange()
        .expectStatus().isOk

      // Check the database
      repository.lookupAgencyInternalLocation(location1.locationId)!!.apply {
        assertThat(locationCode).isEqualTo("005")
        assertThat(description).isEqualTo("MDI-A-1-005")
        assertThat(locationType).isEqualTo("CELL")
        assertThat(unitType?.code).isEqualTo("NA")
        assertThat(parentLocation?.locationId).isEqualTo(-3008)
        assertThat(comment).isEqualTo("this is a cell!")
        assertThat(userDescription).isEqualTo("new description")
        assertThat(listSequence).isEqualTo(5)
        assertThat(tracking).isFalse
      }
    }
  }

  @Nested
  inner class DeactivateLocation {

    private var location1: AgencyInternalLocation? = null

    @AfterEach
    internal fun deleteData() {
      if (location1 != null) {
        repository.delete(location1!!)
      }
    }

    @Test
    fun `already deactivated`() {
      nomisDataBuilder.build {
        location1 = agencyInternalLocation(
          locationCode = "MEDI",
          locationType = "MEDI",
          prisonId = "MDI",
          listSequence = 100,
          active = false,
          deactivationDate = LocalDate.parse("2024-01-01"),
        )
      }
      webTestClient.put().uri("/locations/{locationId}/deactivate", location1!!.locationId)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue("{}"))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Location with id=${location1!!.locationId} is already inactive")
        }
    }

    @Test
    fun `already deactivated but forced`() {
      nomisDataBuilder.build {
        location1 = agencyInternalLocation(
          locationCode = "MEDI",
          locationType = "MEDI",
          prisonId = "MDI",
          listSequence = 100,
          active = false,
          deactivationDate = LocalDate.parse("2024-01-01"),
        )
      }
      webTestClient.put().uri("/locations/{locationId}/deactivate", location1!!.locationId)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue("""{ "deactivateDate": "2024-02-02", "force": true }"""))
        .exchange()
        .expectStatus().isOk

      // Check the database
      repository.lookupAgencyInternalLocation(location1!!.locationId)!!.apply {
        assertThat(deactivateDate).isEqualTo(LocalDate.parse("2024-02-02"))
        assertThat(deactivateReason).isNull()
        assertThat(active).isFalse()
      }
    }

    @Test
    fun `invalid reason code`() {
      nomisDataBuilder.build {
        location1 = agencyInternalLocation(
          locationCode = "MEDI",
          locationType = "MEDI",
          prisonId = "MDI",
          listSequence = 100,
        )
      }
      webTestClient.put().uri("/locations/{locationId}/deactivate", location1!!.locationId)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue("""{ "reasonCode" : "XXX" }"""))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Deactivate Reason code=XXX does not exist")
        }
    }

    @Test
    fun `will deactivate location successfully`() {
      nomisDataBuilder.build {
        location1 = agencyInternalLocation(
          locationCode = "MEDI",
          locationType = "MEDI",
          prisonId = "MDI",
          listSequence = 100,
        )
      }
      webTestClient.put().uri("/locations/{locationId}/deactivate", location1!!.locationId)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue("{}"))
        .exchange()
        .expectStatus().isOk

      // Check the database
      repository.lookupAgencyInternalLocation(location1!!.locationId)!!.apply {
        assertThat(deactivateDate).isEqualTo(LocalDate.now())
        assertThat(deactivateReason).isNull()
        assertThat(active).isFalse()
      }
    }

    @Test
    fun `will deactivate location with reason and dates given`() {
      nomisDataBuilder.build {
        location1 = agencyInternalLocation(
          locationCode = "MEDI",
          locationType = "MEDI",
          prisonId = "MDI",
          listSequence = 100,
        )
      }
      webTestClient.put().uri("/locations/{locationId}/deactivate", location1!!.locationId)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue("""{ "deactivateDate" : "2024-02-15", "reasonCode" : "C", "reactivateDate" : "2024-02-27"}"""))
        .exchange()
        .expectStatus().isOk

      // Check the database
      repository.lookupAgencyInternalLocation(location1!!.locationId)!!.apply {
        assertThat(deactivateDate).isEqualTo(LocalDate.parse("2024-02-15"))
        assertThat(reactivateDate).isEqualTo(LocalDate.parse("2024-02-27"))
        assertThat(deactivateReason?.code).isEqualTo("C")
      }
    }
  }

  @Nested
  inner class ReactivateLocation {

    private var location1: AgencyInternalLocation? = null

    @AfterEach
    internal fun deleteData() {
      if (location1 != null) {
        repository.delete(location1!!)
      }
    }

    @Test
    fun `already active`() {
      nomisDataBuilder.build {
        location1 = agencyInternalLocation(
          locationCode = "MEDI",
          locationType = "MEDI",
          prisonId = "MDI",
          listSequence = 100,
        )
      }
      webTestClient.put().uri("/locations/{locationId}/reactivate", location1!!.locationId)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue("{}"))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Location with id=${location1!!.locationId} is already active")
        }
    }

    @Test
    fun `will reactivate location successfully`() {
      nomisDataBuilder.build {
        location1 = agencyInternalLocation(
          locationCode = "MEDI",
          locationType = "MEDI",
          prisonId = "MDI",
          listSequence = 100,
          active = false,
          deactivationDate = LocalDate.parse("2024-02-01"),
        )
      }
      webTestClient.put().uri("/locations/{locationId}/reactivate", location1!!.locationId)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue("{}"))
        .exchange()
        .expectStatus().isOk

      // Check the database
      repository.lookupAgencyInternalLocation(location1!!.locationId)!!.apply {
        assertThat(reactivateDate).isNull()
        assertThat(deactivateDate).isNull()
        assertThat(deactivateReason).isNull()
        assertThat(active).isTrue()
      }
    }
  }

  @Nested
  inner class UpdateCapacity {

    private var location1: AgencyInternalLocation? = null

    @AfterEach
    internal fun deleteData() {
      if (location1 != null) {
        repository.delete(location1!!)
      }
    }

    @Test
    fun `will update capacity successfully`() {
      nomisDataBuilder.build {
        location1 = agencyInternalLocation(
          locationCode = "MEDI",
          locationType = "MEDI",
          prisonId = "MDI",
          listSequence = 100,
          capacity = 10,
          operationalCapacity = 5,
        )
      }
      webTestClient.put().uri("/locations/{locationId}/capacity", location1!!.locationId)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
                 "capacity"            : 20,
                 "operationalCapacity" : 15
               }
            """.trimIndent(),
          ),
        )
        .exchange()
        .expectStatus().isOk

      // Check the database
      repository.lookupAgencyInternalLocation(location1!!.locationId)!!.apply {
        assertThat(capacity).isEqualTo(20)
        assertThat(operationalCapacity).isEqualTo(15)
      }
    }

    @Test
    fun `will ignore operational capacity`() {
      nomisDataBuilder.build {
        location1 = agencyInternalLocation(
          locationCode = "MEDI",
          locationType = "MEDI",
          prisonId = "MDI",
          listSequence = 100,
          capacity = 10,
          operationalCapacity = 5,
        )
      }
      webTestClient.put().uri("/locations/{locationId}/capacity?ignoreOperationalCapacity=true", location1!!.locationId)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
                 "capacity"            : 20,
                 "operationalCapacity" : 15
               }
            """.trimIndent(),
          ),
        )
        .exchange()
        .expectStatus().isOk

      // Check the database
      repository.lookupAgencyInternalLocation(location1!!.locationId)!!.apply {
        assertThat(capacity).isEqualTo(20)
        assertThat(operationalCapacity).isEqualTo(5)
      }
    }
  }

  @Nested
  inner class UpdateCertification {

    private var location1: AgencyInternalLocation? = null

    @AfterEach
    internal fun deleteData() {
      if (location1 != null) {
        repository.delete(location1!!)
      }
    }

    @Test
    fun `will update certification successfully`() {
      nomisDataBuilder.build {
        location1 = agencyInternalLocation(
          locationCode = "MEDI",
          locationType = "MEDI",
          prisonId = "MDI",
          listSequence = 100,
          cnaCapacity = 5,
        )
      }
      webTestClient.put().uri("/locations/{locationId}/certification", location1!!.locationId)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue("""{ "certified" : false, "cnaCapacity" : 4 }"""))
        .exchange()
        .expectStatus().isOk

      // Check the database
      repository.lookupAgencyInternalLocation(location1!!.locationId)!!.apply {
        assertThat(certified).isFalse
        assertThat(cnaCapacity).isEqualTo(4)
      }
    }
  }

  @Nested
  inner class GetLocationById {

    private lateinit var location1: AgencyInternalLocation

    @BeforeEach
    internal fun setupLocations() {
      nomisDataBuilder.build {
        location1 = agencyInternalLocation(
          locationCode = "100",
          locationType = "CELL",
          prisonId = "LEI",
          parentAgencyInternalLocationId = -2L,
          capacity = 30,
          operationalCapacity = 25,
          cnaCapacity = 20,
          userDescription = "user description",
          listSequence = 100,
          comment = "this is a GET test!",
          deactivationDate = LocalDate.parse("2024-01-01"),
          reactivationDate = LocalDate.parse("2024-01-02"),
        ) {
          attributes("HOU_UNIT_ATT", "OTH")
          usages(-3L, 41, "MEDI", 2)
          amendments(
            amendDateTime = LocalDateTime.parse("2024-01-02T10:15:30"),
            columnName = "CAPACITY",
            oldValue = "4",
            newValue = "5",
            amendUserId = "QQQ99X",
          )
          amendments(
            amendDateTime = LocalDateTime.parse("2023-01-02T10:15:30"),
            columnName = "COMMENT",
            oldValue = "old",
            newValue = "new",
            amendUserId = "QQQ99Y",
          )
        }
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
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.certified").isEqualTo(true)
        .jsonPath("$.locationType").isEqualTo("CELL")
        .jsonPath("$.prisonId").isEqualTo("LEI")
        .jsonPath("$.parentLocationId").isEqualTo(-2L)
        .jsonPath("$.parentKey").isEqualTo("LEI-A-1")
        .jsonPath("$.operationalCapacity").isEqualTo(25)
        .jsonPath("$.cnaCapacity").isEqualTo(20)
        .jsonPath("$.description").isEqualTo("LEI-A-1-100")
        .jsonPath("$.userDescription").isEqualTo("user description")
        .jsonPath("$.locationCode").isEqualTo("100")
        .jsonPath("$.capacity").isEqualTo(30)
        .jsonPath("$.listSequence").isEqualTo(100)
        .jsonPath("$.comment").isEqualTo("this is a GET test!")
        .jsonPath("$.unitType").isEqualTo("NA")
        .jsonPath("$.active").isEqualTo(true)
        .jsonPath("$.deactivateDate").isEqualTo("2024-01-01")
        .jsonPath("$.reasonCode").isEqualTo("A")
        .jsonPath("$.reactivateDate").isEqualTo("2024-01-02")
        .jsonPath("$.tracking").isEqualTo(false)
        .jsonPath("$.profiles[0].profileType").isEqualTo("HOU_UNIT_ATT")
        .jsonPath("$.profiles[0].profileCode").isEqualTo("OTH")
        .jsonPath("$.usages[0].capacity").isEqualTo(41)
        .jsonPath("$.usages[0].sequence").isEqualTo(2)
        .jsonPath("$.usages[0].internalLocationUsageType").isEqualTo("MOVEMENT")
        .jsonPath("$.amendments[0].amendDateTime").isEqualTo("2024-01-02T10:15:30")
        .jsonPath("$.amendments[0].columnName").isEqualTo("CAPACITY")
        .jsonPath("$.amendments[0].oldValue").isEqualTo("4")
        .jsonPath("$.amendments[0].newValue").isEqualTo("5")
        .jsonPath("$.amendments[0].amendedBy").isEqualTo("QQQ99X")
        .jsonPath("$.amendments[1].amendDateTime").isEqualTo("2023-01-02T10:15:30")
        .jsonPath("$.amendments[1].columnName").isEqualTo("COMMENT")
        .jsonPath("$.amendments[1].oldValue").isEqualTo("old")
        .jsonPath("$.amendments[1].newValue").isEqualTo("new")
        .jsonPath("$.amendments[1].amendedBy").isEqualTo("QQQ99Y")
        .jsonPath("$.createUsername").isEqualTo("SA")
        .jsonPath("$.modifyUsername").doesNotExist()
    }
  }

  @Nested
  inner class GetLocationByBusinessKey {

    private lateinit var location1: AgencyInternalLocation

    @BeforeEach
    internal fun createLocations() {
      nomisDataBuilder.build {
        location1 = agencyInternalLocation(
          locationCode = "100",
          locationType = "CELL",
          prisonId = "LEI",
          parentAgencyInternalLocationId = -32L,
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
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.certified").isEqualTo(true)
        .jsonPath("$.locationType").isEqualTo("CELL")
        .jsonPath("$.prisonId").isEqualTo("LEI")
        .jsonPath("$.parentLocationId").isEqualTo(-32L)
        .jsonPath("$.parentKey").isEqualTo("LEI-A-2")
        .jsonPath("$.operationalCapacity").isEqualTo(25)
        .jsonPath("$.cnaCapacity").isEqualTo(20)
        .jsonPath("$.description").isEqualTo("LEI-A-2-100")
        .jsonPath("$.userDescription").isEqualTo("user description")
        .jsonPath("$.locationCode").isEqualTo("100")
        .jsonPath("$.capacity").isEqualTo(30)
        .jsonPath("$.listSequence").isEqualTo(100)
        .jsonPath("$.comment").isEqualTo("this is a key GET test!")
    }
  }

  @Nested
  inner class GetLocationIdsByFilterRequest {

    @Test
    fun `get all ids`() {
      webTestClient.get().uri("/locations/ids")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.numberOfElements").isEqualTo(10)
        .jsonPath("$.content[*].locationId").value<List<Int>> {
          assertThat(it).contains(-3010, -3009, -3008)
        }
    }

    @Test
    fun `can request a different page size`() {
      webTestClient.get().uri {
        it.path("/locations/ids")
          .queryParam("size", "5")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(311)
        .jsonPath("numberOfElements").isEqualTo(5)
        .jsonPath("number").isEqualTo(0)
        .jsonPath("totalPages").isEqualTo(63)
        .jsonPath("size").isEqualTo(5)
        .jsonPath("$.content[*].locationId").value<List<Int>> {
          assertThat(it).contains(-3010, -3009, -3008)
        }
    }

    @Test
    fun `can request a different page`() {
      webTestClient.get().uri {
        it.path("/locations/ids")
          .queryParam("size", "5")
          .queryParam("page", "1")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(311)
        .jsonPath("numberOfElements").isEqualTo(5)
        .jsonPath("number").isEqualTo(1)
        .jsonPath("totalPages").isEqualTo(63)
        .jsonPath("size").isEqualTo(5)
        .jsonPath("$.content[*].locationId").value<List<Int>> {
          assertThat(it).contains(-3005, -3004, -3003)
        }
    }
  }
}
