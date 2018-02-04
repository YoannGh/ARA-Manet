package solution.exo1;

import manet.Message;
import manet.communication.Emitter;
import manet.detection.NeighborProtocol;
import manet.detection.NeighborhoodListener;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;

import java.util.*;

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

	private Queue<Message> pending_probes;
	private Map<Long, Integer> received_probes;

	public NeighborProtocolImpl(String prefix){
		String tmp[]=prefix.split("\\.");
		my_pid=Configuration.lookupPid(tmp[tmp.length-1]);
		this.probe_period=Configuration.getLong(prefix+"."+PAR_PROBE_PERIOD);
		this.timeout=Configuration.getInt(prefix+"."+PAR_TIMEOUT);
		this.emitter_pid=Configuration.getPid(prefix+"."+PAR_EMITTER);
		this.neightbor_listener_pid=Configuration.getPid(prefix+"."+PAR_NEIGHBOR_LISTENER, -1);
		pending_probes = new LinkedList<>();
		received_probes = new HashMap<>();
	}

	@Override
	public List<Long> getNeighbors() {
		return new ArrayList<>(received_probes.keySet());
	}

	public Object clone(){
		NeighborProtocolImpl res=null;
		try {
			res=(NeighborProtocolImpl)super.clone();
			res.timeout = timeout;
			res.probe_period = probe_period;
			res.emitter_pid = emitter_pid;
			res.neightbor_listener_pid = neightbor_listener_pid;
			res.pending_probes = new LinkedList<>(pending_probes);
			res.received_probes = new HashMap<>(received_probes);
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
				Message probeMsg = new Message(node.getID(), Emitter.ALL, MSG_TAG_PROBE, null, my_pid);
				Emitter emitter = (Emitter) node.getProtocol(emitter_pid);
				//Envoi du Probe
				emitter.emit(node, probeMsg);
				//Armer l'envoi du prochain Probe
				EDSimulator.add(probe_period, DO_HEARTBEAT_EVENT, node, my_pid);
				return;
			}
			//Réception d'un évènement TimeOut
			else if (ev.equals(TIMEOUT_EVENT)) {

				Message m = pending_probes.poll();
				if(m != null) {
					Integer nbTimeout = received_probes.get(m.getIdSrc()) - 1;
					if(nbTimeout == 0) {
						received_probes.remove(m.getIdSrc());
						if(neightbor_listener_pid != -1)
							((NeighborhoodListener)node.getProtocol(neightbor_listener_pid)).lostNeighborDetected(node, m.getIdSrc());
					}
					else if(nbTimeout > 0) {
						received_probes.put(m.getIdSrc(), nbTimeout);
					}
					else {
						System.err.println("nbTimeOut = null should not happen");
					}
				} else {
					System.err.println("pending_probes was empty during timeout, this should not happen");
				}
				return;
			}
		}
		else if(event instanceof Message) {
			Message msg = (Message) event;
			if(msg.getIdDest() == node.getID() || msg.getIdDest() == Emitter.ALL) {
				// Réception d'un Probe
				if (msg.getTag().equals(MSG_TAG_PROBE)) {
					pending_probes.offer(msg);
					Integer nbTimeout = received_probes.get(msg.getIdSrc());
					if(nbTimeout == null) {
						received_probes.put(msg.getIdSrc(), 1);
						if (neightbor_listener_pid != -1)
							((NeighborhoodListener) node.getProtocol(neightbor_listener_pid)).newNeighborDetected(node, msg.getIdSrc());
					} else {
						received_probes.put(msg.getIdSrc(), nbTimeout + 1);
					}

					// Armer le prochain TimeOut
					EDSimulator.add(timeout, TIMEOUT_EVENT, node, my_pid);
				}
			}
			return;
		}
		System.err.println("Received unknown event: " + event);
		throw new RuntimeException("Receive unknown Event");
	}

}
