package uk.gov.justice.digital.hmpps.nomisprisonerapi.contactperson

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.PersonAddressDsl.Companion.SHEFFIELD
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.StaffDsl.Companion.ADMIN
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.StaffDsl.Companion.GENERAL
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AddressPhone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Corporate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderContactPerson
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderPersonRestrict
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonEmploymentPK
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonIdentifier
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonIdentifierPK
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonInternetAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonPhone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitorRestriction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AddressPhoneRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CorporateRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderContactPersonRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderPersonRestrictRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonAddressRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonEmploymentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonIdentifierRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonInternetAddressRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonPhoneRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.StaffRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitorRestrictionRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisoners.expectBodyResponse
import java.time.LocalDate
import java.time.LocalDateTime

class ContactPersonResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var repository: Repository

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var personRepository: PersonRepository

  @Autowired
  private lateinit var personContactRepository: OffenderContactPersonRepository

  @Autowired
  private lateinit var personContactRestrictionRepository: OffenderPersonRestrictRepository

  @Autowired
  private lateinit var personRestrictionRepository: VisitorRestrictionRepository

  @Autowired
  private lateinit var personAddressRepository: PersonAddressRepository

  @Autowired
  private lateinit var personInternetAddressRepository: PersonInternetAddressRepository

  @Autowired
  private lateinit var personPhoneRepository: PersonPhoneRepository

  @Autowired
  private lateinit var addressPhoneRepository: AddressPhoneRepository

  @Autowired
  private lateinit var personIdentifierRepository: PersonIdentifierRepository

  @Autowired
  private lateinit var personEmploymentRepository: PersonEmploymentRepository

  @Autowired
  private lateinit var offenderRepository: OffenderRepository

  @Autowired
  private lateinit var offenderBookingRepository: OffenderBookingRepository

  @Autowired
  private lateinit var corporateRepository: CorporateRepository

  @Autowired
  private lateinit var staffRepository: StaffRepository

  @DisplayName("GET /persons/{personId}")
  @Nested
  inner class GetPerson {
    private lateinit var personMinimal: Person
    private lateinit var personFull: Person

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        staff(firstName = "KOFE", lastName = "ADDY") {
          account(username = "KOFEADDY", type = "GENERAL")
        }
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
          whoCreated = "KOFEADDY",
          whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
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
          .headers(setAuthorisation(roles = listOf("BANANAS")))
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
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return basic person data`() {
        webTestClient.get().uri("/persons/${personMinimal.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
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
          .jsonPath("audit.createUsername").isNotEmpty
          .jsonPath("audit.createDatetime").isNotEmpty
      }

      @Test
      fun `will return full person data`() {
        webTestClient.get().uri("/persons/${personFull.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
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
          .jsonPath("audit.createUsername").isEqualTo("KOFEADDY")
          .jsonPath("audit.createDisplayName").isEqualTo("KOFE ADDY")
          .jsonPath("audit.createDatetime").isEqualTo("2020-01-01T10:00:00")
      }
    }

    @Nested
    inner class PersonPhoneNumbers {
      private lateinit var person: Person

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          person = person(
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
        webTestClient.get().uri("/persons/${person.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("phoneNumbers[0].phoneId").isEqualTo(person.phones[0].phoneId)
          .jsonPath("phoneNumbers[0].type.code").isEqualTo("MOB")
          .jsonPath("phoneNumbers[0].type.description").isEqualTo("Mobile")
          .jsonPath("phoneNumbers[0].number").isEqualTo("07399999999")
          .jsonPath("phoneNumbers[0].extension").doesNotExist()
          .jsonPath("phoneNumbers[0].audit.createUsername").isEqualTo("KOFEADDY")
          .jsonPath("phoneNumbers[0].audit.createDisplayName").isEqualTo("KOFE ADDY")
          .jsonPath("phoneNumbers[0].audit.createDatetime").isEqualTo("2020-01-01T10:00:00")
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
        webTestClient.get().uri("/persons/${person.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
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
          .jsonPath("addresses[1].audit.createUsername").isEqualTo("KOFEADDY")
          .jsonPath("addresses[1].audit.createDisplayName").isEqualTo("KOFE ADDY")
          .jsonPath("addresses[1].audit.createDatetime").isEqualTo("2020-01-01T10:00:00")
          .jsonPath("addresses[2].noFixedAddress").isEqualTo(true)
      }

      @Test
      fun `will return phone numbers associated with addresses`() {
        webTestClient.get().uri("/persons/${person.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("addresses[1].phoneNumbers[0].phoneId").isEqualTo(person.addresses[1].phones[0].phoneId)
          .jsonPath("addresses[1].phoneNumbers[0].type.code").isEqualTo("MOB")
          .jsonPath("addresses[1].phoneNumbers[0].type.description").isEqualTo("Mobile")
          .jsonPath("addresses[1].phoneNumbers[0].number").isEqualTo("07399999999")
          .jsonPath("addresses[1].phoneNumbers[0].extension").doesNotExist()
          .jsonPath("addresses[1].phoneNumbers[0].audit.createUsername").isEqualTo("KOFEADDY")
          .jsonPath("addresses[1].phoneNumbers[0].audit.createDisplayName").isEqualTo("KOFE ADDY")
          .jsonPath("addresses[1].phoneNumbers[0].audit.createDatetime").isEqualTo("2020-01-01T10:00:00")
          .jsonPath("addresses[1].phoneNumbers[1].phoneId").isEqualTo(person.addresses[1].phones[1].phoneId)
          .jsonPath("addresses[1].phoneNumbers[1].type.code").isEqualTo("HOME")
          .jsonPath("addresses[1].phoneNumbers[1].type.description").isEqualTo("Home")
          .jsonPath("addresses[1].phoneNumbers[1].number").isEqualTo("01142561919")
          .jsonPath("addresses[1].phoneNumbers[1].extension").isEqualTo("123")
      }
    }

    @Nested
    inner class PersonEmailPersonAddress {
      private lateinit var person: Person

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          person = person(
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
        webTestClient.get().uri("/persons/${person.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("emailAddresses[0].emailAddressId").isEqualTo(person.internetAddresses[0].internetAddressId)
          .jsonPath("emailAddresses[0].email").isEqualTo("john.bog@justice.gov.uk")
          .jsonPath("emailAddresses[0].audit.createUsername").isEqualTo("KOFEADDY")
          .jsonPath("emailAddresses[0].audit.createDisplayName").isEqualTo("KOFE ADDY")
          .jsonPath("emailAddresses[0].audit.createDatetime").isEqualTo("2020-01-01T10:00:00")
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
            employment(
              employerCorporate = corporate,
              whoCreated = "KOFEADDY",
              whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
            )
            employment(employerCorporate = corporate, active = false)
          }
        }
      }

      @Test
      fun `will return employments`() {
        webTestClient.get().uri("/persons/${person.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("employments[0].sequence").isEqualTo(1)
          .jsonPath("employments[0].corporate.id").isEqualTo(corporate.id)
          .jsonPath("employments[0].active").isEqualTo(true)
          .jsonPath("employments[0].corporate.name").isEqualTo("Police")
          .jsonPath("employments[0].audit.createUsername").isEqualTo("KOFEADDY")
          .jsonPath("employments[0].audit.createDisplayName").isEqualTo("KOFE ADDY")
          .jsonPath("employments[0].audit.createDatetime").isEqualTo("2020-01-01T10:00:00")
          .jsonPath("employments[1].sequence").isEqualTo(2)
          .jsonPath("employments[1].corporate.name").isEqualTo("Police")
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
            identifier(
              type = "PNC",
              identifier = "20/0071818T",
              issuedAuthority = "Met Police",
              whoCreated = "KOFEADDY",
              whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
            )
            identifier(type = "STAFF", identifier = "123")
          }
        }
      }

      @Test
      fun `will return identifiers`() {
        webTestClient.get().uri("/persons/${person.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("identifiers[0].sequence").isEqualTo(1)
          .jsonPath("identifiers[0].type.code").isEqualTo("PNC")
          .jsonPath("identifiers[0].type.description").isEqualTo("PNC Number")
          .jsonPath("identifiers[0].issuedAuthority").isEqualTo("Met Police")
          .jsonPath("identifiers[0].audit.createUsername").isEqualTo("KOFEADDY")
          .jsonPath("identifiers[0].audit.createDisplayName").isEqualTo("KOFE ADDY")
          .jsonPath("identifiers[0].audit.createDatetime").isEqualTo("2020-01-01T10:00:00")
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
                whoCreated = "KOFEADDY",
                whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
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
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
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
          .jsonPath("contacts[0].prisoner.bookingSequence").isEqualTo(1)
          .jsonPath("contacts[0].prisoner.offenderNo").isEqualTo("A1234AA")
          .jsonPath("contacts[0].prisoner.lastName").isEqualTo("SMITH")
          .jsonPath("contacts[0].prisoner.firstName").isEqualTo("JOHN")
          .jsonPath("contacts[0].expiryDate").doesNotExist()
          .jsonPath("contacts[0].audit.createUsername").isEqualTo("KOFEADDY")
          .jsonPath("contacts[0].audit.createDisplayName").isEqualTo("KOFE ADDY")
          .jsonPath("contacts[0].audit.createDatetime").isEqualTo("2020-01-01T10:00:00")
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
          .jsonPath("contacts[1].prisoner.bookingSequence").isEqualTo(1)
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
          .jsonPath("contacts[2].prisoner.bookingSequence").isEqualTo(2)
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
      private lateinit var lsaStaffMember: Staff
      private lateinit var generalStaffMember: Staff

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          lsaStaffMember = staff(firstName = "JANE", lastName = "STAFF") {
            account(username = "j.staff_adm", type = ADMIN)
            account(username = "j.staff_gen", type = GENERAL)
          }
          generalStaffMember = staff(firstName = "JANE", lastName = "SMITH") {
            account(username = "j.smith")
          }
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
                  enteredStaff = generalStaffMember,
                  comment = "Banned for life!",
                  effectiveDate = "2020-01-01",
                  expiryDate = "2023-02-02",
                  whoCreated = "KOFEADDY",
                  whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
                )
                restriction(
                  restrictionType = "CCTV",
                  enteredStaff = lsaStaffMember,
                )
              }
            }
          }
        }
      }

      @Test
      fun `will return contact restriction details`() {
        webTestClient.get().uri("/persons/${person.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("contacts[0].restrictions[0].type.code").isEqualTo("BAN")
          .jsonPath("contacts[0].restrictions[0].type.description").isEqualTo("Banned")
          .jsonPath("contacts[0].restrictions[0].comment").isEqualTo("Banned for life!")
          .jsonPath("contacts[0].restrictions[0].effectiveDate").isEqualTo("2020-01-01")
          .jsonPath("contacts[0].restrictions[0].expiryDate").isEqualTo("2023-02-02")
          .jsonPath("contacts[0].restrictions[0].enteredStaff.staffId").isEqualTo(generalStaffMember.id)
          .jsonPath("contacts[0].restrictions[0].enteredStaff.username").isEqualTo("j.smith")
          .jsonPath("contacts[0].restrictions[0].audit.createUsername").isEqualTo("KOFEADDY")
          .jsonPath("contacts[0].restrictions[0].audit.createDisplayName").isEqualTo("KOFE ADDY")
          .jsonPath("contacts[0].restrictions[0].audit.createDatetime").isEqualTo("2020-01-01T10:00:00")
          .jsonPath("contacts[0].restrictions[1].type.code").isEqualTo("CCTV")
          .jsonPath("contacts[0].restrictions[1].type.description").isEqualTo("CCTV")
          .jsonPath("contacts[0].restrictions[1].comment").doesNotExist()
          .jsonPath("contacts[0].restrictions[1].effectiveDate").isEqualTo(LocalDate.now().toString())
          .jsonPath("contacts[0].restrictions[1].expiryDate").doesNotExist()
          .jsonPath("contacts[0].restrictions[1].enteredStaff.staffId").isEqualTo(lsaStaffMember.id)
          .jsonPath("contacts[0].restrictions[1].enteredStaff.username").isEqualTo("j.staff_gen")
      }
    }

    @Nested
    inner class GlobalRestrictions {
      private lateinit var person: Person
      private lateinit var lsaStaffMember: Staff
      private lateinit var generalStaffMember: Staff

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          lsaStaffMember = staff(firstName = "JANE", lastName = "STAFF") {
            account(username = "j.staff_gen", type = GENERAL)
            account(username = "j.staff_adm", type = ADMIN)
          }
          generalStaffMember = staff(firstName = "JANE", lastName = "SMITH") {
            account(username = "j.smith")
          }

          person = person(
            firstName = "JOHN",
            lastName = "BOG",
          ) {
            restriction(
              restrictionType = "BAN",
              enteredStaff = generalStaffMember,
              comment = "Banned for life!",
              effectiveDate = "2020-01-01",
              expiryDate = "2023-02-02",
              whoCreated = "KOFEADDY",
              whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
            )
            restriction(
              restrictionType = "CCTV",
              enteredStaff = lsaStaffMember,
            )
          }
        }
      }

      @Test
      fun `will return global restriction details`() {
        webTestClient.get().uri("/persons/${person.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("restrictions[0].type.code").isEqualTo("BAN")
          .jsonPath("restrictions[0].type.description").isEqualTo("Banned")
          .jsonPath("restrictions[0].comment").isEqualTo("Banned for life!")
          .jsonPath("restrictions[0].effectiveDate").isEqualTo("2020-01-01")
          .jsonPath("restrictions[0].expiryDate").isEqualTo("2023-02-02")
          .jsonPath("restrictions[0].enteredStaff.staffId").isEqualTo(generalStaffMember.id)
          .jsonPath("restrictions[0].enteredStaff.username").isEqualTo("j.smith")
          .jsonPath("restrictions[0].audit.createUsername").isEqualTo("KOFEADDY")
          .jsonPath("restrictions[0].audit.createDisplayName").isEqualTo("KOFE ADDY")
          .jsonPath("restrictions[0].audit.createDatetime").isEqualTo("2020-01-01T10:00:00")
          .jsonPath("restrictions[1].type.code").isEqualTo("CCTV")
          .jsonPath("restrictions[1].type.description").isEqualTo("CCTV")
          .jsonPath("restrictions[1].comment").doesNotExist()
          .jsonPath("restrictions[1].effectiveDate").isEqualTo(LocalDate.now().toString())
          .jsonPath("restrictions[1].expiryDate").doesNotExist()
          .jsonPath("restrictions[1].enteredStaff.staffId").isEqualTo(lsaStaffMember.id)
          .jsonPath("restrictions[1].enteredStaff.username").isEqualTo("j.staff_gen")
      }
    }
  }

  @DisplayName("GET /prisoners/{offenderNo}/contacts")
  @Nested
  inner class GetPrisonerWithContacts {
    private lateinit var friend: Person
    private lateinit var mum: Person
    private lateinit var solicitor: Person
    private lateinit var prisoner: Offender
    private lateinit var friendContact: OffenderContactPerson
    private lateinit var mumContact: OffenderContactPerson
    private lateinit var mumContactOnOldBooking: OffenderContactPerson
    private lateinit var solicitorContact: OffenderContactPerson
    private lateinit var generalStaffMember: Staff

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        generalStaffMember = staff(firstName = "JANE", lastName = "SMITH") {
          account(username = "j.smith")
        }
        friend = person(
          firstName = "JOHN",
          lastName = "BOG",
        )
        mum = person(
          firstName = "BETH",
          lastName = "BEAKS",
        )
        solicitor = person(
          firstName = "AFAN",
          lastName = "ABDALLAH",
        )
        prisoner = offender {
          booking {
            mumContact = contact(
              person = mum,
              active = false,
              contactType = "S",
              relationshipType = "MOT",
            )
            solicitorContact = contact(
              person = solicitor,
              contactType = "O",
              relationshipType = "SOL",
            )
            friendContact = contact(
              person = friend,
              contactType = "S",
              relationshipType = "FRI",
              active = true,
              nextOfKin = false,
              emergencyContact = false,
              approvedVisitor = true,
              comment = "Friend can visit",
            ) {
              restriction(
                restrictionType = "BAN",
                enteredStaff = generalStaffMember,
                comment = "Banned for life!",
                effectiveDate = "2020-01-01",
                expiryDate = "2023-02-02",
                whoCreated = "KOFEADDY",
                whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
              )
              restriction(
                restrictionType = "CCTV",
                enteredStaff = generalStaffMember,
              )
            }
          }
          booking {
            release()
            mumContactOnOldBooking = contact(
              person = mum,
              contactType = "S",
              relationshipType = "MOT",
            )
          }
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
        webTestClient.get().uri("/prisoners/${prisoner.nomsId}/contacts")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/prisoners/${prisoner.nomsId}/contacts")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/prisoners/${prisoner.nomsId}/contacts")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `list will be empty when no prisoner found`() {
        val prisonerWithContacts: PrisonerWithContacts = webTestClient.get().uri("/prisoners/A9999KT/contacts")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk.expectBodyResponse()

        assertThat(prisonerWithContacts.contacts).isEmpty()
      }
    }

    @Nested
    inner class Filtering {
      lateinit var prisonerWithContacts: PrisonerWithContacts

      @Test
      fun `can request active and inactive contacts`() {
        prisonerWithContacts = webTestClient.get().uri("/prisoners/${prisoner.nomsId}/contacts?active-only=false&latest-booking-only=false")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBodyResponse()
        assertThat(prisonerWithContacts.contacts).hasSize(4)
        assertThat(prisonerWithContacts.contacts).anyMatch { !it.active }
      }

      @Test
      fun `can request just active contacts`() {
        prisonerWithContacts = webTestClient.get().uri("/prisoners/${prisoner.nomsId}/contacts?active-only=true&latest-booking-only=false")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBodyResponse()
        assertThat(prisonerWithContacts.contacts).hasSize(3)
        assertThat(prisonerWithContacts.contacts).noneMatch { !it.active }
      }

      @Test
      fun `by default only can active contacts return`() {
        prisonerWithContacts = webTestClient.get().uri("/prisoners/${prisoner.nomsId}/contacts?latest-booking-only=false")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBodyResponse()
        assertThat(prisonerWithContacts.contacts).hasSize(3)
        assertThat(prisonerWithContacts.contacts).noneMatch { !it.active }
      }

      @Test
      fun `can include latest booking only`() {
        prisonerWithContacts = webTestClient.get().uri("/prisoners/${prisoner.nomsId}/contacts?active-only=false&latest-booking-only=false")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBodyResponse()

        assertThat(prisonerWithContacts.contacts).hasSize(4)
        assertThat(prisonerWithContacts.contacts).anyMatch { it.bookingSequence > 1 }
      }

      @Test
      fun `can filter by latest booking only bookings`() {
        prisonerWithContacts = webTestClient.get().uri("/prisoners/${prisoner.nomsId}/contacts?active-only=false&latest-booking-only=true")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBodyResponse()

        assertThat(prisonerWithContacts.contacts).hasSize(3)
        assertThat(prisonerWithContacts.contacts).noneMatch { it.bookingSequence > 1 }
      }

      @Test
      fun `by default only latest booking contacts are returned`() {
        prisonerWithContacts = webTestClient.get().uri("/prisoners/${prisoner.nomsId}/contacts?active-only=false")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBodyResponse()

        assertThat(prisonerWithContacts.contacts).hasSize(3)
        assertThat(prisonerWithContacts.contacts).noneMatch { it.bookingSequence > 1 }
      }

      @Test
      fun `by default only latest booking contacts and active are returned`() {
        prisonerWithContacts = webTestClient.get().uri("/prisoners/${prisoner.nomsId}/contacts")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBodyResponse()

        assertThat(prisonerWithContacts.contacts).hasSize(2)
        assertThat(prisonerWithContacts.contacts).noneMatch { it.bookingSequence > 1 }
        assertThat(prisonerWithContacts.contacts).noneMatch { !it.active }
      }
    }

    @Nested
    inner class HappyPath {
      lateinit var prisonerWithContacts: PrisonerWithContacts

      @BeforeEach
      fun setUp() {
        prisonerWithContacts = webTestClient.get().uri("/prisoners/${prisoner.nomsId}/contacts?active-only=false&latest-booking-only=false")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBodyResponse()
      }

      @Test
      fun `will return contacts for all bookings`() {
        assertThat(prisonerWithContacts.contacts).hasSize(4)

        val mumOnCurrentBooking = prisonerWithContacts.contacts.find { it.id == mumContact.id }!!
        val mumOnOldBooking = prisonerWithContacts.contacts.find { it.id == mumContactOnOldBooking.id }!!

        assertThat(mumOnCurrentBooking.bookingSequence).isEqualTo(1)
        assertThat(mumOnOldBooking.bookingSequence).isEqualTo(2)
      }

      @Test
      fun `will return restrictions for contact`() {
        val friendWithRestriction = prisonerWithContacts.contacts.find { it.id == friendContact.id }!!

        assertThat(friendWithRestriction.restrictions).hasSize(2)

        val restriction = friendWithRestriction.restrictions.first()

        assertThat(restriction.type.code).isEqualTo("BAN")
        assertThat(restriction.type.description).isEqualTo("Banned")
        assertThat(restriction.comment).isEqualTo("Banned for life!")
        assertThat(restriction.effectiveDate).isEqualTo("2020-01-01")
        assertThat(restriction.expiryDate).isEqualTo("2023-02-02")
        assertThat(restriction.enteredStaff.staffId).isEqualTo(generalStaffMember.id)
        assertThat(restriction.enteredStaff.username).isEqualTo("j.smith")
      }

      @Test
      fun `will returns details about the contact relationship`() {
        val friendAsContact = prisonerWithContacts.contacts.find { it.id == friendContact.id }!!

        assertThat(friendContact.relationshipType.code).isEqualTo("FRI")
        assertThat(friendContact.contactType.code).isEqualTo("S")
        assertThat(friendAsContact.active).isTrue
        assertThat(friendAsContact.nextOfKin).isFalse
        assertThat(friendAsContact.emergencyContact).isFalse
        assertThat(friendAsContact.approvedVisitor).isTrue
        assertThat(friendAsContact.comment).isEqualTo("Friend can visit")
      }
    }
  }

  @DisplayName("GET /persons/ids")
  @Nested
  inner class GetPersonIds {
    private var lowestPersonId = 0L
    private var highestPersonId = 0L

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        lowestPersonId = (1..20).map {
          person(
            firstName = "JOHN",
            lastName = "BOG",
            whenCreated = LocalDateTime.parse("2020-01-01T10:00").minusMinutes(it.toLong()),
          )
        }.first().id
        (1..20).forEach { _ ->
          person(
            firstName = "JOHN",
            lastName = "BOG",
            whenCreated = LocalDateTime.parse("2022-01-01T00:00"),
          )
        }
        highestPersonId = (1..20).map {
          person(
            firstName = "JOHN",
            lastName = "BOG",
            whenCreated = LocalDateTime.parse("2024-01-01T10:00").minusMinutes(it.toLong()),
          )
        }.last().id
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
        webTestClient.get().uri("/persons/ids")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/persons/ids")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/persons/ids")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `by default will return first 20 or all persons`() {
        webTestClient.get().uri {
          it.path("/persons/ids")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(60)
          .jsonPath("numberOfElements").isEqualTo(20)
          .jsonPath("number").isEqualTo(0)
          .jsonPath("totalPages").isEqualTo(3)
          .jsonPath("size").isEqualTo(20)
      }

      @Test
      fun `can set page size`() {
        webTestClient.get().uri {
          it.path("/persons/ids")
            .queryParam("size", "1")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(60)
          .jsonPath("numberOfElements").isEqualTo(1)
          .jsonPath("number").isEqualTo(0)
          .jsonPath("totalPages").isEqualTo(60)
          .jsonPath("size").isEqualTo(1)
      }
    }

    @Test
    fun `can filter by fromDate`() {
      webTestClient.get().uri {
        it.path("/persons/ids")
          .queryParam("fromDate", "2020-01-02")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(40)
        .jsonPath("numberOfElements").isEqualTo(20)
        .jsonPath("number").isEqualTo(0)
        .jsonPath("totalPages").isEqualTo(2)
        .jsonPath("size").isEqualTo(20)
    }

    @Test
    fun `can filter by toDate`() {
      webTestClient.get().uri {
        it.path("/persons/ids")
          .queryParam("toDate", "2020-01-02")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(20)
        .jsonPath("numberOfElements").isEqualTo(20)
        .jsonPath("number").isEqualTo(0)
        .jsonPath("totalPages").isEqualTo(1)
        .jsonPath("size").isEqualTo(20)
    }

    @Test
    fun `can filter by fromDate and toDate`() {
      webTestClient.get().uri {
        it.path("/persons/ids")
          .queryParam("fromDate", "2020-01-02")
          .queryParam("toDate", "2022-01-02")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(20)
        .jsonPath("numberOfElements").isEqualTo(20)
        .jsonPath("number").isEqualTo(0)
        .jsonPath("totalPages").isEqualTo(1)
        .jsonPath("size").isEqualTo(20)
    }

    @Test
    fun `will return person Ids create at midnight on the day matching the filter `() {
      webTestClient.get().uri {
        it.path("/persons/ids")
          .queryParam("fromDate", "2021-12-31")
          .queryParam("toDate", "2022-01-01")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(20)

      webTestClient.get().uri {
        it.path("/persons/ids")
          .queryParam("fromDate", "2021-12-31")
          .queryParam("toDate", "2021-12-31")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(0)

      webTestClient.get().uri {
        it.path("/persons/ids")
          .queryParam("fromDate", "2022-01-01")
          .queryParam("toDate", "2022-01-01")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(20)

      webTestClient.get().uri {
        it.path("/persons/ids")
          .queryParam("fromDate", "2022-01-01")
          .queryParam("toDate", "2022-01-02")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(20)

      webTestClient.get().uri {
        it.path("/persons/ids")
          .queryParam("fromDate", "2022-01-02")
          .queryParam("toDate", "2022-01-02")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(0)
    }

    @Test
    fun `will order by personId ascending`() {
      webTestClient.get().uri {
        it.path("/persons/ids")
          .queryParam("size", "60")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("numberOfElements").isEqualTo(60)
        .jsonPath("content[0].personId").isEqualTo(lowestPersonId)
        .jsonPath("content[59].personId").isEqualTo(highestPersonId)
    }
  }

  @DisplayName("GET /persons/ids/all-from-id")
  @Nested
  inner class GetPersonIdsFromId {
    private var lowestPersonId = 0L
    private var highestPersonId = 0L

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        lowestPersonId = (1..20).map {
          person(
            firstName = "JOHN",
            lastName = "BOG",
            whenCreated = LocalDateTime.parse("2020-01-01T10:00").minusMinutes(it.toLong()),
          )
        }.first().id
        (1..20).forEach { _ ->
          person(
            firstName = "JOHN",
            lastName = "BOG",
            whenCreated = LocalDateTime.parse("2022-01-01T00:00"),
          )
        }
        highestPersonId = (1..20).map {
          person(
            firstName = "JOHN",
            lastName = "BOG",
            whenCreated = LocalDateTime.parse("2024-01-01T10:00").minusMinutes(it.toLong()),
          )
        }.last().id
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
        webTestClient.get().uri("/persons/ids/all-from-id")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/persons/ids/all-from-id")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/persons/ids/all-from-id")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `by default will return first 10 or all persons`() {
        webTestClient.get().uri {
          it.path("/persons/ids/all-from-id")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("personIds.size()").isEqualTo(10)
          .jsonPath("lastPersonId").isEqualTo(lowestPersonId + 9)
      }

      @Test
      fun `can set page size`() {
        webTestClient.get().uri {
          it.path("/persons/ids/all-from-id")
            .queryParam("pageSize", "1")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("personIds.size()").isEqualTo(1)
          .jsonPath("lastPersonId").isEqualTo(lowestPersonId)
      }

      @Test
      fun `can set request another page`() {
        webTestClient.get().uri {
          it.path("/persons/ids/all-from-id")
            .queryParam("pageSize", "10")
            .queryParam("personId", lowestPersonId + 9)
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("personIds.size()").isEqualTo(10)
          .jsonPath("personIds[0]").isEqualTo(lowestPersonId + 10)
          .jsonPath("personIds[9]").isEqualTo(lowestPersonId + 19)
          .jsonPath("lastPersonId").isEqualTo(lowestPersonId + 19)
      }

      @Test
      fun `will order by personId ascending`() {
        webTestClient.get().uri {
          it.path("/persons/ids/all-from-id")
            .queryParam("pageSize", "60")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("personIds.size()").isEqualTo(60)
          .jsonPath("lastPersonId").isEqualTo(highestPersonId)
      }
    }
  }

  @DisplayName("POST /persons")
  @Nested
  inner class CreatePerson {
    private val validPerson = CreatePersonRequest(
      firstName = "Jane",
      lastName = "Smith",
    )

    private lateinit var existingPerson: Person

    @BeforeEach
    fun setUp() {
      personRepository.deleteAll()

      nomisDataBuilder.build {
        existingPerson = person(
          firstName = "JOHN",
          lastName = "BOG",
        )
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
        webTestClient.post().uri("/persons")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validPerson)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/persons")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validPerson)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/persons")
          .bodyValue(validPerson)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `return 409 when person id supplied already exists`() {
        webTestClient.post().uri("/persons")
          .bodyValue(validPerson.copy(personId = existingPerson.id))
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isEqualTo(409)
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will create a person with core data and assign an id`() {
        val newPersonCreateResponse: CreatePersonResponse = webTestClient.post().uri("/persons")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .bodyValue(validPerson.copy(personId = null))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody(CreatePersonResponse::class.java)
          .returnResult()
          .responseBody!!

        val newPerson = personRepository.findByIdOrNull(newPersonCreateResponse.personId)!!

        assertThat(newPerson.firstName).isEqualTo("JANE")
        assertThat(newPerson.lastName).isEqualTo("SMITH")
        assertThat(newPerson.isRemitter).isNull()
      }

      @Test
      fun `will create a person with core data and set own id`() {
        webTestClient.post().uri("/persons")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .bodyValue(validPerson.copy(personId = 98765443))
          .exchange()
          .expectStatus()
          .isCreated

        val newPerson = personRepository.findByIdOrNull(98765443)!!

        assertThat(newPerson.firstName).isEqualTo("JANE")
        assertThat(newPerson.lastName).isEqualTo("SMITH")
      }

      @Test
      fun `will create a person with all supplied data`() {
        webTestClient.post().uri("/persons")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .bodyValue(
            validPerson.copy(
              personId = 98765443,
              middleName = "Tina",
              dateOfBirth = LocalDate.parse("1965-07-19"),
              genderCode = "F",
              titleCode = "DR",
              languageCode = "ENG",
              interpreterRequired = true,
              domesticStatusCode = "S",
              isStaff = true,
            ),
          )
          .exchange()
          .expectStatus()
          .isCreated

        val newPerson = personRepository.findByIdOrNull(98765443)!!

        with(newPerson) {
          assertThat(id).isEqualTo(98765443)
          assertThat(firstName).isEqualTo("JANE")
          assertThat(lastName).isEqualTo("SMITH")
          assertThat(middleName).isEqualTo("TINA")
          assertThat(birthDate).isEqualTo(LocalDate.parse("1965-07-19"))
          assertThat(sex?.code).isEqualTo("F")
          assertThat(title?.code).isEqualTo("DR")
          assertThat(domesticStatus?.code).isEqualTo("S")
          assertThat(interpreterRequired).isTrue
          assertThat(isStaff).isTrue
        }
      }
    }
  }

  @DisplayName("DELETE /persons/{personId}")
  @Nested
  inner class DeletePerson {
    private lateinit var person: Person
    private lateinit var phone: PersonPhone
    private lateinit var address: PersonAddress
    private lateinit var addressPhone: AddressPhone
    private lateinit var email: PersonInternetAddress
    private lateinit var corporate: Corporate
    private lateinit var employment: uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonEmployment
    private lateinit var identifier: PersonIdentifier
    private lateinit var prisoner: Offender
    private lateinit var contact: OffenderContactPerson
    private lateinit var staff: Staff
    private lateinit var contactRestriction: OffenderPersonRestrict
    private lateinit var personRestriction: VisitorRestriction

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        staff = staff(firstName = "KOFE", lastName = "ADDY") {
          account(username = "KOFEADDY", type = "GENERAL")
        }
        corporate = corporate(corporateName = "Police")

        person = person(
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
          whoCreated = "KOFEADDY",
          whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
        ) {
          phone = phone(
            phoneType = "MOB",
            phoneNo = "07399999999",
            whoCreated = "KOFEADDY",
            whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
          )
          phone(phoneType = "HOME", phoneNo = "01142561919", extNo = "123")
          address = address(
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
            addressPhone = phone(
              phoneType = "MOB",
              phoneNo = "07399999999",
              whoCreated = "KOFEADDY",
              whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
            )
            phone(phoneType = "HOME", phoneNo = "01142561919", extNo = "123")
          }
          email = email(
            emailAddress = "john.bog@justice.gov.uk",
            whoCreated = "KOFEADDY",
            whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
          )
          email(emailAddress = "john.bog@gmail.com")
          employment = employment(
            employerCorporate = corporate,
            whoCreated = "KOFEADDY",
            whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
          )
          employment(employerCorporate = corporate, active = false)
          identifier = identifier(
            type = "PNC",
            identifier = "20/0071818T",
            issuedAuthority = "Met Police",
            whoCreated = "KOFEADDY",
            whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
          )
          identifier(type = "STAFF", identifier = "123")
          personRestriction = restriction(
            restrictionType = "BAN",
            enteredStaff = staff,
            comment = "Banned for life!",
            effectiveDate = "2020-01-01",
            expiryDate = "2023-02-02",
            whoCreated = "KOFEADDY",
            whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
          )
          restriction(
            restrictionType = "CCTV",
            enteredStaff = staff,
          )
        }
        prisoner = offender(nomsId = "A1234AA", firstName = "JOHN", lastName = "SMITH") {
          booking {
            contact = contact(
              person = person,
              contactType = "S",
              relationshipType = "BRO",
              active = true,
              nextOfKin = true,
              emergencyContact = true,
              approvedVisitor = false,
              comment = "Brother is next to kin",
              whoCreated = "KOFEADDY",
              whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
            ) {
              contactRestriction = restriction(
                restrictionType = "BAN",
                enteredStaff = staff,
                comment = "Banned for life!",
                effectiveDate = "2020-01-01",
                expiryDate = "2023-02-02",
                whoCreated = "KOFEADDY",
                whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
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

    @AfterEach
    fun tearDown() {
      personRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete().uri("/persons/${person.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete().uri("/persons/${person.id}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete().uri("/persons/${person.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `return 204 when person not found`() {
        webTestClient.delete().uri("/persons/99999")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {

      @Test
      fun `will delete person and associated data`() {
        assertThat(personRepository.existsById(person.id)).isTrue()
        assertThat(personRestrictionRepository.existsById(personRestriction.id)).isTrue()
        assertThat(personAddressRepository.existsById(address.addressId)).isTrue()
        assertThat(personInternetAddressRepository.existsById(email.internetAddressId)).isTrue()
        assertThat(personPhoneRepository.existsById(phone.phoneId)).isTrue()
        assertThat(addressPhoneRepository.existsById(addressPhone.phoneId)).isTrue()
        assertThat(personIdentifierRepository.existsById(identifier.id)).isTrue()
        assertThat(personContactRepository.existsById(contact.id)).isTrue()
        assertThat(personContactRestrictionRepository.existsById(contactRestriction.id)).isTrue()

        assertThat(staffRepository.existsById(staff.id)).isTrue()
        assertThat(offenderRepository.existsById(prisoner.id)).isTrue()
        assertThat(corporateRepository.existsById(corporate.id)).isTrue()

        nomisDataBuilder.runInTransaction {
          assertThat(offenderRepository.findByIdOrNull(prisoner.id)!!.latestBooking().contacts).hasSize(1)
        }

        webTestClient.delete().uri("/persons/${person.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isNoContent

        assertThat(personRepository.existsById(person.id)).isFalse()
        assertThat(personRestrictionRepository.existsById(personRestriction.id)).isFalse()
        assertThat(personAddressRepository.existsById(address.addressId)).isFalse()
        assertThat(personInternetAddressRepository.existsById(email.internetAddressId)).isFalse()
        assertThat(personPhoneRepository.existsById(phone.phoneId)).isFalse()
        assertThat(addressPhoneRepository.existsById(addressPhone.phoneId)).isFalse()
        assertThat(personIdentifierRepository.existsById(identifier.id)).isFalse()
        assertThat(personContactRepository.existsById(contact.id)).isFalse()
        assertThat(personContactRestrictionRepository.existsById(contactRestriction.id)).isFalse()

        // will obviously will not get deleted
        assertThat(staffRepository.existsById(staff.id)).isTrue()
        assertThat(offenderRepository.existsById(prisoner.id)).isTrue()
        assertThat(corporateRepository.existsById(corporate.id)).isTrue()

        nomisDataBuilder.runInTransaction {
          assertThat(offenderRepository.findByIdOrNull(prisoner.id)!!.latestBooking().contacts).isEmpty()
        }
      }
    }
  }

  @DisplayName("PUT /persons/{personId}")
  @Nested
  inner class UpdatePerson {
    private lateinit var person: Person
    private lateinit var phone: PersonPhone
    private lateinit var prisoner: Offender
    private lateinit var contact: OffenderContactPerson
    private val validPersonUpdate = UpdatePersonRequest(
      firstName = "Jane",
      lastName = "Smith",
    )

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        person = person(
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
          whoCreated = "KOFEADDY",
          whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
        ) {
          phone = phone(
            phoneType = "MOB",
            phoneNo = "07399999999",
            whoCreated = "KOFEADDY",
            whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
          )
        }
        prisoner = offender(nomsId = "A1234AA", firstName = "JOHN", lastName = "SMITH") {
          booking {
            contact = contact(
              person = person,
              contactType = "S",
              relationshipType = "BRO",
              active = true,
              nextOfKin = true,
              emergencyContact = true,
              approvedVisitor = false,
              comment = "Brother is next to kin",
              whoCreated = "KOFEADDY",
              whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
            )
          }
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
        webTestClient.put().uri("/persons/${person.id}")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validPersonUpdate)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put().uri("/persons/${person.id}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validPersonUpdate)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put().uri("/persons/${person.id}")
          .bodyValue(validPersonUpdate)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `return 404 when person not found`() {
        webTestClient.put().uri("/persons/99999")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .bodyValue(validPersonUpdate)
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {

      @Test
      fun `will update person and leave child entities alone`() {
        assertThat(personRepository.existsById(person.id)).isTrue()
        assertThat(personPhoneRepository.existsById(phone.phoneId)).isTrue()
        assertThat(personContactRepository.existsById(contact.id)).isTrue()
        assertThat(offenderRepository.existsById(prisoner.id)).isTrue()

        nomisDataBuilder.runInTransaction {
          assertThat(offenderRepository.findByIdOrNull(prisoner.id)!!.latestBooking().contacts).hasSize(1)
        }

        webTestClient.put().uri("/persons/${person.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .bodyValue(validPersonUpdate)
          .exchange()
          .expectStatus()
          .isOk

        // nothing gets deleted
        assertThat(personRepository.existsById(person.id)).isTrue()
        assertThat(personPhoneRepository.existsById(phone.phoneId)).isTrue()
        assertThat(personContactRepository.existsById(contact.id)).isTrue()
        assertThat(offenderRepository.existsById(prisoner.id)).isTrue()
      }
    }

    @Test
    fun `will create a person with all supplied data`() {
      webTestClient.put().uri("/persons/${person.id}")
        .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
        .bodyValue(
          validPersonUpdate.copy(
            middleName = "Tina",
            dateOfBirth = LocalDate.parse("1965-07-19"),
            genderCode = "F",
            titleCode = "DR",
            languageCode = "ENG",
            interpreterRequired = true,
            domesticStatusCode = "S",
            isStaff = true,
            deceasedDate = LocalDate.parse("2024-08-19"),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk

      val updatedPerson = personRepository.findByIdOrNull(person.id)!!

      with(updatedPerson) {
        assertThat(firstName).isEqualTo("JANE")
        assertThat(lastName).isEqualTo("SMITH")
        assertThat(middleName).isEqualTo("TINA")
        assertThat(birthDate).isEqualTo(LocalDate.parse("1965-07-19"))
        assertThat(deceasedDate).isEqualTo(LocalDate.parse("2024-08-19"))
        assertThat(sex?.code).isEqualTo("F")
        assertThat(title?.code).isEqualTo("DR")
        assertThat(domesticStatus?.code).isEqualTo("S")
        assertThat(interpreterRequired).isTrue
        assertThat(isStaff).isTrue
      }
    }
  }

  @DisplayName("POST /persons/{personId}/contact")
  @Nested
  inner class CreateContactPerson {
    val offenderNo = "A1234KT"
    private val validContactRequest = CreatePersonContactRequest(
      offenderNo = offenderNo,
      contactTypeCode = "S",
      relationshipTypeCode = "BRO",
      active = true,
      expiryDate = null,
      approvedVisitor = false,
      nextOfKin = true,
      emergencyContact = true,
      comment = "Best friends forever",
    )

    private lateinit var existingPerson: Person
    private lateinit var existingContact: OffenderContactPerson
    private var latestBookingId = 0L

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingPerson = person(
          firstName = "JOHN",
          lastName = "BOG",
        )
        offender(nomsId = offenderNo) {
          latestBookingId = booking {
            existingContact = contact(person = existingPerson, contactType = "S", relationshipType = "SIS")
          }.bookingId
          booking {
            contact(person = existingPerson, contactType = "S", relationshipType = "BRO")
            release()
          }.bookingId
        }
      }
    }

    @AfterEach
    fun tearDown() {
      offenderRepository.deleteAll()
      personRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/contact")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validContactRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/contact")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validContactRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/contact")
          .bodyValue(validContactRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `return 409 when contact relationship supplied already exists on latest booking`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/contact")
          .bodyValue(validContactRequest.copy(relationshipTypeCode = "SIS"))
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isEqualTo(409)
      }

      @Test
      fun `return 404 when person does not exist`() {
        webTestClient.post().uri("/persons/999/contact")
          .bodyValue(validContactRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 400 when prisoner does not exist`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/contact")
          .bodyValue(validContactRequest.copy(offenderNo = "A999ZZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `return 400 when contact type does not exist`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/contact")
          .bodyValue(validContactRequest.copy(contactTypeCode = "ZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `return 400 when relationship type does not exist`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/contact")
          .bodyValue(validContactRequest.copy(relationshipTypeCode = "ZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will create a contact`() {
        val response: CreatePersonContactResponse = webTestClient.post().uri("/persons/${existingPerson.id}/contact")
          .bodyValue(validContactRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody(CreatePersonContactResponse::class.java)
          .returnResult()
          .responseBody!!

        val newContact = personContactRepository.findByIdOrNull(response.personContactId)!!

        with(newContact) {
          assertThat(offenderBooking.bookingId).isEqualTo(latestBookingId)
          assertThat(person?.id).isEqualTo(existingPerson.id)
          assertThat(nextOfKin).isTrue()
          assertThat(approvedVisitor).isFalse()
          assertThat(emergencyContact).isTrue()
          assertThat(active).isTrue()
          assertThat(expiryDate).isNull()
          assertThat(comment).isEqualTo("Best friends forever")
          assertThat(contactType.description).isEqualTo("Social/Family")
          assertThat(relationshipType.description).isEqualTo("Brother")
        }
      }
    }
  }

  @DisplayName("PUT /persons/{personId}/contact/{contactId}")
  @Nested
  inner class UpdateContactPerson {
    val offenderNo = "A1234KT"
    private val validUpdateContactRequest = UpdatePersonContactRequest(
      contactTypeCode = "S",
      relationshipTypeCode = "BRO",
      active = false,
      expiryDate = LocalDate.parse("2024-01-01"),
      approvedVisitor = true,
      nextOfKin = true,
      emergencyContact = true,
      comment = "Best friends forever",
    )

    private lateinit var existingPerson: Person
    private lateinit var existingContact: OffenderContactPerson
    private var latestBookingId = 0L

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingPerson = person(
          firstName = "JOHN",
          lastName = "BOG",
        )
        offender(nomsId = offenderNo) {
          latestBookingId = booking {
            existingContact = contact(
              person = existingPerson,
              contactType = "S",
              relationshipType = "SIS",
              active = true,
              expiryDate = null,
              comment = "Best friends",
              nextOfKin = false,
              emergencyContact = false,
              approvedVisitor = false,
            )
            contact(
              person = existingPerson,
              contactType = "S",
              relationshipType = "FRI",
              active = true,
            )
          }.bookingId
        }
      }
    }

    @AfterEach
    fun tearDown() {
      offenderRepository.deleteAll()
      personRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/contact/${existingContact.id}")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validUpdateContactRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/contact/${existingContact.id}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validUpdateContactRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/contact/${existingContact.id}")
          .bodyValue(validUpdateContactRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `return 409 when contact relationship supplied already exists on latest booking`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/contact/${existingContact.id}")
          .bodyValue(validUpdateContactRequest.copy(relationshipTypeCode = "FRI"))
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isEqualTo(409)
      }

      @Test
      fun `return 404 when person does not exist`() {
        webTestClient.put().uri("/persons/999/contact/${existingContact.id}")
          .bodyValue(validUpdateContactRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 404 when contact does not exist`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/contact/99999")
          .bodyValue(validUpdateContactRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 400 when contact type does not exist`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/contact/${existingContact.id}")
          .bodyValue(validUpdateContactRequest.copy(contactTypeCode = "ZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `return 400 when relationship type does not exist`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/contact/${existingContact.id}")
          .bodyValue(validUpdateContactRequest.copy(relationshipTypeCode = "ZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will update a contact`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/contact/${existingContact.id}")
          .bodyValue(validUpdateContactRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk

        val updatedContact = personContactRepository.findByIdOrNull(existingContact.id)!!

        with(updatedContact) {
          assertThat(offenderBooking.bookingId).isEqualTo(latestBookingId)
          assertThat(person?.id).isEqualTo(existingPerson.id)
          assertThat(nextOfKin).isTrue()
          assertThat(approvedVisitor).isTrue()
          assertThat(emergencyContact).isTrue()
          assertThat(active).isFalse()
          assertThat(expiryDate).isEqualTo(LocalDate.parse("2024-01-01"))
          assertThat(comment).isEqualTo("Best friends forever")
          assertThat(contactType.description).isEqualTo("Social/Family")
          assertThat(relationshipType.description).isEqualTo("Brother")
        }
      }
    }
  }

  @DisplayName("DELETE /persons/{personId}/contact/{contactId}")
  @Nested
  inner class DeleteContactPerson {
    val offenderNo = "A1234KT"

    private lateinit var existingPerson: Person
    private lateinit var existingContact: OffenderContactPerson
    private var bookingId = 0L

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingPerson = person(
          firstName = "JOHN",
          lastName = "BOG",
        )
        offender(nomsId = offenderNo) {
          bookingId = booking {
            existingContact = contact(
              person = existingPerson,
              contactType = "S",
              relationshipType = "SIS",
              active = true,
              expiryDate = null,
              comment = "Best friends",
              nextOfKin = false,
              emergencyContact = false,
              approvedVisitor = false,
            )
            contact(
              person = existingPerson,
              contactType = "S",
              relationshipType = "FRI",
              active = true,
            )
          }.bookingId
        }
      }
    }

    @AfterEach
    fun tearDown() {
      offenderRepository.deleteAll()
      personRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete().uri("/persons/${existingPerson.id}/contact/${existingContact.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete().uri("/persons/${existingPerson.id}/contact/${existingContact.id}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete().uri("/persons/${existingPerson.id}/contact/${existingContact.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `will return 400 if contact exists but on a different person`() {
        assertThat(personRepository.existsById(9999)).isFalse()
        assertThat(personContactRepository.existsById(existingContact.id)).isTrue()
        webTestClient.delete().uri("/persons/9999/contact/${existingContact.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isBadRequest
      }

      @Test
      fun `will return 204 even if contact not found`() {
        assertThat(personContactRepository.existsById(9999)).isFalse()
        webTestClient.delete().uri("/persons/${existingPerson.id}/contact/9999")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isNoContent
        assertThat(personContactRepository.existsById(9999)).isFalse()
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete a contact`() {
        nomisDataBuilder.runInTransaction {
          assertThat(personContactRepository.existsById(existingContact.id)).isTrue()
          assertThat(offenderBookingRepository.findByIdOrNull(bookingId)!!.contacts).anyMatch { it.id == existingContact.id }
          assertThat(personRepository.findByIdOrNull(existingPerson.id)!!.contacts).anyMatch { it.id == existingContact.id }
        }

        webTestClient.delete().uri("/persons/${existingPerson.id}/contact/${existingContact.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isNoContent

        nomisDataBuilder.runInTransaction {
          assertThat(personContactRepository.existsById(existingContact.id)).isFalse()
          assertThat(offenderBookingRepository.findByIdOrNull(bookingId)!!.contacts).noneMatch { it.id == existingContact.id }
          assertThat(personRepository.findByIdOrNull(existingPerson.id)!!.contacts).noneMatch { it.id == existingContact.id }
        }
      }
    }
  }

  @DisplayName("POST /persons/{personId}/address")
  @Nested
  inner class CreatePersonAddress {
    private val validAddressRequest = CreatePersonAddressRequest(
      mailAddress = true,
      primaryAddress = true,
    )

    private lateinit var existingPerson: Person

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingPerson = person(
          firstName = "JOHN",
          lastName = "BOG",
        )
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
        webTestClient.post().uri("/persons/${existingPerson.id}/address")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/address")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/address")
          .bodyValue(validAddressRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when person does not exist`() {
        webTestClient.post().uri("/persons/999/address")
          .bodyValue(validAddressRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 400 when city code does not exist`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/address")
          .bodyValue(validAddressRequest.copy(cityCode = "ZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `return 400 when county code does not exist`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/address")
          .bodyValue(validAddressRequest.copy(countyCode = "ZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `return 400 when country code does not exist`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/address")
          .bodyValue(validAddressRequest.copy(countryCode = "ZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `return 400 when address type code does not exist`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/address")
          .bodyValue(validAddressRequest.copy(typeCode = "ZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will create a address`() {
        val response: CreatePersonAddressResponse = webTestClient.post().uri("/persons/${existingPerson.id}/address")
          .bodyValue(
            validAddressRequest.copy(
              typeCode = "HOME",
              flat = "1A",
              premise = "Bolden Court",
              street = "Fulwood Road",
              locality = "Broomhill",
              cityCode = SHEFFIELD,
              countyCode = "S.YORKSHIRE",
              countryCode = "GBR",
              postcode = "S10 2HH",
              primaryAddress = true,
              mailAddress = true,
              noFixedAddress = false,
              startDate = LocalDate.parse("2001-01-01"),
              endDate = LocalDate.parse("2032-12-31"),
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody(CreatePersonAddressResponse::class.java)
          .returnResult()
          .responseBody!!

        val address = personAddressRepository.findByIdOrNull(response.personAddressId)!!

        with(address) {
          assertThat(addressId).isEqualTo(addressId)
          assertThat(person.id).isEqualTo(existingPerson.id)
          assertThat(addressType?.description).isEqualTo("Home Address")
          assertThat(flat).isEqualTo("1A")
          assertThat(premise).isEqualTo("Bolden Court")
          assertThat(street).isEqualTo("Fulwood Road")
          assertThat(locality).isEqualTo("Broomhill")
          assertThat(city?.description).isEqualTo("Sheffield")
          assertThat(county?.description).isEqualTo("South Yorkshire")
          assertThat(country?.description).isEqualTo("United Kingdom")
          assertThat(primaryAddress).isTrue()
          assertThat(mailAddress).isTrue()
          assertThat(noFixedAddress).isFalse()
          assertThat(startDate).isEqualTo(LocalDate.parse("2001-01-01"))
          assertThat(endDate).isEqualTo(LocalDate.parse("2032-12-31"))
        }

        nomisDataBuilder.runInTransaction {
          val person = personRepository.findByIdOrNull(existingPerson.id)
          assertThat(person?.addresses).anyMatch { it.addressId == address.addressId }
        }
      }
    }
  }

  @DisplayName("PUT /persons/{personId}/address/{addressId}")
  @Nested
  inner class UpdatePersonAddress {
    private val validAddressRequest = UpdatePersonAddressRequest(
      mailAddress = true,
      primaryAddress = true,
    )

    private lateinit var existingPerson: Person
    private lateinit var existingAddress: PersonAddress

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingPerson = person(
          firstName = "JOHN",
          lastName = "BOG",
        ) {
          existingAddress = address()
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
        webTestClient.put().uri("/persons/${existingPerson.id}/address/${existingAddress.addressId}")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/address/${existingAddress.addressId}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/address/${existingAddress.addressId}")
          .bodyValue(validAddressRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when person does not exist`() {
        webTestClient.put().uri("/persons/9999/address/${existingAddress.addressId}")
          .bodyValue(validAddressRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 404 when address does not exist`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/address/99999")
          .bodyValue(validAddressRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 400 when city code does not exist`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/address/${existingAddress.addressId}")
          .bodyValue(validAddressRequest.copy(cityCode = "ZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `return 400 when county code does not exist`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/address/${existingAddress.addressId}")
          .bodyValue(validAddressRequest.copy(countyCode = "ZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `return 400 when country code does not exist`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/address/${existingAddress.addressId}")
          .bodyValue(validAddressRequest.copy(countryCode = "ZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `return 400 when address type code does not exist`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/address/${existingAddress.addressId}")
          .bodyValue(validAddressRequest.copy(typeCode = "ZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will update a address`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/address/${existingAddress.addressId}")
          .bodyValue(
            validAddressRequest.copy(
              typeCode = "HOME",
              flat = "1A",
              premise = "Bolden Court",
              street = "Fulwood Road",
              locality = "Broomhill",
              cityCode = SHEFFIELD,
              countyCode = "S.YORKSHIRE",
              countryCode = "GBR",
              postcode = "S10 2HH",
              primaryAddress = true,
              mailAddress = true,
              noFixedAddress = false,
              startDate = LocalDate.parse("2001-01-01"),
              endDate = LocalDate.parse("2032-12-31"),
              validatedPAF = true,
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk

        val address = personAddressRepository.findByIdOrNull(existingAddress.addressId)!!

        with(address) {
          assertThat(addressId).isEqualTo(addressId)
          assertThat(person.id).isEqualTo(existingPerson.id)
          assertThat(addressType?.description).isEqualTo("Home Address")
          assertThat(flat).isEqualTo("1A")
          assertThat(premise).isEqualTo("Bolden Court")
          assertThat(street).isEqualTo("Fulwood Road")
          assertThat(locality).isEqualTo("Broomhill")
          assertThat(city?.description).isEqualTo("Sheffield")
          assertThat(county?.description).isEqualTo("South Yorkshire")
          assertThat(country?.description).isEqualTo("United Kingdom")
          assertThat(primaryAddress).isTrue()
          assertThat(mailAddress).isTrue()
          assertThat(noFixedAddress).isFalse()
          assertThat(startDate).isEqualTo(LocalDate.parse("2001-01-01"))
          assertThat(endDate).isEqualTo(LocalDate.parse("2032-12-31"))
          assertThat(validatedPAF).isTrue()
        }

        nomisDataBuilder.runInTransaction {
          val person = personRepository.findByIdOrNull(existingPerson.id)
          assertThat(person?.addresses).anyMatch { it.addressId == address.addressId }
        }
      }
    }
  }

  @DisplayName("DELETE /persons/{personId}/address/{addressId}")
  @Nested
  inner class DeletePersonAddress {
    private lateinit var existingPerson: Person
    private lateinit var existingAddress: PersonAddress

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingPerson = person(
          firstName = "JOHN",
          lastName = "BOG",
        ) {
          existingAddress = address()
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
        webTestClient.delete().uri("/persons/${existingPerson.id}/address/${existingAddress.addressId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete().uri("/persons/${existingPerson.id}/address/${existingAddress.addressId}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete().uri("/persons/${existingPerson.id}/address/${existingAddress.addressId}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 400 address exists but  not exist on the person `() {
        webTestClient.delete().uri("/persons/9999/address/${existingAddress.addressId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `return 204 when address does not exist`() {
        webTestClient.delete().uri("/persons/${existingPerson.id}/address/99999")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete a address`() {
        assertThat(personAddressRepository.existsById(existingAddress.addressId)).isTrue()
        webTestClient.delete().uri("/persons/${existingPerson.id}/address/${existingAddress.addressId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isNoContent

        assertThat(personAddressRepository.existsById(existingAddress.addressId)).isFalse()
      }
    }
  }

  @DisplayName("POST /persons/{personId}/email")
  @Nested
  inner class CreatePersonEmail {
    private val validEmailRequest = CreatePersonEmailRequest(
      email = "test@test.com",
    )

    private lateinit var existingPerson: Person

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingPerson = person(
          firstName = "JOHN",
          lastName = "BOG",
        )
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
        webTestClient.post().uri("/persons/${existingPerson.id}/email")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validEmailRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/email")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validEmailRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/email")
          .bodyValue(validEmailRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when person does not exist`() {
        webTestClient.post().uri("/persons/999/email")
          .bodyValue(validEmailRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will create a email`() {
        val response: CreatePersonEmailResponse = webTestClient.post().uri("/persons/${existingPerson.id}/email")
          .bodyValue(
            validEmailRequest.copy(
              email = "test@email.com",
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody(CreatePersonEmailResponse::class.java)
          .returnResult()
          .responseBody!!

        val email = personInternetAddressRepository.findByIdOrNull(response.emailAddressId)!!

        with(email) {
          assertThat(internetAddressId).isEqualTo(response.emailAddressId)
          assertThat(person.id).isEqualTo(existingPerson.id)
          assertThat(email.internetAddress).isEqualTo("test@email.com")
          assertThat(email.internetAddressClass).isEqualTo("EMAIL")
        }

        nomisDataBuilder.runInTransaction {
          val person = personRepository.findByIdOrNull(existingPerson.id)
          assertThat(person?.internetAddresses).anyMatch { it.internetAddressId == email.internetAddressId }
        }
      }
    }
  }

  @DisplayName("PUT /persons/{personId}/email/{emailAddressId}")
  @Nested
  inner class UpdatePersonEmail {
    private val validEmailRequest = CreatePersonEmailRequest(
      email = "test@test.com",
    )

    private lateinit var existingPerson: Person
    private lateinit var existingEmail: PersonInternetAddress

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingPerson = person(
          firstName = "JOHN",
          lastName = "BOG",
        ) {
          existingEmail = email(emailAddress = "test@justice.gov.uk")
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
        webTestClient.put().uri("/persons/${existingPerson.id}/email/${existingEmail.internetAddressId}")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validEmailRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/email/${existingEmail.internetAddressId}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validEmailRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/email/${existingEmail.internetAddressId}")
          .bodyValue(validEmailRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when person does not exist`() {
        webTestClient.put().uri("/persons/99999/email/${existingEmail.internetAddressId}")
          .bodyValue(validEmailRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 404 when email does not exist`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/email/9999")
          .bodyValue(validEmailRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will create a email`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/email/${existingEmail.internetAddressId}")
          .bodyValue(
            validEmailRequest.copy(
              email = "test@email.com",
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk

        val email = personInternetAddressRepository.findByIdOrNull(existingEmail.internetAddressId)!!

        with(email) {
          assertThat(internetAddressId).isEqualTo(existingEmail.internetAddressId)
          assertThat(person.id).isEqualTo(existingPerson.id)
          assertThat(email.internetAddress).isEqualTo("test@email.com")
          assertThat(email.internetAddressClass).isEqualTo("EMAIL")
        }

        nomisDataBuilder.runInTransaction {
          val person = personRepository.findByIdOrNull(existingPerson.id)
          assertThat(person?.internetAddresses).anyMatch { it.internetAddressId == email.internetAddressId }
        }
      }
    }
  }

  @DisplayName("DELETE /persons/{personId}/email/{emailAddressId}")
  @Nested
  inner class DeletePersonEmail {
    private lateinit var existingPerson: Person
    private lateinit var existingEmail: PersonInternetAddress

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingPerson = person(
          firstName = "JOHN",
          lastName = "BOG",
        ) {
          existingEmail = email(emailAddress = "test@justice.gov.uk")
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
        webTestClient.delete().uri("/persons/${existingPerson.id}/email/${existingEmail.internetAddressId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete().uri("/persons/${existingPerson.id}/email/${existingEmail.internetAddressId}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete().uri("/persons/${existingPerson.id}/email/${existingEmail.internetAddressId}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 400 when emails exists but not on the person`() {
        webTestClient.delete().uri("/persons/99999/email/${existingEmail.internetAddressId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `return 204 when email does not exist`() {
        webTestClient.delete().uri("/persons/${existingPerson.id}/email/9999")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will create a email`() {
        assertThat(personInternetAddressRepository.existsById(existingEmail.internetAddressId)).isTrue()
        webTestClient.delete().uri("/persons/${existingPerson.id}/email/${existingEmail.internetAddressId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isNoContent
        assertThat(personInternetAddressRepository.existsById(existingEmail.internetAddressId)).isFalse()
      }
    }
  }

  @DisplayName("POST /persons/{personId}/phone")
  @Nested
  inner class CreatePersonPhone {
    private val validPhoneRequest = CreatePersonPhoneRequest(
      number = "0114 555 5555",
      typeCode = "MOB",
    )

    private lateinit var existingPerson: Person

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingPerson = person(
          firstName = "JOHN",
          lastName = "BOG",
        )
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
        webTestClient.post().uri("/persons/${existingPerson.id}/phone")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/phone")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/phone")
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when person does not exist`() {
        webTestClient.post().uri("/persons/999/phone")
          .bodyValue(validPhoneRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 400 when phone type code does not exist`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/phone")
          .bodyValue(validPhoneRequest.copy(typeCode = "ZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will create a phone`() {
        val response: CreatePersonPhoneResponse = webTestClient.post().uri("/persons/${existingPerson.id}/phone")
          .bodyValue(
            validPhoneRequest.copy(
              number = "07973 555 5555",
              typeCode = "MOB",
              extension = "x555",
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody(CreatePersonPhoneResponse::class.java)
          .returnResult()
          .responseBody!!

        val phone = personPhoneRepository.findByIdOrNull(response.phoneId)!!

        with(phone) {
          assertThat(phoneId).isEqualTo(response.phoneId)
          assertThat(person.id).isEqualTo(existingPerson.id)
          assertThat(phone.phoneNo).isEqualTo("07973 555 5555")
          assertThat(phone.extNo).isEqualTo("x555")
          assertThat(phone.phoneType.description).isEqualTo("Mobile")
        }

        nomisDataBuilder.runInTransaction {
          val person = personRepository.findByIdOrNull(existingPerson.id)
          assertThat(person?.phones).anyMatch { it.phoneId == phone.phoneId }
        }
      }
    }
  }

  @DisplayName("PUT /persons/{personId}/phone/{phoneId}")
  @Nested
  inner class UpdatePersonPhone {
    private val validPhoneRequest = CreatePersonPhoneRequest(
      number = "0114 555 5555",
      typeCode = "MOB",
    )

    private lateinit var existingPerson: Person
    private lateinit var existingPhone: PersonPhone

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingPerson = person(
          firstName = "JOHN",
          lastName = "BOG",
        ) {
          existingPhone = phone(phoneType = "HOME", phoneNo = "0113 4546 4646", extNo = "ext 567")
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
        webTestClient.put().uri("/persons/${existingPerson.id}/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/phone/${existingPhone.phoneId}")
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when person does not exist`() {
        webTestClient.put().uri("/persons/9999/phone/${existingPhone.phoneId}")
          .bodyValue(validPhoneRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 404 when phone does not exist`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/phone/99999")
          .bodyValue(validPhoneRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 400 when phone type code does not exist`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/phone/${existingPhone.phoneId}")
          .bodyValue(validPhoneRequest.copy(typeCode = "ZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will update the phone`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/phone/${existingPhone.phoneId}")
          .bodyValue(
            validPhoneRequest.copy(
              number = "07973 555 5555",
              typeCode = "MOB",
              extension = "x555",
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk

        val phone = personPhoneRepository.findByIdOrNull(existingPhone.phoneId)!!

        with(phone) {
          assertThat(phoneId).isEqualTo(existingPhone.phoneId)
          assertThat(person.id).isEqualTo(existingPerson.id)
          assertThat(phone.phoneNo).isEqualTo("07973 555 5555")
          assertThat(phone.extNo).isEqualTo("x555")
          assertThat(phone.phoneType.description).isEqualTo("Mobile")
        }

        nomisDataBuilder.runInTransaction {
          val person = personRepository.findByIdOrNull(existingPerson.id)
          assertThat(person?.phones).anyMatch { it.phoneId == phone.phoneId }
        }
      }
    }
  }

  @DisplayName("DELETE /persons/{personId}/phone/{phoneId}")
  @Nested
  inner class DeletePersonPhone {
    private lateinit var existingPerson: Person
    private lateinit var existingPhone: PersonPhone

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingPerson = person(
          firstName = "JOHN",
          lastName = "BOG",
        ) {
          existingPhone = phone(phoneType = "HOME", phoneNo = "0113 4546 4646", extNo = "ext 567")
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
        webTestClient.delete().uri("/persons/${existingPerson.id}/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete().uri("/persons/${existingPerson.id}/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete().uri("/persons/${existingPerson.id}/phone/${existingPhone.phoneId}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 400 when phone exists but not on that person`() {
        webTestClient.delete().uri("/persons/9999/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `return 204 when phone does not exist`() {
        webTestClient.delete().uri("/persons/${existingPerson.id}/phone/99999")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete the phone`() {
        assertThat(personPhoneRepository.existsById(existingPhone.phoneId)).isTrue()
        webTestClient.delete().uri("/persons/${existingPerson.id}/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isNoContent
        assertThat(personPhoneRepository.existsById(existingPhone.phoneId)).isFalse()
      }
    }
  }

  @DisplayName("POST /persons/{personId}/address/{addressId}/phone")
  @Nested
  inner class CreatePersonAddressPhone {
    private val validPhoneRequest = CreatePersonPhoneRequest(
      number = "0114 555 5555",
      typeCode = "MOB",
    )

    private lateinit var existingPerson: Person
    private lateinit var anotherPerson: Person
    private lateinit var existingAddress: PersonAddress

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        anotherPerson = person(
          firstName = "JANE",
          lastName = "BOG",
        )
        existingPerson = person(
          firstName = "JOHN",
          lastName = "BOG",
        ) {
          existingAddress = address { }
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
        webTestClient.post().uri("/persons/${existingPerson.id}/address/${existingAddress.addressId}/phone")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/address/${existingAddress.addressId}/phone")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/address/${existingAddress.addressId}/phone")
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when person does not exist`() {
        webTestClient.post().uri("/persons/999/address/${existingAddress.addressId}/phone")
          .bodyValue(validPhoneRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 404 when address does not exist`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/address/999/phone")
          .bodyValue(validPhoneRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 404 when address does not exist on person`() {
        webTestClient.post().uri("/persons/${anotherPerson.id}/address/${existingAddress.addressId}/phone")
          .bodyValue(validPhoneRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 400 when phone type code does not exist`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/address/${existingAddress.addressId}/phone")
          .bodyValue(validPhoneRequest.copy(typeCode = "ZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will create a phone`() {
        val response: CreatePersonPhoneResponse = webTestClient.post().uri("/persons/${existingPerson.id}/address/${existingAddress.addressId}/phone")
          .bodyValue(
            validPhoneRequest.copy(
              number = "07973 555 5555",
              typeCode = "MOB",
              extension = "x555",
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody(CreatePersonPhoneResponse::class.java)
          .returnResult()
          .responseBody!!

        val phone = addressPhoneRepository.findByIdOrNull(response.phoneId)!!

        with(phone) {
          assertThat(phoneId).isEqualTo(response.phoneId)
          assertThat(address.addressId).isEqualTo(existingAddress.addressId)
          assertThat(phone.phoneNo).isEqualTo("07973 555 5555")
          assertThat(phone.extNo).isEqualTo("x555")
          assertThat(phone.phoneType.description).isEqualTo("Mobile")
        }

        nomisDataBuilder.runInTransaction {
          val person = personRepository.findByIdOrNull(existingPerson.id)
          val address = person!!.addresses.find { it.addressId == existingAddress.addressId }
          assertThat(address?.phones).anyMatch { it.phoneId == phone.phoneId }
        }
      }
    }
  }

  @DisplayName("PUT /persons/{personId}/address/{addressId}/phone/{phoneId}")
  @Nested
  inner class UpdatePersonAddressPhone {
    private val validPhoneRequest = CreatePersonPhoneRequest(
      number = "0114 555 5555",
      typeCode = "MOB",
    )

    private lateinit var existingPerson: Person
    private lateinit var existingAddress: PersonAddress
    private lateinit var existingPhone: AddressPhone

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingPerson = person(
          firstName = "JOHN",
          lastName = "BOG",
        ) {
          existingAddress = address {
            existingPhone = phone(phoneType = "HOME", phoneNo = "0113 4546 4646", extNo = "ext 567")
          }
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
        webTestClient.put().uri("/persons/${existingPerson.id}/address/${existingAddress.addressId}/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/address/${existingAddress.addressId}/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/address/${existingAddress.addressId}/phone/${existingPhone.phoneId}")
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when person does not exist`() {
        webTestClient.put().uri("/persons/9999/address/${existingAddress.addressId}/phone/${existingPhone.phoneId}")
          .bodyValue(validPhoneRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 404 when address does not exist`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/address/99999/phone/${existingPhone.phoneId}")
          .bodyValue(validPhoneRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 404 when phone does not exist`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/address/${existingAddress.addressId}/phone/99999")
          .bodyValue(validPhoneRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 400 when phone type code does not exist`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/address/${existingAddress.addressId}/phone/${existingPhone.phoneId}")
          .bodyValue(validPhoneRequest.copy(typeCode = "ZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will update the phone`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/address/${existingAddress.addressId}/phone/${existingPhone.phoneId}")
          .bodyValue(
            validPhoneRequest.copy(
              number = "07973 555 5555",
              typeCode = "MOB",
              extension = "x555",
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk

        val phone = addressPhoneRepository.findByIdOrNull(existingPhone.phoneId)!!

        with(phone) {
          assertThat(phoneId).isEqualTo(existingPhone.phoneId)
          assertThat(address.addressId).isEqualTo(existingAddress.addressId)
          assertThat(phone.phoneNo).isEqualTo("07973 555 5555")
          assertThat(phone.extNo).isEqualTo("x555")
          assertThat(phone.phoneType.description).isEqualTo("Mobile")
        }

        nomisDataBuilder.runInTransaction {
          val person = personRepository.findByIdOrNull(existingPerson.id)
          val address = person!!.addresses.find { it.addressId == existingAddress.addressId }
          assertThat(address?.phones).anyMatch { it.phoneId == phone.phoneId }
        }
      }
    }
  }

  @DisplayName("DELETE /persons/{personId}/address/{addressId}/phone/{phoneId}")
  @Nested
  inner class DeletePersonAddressPhone {
    private lateinit var existingPerson: Person
    private lateinit var existingAddress: PersonAddress
    private lateinit var existingPhone: AddressPhone

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingPerson = person(
          firstName = "JOHN",
          lastName = "BOG",
        ) {
          existingAddress = address {
            existingPhone = phone(phoneType = "HOME", phoneNo = "0113 4546 4646", extNo = "ext 567")
          }
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
        webTestClient.delete().uri("/persons/${existingPerson.id}/address/${existingAddress.addressId}/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete().uri("/persons/${existingPerson.id}/address/${existingAddress.addressId}/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete().uri("/persons/${existingPerson.id}/address/${existingAddress.addressId}/phone/${existingPhone.phoneId}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 400 when phone exists on address but address does not belong to person`() {
        webTestClient.delete().uri("/persons/9999/address/${existingAddress.addressId}/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `return 400 when phone exists but not on address`() {
        webTestClient.delete().uri("/persons/${existingPerson.id}/address/99999/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `return 204 when phone does not exist`() {
        webTestClient.delete().uri("/persons/${existingPerson.id}/address/${existingAddress.addressId}/phone/99999")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete the phone`() {
        assertThat(addressPhoneRepository.existsById(existingPhone.phoneId)).isTrue()
        webTestClient.delete().uri("/persons/${existingPerson.id}/address/${existingAddress.addressId}/phone/${existingPhone.phoneId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isNoContent
        assertThat(addressPhoneRepository.existsById(existingPhone.phoneId)).isFalse()
      }
    }
  }

  @DisplayName("POST /persons/{personId}/identifier")
  @Nested
  inner class CreatePersonIdentifier {
    private val validIdentifierRequest = CreatePersonIdentifierRequest(
      identifier = "SMITY52552DL",
      typeCode = "DL",
      issuedAuthority = "DVLA",
    )

    private lateinit var existingPerson: Person

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingPerson = person(
          firstName = "JOHN",
          lastName = "BOG",
        )
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
        webTestClient.post().uri("/persons/${existingPerson.id}/identifier")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validIdentifierRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/identifier")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validIdentifierRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/identifier")
          .bodyValue(validIdentifierRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when person does not exist`() {
        webTestClient.post().uri("/persons/999/identifier")
          .bodyValue(validIdentifierRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 400 when identifier type code does not exist`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/identifier")
          .bodyValue(validIdentifierRequest.copy(typeCode = "ZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will create a identifier`() {
        val response: CreatePersonIdentifierResponse = webTestClient.post().uri("/persons/${existingPerson.id}/identifier")
          .bodyValue(
            validIdentifierRequest.copy(
              identifier = "SMITY52552DL",
              typeCode = "DL",
              issuedAuthority = "DVLA",
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody(CreatePersonIdentifierResponse::class.java)
          .returnResult()
          .responseBody!!

        val identifier = personIdentifierRepository.findByIdOrNull(PersonIdentifierPK(person = existingPerson, sequence = response.sequence))!!

        with(identifier) {
          assertThat(id.sequence).isEqualTo(response.sequence)
          assertThat(id.person.id).isEqualTo(existingPerson.id)
          assertThat(this.identifier).isEqualTo("SMITY52552DL")
          assertThat(identifierType.description).isEqualTo("Driving Licence")
          assertThat(issuedAuthority).isEqualTo("DVLA")
        }

        nomisDataBuilder.runInTransaction {
          val person = personRepository.findByIdOrNull(existingPerson.id)
          assertThat(person?.identifiers).anyMatch { it.id.sequence == identifier.id.sequence }
        }
      }
    }
  }

  @DisplayName("PUT /persons/{personId}/identifier/{sequence}")
  @Nested
  inner class UpdatePersonIdentifier {
    private val validIdentifierRequest = UpdatePersonIdentifierRequest(
      identifier = "SMITY52552DL",
      typeCode = "DL",
      issuedAuthority = "DVLA",
    )

    private lateinit var existingPerson: Person
    private lateinit var existingIdentifier: PersonIdentifier

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingPerson = person(
          firstName = "JOHN",
          lastName = "BOG",
        ) {
          existingIdentifier = identifier(type = "NINO", identifier = "NE121212K", issuedAuthority = "HMRC")
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
        webTestClient.put().uri("/persons/${existingPerson.id}/identifier/${existingIdentifier.id.sequence}")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validIdentifierRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/identifier/${existingIdentifier.id.sequence}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validIdentifierRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/identifier/${existingIdentifier.id.sequence}")
          .bodyValue(validIdentifierRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when person does not exist`() {
        webTestClient.put().uri("/persons/99999/identifier/${existingIdentifier.id.sequence}")
          .bodyValue(validIdentifierRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 404 when sequence of identifier does not exist`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/identifier/9999")
          .bodyValue(validIdentifierRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 400 when identifier type code does not exist`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/identifier")
          .bodyValue(validIdentifierRequest.copy(typeCode = "ZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will update a identifier`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/identifier/${existingIdentifier.id.sequence}")
          .bodyValue(
            validIdentifierRequest.copy(
              identifier = "SMITY52552DL",
              typeCode = "DL",
              issuedAuthority = "DVLA",
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk

        val identifier = personIdentifierRepository.findByIdOrNull(PersonIdentifierPK(person = existingPerson, sequence = existingIdentifier.id.sequence))!!

        with(identifier) {
          assertThat(id.sequence).isEqualTo(existingIdentifier.id.sequence)
          assertThat(id.person.id).isEqualTo(existingPerson.id)
          assertThat(this.identifier).isEqualTo("SMITY52552DL")
          assertThat(identifierType.description).isEqualTo("Driving Licence")
          assertThat(issuedAuthority).isEqualTo("DVLA")
        }

        nomisDataBuilder.runInTransaction {
          val person = personRepository.findByIdOrNull(existingPerson.id)
          assertThat(person?.identifiers).anyMatch { it.id.sequence == identifier.id.sequence }
        }
      }
    }
  }

  @DisplayName("DELETE /persons/{personId}/identifier/{sequence}")
  @Nested
  inner class DeletePersonIdentifier {
    private lateinit var existingPerson: Person
    private lateinit var existingIdentifier: PersonIdentifier

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingPerson = person(
          firstName = "JOHN",
          lastName = "BOG",
        ) {
          existingIdentifier = identifier(type = "NINO", identifier = "NE121212K", issuedAuthority = "HMRC")
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
        webTestClient.delete().uri("/persons/${existingPerson.id}/identifier/${existingIdentifier.id.sequence}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete().uri("/persons/${existingPerson.id}/identifier/${existingIdentifier.id.sequence}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete().uri("/persons/${existingPerson.id}/identifier/${existingIdentifier.id.sequence}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when person does not exist`() {
        webTestClient.delete().uri("/persons/99999/identifier/${existingIdentifier.id.sequence}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 204 when sequence of identifier does not exist`() {
        webTestClient.delete().uri("/persons/${existingPerson.id}/identifier/9999")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete a identifier`() {
        assertThat(
          personIdentifierRepository.existsById(
            PersonIdentifierPK(
              person = existingPerson,
              sequence = existingIdentifier.id.sequence,
            ),
          ),
        ).isTrue()
        webTestClient.delete().uri("/persons/${existingPerson.id}/identifier/${existingIdentifier.id.sequence}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isNoContent
        assertThat(
          personIdentifierRepository.existsById(
            PersonIdentifierPK(
              person = existingPerson,
              sequence = existingIdentifier.id.sequence,
            ),
          ),
        ).isFalse()
      }
    }
  }

  @DisplayName("POST /persons/{personId}/employment")
  @Nested
  inner class CreatePersonEmployment {
    private lateinit var validEmploymentRequest: CreatePersonEmploymentRequest
    private lateinit var existingPerson: Person
    private lateinit var corporate: Corporate

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        corporate = corporate(corporateName = "Police")
        existingPerson = person(
          firstName = "JOHN",
          lastName = "BOG",
        )
      }

      validEmploymentRequest = CreatePersonEmploymentRequest(
        corporateId = corporate.id,
        active = true,
      )
    }

    @AfterEach
    fun tearDown() {
      personRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/employment")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validEmploymentRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/employment")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validEmploymentRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/employment")
          .bodyValue(validEmploymentRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when person does not exist`() {
        webTestClient.post().uri("/persons/999/employment")
          .bodyValue(validEmploymentRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 400 when corporate Id does not exist`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/employment")
          .bodyValue(validEmploymentRequest.copy(corporateId = 9999))
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will create a employment`() {
        val response: CreatePersonEmploymentResponse = webTestClient.post().uri("/persons/${existingPerson.id}/employment")
          .bodyValue(validEmploymentRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody(CreatePersonEmploymentResponse::class.java)
          .returnResult()
          .responseBody!!

        val employment = personEmploymentRepository.findByIdOrNull(PersonEmploymentPK(person = existingPerson, sequence = response.sequence))!!

        with(employment) {
          assertThat(id.sequence).isEqualTo(response.sequence)
          assertThat(id.person.id).isEqualTo(existingPerson.id)
          assertThat(this.active).isTrue
          assertThat(this.employerCorporate.id).isEqualTo(validEmploymentRequest.corporateId)
        }

        nomisDataBuilder.runInTransaction {
          val person = personRepository.findByIdOrNull(existingPerson.id)
          assertThat(person?.employments).anyMatch { it.id.sequence == employment.id.sequence }
        }
      }
    }
  }

  @DisplayName("PUT /persons/{personId}/employment/{sequence}")
  @Nested
  inner class UpdatePersonEmployment {
    private lateinit var validEmploymentRequest: UpdatePersonEmploymentRequest
    private lateinit var corporate: Corporate
    private lateinit var existingPerson: Person
    private lateinit var existingEmployment: uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonEmployment

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        corporate = corporate(corporateName = "Police")
        existingPerson = person(
          firstName = "JOHN",
          lastName = "BOG",
        ) {
          existingEmployment = employment(
            employerCorporate = corporate,
            active = true,
          )
        }
      }
      validEmploymentRequest = UpdatePersonEmploymentRequest(
        corporateId = corporate.id,
        active = true,
      )
    }

    @AfterEach
    fun tearDown() {
      personRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/employment/${existingEmployment.id.sequence}")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validEmploymentRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/employment/${existingEmployment.id.sequence}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validEmploymentRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/employment/${existingEmployment.id.sequence}")
          .bodyValue(validEmploymentRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when person does not exist`() {
        webTestClient.put().uri("/persons/99999/employment/${existingEmployment.id.sequence}")
          .bodyValue(validEmploymentRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 404 when sequence of employment does not exist`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/employment/9999")
          .bodyValue(validEmploymentRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 400 when employment type code does not exist`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/employment")
          .bodyValue(validEmploymentRequest.copy(corporateId = 999))
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will update a employment`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/employment/${existingEmployment.id.sequence}")
          .bodyValue(validEmploymentRequest.copy(active = false))
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk

        val employment = personEmploymentRepository.findByIdOrNull(PersonEmploymentPK(person = existingPerson, sequence = existingEmployment.id.sequence))!!

        with(employment) {
          assertThat(id.sequence).isEqualTo(existingEmployment.id.sequence)
          assertThat(id.person.id).isEqualTo(existingPerson.id)
          assertThat(this.employerCorporate.id).isEqualTo(corporate.id)
          assertThat(this.active).isFalse
        }

        nomisDataBuilder.runInTransaction {
          val person = personRepository.findByIdOrNull(existingPerson.id)
          assertThat(person?.employments).anyMatch { it.id.sequence == employment.id.sequence }
        }
      }
    }
  }

  @DisplayName("DELETE /persons/{personId}/employment/{sequence}")
  @Nested
  inner class DeletePersonEmployment {
    private lateinit var existingPerson: Person
    private lateinit var existingEmployment: uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonEmployment

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        val corporate = corporate(corporateName = "Police")
        existingPerson = person(
          firstName = "JOHN",
          lastName = "BOG",
        ) {
          existingEmployment = employment(
            employerCorporate = corporate,
            active = true,
          )
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
        webTestClient.delete().uri("/persons/${existingPerson.id}/employment/${existingEmployment.id.sequence}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete().uri("/persons/${existingPerson.id}/employment/${existingEmployment.id.sequence}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete().uri("/persons/${existingPerson.id}/employment/${existingEmployment.id.sequence}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when person does not exist`() {
        webTestClient.delete().uri("/persons/99999/employment/${existingEmployment.id.sequence}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 204 when sequence of employment does not exist`() {
        webTestClient.delete().uri("/persons/${existingPerson.id}/employment/9999")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete a employment`() {
        assertThat(
          personEmploymentRepository.existsById(
            PersonEmploymentPK(
              person = existingPerson,
              sequence = existingEmployment.id.sequence,
            ),
          ),
        ).isTrue()
        webTestClient.delete().uri("/persons/${existingPerson.id}/employment/${existingEmployment.id.sequence}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isNoContent
        assertThat(
          personEmploymentRepository.existsById(
            PersonEmploymentPK(
              person = existingPerson,
              sequence = existingEmployment.id.sequence,
            ),
          ),
        ).isFalse()
      }
    }
  }

  @DisplayName("POST /persons/{personId}/contact/{contactId}/restriction")
  @Nested
  inner class CreatePersonContactRestriction {
    private val restrictionRequest = CreateContactPersonRestrictionRequest(
      typeCode = "BAN",
      comment = "Banned for life",
      effectiveDate = LocalDate.parse("2020-01-01"),
      expiryDate = LocalDate.parse("2026-01-02"),
      enteredStaffUsername = "J.SPEAK",
    )

    private lateinit var staff: Staff
    private lateinit var existingPerson: Person
    private lateinit var anotherPerson: Person
    private lateinit var existingContact: OffenderContactPerson

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        staff = staff(firstName = "KOFE", lastName = "ADDY") {
          account(username = "KOFEADDY_GEN", type = GENERAL)
          account(username = "KOFEADDY_ADM", type = ADMIN)
        }
        anotherPerson = person { }
        existingPerson = person(
          firstName = "JOHN",
          lastName = "BOG",
        )
        offender(nomsId = "A1234KT") {
          booking {
            existingContact = contact(person = existingPerson, contactType = "S", relationshipType = "SIS")
          }
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
        webTestClient.post().uri("/persons/${existingPerson.id}/contact/${existingContact.id}/restriction")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(restrictionRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/contact/${existingContact.id}/restriction")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(restrictionRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/contact/${existingContact.id}/restriction")
          .bodyValue(restrictionRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when person does not exist`() {
        webTestClient.post().uri("/persons/999/contact/${existingContact.id}/restriction")
          .bodyValue(restrictionRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 404 when contact does not exist`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/contact/999/restriction")
          .bodyValue(restrictionRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 404 when contact does not exist on person`() {
        webTestClient.post().uri("/persons/${anotherPerson.id}/contact/${existingContact.id}/restriction")
          .bodyValue(restrictionRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 400 when restriction type code does not exist`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/contact/${existingContact.id}/restriction")
          .bodyValue(restrictionRequest.copy(typeCode = "ZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `return 400 when staff username code does not exist`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/contact/${existingContact.id}/restriction")
          .bodyValue(restrictionRequest.copy(enteredStaffUsername = "ZZZZZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will create a restriction`() {
        val response: CreateContactPersonRestrictionResponse = webTestClient.post().uri("/persons/${existingPerson.id}/contact/${existingContact.id}/restriction")
          .bodyValue(
            restrictionRequest.copy(
              comment = "Banned for life",
              typeCode = "BAN",
              effectiveDate = LocalDate.parse("2020-01-01"),
              expiryDate = LocalDate.parse("2026-01-02"),
              enteredStaffUsername = "KOFEADDY_GEN",
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody(CreateContactPersonRestrictionResponse::class.java)
          .returnResult()
          .responseBody!!

        val restriction = personContactRestrictionRepository.findByIdOrNull(response.id)!!

        with(restriction) {
          assertThat(comment).isEqualTo("Banned for life")
          assertThat(effectiveDate).isEqualTo(LocalDate.parse("2020-01-01"))
          assertThat(expiryDate).isEqualTo(LocalDate.parse("2026-01-02"))
          assertThat(restrictionType.description).isEqualTo("Banned")
          assertThat(enteredStaff.id).isEqualTo(staff.id)
        }

        nomisDataBuilder.runInTransaction {
          val person = personRepository.findByIdOrNull(existingPerson.id)
          val contact = person!!.contacts.find { it.id == existingContact.id }
          assertThat(contact?.restrictions).anyMatch { it.id == restriction.id }
        }
      }
    }
  }

  @DisplayName("PUT /persons/{personId}/contact/{contactId}/restriction/{restrictionId}")
  @Nested
  inner class UpdatePersonContactRestriction {
    private val restrictionRequest = CreateContactPersonRestrictionRequest(
      typeCode = "BAN",
      comment = "Banned for life",
      effectiveDate = LocalDate.parse("2020-01-01"),
      expiryDate = LocalDate.parse("2026-01-02"),
      enteredStaffUsername = "J.SPEAK",
    )

    private lateinit var staff: Staff
    private lateinit var existingPerson: Person
    private lateinit var existingContact: OffenderContactPerson
    private lateinit var createdByStaff: Staff
    private lateinit var existingRestriction: OffenderPersonRestrict

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        staff = staff(firstName = "KOFE", lastName = "ADDY") {
          account(username = "KOFEADDY_GEN", type = GENERAL)
        }
        createdByStaff = staff(firstName = "ANDY", lastName = "SMITH") {
          account(username = "ANDY.SMITH", type = GENERAL)
        }
        existingPerson = person(
          firstName = "JOHN",
          lastName = "BOG",
        )
        offender(nomsId = "A1234KT") {
          booking {
            existingContact = contact(person = existingPerson, contactType = "S", relationshipType = "SIS") {
              existingRestriction = restriction(
                restrictionType = "CCTV",
                enteredStaff = createdByStaff,
                effectiveDate = "2020-12-12",
                expiryDate = "2026-12-12",
                comment = "Watch him",
              )
            }
          }
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
        webTestClient.put().uri("/persons/${existingPerson.id}/contact/${existingContact.id}/restriction/${existingRestriction.id}")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(restrictionRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/contact/${existingContact.id}/restriction/${existingRestriction.id}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(restrictionRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/contact/${existingContact.id}/restriction/${existingRestriction.id}")
          .bodyValue(restrictionRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when person does not exist`() {
        webTestClient.put().uri("/persons/999/contact/${existingContact.id}/restriction/${existingRestriction.id}")
          .bodyValue(restrictionRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 404 when contact does not exist`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/contact/9999/restriction/${existingRestriction.id}")
          .bodyValue(restrictionRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 404 when contact restriction does not exist`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/contact/${existingContact.id}/restriction/9999")
          .bodyValue(restrictionRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 400 when restriction type code does not exist`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/contact/${existingContact.id}/restriction/${existingRestriction.id}")
          .bodyValue(restrictionRequest.copy(typeCode = "ZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `return 400 when staff username code does not exist`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/contact/${existingContact.id}/restriction/${existingRestriction.id}")
          .bodyValue(restrictionRequest.copy(enteredStaffUsername = "ZZZZZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will update a restriction`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/contact/${existingContact.id}/restriction/${existingRestriction.id}")
          .bodyValue(
            restrictionRequest.copy(
              comment = "Banned for life",
              typeCode = "BAN",
              effectiveDate = LocalDate.parse("2020-01-01"),
              expiryDate = LocalDate.parse("2026-01-02"),
              enteredStaffUsername = "KOFEADDY_GEN",
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk

        val restriction = personContactRestrictionRepository.findByIdOrNull(existingRestriction.id)!!

        with(restriction) {
          assertThat(comment).isEqualTo("Banned for life")
          assertThat(effectiveDate).isEqualTo(LocalDate.parse("2020-01-01"))
          assertThat(expiryDate).isEqualTo(LocalDate.parse("2026-01-02"))
          assertThat(restrictionType.description).isEqualTo("Banned")
          assertThat(enteredStaff.id).isEqualTo(staff.id)
        }

        nomisDataBuilder.runInTransaction {
          val person = personRepository.findByIdOrNull(existingPerson.id)
          val contact = person!!.contacts.find { it.id == existingContact.id }
          assertThat(contact?.restrictions).anyMatch { it.id == restriction.id }
        }
      }
    }
  }

  @DisplayName("DELETE /persons/{personId}/contact/{contactId}/restriction/{restrictionId}")
  @Nested
  inner class DeletePersonContactRestriction {
    private lateinit var staff: Staff
    private lateinit var existingPerson: Person
    private lateinit var existingContact: OffenderContactPerson
    private lateinit var createdByStaff: Staff
    private lateinit var existingRestriction: OffenderPersonRestrict

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        staff = staff(firstName = "KOFE", lastName = "ADDY") {
          account(username = "KOFEADDY_GEN", type = GENERAL)
        }
        createdByStaff = staff(firstName = "ANDY", lastName = "SMITH") {
          account(username = "ANDY.SMITH", type = GENERAL)
        }
        existingPerson = person(
          firstName = "JOHN",
          lastName = "BOG",
        )
        offender(nomsId = "A1234KT") {
          booking {
            existingContact = contact(person = existingPerson, contactType = "S", relationshipType = "SIS") {
              existingRestriction = restriction(
                restrictionType = "CCTV",
                enteredStaff = createdByStaff,
                effectiveDate = "2020-12-12",
                expiryDate = "2026-12-12",
                comment = "Watch him",
              )
            }
          }
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
        webTestClient.delete().uri("/persons/${existingPerson.id}/contact/${existingContact.id}/restriction/${existingRestriction.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete().uri("/persons/${existingPerson.id}/contact/${existingContact.id}/restriction/${existingRestriction.id}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete().uri("/persons/${existingPerson.id}/contact/${existingContact.id}/restriction/${existingRestriction.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 400 when restriction and contact exist but not on the person`() {
        webTestClient.delete().uri("/persons/999/contact/${existingContact.id}/restriction/${existingRestriction.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `return 400 when restriction exists but not on contact`() {
        webTestClient.delete().uri("/persons/${existingPerson.id}/contact/9999/restriction/${existingRestriction.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `return 204 when contact restriction does not exist`() {
        webTestClient.delete().uri("/persons/${existingPerson.id}/contact/${existingContact.id}/restriction/9999")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete a restriction`() {
        assertThat(personContactRestrictionRepository.existsById(existingRestriction.id)).isTrue()
        webTestClient.delete().uri("/persons/${existingPerson.id}/contact/${existingContact.id}/restriction/${existingRestriction.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isNoContent
        assertThat(personContactRestrictionRepository.existsById(existingRestriction.id)).isFalse()
      }
    }
  }

  @DisplayName("POST /persons/{personId}/restriction")
  @Nested
  inner class CreatePersonRestriction {
    private val restrictionRequest = CreateContactPersonRestrictionRequest(
      typeCode = "BAN",
      comment = "Banned for life",
      effectiveDate = LocalDate.parse("2020-01-01"),
      expiryDate = LocalDate.parse("2026-01-02"),
      enteredStaffUsername = "KOFEADDY_GEN",
    )

    private lateinit var staff: Staff
    private lateinit var existingPerson: Person

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        staff = staff(firstName = "KOFE", lastName = "ADDY") {
          account(username = "KOFEADDY_GEN", type = GENERAL)
          account(username = "KOFEADDY_ADM", type = ADMIN)
        }
        existingPerson = person(
          firstName = "JOHN",
          lastName = "BOG",
        )
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
        webTestClient.post().uri("/persons/${existingPerson.id}/restriction")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(restrictionRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/restriction")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(restrictionRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/restriction")
          .bodyValue(restrictionRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when person does not exist`() {
        webTestClient.post().uri("/persons/999/restriction")
          .bodyValue(restrictionRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 400 when restriction type code does not exist`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/restriction")
          .bodyValue(restrictionRequest.copy(typeCode = "ZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `return 400 when staff username code does not exist`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/restriction")
          .bodyValue(restrictionRequest.copy(enteredStaffUsername = "ZZZZZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will create a restriction`() {
        val response: CreateContactPersonRestrictionResponse = webTestClient.post().uri("/persons/${existingPerson.id}/restriction")
          .bodyValue(
            restrictionRequest.copy(
              comment = "Banned for life",
              typeCode = "BAN",
              effectiveDate = LocalDate.parse("2020-01-01"),
              expiryDate = LocalDate.parse("2026-01-02"),
              enteredStaffUsername = "KOFEADDY_GEN",
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBody(CreateContactPersonRestrictionResponse::class.java)
          .returnResult()
          .responseBody!!

        val restriction = personRestrictionRepository.findByIdOrNull(response.id)!!

        with(restriction) {
          assertThat(comment).isEqualTo("Banned for life")
          assertThat(effectiveDate).isEqualTo(LocalDate.parse("2020-01-01"))
          assertThat(expiryDate).isEqualTo(LocalDate.parse("2026-01-02"))
          assertThat(restrictionType.description).isEqualTo("Banned")
          assertThat(enteredStaff.id).isEqualTo(staff.id)
        }

        nomisDataBuilder.runInTransaction {
          val person = personRepository.findByIdOrNull(existingPerson.id)
          assertThat(person?.restrictions).anyMatch { it.id == restriction.id }
        }
      }
    }
  }

  @DisplayName("PUT /persons/{personId}/restriction/{restrictionId}")
  @Nested
  inner class UpdatePersonRestriction {
    private val restrictionRequest = UpdateContactPersonRestrictionRequest(
      typeCode = "BAN",
      comment = "Banned for life",
      effectiveDate = LocalDate.parse("2020-01-01"),
      expiryDate = LocalDate.parse("2026-01-02"),
      enteredStaffUsername = "KOFEADDY_GEN",
    )

    private lateinit var staff: Staff
    private lateinit var existingPerson: Person
    private lateinit var existingRestriction: VisitorRestriction
    private lateinit var createdByStaff: Staff

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        staff = staff(firstName = "KOFE", lastName = "ADDY") {
          account(username = "KOFEADDY_GEN", type = GENERAL)
        }
        createdByStaff = staff(firstName = "ANDY", lastName = "SMITH") {
          account(username = "ANDY.SMITH", type = GENERAL)
        }
        existingPerson = person(
          firstName = "JOHN",
          lastName = "BOG",
        ) {
          existingRestriction = restriction(
            restrictionType = "CCTV",
            enteredStaff = createdByStaff,
            effectiveDate = "2020-12-12",
            expiryDate = "2026-12-12",
            comment = "Watch him",
          )
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
        webTestClient.put().uri("/persons/${existingPerson.id}/restriction/${existingRestriction.id}")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(restrictionRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/restriction/${existingRestriction.id}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(restrictionRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/restriction/${existingRestriction.id}")
          .bodyValue(restrictionRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 404 when person does not exist`() {
        webTestClient.put().uri("/persons/999/restriction/${existingRestriction.id}")
          .bodyValue(restrictionRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 404 when restriction does not exist`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/restriction/9999")
          .bodyValue(restrictionRequest)
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 400 when restriction type code does not exist`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/restriction/${existingRestriction.id}")
          .bodyValue(restrictionRequest.copy(typeCode = "ZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `return 400 when staff username code does not exist`() {
        webTestClient.post().uri("/persons/${existingPerson.id}/restriction")
          .bodyValue(restrictionRequest.copy(enteredStaffUsername = "ZZZZZZ"))
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will update a restriction`() {
        webTestClient.put().uri("/persons/${existingPerson.id}/restriction/${existingRestriction.id}")
          .bodyValue(
            restrictionRequest.copy(
              comment = "Banned for life",
              typeCode = "BAN",
              effectiveDate = LocalDate.parse("2020-01-01"),
              expiryDate = LocalDate.parse("2026-01-02"),
              enteredStaffUsername = "KOFEADDY_GEN",
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isOk

        val restriction = personRestrictionRepository.findByIdOrNull(existingRestriction.id)!!

        with(restriction) {
          assertThat(comment).isEqualTo("Banned for life")
          assertThat(effectiveDate).isEqualTo(LocalDate.parse("2020-01-01"))
          assertThat(expiryDate).isEqualTo(LocalDate.parse("2026-01-02"))
          assertThat(restrictionType.description).isEqualTo("Banned")
          assertThat(enteredStaff.id).isEqualTo(staff.id)
        }

        nomisDataBuilder.runInTransaction {
          val person = personRepository.findByIdOrNull(existingPerson.id)
          assertThat(person?.restrictions).anyMatch { it.id == restriction.id }
        }
      }
    }
  }

  @DisplayName("DELETE /persons/{personId}/restriction/{restrictionId}")
  @Nested
  inner class DeletePersonRestriction {
    private lateinit var staff: Staff
    private lateinit var existingPerson: Person
    private lateinit var existingRestriction: VisitorRestriction
    private lateinit var createdByStaff: Staff

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        staff = staff(firstName = "KOFE", lastName = "ADDY") {
          account(username = "KOFEADDY_GEN", type = GENERAL)
        }
        createdByStaff = staff(firstName = "ANDY", lastName = "SMITH") {
          account(username = "ANDY.SMITH", type = GENERAL)
        }
        existingPerson = person(
          firstName = "JOHN",
          lastName = "BOG",
        ) {
          existingRestriction = restriction(
            restrictionType = "CCTV",
            enteredStaff = createdByStaff,
            effectiveDate = "2020-12-12",
            expiryDate = "2026-12-12",
            comment = "Watch him",
          )
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
        webTestClient.delete().uri("/persons/${existingPerson.id}/restriction/${existingRestriction.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete().uri("/persons/${existingPerson.id}/restriction/${existingRestriction.id}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete().uri("/persons/${existingPerson.id}/restriction/${existingRestriction.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `return 400 when restriction exists but not on the person`() {
        webTestClient.delete().uri("/persons/999/restriction/${existingRestriction.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `return 204 when restriction does not exist`() {
        webTestClient.delete().uri("/persons/${existingPerson.id}/restriction/9999")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete a restriction`() {
        assertThat(personRestrictionRepository.existsById(existingRestriction.id)).isTrue()
        webTestClient.delete().uri("/persons/${existingPerson.id}/restriction/${existingRestriction.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus()
          .isNoContent
        assertThat(personRestrictionRepository.existsById(existingRestriction.id)).isFalse()
      }
    }
  }

  @DisplayName("GET /contact/{contactId}")
  @Nested
  inner class GetContact {
    private lateinit var existingPerson: Person
    private lateinit var existingContact: OffenderContactPerson
    private lateinit var existingOffender: Offender

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingPerson = person(
          firstName = "JOHN",
          lastName = "SMITH",
        )
        existingOffender = offender(nomsId = "A1234AA") {
          booking {
            existingContact = contact(
              person = existingPerson,
              contactType = "S",
              relationshipType = "BRO",
              emergencyContact = true,
              nextOfKin = true,
              active = true,
              approvedVisitor = true,
              comment = "Some comment text",
            )
          }
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
        webTestClient.get().uri("/contact/${existingContact.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/contact/${existingContact.id}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/contact/${existingContact.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `return 404 when contact not found`() {
        webTestClient.get().uri("/contact/99999")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return contact details`() {
        val response = webTestClient.get().uri("/contact/${existingContact.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse<PersonContact>()

        with(response) {
          assertThat(id).isEqualTo(existingContact.id)
          assertThat(contactType.code).isEqualTo("S")
          assertThat(relationshipType.code).isEqualTo("BRO")
          assertThat(active).isTrue
          assertThat(approvedVisitor).isTrue
          assertThat(emergencyContact).isTrue
          assertThat(nextOfKin).isTrue
          assertThat(comment).isEqualTo("Some comment text")
          assertThat(prisoner.offenderNo).isEqualTo("A1234AA")
          assertThat(prisoner.firstName).isEqualTo(existingOffender.firstName)
          assertThat(prisoner.lastName).isEqualTo(existingOffender.lastName)
        }
      }
    }
  }

  @DisplayName("GET /prisoners/{offenderNo}/restrictions")
  @Nested
  inner class GetPrisonerWithRestrictions {
    private lateinit var prisoner: Offender
    private lateinit var generalStaffMember: Staff
    private lateinit var lsaStaffMember: Staff

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        generalStaffMember = staff(firstName = "JANE", lastName = "SMITH") {
          account(username = "j.smith")
        }
        lsaStaffMember = staff(firstName = "JOHN", lastName = "STAFF") {
          account(username = "j.staff_gen", type = GENERAL)
          account(username = "j.staff_adm", type = ADMIN)
        }
        prisoner = offender {
          booking {
            restriction(
              restrictionType = "BAN",
              enteredStaff = generalStaffMember,
              authorisedStaff = lsaStaffMember,
              comment = "Banned for life!",
              effectiveDate = LocalDate.now(),
              expiryDate = LocalDate.now().plusDays(10),
            )
            restriction(
              restrictionType = "CCTV",
              enteredStaff = lsaStaffMember,
            )
          }
          booking {
            release()
            restriction(
              restrictionType = "BAN",
              enteredStaff = generalStaffMember,
              comment = "Banned on old booking",
              effectiveDate = LocalDate.now().minusDays(10),
              expiryDate = LocalDate.now().minusDays(5),
            )
          }
        }
      }
    }

    @AfterEach
    fun tearDown() {
      repository.deleteOffenders()
      personRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/prisoners/${prisoner.nomsId}/restrictions")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/prisoners/${prisoner.nomsId}/restrictions")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/prisoners/${prisoner.nomsId}/restrictions")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `list will be empty when no prisoner found`() {
        webTestClient.get().uri("/prisoners/A1234ZZ/restrictions")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("restrictions").isEmpty
      }
    }

    @Nested
    inner class Filtering {

      @Test
      fun `can include all bookings`() {
        webTestClient.get().uri("/prisoners/${prisoner.nomsId}/restrictions?latest-booking-only=false")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("restrictions.length()").isEqualTo(3)
      }

      @Test
      fun `can filter by latest booking only bookings`() {
        webTestClient.get().uri("/prisoners/${prisoner.nomsId}/restrictions?latest-booking-only=true")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("restrictions.length()").isEqualTo(2)
      }

      @Test
      fun `by default only latest booking restrictions are returned`() {
        webTestClient.get().uri("/prisoners/${prisoner.nomsId}/restrictions")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("restrictions.length()").isEqualTo(2)
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return restrictions for all bookings`() {
        webTestClient.get().uri("/prisoners/${prisoner.nomsId}/restrictions?latest-booking-only=false")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("restrictions.length()").isEqualTo(3)
      }

      @Test
      fun `will return details about the restrictions`() {
        webTestClient.get().uri("/prisoners/${prisoner.nomsId}/restrictions?latest-booking-only=false")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CONTACTPERSONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("restrictions[0].type.code").isEqualTo("BAN")
          .jsonPath("restrictions[0].type.description").isEqualTo("Banned")
          .jsonPath("restrictions[0].comment").isEqualTo("Banned for life!")
          .jsonPath("restrictions[0].effectiveDate").isEqualTo(LocalDate.now().toString())
          .jsonPath("restrictions[0].expiryDate").isEqualTo(LocalDate.now().plusDays(10).toString())
          .jsonPath("restrictions[0].enteredStaff.staffId").isEqualTo(generalStaffMember.id)
          .jsonPath("restrictions[0].enteredStaff.username").isEqualTo("j.smith")
          .jsonPath("restrictions[0].authorisedStaff.staffId").isEqualTo(lsaStaffMember.id)
          .jsonPath("restrictions[0].authorisedStaff.username").isEqualTo("j.staff_gen")
          .jsonPath("restrictions[1].type.code").isEqualTo("CCTV")
          .jsonPath("restrictions[1].type.description").isEqualTo("CCTV")
          .jsonPath("restrictions[1].comment").doesNotExist()
          .jsonPath("restrictions[1].effectiveDate").isEqualTo(LocalDate.now().toString())
          .jsonPath("restrictions[1].expiryDate").doesNotExist()
          .jsonPath("restrictions[1].enteredStaff.staffId").isEqualTo(lsaStaffMember.id)
          .jsonPath("restrictions[1].enteredStaff.username").isEqualTo("j.staff_gen")
          .jsonPath("restrictions[1].authorisedStaff").doesNotExist()
          .jsonPath("restrictions[2].type.code").isEqualTo("BAN")
          .jsonPath("restrictions[2].type.description").isEqualTo("Banned")
          .jsonPath("restrictions[2].comment").isEqualTo("Banned on old booking")
          .jsonPath("restrictions[2].effectiveDate").isEqualTo(LocalDate.now().minusDays(10).toString())
          .jsonPath("restrictions[2].expiryDate").isEqualTo(LocalDate.now().minusDays(5).toString())
          .jsonPath("restrictions[2].enteredStaff.staffId").isEqualTo(generalStaffMember.id)
          .jsonPath("restrictions[2].enteredStaff.username").isEqualTo("j.smith")
          .jsonPath("restrictions[2].authorisedStaff").doesNotExist()
      }
    }
  }
}
