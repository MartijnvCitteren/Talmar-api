package eu.talmar.api.testSupport;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

/**
 * Prints the seed of the random test order (configured in the root {@code pom.xml}), so a failure
 * that depends on test order can be reproduced with {@code -Dtest.order.seed=<seed>}.
 */
public class TestOrderSeedListener implements TestExecutionListener {

  private static final String SEED_PARAMETER = "junit.jupiter.execution.order.random.seed";

  @Override
  public void testPlanExecutionStarted(TestPlan testPlan) {
    testPlan
        .getConfigurationParameters()
        .get(SEED_PARAMETER)
        .ifPresent(
            seed ->
                System.out.printf(
                    "Random test order seed: %s (reproduce with -Dtest.order.seed=%s)%n",
                    seed, seed));
  }
}
