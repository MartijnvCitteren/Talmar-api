---
name: unit-testing
description: Testing conventions for Java/Spring Boot classes in this project — JUnit 5 with Mockito for services, @DataJpaTest for repositories, RestAssured integration tests through the controller only with external systems on WireMock, Gherkin-style test naming with @DisplayName, parameterized tests, no assertions on mocked values, and an 85% coverage target for classes where it's meaningful. Use this whenever writing, reviewing, or reasoning about unit or integration tests.
---

# Testing

## Which tool for which layer

- **Repositories**: use `@DataJpaTest`. This gives you a real (embedded) JPA context
  with transactional rollback per test, without booting the full application context.
  Don't mock the repository layer itself — you're testing the actual query/mapping
  behavior.
- **Services**: use Mockito. Mock every collaborator (`@Mock`), inject them into the
  class under test (`@InjectMocks`), and run under
  `@ExtendWith(MockitoExtension.class)`. Do not boot a Spring context for a service
  test — if you find yourself reaching for `@SpringBootTest` here, that's a smell.

## Only verify real behaviour (maintainer rule)

A check that only repeats what the test itself set up proves nothing and misleads the reader. Remove it.

- **Never assert a value a mock returned.** If the repository mock returns `3` and the service returns it, asserting
  `3` tests Mockito. Verify the interaction instead: `verify(generationRepository).deleteRequestedBefore(cutoff)`.
- Likewise, don't assert that a mocked draft or failure reason ended up on the object under test; `verify(...).save(...)`
  is what the service does. Assert a value only when the class under test computed it (e.g. a timestamp from the
  injected `Clock`, or the failure reason the service chose itself when the generator threw).
- Use the specific assertion: `assertNull(x)`, not `assertEquals(null, x)`.
- A lambda in `assertThrows` contains exactly one call that can throw (SonarQube): prepare ids, drafts and other
  arguments in local variables before it.

## Service tests: Mockito setup

- Use `@InjectMocks`, not a hand-written constructor call in `@BeforeEach`.
- Fixed values are `private static final` constants. A collaborator that is a value, such as a `Clock` or a properties
  record, goes in as `@Spy private Clock clock = CLOCK;` so `@InjectMocks` can use it. Error Prone flags a field that is
  never read: read the spy in the test where the class under test uses it (`LocalDateTime.now(clock)`), or fall back to
  the constructor when the test has no reason to read it.
- Name variables after what they hold (`userPrompt`, `pendingGeneration`), and test doubles after what they replace
  (`MISTRAL_WIREMOCK`).

## Integration tests (`integration/`, RestAssured)

- **Only through the API.** A test sends HTTP requests and asserts on HTTP responses. It never inserts, reads or
  verifies through a repository, a use case or another class halfway the process, and never starts halfway a flow (e.g.
  by saving a domain object directly). State that the API cannot reach in a test (e.g. a timeout after 90 seconds) is
  covered by unit tests instead.
- **Replace only what is outside the application.** External systems (Mistral) run on WireMock (`testSupport/MistralStub`);
  never `@MockitoBean` one of our own ports. What the external system received is an observable effect you may verify.
- **Fixtures as SQL.** Data a test needs to exist beforehand (e.g. a vacancy to generate a description for) is seeded
  with an `@Sql` script under `src/test/resources/sql/`, which first clears the tables it touches. Don't create it
  through another endpoint in every test.
- One test class per endpoint or flow (e.g. starting a generation, polling it), each with happy and unhappy flows.
- `@Async` work runs on `SynchronousAsyncConfig`'s executor, which finishes it before the request returns: no sleeping
  or polling.

## Coverage target: 85%, but only where it's meaningful

Aim for 85% coverage on classes where testing actually proves something:

- **Always relevant**: service classes, validators, mappers with actual logic,
  anything containing a business rule or a branch.
- **Not relevant**: `@Configuration` classes, simple property-holder classes, plain
  records/DTOs with no behavior, the application entry point class.

Don't chase the number on the "not relevant" bucket — that's coverage theater, not
useful testing. If you're unsure whether a class counts, ask: does this class contain
a decision, a transformation, or a rule? If yes, it's in scope.

## Test naming

Use Gherkin-style naming, both in the method name and in `@DisplayName`:

- Method name: camelCase concatenation of the given/when/then phrase, e.g.
  `givenValidVacancyWhenProcessThenSuccessfulSave()`.
- `@DisplayName`: the same phrase, written naturally, e.g.
  `@DisplayName("given valid vacancy, when process then successful save")`.

## No given/when/then comments

Never write `// given`, `// when`, `// then` comments inside the test body. The test
name and `@DisplayName` already carry that narrative — repeating it as inline comments
is exactly the kind of "what does this do" comment that should not exist. Use a blank
line to visually separate the arrange/act/assert sections instead, if you want the
separation to be visible.

## Parameterized tests

Use `@ParameterizedTest` whenever you're asserting the same behavior across a set of
inputs — for example, confirming that every forbidden character is rejected by a
validator. Don't write five near-identical test methods when one parameterized test
covers the same ground; that's exactly the kind of duplication the code reviewer will
flag.

## Assertions and verification

Use plain JUnit 5 assertions and Mockito verification — not AssertJ's fluent style:

- `assertEquals(expected, actual)`
- `assertThrows(SomeException.class, () -> ...)`
- `assertNull(value)`
- `verify(mock).someMethod(any())`

## Example: service test (Mockito)

```java
@ExtendWith(MockitoExtension.class)
class VacancyServiceTest {

    @Mock
    private VacancyRepository vacancyRepository;

    @InjectMocks
    private VacancyService vacancyService;

    @Test
    @DisplayName("given valid vacancy, when process then successful save")
    void givenValidVacancyWhenProcessThenSuccessfulSave() {
        Vacancy vacancy = new Vacancy("Backend Engineer", "Leiden");

        vacancyService.process(vacancy);

        verify(vacancyRepository).save(vacancy);
    }

    @Test
    @DisplayName("given null vacancy, when process then throws")
    void givenNullVacancyWhenProcessThenThrows() {
        assertThrows(IllegalArgumentException.class, () -> vacancyService.process(null));
    }
}
```

## Example: repository test (@DataJpaTest)

```java
@DataJpaTest
class VacancyRepositoryTest {

    @Autowired
    private VacancyRepository vacancyRepository;

    @Test
    @DisplayName("given saved vacancy, when find by title then returns vacancy")
    void givenSavedVacancyWhenFindByTitleThenReturnsVacancy() {
        vacancyRepository.save(new Vacancy("Backend Engineer", "Leiden"));

        Optional<Vacancy> result = vacancyRepository.findByTitle("Backend Engineer");

        assertEquals("Leiden", result.orElseThrow().getLocation());
    }
}
```

## Example: parameterized test

```java
@ParameterizedTest
@ValueSource(strings = {"<", ">", ";", "--", "/*"})
@DisplayName("given forbidden character, when validate then rejected")
void givenForbiddenCharacterWhenValidateThenRejected(String forbiddenChar) {
    boolean result = vacancyValidator.isValid("Backend Engineer" + forbiddenChar);

    assertFalse(result);
}
```