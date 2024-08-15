package ymakhno.cgt.uk.fx;


import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public interface FxRatesLoader {
  Map<LocalDate, BigDecimal> loadFxRates() throws IOException;
}
