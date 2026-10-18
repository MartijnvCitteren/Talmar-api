package eu.talmar.api.application.port.out;

/**
 * The public mail domains (gmail, outlook, proton, ...) whose addresses never create or join a
 * company: such an address says nothing about the company someone works for.
 */
public interface PublicEmailDomains {

  /**
   * Whether a domain is a public mail domain.
   *
   * @param emailDomain the domain part of an e-mail address, in lower case
   */
  boolean contains(String emailDomain);
}
