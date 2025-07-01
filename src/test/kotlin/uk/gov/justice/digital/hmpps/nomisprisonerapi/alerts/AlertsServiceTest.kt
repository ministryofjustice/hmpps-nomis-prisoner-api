package uk.gov.justice.digital.hmpps.nomisprisonerapi.alerts

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AlertCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AlertStatus.ACTIVE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AlertStatus.INACTIVE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AlertType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Gender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAlert
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAlertId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class AlertsServiceTest {
  private lateinit var alertsService: AlertsService

  @Mock
  lateinit var offenderBookingRepository: OffenderBookingRepository

  @BeforeEach
  fun setUp() {
    alertsService = AlertsService(
      offenderBookingRepository = offenderBookingRepository,
      offenderAlertRepository = mock(),
      alertCodeRepository = mock(),
      alertTypeRepository = mock(),
      workFlowActionRepository = mock(),
    )
  }

  @Nested
  inner class GetAlerts {
    @Nested
    @DisplayName("With no alerts on any booking")
    inner class WithNoAlertsAtAll {
      @BeforeEach
      fun setUp() {
        whenever(offenderBookingRepository.findAllByOffenderNomsId("A1234KT")).thenReturn(
          listOf(
            booking(),
          ),
        )
      }

      @Test
      fun `there will be no alerts`() {
        val alerts = alertsService.getAlerts("A1234KT")

        assertThat(alerts.latestBookingAlerts).isEmpty()
      }
    }

    @Nested
    @DisplayName("With alerts only on the latest booking")
    inner class WithAlertsOnlyOnLatestBooking {
      @BeforeEach
      fun setUp() {
        whenever(offenderBookingRepository.findAllByOffenderNomsId("A1234KT")).thenReturn(
          listOf(
            booking().apply {
              this.alerts += listOf(
                alert(booking = this, sequence = 1, alertCode = "A"),
                alert(booking = this, sequence = 2, alertCode = "B"),
              )
            },
          ),
        )
      }

      @Test
      fun `there will be two alerts on latest`() {
        val alerts = alertsService.getAlerts("A1234KT")

        assertThat(alerts.latestBookingAlerts).extracting<String> { it.alertCode.code }.containsOnly("A", "B")
      }
    }

    @Nested
    @DisplayName("With alerts only on the previous booking")
    inner class WithAlertsOnlyOnPreviousBooking {
      @BeforeEach
      fun setUp() {
        whenever(offenderBookingRepository.findAllByOffenderNomsId("A1234KT")).thenReturn(
          listOf(
            booking(
              bookingSequence = 1,
            ),
            booking(
              bookingSequence = 2,
            ).apply {
              this.alerts += listOf(
                alert(booking = this, sequence = 1, alertCode = "A"),
                alert(booking = this, sequence = 2, alertCode = "B"),
              )
            },
          ),
        )
      }

      @Test
      fun `there will be no alerts on latest`() {
        val alerts = alertsService.getAlerts("A1234KT")

        assertThat(alerts.latestBookingAlerts).isEmpty()
      }
    }
  }
}

private fun booking(bookingSequence: Int = 1): OffenderBooking = OffenderBooking(
  bookingSequence = bookingSequence,
  bookingBeginDate = LocalDateTime.now(),
  offender = Offender(nomsId = "A1234KT", gender = Gender("M", "MALE"), lastName = "SMITH", firstName = "JOHN"),
  location = AgencyLocation("LEI", "Leeds"),
)

private fun alert(
  sequence: Long = 1,
  alertCode: String = "A",
  alertDate: LocalDate = LocalDate.now(),
  auditTimestamp: LocalDateTime? = LocalDateTime.now(),
  active: Boolean = true,
  booking: OffenderBooking,
): OffenderAlert = OffenderAlert(
  id = OffenderAlertId(booking, sequence),
  alertCode = AlertCode(alertCode, "A Alert", parentCode = "X"),
  alertType = AlertType("X", "X Type"),
  alertDate = alertDate,
  createUsername = "BOB",
  alertStatus = if (active) ACTIVE else INACTIVE,

).apply {
  this.auditTimestamp = auditTimestamp
  this.createDatetime = LocalDateTime.now()
}
