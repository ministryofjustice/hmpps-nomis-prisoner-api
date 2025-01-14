package uk.gov.justice.digital.hmpps.nomisprisonerapi.corporates

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Corporate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CorporateRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisoners.expectBodyResponse
import java.time.LocalDate
import java.time.LocalDateTime

class CorporateResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var corporateRepository: CorporateRepository

  @Nested
  @DisplayName("GET /corporates/{corporateId}")
  inner class GetCorporate {
    @AfterEach
    fun tearDown() {
      corporateRepository.deleteAll()
    }

    @Nested
    inner class Security {
      private lateinit var corporate: Corporate

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          corporate = corporate(corporateName = "Police")
        }
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/corporates/${corporate.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/corporates/${corporate.id}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/corporates/${corporate.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `will return 404 if corporate does not exist`() {
        webTestClient.get().uri("/corporates/999999")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      private lateinit var corporate: Corporate
      private lateinit var hotel: Corporate

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          corporate = corporate(corporateName = "Police")
          hotel = corporate(
            corporateName = "Holiday Inn",
            caseloadId = "LEI",
            active = false,
            expiryDate = LocalDate.parse("2023-04-01"),
            taxNo = "G123445",
            feiNumber = "1",
            commentText = "Good place to work",
            whoCreated = "M.BOLD",
            whenCreated = LocalDateTime.parse("2023-03-22T10:20:30"),

          ) {
            type("YOTWORKER")
            type("TEA")
          }
        }
      }

      @Test
      fun `will find a corporate when it exists`() {
        webTestClient.get().uri("/corporates/${corporate.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("id").isEqualTo(corporate.id)
      }

      @Test
      fun `will return the core corporate data`() {
        val corporateOrganisation: CorporateOrganisation = webTestClient.get().uri("/corporates/${hotel.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse()

        with(corporateOrganisation) {
          assertThat(id).isEqualTo(hotel.id)
          assertThat(name).isEqualTo("Holiday Inn")
          assertThat(caseload?.code).isEqualTo("LEI")
          assertThat(caseload?.description).isEqualTo("LEEDS (HMP)")
          assertThat(comment).isEqualTo("Good place to work")
          assertThat(programmeNumber).isEqualTo("1")
          assertThat(vatNumber).isEqualTo("G123445")
          assertThat(active).isFalse()
          assertThat(expiryDate).isEqualTo(LocalDate.parse("2023-04-01"))
          assertThat(audit.createDatetime).isEqualTo(LocalDateTime.parse("2023-03-22T10:20:30"))
          assertThat(audit.createUsername).isEqualTo("M.BOLD")
        }
      }

      @Test
      fun `will return any associated corporate types`() {
        val corporateOrganisation: CorporateOrganisation = webTestClient.get().uri("/corporates/${hotel.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse()

        with(corporateOrganisation) {
          assertThat(types).hasSize(2)
          assertThat(types[0].type).isEqualTo(
            CodeDescription(
              code = "YOTWORKER",
              description = "YOT Offender Supervisor/Manager",
            ),
          )
          assertThat(types[1].type).isEqualTo(
            CodeDescription(
              code = "TEA",
              description = "Teacher",
            ),
          )
        }
      }
    }
  }
}
