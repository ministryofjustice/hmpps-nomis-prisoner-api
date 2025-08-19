package uk.gov.justice.digital.hmpps.nomisprisonerapi.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus.NO_CONTENT
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ServiceAgencySwitchId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ExternalServiceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ServiceAgencySwitchesRepository

class ServiceAgencySwitchResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var externalServiceRepository: ExternalServiceRepository

  @Autowired
  private lateinit var serviceAgencySwitchesRepository: ServiceAgencySwitchesRepository

  @Autowired
  private lateinit var agencyLocationRepository: AgencyLocationRepository

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  lateinit var repository: Repository

  @BeforeEach
  fun `set up`() {
    nomisDataBuilder.build {
      externalService(serviceName = "SOME_SERVICE") {
        serviceAgencySwitch(agencyId = "LEI")
        serviceAgencySwitch(agencyId = "MDI")
      }
      externalService(serviceName = "OTHER_SERVICE") {
        serviceAgencySwitch(agencyId = "*ALL*")
      }
    }
  }

  @AfterEach
  fun `tear down`() {
    serviceAgencySwitchesRepository.deleteAll()
    externalServiceRepository.deleteAll()
  }

  @Nested
  @DisplayName("GET /agency-switches/{serviceCode}")
  inner class GetServiceAgencies {
    @Test
    fun `should return unauthorised without an auth token`() {
      webTestClient.get()
        .uri("/agency-switches/SOME_SERVICE")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should return forbidden without a role`() {
      webTestClient.get()
        .uri("/agency-switches/SOME_SERVICE")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return forbidden without a valid role`() {
      webTestClient.get()
        .uri("/agency-switches/SOME_SERVICE")
        .headers(setAuthorisation(roles = listOf("ROLE_INVALID")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return not found if service does not exist`() {
      webTestClient.get()
        .uri("/agency-switches/UNKNOWN_SERVICE")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("Service code UNKNOWN_SERVICE does not exist")
        }
    }

    @Test
    fun `should return a list of agencies for the service`() {
      webTestClient.get()
        .uri("/agency-switches/SOME_SERVICE")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.size()").isEqualTo(2)
        .jsonPath("$[0].agencyId").isEqualTo("LEI")
        .jsonPath("$[1].agencyId").isEqualTo("MDI")
    }

    @Test
    fun `should return a list of agencies even if all are switched on`() {
      webTestClient.get()
        .uri("/agency-switches/OTHER_SERVICE")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.size()").isEqualTo(1)
        .jsonPath("$[0].agencyId").isEqualTo("*ALL*")
    }

    @Test
    fun `should return an empty list if no agencies`() {
      nomisDataBuilder.build {
        externalService(serviceName = "ANOTHER_SERVICE")
      }

      webTestClient.get()
        .uri("/agency-switches/ANOTHER_SERVICE")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody().jsonPath("$.size()").isEqualTo(0)
    }
  }

  @Nested
  @DisplayName("GET /agency-switches/{serviceCode}/agency/{agencyId}")
  inner class GetServiceAgency {
    @Test
    fun `should return unauthorised without an auth token`() {
      webTestClient.get()
        .uri("/agency-switches/SOME_SERVICE/agency/MDI")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should return forbidden without a role`() {
      webTestClient.get()
        .uri("/agency-switches/SOME_SERVICE/agency/MDI")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return forbidden without a valid role`() {
      webTestClient.get()
        .uri("/agency-switches/SOME_SERVICE/agency/MDI")
        .headers(setAuthorisation(roles = listOf("ROLE_INVALID")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return not found if agency not turned on for service`() {
      webTestClient.get()
        .uri("/agency-switches/SOME_SERVICE/agency/BXI")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("Service SOME_SERVICE not turned on for agency BXI")
        }
    }

    @Test
    fun `should return 204 if service turned on for agency`() {
      webTestClient.get()
        .uri("/agency-switches/SOME_SERVICE/agency/MDI")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isEqualTo(NO_CONTENT)
    }

    @Test
    fun `should return 204 if service turned on for all agencies`() {
      webTestClient.get()
        .uri("/agency-switches/OTHER_SERVICE/agency/MDI")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isEqualTo(NO_CONTENT)
    }
  }

  @Nested
  @DisplayName("POST /agency-switches/{serviceCode}/agency/{agencyId}")
  inner class CreateServiceAgency {
    @Test
    fun `should return unauthorised without an auth token`() {
      webTestClient.post()
        .uri("/agency-switches/SOME_SERVICE/agency/MDI")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should return forbidden without a role`() {
      webTestClient.post()
        .uri("/agency-switches/SOME_SERVICE/agency/MDI")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return forbidden without a valid role`() {
      webTestClient.post()
        .uri("/agency-switches/SOME_SERVICE/agency/MDI")
        .headers(setAuthorisation(roles = listOf("ROLE_INVALID")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return not found if the service doesn't exist`() {
      webTestClient.post()
        .uri("/agency-switches/UNKNOWN_SERVICE/agency/BXI")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("Service UNKNOWN_SERVICE does not exist")
        }
    }

    @Test
    fun `should return not found if the agency doesn't exist`() {
      webTestClient.post()
        .uri("/agency-switches/SOME_SERVICE/agency/UNKNOWN_AGENCY")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("Agency UNKNOWN_AGENCY does not exist")
        }
    }

    @Test
    fun `should return created if agency NOT already turned on for service`() {
      webTestClient.post()
        .uri("/agency-switches/SOME_SERVICE/agency/BXI")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isCreated

      val service = externalServiceRepository.findByIdOrNull("SOME_SERVICE") ?: throw NotFoundException("SOME_SERVICE not found")
      val agencyLocation = agencyLocationRepository.findByIdOrNull("BXI") ?: throw NotFoundException("BXI not found")
      val serviceAgency = serviceAgencySwitchesRepository.findByIdOrNull(ServiceAgencySwitchId(service, agencyLocation))
      assertThat(serviceAgency).isNotNull
    }

    @Test
    fun `should return created if agency IS already turned on for service`() {
      webTestClient.post()
        .uri("/agency-switches/SOME_SERVICE/agency/MDI")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isCreated

      val service = externalServiceRepository.findByIdOrNull("SOME_SERVICE") ?: throw NotFoundException("SOME_SERVICE not found")
      val agencyLocation = agencyLocationRepository.findByIdOrNull("MDI") ?: throw NotFoundException("BXI not found")
      val serviceAgency = serviceAgencySwitchesRepository.findByIdOrNull(ServiceAgencySwitchId(service, agencyLocation))
      assertThat(serviceAgency).isNotNull
    }
  }

  @Nested
  @DisplayName("GET /agency-switches/{serviceCode}/prisoner/{prisonNumber}")
  inner class GetServiceAgencyForPrisoner {

    @BeforeEach
    internal fun createPrisoners() {
      nomisDataBuilder.build {
        offender(nomsId = "A1234TT") {
          booking {}
        }
        offender(nomsId = "A1234SS") {
          booking(agencyLocationId = "MDI")
        }
      }
    }

    @AfterEach
    fun cleanUp() {
      repository.deleteOffenders()
    }

    @Test
    fun `should return unauthorised without an auth token`() {
      webTestClient.get()
        .uri("/agency-switches/SOME_SERVICE/prisoner/A1234TT")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should return forbidden without a role`() {
      webTestClient.get()
        .uri("/agency-switches/SOME_SERVICE/prisoner/A1234TT")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return forbidden without a valid role`() {
      webTestClient.get()
        .uri("/agency-switches/SOME_SERVICE/prisoner/A1234TT")
        .headers(setAuthorisation(roles = listOf("ROLE_INVALID")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return not found if the service doesn't exist`() {
      webTestClient.get()
        .uri("/agency-switches/UNKNOWN_SERVICE/prisoner/A1234SS")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("Service UNKNOWN_SERVICE not turned on for prisoner A1234SS")
        }
    }

    @Test
    fun `should return not found if the prisoner doesn't exist`() {
      webTestClient.get()
        .uri("/agency-switches/SOME_SERVICE/prisoner/A9999BC")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("No prisoner with offender A9999BC found")
        }
    }

    @Test
    fun `should return not found if prisoner's agency not turned on for service`() {
      webTestClient.get()
        .uri("/agency-switches/SOME_SERVICE/prisoner/A1234TT")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("Service SOME_SERVICE not turned on for prisoner A1234TT")
        }
    }

    @Test
    fun `should return 204 if service turned on for prisoner's agency`() {
      webTestClient.get()
        .uri("/agency-switches/SOME_SERVICE/prisoner/A1234SS")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isEqualTo(NO_CONTENT)
    }
  }
}
