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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationInternetAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationPhone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.InternetAddress.Companion.EMAIL_INTERNET_ADDRESS_CLASS
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PhoneUsage
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationInternetAddressRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationPhoneRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.util.Optional

class AgencyServiceTest {
  private val agencyLocationRepository: AgencyLocationRepository = mock()
  private val agencyLocationInternetAddressRepository: AgencyLocationInternetAddressRepository = mock()
  private val agencyLocationPhoneRepository: AgencyLocationPhoneRepository = mock()
  private val phoneUsageRepository: ReferenceCodeRepository<PhoneUsage> = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val agencyService = AgencyService(
    agencyLocationRepository = agencyLocationRepository,
    agencyLocationInternetAddressRepository = agencyLocationInternetAddressRepository,
    agencyLocationPhoneRepository = agencyLocationPhoneRepository,
    phoneUsageRepository = phoneUsageRepository,
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
}
