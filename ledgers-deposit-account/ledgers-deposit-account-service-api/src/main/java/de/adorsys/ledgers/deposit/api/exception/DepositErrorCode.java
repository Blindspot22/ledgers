package de.adorsys.ledgers.deposit.api.exception;

public enum DepositErrorCode {
   DEPOSIT_ACCOUNT_EXISTS,
   INSUFFICIENT_FUNDS,
   DEPOSIT_ACCOUNT_NOT_FOUND,
   PAYMENT_NOT_FOUND,
   PAYMENT_PROCESSING_FAILURE,
   PAYMENT_WITH_ID_EXISTS,
   TRANSACTION_NOT_FOUND,
   DEPOSIT_OPERATION_FAILURE,
   ACCOUNT_BLOCKED_DELETED,
   COULD_NOT_EXECUTE_STATEMENT
}
