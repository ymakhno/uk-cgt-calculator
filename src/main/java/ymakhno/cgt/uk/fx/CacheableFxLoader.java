package ymakhno.cgt.uk.fx;


import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class CacheableFxLoader implements FxRatesLoader {
  private final Path cachePath;
  private final FxRatesLoader loader;

  public CacheableFxLoader(Path cachePath, FxRatesLoader loader) {
    this.cachePath = cachePath;
    this.loader = loader;
  }

  @Override
  public Map<LocalDate, BigDecimal> loadFxRates() throws IOException {
    if (Files.exists(cachePath)) {
      return loadFromCache();
    }
    return writeToCache(loader.loadFxRates());
  }

  private Map<LocalDate, BigDecimal> writeToCache(Map<LocalDate, BigDecimal> fxRates)
      throws IOException {
    var csvLines =
        fxRates.keySet().stream()
            .sorted()
            .map(date -> date.toString() + "," + fxRates.get(date))
            .collect(Collectors.joining("\n"));
    Files.writeString(cachePath, csvLines);
    return fxRates;
  }

  private Map<LocalDate, BigDecimal> loadFromCache() throws IOException {
    Map<LocalDate, BigDecimal> rates = new HashMap<>();
    for (var line : Files.readAllLines(cachePath)) {
      String[] parts = line.split(",");
      rates.put(
          LocalDate.parse(parts[0], DateTimeFormatter.ofPattern("yyyy-MM-dd")),
          BigDecimal.valueOf(Double.parseDouble(parts[1])));
    }
    return rates;
  }
}
