package uk.gov.justice.digital.hmpps.nomisprisonerapi.corporates

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CorporateAddressDsl.Companion.SHEFFIELD
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CorporateInternetAddressDsl.Companion.EMAIL
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CorporateInternetAddressDsl.Companion.WEB
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

      @Nested
      inner class CoreCorporateData {
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

      @Nested
      inner class CorporatePhoneNumbers {
        private lateinit var corporate: Corporate

        @BeforeEach
        fun setUp() {
          nomisDataBuilder.build {
            corporate = corporate(
              corporateName = "Boots",
            ) {
              phone(
                phoneType = "MOB",
                phoneNo = "07399999999",
                whoCreated = "KOFEADDY",
                whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
              )
              phone(phoneType = "HOME", phoneNo = "01142561919", extNo = "123")
            }
          }
        }

        @Test
        fun `will return phone numbers`() {
          webTestClient.get().uri("/corporates/${corporate.id}")
            .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("phoneNumbers[0].id").isEqualTo(corporate.phones[0].phoneId)
            .jsonPath("phoneNumbers[0].type.code").isEqualTo("MOB")
            .jsonPath("phoneNumbers[0].type.description").isEqualTo("Mobile")
            .jsonPath("phoneNumbers[0].number").isEqualTo("07399999999")
            .jsonPath("phoneNumbers[0].extension").doesNotExist()
            .jsonPath("phoneNumbers[0].audit.createUsername").isEqualTo("KOFEADDY")
            .jsonPath("phoneNumbers[0].audit.createDatetime").isEqualTo("2020-01-01T10:00:00")
            .jsonPath("phoneNumbers[1].id").isEqualTo(corporate.phones[1].phoneId)
            .jsonPath("phoneNumbers[1].type.code").isEqualTo("HOME")
            .jsonPath("phoneNumbers[1].type.description").isEqualTo("Home")
            .jsonPath("phoneNumbers[1].number").isEqualTo("01142561919")
            .jsonPath("phoneNumbers[1].extension").isEqualTo("123")
        }
      }

      @Nested
      inner class Addresses {
        private lateinit var corporate: Corporate

        @BeforeEach
        fun setUp() {
          nomisDataBuilder.build {
            corporate = corporate(
              corporateName = "Boots",
            ) {
              address(
                premise = null,
                street = null,
                locality = null,
                postcode = "S1 3GG",
              )
              address(
                type = "HOME",
                flat = "3B",
                premise = "Brown Court",
                street = "Scotland Street",
                locality = "Hunters Bar",
                postcode = "S1 3GG",
                city = SHEFFIELD,
                county = "S.YORKSHIRE",
                country = "ENG",
                validatedPAF = true,
                noFixedAddress = false,
                primaryAddress = true,
                mailAddress = true,
                comment = "Not to be used",
                startDate = "2024-10-01",
                endDate = "2024-11-01",
                whoCreated = "KOFEADDY",
                whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
              ) {
                phone(
                  phoneType = "MOB",
                  phoneNo = "07399999999",
                  whoCreated = "KOFEADDY",
                  whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
                )
                phone(phoneType = "HOME", phoneNo = "01142561919", extNo = "123")
              }
              address(
                noFixedAddress = true,
                primaryAddress = false,
                premise = null,
                street = null,
                locality = null,
              )
            }
          }
        }

        @Test
        fun `will return addresses`() {
          webTestClient.get().uri("/corporates/${corporate.id}")
            .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("addresses[0].id").isEqualTo(corporate.addresses[0].addressId)
            .jsonPath("addresses[0].type").doesNotExist()
            .jsonPath("addresses[0].flat").doesNotExist()
            .jsonPath("addresses[0].premise").doesNotExist()
            .jsonPath("addresses[0].street").doesNotExist()
            .jsonPath("addresses[0].locality").doesNotExist()
            .jsonPath("addresses[0].city").doesNotExist()
            .jsonPath("addresses[0].county").doesNotExist()
            .jsonPath("addresses[0].country").doesNotExist()
            .jsonPath("addresses[0].validatedPAF").isEqualTo(false)
            .jsonPath("addresses[0].noFixedAddress").doesNotExist()
            .jsonPath("addresses[0].primaryAddress").isEqualTo(false)
            .jsonPath("addresses[0].mailAddress").isEqualTo(false)
            .jsonPath("addresses[0].comment").doesNotExist()
            .jsonPath("addresses[0].startDate").doesNotExist()
            .jsonPath("addresses[0].endDate").doesNotExist()
            .jsonPath("addresses[1].id").isEqualTo(corporate.addresses[1].addressId)
            .jsonPath("addresses[1].type.code").isEqualTo("HOME")
            .jsonPath("addresses[1].type.description").isEqualTo("Home Address")
            .jsonPath("addresses[1].flat").isEqualTo("3B")
            .jsonPath("addresses[1].premise").isEqualTo("Brown Court")
            .jsonPath("addresses[1].street").isEqualTo("Scotland Street")
            .jsonPath("addresses[1].locality").isEqualTo("Hunters Bar")
            .jsonPath("addresses[1].postcode").isEqualTo("S1 3GG")
            .jsonPath("addresses[1].city.code").isEqualTo("25343")
            .jsonPath("addresses[1].city.description").isEqualTo("Sheffield")
            .jsonPath("addresses[1].county.code").isEqualTo("S.YORKSHIRE")
            .jsonPath("addresses[1].county.description").isEqualTo("South Yorkshire")
            .jsonPath("addresses[1].country.code").isEqualTo("ENG")
            .jsonPath("addresses[1].country.description").isEqualTo("England")
            .jsonPath("addresses[1].validatedPAF").isEqualTo(true)
            .jsonPath("addresses[1].noFixedAddress").isEqualTo(false)
            .jsonPath("addresses[1].primaryAddress").isEqualTo(true)
            .jsonPath("addresses[1].mailAddress").isEqualTo(true)
            .jsonPath("addresses[1].comment").isEqualTo("Not to be used")
            .jsonPath("addresses[1].startDate").isEqualTo("2024-10-01")
            .jsonPath("addresses[1].endDate").isEqualTo("2024-11-01")
            .jsonPath("addresses[1].audit.createUsername").isEqualTo("KOFEADDY")
            .jsonPath("addresses[1].audit.createDatetime").isEqualTo("2020-01-01T10:00:00")
            .jsonPath("addresses[2].noFixedAddress").isEqualTo(true)
        }

        @Test
        fun `will return phone numbers associated with addresses`() {
          webTestClient.get().uri("/corporates/${corporate.id}")
            .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("addresses[1].phoneNumbers[0].id").isEqualTo(corporate.addresses[1].phones[0].phoneId)
            .jsonPath("addresses[1].phoneNumbers[0].type.code").isEqualTo("MOB")
            .jsonPath("addresses[1].phoneNumbers[0].type.description").isEqualTo("Mobile")
            .jsonPath("addresses[1].phoneNumbers[0].number").isEqualTo("07399999999")
            .jsonPath("addresses[1].phoneNumbers[0].extension").doesNotExist()
            .jsonPath("addresses[1].phoneNumbers[0].audit.createUsername").isEqualTo("KOFEADDY")
            .jsonPath("addresses[1].phoneNumbers[0].audit.createDatetime").isEqualTo("2020-01-01T10:00:00")
            .jsonPath("addresses[1].phoneNumbers[1].id").isEqualTo(corporate.addresses[1].phones[1].phoneId)
            .jsonPath("addresses[1].phoneNumbers[1].type.code").isEqualTo("HOME")
            .jsonPath("addresses[1].phoneNumbers[1].type.description").isEqualTo("Home")
            .jsonPath("addresses[1].phoneNumbers[1].number").isEqualTo("01142561919")
            .jsonPath("addresses[1].phoneNumbers[1].extension").isEqualTo("123")
        }
      }

      @Nested
      inner class CorporateEmailCorporateAddress {
        private lateinit var corporate: Corporate

        @BeforeEach
        fun setUp() {
          nomisDataBuilder.build {
            corporate = corporate(
              corporateName = "BOOTS",
            ) {
              internetAddress(
                internetAddress = "john.bog@justice.gov.uk",
                internetAddressClass = EMAIL,
                whoCreated = "KOFEADDY",
                whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
              )
              internetAddress(internetAddress = "www.boots.com", internetAddressClass = WEB)
            }
          }
        }

        @Test
        fun `will return email address`() {
          webTestClient.get().uri("/corporates/${corporate.id}")
            .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("internetAddresses[0].id").isEqualTo(corporate.internetAddresses[0].internetAddressId)
            .jsonPath("internetAddresses[0].internetAddress").isEqualTo("john.bog@justice.gov.uk")
            .jsonPath("internetAddresses[0].type").isEqualTo("EMAIL")
            .jsonPath("internetAddresses[0].audit.createUsername").isEqualTo("KOFEADDY")
            .jsonPath("internetAddresses[0].audit.createDatetime").isEqualTo("2020-01-01T10:00:00")
            .jsonPath("internetAddresses[1].id").isEqualTo(corporate.internetAddresses[1].internetAddressId)
            .jsonPath("internetAddresses[1].internetAddress").isEqualTo("www.boots.com")
            .jsonPath("internetAddresses[1].type").isEqualTo("WEB")
        }
      }
    }
  }
}
