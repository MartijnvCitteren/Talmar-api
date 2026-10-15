package app.jobzy.api.application.port.in;

public interface PurgeUnconfirmedRegistrationsUseCase {

  /**
   * Deletes registrations whose verification link expired unused, and companies left without users.
   *
   * @return the number of users deleted
   */
  int purgeUnconfirmedRegistrations();
}
