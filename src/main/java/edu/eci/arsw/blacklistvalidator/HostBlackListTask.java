package edu.eci.arsw.blacklistvalidator;

import edu.eci.arsw.spamkeywordsdatasource.HostBlacklistsDataSourceFacade;
import java.util.LinkedList;
import java.util.List;

public class HostBlackListTask implements Runnable{
    private final HostBlacklistsDataSourceFacade skds;
    private final String ipaddress;
    private final int startIndex;
    private final int endIndex;

    private int ocurrencesCount = 0;
    private int checkedListCount = 0;
    private final List<Integer> blacklistOcurrences = new LinkedList<>();

    public HostBlackListTask(HostBlacklistsDataSourceFacade skds, String ipaddress, int startIndex, int endIndex) {
        this.skds = skds;
        this.ipaddress = ipaddress;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }
    public void run() {
        for (int i = startIndex; i < endIndex; i++) {
            if (skds.isInBlackListServer(i, ipaddress)) {
                ocurrencesCount++;
                blacklistOcurrences.add(i);
            }
            checkedListCount++;
        }
    }

    public int getOcurrencesCount() {
        return ocurrencesCount;
    }

    public int getCheckedListCount() {
        return checkedListCount;
    }

    public List<Integer> getBlacklistOcurrences() {
        return blacklistOcurrences;
    }
}
