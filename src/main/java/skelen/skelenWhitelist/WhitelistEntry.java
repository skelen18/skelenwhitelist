package skelen.skelenWhitelist;

public class WhitelistEntry {
    private String uuid;
    private String name;

    public WhitelistEntry(String uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }
}
