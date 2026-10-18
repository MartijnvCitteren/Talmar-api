package eu.talmar.api.adapter.in.rest;

import eu.talmar.api.shared.exception.BaseException;

/**
 * Thrown by a controller for an operation that the API contract declares but that is not built yet.
 * Mapped to a 501 problem response by {@code GlobalExceptionHandler}, so a client gets an explicit
 * "not built yet" instead of a silent empty success.
 */
public class EndpointNotImplementedException extends BaseException {

  /**
   * @param operationId the contract's {@code operationId} of the operation that is not built yet
   */
  public EndpointNotImplementedException(String operationId) {
    super("Operation is not implemented yet: " + operationId);
  }
}
