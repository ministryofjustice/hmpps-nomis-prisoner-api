package uk.gov.justice.digital.hmpps.nomisprisonerapi.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SplashScreen.Companion.SPLASH_ALL_PRISONS

class SplashScreenResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var repository: Repository

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @BeforeEach
  fun `set up`() {
    nomisDataBuilder.build {
      splashScreen(moduleName = "SCREEN1", accessBlockedCode = "NO")
      splashScreen(moduleName = "SCREEN2", accessBlockedCode = "COND", warningText = "A warning", blockedText = "Block message") {
        splashCondition(prisonId = SPLASH_ALL_PRISONS, accessBlocked = true)
      }
      splashScreen(moduleName = "SCREEN3", accessBlockedCode = "YES", warningText = "A warning", blockedText = "Block message") {
        splashCondition(prisonId = "LEI", accessBlocked = true)
        splashCondition(prisonId = "MDI", accessBlocked = false)
      }
      splashScreen(moduleName = "SCREEN4", accessBlockedCode = "COND", warningText = "A warning", blockedText = "Block message") {
        splashCondition(prisonId = "ASI", accessBlocked = true)
        splashCondition(prisonId = "BLI", accessBlocked = false)
        splashCondition(prisonId = "LEI", accessBlocked = true)
        splashCondition(prisonId = "MDI", accessBlocked = false)
      }
    }
  }

  @AfterEach
  fun `tear down`() {
    repository.deleteAllSplashScreens()
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
        .jsonPath("accessBlockedType.code").isEqualTo("YES")
        .jsonPath("warningText").isEqualTo("A warning")
        .jsonPath("blockedText").isEqualTo("Block message")
    }

    @Test
    fun `should return splash condition data if it exists`() {
      webTestClient.get()
        .uri("/splash-screens/SCREEN3")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SCREEN_ACCESS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("moduleName").isEqualTo("SCREEN3")
        .jsonPath("conditions.size()").isEqualTo(2)
        .jsonPath("conditions[0].prisonId").isEqualTo("LEI")
        .jsonPath("conditions[0].accessBlocked").isEqualTo(true)
        .jsonPath("conditions[0].type.code").isEqualTo("CASELOAD")
        .jsonPath("conditions[1].prisonId").isEqualTo("MDI")
        .jsonPath("conditions[1].accessBlocked").isEqualTo(false)
        .jsonPath("conditions[1].type.code").isEqualTo("CASELOAD")
    }

    @Test
    fun `should not return splash condition data if it doesn't exist`() {
      webTestClient.get()
        .uri("/splash-screens/SCREEN1")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SCREEN_ACCESS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("moduleName").isEqualTo("SCREEN1")
        .jsonPath("conditions.size()").isEqualTo(0)
    }
  }

  @Nested
  @DisplayName("GET /splash-screens/{moduleName}/blocked")
  inner class GetBlockedPrisons {
    @Test
    fun `should return unauthorised without an auth token`() {
      webTestClient.get()
        .uri("/splash-screens/SCREEN1/blocked")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should return forbidden without a role`() {
      webTestClient.get()
        .uri("/splash-screens/SCREEN1/blocked")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return forbidden without a valid role`() {
      webTestClient.get()
        .uri("/splash-screens/SCREEN1/blocked")
        .headers(setAuthorisation(roles = listOf("ROLE_INVALID")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return empty list if splash screen does not exist`() {
      webTestClient.get()
        .uri("/splash-screens/UNKNOWN_SCREEN/blocked")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SCREEN_ACCESS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("size()").isEqualTo(0)
    }

    @Test
    fun `should return no prisons for 'NO' splash screen blocked type`() {
      webTestClient.get()
        .uri("/splash-screens/SCREEN1/blocked")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SCREEN_ACCESS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("size()").isEqualTo(0)
    }

    @Test
    fun `should return all prisons for 'YES' splash screen blocked type, irrespective of condition setting`() {
      webTestClient.get()
        .uri("/splash-screens/SCREEN3/blocked")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SCREEN_ACCESS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("size()").isEqualTo(2)
        .jsonPath("[0].prisonId").isEqualTo("LEI")
        .jsonPath("[1].prisonId").isEqualTo("MDI")
    }

    @Test
    fun `should return special case for 'COND' with splash screen blocked type and 'ALL' prisons`() {
      webTestClient.get()
        .uri("/splash-screens/SCREEN2/blocked")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SCREEN_ACCESS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("size()").isEqualTo(1)
        .jsonPath("[0].prisonId").isEqualTo(SPLASH_ALL_PRISONS)
    }

    @Test
    fun `should only return prisons for 'COND' splash screen blocked type with accessBlocked condition set`() {
      webTestClient.get()
        .uri("/splash-screens/SCREEN4/blocked")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SCREEN_ACCESS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("size()").isEqualTo(2)
        .jsonPath("[0].prisonId").isEqualTo("ASI")
        .jsonPath("[1].prisonId").isEqualTo("LEI")
    }
  }
}
