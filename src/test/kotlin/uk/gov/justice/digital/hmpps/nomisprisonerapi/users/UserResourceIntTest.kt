package uk.gov.justice.digital.hmpps.nomisprisonerapi.users

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.StaffDsl.Companion.ADMIN
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Role
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.RoleRepository
import java.time.LocalDateTime

class UserResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var rolesRepository: RoleRepository
  private lateinit var staff1: Staff
  private lateinit var staff2: Staff
  private lateinit var role1: Role

  @BeforeEach
  fun setup() {
    nomisDataBuilder.build {
      role1 = role(
        code = "CODE_1",
        name = "This is a test role",
        userAccountType = "GENERAL",
      )
      role(
        code = "CODE_2",
        name = "This is the second test role",
        userAccountType = "ADMIN",
      )
      staff1 = staff(firstName = "JIM", lastName = "STAFFA") {
        email(emailAddress = "jim.staffa@justice.gov.uk")
        account(username = "JIIMSTAFFA_GEN", activeCaseloadId = "MDI", lastLoggedIn = LocalDateTime.parse("2026-03-17T12:30"))
        account(username = "JIIMSTAFFA_ADM", type = ADMIN) {
          userCaseload(caseloadId = "MDI")
        }
      }

      staff2 = staff(firstName = "JOE", lastName = "STAFFB") {
        account(username = "JOESTAFFB")
      }
    }
  }

  @AfterEach
  fun tearDown() {
    rolesRepository.deleteAll()
    repository.delete(staff1)
    repository.delete(staff2)
  }

  @Nested
  @DisplayName("GET /users/{username}")
  inner class GetUser {

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/users/${staff1.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/users/${staff1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/users/${staff1.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun `unknown incident should return not found`() {
      webTestClient.get().uri("/users/-99999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
        .jsonPath("userMessage").value<String> {
          assertThat(it).contains("Not Found: User with id=-99999 does not exist")
        }
    }

    @Test
    fun `will return user details`() {
      webTestClient.get().uri("/users/${staff1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("id").isEqualTo(staff1.id)
        .jsonPath("firstName").isEqualTo("JIM")
        .jsonPath("lastName").isEqualTo("STAFFA")
        .jsonPath("email").isEqualTo("jim.staffa@justice.gov.uk")
        .jsonPath("statusCode").isEqualTo("ACTIVE")
        .jsonPath("audit.createDatetime").isNotEmpty
        .jsonPath("audit.createUsername").isEqualTo("SA")
    }

    @Test
    fun `will return a user's account details`() {
      webTestClient.get().uri("/users/${staff1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("id").isEqualTo(staff1.id)
        .jsonPath("accounts.size()").isEqualTo(2)
        .jsonPath("accounts[0].username").isEqualTo("JIIMSTAFFA_GEN")
        .jsonPath("accounts[0].sourceCode").isEqualTo("USER")
        .jsonPath("accounts[0].typeCode").isEqualTo("GENERAL")
        .jsonPath("accounts[0].activeCaseloadId").isEqualTo("MDI")
        .jsonPath("accounts[0].lastLoggedIn").isEqualTo("2026-03-17T12:30:00")
        .jsonPath("accounts[0].caseloads.size()").isEqualTo(0)
        .jsonPath("audit.createDatetime").isNotEmpty
        .jsonPath("audit.createUsername").isEqualTo("SA")
        .jsonPath("accounts[1].typeCode").isEqualTo("ADMIN")
        .jsonPath("accounts[1].lastLoggedIn").doesNotExist()
        .jsonPath("accounts[1].caseloads.size()").isEqualTo(1)
        .jsonPath("accounts[1].caseloads[0]]").isEqualTo("MDI")
    }
  }
}
