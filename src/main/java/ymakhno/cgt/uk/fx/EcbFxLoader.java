package ymakhno.cgt.uk.fx;


import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.zip.ZipFile;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public final class EcbFxLoader implements FxRatesLoader {
  private static final BigDecimal MINUS_ONE = BigDecimal.valueOf(-1);
  private static final int MAX_CURRENCIES = 1000;

  private static final String ECB_URI =
      "http://www.ecb.europa.eu/stats/eurofxref/eurofxref-hist.zip";

  @Override
  public Map<LocalDate, BigDecimal> loadFxRates() throws IOException {
    var httpClient = new OkHttpClient();
    var request = new Request.Builder().url(ECB_URI).build();
    try (var response = httpClient.newCall(request).execute()) {
      return parseDownloadedZip(response.body().bytes());
    }
  }

  private Map<LocalDate, BigDecimal> parseDownloadedZip(byte[] zipBytes) throws IOException {
    var tempFile = Files.createTempFile("pref", "suff");
    Files.write(tempFile, zipBytes, StandardOpenOption.APPEND);
    try (ZipFile zipFile = new ZipFile(tempFile.toFile())) {
      var entry = zipFile.stream().findFirst();
      if (entry.isEmpty()) {
        return new HashMap<>();
      }
      return parseFxRatesCsv(zipFile.getInputStream(entry.get()));
    } finally {
      Files.delete(tempFile);
    }
  }

  private Map<LocalDate, BigDecimal> parseFxRatesCsv(InputStream csv) {
    Scanner scanner = new Scanner(csv);
    scanner.useDelimiter(",");
    int usdIdx = -1;
    int gbpIdx = -1;
    for (int i = 0; i < MAX_CURRENCIES && (usdIdx == -1 || gbpIdx == -1); i++) {
      String name = scanner.next().replace("\n", "");
      if (name.equals("USD")) {
        usdIdx = i;
      }
      if (name.equals("GBP")) {
        gbpIdx = i;
      }
    }
    if (usdIdx == -1 || gbpIdx == -1) {
      return new HashMap<>();
    }
    scanner.nextLine();

    Map<LocalDate, BigDecimal> fxRates = new HashMap<>();
    while (scanner.hasNextLine()) {
      LocalDate date = null;
      BigDecimal usdRate = MINUS_ONE;
      BigDecimal gbpRate = MINUS_ONE;
      for (int i = 0; i < Math.max(usdIdx, gbpIdx) + 1; i++) {
        String value = scanner.next();
        if (i == 0) {
          date = LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        if (i == usdIdx) {
          usdRate = parseRate(value);
        }
        if (i == gbpIdx) {
          gbpRate = parseRate(value);
        }
      }
      scanner.nextLine();
      if (date != null && usdRate != null && gbpRate != null) {
        fxRates.put(date, usdRate.divide(gbpRate, 8, RoundingMode.UP));
      }
    }
    return fxRates;
  }

  private static BigDecimal parseRate(String rate) {
    if (rate.trim().equalsIgnoreCase("n/a")) {
      return null;
    }
    return BigDecimal.valueOf(Double.parseDouble(rate));
  }
}
