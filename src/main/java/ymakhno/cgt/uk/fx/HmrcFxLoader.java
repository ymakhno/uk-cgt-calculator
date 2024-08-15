package ymakhno.cgt.uk.fx;


import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class HmrcFxLoader implements FxRatesLoader {

  private final OkHttpClient client = new OkHttpClient();

  @Override
  public Map<LocalDate, BigDecimal> loadFxRates() {
    var service = Executors.newFixedThreadPool(128);
    List<Future<Map<LocalDate, BigDecimal>>> monthlyRates = new ArrayList<>();
    for (int year = 2020; year < 2025; year++) {
      for (int month = 1; month <= 12; month++) {
        monthlyRates.add(service.submit(createLoadTask(month, year)));
      }
    }
    try {
      service.shutdown();
      service.awaitTermination(10000, TimeUnit.DAYS);
      HashMap<LocalDate, BigDecimal> allRates = new HashMap<>();
      for (var monthlyRate : monthlyRates) {
        allRates.putAll(monthlyRate.get());
      }
      return allRates;
    } catch (Exception e) {
      // ignore
      return new HashMap<>();
    }
  }

  private Callable<Map<LocalDate, BigDecimal>> createLoadTask(int month, int year) {
    return () -> year < 2021 ? requestMonthOld(month, year) : requestMonthNew(month, year);
  }

  private Map<LocalDate, BigDecimal> requestMonthOld(int month, int year) {
    var url =
        String.format(
            "https://www.hmrc.gov.uk/softwaredevelopers/rates/exrates-monthly-%02d%d.XML",
            month, year - 2000);
    var request = new Request.Builder().url(url).get().build();
    try (var response = client.newCall(request).execute()) {
      DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document document = builder.parse(response.body().byteStream());
      NodeList nodeList = document.getDocumentElement().getElementsByTagName("exchangeRate");
      BigDecimal rate = null;
      for (int i = 0; i < nodeList.getLength(); i++) {
        Node node = nodeList.item(i);
        if (node.getNodeType() != Node.ELEMENT_NODE) {
          continue;
        }
        NodeList currencyCodeElement = ((Element) node).getElementsByTagName("currencyCode");
        if (currencyCodeElement.getLength() != 1) {
          continue;
        }
        String currencyCode = currencyCodeElement.item(0).getFirstChild().getTextContent();
        if (!currencyCode.equals("USD")) {
          continue;
        }
        NodeList rateElement = ((Element) node).getElementsByTagName("rateNew");
        if (rateElement.getLength() != 1) {
          continue;
        }
        rate =
            BigDecimal.valueOf(
                Double.parseDouble(rateElement.item(0).getFirstChild().getTextContent()));
      }
      if (rate == null) {
        return new HashMap<>();
      }
      Map<LocalDate, BigDecimal> fxRates = new HashMap<>();
      for (LocalDate current = LocalDate.of(year, month, 1);
          current.getMonthValue() == month;
          current = current.plusDays(1)) {
        fxRates.put(current, rate);
      }
      return fxRates;
    } catch (Exception e) {
      return new HashMap<>();
    }
  }

  private Map<LocalDate, BigDecimal> requestMonthNew(int month, int year) throws IOException {
    var request =
        new Request.Builder()
            .url(
                String.format(
                    "https://www.trade-tariff.service.gov.uk/api/v2/exchange_rates/files/monthly_csv_%d-%d.csv",
                    year, month))
            .get()
            .build();
    try (var response = client.newCall(request).execute()) {
      Scanner scanner = new Scanner(response.body().byteStream());
      scanner.useDelimiter("[,\\n]");
      scanner.nextLine();
      while (scanner.hasNextLine()) {
        scanner.next(); // Country
        scanner.next(); // Currency name
        String currency = scanner.next();
        if (currency.equals("USD")) {
          break;
        }
        scanner.nextLine();
      }
      // We are on USD line or finished file.
      if (!scanner.hasNextLine()) {
        return new HashMap<>();
      }
      BigDecimal fx = scanner.nextBigDecimal();
      LocalDate from = LocalDate.parse(scanner.next(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
      LocalDate to = LocalDate.parse(scanner.next(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
      Map<LocalDate, BigDecimal> fxRates = new HashMap<>();
      for (LocalDate current = from; !current.isAfter(to); current = current.plusDays(1)) {
        fxRates.put(current, fx);
      }
      return fxRates;
    }
  }
}
