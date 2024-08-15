package ymakhno.cgt.uk;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record Sale(
    LocalDate date,
    BigDecimal amount,
    BigDecimal salePriceGbp,
    BigDecimal feeGbp,
    List<WeightedCost> weightedCosts) {
  public record WeightedCost(BigDecimal amount, BigDecimal costPriceGbp) {
    BigDecimal value() {
      return amount.multiply(costPriceGbp);
    }
  }

  BigDecimal acquireValue() {
    return weightedCosts.stream().map(WeightedCost::value).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  BigDecimal saleValue() {
    return amount.multiply(salePriceGbp).subtract(feeGbp);
  }

  BigDecimal capitalTaxGain() {
    return saleValue().subtract(acquireValue());
  }
}
