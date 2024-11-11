package uk.gov.justice.digital.hmpps.nomisprisonerapi.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.SplashScreenRepository

class SplashScreenResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var splashScreenRepository: SplashScreenRepository

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @BeforeEach
  fun `set up`() {
    nomisDataBuilder.build {
      splashScreen(moduleName = "SCREEN1", blockAccessCode = "NO") {
        // TODO splashCondition(prisonId = "LEI")
        // TODO splashCondition(prisonId = "MDI")
      }

      splashScreen(moduleName = "SCREEN2", blockAccessCode = "COND", warningText = "A warning", blockedText = "Block message")
      splashScreen(moduleName = "SCREEN3", blockAccessCode = "YES", warningText = "A warning", blockedText = "Block message")
    }
  }

  @AfterEach
  fun `tear down`() {
    // TODO splashConditionRepository.deleteAll()
    splashScreenRepository.deleteAll()
  }

  @Nested
  @DisplayName("GET /splash-screens/{moduleName}")
  inner class GetSplashScreen {
    @Test
    fun `should return unauthorised without an auth token`() {
      webTestClient.get()
        .uri("/splash-screens/SCREEN1")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should return forbidden without a role`() {
      webTestClient.get()
        .uri("/splash-screens/SCREEN1")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return forbidden without a valid role`() {
      webTestClient.get()
        .uri("/splash-screens/SCREEN1")
        .headers(setAuthorisation(roles = listOf("ROLE_INVALID")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return not found if splash screen does not exist`() {
      webTestClient.get()
        .uri("/splash-screens/UNKNOWN_SCREEN")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SCREEN_ACCESS")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("Not Found: Splash screen with screen/module name UNKNOWN_SCREEN does not exist")
        }
    }

    @Test
    fun `should return configuration splash screen data if exists`() {
      webTestClient.get()
        .uri("/splash-screens/SCREEN3")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SCREEN_ACCESS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("moduleName").isEqualTo("SCREEN3")
        .jsonPath("blockAccessCode.code").isEqualTo("YES")
        .jsonPath("warningText").isEqualTo("A warning")
        .jsonPath("blockedText").isEqualTo("Block message")
    }
  }
}
