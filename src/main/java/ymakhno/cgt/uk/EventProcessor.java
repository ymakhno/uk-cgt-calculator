package ymakhno.cgt.uk;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EventProcessor {
  private final HoldingPool holdingPool;
  private final SaleCalculator saleCalculator;

  public EventProcessor() {
    var bedAndBreakfastVestings = new BedAndBreakfastVestings();
    holdingPool = new HoldingPool(bedAndBreakfastVestings);
    saleCalculator = new SaleCalculator(bedAndBreakfastVestings, holdingPool);
  }

  public List<Sale> processEvents(List<Event> events) {
    return processOrderedEvents(events.stream().sorted(Comparator.comparing(Event::date)).toList());
  }

  private List<Sale> processOrderedEvents(List<Event> events) {
    List<Sale> result = new ArrayList<>();
    for (Event event : events) {
      switch (event.eventType()) {
        case SALE -> result.add(saleCalculator.calculateSale(event, events));
        case VEST -> holdingPool.addVestInPool(event);
      }
    }
    return result;
  }
}
