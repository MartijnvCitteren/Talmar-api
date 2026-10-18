package eu.talmar.api.adapter.out.blocklist;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ByteArrayResource;

class ClasspathPublicEmailDomainsTest {

  private final ClasspathPublicEmailDomains shippedList = new ClasspathPublicEmailDomains();

  @ParameterizedTest
  @ValueSource(
      strings = {
        "gmail.com",
        "googlemail.com",
        "outlook.com",
        "hotmail.com",
        "hotmail.co.uk",
        "live.nl",
        "yahoo.fr",
        "icloud.com",
        "proton.me",
        "protonmail.com",
        "gmx.de",
        "ziggo.nl",
        "kpnmail.nl"
      })
  @DisplayName(
      "given a public mail domain, when checked against the shipped list then it is public")
  void givenPublicMailDomainWhenCheckedAgainstShippedListThenItIsPublic(String domain) {
    assertTrue(shippedList.contains(domain));
  }

  @ParameterizedTest
  @ValueSource(strings = {"acme.nl", "talmar.eu", "sub.gmail.com.acme.nl", "mygmail.com"})
  @DisplayName(
      "given a company domain, when checked against the shipped list then it is not public")
  void givenCompanyDomainWhenCheckedAgainstShippedListThenItIsNotPublic(String domain) {
    assertFalse(shippedList.contains(domain));
  }

  @Test
  @DisplayName(
      "given a list with comments, blank lines and upper case, when read then only the entries"
          + " count")
  void givenListWithCommentsBlankLinesAndUpperCaseWhenReadThenOnlyTheEntriesCount() {
    var publicDomains =
        new ClasspathPublicEmailDomains(
            new ByteArrayResource(
                "# comment\n\n  Example.ORG  # trailing comment\nfree.*\n"
                    .getBytes(StandardCharsets.UTF_8)));

    assertTrue(publicDomains.contains("example.org"));
    assertTrue(publicDomains.contains("free.fr"));
    assertFalse(publicDomains.contains("comment"));
    assertFalse(publicDomains.contains("freedom.fr"));
  }
}
