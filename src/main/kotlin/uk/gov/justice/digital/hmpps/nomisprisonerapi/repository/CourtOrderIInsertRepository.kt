package uk.gov.justice.digital.hmpps.nomisprisonerapi.repository

import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtOrder

@Repository
interface CourtOrderInsertRepository {
  fun insertOnePerEvent(courtOrder: CourtOrder): Int
}

@Profile("oracle")
@Repository
class CourtOrderInsertRepositoryOracle(val template: JdbcTemplate) : CourtOrderInsertRepository {
  override fun insertOnePerEvent(courtOrder: CourtOrder) = template.update(
    //language=Oracle
    """
      insert into orders (ORDER_ID, OFFENDER_BOOK_ID, CASE_ID, COURT_DATE, ORDER_TYPE, ISSUING_AGY_LOC_ID, ORDER_STATUS,
                          EVENT_ID)
      select ORDER_ID.nextval,
             ?,
             ?,
             ?,
             ?,
             ?,
             ?,
             ?
      from dual
      where not exists (select * from orders where EVENT_ID = ?)
    """.trimIndent(),
    courtOrder.offenderBooking.bookingId,
    courtOrder.courtCase.id,
    courtOrder.courtDate,
    courtOrder.orderType,
    courtOrder.issuingCourt.id,
    courtOrder.orderStatus,
    courtOrder.courtEvent!!.id,
    courtOrder.courtEvent!!.id,
  )
}

@Profile("!oracle")
@Repository
class CourtOrderInsertRepositoryH2(val template: JdbcTemplate) : CourtOrderInsertRepository {
  override fun insertOnePerEvent(courtOrder: CourtOrder) = template.update(
    //language=H2
    """
      insert into orders (ORDER_ID, OFFENDER_BOOK_ID, CASE_ID, COURT_DATE, ORDER_TYPE, ISSUING_AGY_LOC_ID, ORDER_STATUS,
                          EVENT_ID) 
      values(
             NEXT VALUE FOR ORDER_ID,
             ?,
             ?,
             ?,
             ?,
             ?,
             ?,
             ?
      )
    """.trimIndent(),
    courtOrder.offenderBooking.bookingId,
    courtOrder.courtCase.id,
    courtOrder.courtDate,
    courtOrder.orderType,
    courtOrder.issuingCourt.id,
    courtOrder.orderStatus,
    courtOrder.courtEvent!!.id,
  )
}
