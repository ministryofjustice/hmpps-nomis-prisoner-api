package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.VisitRoomCountResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.filter.VisitFilter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Visit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.specification.VisitSpecification
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Root

interface VisitCustomRepository {
  fun findFutureRoomUsageWithFilter(filter: VisitFilter): List<VisitRoomCountResponse>
}

@Repository
class VisitCustomRepositoryImpl(
  @PersistenceContext
  private val entityManager: EntityManager
) : VisitCustomRepository {

  override fun findFutureRoomUsageWithFilter(filter: VisitFilter): List<VisitRoomCountResponse> {
    val criteriaBuilder: CriteriaBuilder = entityManager.criteriaBuilder
    val criteriaQuery: CriteriaQuery<VisitRoomCountResponse> =
      criteriaBuilder.createQuery(VisitRoomCountResponse::class.java)
    val root: Root<Visit> = criteriaQuery.from(Visit::class.java)

    // using existing predicate code to build filter predicate from Visit specification
    val toPredicate = VisitSpecification(filter).toPredicate(root, criteriaQuery, criteriaBuilder)

    val roomDescriptionExpression =
      root.get<String>(Visit::agencyInternalLocation.name).get<String>(AgencyInternalLocation::description.name)
    val prisonIdExpression =
      root.get<String>(Visit::location.name).get<String>(AgencyLocation::id.name)
    criteriaQuery.multiselect(
      roomDescriptionExpression,
      criteriaBuilder.count(root),
      prisonIdExpression
    ).where(toPredicate).groupBy(roomDescriptionExpression, prisonIdExpression).orderBy(
      criteriaBuilder.asc(
        roomDescriptionExpression
      )
    )
    return entityManager.createQuery(criteriaQuery).resultList
  }
}
