package uk.gov.justice.digital.hmpps.nomisprisonerapi.roles

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.RoleRepository

class RolesResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var rolesRepository: RoleRepository

  @BeforeEach
  fun setUp() {
    nomisDataBuilder.build {
      role(
        code = "CODE_1",
        name = "This is a test role",
        userAccountType = "GENERAL",
      )
      role(
        code = "CODE_2",
        name = "This is the second test role",
        userAccountType = "ADMIN",
      )
      role(
        code = "CODE_NON_DPS",
        name = "This is a non-dps test role",
        "GENERAL",
        type = "INST",
      )
    }
  }

  @AfterEach
  fun tearDown() {
    rolesRepository.deleteAll()
  }

  @Nested
  @DisplayName("GET /roles")
  inner class GetAllDpsRoles {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/roles")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/roles")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/roles")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun `get all dps roles`() {
      webTestClient.get().uri("/roles")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("size()").isEqualTo(2)
        .jsonPath("[0].code").isEqualTo("CODE_1")
        .jsonPath("[0].name").isEqualTo("This is a test role")
        .jsonPath("[0].adminRoleOnly").isEqualTo(false)
        .jsonPath("[1].code").isEqualTo("CODE_2")
        .jsonPath("[1].name").isEqualTo("This is the second test role")
        .jsonPath("[1].adminRoleOnly").isEqualTo(false)
    }

    @Test
    fun `get all roles`() {
      webTestClient.get().uri("/roles?all-roles=true")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("size()").isEqualTo(3)
        .jsonPath("[0].code").isEqualTo("CODE_1")
        .jsonPath("[0].name").isEqualTo("This is a test role")
        .jsonPath("[0].adminRoleOnly").isEqualTo(false)
        .jsonPath("[1].code").isEqualTo("CODE_2")
        .jsonPath("[1].name").isEqualTo("This is the second test role")
        .jsonPath("[1].adminRoleOnly").isEqualTo(false)
        .jsonPath("[2].code").isEqualTo("CODE_NON_DPS")
        .jsonPath("[2].name").isEqualTo("This is a non-dps test role")
        .jsonPath("[2].adminRoleOnly").isEqualTo(false)
    }
  }

  @Nested
  @DisplayName("GET /roles/{roleCode}")
  inner class GetADpsRole {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/roles/CODE_1")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/roles/CODE_1")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/roles/CODE_1")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun `will return 404 if not found`() {
      webTestClient.get().uri("/roles/DOES_NOT_EXIST")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
        .jsonPath("userMessage").value<String> {
          assertThat(it).contains("Not Found: Role with code DOES_NOT_EXIST not found")
        }
    }

    @Test
    fun `get retrieve a role`() {
      webTestClient.get().uri("/roles/CODE_1")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("code").isEqualTo("CODE_1")
        .jsonPath("name").isEqualTo("This is a test role")
        .jsonPath("adminRoleOnly").isEqualTo(false)
    }
  }
}
