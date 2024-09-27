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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Corporate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderContactPerson
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonRepository
import java.time.LocalDate
import java.time.LocalDateTime

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

    @Nested
    inner class Employments {
      private lateinit var person: Person
      private lateinit var corporate: Corporate

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          corporate = corporate(corporateName = "Police")
          person = person(
            firstName = "JOHN",
            lastName = "BOG",
          ) {
            employment(employerCorporate = corporate)
            employment(active = false)
          }
        }
      }

      @Test
      fun `will return employments`() {
        webTestClient.get().uri("/persons/${person.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("employments[0].sequence").isEqualTo(1)
          .jsonPath("employments[0].corporate.id").isEqualTo(corporate.id)
          .jsonPath("employments[0].active").isEqualTo(true)
          .jsonPath("employments[0].corporate.name").isEqualTo("Police")
          .jsonPath("employments[1].sequence").isEqualTo(2)
          .jsonPath("employments[1].corporate").doesNotExist()
          .jsonPath("employments[1].active").isEqualTo(false)
      }
    }

    @Nested
    inner class Identifiers {
      private lateinit var person: Person

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          person = person(
            firstName = "JOHN",
            lastName = "BOG",
          ) {
            identifier(type = "PNC", identifier = "20/0071818T", issuedAuthority = "Met Police")
            identifier(type = "STAFF", identifier = "123")
          }
        }
      }

      @Test
      fun `will return identifiers`() {
        webTestClient.get().uri("/persons/${person.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("identifiers[0].sequence").isEqualTo(1)
          .jsonPath("identifiers[0].type.code").isEqualTo("PNC")
          .jsonPath("identifiers[0].type.description").isEqualTo("PNC Number")
          .jsonPath("identifiers[0].issuedAuthority").isEqualTo("Met Police")
          .jsonPath("identifiers[1].sequence").isEqualTo(2)
          .jsonPath("identifiers[1].type.code").isEqualTo("STAFF")
          .jsonPath("identifiers[1].type.description").isEqualTo("Staff Pass/ Identity Card")
          .jsonPath("identifiers[1].issuedAuthority").doesNotExist()
      }
    }

    @Nested
    inner class Contacts {
      private lateinit var person: Person
      private lateinit var prisonerA1234AA: Offender
      private var prisonerA1234AABookingId: Long = 0
      private var prisonerA1234AAOldBookingId: Long = 0
      private lateinit var prisonerA1234BB: Offender
      private var prisonerA1234BBBookingId: Long = 0
      private lateinit var nextOfKinContactToA1234AA: OffenderContactPerson
      private lateinit var visitorContactToA1234AA: OffenderContactPerson
      private lateinit var oldContactToA1234AA: OffenderContactPerson
      private lateinit var contactToA1234BB: OffenderContactPerson

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          person = person(
            firstName = "JOHN",
            lastName = "BOG",
          ) {
            identifier(type = "PNC", identifier = "20/0071818T", issuedAuthority = "Met Police")
            identifier(type = "STAFF", identifier = "123")
          }
          prisonerA1234AA = offender(nomsId = "A1234AA", firstName = "JOHN", lastName = "SMITH") {
            prisonerA1234AABookingId = booking {
              nextOfKinContactToA1234AA = contact(
                person = person,
                contactType = "S",
                relationshipType = "BRO",
                active = true,
                nextOfKin = true,
                emergencyContact = true,
                approvedVisitor = false,
                comment = "Brother is next to kin",
              )
              // same person can be contact twice so long as there is a different relationship
              visitorContactToA1234AA = contact(
                person = person,
                contactType = "S",
                relationshipType = "FRI",
                active = true,
                nextOfKin = false,
                emergencyContact = false,
                approvedVisitor = true,
                comment = "Friend can visit",
              )
            }.bookingId
            prisonerA1234AAOldBookingId = booking {
              release(date = LocalDateTime.now().minusYears(10))
              oldContactToA1234AA = contact(
                person = person,
                contactType = "S",
                relationshipType = "FRI",
                active = true,
                nextOfKin = false,
                emergencyContact = false,
                approvedVisitor = true,
                comment = "Friend can visit sometimes",
              )
            }.bookingId
          }
          prisonerA1234BB = offender(nomsId = "A1234BB", firstName = "KWAME", lastName = "KOBI") {
            prisonerA1234BBBookingId = booking {
              contactToA1234BB = contact(
                person = person,
                contactType = "O",
                relationshipType = "POL",
                active = false,
                nextOfKin = false,
                emergencyContact = false,
                approvedVisitor = false,
                expiryDate = "2022-02-12",
              )
            }.bookingId
          }
        }
      }

      @Test
      fun `will return contact details`() {
        webTestClient.get().uri("/persons/${person.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("contacts[0].id").isEqualTo(nextOfKinContactToA1234AA.id)
          .jsonPath("contacts[0].contactType.code").isEqualTo("S")
          .jsonPath("contacts[0].contactType.description").isEqualTo("Social/Family")
          .jsonPath("contacts[0].relationshipType.code").isEqualTo("BRO")
          .jsonPath("contacts[0].relationshipType.description").isEqualTo("Brother")
          .jsonPath("contacts[0].active").isEqualTo(true)
          .jsonPath("contacts[0].approvedVisitor").isEqualTo(false)
          .jsonPath("contacts[0].nextOfKin").isEqualTo(true)
          .jsonPath("contacts[0].emergencyContact").isEqualTo(true)
          .jsonPath("contacts[0].comment").isEqualTo("Brother is next to kin")
          .jsonPath("contacts[0].prisoner.bookingId").isEqualTo(prisonerA1234AABookingId)
          .jsonPath("contacts[0].prisoner.offenderNo").isEqualTo("A1234AA")
          .jsonPath("contacts[0].prisoner.lastName").isEqualTo("SMITH")
          .jsonPath("contacts[0].prisoner.firstName").isEqualTo("JOHN")
          .jsonPath("contacts[0].expiryDate").doesNotExist()
          .jsonPath("contacts[1].id").isEqualTo(visitorContactToA1234AA.id)
          .jsonPath("contacts[1].contactType.code").isEqualTo("S")
          .jsonPath("contacts[1].contactType.description").isEqualTo("Social/Family")
          .jsonPath("contacts[1].relationshipType.code").isEqualTo("FRI")
          .jsonPath("contacts[1].relationshipType.description").isEqualTo("Friend")
          .jsonPath("contacts[1].active").isEqualTo(true)
          .jsonPath("contacts[1].approvedVisitor").isEqualTo(true)
          .jsonPath("contacts[1].nextOfKin").isEqualTo(false)
          .jsonPath("contacts[1].emergencyContact").isEqualTo(false)
          .jsonPath("contacts[1].comment").isEqualTo("Friend can visit")
          .jsonPath("contacts[1].expiryDate").doesNotExist()
          .jsonPath("contacts[1].prisoner.offenderNo").isEqualTo("A1234AA")
          .jsonPath("contacts[1].prisoner.bookingId").isEqualTo(prisonerA1234AABookingId)
          .jsonPath("contacts[1].prisoner.lastName").isEqualTo("SMITH")
          .jsonPath("contacts[1].prisoner.firstName").isEqualTo("JOHN")
          .jsonPath("contacts[2].id").isEqualTo(oldContactToA1234AA.id)
          .jsonPath("contacts[2].contactType.code").isEqualTo("S")
          .jsonPath("contacts[2].contactType.description").isEqualTo("Social/Family")
          .jsonPath("contacts[2].relationshipType.code").isEqualTo("FRI")
          .jsonPath("contacts[2].relationshipType.description").isEqualTo("Friend")
          .jsonPath("contacts[2].active").isEqualTo(true)
          .jsonPath("contacts[2].approvedVisitor").isEqualTo(true)
          .jsonPath("contacts[2].nextOfKin").isEqualTo(false)
          .jsonPath("contacts[2].emergencyContact").isEqualTo(false)
          .jsonPath("contacts[2].comment").isEqualTo("Friend can visit sometimes")
          .jsonPath("contacts[2].expiryDate").doesNotExist()
          .jsonPath("contacts[2].prisoner.bookingId").isEqualTo(prisonerA1234AAOldBookingId)
          .jsonPath("contacts[2].prisoner.offenderNo").isEqualTo("A1234AA")
          .jsonPath("contacts[2].prisoner.lastName").isEqualTo("SMITH")
          .jsonPath("contacts[2].prisoner.firstName").isEqualTo("JOHN")
          .jsonPath("contacts[3].id").isEqualTo(contactToA1234BB.id)
          .jsonPath("contacts[3].contactType.code").isEqualTo("O")
          .jsonPath("contacts[3].contactType.description").isEqualTo("Official")
          .jsonPath("contacts[3].relationshipType.code").isEqualTo("POL")
          .jsonPath("contacts[3].relationshipType.description").isEqualTo("Police Officer")
          .jsonPath("contacts[3].active").isEqualTo(false)
          .jsonPath("contacts[3].approvedVisitor").isEqualTo(false)
          .jsonPath("contacts[3].nextOfKin").isEqualTo(false)
          .jsonPath("contacts[3].emergencyContact").isEqualTo(false)
          .jsonPath("contacts[3].comment").doesNotExist()
          .jsonPath("contacts[3].expiryDate").isEqualTo("2022-02-12")
          .jsonPath("contacts[3].prisoner.bookingId").isEqualTo(prisonerA1234BBBookingId)
          .jsonPath("contacts[3].prisoner.offenderNo").isEqualTo("A1234BB")
          .jsonPath("contacts[3].prisoner.lastName").isEqualTo("KOBI")
          .jsonPath("contacts[3].prisoner.firstName").isEqualTo("KWAME")
      }
    }

    @Nested
    inner class ContactRestrictions {
      private lateinit var person: Person
      private lateinit var prisoner: Offender
      private lateinit var contact: OffenderContactPerson
      private lateinit var staff: Staff

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          staff = staff(firstName = "JANE", lastName = "STAFF")
          person = person(
            firstName = "JOHN",
            lastName = "BOG",
          )
          prisoner = offender {
            booking {
              contact = contact(
                person = person,
              ) {
                restriction(
                  restrictionType = "BAN",
                  enteredStaff = staff,
                  comment = "Banned for life!",
                  effectiveDate = "2020-01-01",
                  expiryDate = "2023-02-02",
                )
                restriction(
                  restrictionType = "CCTV",
                  enteredStaff = staff,
                )
              }
            }
          }
        }
      }

      @Test
      fun `will return contact restriction details`() {
        webTestClient.get().uri("/persons/${person.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("contacts[0].restrictions[0].type.code").isEqualTo("BAN")
          .jsonPath("contacts[0].restrictions[0].type.description").isEqualTo("Banned")
          .jsonPath("contacts[0].restrictions[0].comment").isEqualTo("Banned for life!")
          .jsonPath("contacts[0].restrictions[0].effectiveDate").isEqualTo("2020-01-01")
          .jsonPath("contacts[0].restrictions[0].expiryDate").isEqualTo("2023-02-02")
          .jsonPath("contacts[0].restrictions[0].enteredStaff.staffId").isEqualTo(staff.id)
          .jsonPath("contacts[0].restrictions[1].type.code").isEqualTo("CCTV")
          .jsonPath("contacts[0].restrictions[1].type.description").isEqualTo("CCTV")
          .jsonPath("contacts[0].restrictions[1].comment").doesNotExist()
          .jsonPath("contacts[0].restrictions[1].effectiveDate").isEqualTo(LocalDate.now().toString())
          .jsonPath("contacts[0].restrictions[1].expiryDate").doesNotExist()
          .jsonPath("contacts[0].restrictions[1].enteredStaff.staffId").isEqualTo(staff.id)
      }
    }

    @Nested
    inner class GlobalRestrictions {
      private lateinit var person: Person
      private lateinit var staff: Staff

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          staff = staff(firstName = "JANE", lastName = "STAFF")
          person = person(
            firstName = "JOHN",
            lastName = "BOG",
          ) {
            restriction(
              restrictionType = "BAN",
              enteredStaff = staff,
              comment = "Banned for life!",
              effectiveDate = "2020-01-01",
              expiryDate = "2023-02-02",
            )
            restriction(
              restrictionType = "CCTV",
              enteredStaff = staff,
            )
          }
        }
      }

      @Test
      fun `will return global restriction details`() {
        webTestClient.get().uri("/persons/${person.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("restrictions[0].type.code").isEqualTo("BAN")
          .jsonPath("restrictions[0].type.description").isEqualTo("Banned")
          .jsonPath("restrictions[0].comment").isEqualTo("Banned for life!")
          .jsonPath("restrictions[0].effectiveDate").isEqualTo("2020-01-01")
          .jsonPath("restrictions[0].expiryDate").isEqualTo("2023-02-02")
          .jsonPath("restrictions[0].enteredStaff.staffId").isEqualTo(staff.id)
          .jsonPath("restrictions[1].type.code").isEqualTo("CCTV")
          .jsonPath("restrictions[1].type.description").isEqualTo("CCTV")
          .jsonPath("restrictions[1].comment").doesNotExist()
          .jsonPath("restrictions[1].effectiveDate").isEqualTo(LocalDate.now().toString())
          .jsonPath("restrictions[1].expiryDate").doesNotExist()
          .jsonPath("restrictions[1].enteredStaff.staffId").isEqualTo(staff.id)
      }
    }
  }
}
