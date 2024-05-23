package uk.gov.justice.digital.hmpps.nomisprisonerapi.config

import jakarta.annotation.PostConstruct
import oracle.jdbc.driver.OracleConnection
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.context.annotation.Lazy
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.stereotype.Component
import java.sql.Connection
import java.sql.SQLException
import java.util.Properties
import javax.sql.DataSource

const val PROXY_USERNAME = "PROXY_USERNAME"

/**
 * Proof of concept of port of prison-api proxy connection.
 * This requires proxy Oracle permission added to our database user
 */
@Aspect
@Component
class DatabaseConnectionConfigurator(@Lazy private val rolePasswordGetter: RolePassword) {
  private val log = LoggerFactory.getLogger(this::class.java)
  private val rolePassword: String by lazy { rolePasswordGetter.get() }

  @Pointcut("execution (* com.zaxxer.hikari.HikariDataSource.getConnection())")
  fun getDataSourceConnection() {
  }

  @Around("getDataSourceConnection()")
  fun onGetDataSourceConnection(getConnection: ProceedingJoinPoint): Any {
    val connection = getConnection.proceed() as Connection
    return connection.takeIf { MDC.get(PROXY_USERNAME) != null }
      ?.let {
        val info = Properties().apply {
          this[OracleConnection.PROXY_USER_NAME] = MDC.get(PROXY_USERNAME)
        }
        val physicalConnection = connection.unwrap(Connection::class.java)
        val oracleConnection: OracleConnection? =
          physicalConnection.takeIf { physicalConnection is OracleConnection } as? OracleConnection
        oracleConnection?.apply {
          kotlin.runCatching {
            log.info("Setting proxy connection for {}", info[OracleConnection.PROXY_USER_NAME])
            openProxySession(OracleConnection.PROXYTYPE_USER_NAME, info)
            prepareStatement("SET ROLE TAG_USER IDENTIFIED BY $rolePassword").use { ps -> ps.execute() }
          }.onSuccess { ProxySessionClosingConnection(connection) }
            .recover {
              log.error("Could not set proxy connection so using normal connection", it)
            }
        }
      } ?: connection
  }
}

@Component
class RolePassword(private val jdbcTemplate: JdbcTemplate, private val dataSource: DataSource) {
  private val log = LoggerFactory.getLogger(this::class.java)
  private val password: String by lazy { this.readPassword() }
  fun get(): String = password

  @PostConstruct
  fun getDatabasePassword() {
    val physicalConnection = dataSource.connection.unwrap(Connection::class.java)
    when (physicalConnection) {
      is OracleConnection -> log.info("Database role password retrieved and it is not blank: ${password.isNotEmpty()}")
      else -> log.info("Not Oracle so no need for database role password")
    }
  }

  private fun readPassword(): String {
    log.info("Reading database role password")
    val encryptedPassword = jdbcTemplate.queryForObject<String>(
      """
        SELECT PROFILE_VALUE ROLE_PWD
        FROM SYSTEM_PROFILES
        WHERE PROFILE_TYPE = 'SYS' AND PROFILE_CODE = 'ROLE_PSWD'
    """,
    )
    return jdbcTemplate.queryForObject(
      """
        SELECT decryption('2DECRYPTPASSWRD', :encryptedPassword) FROM DUAL
    """,
      String::class.java,
      encryptedPassword,
    )
  }
}

class ProxySessionClosingConnection(private val connection: Connection) : Connection by connection {
  companion object {
    val log = LoggerFactory.getLogger(this::class.java)!!
  }

  @Throws(SQLException::class)
  override fun close() {
    log.info("Closing proxy connection")
    (connection.unwrap(Connection::class.java) as OracleConnection)
      .also { oracleConnection ->
        oracleConnection.close(OracleConnection.PROXY_SESSION)
        connection.close()
      }
  }
}

inline fun <T> proxy(username: String?, func: () -> T): T {
  MDC.put(PROXY_USERNAME, username)
  try {
    return func()
  } finally {
    MDC.remove(PROXY_USERNAME)
  }
}
