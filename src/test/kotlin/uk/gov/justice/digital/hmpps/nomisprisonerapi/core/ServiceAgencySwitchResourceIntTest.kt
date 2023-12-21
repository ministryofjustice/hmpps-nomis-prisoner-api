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

  @BeforeEach
  fun `set up`() {
    nomisDataBuilder.build {
      externalService(serviceName = "SOME_SERVICE") {
        serviceAgencySwitch(prisonId = "LEI")
        serviceAgencySwitch(prisonId = "MDI")
      }
    }
  }

  @AfterEach
  fun `tear down`() {
    serviceAgencySwitchesRepository.deleteAll()
    externalServiceRepository.deleteAll()
  }

  @Nested
  @DisplayName("GET /service-prisons/{serviceCode}")
  inner class GetServicePrisons {
    @Test
    fun `should return unauthorised without an auth token`() {
      webTestClient.get()
        .uri("/service-prisons/SOME_SERVICE")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should return forbidden without a role`() {
      webTestClient.get()
        .uri("/service-prisons/SOME_SERVICE")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return forbidden without a valid role`() {
      webTestClient.get()
        .uri("/service-prisons/SOME_SERVICE")
        .headers(setAuthorisation(roles = listOf("ROLE_INVALID")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return not found if service does not exist`() {
      webTestClient.get()
        .uri("/service-prisons/UNKNOWN_SERVICE")
        .headers(setAuthorisation(roles = listOf("ROLE_SYNCHRONISATION_REPORTING")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("Service code UNKNOWN_SERVICE does not exist")
        }
    }

    @Test
    fun `should return a list of prisons for the service`() {
      webTestClient.get()
        .uri("/service-prisons/SOME_SERVICE")
        .headers(setAuthorisation(roles = listOf("ROLE_SYNCHRONISATION_REPORTING")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.size()").isEqualTo(2)
        .jsonPath("$[0].prisonId").isEqualTo("LEI")
        .jsonPath("$[1].prisonId").isEqualTo("MDI")
    }

    @Test
    fun `should return an empty list if no prisons`() {
      nomisDataBuilder.build {
        externalService(serviceName = "ANOTHER_SERVICE")
      }

      webTestClient.get()
        .uri("/service-prisons/ANOTHER_SERVICE")
        .headers(setAuthorisation(roles = listOf("ROLE_SYNCHRONISATION_REPORTING")))
        .exchange()
        .expectStatus().isOk
        .expectBody().jsonPath("$.size()").isEqualTo(0)
    }
  }

  @Nested
  @DisplayName("GET /service-prisons/{serviceCode}/prison/{prisonId}")
  inner class GetServicePrison {
    @Test
    fun `should return unauthorised without an auth token`() {
      webTestClient.get()
        .uri("/service-prisons/SOME_SERVICE/prison/MDI")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should return forbidden without a role`() {
      webTestClient.get()
        .uri("/service-prisons/SOME_SERVICE/prison/MDI")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return forbidden without a valid role`() {
      webTestClient.get()
        .uri("/service-prisons/SOME_SERVICE/prison/MDI")
        .headers(setAuthorisation(roles = listOf("ROLE_INVALID")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return not found if prison not turned on for service`() {
      webTestClient.get()
        .uri("/service-prisons/SOME_SERVICE/prison/BXI")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("Service SOME_SERVICE not turned on for prison BXI")
        }
    }

    @Test
    fun `should return 204 if service turned on for prison`() {
      webTestClient.get()
        .uri("/service-prisons/SOME_SERVICE/prison/MDI")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isEqualTo(NO_CONTENT)
    }
  }

  @Nested
  @DisplayName("POST /service-prisons/{serviceCode}/prison/{prisonId}")
  inner class CreateServicePrison {
    @Test
    fun `should return unauthorised without an auth token`() {
      webTestClient.post()
        .uri("/service-prisons/SOME_SERVICE/prison/MDI")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should return forbidden without a role`() {
      webTestClient.post()
        .uri("/service-prisons/SOME_SERVICE/prison/MDI")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return forbidden without a valid role`() {
      webTestClient.post()
        .uri("/service-prisons/SOME_SERVICE/prison/MDI")
        .headers(setAuthorisation(roles = listOf("ROLE_INVALID")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return not found if the service doesn't exist`() {
      webTestClient.post()
        .uri("/service-prisons/UNKNOWN_SERVICE/prison/BXI")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("Service UNKNOWN_SERVICE does not exist")
        }
    }

    @Test
    fun `should return not found if the prison doesn't exist`() {
      webTestClient.post()
        .uri("/service-prisons/SOME_SERVICE/prison/UNKNOWN_PRISON")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("Prison UNKNOWN_PRISON does not exist")
        }
    }

    @Test
    fun `should return created if prison NOT already turned on for service`() {
      webTestClient.post()
        .uri("/service-prisons/SOME_SERVICE/prison/BXI")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isCreated

      val service = externalServiceRepository.findByIdOrNull("SOME_SERVICE") ?: throw NotFoundException("SOME_SERVICE not found")
      val agencyLocation = agencyLocationRepository.findByIdOrNull("BXI") ?: throw NotFoundException("BXI not found")
      val servicePrison = serviceAgencySwitchesRepository.findByIdOrNull(ServiceAgencySwitchId(service, agencyLocation))
      assertThat(servicePrison).isNotNull
    }

    @Test
    fun `should return created if prison IS already turned on for service`() {
      webTestClient.post()
        .uri("/service-prisons/SOME_SERVICE/prison/MDI")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isCreated

      val service = externalServiceRepository.findByIdOrNull("SOME_SERVICE") ?: throw NotFoundException("SOME_SERVICE not found")
      val agencyLocation = agencyLocationRepository.findByIdOrNull("MDI") ?: throw NotFoundException("BXI not found")
      val servicePrison = serviceAgencySwitchesRepository.findByIdOrNull(ServiceAgencySwitchId(service, agencyLocation))
      assertThat(servicePrison).isNotNull
    }
  }
}
