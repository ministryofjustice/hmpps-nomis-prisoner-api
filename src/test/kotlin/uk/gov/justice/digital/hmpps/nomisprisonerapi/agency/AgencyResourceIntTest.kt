package uk.gov.justice.digital.hmpps.nomisprisonerapi.agency

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.AgencyLocationDsl.Companion.BRENT
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.AgencyLocationDsl.Companion.BROMLEY
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.AgencyLocationDsl.Companion.SHEFFIELD
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.expectBodyResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Area
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Region
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SubArea
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationAddressRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationInternetAddressRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationPhoneRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AreaRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.RegionRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.SubAreaRepository
import java.time.LocalDate

class AgencyResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var agencyLocationRepository: AgencyLocationRepository

  @Autowired
  private lateinit var agencyLocationInternetAddressRepository: AgencyLocationInternetAddressRepository

  @Autowired
  private lateinit var agencyLocationPhoneRepository: AgencyLocationPhoneRepository

  @Autowired
  private lateinit var agencyLocationAddressRepository: AgencyLocationAddressRepository

  @Autowired
  private lateinit var areaRepository: AreaRepository

  @Autowired
  private lateinit var subAreaRepository: SubAreaRepository

  @Autowired
  private lateinit var regionRepository: RegionRepository

  @DisplayName("GET /agency/ids/all")
  @Nested
  inner class GetAllAgencies {
    lateinit var legacyGenericAgency: AgencyLocation
    lateinit var dodgyAgency: AgencyLocation
    lateinit var approvedPremise: AgencyLocation
    lateinit var court: AgencyLocation
    lateinit var probationOffice: AgencyLocation
    lateinit var prison: AgencyLocation
    lateinit var londonRegion: Region
    lateinit var londonArea: Area
    lateinit var southEastArea: Area
    lateinit var eastLondon: SubArea
    lateinit var londonDistrict: Area

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        londonDistrict = area(code = "10", "Thames Valley", areaTypeCode = "COMM")
        southEastArea = area(code = "LONDON", "London")
        londonRegion = region(code = "LON", "London Region") {
          londonArea = area(code = "62", "London Area", areaTypeCode = "COMM") {
            eastLondon = subArea("LON_E", description = "East London", areaTypeCode = "COMM")
          }
        }
        legacyGenericAgency = agencyLocation(
          agencyLocationId = "XXI",
          description = "HMP XXI",
          type = "CRC",
        )
        dodgyAgency = agencyLocation(
          agencyLocationId = "AUHSA;",
          description = "Augustus House AP",
          type = "APPR",
        )
        prison = prison(
          agencyLocationId = "AAI",
          description = "HMP AAI",
          district = londonDistrict,
        )
        probationOffice = agencyLocation(
          agencyLocationId = "BOW001",
          description = "Tower Hamlets Probation  Bow",
          longDescription = "Tower Hamlets Probation Bow East London",
          type = "COMM",
          region = southEastArea,
          area = londonArea,
          subArea = eastLondon,
          nomsRegion = londonRegion,
          payrollRegionCode = "LTV",
          cjitCode = "D62L087",
        ) {
          localAuthority(BRENT)
          localAuthority(BROMLEY)
          address(
            type = "BUS",
            noFixedAddress = null,
            primaryAddress = false,
            premise = null,
            street = null,
            locality = null,
            city = null,
            county = null,
            country = null,
          )
          address(
            type = "BUS",
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
            phone(
              phoneType = "BUS",
              phoneNo = "07399999999",
              extNo = "123",
            )
            phone(
              phoneType = "FAX",
              phoneNo = "01142561919",
            )
          }

          phone(
            phoneType = "BUS",
            phoneNo = "0114 55 5555",
            extNo = "123",
          )
          phone(
            phoneType = "FAX",
            phoneNo = "0114 44 5555",
          )
          email(
            address = "probation@gov.uk",
          )
          email(
            address = "justice@gov.uk",
          )
        }
        approvedPremise = agencyLocation(
          agencyLocationId = "THA029",
          description = "Approved Premises",
          district = londonDistrict,
          active = false,
          type = "APPR",
          deactivationDate = LocalDate.parse("2022-01-01"),
          updateAllowed = false,
          contactName = "Gerald Simpson",
          disabilityAccessCode = "Y",
        )
        court = agencyLocation(
          agencyLocationId = "SHEFCC",
          description = "Sheffield Crown Court",
          type = "CRT",
          courtTypeCode = "CC",
        )
      }
    }

    @AfterEach
    fun tearDown() {
      agencyLocationRepository.deleteById(legacyGenericAgency.id)
      agencyLocationRepository.deleteById(dodgyAgency.id)
      agencyLocationRepository.delete(approvedPremise)
      agencyLocationRepository.delete(court)
      agencyLocationRepository.delete(probationOffice)
      agencyLocationRepository.delete(prison)
      subAreaRepository.delete(eastLondon)
      areaRepository.delete(londonArea)
      regionRepository.delete(londonRegion)
      areaRepository.delete(southEastArea)
      areaRepository.delete(londonDistrict)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/agency/ids/all")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/agency/ids/all")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/agency/ids/all")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `can exclude prisons and the dodgy agency with semi-colon in ID`() {
        val response: AgencyIdsResponse = webTestClient.get().uri("/agency/ids/all?excludeType=INST")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(response.agencyIds.map { it.agencyId }).contains(
          "XXI",
          "THA029",
          "SHEFCC",
          "BOW001",
        )
        assertThat(response.agencyIds.map { it.agencyId }).doesNotContain(
          "AAI",
          "AUHSA;",
        )
      }

      @Test
      fun `can include all agency types`() {
        val response: AgencyIdsResponse = webTestClient.get().uri("/agency/ids/all")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(response.agencyIds.map { it.agencyId }).contains(
          "XXI",
          "THA029",
          "SHEFCC",
          "BOW001",
          "AAI",
        )
      }
    }
  }

  @DisplayName("GET /agency/{agencyId}")
  @Nested
  inner class GetAgency {
    lateinit var legacyGenericAgency: AgencyLocation
    lateinit var approvedPremise: AgencyLocation
    lateinit var court: AgencyLocation
    lateinit var probationOffice: AgencyLocation
    lateinit var prison: AgencyLocation
    lateinit var londonRegion: Region
    lateinit var londonArea: Area
    lateinit var southEastArea: Area
    lateinit var eastLondon: SubArea
    lateinit var londonDistrict: Area

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        londonDistrict = area(code = "10", "Thames Valley", areaTypeCode = "COMM")
        southEastArea = area(code = "LONDON", "London")
        londonRegion = region(code = "LON", "London Region") {
          londonArea = area(code = "62", "London Area", areaTypeCode = "COMM") {
            eastLondon = subArea("LON_E", description = "East London", areaTypeCode = "COMM")
          }
        }
        legacyGenericAgency = agencyLocation(
          agencyLocationId = "XXI",
          description = "HMP XXI",
          type = "INST",
        )
        prison = prison(
          agencyLocationId = "AAI",
          description = "HMP AAI",
          district = londonDistrict,
        )
        probationOffice = agencyLocation(
          agencyLocationId = "BOW001",
          description = "Tower Hamlets Probation  Bow",
          longDescription = "Tower Hamlets Probation Bow East London",
          type = "COMM",
          region = southEastArea,
          area = londonArea,
          subArea = eastLondon,
          nomsRegion = londonRegion,
          payrollRegionCode = "LTV",
          cjitCode = "D62L087",
        ) {
          localAuthority(BRENT)
          localAuthority(BROMLEY)
          address(
            type = "BUS",
            noFixedAddress = null,
            primaryAddress = false,
            premise = null,
            street = null,
            locality = null,
            city = null,
            county = null,
            country = null,
          )
          address(
            type = "BUS",
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
            phone(
              phoneType = "BUS",
              phoneNo = "07399999999",
              extNo = "123",
            )
            phone(
              phoneType = "FAX",
              phoneNo = "01142561919",
            )
          }

          phone(
            phoneType = "BUS",
            phoneNo = "0114 55 5555",
            extNo = "123",
          )
          phone(
            phoneType = "FAX",
            phoneNo = "0114 44 5555",
          )
          email(
            address = "probation@gov.uk",
          )
          email(
            address = "justice@gov.uk",
          )
        }
        approvedPremise = agencyLocation(
          agencyLocationId = "THA029",
          description = "Approved Premises",
          district = londonDistrict,
          active = false,
          type = "APPR",
          deactivationDate = LocalDate.parse("2022-01-01"),
          updateAllowed = false,
          contactName = "Gerald Simpson",
          disabilityAccessCode = "Y",
        )
        court = agencyLocation(
          agencyLocationId = "SHEFCC",
          description = "Sheffield Crown Court",
          type = "CRT",
          courtTypeCode = "CC",
        )
      }
    }

    @AfterEach
    fun tearDown() {
      agencyLocationRepository.deleteById(legacyGenericAgency.id)
      agencyLocationRepository.delete(approvedPremise)
      agencyLocationRepository.delete(court)
      agencyLocationRepository.delete(probationOffice)
      agencyLocationRepository.delete(prison)
      subAreaRepository.delete(eastLondon)
      areaRepository.delete(londonArea)
      regionRepository.delete(londonRegion)
      areaRepository.delete(southEastArea)
      areaRepository.delete(londonDistrict)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/agency/XXI")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/agency/XXI")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/agency/XXI")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `will return 404 if agency does not exist`() {
        webTestClient.get().uri("/agency/ZZI")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return prison details`() {
        val prison: AgencyResponse = webTestClient.get().uri("/agency/AAI")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(prison.agencyId).isEqualTo("AAI")
        assertThat(prison.description).isEqualTo("HMP AAI")
        assertThat(prison.district?.description).isEqualTo("Thames Valley")
        assertThat(prison.active).isTrue
        assertThat(prison.deactivationDate).isNull()
        assertThat(prison.updateAllowed).isTrue
        assertThat(prison.contactName).isNull()
      }

      @Test
      fun `will return agency details for an approved premises`() {
        val agency: AgencyResponse = webTestClient.get().uri("/agency/THA029")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(agency.agencyId).isEqualTo("THA029")
        assertThat(agency.description).isEqualTo("Approved Premises")
        assertThat(agency.type.description).isEqualTo("Approved Premises")
        assertThat(agency.active).isFalse
        assertThat(agency.deactivationDate).isEqualTo(LocalDate.parse("2022-01-01"))
        assertThat(agency.updateAllowed).isFalse
        assertThat(agency.contactName).isEqualTo("Gerald Simpson")
        assertThat(agency.disabilityAccessCode).isEqualTo("Y")
      }

      @Test
      fun `will return details for an approved premises`() {
        val agency: AgencyResponse = webTestClient.get().uri("/agency/THA029")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(agency.agencyId).isEqualTo("THA029")
        assertThat(agency.description).isEqualTo("Approved Premises")
        assertThat(agency.district?.description).isEqualTo("Thames Valley")
        assertThat(agency.type.description).isEqualTo("Approved Premises")
        assertThat(agency.active).isFalse
        assertThat(agency.deactivationDate).isEqualTo(LocalDate.parse("2022-01-01"))
        assertThat(agency.updateAllowed).isFalse
        assertThat(agency.contactName).isEqualTo("Gerald Simpson")
      }

      @Test
      fun `will return details for a court`() {
        val agency: AgencyResponse = webTestClient.get().uri("/agency/SHEFCC")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(agency.agencyId).isEqualTo("SHEFCC")
        assertThat(agency.description).isEqualTo("Sheffield Crown Court")
        assertThat(agency.courtType?.description).isEqualTo("Crown Court")
      }

      @Test
      fun `will return details for a probation office`() {
        val agency: AgencyResponse = webTestClient.get().uri("/agency/BOW001")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(agency.agencyId).isEqualTo("BOW001")
        assertThat(agency.nomsRegion?.description).isEqualTo("London Region")
        assertThat(agency.region?.description).isEqualTo("London")
        assertThat(agency.area?.description).isEqualTo("London Area")
        assertThat(agency.subArea?.description).isEqualTo("East London")
        assertThat(agency.longDescription).isEqualTo("Tower Hamlets Probation Bow East London")
        assertThat(agency.payrollRegion?.description).isEqualTo("London & Thames Valley")
        assertThat(agency.cjitCode).isEqualTo("D62L087")
        assertThat(agency.localAuthorities).extracting<String> { it.description }.containsExactlyInAnyOrder(
          "Brent",
          "Bromley",
        )
      }

      @Test
      fun `will return address details for an agency`() {
        val agency: AgencyResponse = webTestClient.get().uri("/agency/BOW001")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(agency.agencyId).isEqualTo("BOW001")
        assertThat(agency.addresses[0].id).isEqualTo(probationOffice.addresses[0].addressId)
        assertThat(agency.addresses[0].type?.code).isEqualTo("BUS")
        assertThat(agency.addresses[0].flat).isNull()
        assertThat(agency.addresses[0].premise).isNull()
        assertThat(agency.addresses[0].street).isNull()
        assertThat(agency.addresses[0].locality).isNull()
        assertThat(agency.addresses[0].city).isNull()
        assertThat(agency.addresses[0].county).isNull()
        assertThat(agency.addresses[0].country).isNull()
        assertThat(agency.addresses[0].validatedPAF).isEqualTo(false)
        assertThat(agency.addresses[0].noFixedAddress).isNull()
        assertThat(agency.addresses[0].primaryAddress).isEqualTo(false)
        assertThat(agency.addresses[0].mailAddress).isEqualTo(false)
        assertThat(agency.addresses[0].comment).isNull()
        assertThat(agency.addresses[0].startDate).isNull()
        assertThat(agency.addresses[0].endDate).isNull()
        assertThat(agency.addresses[1].id).isEqualTo(probationOffice.addresses[1].addressId)
        assertThat(agency.addresses[1].type?.code).isEqualTo("BUS")
        assertThat(agency.addresses[1].type?.description).isEqualTo("Business Address")
        assertThat(agency.addresses[1].flat).isEqualTo("3B")
        assertThat(agency.addresses[1].premise).isEqualTo("Brown Court")
        assertThat(agency.addresses[1].street).isEqualTo("Scotland Street")
        assertThat(agency.addresses[1].locality).isEqualTo("Hunters Bar")
        assertThat(agency.addresses[1].postcode).isEqualTo("S1 3GG")
        assertThat(agency.addresses[1].city?.code).isEqualTo("25343")
        assertThat(agency.addresses[1].city?.description).isEqualTo("Sheffield")
        assertThat(agency.addresses[1].county?.code).isEqualTo("S.YORKSHIRE")
        assertThat(agency.addresses[1].county?.description).isEqualTo("South Yorkshire")
        assertThat(agency.addresses[1].country?.code).isEqualTo("ENG")
        assertThat(agency.addresses[1].country?.description).isEqualTo("England")
        assertThat(agency.addresses[1].validatedPAF).isEqualTo(true)
        assertThat(agency.addresses[1].noFixedAddress).isEqualTo(false)
        assertThat(agency.addresses[1].primaryAddress).isEqualTo(true)
        assertThat(agency.addresses[1].mailAddress).isEqualTo(true)
        assertThat(agency.addresses[1].comment).isEqualTo("Not to be used")
        assertThat(agency.addresses[1].startDate).isEqualTo("2024-10-01")
        assertThat(agency.addresses[1].endDate).isEqualTo("2024-11-01")
      }

      @Test
      fun `will return address phone details for an agency`() {
        val agency: AgencyResponse = webTestClient.get().uri("/agency/BOW001")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(agency.agencyId).isEqualTo("BOW001")
        assertThat(agency.addresses[1].id).isEqualTo(probationOffice.addresses[1].addressId)
        assertThat(agency.addresses[1].phoneNumbers[0].id).isEqualTo(probationOffice.addresses[1].phones[0].phoneId)
        assertThat(agency.addresses[1].phoneNumbers[0].type.description).isEqualTo("Business")
        assertThat(agency.addresses[1].phoneNumbers[0].number).isEqualTo("07399999999")
        assertThat(agency.addresses[1].phoneNumbers[1].id).isEqualTo(probationOffice.addresses[1].phones[1].phoneId)
        assertThat(agency.addresses[1].phoneNumbers[1].type.description).isEqualTo("Fax")
        assertThat(agency.addresses[1].phoneNumbers[1].number).isEqualTo("01142561919")
      }

      @Test
      fun `will return global phone details for an agency`() {
        val agency: AgencyResponse = webTestClient.get().uri("/agency/BOW001")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(agency.agencyId).isEqualTo("BOW001")
        assertThat(agency.phones[0].id).isEqualTo(probationOffice.phones[0].phoneId)
        assertThat(agency.phones[0].type.description).isEqualTo("Business")
        assertThat(agency.phones[0].number).isEqualTo("0114 55 5555")
        assertThat(agency.phones[0].extension).isEqualTo("123")
        assertThat(agency.phones[1].id).isEqualTo(probationOffice.phones[1].phoneId)
        assertThat(agency.phones[1].type.description).isEqualTo("Fax")
        assertThat(agency.phones[1].number).isEqualTo("0114 44 5555")
      }

      @Test
      fun `will return email details for an agency`() {
        val agency: AgencyResponse = webTestClient.get().uri("/agency/BOW001")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(agency.agencyId).isEqualTo("BOW001")
        assertThat(agency.emailAddresses[0].id).isEqualTo(probationOffice.emailAddresses[0].internetAddressId)
        assertThat(agency.emailAddresses[0].emailAddress).isEqualTo("probation@gov.uk")
        assertThat(agency.emailAddresses[1].id).isEqualTo(probationOffice.emailAddresses[1].internetAddressId)
        assertThat(agency.emailAddresses[1].emailAddress).isEqualTo("justice@gov.uk")
      }
    }
  }

  @DisplayName("POST /agency/{agencyId}/emails")
  @Nested
  inner class CreateAgencyEmail {
    private val validEmailRequest = CreateAgencyEmailAddressRequest(
      emailAddress = "test1@test.com",
    )

    private lateinit var existingAgency: AgencyLocation

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingAgency = agencyLocation(
          agencyLocationId = "XXI",
          description = "HMP XXI",
          type = "INST",
        )
      }
    }

    @AfterEach
    fun tearDown() {
      agencyLocationRepository.deleteById(existingAgency.id)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/agency/${existingAgency.id}/emails")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validEmailRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/agency/${existingAgency.id}/emails")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validEmailRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/agency/${existingAgency.id}/emails")
          .bodyValue(validEmailRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `will return 404 if agency does not exist`() {
        webTestClient.post().uri("/agency/ZZI/emails")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .bodyValue(validEmailRequest)
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will create an email address for the agency`() {
        val response: CreateAgencyEmailAddressResponse = webTestClient.post().uri("/agency/${existingAgency.id}/emails")
          .bodyValue(
            validEmailRequest.copy(
              emailAddress = "test@justice.gov.uk",
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBodyResponse()

        with(agencyLocationInternetAddressRepository.findByIdOrNull(response.id)!!) {
          assertThat(internetAddressId).isEqualTo(response.id)
          assertThat(agencyLocation.id).isEqualTo(existingAgency.id)
          assertThat(internetAddressClass).isEqualTo("EMAIL")
          assertThat(internetAddress).isEqualTo("test@justice.gov.uk")
        }

        val agency: AgencyResponse = webTestClient.get().uri("/agency/${existingAgency.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()
        assertThat(agency.emailAddresses).anyMatch { it.id == response.id && it.emailAddress == "test@justice.gov.uk" }

        verify(telemetryClient).trackEvent(
          eq("agency-email-inserted"),
          check {
            assertThat(it).containsEntry("agencyId", existingAgency.id)
            assertThat(it).containsEntry("emailAddressId", response.id.toString())
            assertThat(it).doesNotContainValue("test@justice.gov.uk")
          },
          isNull(),
        )
      }
    }
  }

  @DisplayName("POST /agency/{agencyId}/phones")
  @Nested
  inner class CreateAgencyPhone {
    private val validPhoneRequest = CreateAgencyPhoneNumberRequest(
      number = "0114 555 555",
      typeCode = "BUS",
    )

    private lateinit var existingAgency: AgencyLocation

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingAgency = agencyLocation(
          agencyLocationId = "XXI",
          description = "HMP XXI",
          type = "INST",
        )
      }
    }

    @AfterEach
    fun tearDown() {
      agencyLocationRepository.deleteById(existingAgency.id)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/agency/${existingAgency.id}/phones")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/agency/${existingAgency.id}/phones")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/agency/${existingAgency.id}/phones")
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `will return 404 if agency does not exist`() {
        webTestClient.post().uri("/agency/ZZI/phones")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .bodyValue(validPhoneRequest)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `will return 400 if phone type does not exist`() {
        webTestClient.post().uri("/agency/${existingAgency.id}/phones")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .bodyValue(validPhoneRequest.copy(typeCode = "RUBBISH"))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will create a phone number at the agency level`() {
        val response: CreateAgencyPhoneNumberResponse = webTestClient.post().uri("/agency/${existingAgency.id}/phones")
          .bodyValue(
            validPhoneRequest.copy(
              number = "0114 555 555",
              extension = "x432",
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBodyResponse()

        with(agencyLocationPhoneRepository.findByIdOrNull(response.id)!!) {
          assertThat(phoneId).isEqualTo(response.id)
          assertThat(agencyLocation.id).isEqualTo(existingAgency.id)
          assertThat(phoneNo).isEqualTo("0114 555 555")
          assertThat(extNo).isEqualTo("x432")
          assertThat(phoneType.code).isEqualTo("BUS")
        }

        val agency: AgencyResponse = webTestClient.get().uri("/agency/${existingAgency.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()
        assertThat(agency.phones).anyMatch { it.id == response.id && it.number == "0114 555 555" && it.extension == "x432" }

        verify(telemetryClient).trackEvent(
          eq("agency-phone-inserted"),
          check {
            assertThat(it).containsEntry("agencyId", existingAgency.id)
            assertThat(it).containsEntry("phoneId", response.id.toString())
          },
          isNull(),
        )
      }
    }
  }

  @DisplayName("POST /agency/{agencyId}/addresses")
  @Nested
  inner class CreateAgencyAddress {
    private val validAddressRequest = CreateAgencyAddressRequest()

    private lateinit var existingAgency: AgencyLocation

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingAgency = agencyLocation(
          agencyLocationId = "XXI",
          description = "HMP XXI",
          type = "INST",
        )
      }
    }

    @AfterEach
    fun tearDown() {
      agencyLocationRepository.deleteById(existingAgency.id)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/agency/${existingAgency.id}/addresses")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(validAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/agency/${existingAgency.id}/addresses")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(validAddressRequest)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/agency/${existingAgency.id}/addresses")
          .bodyValue(validAddressRequest)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `will return 404 if agency does not exist`() {
        webTestClient.post().uri("/agency/ZZI/addresses")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .bodyValue(validAddressRequest)
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will create an address at the agency level`() {
        val response: CreateAgencyAddressResponse = webTestClient.post().uri("/agency/${existingAgency.id}/addresses")
          .bodyValue(
            validAddressRequest.copy(
              flat = "1A",
              premise = "Bolden Court",
              street = "Fulwood Road",
              locality = "Broomhill",
              city = "Sheffield",
              county = "South Yorkshire",
              country = "United Kingdom",
              postcode = "S10 2HH",
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBodyResponse()

        with(agencyLocationAddressRepository.findByIdOrNull(response.id)!!) {
          assertThat(addressId).isEqualTo(response.id)
          assertThat(agencyLocation.id).isEqualTo(existingAgency.id)
          assertThat(addressType?.code).isEqualTo("BUS")
          assertThat(flat).isEqualTo("1A")
          assertThat(premise).isEqualTo("Bolden Court")
          assertThat(street).isEqualTo("Fulwood Road")
          assertThat(locality).isEqualTo("Broomhill")
          assertThat(city?.code).isEqualTo(SHEFFIELD)
          assertThat(county?.code).isEqualTo("S.YORKSHIRE")
          assertThat(country?.code).isEqualTo("GBR")
          assertThat(postalCode).isEqualTo("S10 2HH")
          assertThat(primaryAddress).isFalse()
          assertThat(mailAddress).isFalse()
          assertThat(noFixedAddress).isFalse()
          assertThat(comment).isNull()
          assertThat(startDate).isEqualTo(LocalDate.now())
          assertThat(endDate).isNull()
        }

        val agency: AgencyResponse = webTestClient.get().uri("/agency/${existingAgency.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()
        assertThat(agency.addresses).anyMatch { it.id == response.id && it.premise == "Bolden Court" }

        verify(telemetryClient).trackEvent(
          eq("agency-address-inserted"),
          check {
            assertThat(it).containsEntry("agencyId", existingAgency.id)
            assertThat(it).containsEntry("addressId", response.id.toString())
          },
          isNull(),
        )
      }

      @Test
      fun `will add a second address to the agency, keeping the first`() {
        val firstResponse: CreateAgencyAddressResponse = webTestClient.post().uri("/agency/${existingAgency.id}/addresses")
          .bodyValue(
            validAddressRequest.copy(
              premise = "Bolden Court",
              street = "Fulwood Road",
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBodyResponse()

        val secondResponse: CreateAgencyAddressResponse = webTestClient.post().uri("/agency/${existingAgency.id}/addresses")
          .bodyValue(
            validAddressRequest.copy(
              premise = "22",
              street = "West Street",
              city = "Sheffield",
              county = "South Yorkshire",
              country = "United Kingdom",
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBodyResponse()

        assertThat(secondResponse.id).isNotEqualTo(firstResponse.id)

        with(agencyLocationAddressRepository.findByIdOrNull(firstResponse.id)!!) {
          assertThat(agencyLocation.id).isEqualTo(existingAgency.id)
          assertThat(premise).isEqualTo("Bolden Court")
        }
        with(agencyLocationAddressRepository.findByIdOrNull(secondResponse.id)!!) {
          assertThat(agencyLocation.id).isEqualTo(existingAgency.id)
          assertThat(premise).isEqualTo("22")
          assertThat(city?.code).isEqualTo(SHEFFIELD)
        }

        val agency: AgencyResponse = webTestClient.get().uri("/agency/${existingAgency.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()
        assertThat(agency.addresses).hasSize(2)
        assertThat(agency.addresses).anyMatch { it.id == firstResponse.id && it.premise == "Bolden Court" }
        assertThat(agency.addresses).anyMatch { it.id == secondResponse.id && it.premise == "22" }
      }

      @Test
      fun `will match city, county and country descriptions case insensitively`() {
        val response: CreateAgencyAddressResponse = webTestClient.post().uri("/agency/${existingAgency.id}/addresses")
          .bodyValue(
            validAddressRequest.copy(
              city = "SHEFFIELD",
              county = "south yorkshire",
              country = "united kingdom",
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBodyResponse()

        with(agencyLocationAddressRepository.findByIdOrNull(response.id)!!) {
          assertThat(city?.code).isEqualTo(SHEFFIELD)
          assertThat(county?.code).isEqualTo("S.YORKSHIRE")
          assertThat(country?.code).isEqualTo("GBR")
        }
      }

      @Test
      fun `will set city, county and country to null and record lookup failures in telemetry when descriptions do not match`() {
        val response: CreateAgencyAddressResponse = webTestClient.post().uri("/agency/${existingAgency.id}/addresses")
          .bodyValue(
            validAddressRequest.copy(
              city = "Rubbish City",
              county = "Rubbish County",
              country = "Rubbish Country",
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isCreated
          .expectBodyResponse()

        with(agencyLocationAddressRepository.findByIdOrNull(response.id)!!) {
          assertThat(city).isNull()
          assertThat(county).isNull()
          assertThat(country).isNull()
        }

        verify(telemetryClient).trackEvent(
          eq("agency-address-inserted"),
          check {
            assertThat(it).containsEntry("agencyId", existingAgency.id)
            assertThat(it).containsEntry("addressId", response.id.toString())
            assertThat(it).containsEntry("cityLookupFailure", "Rubbish City")
            assertThat(it).containsEntry("countyLookupFailure", "Rubbish County")
            assertThat(it).containsEntry("countryLookupFailure", "Rubbish Country")
          },
          isNull(),
        )
      }
    }
  }

  @DisplayName("PUT /agency/{agencyId}/emails")
  @Nested
  inner class UpdateAgencyEmailAddresses {
    private lateinit var existingAgency: AgencyLocation
    private lateinit var existingEmail: uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationInternetAddress

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingAgency = agencyLocation(
          agencyLocationId = "XXI",
          description = "HMP XXI",
          type = "INST",
        ) {
          existingEmail = email(address = "old@justice.gov.uk")
        }
      }
    }

    @AfterEach
    fun tearDown() {
      agencyLocationRepository.deleteById(existingAgency.id)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.put().uri("/agency/${existingAgency.id}/emails")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(UpdateAgencyEmailAddressesRequest(emailAddresses = listOf("new@justice.gov.uk")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put().uri("/agency/${existingAgency.id}/emails")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .bodyValue(UpdateAgencyEmailAddressesRequest(emailAddresses = listOf("new@justice.gov.uk")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put().uri("/agency/${existingAgency.id}/emails")
          .bodyValue(UpdateAgencyEmailAddressesRequest(emailAddresses = listOf("new@justice.gov.uk")))
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `will return 404 if agency does not exist`() {
        webTestClient.put().uri("/agency/ZZI/emails")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .bodyValue(UpdateAgencyEmailAddressesRequest(emailAddresses = listOf("new@justice.gov.uk")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will replace the single existing email address with the single new one`() {
        val response: AgencyEmailAddressesResponse = webTestClient.put().uri("/agency/${existingAgency.id}/emails")
          .bodyValue(UpdateAgencyEmailAddressesRequest(emailAddresses = listOf("new@justice.gov.uk")))
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBodyResponse()

        assertThat(response.emailAddresses).hasSize(1)
        assertThat(response.emailAddresses[0].id).isEqualTo(existingEmail.internetAddressId)
        assertThat(response.emailAddresses[0].emailAddress).isEqualTo("new@justice.gov.uk")

        with(agencyLocationInternetAddressRepository.findByIdOrNull(existingEmail.internetAddressId)!!) {
          assertThat(internetAddress).isEqualTo("new@justice.gov.uk")
        }

        val agency: AgencyResponse = webTestClient.get().uri("/agency/${existingAgency.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()
        assertThat(agency.emailAddresses).hasSize(1)
        assertThat(agency.emailAddresses[0].emailAddress).isEqualTo("new@justice.gov.uk")

        verify(telemetryClient).trackEvent(
          eq("agency.email.updated"),
          check {
            assertThat(it).containsEntry("agencyId", existingAgency.id)
            assertThat(it).containsEntry("emailAddressIds", existingEmail.internetAddressId.toString())
          },
          isNull(),
        )
      }
    }
  }

  @DisplayName("PUT /agency/{agencyId}/phones")
  @Nested
  inner class UpdateAgencyPhoneNumbers {
    private lateinit var existingAgency: AgencyLocation
    private lateinit var existingPhone: uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationPhone

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        existingAgency = agencyLocation(
          agencyLocationId = "XXI",
          description = "HMP XXI",
          type = "INST",
        ) {
          existingPhone = phone(phoneType = "BUS", phoneNo = "0114 555 555")
        }
      }
    }

    @AfterEach
    fun tearDown() {
      agencyLocationRepository.deleteById(existingAgency.id)
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will replace the single existing phone number with the single new one`() {
        val response: AgencyPhoneNumbersResponse = webTestClient.put().uri("/agency/${existingAgency.id}/phones")
          .bodyValue(
            UpdateAgencyPhoneNumbersRequest(
              phoneNumbers = listOf(UpdateAgencyPhoneNumber(number = "0114 999 999", extension = "x432", typeCode = "HOME")),
            ),
          )
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBodyResponse()

        assertThat(response.phoneNumbers).hasSize(1)
        assertThat(response.phoneNumbers[0].id).isEqualTo(existingPhone.phoneId)
        assertThat(response.phoneNumbers[0].number).isEqualTo("0114 999 999")
        assertThat(response.phoneNumbers[0].extension).isEqualTo("x432")
        assertThat(response.phoneNumbers[0].type.code).isEqualTo("HOME")

        with(agencyLocationPhoneRepository.findByIdOrNull(existingPhone.phoneId)!!) {
          assertThat(phoneNo).isEqualTo("0114 999 999")
          assertThat(extNo).isEqualTo("x432")
          assertThat(phoneType.code).isEqualTo("HOME")
        }

        val agency: AgencyResponse = webTestClient.get().uri("/agency/${existingAgency.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()
        assertThat(agency.phones).hasSize(1)
        assertThat(agency.phones[0].number).isEqualTo("0114 999 999")

        verify(telemetryClient).trackEvent(
          eq("agency.phone.updated"),
          check {
            assertThat(it).containsEntry("agencyId", existingAgency.id)
            assertThat(it).containsEntry("phoneIds", existingPhone.phoneId.toString())
          },
          isNull(),
        )
      }
    }
  }
}
