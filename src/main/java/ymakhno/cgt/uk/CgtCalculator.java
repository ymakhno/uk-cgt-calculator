package ymakhno.cgt.uk;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CgtCalculator {

  public static Map<LocalDate, BigDecimal> calculateGainPerYear(List<Sale> sales) {
    return sales.stream()
        .collect(
            Collectors.toMap(CgtCalculator::detectTaxYear, Sale::capitalTaxGain, BigDecimal::add));
  }

  private static LocalDate detectTaxYear(Sale sale) {
    LocalDate financialStart = LocalDate.of(2030, Month.APRIL, 6);
    while (sale.date().isBefore(financialStart)) {
      financialStart = financialStart.minusYears(1);
    }
    return financialStart;
  }
}
