package uk.gov.justice.digital.hmpps.nomisprisonerapi.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository

interface CorporateInsertRepository {
  fun insertCorporateIfNotExists(corporateName: String): Int
}

@Profile("oracle")
@Repository
class CorporateInsertRepositoryOracle(
  @PersistenceContext private val entityManager: EntityManager,
) : CorporateInsertRepository {

  override fun insertCorporateIfNotExists(corporateName: String): Int = entityManager.createNativeQuery(
    """
      MERGE INTO corporates c
      USING (SELECT ? AS corporate_name FROM dual) s
      ON (c.corporate_name = s.corporate_name)
      WHEN NOT MATCHED THEN
        INSERT (corporate_id, CORPORATE_NAME, suspended_flag, created_date)
        VALUES (corporate_id.nextval, s.corporate_name, 'N', sysdate)
      """,
  )
    .setParameter(1, corporateName)
    .executeUpdate()
}

@Profile("!oracle")
@Repository
class CorporateInsertRepositoryH2(
  @PersistenceContext private val entityManager: EntityManager,
) : CorporateInsertRepository {

  override fun insertCorporateIfNotExists(corporateName: String): Int = entityManager.createNativeQuery(
    """
      INSERT INTO corporates (corporate_id, corporate_name, suspended_flag, created_date)
      SELECT corporate_id.NEXTVAL, ?, 'N', sysdate
      WHERE NOT EXISTS (
        SELECT 1 FROM corporates WHERE corporate_name = ?
      )
      """,
  )
    .setParameter(1, corporateName)
    .setParameter(2, corporateName)
    .executeUpdate()
}
