import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

// Author Anand Sainbileg 20327050
public class RoutingTable {
    private ConcurrentHashMap<String, RoutingTableEntry> entries = new ConcurrentHashMap<>();

    public void addEntry(String destination, String nextHop, long validityDurationInSeconds) {
        RoutingTableEntry newEntry = new RoutingTableEntry(destination, nextHop, validityDurationInSeconds);
        entries.put(destination, newEntry);
        //printAllEntries(); 
    }

    public void removeEntry(String destination) {
        entries.remove(destination);
    }
    
    public void removeExpiredEntries() {
        Iterator<RoutingTableEntry> iterator = entries.values().iterator();
        while (iterator.hasNext()) {
            RoutingTableEntry entry = iterator.next();
            if (entry.hasExpired()) {
                iterator.remove();
                System.out.println("entry removed");
            }
        }
    }

    public boolean containsEntry(String destination, String nextHop) {
        RoutingTableEntry entry = entries.get(destination);
        return entry != null && entry.getNextHop().equals(nextHop) && !entry.hasExpired();
    }

    public String getNextHop(String destination) {
        RoutingTableEntry entry = entries.get(destination);
        if (entry != null && !entry.hasExpired()) {
            return entry.getNextHop();
        }
        return null; // Return null if there is no route or the route has expired
    }

    public void printAllEntries() {
        for (RoutingTableEntry entry : entries.values()) {
            System.out.println(entry);
        }
    }
}
