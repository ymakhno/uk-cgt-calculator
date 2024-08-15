package ymakhno.cgt.uk;


import java.math.BigDecimal;
import java.math.RoundingMode;
import ymakhno.cgt.uk.Event.EventType;

public class HoldingPool {
  private BigDecimal amount = BigDecimal.ZERO;
  private BigDecimal priceGbp = BigDecimal.ZERO;

  private final BedAndBreakfastVestings bedAndBreakfastVestings;

  public HoldingPool(BedAndBreakfastVestings bedAndBreakfastVestings) {
    this.bedAndBreakfastVestings = bedAndBreakfastVestings;
  }

  public void addVestInPool(Event event) {
    if (event.eventType() != EventType.VEST) {
      throw new IllegalArgumentException();
    }
    BigDecimal remainingAfterBB = bedAndBreakfastVestings.calculateRemainingAmount(event);
    BigDecimal newAmount = amount.add(remainingAfterBB);
    priceGbp =
        priceGbp
            .multiply(amount)
            .add(event.priceGbp().multiply(remainingAfterBB))
            .divide(newAmount, 6, RoundingMode.UP);
    amount = newAmount;
  }

  public BigDecimal getCurrentPriceGbp() {
    return priceGbp;
  }

  public void releaseFromPool(BigDecimal value) {
    amount = amount.subtract(value);
  }
}
