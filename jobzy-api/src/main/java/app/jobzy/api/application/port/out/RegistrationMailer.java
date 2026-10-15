package app.jobzy.api.application.port.out;

import app.jobzy.api.domain.identity.User;

/**
 * Sends the mails of the registration flow. The implementation decides on wording, language and
 * transport; the use case only decides which mail goes to whom.
 */
public interface RegistrationMailer {

  /**
   * Sends the mail with the confirmation link.
   *
   * @param user the user to send it to
   * @param rawToken the verification token to put in the link; only its hash is stored
   */
  void sendVerificationMail(User user, String rawToken);

  /**
   * Tells someone who registers again with a confirmed address that the account already exists and
   * invites them to log in. Sent instead of an error, so the registration response does not reveal
   * which addresses have an account.
   */
  void sendAccountExistsMail(User user);
}
