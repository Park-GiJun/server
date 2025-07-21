package kr.hhplus.be.server.domain.payment.exception

import kr.hhplus.be.server.domain.common.exception.EntityNotFoundException
import kr.hhplus.be.server.domain.common.exception.BusinessRuleViolationException

class PaymentNotFoundException(paymentId: Long) : EntityNotFoundException("Payment", paymentId.toString())

class PaymentAlreadyProcessedException(reservationId: Long) :
    BusinessRuleViolationException("Payment already processed for reservation $reservationId")

class InvalidPaymentAmountException(amount: Int) :
    BusinessRuleViolationException("Invalid payment amount: $amount. Amount must be positive")