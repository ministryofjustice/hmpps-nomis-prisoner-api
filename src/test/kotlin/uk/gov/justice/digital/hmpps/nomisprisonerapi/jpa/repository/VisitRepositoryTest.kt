package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderVisitBalanceAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SearchLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Visit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrderAdjustmentReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitVisitor
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser
import java.time.LocalDate
import java.time.LocalDateTime

@WithMockAuthUser
class VisitRepositoryTest : IntegrationTestBase() {
  @Autowired
  lateinit var builderRepository: Repository

  @Autowired
  lateinit var repository: VisitRepository

  @Autowired
  lateinit var visitVisitorRepository: VisitVisitorRepository

  @Autowired
  lateinit var visitTypeRepository: ReferenceCodeRepository<VisitType>

  @Autowired
  lateinit var visitStatusRepository: ReferenceCodeRepository<VisitStatus>

  @Autowired
  lateinit var searchRepository: ReferenceCodeRepository<SearchLevel>

  @Autowired
  lateinit var agencyRepository: AgencyLocationRepository

  @Autowired
  lateinit var agencyInternalRepository: AgencyInternalLocationRepository

  @Autowired
  lateinit var offenderVisitBalanceAdjustmentRepository: OffenderVisitBalanceAdjustmentRepository

  @Autowired
  lateinit var visitOrderAdjustmentReasonRepository: ReferenceCodeRepository<VisitOrderAdjustmentReason>

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Test
  fun saveVisit() {
    lateinit var seedOffenderBooking: OffenderBooking
    lateinit var seedPerson1: Person
    lateinit var seedPerson2: Person

    nomisDataBuilder.build {
      offender {
        seedOffenderBooking = booking {
          visitBalance()
        }
      }
      seedPerson1 = person { }
      seedPerson2 = person { }
    }

    val visit = Visit(
      offenderBooking = seedOffenderBooking,
      commentText = "comment text",
      visitorConcernText = "visitor concerns",
      visitDate = LocalDate.parse("2009-12-21"),
      startDateTime = LocalDateTime.parse("2009-12-21T13:15"),
      endDateTime = LocalDateTime.parse("2009-12-21T14:15"),
      visitType = visitTypeRepository.findById(VisitType.pk("SCON")).orElseThrow(),
      visitStatus = visitStatusRepository.findById(VisitStatus.pk("SCH")).orElseThrow(),
      searchLevel = searchRepository.findById(SearchLevel.pk("FULL")).orElseThrow(),
      location = agencyRepository.findById("LEI").orElseThrow(),
      agencyInternalLocation = agencyInternalRepository.findById(-3L).orElseThrow(),
    )

    visit.visitors.add(
      VisitVisitor(
        offenderBooking = seedOffenderBooking,
        visit = visit,
        person = seedPerson1,
        assistedVisit = true,
        groupLeader = true,
      ),
    )
    visit.visitors.add(
      VisitVisitor(
        offenderBooking = seedOffenderBooking,
        visit = visit,
        person = seedPerson2,
        eventId = visitVisitorRepository.getEventId(),
      ),
    )

    builderRepository.runInTransaction {
      repository.save(visit)

      val persistedVisitList = repository.findByOffenderBooking(seedOffenderBooking)
      assertThat(persistedVisitList).isNotEmpty
      val persistedVisit = persistedVisitList[0]

      assertThat(persistedVisit.visitDate).isEqualTo(LocalDate.parse("2009-12-21"))
      assertThat(persistedVisit.startDateTime).isEqualTo(LocalDateTime.parse("2009-12-21T13:15"))
      assertThat(persistedVisit.endDateTime).isEqualTo(LocalDateTime.parse("2009-12-21T14:15"))
      assertThat(persistedVisit.id).isNotNull
      assertThat(persistedVisit.searchLevel!!.description).isEqualTo("Full Search")
      assertThat(persistedVisit.visitStatus.description).isEqualTo("Scheduled")
      assertThat(persistedVisit.visitorConcernText).isEqualTo("visitor concerns")

      val visitVisitors = persistedVisit.visitors
      assertThat(visitVisitors.size).isEqualTo(2)
      val (_, offenderBooking, parentVisit, person, groupLeader, assistedVisit) = visitVisitors[0]
      assertThat(offenderBooking?.bookingId).isEqualTo(seedOffenderBooking.bookingId)
      assertThat(parentVisit.id).isEqualTo(visit.id)
      assertThat(groupLeader).isTrue
      assertThat(assistedVisit).isTrue
      assertThat(person?.id).isEqualTo(seedPerson1.id)
      assertThat(visitVisitors[1].person?.id).isEqualTo(seedPerson2.id)
      assertThat(visitVisitors[1].eventId).isGreaterThan(0)
    }
  }

  @Test
  fun saveBalanceAdjustment() {
    lateinit var seedOffenderBooking: OffenderBooking
    lateinit var staffUser: Staff

    nomisDataBuilder.build {
      staffUser = staff(firstName = "JANE", lastName = "STAFF") {
        account(username = "JANESTAFF")
      }
      offender {
        seedOffenderBooking = booking {
          visitBalance(remainingPrivilegedVisitOrders = 2, remainingVisitOrders = 25)
        }
      }
    }

    val seedBalance = seedOffenderBooking.visitBalance ?: throw IllegalStateException("No visit balance")
    assertThat(seedBalance.remainingVisitOrders).isEqualTo(25)
    assertThat(seedBalance.remainingPrivilegedVisitOrders).isEqualTo(2)

    offenderVisitBalanceAdjustmentRepository.save(
      OffenderVisitBalanceAdjustment(
        visitBalance = seedOffenderBooking.visitBalance!!,
        adjustDate = LocalDate.of(2020, 12, 21),
        adjustReasonCode = visitOrderAdjustmentReasonRepository.findById(VisitOrderAdjustmentReason.VO_ISSUE)
          .orElseThrow(),
        remainingVisitOrders = -1,
        // from offender_visit_balances
        previousRemainingVisitOrders = seedBalance.remainingVisitOrders,
        commentText = "test comment",
        authorisedStaffId = staffUser.id,
        endorsedStaffId = staffUser.id,
      ),
    )

    val offenderVisitBalanceAdjustment = offenderVisitBalanceAdjustmentRepository.findAll().first()
    assertThat(offenderVisitBalanceAdjustment.id).isEqualTo(1)
    assertThat(offenderVisitBalanceAdjustment.offenderBooking.bookingId).isEqualTo(seedOffenderBooking.bookingId)
    assertThat(offenderVisitBalanceAdjustment.adjustDate).isEqualTo(LocalDate.of(2020, 12, 21))
    assertThat(offenderVisitBalanceAdjustment.adjustReasonCode.code).isEqualTo("VO_ISSUE")
    assertThat(offenderVisitBalanceAdjustment.remainingVisitOrders).isEqualTo(-1)
    assertThat(offenderVisitBalanceAdjustment.previousRemainingVisitOrders).isEqualTo(25)
    assertThat(offenderVisitBalanceAdjustment.commentText).isEqualTo("test comment")
    assertThat(offenderVisitBalanceAdjustment.authorisedStaffId).isEqualTo(staffUser.id)
    assertThat(offenderVisitBalanceAdjustment.endorsedStaffId).isEqualTo(staffUser.id)
    assertThat(offenderVisitBalanceAdjustment.visitBalance.offenderBookingId).isEqualTo(seedOffenderBooking.bookingId)
  }
}
