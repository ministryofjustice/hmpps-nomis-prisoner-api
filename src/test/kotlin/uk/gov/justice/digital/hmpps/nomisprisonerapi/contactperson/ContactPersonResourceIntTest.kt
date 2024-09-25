package uk.gov.justice.digital.hmpps.nomisprisonerapi.contactperson

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.PersonAddressDsl.Companion.SHEFFIELD
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonRepository

class ContactPersonResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var personRepository: PersonRepository

  @DisplayName("GET /persons/{personId}")
  @Nested
  inner class GetPerson {
    private lateinit var personMinimal: Person
    private lateinit var personFull: Person

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        personMinimal = person(
          firstName = "JOHN",
          lastName = "BOG",
        ) {
        }
        personFull = person(
          firstName = "JANE",
          lastName = "NARK",
          middleName = "LIZ",
          dateOfBirth = "1999-12-22",
          gender = "F",
          title = "DR",
          language = "VIE",
          interpreterRequired = true,
          domesticStatus = "M",
          deceasedDate = "2023-12-22",
          isStaff = true,
          isRemitter = true,
          keepBiometrics = true,
        ) {
        }
      }
    }

    @AfterEach
    fun tearDown() {
      personRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/persons/${personMinimal.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/persons/${personMinimal.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/persons/${personMinimal.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `return 404 when person not found`() {
        webTestClient.get().uri("/persons/99999")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return basic person data`() {
        webTestClient.get().uri("/persons/${personMinimal.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("personId").isEqualTo(personMinimal.id)
          .jsonPath("firstName").isEqualTo("JOHN")
          .jsonPath("lastName").isEqualTo("BOG")
          .jsonPath("middleName").doesNotExist()
          .jsonPath("dateOfBirth").doesNotExist()
          .jsonPath("gender").doesNotExist()
          .jsonPath("title").doesNotExist()
          .jsonPath("language").doesNotExist()
          .jsonPath("interpreterRequired").isEqualTo(false)
          .jsonPath("domesticStatus").doesNotExist()
          .jsonPath("deceasedDate").doesNotExist()
          .jsonPath("isStaff").doesNotExist()
          .jsonPath("isRemitter").doesNotExist()
          .jsonPath("keepBiometrics").isEqualTo(false)
          .jsonPath("audit.createUsername").isNotEmpty
          .jsonPath("audit.createDatetime").isNotEmpty
      }

      @Test
      fun `will return full person data`() {
        webTestClient.get().uri("/persons/${personFull.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("personId").isEqualTo(personFull.id)
          .jsonPath("firstName").isEqualTo("JANE")
          .jsonPath("lastName").isEqualTo("NARK")
          .jsonPath("middleName").isEqualTo("LIZ")
          .jsonPath("dateOfBirth").isEqualTo("1999-12-22")
          .jsonPath("gender.code").isEqualTo("F")
          .jsonPath("gender.description").isEqualTo("Female")
          .jsonPath("title.code").isEqualTo("DR")
          .jsonPath("title.description").isEqualTo("Dr")
          .jsonPath("language.code").isEqualTo("VIE")
          .jsonPath("language.description").isEqualTo("Vietnamese")
          .jsonPath("interpreterRequired").isEqualTo(true)
          .jsonPath("domesticStatus.code").isEqualTo("M")
          .jsonPath("domesticStatus.description").isEqualTo("Married or in civil partnership")
          .jsonPath("deceasedDate").isEqualTo("2023-12-22")
          .jsonPath("isStaff").isEqualTo(true)
          .jsonPath("isRemitter").isEqualTo(true)
          .jsonPath("keepBiometrics").isEqualTo(true)
          .jsonPath("audit.createUsername").isNotEmpty
          .jsonPath("audit.createDatetime").isNotEmpty
      }
    }

    @Nested
    inner class PhoneNumbers {
      private lateinit var person: Person

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          person = person(
            firstName = "JOHN",
            lastName = "BOG",
          ) {
            phone(phoneType = "MOB", phoneNo = "07399999999")
            phone(phoneType = "HOME", phoneNo = "01142561919", extNo = "123")
          }
        }
      }

      @Test
      fun `will return phone numbers`() {
        webTestClient.get().uri("/persons/${person.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("phoneNumbers[0].phoneId").isEqualTo(person.phones[0].phoneId)
          .jsonPath("phoneNumbers[0].type.code").isEqualTo("MOB")
          .jsonPath("phoneNumbers[0].type.description").isEqualTo("Mobile")
          .jsonPath("phoneNumbers[0].number").isEqualTo("07399999999")
          .jsonPath("phoneNumbers[0].extension").doesNotExist()
          .jsonPath("phoneNumbers[1].phoneId").isEqualTo(person.phones[1].phoneId)
          .jsonPath("phoneNumbers[1].type.code").isEqualTo("HOME")
          .jsonPath("phoneNumbers[1].type.description").isEqualTo("Home")
          .jsonPath("phoneNumbers[1].number").isEqualTo("01142561919")
          .jsonPath("phoneNumbers[1].extension").isEqualTo("123")
      }
    }

    @Nested
    inner class Addresses {
      private lateinit var person: Person

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          person = person(
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
            ) {
              phone(phoneType = "MOB", phoneNo = "07399999999")
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
        webTestClient.get().uri("/persons/${person.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("addresses[0].addressId").isEqualTo(person.addresses[0].addressId)
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
          .jsonPath("addresses[1].addressId").isEqualTo(person.addresses[1].addressId)
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
        webTestClient.get().uri("/persons/${person.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("addresses[1].phoneNumbers[0].phoneId").isEqualTo(person.addresses[1].phones[0].phoneId)
          .jsonPath("addresses[1].phoneNumbers[0].type.code").isEqualTo("MOB")
          .jsonPath("addresses[1].phoneNumbers[0].type.description").isEqualTo("Mobile")
          .jsonPath("addresses[1].phoneNumbers[0].number").isEqualTo("07399999999")
          .jsonPath("addresses[1].phoneNumbers[0].extension").doesNotExist()
          .jsonPath("addresses[1].phoneNumbers[1].phoneId").isEqualTo(person.addresses[1].phones[1].phoneId)
          .jsonPath("addresses[1].phoneNumbers[1].type.code").isEqualTo("HOME")
          .jsonPath("addresses[1].phoneNumbers[1].type.description").isEqualTo("Home")
          .jsonPath("addresses[1].phoneNumbers[1].number").isEqualTo("01142561919")
          .jsonPath("addresses[1].phoneNumbers[1].extension").isEqualTo("123")
      }
    }

    @Nested
    inner class EmailAddress {
      private lateinit var person: Person

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          person = person(
            firstName = "JOHN",
            lastName = "BOG",
          ) {
            email(emailAddress = "john.bog@justice.gov.uk")
            email(emailAddress = "john.bog@gmail.com")
          }
        }
      }

      @Test
      fun `will return email address`() {
        webTestClient.get().uri("/persons/${person.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("emailAddresses[0].emailAddressId").isEqualTo(person.internetAddresses[0].internetAddressId)
          .jsonPath("emailAddresses[0].email").isEqualTo("john.bog@justice.gov.uk")
          .jsonPath("emailAddresses[1].emailAddressId").isEqualTo(person.internetAddresses[1].internetAddressId)
          .jsonPath("emailAddresses[1].email").isEqualTo("john.bog@gmail.com")
      }
    }
  }
}
