package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.AuditorAwareImpl
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderVisitBalanceAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SearchLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Visit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrderType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitVisitor
import java.time.LocalDate
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AuthenticationFacade::class, AuditorAwareImpl::class)
@WithMockUser
class VisitRepositoryTest {

  @Autowired
  lateinit var repository: VisitRepository

  @Autowired
  lateinit var offenderBookingRepository: OffenderBookingRepository

  @Autowired
  lateinit var visitOrderRepository: VisitOrderRepository

  @Autowired
  lateinit var visitVisitorRepository: VisitVisitorRepository

  @Autowired
  lateinit var visitTypeRepository: ReferenceCodeRepository<VisitType>

  @Autowired
  lateinit var visitStatusRepository: ReferenceCodeRepository<VisitStatus>

  @Autowired
  lateinit var visitOrderTypeRepository: ReferenceCodeRepository<VisitOrderType>

  @Autowired
  lateinit var searchRepository: ReferenceCodeRepository<SearchLevel>

  @Autowired
  lateinit var agencyRepository: AgencyLocationRepository

  @Autowired
  lateinit var agencyInternalRepository: AgencyInternalLocationRepository

  @Autowired
  lateinit var offenderVisitBalanceRepository: OffenderVisitBalanceRepository

  @Autowired
  lateinit var offenderVisitBalanceAdjustmentRepository: OffenderVisitBalanceAdjustmentRepository

  @Autowired
  lateinit var entityManager: TestEntityManager

  @Test
  fun saveVisit() {
    val seedOffenderBooking = offenderBookingRepository.findById(-10L).orElseThrow()

    val visitOrder = visitOrderRepository.save(
      VisitOrder(
        null, 1L, seedOffenderBooking, LocalDate.now(),
        visitOrderTypeRepository.findById(VisitOrderType.SVO).orElseThrow(),
        visitStatusRepository.findById(VisitStatus.NORM).orElseThrow()
      )
    )

    val visit = Visit(
      offenderBooking = seedOffenderBooking,
      commentText = "comment text",
      visitorConcernText = "visitor concerns",
      visitDate = LocalDate.of(2009, 12, 21),
      startTime = LocalDateTime.of(2009, 12, 21, 13, 15),
      endTime = LocalDateTime.of(2009, 12, 21, 14, 15),
      visitType = visitTypeRepository.findById(VisitType.pk("SCON")).orElseThrow(),
      visitStatus = visitStatusRepository.findById(VisitStatus.pk("SCH")).orElseThrow(),
      searchLevel = searchRepository.findById(SearchLevel.pk("FULL")).orElseThrow(),
      location = agencyRepository.findById("LEI").orElseThrow(),
      agencyInternalLocation = agencyInternalRepository.findById(-3L).orElseThrow(),
      visitOrder = visitOrder,
    )

    // visit.agencyVisitSlot = agencyVisitSlotRepository.findById(-1L).orElseThrow()
    repository.save(visit)
    entityManager.flush()

    val persistedVisitList = repository.findByOffenderBooking(seedOffenderBooking)
    assertThat(persistedVisitList).isNotEmpty
    val persistedVisit = persistedVisitList[0]
    visitVisitorRepository.save(
      VisitVisitor(
        offenderBooking = seedOffenderBooking,
        visitId = persistedVisitList[0].id!!,
        personId = -1L,
        assistedVisit = true,
        groupLeader = true
      )
    )
    visitVisitorRepository.save(
      VisitVisitor(
        offenderBooking = seedOffenderBooking,
        visitId = persistedVisitList[0].id!!,
        personId = -2L,
      )
    )

    entityManager.flush()
    entityManager.refresh(persistedVisit)

    assertThat(persistedVisit.visitDate).isEqualTo(LocalDate.of(2009, 12, 21))
    assertThat(persistedVisit.startTime).isEqualTo(LocalDateTime.of(2009, 12, 21, 13, 15))
    assertThat(persistedVisit.endTime).isEqualTo(LocalDateTime.of(2009, 12, 21, 14, 15))
    assertThat(persistedVisit.id).isNotNull
    assertThat(persistedVisit.searchLevel!!.description).isEqualTo("Full Search")
    assertThat(persistedVisit.visitStatus!!.description).isEqualTo("Scheduled")
    assertThat(persistedVisit.visitorConcernText).isEqualTo("visitor concerns")
//    val visitOrder = persistedVisit.visitOrder
//    assertThat(visitOrder).isNotNull
//    assertThat(visitOrder!!.visitOrderType?.description).isEqualTo("Visiting Order")
//    assertThat(visitOrder.issueDate).isEqualTo(LocalDate.of(2001, 1, 1))
//    assertThat(visitOrder.status?.description).isEqualTo("Active")
//    assertThat(visitOrder.commentText).isEqualTo("Some VO Comment Text")
//    assertThat(visitOrder.offenderBooking?.bookingId).isEqualTo(-10)
//    val agencyVisitSlot = persistedVisit.agencyVisitSlot
//    assertThat(agencyVisitSlot!!.maxAdults).isEqualTo(18017)
//    assertThat(agencyVisitSlot.maxGroups).isEqualTo(30)
//    assertThat(agencyVisitSlot.weekDay).isEqualTo("SUN")
//    assertThat(agencyVisitSlot.location?.id).isEqualTo("LEI")
//    assertThat(agencyVisitSlot.agencyInternalLocation?.description).isEqualTo("LEI-A-1-1")
//    val visitOrderVisitors = visitOrder.visitors
//    assertThat(visitOrderVisitors.size).isEqualTo(1)
//    val visitOrderVisitorPerson = visitOrderVisitors[0].person
//    assertThat(visitOrderVisitorPerson).isNotNull
//    assertThat(visitOrderVisitorPerson?.id).isEqualTo(-1)
    val visitVisitors = persistedVisit.visitors
    assertThat(visitVisitors.size).isEqualTo(2)
    val (_, offenderBooking, visitId, personId, groupLeader, assistedVisit) = visitVisitors[0]
    assertThat(offenderBooking?.bookingId).isEqualTo(-10L)
    assertThat(visitId).isEqualTo(visit.id!!)
    assertThat(groupLeader).isTrue
    assertThat(assistedVisit).isTrue
    assertThat(personId).isEqualTo(-1)
    assertThat(visitVisitors[1].personId).isEqualTo(-2)
  }

  @Test
  fun saveBalanceAdjustment() {
    val seedOffenderBooking = offenderBookingRepository.findById(-10L).orElseThrow()
    val seedBalance = offenderVisitBalanceRepository.findById(-10L).orElseThrow()
    assertThat(seedBalance.remainingVisitOrders).isEqualTo(25)
    assertThat(seedBalance.remainingPrivilegedVisitOrders).isEqualTo(2)

    offenderVisitBalanceAdjustmentRepository.save(
      OffenderVisitBalanceAdjustment(
        offenderBooking = seedOffenderBooking,
        adjustDate = LocalDate.of(2020, 12, 21),
        adjustReasonCode = "VO_ISSUE",
        remainingVisitOrders = -1,
        previousRemainingVisitOrders = seedBalance.remainingVisitOrders, // from offender_visit_balances
        commentText = "test comment",
        authorisedStaffId = 123L,
        endorsedStaffId = 123L,
      )
    )
    entityManager.flush()

    val offenderVisitBalanceAdjustment = offenderVisitBalanceAdjustmentRepository.findAll().first()
    assertThat(offenderVisitBalanceAdjustment.id).isEqualTo(1)
    assertThat(offenderVisitBalanceAdjustment.offenderBooking.bookingId).isEqualTo(-10)
    assertThat(offenderVisitBalanceAdjustment.adjustDate).isEqualTo(LocalDate.of(2020, 12, 21))
    assertThat(offenderVisitBalanceAdjustment.adjustReasonCode).isEqualTo("VO_ISSUE")
    assertThat(offenderVisitBalanceAdjustment.remainingVisitOrders).isEqualTo(-1)
    assertThat(offenderVisitBalanceAdjustment.previousRemainingVisitOrders).isEqualTo(25)
    assertThat(offenderVisitBalanceAdjustment.commentText).isEqualTo("test comment")
    assertThat(offenderVisitBalanceAdjustment.authorisedStaffId).isEqualTo(123L)
    assertThat(offenderVisitBalanceAdjustment.endorsedStaffId).isEqualTo(123L)
  }
}
