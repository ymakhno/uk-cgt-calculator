package ymakhno.cgt.uk;


import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import ymakhno.cgt.uk.Event.EventType;

public class BedAndBreakfastVestings {
  private final Map<Integer, BigDecimal> shares = new HashMap<>();

  BigDecimal acquireShare(Event event, BigDecimal maxShare) {
    if (event.eventType() != EventType.VEST) {
      throw new IllegalArgumentException();
    }
    BigDecimal acquiredBefore = shares.getOrDefault(event.id(), BigDecimal.ZERO);
    BigDecimal remaining = event.amount().subtract(acquiredBefore);
    BigDecimal acquiredNow = remaining.min(maxShare);
    shares.put(event.id(), acquiredBefore.add(acquiredNow));
    return acquiredNow;
  }

  BigDecimal calculateRemainingAmount(Event event) {
    if (event.eventType() != EventType.VEST) {
      throw new IllegalArgumentException();
    }
    return event.amount().subtract(shares.getOrDefault(event.id(), BigDecimal.ZERO));
  }
}
