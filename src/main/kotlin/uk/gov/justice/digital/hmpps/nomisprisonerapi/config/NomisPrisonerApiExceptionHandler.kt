package uk.gov.justice.digital.hmpps.nomisprisonerapi.config

import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.ConflictException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException

@RestControllerAdvice
class NomisPrisonerApiExceptionHandler {
  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ErrorResponse> {
    log.debug("Forbidden (403) returned with message {}", e.message)
    return ResponseEntity
      .status(HttpStatus.FORBIDDEN)
      .body(ErrorResponse(status = (HttpStatus.FORBIDDEN.value())))
  }

  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: Exception): ResponseEntity<ErrorResponse> {
    log.info("Validation exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Validation failure: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(NotFoundException::class)
  fun handleNotFoundException(e: Exception): ResponseEntity<ErrorResponse?>? {
    log.info("Not Found: {}", e.message)
    return ResponseEntity
      .status(NOT_FOUND)
      .body(
        ErrorResponse(
          status = NOT_FOUND,
          userMessage = "Not Found: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(BadDataException::class)
  fun handleBadRequestException(e: Exception): ResponseEntity<ErrorResponse?>? {
    log.info("Bad request: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Bad request: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(HttpMessageNotReadableException::class)
  fun handleHttpMessageNotReadablException(e: Exception): ResponseEntity<ErrorResponse?>? {
    log.info("Bad request: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Bad request: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(MethodArgumentNotValidException::class)
  fun handleValidationErrorException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse?>? {
    log.info("Bad request: {}", e.message)
    val message = if (e.hasFieldErrors()) { "${e.fieldError?.field} ${e.fieldError?.defaultMessage}" } else { e.message }
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Bad request: ${e.message}",
          developerMessage = message,
        ),
      )
  }

  @ExceptionHandler(ConflictException::class)
  fun handleConflictException(e: Exception): ResponseEntity<ErrorResponse?>? {
    log.info("Conflict http error: {}", e.message)
    return ResponseEntity
      .status(CONFLICT)
      .body(
        ErrorResponse(
          status = CONFLICT,
          userMessage = e.message,
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException::class)
  fun handleMethodArgumentTypeMismatchException(e: Exception): ResponseEntity<ErrorResponse> {
    log.info("Validation exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Invalid Argument: ${e.cause?.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(java.lang.Exception::class)
  fun handleException(e: java.lang.Exception): ResponseEntity<ErrorResponse?>? {
    log.error("Unexpected exception", e)
    return ResponseEntity
      .status(INTERNAL_SERVER_ERROR)
      .body(
        ErrorResponse(
          status = INTERNAL_SERVER_ERROR,
          userMessage = "Unexpected error: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

data class ErrorResponse(
  val status: Int,
  val errorCode: Int? = null,
  val userMessage: String? = null,
  val developerMessage: String? = null,
  val moreInfo: String? = null,
) {
  constructor(
    status: HttpStatus,
    errorCode: Int? = null,
    userMessage: String? = null,
    developerMessage: String? = null,
    moreInfo: String? = null,
  ) :
    this(status.value(), errorCode, userMessage, developerMessage, moreInfo)
}
