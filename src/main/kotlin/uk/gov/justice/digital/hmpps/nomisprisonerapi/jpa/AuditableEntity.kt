package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import javax.persistence.EntityListeners
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import java.io.Serializable
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.MappedSuperclass

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class AuditableEntity : Serializable {
  @Column(name = "CREATE_USER_ID")
  @CreatedBy
  protected var createUserId: String? = null

  @Column(name = "CREATE_DATETIME")
  @CreatedDate
  protected var createDatetime: LocalDateTime? = null

  @Column(name = "MODIFY_USER_ID")
  @LastModifiedBy
  protected var modifyUserId: String? = null

  @Column(name = "MODIFY_DATETIME")
  @LastModifiedDate
  protected var modifyDatetime: LocalDateTime? = null
}
