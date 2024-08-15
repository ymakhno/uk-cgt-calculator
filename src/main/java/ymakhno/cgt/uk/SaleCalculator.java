package ymakhno.cgt.uk;


import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import ymakhno.cgt.uk.Event.EventType;
import ymakhno.cgt.uk.Sale.WeightedCost;

public class SaleCalculator {
  private final BedAndBreakfastVestings bedAndBreakfastVestings;
  private final HoldingPool holdingPool;

  public SaleCalculator(BedAndBreakfastVestings bedAndBreakfastVestings, HoldingPool holdingPool) {
    this.bedAndBreakfastVestings = bedAndBreakfastVestings;
    this.holdingPool = holdingPool;
  }

  public Sale calculateSale(Event sale, List<Event> allEvents) {
    BigDecimal[] amountLeft = {sale.amount()};
    List<WeightedCost> bedAndBreakfastCost =
        getBedAndBreakfastVestingsForSale(sale, allEvents).stream()
            .flatMap(
                vest ->
                    applyBedAndBreakfast(vest, amountLeft[0]).stream()
                        .peek(cost -> amountLeft[0] = amountLeft[0].subtract(cost.amount())))
            .toList();
    Optional<WeightedCost> poolCost = applyPoolCost(amountLeft[0]);
    return new Sale(
        sale.date(),
        sale.amount(),
        sale.priceGbp(),
        sale.feeGbp(),
        Stream.concat(bedAndBreakfastCost.stream(), poolCost.stream()).toList());
  }

  private Optional<WeightedCost> applyPoolCost(BigDecimal amountLeft) {
    if (amountLeft.stripTrailingZeros().equals(BigDecimal.ZERO)) {
      return Optional.empty();
    }
    var result = Optional.of(new WeightedCost(amountLeft, holdingPool.getCurrentPriceGbp()));
    holdingPool.releaseFromPool(amountLeft);
    return result;
  }

  private Optional<WeightedCost> applyBedAndBreakfast(Event vest, BigDecimal amountLeft) {
    BigDecimal acquired = bedAndBreakfastVestings.acquireShare(vest, amountLeft);
    if (acquired.stripTrailingZeros().equals(BigDecimal.ZERO)) {
      return Optional.empty();
    }
    return Optional.of(new WeightedCost(acquired, vest.priceGbp()));
  }

  private List<Event> getBedAndBreakfastVestingsForSale(Event sale, List<Event> allEvents) {
    return allEvents.stream()
        .filter(e -> e.eventType() == EventType.VEST)
        .filter(e -> e.date().isAfter(sale.date()) || e.date().isEqual(sale.date()))
        .filter(e -> ChronoUnit.DAYS.between(sale.date(), e.date()) <= 30)
        .toList();
  }
}
