// Author Anand Sainbileg 20327050

public class RoutingTableEntry {
    private String destination;
    private String nextHop;
    private long expiryTime; // The expiry time in milliseconds

    public RoutingTableEntry(String destination, String nextHop, long validityDurationInSeconds) {
        this.destination = destination;
        this.nextHop = nextHop;
        this.expiryTime = System.currentTimeMillis() + validityDurationInSeconds * 1000;
    }

    // Checks if the entry has expired
    public boolean hasExpired() {
        return System.currentTimeMillis() > expiryTime;
    }

    // Getters for destination and next hop
    public String getDestination() {
        return destination;
    }

    public String getNextHop() {
        return nextHop;
    }

    // Setter for the expiry time if you need to update it
    public void setExpiryTime(long expiryTimeInMillis) {
        this.expiryTime = expiryTimeInMillis;
    }

    // This will return the time remaining in seconds
    public long getTimeRemainingInSeconds() {
        return (expiryTime - System.currentTimeMillis()) / 1000;
    }

    // A string representation of the routing table entry
    @Override
    public String toString() {
        return "RoutingTableEntry{" +
               "destination='" + destination + '\'' +
               ", nextHop='" + nextHop + '\'' +
               ", expiryTime=" + getTimeRemainingInSeconds() +
               " seconds remaining}";
    }
}
