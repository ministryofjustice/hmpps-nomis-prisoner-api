package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.Generated
import java.time.LocalDateTime

@Entity
@Table(name = "MERGE_TRANSACTIONS")
class MergeTransaction(
  @Id
  @SequenceGenerator(name = "MERGE_TRANSACTIONS_ID", sequenceName = "MERGE_TRANSACTIONS_ID", allocationSize = 1)
  @GeneratedValue(generator = "MERGE_TRANSACTIONS_ID")
  @Column(name = "MERGE_TRANSACTION_ID")
  val id: Long = 0,

  @Column(name = "REQUEST_DATE", nullable = false)
  val requestDate: LocalDateTime,

  @Column(name = "REQUEST_STATUS_CODE", nullable = false)
  val requestStatusCode: String,

  @Column(name = "TRANSACTION_SOURCE", nullable = false)
  val transactionSource: String,

  @Column(name = "OFFENDER_BOOK_ID_1")
  val offenderBookId1: Long,

  @Column(name = "ROOT_OFFENDER_ID_1")
  val rootOffenderId1: Long,

  @Column(name = "OFFENDER_ID_1")
  val offenderId1: Long,

  @Column(name = "OFFENDER_ID_DISPLAY_1")
  val nomsId1: String,

  @Column(name = "LAST_NAME_1")
  val lastName1: String,

  @Column(name = "FIRST_NAME_1")
  val firstName1: String,

  @Column(name = "OFFENDER_BOOK_ID_2")
  val offenderBookId2: Long,

  @Column(name = "ROOT_OFFENDER_ID_2")
  val rootOffenderId2: Long,

  @Column(name = "OFFENDER_ID_2")
  val offenderId2: Long,

  @Column(name = "OFFENDER_ID_DISPLAY_2")
  val nomsId2: String,

  @Column(name = "LAST_NAME_2")
  val lastName2: String,

  @Column(name = "FIRST_NAME_2")
  val firstName2: String,

) {
  @Column(name = "CREATE_USER_ID", insertable = false, updatable = false, nullable = false)
  @Generated
  lateinit var createUsername: String

  @Column(name = "CREATE_DATETIME", insertable = false, updatable = false, nullable = false)
  @Generated
  lateinit var createDatetime: LocalDateTime

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as MergeTransaction
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
