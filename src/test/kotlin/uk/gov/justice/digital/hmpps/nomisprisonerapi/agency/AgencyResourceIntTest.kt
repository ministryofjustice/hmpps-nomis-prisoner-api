package uk.gov.justice.digital.hmpps.nomisprisonerapi.agency

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.AgencyLocationDsl.Companion.BRENT
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.AgencyLocationDsl.Companion.BROMLEY
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.AgencyLocationDsl.Companion.SHEFFIELD
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.expectBodyResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Agency
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Area
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Prison
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AreaRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PrisonRepository
import java.time.LocalDate

class AgencyResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var agencyLocationRepository: AgencyLocationRepository

  @Autowired
  private lateinit var agencyRepository: AgencyRepository

  @Autowired
  private lateinit var prisonRepository: PrisonRepository

  @Autowired
  private lateinit var areaRepository: AreaRepository

  @DisplayName("GET /prison/{prisonId}")
  @Nested
  inner class GetPrison {
    lateinit var legacyGenericAgency: AgencyLocation
    lateinit var approvedPremise: Agency
    lateinit var court: Agency
    lateinit var probationOffice: Agency
    lateinit var prison: Prison
    lateinit var londonRegion: Area
    lateinit var londonArea: Area
    lateinit var southEastArea: Area
    lateinit var eastLondon: Area
    lateinit var londonDistrict: Area

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        londonDistrict = area(code = "10", "Thames Valley", areaTypeCode = "COMM")
        southEastArea = region(code = "SE", "South East")
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
        probationOffice = agency(
          agencyLocationId = "BOW001",
          description = "Tower Hamlets Probation  Bow",
          longDescription = "Tower Hamlets Probation Bow East London",
          type = "COMM",
          region = londonRegion,
          area = londonArea,
          subArea = eastLondon,
          nomsRegion = southEastArea,
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
        approvedPremise = agency(
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
        court = agency(
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
      agencyRepository.delete(approvedPremise)
      agencyRepository.delete(court)
      agencyRepository.delete(probationOffice)
      prisonRepository.delete(prison)
      areaRepository.delete(eastLondon)
      areaRepository.delete(londonArea)
      areaRepository.delete(londonRegion)
      areaRepository.delete(southEastArea)
      areaRepository.delete(londonDistrict)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/prison/XXI")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/prison/XXI")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/prison/XXI")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `will return 404 if prison does not exist`() {
        webTestClient.get().uri("/prison/ZZI")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return prison details`() {
        val prison: PrisonResponse = webTestClient.get().uri("/prison/AAI")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(prison.prisonId).isEqualTo("AAI")
        assertThat(prison.description).isEqualTo("HMP AAI")
        assertThat(prison.district?.description).isEqualTo("Thames Valley")
        assertThat(prison.active).isTrue
        assertThat(prison.deactivationDate).isNull()
        assertThat(prison.updateAllowed).isTrue
        assertThat(prison.contactName).isNull()
      }

      @Test
      fun `will return generic agency location details for a prison`() {
        val agency: AgencyResponse = webTestClient.get().uri("/agency-location/AAI")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(agency.agencyId).isEqualTo("AAI")
        assertThat(agency.description).isEqualTo("HMP AAI")
        assertThat(agency.active).isTrue
        assertThat(agency.deactivationDate).isNull()
        assertThat(agency.updateAllowed).isTrue
        assertThat(agency.contactName).isNull()
      }

      @Test
      fun `will return generic agency details for an approved premises`() {
        val agency: AgencyResponse = webTestClient.get().uri("/agency-location/THA029")
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
        assertThat(agency.nomsRegion?.description).isEqualTo("South East")
        assertThat(agency.region?.description).isEqualTo("London Region")
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

      @Test
      fun `will not find as agency when is prison`() {
        webTestClient.get().uri("/agency/AAI")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `will not find as prison when is agency`() {
        webTestClient.get().uri("/prison/THA029")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }
  }
}
