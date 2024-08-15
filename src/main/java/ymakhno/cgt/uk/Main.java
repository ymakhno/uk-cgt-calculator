package ymakhno.cgt.uk;


import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Stream;
import ymakhno.cgt.uk.Event.EventType;
import ymakhno.cgt.uk.fx.CacheableFxLoader;
import ymakhno.cgt.uk.fx.EcbFxLoader;

public class Main {

  private static List<Event> parseVestEvents(
      String filename, LongAdder idAllocator, Map<LocalDate, BigDecimal> fxRates)
      throws IOException {
    Scanner scanner = new Scanner(Path.of(filename));
    scanner.useDelimiter("[,\\n]");
    scanner.nextLine();
    List<Event> result = new ArrayList<>();
    int id = 0;
    while (scanner.hasNext()) {
      LocalDate date =
          LocalDate.parse(
              scanner.next().replace("Sep-", "Sept-"), DateTimeFormatter.ofPattern("dd-LLL-yyyy"));
      scanner.next(); // Order number
      scanner.next(); // Plan
      scanner.next(); // Type
      scanner.next(); // Status
      BigDecimal priceUsd = parseMoney(scanner.next());
      scanner.next(); // Full amount
      scanner.next(); // Proceed
      BigDecimal amount = scanner.nextBigDecimal();
      scanner.next(); // Payment method
      BigDecimal fx = getFxRate(fxRates, date); // scanner.nextBigDecimal();
      scanner.next(); // FX

      result.add(
          new Event(
              idAllocator.intValue(),
              EventType.VEST,
              date,
              priceUsd,
              fx,
              amount,
              /* fee= */ BigDecimal.ZERO));
      idAllocator.increment();
    }
    return result;
  }

  private static BigDecimal getFxRate(Map<LocalDate, BigDecimal> fxRates, LocalDate date) {
    while (!fxRates.containsKey(date)) {
      date = date.minusDays(1);
    }
    return fxRates.get(date);
  }

  private static List<Event> parseSaleEvents(
      String filename, LongAdder idAllocator, Map<LocalDate, BigDecimal> fxRates)
      throws IOException {
    Scanner scanner = new Scanner(Path.of(filename));
    scanner.useDelimiter("[,\\n]");
    scanner.nextLine();
    List<Event> result = new ArrayList<>();
    while (scanner.hasNext()) {
      LocalDate date =
          LocalDate.parse(
              scanner.next().replace("Sep-", "Sept-"), DateTimeFormatter.ofPattern("dd-LLL-yyyy"));
      scanner.next(); // Order number
      scanner.next(); // Plan
      scanner.next(); // Type
      scanner.next(); // Status
      BigDecimal priceUsd = parseMoney(scanner.next());
      BigDecimal amount = scanner.nextBigDecimal().negate();
      BigDecimal receivedTotal = parseMoney(scanner.next());
      scanner.next(); //
      scanner.next(); //
      BigDecimal fx = getFxRate(fxRates, date); // scanner.nextBigDecimal();
      scanner.next(); // FX
      if (date.isBefore(LocalDate.of(2022, Month.JULY, 15))) {
        priceUsd = priceUsd.divide(BigDecimal.valueOf(20));
        amount = amount.multiply(BigDecimal.valueOf(20));
      }
      BigDecimal fee = amount.multiply(priceUsd).subtract(receivedTotal);
      result.add(
          new Event(idAllocator.intValue(), EventType.SALE, date, priceUsd, fx, amount, fee));
      idAllocator.increment();
    }
    return result;
  }

  private static BigDecimal parseMoney(String token) {
    return BigDecimal.valueOf(
        Double.parseDouble(token.replace("\"", "").replace(",", "").replace("$", "")));
  }

  public static void main(String[] args) throws Exception {
    LongAdder idAllocator = new LongAdder();
    Path cachePath = Path.of("/Users/iuriimak/projects/report/cache");
    Map<LocalDate, BigDecimal> fxRates =
        new CacheableFxLoader(cachePath, new EcbFxLoader()).loadFxRates();
    List<Event> events =
        Stream.concat(
                parseVestEvents(
                    "/Users/iuriimak/projects/report/releases.csv", idAllocator, fxRates)
                    .stream(),
                parseSaleEvents("/Users/iuriimak/projects/report/sales.csv", idAllocator, fxRates)
                    .stream())
            .toList();
    List<Sale> processedSales = new EventProcessor().processEvents(events);
    System.out.println(CgtCalculator.calculateGainPerYear(processedSales));
  }
}
