/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.eci.arsw.blacklistvalidator;

import edu.eci.arsw.spamkeywordsdatasource.HostBlacklistsDataSourceFacade;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author hcadavid
 */
public class HostBlackListsValidator {

    private static final int BLACK_LIST_ALARM_COUNT=5;
    
    /**
     * Check the given host's IP address in all the available black lists,
     * and report it as NOT Trustworthy when such IP was reported in at least
     * BLACK_LIST_ALARM_COUNT lists, or as Trustworthy in any other case.
     * The search is not exhaustive: When the number of occurrences is equal to
     * BLACK_LIST_ALARM_COUNT, the search is finished, the host reported as
     * NOT Trustworthy, and the list of the five blacklists returned.
     * @param ipaddress suspicious host's IP address.
     * @return  Blacklists numbers where the given host's IP address was found.
     */
    public List<Integer> checkHost(String ipaddress, int n){
        
        LinkedList<Integer> blackListOcurrences=new LinkedList<>();
        HostBlacklistsDataSourceFacade skds=HostBlacklistsDataSourceFacade.getInstance();

        int totalServers = skds.getRegisteredServersCount();
        int patitionSize = totalServers / n;
        int remainder = totalServers % n;

        List<HostBlackListTask> tasks = new LinkedList<>();
        List<Thread> threads = new LinkedList<>();
                
        int start = 0;
        for (int i = 0; i < n; i++){
            int end = start + patitionSize;
            if (i == n-1){
                end += remainder;
            }
                
            HostBlackListTask task = new HostBlackListTask(skds, ipaddress, start, end);
            Thread thread = new Thread(task);

            tasks.add(task);
            threads.add(thread);
            thread.start();
            start = end;
        }

        for(Thread t : threads){
            try {
                t.join();
            }catch (InterruptedException e) {
                Logger.getLogger(HostBlackListsValidator.class.getName()).log(Level.SEVERE, null, e);
            }
        }

        int totalOcurrences = 0;
        int totalCheckedLists = 0;

        for(HostBlackListTask task : tasks){
            totalOcurrences += task.getOcurrencesCount();
            totalCheckedLists += task.getCheckedListCount();
            blackListOcurrences.addAll(task.getBlacklistOcurrences());
        }

        if (totalOcurrences>=BLACK_LIST_ALARM_COUNT){
            skds.reportAsNotTrustworthy(ipaddress);
        }
        else{
            skds.reportAsTrustworthy(ipaddress);
        }                
        
        LOG.log(Level.INFO, "Checked Black Lists:{0} of {1}", new Object[]{totalCheckedLists, skds.getRegisteredServersCount()});
        
        return blackListOcurrences;
    }
    
    
    private static final Logger LOG = Logger.getLogger(HostBlackListsValidator.class.getName());
    
    
    
}
