package eu.talmar.api.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.GeneralCodingRules;
import eu.talmar.api.shared.gdpr.PersonalData;
import eu.talmar.api.shared.gdpr.ProcessData;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ArchitectureTest {
  private static final JavaClasses classes =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("eu.talmar.api");

  private static final JavaClasses testClasses =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.ONLY_INCLUDE_TESTS)
          .importPackages("eu.talmar.api");

  @Test
  @DisplayName("Application is free of Cycles")
  void talmar_isFreeOf_Cycles() {
    ArchRule myRule = slices().matching("eu.talmar.api.(*)..").should().beFreeOfCycles();
    myRule.check(classes);
  }

  @Test
  @DisplayName("Domain Classes do not rely on classes in Application or Adapter layer ")
  void domainClasses_doesNotRelyOn_ApplicationOrAdapter() {
    ArchRule myRule =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..application..", "..adapter..");

    myRule.check(classes);
  }

  @Test
  @DisplayName(
      "Domain Classes only depends on java and shared packages, only uuid generation allowed")
  void domainClasses_doNot_haveDependencies_onFrameworks() {
    ArchRule myRule =
        classes()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage(
                "java..",
                "eu.talmar.api.shared..",
                "eu.talmar.api.domain..",
                "com.fasterxml.uuid..");
    myRule.check(classes);
  }

  @Test
  @DisplayName("Application Classes only depend on domain, application and shared packages")
  void applicationClasses_onlyDependOn_DomainApplicationAndShared() {
    ArchRule myRule =
        noClasses()
            .that()
            .resideInAPackage("..application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..adapter..");
    myRule.check(classes);
  }

  // Ban list: typical mistakes of agents trained on older Spring/Java code.

  @Test
  @DisplayName("No class uses Jackson 2 (com.fasterxml.jackson outside the shared annotations)")
  void noClass_uses_jackson2() {
    ArchRule myRule =
        noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "com.fasterxml.jackson.core..",
                "com.fasterxml.jackson.databind..",
                "com.fasterxml.jackson.datatype..",
                "com.fasterxml.jackson.dataformat..",
                "com.fasterxml.jackson.module..")
            .because(
                "Spring Boot 4 uses Jackson 3 (tools.jackson.*); only the annotations in"
                    + " com.fasterxml.jackson.annotation are shared between both versions");
    myRule.check(classes);
  }

  @Test
  @DisplayName("No class uses the Java EE javax namespaces that moved to jakarta")
  void noClass_uses_javaEeJavax() {
    ArchRule myRule =
        noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "javax.annotation",
                "javax.inject..",
                "javax.persistence..",
                "javax.servlet..",
                "javax.transaction..",
                "javax.validation..",
                "javax.ws.rs..",
                "javax.xml.bind..")
            .because("Spring Boot 3+ uses Jakarta EE (jakarta.*)");
    myRule.check(classes);
  }

  @Test
  @DisplayName("No class uses the legacy date and time API")
  void noClass_uses_legacyDateTimeApi() {
    ArchRule myRule =
        noClasses()
            .should()
            .dependOnClassesThat()
            .belongToAnyOf(
                Date.class,
                Calendar.class,
                SimpleDateFormat.class,
                java.sql.Date.class,
                java.sql.Time.class,
                java.sql.Timestamp.class)
            .because("use java.time instead");
    myRule.check(classes);
  }

  @Test
  @DisplayName("No class writes to System.out or System.err")
  void noClass_accesses_standardStreams() {
    GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS
        .because("use the logger (@Log4j2)")
        .check(classes);
  }

  @Test
  @DisplayName("No class uses field injection")
  void noClass_uses_fieldInjection() {
    GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION
        .because(
            "use constructor injection (@RequiredArgsConstructor); MapStruct mappers with"
                + " 'uses' need injectionStrategy = InjectionStrategy.CONSTRUCTOR")
        .check(classes);
  }

  @Test
  @DisplayName("@Transactional is only used on application services")
  void transactional_isOnlyUsedOn_applicationServices() {
    String reason = "transaction boundaries belong to the use case, not to adapters or the domain";
    ArchRule onClasses =
        noClasses()
            .that()
            .resideOutsideOfPackage("eu.talmar.api.application.service..")
            .should()
            .beMetaAnnotatedWith("org.springframework.transaction.annotation.Transactional")
            .orShould()
            .beMetaAnnotatedWith("jakarta.transaction.Transactional")
            .because(reason);
    ArchRule onMethods =
        noMethods()
            .that()
            .areDeclaredInClassesThat()
            .resideOutsideOfPackage("eu.talmar.api.application.service..")
            .should()
            .beMetaAnnotatedWith("org.springframework.transaction.annotation.Transactional")
            .orShould()
            .beMetaAnnotatedWith("jakarta.transaction.Transactional")
            .because(reason);
    onClasses.check(classes);
    onMethods.check(classes);
  }

  @Test
  @DisplayName("No class asks java.time for now() without a Clock")
  void noClass_calls_nowWithoutClock() {
    ArchRule myRule =
        noClasses()
            .should()
            .callMethodWhere(NOW_WITHOUT_CLOCK)
            .because("inject the Clock bean and call now(clock), so tests can pin the time");
    myRule.check(classes);
    myRule.check(testClasses);
  }

  @Test
  @DisplayName("No test calls Thread.sleep")
  void noTest_calls_sleep() {
    ArchRule myRule =
        noClasses()
            .should()
            .callMethodWhere(SLEEP)
            .because(
                "sleeping makes tests slow and flaky; use a fixed Clock or wait on a condition");
    myRule.check(testClasses);
  }

  // GDPR: personal data must be wipeable by anonymization without touching process data.

  @Test
  @DisplayName("Every persistence-entity field is classified as @PersonalData or @ProcessData")
  void persistenceEntityFields_areClassifiedAs_personalOrProcessData() {
    ArchRule myRule =
        fields()
            .that()
            .areDeclaredInClassesThat(PERSISTENCE_ENTITY)
            .and()
            .areNotStatic()
            .and()
            .doNotHaveModifier(JavaModifier.SYNTHETIC)
            .and(NOT_A_KEY_OR_ASSOCIATION)
            .should(BE_ANNOTATED_WITH_EXACTLY_ONE_DATA_CATEGORY)
            .because(
                "anonymization wipes personal data and keeps process data; decide the category"
                    + " explicitly for every new column");
    myRule.check(classes);
  }

  private static final List<String> TIME_TYPES =
      List.of(
              Instant.class,
              LocalDate.class,
              LocalDateTime.class,
              LocalTime.class,
              MonthDay.class,
              OffsetDateTime.class,
              OffsetTime.class,
              Year.class,
              YearMonth.class,
              ZonedDateTime.class)
          .stream()
          .map(Class::getName)
          .toList();

  private static final DescribedPredicate<JavaMethodCall> NOW_WITHOUT_CLOCK =
      DescribedPredicate.describe(
          "now() on a java.time type without a Clock argument",
          call -> {
            List<JavaClass> parameters = call.getTarget().getRawParameterTypes();
            boolean takesClock =
                parameters.size() == 1 && parameters.getFirst().isEquivalentTo(Clock.class);
            return call.getName().equals("now")
                && TIME_TYPES.contains(call.getTargetOwner().getName())
                && !takesClock;
          });

  private static final DescribedPredicate<JavaMethodCall> SLEEP =
      DescribedPredicate.describe(
          "Thread.sleep or TimeUnit.sleep",
          call ->
              call.getName().equals("sleep")
                  && (call.getTargetOwner().isEquivalentTo(Thread.class)
                      || call.getTargetOwner().isEquivalentTo(TimeUnit.class)));

  private static final DescribedPredicate<JavaClass> PERSISTENCE_ENTITY =
      DescribedPredicate.describe(
          "are JPA entities, mapped superclasses or embeddables",
          javaClass ->
              javaClass.isAnnotatedWith(Entity.class)
                  || javaClass.isAnnotatedWith(MappedSuperclass.class)
                  || javaClass.isAnnotatedWith(Embeddable.class));

  /**
   * Ids, versions and associations are keys and links, not data: the columns they point to are
   * classified in their own entity.
   */
  private static final DescribedPredicate<JavaField> NOT_A_KEY_OR_ASSOCIATION =
      DescribedPredicate.describe(
          "are not an @Id, @Version, @Transient or association",
          field ->
              List.of(
                      Id.class,
                      Version.class,
                      Transient.class,
                      OneToOne.class,
                      OneToMany.class,
                      ManyToOne.class,
                      ManyToMany.class)
                  .stream()
                  .noneMatch(field::isAnnotatedWith));

  private static final ArchCondition<JavaField> BE_ANNOTATED_WITH_EXACTLY_ONE_DATA_CATEGORY =
      new ArchCondition<>("be annotated with exactly one of @PersonalData or @ProcessData") {
        @Override
        public void check(JavaField field, ConditionEvents events) {
          boolean personal = field.isAnnotatedWith(PersonalData.class);
          boolean process = field.isAnnotatedWith(ProcessData.class);
          if (personal == process) {
            String problem = personal ? "is annotated with both" : "is annotated with neither";
            events.add(
                SimpleConditionEvent.violated(
                    field,
                    field.getFullName() + " " + problem + " @PersonalData and @ProcessData"));
          }
        }
      };
}
