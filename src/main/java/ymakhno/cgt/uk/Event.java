package ymakhno.cgt.uk;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public record Event(
    int id,
    EventType eventType,
    LocalDate date,
    BigDecimal priceUsd,
    BigDecimal fx,
    BigDecimal amount,
    BigDecimal feeUsd) {

  public enum EventType {
    SALE,
    VEST
  }

  public BigDecimal priceGbp() {
    return priceUsd.divide(fx, 6, RoundingMode.UP);
  }

  public BigDecimal feeGbp() {
    return feeUsd.divide(fx, 6, RoundingMode.UP);
  }
}
