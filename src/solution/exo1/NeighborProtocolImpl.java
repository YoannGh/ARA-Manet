package solution.exo1;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import manet.Message;
import manet.communication.Emitter;
import manet.detection.NeighborProtocol;
import manet.detection.NeighborhoodListener;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;

import javax.net.ssl.ExtendedSSLSession;

public class NeighborProtocolImpl implements NeighborProtocol, EDProtocol {
	
	public static final String DO_HEARTBEAT_EVENT = "do_heartbeat";
	public static final String TIMEOUT_EVENT = "timeout";

	public static final String MSG_TAG_PROBE = "probe";

	private static final String PAR_PROBE_PERIOD="probe_period";
	private static final String PAR_TIMEOUT="timeout";
	private static final String PAR_EMITTER="emitter";
	private static final String PAR_NEIGHBOR_LISTENER="neighbor_listener";
	
	private final int my_pid;
	
	private int timeout;
	private long probe_period;
	private int emitter_pid;
	private int neightbor_listener_pid;
	private int current_probe_id = 1;

	private List<Message> neighbors_msg;
	private List<Long> neighbors_ids;
	
	public NeighborProtocolImpl(String prefix){
		String tmp[]=prefix.split("\\.");
		my_pid=Configuration.lookupPid(tmp[tmp.length-1]);
		this.probe_period=Configuration.getLong(prefix+"."+PAR_PROBE_PERIOD);
		this.timeout=Configuration.getInt(prefix+"."+PAR_TIMEOUT);
		this.emitter_pid=Configuration.getPid(prefix+"."+PAR_EMITTER);
		this.neightbor_listener_pid=Configuration.getPid(prefix+"."+PAR_NEIGHBOR_LISTENER, -1);

		neighbors_msg = new LinkedList<>();
		neighbors_ids = new LinkedList<>();

	}
	
	@Override
	public List<Long> getNeighbors() {
		return neighbors_ids;
	}
	
	public Object clone(){
		NeighborProtocolImpl res=null;
		try {
			res=(NeighborProtocolImpl)super.clone();
			res.timeout = timeout;
			res.probe_period = probe_period;
			res.emitter_pid = emitter_pid;
			res.neightbor_listener_pid = neightbor_listener_pid;
			res.current_probe_id = current_probe_id;
			res.neighbors_msg = new ArrayList<>(neighbors_msg);
			res.neighbors_ids = new ArrayList<>(neighbors_ids);
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
			if(ev.equals(DO_HEARTBEAT_EVENT)) {
				current_probe_id++;

				for(int i = 0; i < Network.size(); i++) {
					Node dest = Network.get(i);
					if(dest.getID() != node.getID()) {
						Message probeMsg = new Message(node.getID(), dest.getID(), MSG_TAG_PROBE, new Integer(current_probe_id) , my_pid);
						Emitter emitter = (Emitter) node.getProtocol(emitter_pid);
						//Envoi du Probe
						emitter.emit(node, probeMsg);
					}
				}
				//Armer l'envoi du prochain Probe
				EDSimulator.add(probe_period, DO_HEARTBEAT_EVENT, node, my_pid);
				return;
			}
			//Réception d'un évènement TimeOut
			else if (ev.equals(TIMEOUT_EVENT)) {
				for(Iterator<Message> itr = neighbors_msg.iterator(); itr.hasNext(); ) {
					Message m = itr.next();
					Integer msg_probe_id = (Integer) m.getContent();
					if(msg_probe_id != current_probe_id) {
						itr.remove();
						neighbors_ids.remove(m.getIdSrc());
						if(neightbor_listener_pid != -1)
							((NeighborhoodListener)node.getProtocol(neightbor_listener_pid)).lostNeighborDetected(node, m.getIdSrc());
					}
				}
				return;
			}
		}
		else if(event instanceof Message) {
			Message msg = (Message) event;
			if(msg.getIdDest() == node.getID()) {
				// Réception d'un Probe
				if (msg.getTag().equals(MSG_TAG_PROBE)) {
					Integer msg_probe_id = (Integer) msg.getContent();
					if (msg_probe_id == current_probe_id) {
						if (!neighbors_ids.contains(msg.getIdSrc())) {
							neighbors_msg.add(msg);
							neighbors_ids.add(msg.getIdSrc());
							if (neightbor_listener_pid != -1)
								((NeighborhoodListener) node.getProtocol(neightbor_listener_pid)).newNeighborDetected(node, msg.getIdSrc());
						}
					}
					// Armer le prochain TimeOut
					EDSimulator.add(timeout, TIMEOUT_EVENT, node, my_pid);
				}
			}
			return;
		}
		System.out.println("Received unknown event: " + event);
		throw new RuntimeException("Receive unknown Event");
	}

}