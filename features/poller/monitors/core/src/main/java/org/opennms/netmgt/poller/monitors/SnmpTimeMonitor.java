package org.opennms.netmgt.poller.monitors;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;

import org.opennms.core.utils.ParameterMap;
import org.opennms.netmgt.config.BasicSchedule;
import org.opennms.netmgt.config.BasicScheduleUtils;
import org.opennms.netmgt.config.Time;
import org.opennms.netmgt.poller.PollStatus;
import org.opennms.netmgt.poller.MonitoredService;

public class SnmpTimeMonitor extends SnmpMonitor {

    public PollStatus poll(MonitoredService svc, Map<String, Object> parameters) {
        String begins = ParameterMap.getKeyedString(parameters, "begins", "00:00:00");
        String ends = ParameterMap.getKeyedString(parameters, "ends", "23:59:59");
        
        Time time = new Time();
        time.setBegins(begins);
        time.setEnds(ends);
        BasicSchedule basicSchedule = new BasicSchedule();
        basicSchedule.setName("SnmpTimeMonitorSchedule");
        Collection<Time> times =  new HashSet<Time>();
        times.add(time);
        basicSchedule.setTimeCollection(times);
        
        if ( BasicScheduleUtils.isTimeInSchedule(new Date(), basicSchedule)) {
               return super.poll(svc, parameters);
        }

        return PollStatus.up();
       
    }
}
