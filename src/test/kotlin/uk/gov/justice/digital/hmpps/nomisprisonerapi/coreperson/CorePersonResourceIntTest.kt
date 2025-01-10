package uk.gov.justice.digital.hmpps.nomisprisonerapi.coreperson

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderAddressDsl.Companion.SHEFFIELD
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import java.time.LocalDate
import java.time.LocalDateTime

class CorePersonResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var offenderRepository: OffenderRepository

  @DisplayName("GET /core-person/{prisonNumber}")
  @Nested
  inner class GetOffender {
    private lateinit var offenderMinimal: Offender
    private lateinit var offenderFull: Offender

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        staff(firstName = "KOFE", lastName = "ADDY") {
          account(username = "KOFEADDY", type = "GENERAL")
        }
        offenderMinimal = offender(
          nomsId = "A1234BC",
          firstName = "JOHN",
          lastName = "BOG",
          birthDate = null,
        ) {
        }
        offenderFull = offender(
          nomsId = "B1234CD",
          titleCode = "MRS",
          firstName = "JANE",
          middleName = "Mary",
          middleName2 = "Ann",
          lastName = "NARK",
          birthDate = LocalDate.parse("1999-12-22"),
          birthPlace = "LONDON",
          ethnicityCode = "M3",
          genderCode = "F",
          whoCreated = "KOFEADDY",
          whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
        ) {
        }
      }
    }

    @AfterEach
    fun tearDown() {
      offenderRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/core-person/${offenderMinimal.nomsId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/core-person/${offenderMinimal.nomsId}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/core-person/${offenderMinimal.nomsId}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `return 404 when offender not found`() {
        webTestClient.get().uri("/core-person/AB1234C")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return basic offender data`() {
        webTestClient.get().uri("/core-person/${offenderMinimal.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("prisonNumber").isEqualTo(offenderMinimal.nomsId)
          .jsonPath("offenderId").isEqualTo(offenderMinimal.id)
          .jsonPath("title").doesNotExist()
          .jsonPath("firstName").isEqualTo("JOHN")
          .jsonPath("middleName1").doesNotExist()
          .jsonPath("middleName2").doesNotExist()
          .jsonPath("lastName").isEqualTo("BOG")
          .jsonPath("dateOfBirth").doesNotExist()
          .jsonPath("birthPlace").doesNotExist()
          .jsonPath("race").doesNotExist()
          .jsonPath("sex.code").isEqualTo("M")
          .jsonPath("sex.description").isEqualTo("Male")
          .jsonPath("aliases").doesNotExist()
          .jsonPath("audit.createUsername").isNotEmpty
          .jsonPath("audit.createDatetime").isNotEmpty
      }

      @Test
      fun `will return full offender data`() {
        webTestClient.get().uri("/core-person/${offenderFull.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("prisonNumber").isEqualTo(offenderFull.nomsId)
          .jsonPath("offenderId").isEqualTo(offenderFull.id)
          .jsonPath("title.code").isEqualTo("MRS")
          .jsonPath("title.description").isEqualTo("Mrs")
          .jsonPath("firstName").isEqualTo("JANE")
          .jsonPath("middleName1").isEqualTo("Mary")
          .jsonPath("middleName2").isEqualTo("Ann")
          .jsonPath("lastName").isEqualTo("NARK")
          .jsonPath("dateOfBirth").isEqualTo("1999-12-22")
          .jsonPath("birthPlace").isEqualTo("LONDON")
          .jsonPath("race.code").isEqualTo("M3")
          .jsonPath("race.description").isEqualTo("Mixed: White and Asian")
          .jsonPath("sex.code").isEqualTo("F")
          .jsonPath("sex.description").isEqualTo("Female")
          .jsonPath("audit.createUsername").isEqualTo("KOFEADDY")
          .jsonPath("audit.createDisplayName").isEqualTo("KOFE ADDY")
          .jsonPath("audit.createDatetime").isEqualTo("2020-01-01T10:00:00")
      }
    }

    @Nested
    inner class Aliases {
      private lateinit var offender: Offender
      private lateinit var alias: Offender

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          staff(firstName = "KOFE", lastName = "ADDY") {
            account(username = "KOFEADDY", type = "GENERAL")
          }
          offender = offender(
            nomsId = "C1234EF",
            firstName = "JANE",
            lastName = "NARK",
            birthDate = LocalDate.parse("1999-12-22"),
          ) {
            alias = alias(
              titleCode = "MR",
              lastName = "NTHANDA",
              firstName = "LEKAN",
              middleName = "Fred",
              middleName2 = "Johas",
              birthDate = LocalDate.parse("1965-07-19"),
              ethnicityCode = "M1",
              genderCode = "M",
              whoCreated = "KOFEADDY",
              whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
            ) {}
            identifier(
              type = "PNC",
              identifier = "20/0071818T",
              issuedAuthority = "Met Police",
              issuedDate = LocalDate.parse("2020-01-01"),
              verified = true,
            )
          }
        }
      }

      @Test
      fun `will return aliases`() {
        webTestClient.get().uri("/core-person/${offender.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("prisonNumber").isEqualTo(offender.nomsId)
          .jsonPath("offenderId").isEqualTo(offender.id)
          .jsonPath("firstName").isEqualTo("JANE")
          .jsonPath("lastName").isEqualTo("NARK")
          .jsonPath("aliases.length()").isEqualTo(1)
          .jsonPath("aliases[0].offenderId").isEqualTo(alias.id)
          .jsonPath("aliases[0].title.code").isEqualTo("MR")
          .jsonPath("aliases[0].title.description").isEqualTo("Mr")
          .jsonPath("aliases[0].firstName").isEqualTo("LEKAN")
          .jsonPath("aliases[0].middleName1").isEqualTo("Fred")
          .jsonPath("aliases[0].middleName2").isEqualTo("Johas")
          .jsonPath("aliases[0].lastName").isEqualTo("NTHANDA")
          .jsonPath("aliases[0].dateOfBirth").isEqualTo("1965-07-19")
          .jsonPath("aliases[0].race.code").isEqualTo("M1")
          .jsonPath("aliases[0].race.description").isEqualTo("Mixed: White and Black Caribbean")
          .jsonPath("aliases[0].sex.code").isEqualTo("M")
          .jsonPath("aliases[0].sex.description").isEqualTo("Male")
          .jsonPath("aliases[0].audit.createUsername").isEqualTo("KOFEADDY")
          .jsonPath("aliases[0].audit.createDisplayName").isEqualTo("KOFE ADDY")
          .jsonPath("aliases[0].audit.createDatetime").isEqualTo("2020-01-01T10:00:00")
      }
    }

    @Nested
    inner class Identifiers {
      private lateinit var offender: Offender

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          staff(firstName = "KOFE", lastName = "ADDY") {
            account(username = "KOFEADDY", type = "GENERAL")
          }
          offender = offender(
            nomsId = "C1234EF",
            firstName = "JANE",
            lastName = "NARK",
            birthDate = LocalDate.parse("1999-12-22"),
          ) {
            identifier(
              type = "PNC",
              identifier = "20/0071818T",
              issuedAuthority = "Met Police",
              issuedDate = LocalDate.parse("2020-01-01"),
              verified = true,
            )
            identifier(type = "STAFF", identifier = "123")
          }
        }
      }

      @Test
      fun `will return identifiers`() {
        webTestClient.get().uri("/core-person/${offender.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON"))).exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("identifiers.length()").isEqualTo(2)
          .jsonPath("identifiers[0].sequence").isEqualTo(1)
          .jsonPath("identifiers[0].offenderId").isEqualTo(offender.id)
          .jsonPath("identifiers[0].type.code").isEqualTo("PNC")
          .jsonPath("identifiers[0].type.description").isEqualTo("PNC Number")
          .jsonPath("identifiers[0].issuedAuthority").isEqualTo("Met Police")
          .jsonPath("identifiers[0].issuedDate").isEqualTo("2020-01-01")
          .jsonPath("identifiers[0].verified").isEqualTo(true)
          .jsonPath("identifiers[1].sequence").isEqualTo(2)
          .jsonPath("identifiers[1].offenderId").isEqualTo(offender.id)
          .jsonPath("identifiers[1].type.code").isEqualTo("STAFF")
          .jsonPath("identifiers[1].type.description").isEqualTo("Staff Pass/ Identity Card")
          .jsonPath("identifiers[1].issuedAuthority").doesNotExist()
          .jsonPath("identifiers[1].issuedDate").doesNotExist()
          .jsonPath("identifiers[1].verified").isEqualTo(false)
      }

      @Test
      fun `will return identifiers from all offender records rather than just the current alias`() {
        // TODO: write this test
      }
    }

    @Nested
    inner class Addresses {
      private lateinit var offender: Offender

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(
            firstName = "JOHN",
            lastName = "BOG",
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
        webTestClient.get().uri("/core-person/${offender.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("addresses[0].addressId").isEqualTo(offender.addresses[0].addressId)
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
          .jsonPath("addresses[1].addressId").isEqualTo(offender.addresses[1].addressId)
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
          .jsonPath("addresses[2].noFixedAddress").isEqualTo(true)
      }

      @Test
      fun `will return phone numbers associated with addresses`() {
        webTestClient.get().uri("/core-person/${offender.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("addresses[1].phoneNumbers[0].phoneId").isEqualTo(offender.addresses[1].phones[0].phoneId)
          .jsonPath("addresses[1].phoneNumbers[0].type.code").isEqualTo("MOB")
          .jsonPath("addresses[1].phoneNumbers[0].type.description").isEqualTo("Mobile")
          .jsonPath("addresses[1].phoneNumbers[0].number").isEqualTo("07399999999")
          .jsonPath("addresses[1].phoneNumbers[0].extension").doesNotExist()
          .jsonPath("addresses[1].phoneNumbers[1].phoneId").isEqualTo(offender.addresses[1].phones[1].phoneId)
          .jsonPath("addresses[1].phoneNumbers[1].type.code").isEqualTo("HOME")
          .jsonPath("addresses[1].phoneNumbers[1].type.description").isEqualTo("Home")
          .jsonPath("addresses[1].phoneNumbers[1].number").isEqualTo("01142561919")
          .jsonPath("addresses[1].phoneNumbers[1].extension").isEqualTo("123")
      }
    }

    @Nested
    inner class OffenderPhoneNumbers {
      private lateinit var offender: Offender

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(
            firstName = "JOHN",
            lastName = "BOG",
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
        webTestClient.get().uri("/core-person/${offender.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("phoneNumbers[0].phoneId").isEqualTo(offender.phones[0].phoneId)
          .jsonPath("phoneNumbers[0].type.code").isEqualTo("MOB")
          .jsonPath("phoneNumbers[0].type.description").isEqualTo("Mobile")
          .jsonPath("phoneNumbers[0].number").isEqualTo("07399999999")
          .jsonPath("phoneNumbers[0].extension").doesNotExist()
          .jsonPath("phoneNumbers[1].phoneId").isEqualTo(offender.phones[1].phoneId)
          .jsonPath("phoneNumbers[1].type.code").isEqualTo("HOME")
          .jsonPath("phoneNumbers[1].type.description").isEqualTo("Home")
          .jsonPath("phoneNumbers[1].number").isEqualTo("01142561919")
          .jsonPath("phoneNumbers[1].extension").isEqualTo("123")
      }
    }

    @Nested
    inner class OffenderEmailOffenderAddress {
      private lateinit var offender: Offender

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(
            firstName = "JOHN",
            lastName = "BOG",
          ) {
            email(
              emailAddress = "john.bog@justice.gov.uk",
              whoCreated = "KOFEADDY",
              whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
            )
            email(emailAddress = "john.bog@gmail.com")
          }
        }
      }

      @Test
      fun `will return email address`() {
        webTestClient.get().uri("/core-person/${offender.nomsId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CORE_PERSON")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("emailAddresses[0].emailAddressId").isEqualTo(offender.internetAddresses[0].internetAddressId)
          .jsonPath("emailAddresses[0].email").isEqualTo("john.bog@justice.gov.uk")
          .jsonPath("emailAddresses[1].emailAddressId").isEqualTo(offender.internetAddresses[1].internetAddressId)
          .jsonPath("emailAddresses[1].email").isEqualTo("john.bog@gmail.com")
      }
    }
  }
}
