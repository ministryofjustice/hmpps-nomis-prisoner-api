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
        assertThat(alerts.previousBookingsAlerts).isEmpty()
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
      fun `there will be two alerts on latest and none on previous`() {
        val alerts = alertsService.getAlerts("A1234KT")

        assertThat(alerts.latestBookingAlerts).extracting<String> { it.alertCode.code }.containsOnly("A", "B")
        assertThat(alerts.previousBookingsAlerts).isEmpty()
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
      fun `there will be two alerts on previous and none on latest`() {
        val alerts = alertsService.getAlerts("A1234KT")

        assertThat(alerts.previousBookingsAlerts).extracting<String> { it.alertCode.code }.containsOnly("A", "B")
        assertThat(alerts.latestBookingAlerts).isEmpty()
      }
    }

    @Nested
    @DisplayName("With the same alerts on latest and previous bookings")
    inner class WithSameAlertsOnLatestAndPrevious {
      @BeforeEach
      fun setUp() {
        whenever(offenderBookingRepository.findAllByOffenderNomsId("A1234KT")).thenReturn(
          listOf(
            booking(
              bookingSequence = 1,
            ).apply {
              this.alerts += listOf(
                alert(booking = this, sequence = 1, alertCode = "A"),
                alert(booking = this, sequence = 2, alertCode = "B"),
              )
            },
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
      fun `there will be two alerts on latest and none on previous`() {
        val alerts = alertsService.getAlerts("A1234KT")

        assertThat(alerts.latestBookingAlerts).extracting<String> { it.alertCode.code }.containsOnly("A", "B")
        assertThat(alerts.previousBookingsAlerts).isEmpty()
      }
    }

    @Nested
    @DisplayName("With the extra alerts on previous booking")
    inner class WithExtraAlertsOnPrevious {
      @BeforeEach
      fun setUp() {
        whenever(offenderBookingRepository.findAllByOffenderNomsId("A1234KT")).thenReturn(
          listOf(
            booking(
              bookingSequence = 1,
            ).apply {
              this.alerts += listOf(
                alert(booking = this, sequence = 1, alertCode = "A"),
                alert(booking = this, sequence = 2, alertCode = "B"),
              )
            },
            booking(
              bookingSequence = 2,
            ).apply {
              this.alerts += listOf(
                alert(booking = this, sequence = 1, alertCode = "A"),
                alert(booking = this, sequence = 2, alertCode = "B"),
                alert(booking = this, sequence = 3, alertCode = "C"),
              )
            },
          ),
        )
      }

      @Test
      fun `there will be two alerts on latest and one on previous`() {
        val alerts = alertsService.getAlerts("A1234KT")

        assertThat(alerts.latestBookingAlerts).extracting<String> { it.alertCode.code }.containsOnly("A", "B")
        assertThat(alerts.previousBookingsAlerts).extracting<String> { it.alertCode.code }.containsOnly("C")
      }
    }

    @Nested
    @DisplayName("With the extra alerts on several previous booking")
    inner class WithExtraAlertsOnSeveralPreviousBookings {
      @BeforeEach
      fun setUp() {
        whenever(offenderBookingRepository.findAllByOffenderNomsId("A1234KT")).thenReturn(
          listOf(
            booking(
              bookingSequence = 1,
            ).apply {
              this.alerts += listOf(
                alert(booking = this, sequence = 1, alertCode = "A"),
                alert(booking = this, sequence = 2, alertCode = "B"),
              )
            },
            booking(
              bookingSequence = 2,
            ).apply {
              this.alerts += listOf(
                alert(booking = this, sequence = 1, alertCode = "A"),
                alert(booking = this, sequence = 2, alertCode = "B"),
                alert(booking = this, sequence = 3, alertCode = "C"),
              )
            },
            booking(
              bookingSequence = 3,
            ).apply {
              this.alerts += listOf(
                alert(booking = this, sequence = 4, alertCode = "D"),
              )
            },
          ),
        )
      }

      @Test
      fun `there will be two alerts on latest and two on previous`() {
        val alerts = alertsService.getAlerts("A1234KT")

        assertThat(alerts.latestBookingAlerts).extracting<String> { it.alertCode.code }.containsOnly("A", "B")
        assertThat(alerts.previousBookingsAlerts).extracting<String> { it.alertCode.code }.containsOnly("C", "D")
      }
    }

    @Nested
    @DisplayName("With the same extra alerts on previous booking that have the same alert dates but different status")
    inner class WithPreviousBookingAlertDatesSame {
      private val alertDate = LocalDate.parse("2021-01-01")

      @BeforeEach
      fun setUp() {
        whenever(offenderBookingRepository.findAllByOffenderNomsId("A1234KT")).thenReturn(
          listOf(
            booking(
              bookingSequence = 1,
            ).apply {
              this.alerts += listOf(
                alert(booking = this, sequence = 1, alertCode = "A"),
              )
            },
            booking(
              bookingSequence = 2,
            ).apply {
              this.alerts += listOf(
                alert(booking = this, sequence = 20, alertCode = "B", alertDate = alertDate, active = false),
                alert(booking = this, sequence = 30, alertCode = "B", alertDate = alertDate, active = true),
                alert(booking = this, sequence = 40, alertCode = "B", alertDate = alertDate, active = false),
              )
            },
          ),
        )
      }

      @Test
      fun `will choose the active alert over the inactive`() {
        val alerts = alertsService.getAlerts("A1234KT")

        assertThat(alerts.latestBookingAlerts).extracting<String> { it.alertCode.code }.containsOnly("A")
        assertThat(alerts.previousBookingsAlerts).extracting<String> { it.alertCode.code }.containsOnly("B")
        assertThat(alerts.previousBookingsAlerts).extracting<Long> { it.alertSequence }.containsOnly(30)
      }
    }

    @Nested
    @DisplayName("With alerts on older bookings but newer alert dates")
    inner class WithPreviousBookingsAlertDatesNewerOnOlderBooking {
      private val alertDate = LocalDate.parse("2021-01-01")

      @BeforeEach
      fun setUp() {
        whenever(offenderBookingRepository.findAllByOffenderNomsId("A1234KT")).thenReturn(
          listOf(
            booking(
              bookingSequence = 1,
            ).apply {
              this.alerts += listOf(
                alert(booking = this, sequence = 1, alertCode = "A"),
              )
            },
            booking(
              bookingSequence = 2,
            ).apply {
              this.alerts += listOf(
                alert(booking = this, sequence = 20, alertCode = "B", alertDate = alertDate, active = false),
              )
            },
            booking(
              bookingSequence = 3,
            ).apply {
              this.alerts += listOf(
                alert(booking = this, sequence = 30, alertCode = "B", alertDate = alertDate.plusDays(1), active = true),
              )
            },
          ),
        )
      }

      @Test
      fun `will choose more recent booking over the more recent alerts`() {
        val alerts = alertsService.getAlerts("A1234KT")

        assertThat(alerts.latestBookingAlerts).extracting<String> { it.alertCode.code }.containsOnly("A")
        assertThat(alerts.previousBookingsAlerts).extracting<String> { it.alertCode.code }.containsOnly("B")
        assertThat(alerts.previousBookingsAlerts).extracting<Long> { it.alertSequence }.containsOnly(20)
      }
    }

    @Nested
    @DisplayName("With the same extra alerts on previous booking that have the same alert dates, same status but different audit dates")
    inner class WithPreviousBookingManyAlertDatesSameDifferentAuditDates {
      private val alertDate = LocalDate.parse("2021-01-01")

      @BeforeEach
      fun setUp() {
        whenever(offenderBookingRepository.findAllByOffenderNomsId("A1234KT")).thenReturn(
          listOf(
            booking(
              bookingSequence = 1,
            ).apply {
              this.alerts += listOf(
                alert(booking = this, sequence = 1, alertCode = "A"),
              )
            },
            booking(
              bookingSequence = 2,
            ).apply {
              this.alerts += listOf(
                alert(booking = this, sequence = 20, alertCode = "B", alertDate = alertDate, active = true, auditTimestamp = LocalDateTime.now().minusDays(10)),
                alert(booking = this, sequence = 30, alertCode = "B", alertDate = alertDate, active = true, auditTimestamp = LocalDateTime.now().minusDays(1)),
                alert(booking = this, sequence = 40, alertCode = "B", alertDate = alertDate, active = true, auditTimestamp = LocalDateTime.now().minusDays(20)),
              )
            },
          ),
        )
      }

      @Test
      fun `will choose the active alerts on the same date with the latest audit timestamp`() {
        val alerts = alertsService.getAlerts("A1234KT")

        assertThat(alerts.latestBookingAlerts).extracting<String> { it.alertCode.code }.containsOnly("A")
        assertThat(alerts.previousBookingsAlerts).extracting<String> { it.alertCode.code }.containsOnly("B")
        assertThat(alerts.previousBookingsAlerts).extracting<Long> { it.alertSequence }.containsOnly(30)
      }
    }

    @Nested
    @DisplayName("With the same extra alerts on previous booking that have the same alert dates, same status and same audit dates")
    inner class WithPreviousBookingManyAlertDatesSameAuditDatesSame {
      private val alertDate = LocalDate.parse("2021-01-01")
      private val auditTimestamp = LocalDateTime.parse("2021-01-02T10:00")

      @BeforeEach
      fun setUp() {
        whenever(offenderBookingRepository.findAllByOffenderNomsId("A1234KT")).thenReturn(
          listOf(
            booking(
              bookingSequence = 1,
            ).apply {
              this.alerts += listOf(
                alert(booking = this, sequence = 1, alertCode = "A"),
              )
            },
            booking(
              bookingSequence = 2,
            ),
            booking(
              bookingSequence = 3,
            ).apply {
              this.alerts += listOf(
                alert(booking = this, sequence = 20, alertCode = "B", alertDate = alertDate, active = true, auditTimestamp = auditTimestamp),
                alert(booking = this, sequence = 30, alertCode = "B", alertDate = alertDate, active = true, auditTimestamp = auditTimestamp),
                alert(booking = this, sequence = 40, alertCode = "B", alertDate = alertDate, active = true, auditTimestamp = LocalDateTime.now().minusYears(99)),
              )
            },
          ),
        )
      }

      @Test
      fun `will choose all matching active alerts with the latest date and audit timestamp`() {
        val alerts = alertsService.getAlerts("A1234KT")

        assertThat(alerts.latestBookingAlerts).extracting<String> { it.alertCode.code }.containsOnly("A")
        assertThat(alerts.previousBookingsAlerts).extracting<String> { it.alertCode.code }.containsOnly("B", "B")
        assertThat(alerts.previousBookingsAlerts).extracting<Long> { it.alertSequence }.containsOnly(20, 30)
      }
    }

    @Nested
    @DisplayName("With the same extra alerts on previous booking that have the same alert dates, same status and same audit dates but some have null dates")
    inner class WithPreviousBookingManyAlertDatesSameAuditDatesSameSomeNull {
      private val alertDate = LocalDate.parse("2021-01-01")
      private val auditTimestamp = LocalDateTime.parse("2021-01-02T10:00")

      @BeforeEach
      fun setUp() {
        whenever(offenderBookingRepository.findAllByOffenderNomsId("A1234KT")).thenReturn(
          listOf(
            booking(
              bookingSequence = 1,
            ).apply {
              this.alerts += listOf(
                alert(booking = this, sequence = 1, alertCode = "A"),
              )
            },
            booking(
              bookingSequence = 2,
            ),
            booking(
              bookingSequence = 3,
            ),
            booking(
              bookingSequence = 4,
            ).apply {
              this.alerts += listOf(
                alert(booking = this, sequence = 20, alertCode = "B", alertDate = alertDate, active = true, auditTimestamp = auditTimestamp),
                alert(booking = this, sequence = 30, alertCode = "B", alertDate = alertDate, active = true, auditTimestamp = auditTimestamp),
                alert(booking = this, sequence = 40, alertCode = "B", alertDate = alertDate, active = true, auditTimestamp = null),
              )
            },
          ),
        )
      }

      @Test
      fun `will choose the active with the latest audit timestamp`() {
        val alerts = alertsService.getAlerts("A1234KT")

        assertThat(alerts.latestBookingAlerts).extracting<String> { it.alertCode.code }.containsOnly("A")
        assertThat(alerts.previousBookingsAlerts).extracting<String> { it.alertCode.code }.containsOnly("B", "B")
        assertThat(alerts.previousBookingsAlerts).extracting<Long> { it.alertSequence }.containsOnly(20, 30)
      }
    }
  }
}

private fun booking(bookingSequence: Int = 1): OffenderBooking =
  OffenderBooking(
    bookingSequence = bookingSequence,
    bookingBeginDate = LocalDateTime.now(),
    offender = Offender(nomsId = "A1234KT", gender = Gender("M", "MALE"), lastName = "SMITH"),
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
  alertCode = AlertCode(alertCode, "A Alert"),
  alertType = AlertType("X", "X Type"),
  alertDate = alertDate,
  createUsername = "BOB",
  alertStatus = if (active) ACTIVE else INACTIVE,

).apply {
  this.auditTimestamp = auditTimestamp
  this.createDatetime = LocalDateTime.now()
}
