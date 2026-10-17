package eu.talmar.api.domain;

import com.fasterxml.uuid.Generators;
import java.util.UUID;

/** Generates UUIDv7 identifiers (time-ordered, random tail). */
public class UuidV7Generator {

  /** Returns a new UUIDv7 value. */
  public static UUID getUUID() {
    return Generators.timeBasedEpochRandomGenerator().generate();
  }
}
