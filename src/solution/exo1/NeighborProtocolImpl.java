package solution.exo1;

import java.util.List;

import manet.Message;
import manet.communication.Emitter;
import manet.detection.NeighborProtocol;
import peersim.config.Configuration;
import peersim.core.Node;
import peersim.edsim.EDProtocol;

public class NeighborProtocolImpl implements NeighborProtocol, EDProtocol {
	
	public static final String TAG_DO_HEARTBEAT = "do_heartbeat";
	public static final String TAG_PROBE = "probe";
	public static final String TAG_TIMEOUT = "timeout";
	
	
	private static final String PAR_PROBE_PERIOD="probe_period";
	private static final String PAR_TIMEOUT="timeout";
	private static final String PAR_EMITTER="emitter";
	private static final String PAR_NEIGHBOR_LISTENER="neighbor_listener";
	
	private final int my_pid;
	
	private int timeout;
	private int probe_period;
	private int emitter_pid;
	private int neightbor_listener_pid;
	private int probe_id = 1;
	
	public NeighborProtocolImpl(String prefix){
		String tmp[]=prefix.split("\\.");
		my_pid=Configuration.lookupPid(tmp[tmp.length-1]);
		this.probe_period=Configuration.getInt(prefix+"."+PAR_PROBE_PERIOD);
		this.timeout=Configuration.getInt(prefix+"."+PAR_TIMEOUT);
		this.emitter_pid=Configuration.getPid(prefix+"."+PAR_EMITTER);
		this.neightbor_listener_pid=Configuration.getPid(prefix+"."+PAR_NEIGHBOR_LISTENER, -1);
	}
	
	@Override
	public List<Long> getNeighbors() {
		
		return null;
	}
	
	public Object clone(){
		NeighborProtocolImpl res=null;
		try {
			res=(NeighborProtocolImpl)super.clone();
		} catch (CloneNotSupportedException e) {}
		return res;
	}

	@Override
	public void processEvent(Node node, int pid, Object event) {
		if(pid != my_pid){
			throw new RuntimeException("Receive Event for wrong protocol");
		}
		if(event instanceof String) {
			String ev = (String) event;
			if(ev.equals(TAG_DO_HEARTBEAT)) {
				
				Message probeMsg = new Message(my_pid, iddest, TAG_PROBE, new Integer(probe_id) , my_pid);
				Emitter emitter = (Emitter) node.getProtocol(emitter_pid);
				emitter.emit(node, probeMsg);
			}
		}
		else if(event instanceof Message) {
			/*String ev = (String) event;
			if(ev.equals(loop_event)){
				move(node);
				return;
			}*/
			Message msg = (Message) event;
			if(msg.getTag().equals(TAG_HEARTBEAT)) {
				
			}
		}
		throw new RuntimeException("Receive unknown Event");
		
	}

}
