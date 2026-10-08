package uk.gov.justice.digital.hmpps.nomisprisonerapi.agency

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.test.util.ReflectionTestUtils
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AddressPhone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AddressType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationInternetAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationPhone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.City
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Country
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.County
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.InternetAddress.Companion.EMAIL_INTERNET_ADDRESS_CLASS
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PhoneUsage
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationAddressRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationInternetAddressRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationPhoneRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.util.Optional

class AgencyServiceTest {
  private val agencyLocationRepository: AgencyLocationRepository = mock()
  private val agencyLocationInternetAddressRepository: AgencyLocationInternetAddressRepository = mock()
  private val agencyLocationPhoneRepository: AgencyLocationPhoneRepository = mock()
  private val agencyLocationAddressRepository: AgencyLocationAddressRepository = mock()
  private val phoneUsageRepository: ReferenceCodeRepository<PhoneUsage> = mock()
  private val addressTypeRepository: ReferenceCodeRepository<AddressType> = mock()
  private val cityRepository: ReferenceCodeRepository<City> = mock()
  private val countyRepository: ReferenceCodeRepository<County> = mock()
  private val countryRepository: ReferenceCodeRepository<Country> = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val agencyService = AgencyService(
    agencyLocationRepository = agencyLocationRepository,
    agencyLocationInternetAddressRepository = agencyLocationInternetAddressRepository,
    agencyLocationPhoneRepository = agencyLocationPhoneRepository,
    agencyLocationAddressRepository = agencyLocationAddressRepository,
    phoneUsageRepository = phoneUsageRepository,
    addressTypeRepository = addressTypeRepository,
    cityRepository = cityRepository,
    countyRepository = countyRepository,
    countryRepository = countryRepository,
    telemetryClient = telemetryClient,
  )

  private fun agencyEmail(agency: AgencyLocation, id: Long, emailAddress: String) = AgencyLocationInternetAddress(
    agencyLocation = agency,
    internetAddress = emailAddress,
    internetAddressClass = EMAIL_INTERNET_ADDRESS_CLASS,
  ).also { ReflectionTestUtils.setField(it, "internetAddressId", id) }

  @Nested
  @DisplayName("updateAgencyEmailAddresses")
  inner class UpdateAgencyEmailAddresses {

    @Test
    fun `will throw not found if agency does not exist`() {
      whenever(agencyLocationRepository.findById("ZZI")).thenReturn(Optional.empty())

      assertThatThrownBy {
        agencyService.updateAgencyEmailAddresses("ZZI", UpdateAgencyEmailAddressesRequest(listOf("new@justice.gov.uk")))
      }.isInstanceOf(NotFoundException::class.java)

      verifyNoInteractions(telemetryClient)
    }

    @Nested
    @DisplayName("happy path - single existing email replaced with a single new email")
    inner class HappyPath {
      private val agency = AgencyLocation(id = "WWI", description = "Wandsworth")
      private val existingEmail = agencyEmail(agency, 1, "old@justice.gov.uk")

      @BeforeEach
      fun setUp() {
        agency.emailAddresses.add(existingEmail)
        whenever(agencyLocationRepository.findById("WWI")).thenReturn(Optional.of(agency))
      }

      @Test
      fun `will update the existing email address in place preserving its id`() {
        val response = agencyService.updateAgencyEmailAddresses("WWI", UpdateAgencyEmailAddressesRequest(listOf("new@justice.gov.uk")))

        assertThat(response.emailAddresses).hasSize(1)
        assertThat(response.emailAddresses[0].id).isEqualTo(1)
        assertThat(response.emailAddresses[0].emailAddress).isEqualTo("new@justice.gov.uk")
        assertThat(existingEmail.internetAddress).isEqualTo("new@justice.gov.uk")
      }

      @Test
      fun `will not create or delete any email addresses`() {
        agencyService.updateAgencyEmailAddresses("WWI", UpdateAgencyEmailAddressesRequest(listOf("new@justice.gov.uk")))

        verify(agencyLocationInternetAddressRepository, never()).save(any<AgencyLocationInternetAddress>())
        verify(agencyLocationInternetAddressRepository, never()).deleteAll(any<List<AgencyLocationInternetAddress>>())
      }

      @Test
      fun `will raise telemetry event with the email id`() {
        agencyService.updateAgencyEmailAddresses("WWI", UpdateAgencyEmailAddressesRequest(listOf("new@justice.gov.uk")))

        verify(telemetryClient).trackEvent(
          eq("agency.email.updated"),
          check {
            assertThat(it).containsEntry("agencyId", "WWI")
            assertThat(it).containsEntry("emailAddressIds", "1")
          },
          isNull(),
        )
      }

      @Test
      fun `will not raise a telemetry event or change anything if the email address is unchanged`() {
        val response = agencyService.updateAgencyEmailAddresses("WWI", UpdateAgencyEmailAddressesRequest(listOf("old@justice.gov.uk")))

        assertThat(response.emailAddresses[0].emailAddress).isEqualTo("old@justice.gov.uk")
        verifyNoInteractions(telemetryClient)
        verify(agencyLocationInternetAddressRepository, never()).save(any<AgencyLocationInternetAddress>())
      }
    }

    @Nested
    @DisplayName("when the agency has no existing email addresses")
    inner class NoExistingEmails {
      private val agency = AgencyLocation(id = "WWI", description = "Wandsworth")

      @BeforeEach
      fun setUp() {
        whenever(agencyLocationRepository.findById("WWI")).thenReturn(Optional.of(agency))
      }

      @Test
      fun `will create a new email address for each requested email`() {
        whenever(agencyLocationInternetAddressRepository.save(any<AgencyLocationInternetAddress>())).thenAnswer {
          agencyEmail(agency, 10, (it.arguments[0] as AgencyLocationInternetAddress).internetAddress)
        }

        val response = agencyService.updateAgencyEmailAddresses(
          "WWI",
          UpdateAgencyEmailAddressesRequest(listOf("one@justice.gov.uk", "two@justice.gov.uk")),
        )

        assertThat(response.emailAddresses).extracting("emailAddress")
          .containsExactly("one@justice.gov.uk", "two@justice.gov.uk")
        verify(agencyLocationInternetAddressRepository, org.mockito.kotlin.times(2)).save(any<AgencyLocationInternetAddress>())
      }

      @Test
      fun `will not raise a telemetry event or do anything if no emails requested and none exist`() {
        val response = agencyService.updateAgencyEmailAddresses("WWI", UpdateAgencyEmailAddressesRequest(emptyList()))

        assertThat(response.emailAddresses).isEmpty()
        verifyNoInteractions(telemetryClient)
      }
    }

    @Nested
    @DisplayName("when the requested list is longer than the existing list")
    inner class GrowingList {
      private val agency = AgencyLocation(id = "WWI", description = "Wandsworth")
      private val existingEmail = agencyEmail(agency, 1, "existing@justice.gov.uk")

      @BeforeEach
      fun setUp() {
        agency.emailAddresses.add(existingEmail)
        whenever(agencyLocationRepository.findById("WWI")).thenReturn(Optional.of(agency))
        whenever(agencyLocationInternetAddressRepository.save(any<AgencyLocationInternetAddress>())).thenAnswer {
          agencyEmail(agency, 2, (it.arguments[0] as AgencyLocationInternetAddress).internetAddress)
        }
      }

      @Test
      fun `will update the existing email address and create a new one for the extra email`() {
        val response = agencyService.updateAgencyEmailAddresses(
          "WWI",
          UpdateAgencyEmailAddressesRequest(listOf("existing@justice.gov.uk", "new@justice.gov.uk")),
        )

        assertThat(response.emailAddresses).hasSize(2)
        assertThat(response.emailAddresses[0].id).isEqualTo(1)
        assertThat(response.emailAddresses[0].emailAddress).isEqualTo("existing@justice.gov.uk")
        assertThat(response.emailAddresses[1].id).isEqualTo(2)
        assertThat(response.emailAddresses[1].emailAddress).isEqualTo("new@justice.gov.uk")
        verify(agencyLocationInternetAddressRepository, never()).deleteAll(any<List<AgencyLocationInternetAddress>>())
      }

      @Test
      fun `will raise telemetry event with ids for all resulting emails`() {
        agencyService.updateAgencyEmailAddresses(
          "WWI",
          UpdateAgencyEmailAddressesRequest(listOf("existing@justice.gov.uk", "new@justice.gov.uk")),
        )

        verify(telemetryClient).trackEvent(
          eq("agency.email.updated"),
          check {
            assertThat(it).containsEntry("agencyId", "WWI")
            assertThat(it).containsEntry("emailAddressIds", "1,2")
          },
          isNull(),
        )
      }

      @Test
      fun `will update the existing email address in place and create a new one even when none of the requested emails match the original`() {
        val response = agencyService.updateAgencyEmailAddresses(
          "WWI",
          UpdateAgencyEmailAddressesRequest(listOf("new1@justice.gov.uk", "new2@justice.gov.uk")),
        )

        assertThat(response.emailAddresses).hasSize(2)
        assertThat(response.emailAddresses[0].id).isEqualTo(1)
        assertThat(response.emailAddresses[0].emailAddress).isEqualTo("new1@justice.gov.uk")
        assertThat(response.emailAddresses[1].id).isEqualTo(2)
        assertThat(response.emailAddresses[1].emailAddress).isEqualTo("new2@justice.gov.uk")
        assertThat(existingEmail.internetAddress).isEqualTo("new1@justice.gov.uk")

        verify(agencyLocationInternetAddressRepository, org.mockito.kotlin.times(1)).save(any<AgencyLocationInternetAddress>())
        verify(agencyLocationInternetAddressRepository, never()).deleteAll(any<List<AgencyLocationInternetAddress>>())
        verify(telemetryClient).trackEvent(
          eq("agency.email.updated"),
          check {
            assertThat(it).containsEntry("agencyId", "WWI")
            assertThat(it).containsEntry("emailAddressIds", "1,2")
          },
          isNull(),
        )
      }
    }

    @Nested
    @DisplayName("when the requested list is shorter than the existing list")
    inner class ShrinkingList {
      private val agency = AgencyLocation(id = "WWI", description = "Wandsworth")
      private val existingEmail1 = agencyEmail(agency, 1, "one@justice.gov.uk")
      private val existingEmail2 = agencyEmail(agency, 2, "two@justice.gov.uk")

      @BeforeEach
      fun setUp() {
        agency.emailAddresses.add(existingEmail1)
        agency.emailAddresses.add(existingEmail2)
        whenever(agencyLocationRepository.findById("WWI")).thenReturn(Optional.of(agency))
      }

      @Test
      fun `will update the first email address and remove the additional ones`() {
        val response = agencyService.updateAgencyEmailAddresses(
          "WWI",
          UpdateAgencyEmailAddressesRequest(listOf("updated@justice.gov.uk")),
        )

        assertThat(response.emailAddresses).hasSize(1)
        assertThat(response.emailAddresses[0].id).isEqualTo(1)
        assertThat(response.emailAddresses[0].emailAddress).isEqualTo("updated@justice.gov.uk")

        verify(agencyLocationInternetAddressRepository).deleteAll(listOf(existingEmail2))
        verify(agencyLocationInternetAddressRepository, never()).save(any<AgencyLocationInternetAddress>())
      }

      @Test
      fun `will remove all email addresses when requesting an empty list`() {
        val response = agencyService.updateAgencyEmailAddresses("WWI", UpdateAgencyEmailAddressesRequest(emptyList()))

        assertThat(response.emailAddresses).isEmpty()
        verify(agencyLocationInternetAddressRepository).deleteAll(listOf(existingEmail1, existingEmail2))
        verify(telemetryClient).trackEvent(
          eq("agency.email.updated"),
          check {
            assertThat(it).containsEntry("agencyId", "WWI")
            assertThat(it).containsEntry("emailAddressIds", "")
          },
          isNull(),
        )
      }

      @Test
      fun `will remove the extra email address when the single requested email matches the first original email`() {
        val response = agencyService.updateAgencyEmailAddresses(
          "WWI",
          UpdateAgencyEmailAddressesRequest(listOf("one@justice.gov.uk")),
        )

        assertThat(response.emailAddresses).hasSize(1)
        assertThat(response.emailAddresses[0].id).isEqualTo(1)
        assertThat(response.emailAddresses[0].emailAddress).isEqualTo("one@justice.gov.uk")

        verify(agencyLocationInternetAddressRepository).deleteAll(listOf(existingEmail2))
        verify(agencyLocationInternetAddressRepository, never()).save(any<AgencyLocationInternetAddress>())
        verify(telemetryClient).trackEvent(
          eq("agency.email.updated"),
          check {
            assertThat(it).containsEntry("agencyId", "WWI")
            assertThat(it).containsEntry("emailAddressIds", "1")
          },
          isNull(),
        )
      }
    }
  }

  @Nested
  @DisplayName("createAgencyPhone")
  inner class CreateAgencyPhone {
    private val agency = AgencyLocation(id = "WWI", description = "Wandsworth")
    private val phoneUsage = PhoneUsage("BUS", "Business")

    @BeforeEach
    fun setUp() {
      whenever(agencyLocationRepository.findById("WWI")).thenReturn(Optional.of(agency))
      whenever(phoneUsageRepository.findById(PhoneUsage.pk("BUS"))).thenReturn(Optional.of(phoneUsage))
      whenever(agencyLocationPhoneRepository.saveAndFlush(any<AgencyLocationPhone>())).thenAnswer {
        (it.arguments[0] as AgencyLocationPhone).also { phone -> ReflectionTestUtils.setField(phone, "phoneId", 1L) }
      }
    }

    @Test
    fun `will throw not found if agency does not exist`() {
      whenever(agencyLocationRepository.findById("ZZI")).thenReturn(Optional.empty())

      assertThatThrownBy {
        agencyService.createAgencyPhone("ZZI", CreateAgencyPhoneNumberRequest(number = "0114 555 555", typeCode = "BUS"))
      }.isInstanceOf(NotFoundException::class.java)

      verifyNoInteractions(telemetryClient)
    }

    @Test
    fun `will throw bad data if phone type does not exist`() {
      whenever(phoneUsageRepository.findById(PhoneUsage.pk("RUBBISH"))).thenReturn(Optional.empty())

      assertThatThrownBy {
        agencyService.createAgencyPhone("WWI", CreateAgencyPhoneNumberRequest(number = "0114 555 555", typeCode = "RUBBISH"))
      }.isInstanceOf(BadDataException::class.java)

      verifyNoInteractions(telemetryClient)
    }

    @Test
    fun `will create a phone number at the agency level`() {
      val response = agencyService.createAgencyPhone(
        "WWI",
        CreateAgencyPhoneNumberRequest(number = "0114 555 555", extension = "x432", typeCode = "BUS"),
      )

      assertThat(response.id).isEqualTo(1)
      verify(agencyLocationPhoneRepository).saveAndFlush(
        check {
          assertThat(it.agencyLocation).isEqualTo(agency)
          assertThat(it.phoneNo).isEqualTo("0114 555 555")
          assertThat(it.extNo).isEqualTo("x432")
          assertThat(it.phoneType).isEqualTo(phoneUsage)
        },
      )
    }

    @Test
    fun `will raise telemetry event with the phone id`() {
      val response = agencyService.createAgencyPhone("WWI", CreateAgencyPhoneNumberRequest(number = "0114 555 555", typeCode = "BUS"))

      verify(telemetryClient).trackEvent(
        eq("agency-phone-inserted"),
        check {
          assertThat(it).containsEntry("agencyId", "WWI")
          assertThat(it).containsEntry("phoneId", response.id.toString())
        },
        isNull(),
      )
    }
  }

  @Nested
  @DisplayName("createAgencyAddress")
  inner class CreateAgencyAddress {
    private val agency = AgencyLocation(id = "WWI", description = "Wandsworth")
    private val addressType = AddressType("BUS", "Business Address")
    private val city = City("25343", "Sheffield")
    private val county = County("S.YORKSHIRE", "South Yorkshire")
    private val country = Country("ENG", "England")

    @BeforeEach
    fun setUp() {
      whenever(agencyLocationRepository.findById("WWI")).thenReturn(Optional.of(agency))
      whenever(addressTypeRepository.findById(AddressType.pk("BUS"))).thenReturn(Optional.of(addressType))
      whenever(cityRepository.findByDomainAndDescriptionIgnoreCase(City.CITY, "Sheffield")).thenReturn(city)
      whenever(countyRepository.findByDomainAndDescriptionIgnoreCase(County.COUNTY, "South Yorkshire")).thenReturn(county)
      whenever(countryRepository.findByDomainAndDescriptionIgnoreCase(Country.COUNTRY, "England")).thenReturn(country)
      whenever(agencyLocationAddressRepository.saveAndFlush(any<AgencyLocationAddress>())).thenAnswer {
        (it.arguments[0] as AgencyLocationAddress).also { address -> ReflectionTestUtils.setField(address, "addressId", 1L) }
      }
    }

    @Test
    fun `will throw not found if agency does not exist`() {
      whenever(agencyLocationRepository.findById("ZZI")).thenReturn(Optional.empty())

      assertThatThrownBy {
        agencyService.createAgencyAddress("ZZI", CreateAgencyAddressRequest())
      }.isInstanceOf(NotFoundException::class.java)

      verifyNoInteractions(telemetryClient)
    }

    @Test
    fun `will throw bad data if the default BUS address type does not exist`() {
      whenever(addressTypeRepository.findById(AddressType.pk("BUS"))).thenReturn(Optional.empty())

      assertThatThrownBy {
        agencyService.createAgencyAddress("WWI", CreateAgencyAddressRequest())
      }.isInstanceOf(BadDataException::class.java)

      verifyNoInteractions(telemetryClient)
    }

    @Test
    fun `will set city to null and record telemetry if city does not exist`() {
      whenever(cityRepository.findByDomainAndDescriptionIgnoreCase(City.CITY, "Rubbish")).thenReturn(null)

      val response = agencyService.createAgencyAddress("WWI", CreateAgencyAddressRequest(city = "Rubbish"))

      verify(agencyLocationAddressRepository).saveAndFlush(
        check {
          assertThat(it.city).isNull()
        },
      )
      verify(telemetryClient).trackEvent(
        eq("agency-address-inserted"),
        check {
          assertThat(it).containsEntry("agencyId", "WWI")
          assertThat(it).containsEntry("addressId", response.id.toString())
          assertThat(it).containsEntry("cityLookupFailure", "Rubbish")
        },
        isNull(),
      )
    }

    @Test
    fun `will set county to null and record telemetry if county does not exist`() {
      whenever(countyRepository.findByDomainAndDescriptionIgnoreCase(County.COUNTY, "Rubbish")).thenReturn(null)

      val response = agencyService.createAgencyAddress("WWI", CreateAgencyAddressRequest(county = "Rubbish"))

      verify(agencyLocationAddressRepository).saveAndFlush(
        check {
          assertThat(it.county).isNull()
        },
      )
      verify(telemetryClient).trackEvent(
        eq("agency-address-inserted"),
        check {
          assertThat(it).containsEntry("agencyId", "WWI")
          assertThat(it).containsEntry("addressId", response.id.toString())
          assertThat(it).containsEntry("countyLookupFailure", "Rubbish")
        },
        isNull(),
      )
    }

    @Test
    fun `will set country to null and record telemetry if country does not exist`() {
      whenever(countryRepository.findByDomainAndDescriptionIgnoreCase(Country.COUNTRY, "Rubbish")).thenReturn(null)

      val response = agencyService.createAgencyAddress("WWI", CreateAgencyAddressRequest(country = "Rubbish"))

      verify(agencyLocationAddressRepository).saveAndFlush(
        check {
          assertThat(it.country).isNull()
        },
      )
      verify(telemetryClient).trackEvent(
        eq("agency-address-inserted"),
        check {
          assertThat(it).containsEntry("agencyId", "WWI")
          assertThat(it).containsEntry("addressId", response.id.toString())
          assertThat(it).containsEntry("countryLookupFailure", "Rubbish")
        },
        isNull(),
      )
    }

    @Test
    fun `will match city, county and country descriptions case insensitively`() {
      whenever(cityRepository.findByDomainAndDescriptionIgnoreCase(City.CITY, "SHEFFIELD")).thenReturn(city)
      whenever(countyRepository.findByDomainAndDescriptionIgnoreCase(County.COUNTY, "south yorkshire")).thenReturn(county)
      whenever(countryRepository.findByDomainAndDescriptionIgnoreCase(Country.COUNTRY, "EnglanD")).thenReturn(country)

      agencyService.createAgencyAddress(
        "WWI",
        CreateAgencyAddressRequest(city = "SHEFFIELD", county = "south yorkshire", country = "EnglanD"),
      )

      verify(agencyLocationAddressRepository).saveAndFlush(
        check {
          assertThat(it.city).isEqualTo(city)
          assertThat(it.county).isEqualTo(county)
          assertThat(it.country).isEqualTo(country)
        },
      )
    }

    @Test
    fun `will create an address at the agency level, defaulting the address type and looking up other reference data by description`() {
      val response = agencyService.createAgencyAddress(
        "WWI",
        CreateAgencyAddressRequest(
          flat = "1A",
          premise = "Bolden Court",
          street = "Fulwood Road",
          locality = "Broomhill",
          postcode = "S10 2HH",
          city = "Sheffield",
          county = "South Yorkshire",
          country = "England",
        ),
      )

      assertThat(response.id).isEqualTo(1)
      verify(agencyLocationAddressRepository).saveAndFlush(
        check {
          assertThat(it.agencyLocation).isEqualTo(agency)
          assertThat(it.addressType).isEqualTo(addressType)
          assertThat(it.flat).isEqualTo("1A")
          assertThat(it.premise).isEqualTo("Bolden Court")
          assertThat(it.street).isEqualTo("Fulwood Road")
          assertThat(it.locality).isEqualTo("Broomhill")
          assertThat(it.postalCode).isEqualTo("S10 2HH")
          assertThat(it.city).isEqualTo(city)
          assertThat(it.county).isEqualTo(county)
          assertThat(it.country).isEqualTo(country)
        },
      )
    }

    @Test
    fun `will default primary, mail and no-fixed-address flags, comment and dates rather than take them from the request`() {
      agencyService.createAgencyAddress("WWI", CreateAgencyAddressRequest())

      verify(agencyLocationAddressRepository).saveAndFlush(
        check {
          assertThat(it.primaryAddress).isFalse()
          assertThat(it.mailAddress).isFalse()
          assertThat(it.noFixedAddress).isFalse()
          assertThat(it.comment).isNull()
          assertThat(it.startDate).isEqualTo(java.time.LocalDate.now())
          assertThat(it.endDate).isNull()
        },
      )
    }

    @Test
    fun `will raise telemetry event with the address id`() {
      val response = agencyService.createAgencyAddress("WWI", CreateAgencyAddressRequest())

      verify(telemetryClient).trackEvent(
        eq("agency-address-inserted"),
        check {
          assertThat(it).containsEntry("agencyId", "WWI")
          assertThat(it).containsEntry("addressId", response.id.toString())
        },
        isNull(),
      )
    }
  }

  @Nested
  @DisplayName("updateAgencyPhoneNumbers")
  inner class UpdateAgencyPhoneNumbers {
    private val busPhoneUsage = PhoneUsage("BUS", "Business")
    private val homePhoneUsage = PhoneUsage("HOME", "Home")

    private fun agencyPhone(agency: AgencyLocation, id: Long, number: String, phoneType: PhoneUsage = busPhoneUsage, extension: String? = null) = AgencyLocationPhone(
      agencyLocation = agency,
      phoneType = phoneType,
      phoneNo = number,
      extNo = extension,
    ).also { ReflectionTestUtils.setField(it, "phoneId", id) }

    @BeforeEach
    fun setUp() {
      whenever(phoneUsageRepository.findById(PhoneUsage.pk("BUS"))).thenReturn(Optional.of(busPhoneUsage))
      whenever(phoneUsageRepository.findById(PhoneUsage.pk("HOME"))).thenReturn(Optional.of(homePhoneUsage))
    }

    @Test
    fun `will throw not found if agency does not exist`() {
      whenever(agencyLocationRepository.findById("ZZI")).thenReturn(Optional.empty())

      assertThatThrownBy {
        agencyService.updateAgencyPhoneNumbers(
          "ZZI",
          UpdateAgencyPhoneNumbersRequest(listOf(UpdateAgencyPhoneNumber(number = "0114 555 555", typeCode = "BUS"))),
        )
      }.isInstanceOf(NotFoundException::class.java)
    }

    @Nested
    @DisplayName("happy path - single existing phone number replaced with a single new one")
    inner class HappyPath {
      private val agency = AgencyLocation(id = "WWI", description = "Wandsworth")
      private val existingPhone = agencyPhone(agency, 1, "0114 555 555")

      @BeforeEach
      fun setUp() {
        agency.phones.add(existingPhone)
        whenever(agencyLocationRepository.findById("WWI")).thenReturn(Optional.of(agency))
      }

      @Test
      fun `will update the existing phone number in place preserving its id`() {
        val response = agencyService.updateAgencyPhoneNumbers(
          "WWI",
          UpdateAgencyPhoneNumbersRequest(listOf(UpdateAgencyPhoneNumber(number = "0114 999 999", extension = "x432", typeCode = "HOME"))),
        )

        assertThat(response.phoneNumbers).hasSize(1)
        assertThat(response.phoneNumbers[0].id).isEqualTo(1)
        assertThat(response.phoneNumbers[0].number).isEqualTo("0114 999 999")
        assertThat(response.phoneNumbers[0].extension).isEqualTo("x432")
        assertThat(existingPhone.phoneNo).isEqualTo("0114 999 999")
        assertThat(existingPhone.phoneType).isEqualTo(homePhoneUsage)
      }

      @Test
      fun `will not raise a telemetry event or change anything if the phone number is unchanged`() {
        val response = agencyService.updateAgencyPhoneNumbers(
          "WWI",
          UpdateAgencyPhoneNumbersRequest(listOf(UpdateAgencyPhoneNumber(number = "0114 555 555", typeCode = "BUS"))),
        )

        assertThat(response.phoneNumbers[0].number).isEqualTo("0114 555 555")
        verifyNoInteractions(telemetryClient)
      }
    }

    @Nested
    @DisplayName("when a requested number already exists against one of the agency's addresses")
    inner class AddressLevelNumber {
      private val agency = AgencyLocation(id = "WWI", description = "Wandsworth")
      private val address = AgencyLocationAddress(agencyLocation = agency, premise = "22")
      private val addressPhone = AddressPhone(address = address, phoneType = busPhoneUsage, phoneNo = "0114 111 111")
        .also { ReflectionTestUtils.setField(it, "phoneId", 99L) }

      @BeforeEach
      fun setUp() {
        address.phones.add(addressPhone)
        agency.addresses.add(address)
        whenever(agencyLocationRepository.findById("WWI")).thenReturn(Optional.of(agency))
      }

      @Test
      fun `will leave the address-level phone number unchanged and not create an agency-level duplicate`() {
        val response = agencyService.updateAgencyPhoneNumbers(
          "WWI",
          UpdateAgencyPhoneNumbersRequest(listOf(UpdateAgencyPhoneNumber(number = "0114 111 111", typeCode = "BUS"))),
        )

        assertThat(response.phoneNumbers).isEmpty()
        assertThat(address.phones).containsExactly(addressPhone)
        verifyNoInteractions(telemetryClient)
      }

      @Test
      fun `will add any other requested number at the agency level while leaving the address number alone`() {
        whenever(agencyLocationPhoneRepository.save(any<AgencyLocationPhone>())).thenAnswer {
          (it.arguments[0] as AgencyLocationPhone).also { phone -> ReflectionTestUtils.setField(phone, "phoneId", 2L) }
        }

        val response = agencyService.updateAgencyPhoneNumbers(
          "WWI",
          UpdateAgencyPhoneNumbersRequest(
            listOf(
              UpdateAgencyPhoneNumber(number = "0114 111 111", typeCode = "BUS"),
              UpdateAgencyPhoneNumber(number = "0114 222 222", typeCode = "HOME"),
            ),
          ),
        )

        assertThat(response.phoneNumbers).hasSize(1)
        assertThat(response.phoneNumbers[0].number).isEqualTo("0114 222 222")
        assertThat(address.phones).containsExactly(addressPhone)
      }
    }

    @Nested
    @DisplayName("shrinking the list")
    inner class ShrinkingList {
      private val agency = AgencyLocation(id = "WWI", description = "Wandsworth")
      private val existingPhone1 = agencyPhone(agency, 1, "0114 111 111")
      private val existingPhone2 = agencyPhone(agency, 2, "0114 222 222")

      @BeforeEach
      fun setUp() {
        agency.phones.add(existingPhone1)
        agency.phones.add(existingPhone2)
        whenever(agencyLocationRepository.findById("WWI")).thenReturn(Optional.of(agency))
      }

      @Test
      fun `will remove all phone numbers when requesting an empty list`() {
        val response = agencyService.updateAgencyPhoneNumbers("WWI", UpdateAgencyPhoneNumbersRequest(emptyList()))

        assertThat(response.phoneNumbers).isEmpty()
        verify(agencyLocationPhoneRepository).deleteAll(listOf(existingPhone1, existingPhone2))
        verify(telemetryClient).trackEvent(
          eq("agency.phone.updated"),
          check {
            assertThat(it).containsEntry("phoneIds", "")
          },
          isNull(),
        )
      }

      @Test
      fun `will keep the id of the matching existing phone number and remove the other one`() {
        val response = agencyService.updateAgencyPhoneNumbers(
          "WWI",
          UpdateAgencyPhoneNumbersRequest(listOf(UpdateAgencyPhoneNumber(number = "0114 111 111", typeCode = "BUS"))),
        )

        assertThat(response.phoneNumbers).hasSize(1)
        assertThat(response.phoneNumbers[0].id).isEqualTo(1)
        assertThat(response.phoneNumbers[0].number).isEqualTo("0114 111 111")
        verify(agencyLocationPhoneRepository, never()).deleteAll(listOf(existingPhone1))
        verify(agencyLocationPhoneRepository).deleteAll(listOf(existingPhone2))
      }
    }
  }

  @Nested
  @DisplayName("updateAgencyAddresses")
  inner class UpdateAgencyAddresses {
    private val addressType = AddressType("BUS", "Business Address")
    private val city = City("25343", "Sheffield")
    private val county = County("S.YORKSHIRE", "South Yorkshire")
    private val country = Country("ENG", "England")

    private fun agencyAddress(agency: AgencyLocation, id: Long, postcode: String? = null, premise: String? = null) = AgencyLocationAddress(
      agencyLocation = agency,
      addressType = addressType,
      postalCode = postcode,
      premise = premise,
    ).also { ReflectionTestUtils.setField(it, "addressId", id) }

    @BeforeEach
    fun setUp() {
      whenever(addressTypeRepository.findById(AddressType.pk("BUS"))).thenReturn(Optional.of(addressType))
      whenever(cityRepository.findByDomainAndDescriptionIgnoreCase(City.CITY, "Sheffield")).thenReturn(city)
      whenever(countyRepository.findByDomainAndDescriptionIgnoreCase(County.COUNTY, "South Yorkshire")).thenReturn(county)
      whenever(countryRepository.findByDomainAndDescriptionIgnoreCase(Country.COUNTRY, "England")).thenReturn(country)
    }

    @Test
    fun `will throw not found if agency does not exist`() {
      whenever(agencyLocationRepository.findById("ZZI")).thenReturn(Optional.empty())

      assertThatThrownBy {
        agencyService.updateAgencyAddresses("ZZI", UpdateAgencyAddressesRequest(listOf(UpdateAgencyAddress(premise = "22"))))
      }.isInstanceOf(NotFoundException::class.java)
    }

    @Nested
    @DisplayName("happy path - single existing address replaced with a single new one")
    inner class HappyPath {
      private val agency = AgencyLocation(id = "WWI", description = "Wandsworth")
      private val existingAddress = agencyAddress(agency, 1, postcode = "S10 1AA", premise = "Old Premise")

      @BeforeEach
      fun setUp() {
        agency.addresses.add(existingAddress)
        whenever(agencyLocationRepository.findById("WWI")).thenReturn(Optional.of(agency))
      }

      @Test
      fun `will update the existing address in place preserving its id, looking up reference data by description`() {
        val response = agencyService.updateAgencyAddresses(
          "WWI",
          UpdateAgencyAddressesRequest(
            listOf(
              UpdateAgencyAddress(
                premise = "New Premise",
                street = "Fulwood Road",
                postcode = "S10 2HH",
                city = "Sheffield",
                county = "South Yorkshire",
                country = "England",
              ),
            ),
          ),
        )

        assertThat(response.addresses).hasSize(1)
        assertThat(response.addresses[0].id).isEqualTo(1)
        assertThat(response.addresses[0].premise).isEqualTo("New Premise")
        assertThat(existingAddress.premise).isEqualTo("New Premise")
        assertThat(existingAddress.street).isEqualTo("Fulwood Road")
        assertThat(existingAddress.postalCode).isEqualTo("S10 2HH")
        assertThat(existingAddress.city).isEqualTo(city)
        assertThat(existingAddress.county).isEqualTo(county)
        assertThat(existingAddress.country).isEqualTo(country)
        // address type is not part of the update request and is left unchanged
        assertThat(existingAddress.addressType).isEqualTo(addressType)
      }

      @Test
      fun `will match the single existing and requested address directly even when postcodes differ`() {
        val response = agencyService.updateAgencyAddresses(
          "WWI",
          UpdateAgencyAddressesRequest(listOf(UpdateAgencyAddress(premise = "New Premise", postcode = "DIFFERENT"))),
        )

        assertThat(response.addresses).hasSize(1)
        assertThat(response.addresses[0].id).isEqualTo(1)
        assertThat(existingAddress.premise).isEqualTo("New Premise")
      }

      @Test
      fun `will raise telemetry event with the address id`() {
        val response = agencyService.updateAgencyAddresses(
          "WWI",
          UpdateAgencyAddressesRequest(listOf(UpdateAgencyAddress(premise = "New Premise"))),
        )

        verify(telemetryClient).trackEvent(
          eq("agency.address.updated"),
          check {
            assertThat(it).containsEntry("agencyId", "WWI")
            assertThat(it).containsEntry("addressIds", response.addresses[0].id.toString())
          },
          isNull(),
        )
      }

      @Test
      fun `will set city to null and record a lookup failure in telemetry if city does not exist`() {
        val response = agencyService.updateAgencyAddresses(
          "WWI",
          UpdateAgencyAddressesRequest(listOf(UpdateAgencyAddress(city = "Rubbish"))),
        )

        assertThat(existingAddress.city).isNull()
        verify(telemetryClient).trackEvent(
          eq("agency.address.updated"),
          check {
            assertThat(it).containsEntry("addressIds", response.addresses[0].id.toString())
            assertThat(it).containsEntry("cityLookupFailure", "Rubbish")
          },
          isNull(),
        )
      }
    }

    @Nested
    @DisplayName("when there is more than one address")
    inner class MultipleAddresses {
      private val agency = AgencyLocation(id = "WWI", description = "Wandsworth")
      private val existingAddress1 = agencyAddress(agency, 1, postcode = "AA1 1AA", premise = "First")
      private val existingAddress2 = agencyAddress(agency, 2, postcode = "BB2 2BB", premise = "Second")

      @BeforeEach
      fun setUp() {
        agency.addresses.add(existingAddress1)
        agency.addresses.add(existingAddress2)
        whenever(agencyLocationRepository.findById("WWI")).thenReturn(Optional.of(agency))
      }

      @Test
      fun `will match existing and requested addresses by ordering both by postcode`() {
        val response = agencyService.updateAgencyAddresses(
          "WWI",
          UpdateAgencyAddressesRequest(
            listOf(
              // supplied in the opposite order to the existing addresses, should still match on postcode
              UpdateAgencyAddress(premise = "Updated Second", postcode = "BB2 2BB"),
              UpdateAgencyAddress(premise = "Updated First", postcode = "AA1 1AA"),
            ),
          ),
        )

        assertThat(response.addresses).hasSize(2)
        assertThat(existingAddress1.premise).isEqualTo("Updated First")
        assertThat(existingAddress2.premise).isEqualTo("Updated Second")
      }

      @Test
      fun `will remove all addresses when requesting an empty list`() {
        val response = agencyService.updateAgencyAddresses("WWI", UpdateAgencyAddressesRequest(emptyList()))

        assertThat(response.addresses).isEmpty()
        verify(agencyLocationAddressRepository).deleteAll(listOf(existingAddress1, existingAddress2))
      }

      @Test
      fun `will create a new address, defaulting the address type to BUS, when requesting more addresses than exist`() {
        whenever(agencyLocationAddressRepository.save(any<AgencyLocationAddress>())).thenAnswer {
          (it.arguments[0] as AgencyLocationAddress).also { address -> ReflectionTestUtils.setField(address, "addressId", 3L) }
        }

        val response = agencyService.updateAgencyAddresses(
          "WWI",
          UpdateAgencyAddressesRequest(
            listOf(
              UpdateAgencyAddress(premise = "First", postcode = "AA1 1AA"),
              UpdateAgencyAddress(premise = "Second", postcode = "BB2 2BB"),
              UpdateAgencyAddress(premise = "Third", postcode = "ZZ9 9ZZ"),
            ),
          ),
        )

        assertThat(response.addresses).hasSize(3)
        verify(agencyLocationAddressRepository).save(
          check<AgencyLocationAddress> {
            assertThat(it.premise).isEqualTo("Third")
            assertThat(it.addressType).isEqualTo(addressType)
          },
        )
      }

      @Test
      fun `will record each address's lookup failure separately in telemetry`() {
        val response = agencyService.updateAgencyAddresses(
          "WWI",
          UpdateAgencyAddressesRequest(
            listOf(
              UpdateAgencyAddress(premise = "First", postcode = "AA1 1AA", city = "Rubbish 1"),
              UpdateAgencyAddress(premise = "Second", postcode = "BB2 2BB", city = "Rubbish 2"),
            ),
          ),
        )

        verify(telemetryClient).trackEvent(
          eq("agency.address.updated"),
          check {
            assertThat(it).containsEntry("addressIds", "${response.addresses[0].id},${response.addresses[1].id}")
            assertThat(it).containsEntry("cityLookupFailure[0]", "Rubbish 1")
            assertThat(it).containsEntry("cityLookupFailure[1]", "Rubbish 2")
          },
          isNull(),
        )
      }
    }
  }
}
