package uk.gov.justice.digital.hmpps.nomisprisonerapi.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository

interface TapAddressInsertRepository {
  fun insertAddressIfNotExists(ownerClass: String, ownerId: Long, premise: String, street: String?, postalCode: String?): Int
}

@Profile("oracle")
@Repository
class TapAddressInsertRepositoryOracle(
  @PersistenceContext private val entityManager: EntityManager,
) : TapAddressInsertRepository {

  override fun insertAddressIfNotExists(ownerClass: String, ownerId: Long, premise: String, street: String?, postalCode: String?): Int = entityManager.createNativeQuery(
    """
      MERGE into addresses addr
      using (select ? owner_class, ? owner_id, ? premise, ? street, ? postal_code from dual) s
        on (addr.owner_class=s.owner_class 
          and addr.owner_id=s.owner_id 
          and addr.premise=s.premise 
          and coalesce(addr.street, ' ') = coalesce(s.street, ' ') 
          and coalesce(addr.postal_code, ' ') = coalesce(s.postal_code, ' ')
        )
      when not matched then
        insert (address_id, owner_class, owner_id, premise, street, POSTAL_CODE, primary_flag, mail_flag)
        values (address_id.nextval, s.owner_class, s.owner_id, s.premise, s.street, s.postal_code, 'N', 'N')
      """,
  )
    .setParameter(1, ownerClass)
    .setParameter(2, ownerId)
    .setParameter(3, premise)
    .setParameter(4, street)
    .setParameter(5, postalCode)
    .executeUpdate()
}

@Profile("!oracle")
@Repository
class TapAddressInsertRepositoryH2(
  @PersistenceContext private val entityManager: EntityManager,
) : TapAddressInsertRepository {

  override fun insertAddressIfNotExists(ownerClass: String, ownerId: Long, premise: String, street: String?, postalCode: String?): Int = entityManager.createNativeQuery(
    """
      insert into addresses (address_id, owner_class, owner_id, premise, street, postal_code, primary_flag, mail_flag)
      select address_id.nextval, ?, ?, ?, ?, ?, 'N', 'N'
      where not exists (
        select 1 from addresses where owner_class = ? and owner_id = ? and premise = ? and coalesce(street, ' ') = coalesce(?, ' ') and  coalesce(postal_code, ' ') = coalesce(?, ' ')
      )
      """,
  )
    .setParameter(1, ownerClass)
    .setParameter(2, ownerId)
    .setParameter(3, premise)
    .setParameter(4, street)
    .setParameter(5, postalCode)
    .setParameter(6, ownerClass)
    .setParameter(7, ownerId)
    .setParameter(8, premise)
    .setParameter(9, street)
    .setParameter(10, postalCode)
    .executeUpdate()
}
