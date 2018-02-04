package solution.exo2;

import manet.algorithm.gossip.GossipProtocol;
import manet.communication.Emitter;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;

import org.lsmp.djep.groupJep.GOperatorSet;

import manet.Message;

public class GossipProtocolImpl implements GossipProtocol, EDProtocol{

	private static final String PAR_EMITTER = "emitter";

	public static final String MSG_TAG_GOSSIP = "gossip";
	private static final String MSG_TAG_TIMER = "timer";

	private final int my_pid;
	private int emitter_pid;
	private long sender = -1;
	private int broadcastId = 0;

	public GossipProtocolImpl(String prefix) {
		String tmp[]=prefix.split("\\.");
		my_pid=Configuration.lookupPid(tmp[tmp.length-1]);
		this.emitter_pid=Configuration.getPid(prefix+"."+PAR_EMITTER);
	}

	@Override
	public void initiateGossip(Node node, int id, long id_initiator) {

		broadcastId = id;
		for(int i = 0 ; i< Network.size() ; i++){
			Node n = Network.get(i);
			EmitterGossip eg = (EmitterGossip) n.getProtocol(emitter_pid);
			eg.reset();
			sender = -1;
		}

		Message gossipMsg = new Message(id_initiator, -1, MSG_TAG_GOSSIP, MSG_TAG_GOSSIP, my_pid);
		EmitterGossip emitter = (EmitterGossip) node.getProtocol(emitter_pid);
		emitter.emitSpontaneous(node, gossipMsg);
	}

	@Override
	public void processEvent(Node node, int pid, Object event) {
		if(pid != my_pid){
			throw new RuntimeException("Receive Event for wrong protocol");
		}
		if (event instanceof Message) {
			Message msg = (Message) event;
			String ev = msg.getTag();
			if( ev == MSG_TAG_GOSSIP) {
				sender = msg.getIdSrc();
				Emitter emitter = (Emitter) node.getProtocol(emitter_pid);
				emitter.emit(node, msg);
			}
			else if( ev == MSG_TAG_TIMER) {
				int id_broadcast = (int) msg.getContent();
				NotProbabilistEmitter npe;
				if(broadcastId == id_broadcast) {
					Message timerMsg = new Message(node.getID(), -1, MSG_TAG_GOSSIP, MSG_TAG_GOSSIP, my_pid);
					npe = (NotProbabilistEmitter) node.getProtocol(emitter_pid);
					npe.processTimer(node, msg);
				}
			}
			return;
		}
		System.out.println("Received unknown event: " + event);
		throw new RuntimeException("Receive unknown Event");
	}

	public long getSender() {
		return sender;
	}

	public int getBroadcastId() {
		return broadcastId;
	}

	public Object clone(){
		GossipProtocolImpl res = null;
		try {
			res = (GossipProtocolImpl) super.clone();
			res.emitter_pid = emitter_pid;
			res.sender = sender;
			res.broadcastId = broadcastId;
		} catch (CloneNotSupportedException e) {}
		return res;
	}
}
