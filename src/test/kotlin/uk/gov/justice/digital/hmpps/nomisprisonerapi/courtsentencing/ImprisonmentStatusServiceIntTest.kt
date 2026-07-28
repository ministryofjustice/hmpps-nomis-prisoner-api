package uk.gov.justice.digital.hmpps.nomisprisonerapi.courtsentencing

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderChargeDsl.Offences.GENOCIDE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderChargeDsl.Offences.HOUSE_DRAWN_VEHICLE_NOT_STOP
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderChargeDsl.ResultCode.ADJOURNMENT
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderChargeDsl.ResultCode.COMMITTED_CROWN_COURT
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderChargeDsl.ResultCode.DISMISSED
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderChargeDsl.ResultCode.IMPRISONMENT
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtOrder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.StaffRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@ActiveProfiles("test")
@SpringBootTest
class ImprisonmentStatusServiceIntTest {
  @Autowired
  private lateinit var imprisonmentStatusService: ImprisonmentStatusService

  @Autowired
  lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  lateinit var offenderRepository: OffenderRepository

  @Autowired
  lateinit var staffRepository: StaffRepository

  lateinit var prisoner: Offender
  lateinit var staff: Staff

  @AfterEach
  fun afterEach() {
    if (::prisoner.isInitialized) {
      offenderRepository.deleteById(prisoner.id)
    }
    if (::staff.isInitialized) {
      staffRepository.deleteById(staff.id)
    }
  }

  @Nested
  inner class ChargeHasNoOutcome {

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        staff = staff {
          account {}
        }
        prisoner = offender(nomsId = "A1234AB") {
          booking(agencyLocationId = "MDI") {
            imprisonmentStatus(statusCode = "RECEP_UNS")
            courtCase(
              reportingStaff = staff,
              statusUpdateStaff = staff,
            ) {
              val offenderCharge = offenderCharge(offenceCode = "RT88074", plea = "G", resultCode1 = null)
              courtEvent {
                courtEventCharge(
                  resultCode1 = null,
                  offenderCharge = offenderCharge,
                  plea = "NG",
                )
              }
            }
          }
        }
      }
    }

    @Test
    fun `current imprisonment status will be set to unknown`() {
      imprisonmentStatusService.recalculateImprisonmentStatus(
        offenderNo = prisoner.nomsId,
        ImprisonmentStatusService.Companion.ChangeType.UPDATE_RESULT,
      )

      nomisDataBuilder.runInTransaction {
        val imprisonmentStatuses = offenderRepository.findByNomsId(prisoner.nomsId).single()
          .latestBooking().imprisonmentStatuses.sortedBy { it.id.sequence }
        assertThat(imprisonmentStatuses).hasSize(2)
        with(imprisonmentStatuses[0]) {
          assertThat(status?.description).isEqualTo("Convicted unsentenced (reception)")
          assertThat(latestStatus).isFalse()
          assertThat(expiryDate).isEqualTo(LocalDate.now())
        }
        with(imprisonmentStatuses[1]) {
          assertThat(status?.description).isEqualTo("Disposal Not Known")
          assertThat(status?.code).isEqualTo("UNKNOWN")
          assertThat(statusCode).isEqualTo("UNKNOWN")
          assertThat(latestStatus).isTrue()
          assertThat(expiryDate).isNull()
          assertThat(createDate).isEqualTo(LocalDate.now())
          assertThat(effectiveDate).isEqualTo(LocalDate.now())
          assertThat(effectiveTime).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
          assertThat(prison.id).isEqualTo("MDI")
          assertThat(commentText).isEqualTo("DPS Auto created - Updated offence outcome result.")
        }
      }
    }

    @Test
    fun `imprisonment status will be set only once`() {
      repeat(10) {
        imprisonmentStatusService.recalculateImprisonmentStatus(
          offenderNo = prisoner.nomsId,
          ImprisonmentStatusService.Companion.ChangeType.UPDATE_RESULT,
        )
      }

      nomisDataBuilder.runInTransaction {
        val imprisonmentStatuses = offenderRepository.findByNomsId(prisoner.nomsId).single()
          .latestBooking().imprisonmentStatuses.sortedBy { it.id.sequence }
        assertThat(imprisonmentStatuses).hasSize(2)
      }
    }
  }

  @Nested
  inner class SingleChargeHasOutcomeCommittedCrownCourt {
    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        staff = staff {
          account {}
        }
        prisoner = offender(nomsId = "A1234AB") {
          booking(agencyLocationId = "MDI") {
            imprisonmentStatus(statusCode = "RECEP_UNS")
            courtCase(
              reportingStaff = staff,
              statusUpdateStaff = staff,
            ) {
              val offenderCharge = offenderCharge(offenceCode = "RT88074", plea = "G", resultCode1 = COMMITTED_CROWN_COURT.code)
              courtEvent {
                courtEventCharge(
                  resultCode1 = offenderCharge.resultCode1?.code,
                  offenderCharge = offenderCharge,
                  plea = "NG",
                )
              }
            }
          }
        }
      }
    }

    @Test
    fun `current imprisonment status will be set to committed to crown court for trial`() {
      imprisonmentStatusService.recalculateImprisonmentStatus(
        offenderNo = prisoner.nomsId,
        ImprisonmentStatusService.Companion.ChangeType.UPDATE_RESULT,
      )

      nomisDataBuilder.runInTransaction {
        val imprisonmentStatuses = offenderRepository.findByNomsId(prisoner.nomsId).single()
          .latestBooking().imprisonmentStatuses.sortedBy { it.id.sequence }
        assertThat(imprisonmentStatuses).hasSize(2)
        assertThat(imprisonmentStatuses.filter { it.latestStatus }).hasSize(1)

        with(imprisonmentStatuses[1]) {
          assertThat(status?.description).isEqualTo("Committed to Crown Court for Trial")
          assertThat(latestStatus).isTrue()
          assertThat(expiryDate).isNull()
        }
      }
    }
  }

  @Nested
  inner class MultipleChargesAllHaveOutcomeCommittedCrownCourt {
    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        staff = staff {
          account {}
        }
        prisoner = offender(nomsId = "A1234AB") {
          booking(agencyLocationId = "MDI") {
            imprisonmentStatus(statusCode = "RECEP_UNS")
            courtCase(
              reportingStaff = staff,
              statusUpdateStaff = staff,
            ) {
              repeat(4) {
                val offenderCharge =
                  offenderCharge(offenceCode = "RT88074", plea = "G", resultCode1 = COMMITTED_CROWN_COURT.code)
                courtEvent {
                  courtEventCharge(
                    resultCode1 = offenderCharge.resultCode1?.code,
                    offenderCharge = offenderCharge,
                    plea = "NG",
                  )
                }
              }
            }
          }
        }
      }
    }

    @Test
    fun `current imprisonment status will be set to committed to crown court for trial`() {
      imprisonmentStatusService.recalculateImprisonmentStatus(
        offenderNo = prisoner.nomsId,
        ImprisonmentStatusService.Companion.ChangeType.UPDATE_RESULT,
      )

      nomisDataBuilder.runInTransaction {
        val imprisonmentStatuses = offenderRepository.findByNomsId(prisoner.nomsId).single()
          .latestBooking().imprisonmentStatuses.sortedBy { it.id.sequence }
        assertThat(imprisonmentStatuses).hasSize(2)
        assertThat(imprisonmentStatuses.filter { it.latestStatus }).hasSize(1)

        with(imprisonmentStatuses[1]) {
          assertThat(status?.description).isEqualTo("Committed to Crown Court for Trial")
          assertThat(latestStatus).isTrue()
          assertThat(expiryDate).isNull()
        }
      }
    }
  }

  @Nested
  inner class MultipleChargesAllHaveDifferentRanking {
    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        staff = staff {
          account {}
        }
        prisoner = offender(nomsId = "A1234AB") {
          booking(agencyLocationId = "MDI") {
            imprisonmentStatus(statusCode = "RECEP_UNS")
            courtCase(
              reportingStaff = staff,
              statusUpdateStaff = staff,
            ) {
              val adjournmentCharge =
                offenderCharge(offenceCode = "RT88074", plea = "G", resultCode1 = ADJOURNMENT.code)
              courtEvent {
                courtEventCharge(
                  resultCode1 = adjournmentCharge.resultCode1?.code,
                  offenderCharge = adjournmentCharge,
                  plea = "NG",
                )
              }
              val committedCrownCourtCharge =
                offenderCharge(offenceCode = "RT88074", plea = "G", resultCode1 = COMMITTED_CROWN_COURT.code)
              courtEvent {
                courtEventCharge(
                  resultCode1 = committedCrownCourtCharge.resultCode1?.code,
                  offenderCharge = committedCrownCourtCharge,
                  plea = "NG",
                )
              }
              val dismissedCharge =
                offenderCharge(offenceCode = "RT88074", plea = "G", resultCode1 = DISMISSED.code)
              courtEvent {
                courtEventCharge(
                  resultCode1 = dismissedCharge.resultCode1?.code,
                  offenderCharge = dismissedCharge,
                  plea = "NG",
                )
              }
            }
          }
        }
      }
    }

    @Test
    fun `current imprisonment status will be set to committed to crown court for trial`() {
      imprisonmentStatusService.recalculateImprisonmentStatus(
        offenderNo = prisoner.nomsId,
        ImprisonmentStatusService.Companion.ChangeType.UPDATE_RESULT,
      )

      nomisDataBuilder.runInTransaction {
        val imprisonmentStatuses = offenderRepository.findByNomsId(prisoner.nomsId).single()
          .latestBooking().imprisonmentStatuses.sortedBy { it.id.sequence }
        assertThat(imprisonmentStatuses).hasSize(2)
        assertThat(imprisonmentStatuses.filter { it.latestStatus }).hasSize(1)

        with(imprisonmentStatuses[1]) {
          assertThat(status?.description).isEqualTo("Committed to Crown Court for Trial")
          assertThat(latestStatus).isTrue()
          assertThat(expiryDate).isNull()
        }
      }
    }
  }

  @Nested
  inner class MultipleChargesAllHaveDifferentRankingButAlreadySet {
    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        staff = staff {
          account {}
        }
        prisoner = offender(nomsId = "A1234AB") {
          booking(agencyLocationId = "MDI") {
            imprisonmentStatus(statusCode = "RECEP_UNS", latestStatus = false, expiryDate = LocalDate.now().minusDays(10))
            imprisonmentStatus(statusCode = "TRL", latestStatus = true, createDate = LocalDate.now().minusDays(9))
            courtCase(
              reportingStaff = staff,
              statusUpdateStaff = staff,
            ) {
              val adjournmentCharge =
                offenderCharge(offenceCode = "RT88074", plea = "G", resultCode1 = ADJOURNMENT.code)
              courtEvent {
                courtEventCharge(
                  resultCode1 = adjournmentCharge.resultCode1?.code,
                  offenderCharge = adjournmentCharge,
                  plea = "NG",
                )
              }
              val committedCrownCourtCharge =
                offenderCharge(offenceCode = "RT88074", plea = "G", resultCode1 = COMMITTED_CROWN_COURT.code)
              courtEvent {
                courtEventCharge(
                  resultCode1 = committedCrownCourtCharge.resultCode1?.code,
                  offenderCharge = committedCrownCourtCharge,
                  plea = "NG",
                )
              }
              val dismissedCharge =
                offenderCharge(offenceCode = "RT88074", plea = "G", resultCode1 = DISMISSED.code)
              courtEvent {
                courtEventCharge(
                  resultCode1 = dismissedCharge.resultCode1?.code,
                  offenderCharge = dismissedCharge,
                  plea = "NG",
                )
              }
            }
          }
        }
      }
    }

    @Test
    fun `current imprisonment status will still be set to committed to crown court for trial`() {
      imprisonmentStatusService.recalculateImprisonmentStatus(
        offenderNo = prisoner.nomsId,
        ImprisonmentStatusService.Companion.ChangeType.UPDATE_RESULT,
      )

      nomisDataBuilder.runInTransaction {
        val imprisonmentStatuses = offenderRepository.findByNomsId(prisoner.nomsId).single()
          .latestBooking().imprisonmentStatuses.sortedBy { it.id.sequence }
        assertThat(imprisonmentStatuses).hasSize(2)

        with(imprisonmentStatuses[0]) {
          assertThat(status?.description).isEqualTo("Convicted unsentenced (reception)")
          assertThat(latestStatus).isFalse()
          assertThat(expiryDate).isEqualTo(LocalDate.now().minusDays(10))
        }

        with(imprisonmentStatuses[1]) {
          assertThat(status?.description).isEqualTo("Committed to Crown Court for Trial")
          assertThat(latestStatus).isTrue()
          assertThat(expiryDate).isNull()
          assertThat(createDate).isEqualTo(LocalDate.now().minusDays(9))
        }
      }
    }
  }

  @Nested
  inner class WithInactiveSentence {
    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        staff = staff {
          account {}
        }
        prisoner = offender(nomsId = "A1234AB") {
          booking(agencyLocationId = "MDI") {
            imprisonmentStatus(statusCode = "RECEP_UNS", latestStatus = false, expiryDate = LocalDate.now().minusDays(10))
            imprisonmentStatus(statusCode = "UNK_SENT", latestStatus = true, createDate = LocalDate.now().minusDays(9))
            courtCase(
              reportingStaff = staff,
              statusUpdateStaff = staff,
            ) {
              lateinit var courtOrder: CourtOrder

              val offenderCharge =
                offenderCharge(offenceCode = "RT88074", plea = "G", resultCode1 = IMPRISONMENT.code)
              courtEvent {
                courtEventCharge(
                  resultCode1 = offenderCharge.resultCode1?.code,
                  offenderCharge = offenderCharge,
                  plea = "NG",
                )
                courtOrder = courtOrder {
                  sentencePurpose(purposeCode = "PUNISH")
                }
              }
              sentence(
                courtOrder = courtOrder,
                calculationType = "ADIMP",
                category = "2020",
                lineSequence = 1,
                status = "I",
              ) {
                offenderSentenceCharge(offenderCharge)
                term(startDate = LocalDate.parse("2022-01-01"), years = 2)
              }
            }
          }
        }
      }
    }

    @Test
    fun `current imprisonment status will still be set to unknown sentence`() {
      imprisonmentStatusService.recalculateImprisonmentStatus(
        offenderNo = prisoner.nomsId,
        ImprisonmentStatusService.Companion.ChangeType.UPDATE_RESULT,
      )

      nomisDataBuilder.runInTransaction {
        val imprisonmentStatuses = offenderRepository.findByNomsId(prisoner.nomsId).single()
          .latestBooking().imprisonmentStatuses.sortedBy { it.id.sequence }
        assertThat(imprisonmentStatuses).hasSize(2)

        with(imprisonmentStatuses[0]) {
          assertThat(status?.description).isEqualTo("Convicted unsentenced (reception)")
          assertThat(latestStatus).isFalse()
          assertThat(expiryDate).isEqualTo(LocalDate.now().minusDays(10))
        }

        with(imprisonmentStatuses[1]) {
          assertThat(status?.description).isEqualTo("Unknown Sentenced")
          assertThat(status?.code).isEqualTo("UNK_SENT")
          assertThat(latestStatus).isTrue()
          assertThat(expiryDate).isNull()
          assertThat(createDate).isEqualTo(LocalDate.now().minusDays(9))
        }
      }
    }
  }

  @Nested
  inner class WhenSentencedAfterConviction {
    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        staff = staff {
          account {}
        }
        prisoner = offender(nomsId = "A1234AB") {
          booking(agencyLocationId = "MDI") {
            imprisonmentStatus(statusCode = "RECEP_UNS", latestStatus = false, expiryDate = LocalDate.now().minusDays(10))
            imprisonmentStatus(statusCode = "UNK_SENT", latestStatus = true, createDate = LocalDate.now().minusDays(9))
            courtCase(
              reportingStaff = staff,
              statusUpdateStaff = staff,
            ) {
              lateinit var courtOrder: CourtOrder

              val offenderCharge =
                offenderCharge(offenceCode = "RT88074", plea = "G", resultCode1 = IMPRISONMENT.code)
              courtEvent {
                courtEventCharge(
                  resultCode1 = offenderCharge.resultCode1?.code,
                  offenderCharge = offenderCharge,
                  plea = "NG",
                )
                courtOrder = courtOrder {
                  sentencePurpose(purposeCode = "PUNISH")
                }
              }
              sentence(
                courtOrder = courtOrder,
                calculationType = "ADIMP",
                category = "2020",
                lineSequence = 1,
                status = "A",
              ) {
                offenderSentenceCharge(offenderCharge)
                term(startDate = LocalDate.parse("2022-01-01"), years = 2)
              }
            }
          }
        }
      }
    }

    @Test
    fun `current imprisonment status will still be set to adult imprisonment`() {
      imprisonmentStatusService.recalculateImprisonmentStatus(
        offenderNo = prisoner.nomsId,
        ImprisonmentStatusService.Companion.ChangeType.UPDATE_RESULT,
      )

      nomisDataBuilder.runInTransaction {
        val imprisonmentStatuses = offenderRepository.findByNomsId(prisoner.nomsId).single()
          .latestBooking().imprisonmentStatuses.sortedBy { it.id.sequence }
        assertThat(imprisonmentStatuses).hasSize(3)

        with(imprisonmentStatuses[0]) {
          assertThat(status?.description).isEqualTo("Convicted unsentenced (reception)")
          assertThat(latestStatus).isFalse()
          assertThat(expiryDate).isEqualTo(LocalDate.now().minusDays(10))
        }

        with(imprisonmentStatuses[1]) {
          assertThat(status?.description).isEqualTo("Unknown Sentenced")
          assertThat(latestStatus).isFalse()
          assertThat(expiryDate).isEqualTo(LocalDate.now())
        }

        with(imprisonmentStatuses[2]) {
          assertThat(status?.description).isEqualTo("Adult Imprisonment Without Option CJA03")
          assertThat(status?.code).isEqualTo("SENT03")
          assertThat(latestStatus).isTrue()
          assertThat(expiryDate).isNull()
          assertThat(createDate).isEqualTo(LocalDate.now())
        }
      }
    }
  }

  @Nested
  inner class WithMultipleSentences {
    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        staff = staff {
          account {}
        }
        prisoner = offender(nomsId = "A1234AB") {
          booking(agencyLocationId = "MDI") {
            imprisonmentStatus(statusCode = "RECEP_UNS", latestStatus = false, expiryDate = LocalDate.now().minusDays(10))
            imprisonmentStatus(statusCode = "UNK_SENT", latestStatus = true, createDate = LocalDate.now().minusDays(9))
            courtCase(
              reportingStaff = staff,
              statusUpdateStaff = staff,
            ) {
              lateinit var courtOrder: CourtOrder

              val horseCharge =
                offenderCharge(offenceCode = HOUSE_DRAWN_VEHICLE_NOT_STOP.code, plea = "G", resultCode1 = IMPRISONMENT.code)
              val genocideCharge =
                offenderCharge(offenceCode = GENOCIDE.code, plea = "G", resultCode1 = IMPRISONMENT.code)
              courtEvent {
                courtEventCharge(
                  resultCode1 = horseCharge.resultCode1?.code,
                  offenderCharge = horseCharge,
                  plea = "NG",
                )
                courtEventCharge(
                  resultCode1 = horseCharge.resultCode1?.code,
                  offenderCharge = horseCharge,
                  plea = "NG",
                )
                courtEventCharge(
                  resultCode1 = genocideCharge.resultCode1?.code,
                  offenderCharge = genocideCharge,
                  plea = "NG",
                )
                courtOrder = courtOrder {
                  sentencePurpose(purposeCode = "PUNISH")
                }
              }
              sentence(
                courtOrder = courtOrder,
                calculationType = "ADIMP",
                category = "2020",
                lineSequence = 1,
                status = "A",
              ) {
                offenderSentenceCharge(horseCharge)
                term(startDate = LocalDate.parse("2022-01-01"), years = 2)
              }
              sentence(
                courtOrder = courtOrder,
                calculationType = "ADIMP",
                category = "2020",
                lineSequence = 1,
                status = "A",
              ) {
                offenderSentenceCharge(genocideCharge)
                term(startDate = LocalDate.now(), years = 2)
              }
              sentence(
                courtOrder = courtOrder,
                calculationType = "ALP",
                category = "2020",
                lineSequence = 2,
                status = "A",
              ) {
                offenderSentenceCharge(horseCharge)
                term(startDate = LocalDate.now(), years = 99)
              }
            }
          }
        }
      }
    }

    @Test
    fun `current imprisonment status will still be set using must serve sentence`() {
      imprisonmentStatusService.recalculateImprisonmentStatus(
        offenderNo = prisoner.nomsId,
        ImprisonmentStatusService.Companion.ChangeType.UPDATE_RESULT,
      )

      nomisDataBuilder.runInTransaction {
        val imprisonmentStatuses = offenderRepository.findByNomsId(prisoner.nomsId).single()
          .latestBooking().imprisonmentStatuses.sortedBy { it.id.sequence }
        assertThat(imprisonmentStatuses).hasSize(3)

        with(imprisonmentStatuses[0]) {
          assertThat(status?.description).isEqualTo("Convicted unsentenced (reception)")
          assertThat(latestStatus).isFalse()
          assertThat(expiryDate).isEqualTo(LocalDate.now().minusDays(10))
        }

        with(imprisonmentStatuses[1]) {
          assertThat(status?.description).isEqualTo("Unknown Sentenced")
          assertThat(latestStatus).isFalse()
          assertThat(expiryDate).isEqualTo(LocalDate.now())
        }

        with(imprisonmentStatuses[2]) {
          assertThat(status?.description).isEqualTo("Automatic")
          assertThat(status?.code).isEqualTo("ALP")
          assertThat(latestStatus).isTrue()
          assertThat(expiryDate).isNull()
          assertThat(createDate).isEqualTo(LocalDate.now())
        }
      }
    }
  }
}
