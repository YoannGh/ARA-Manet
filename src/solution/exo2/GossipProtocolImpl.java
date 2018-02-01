package solution.exo2;

import manet.algorithm.gossip.GossipProtocol;
import manet.communication.Emitter;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;

import manet.Message;

public class GossipProtocolImpl implements GossipProtocol, EDProtocol{

	private static final String PAR_EMITTER = "emitter";

	public static final String MSG_TAG_GOSSIP = "gossip";

	private final int my_pid;
	private final int emitter_pid;

	public GossipProtocolImpl(String prefix) {
		String tmp[]=prefix.split("\\.");
		my_pid=Configuration.lookupPid(tmp[tmp.length-1]);
		this.emitter_pid=Configuration.getPid(prefix+"."+PAR_EMITTER);
	}

	@Override
	public void initiateGossip(Node node, int id, long id_initiator) {

		for(int i = 0 ; i< Network.size() ; i++){
			Node n = Network.get(i);
			EmitterGossip eg = (EmitterGossip) n.getProtocol(emitter_pid);
			eg.reset();
		}


		Message gossipMsg = new Message(id_initiator, -1, MSG_TAG_GOSSIP, MSG_TAG_GOSSIP, my_pid);
		EmitterGossip emitter = (EmitterGossip) node.getProtocol(emitter_pid);
		emitter.emit(node, gossipMsg);
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
				Message gossipMsg = new Message(msg.getIdSrc(), -1, MSG_TAG_GOSSIP, MSG_TAG_GOSSIP, my_pid);
				Emitter emitter = (Emitter) node.getProtocol(emitter_pid);
				emitter.emit(node, gossipMsg);
			}
			return;
		}
		System.out.println("Received unknown event: " + event);
		throw new RuntimeException("Receive unknown Event");
	}

	public Object clone(){
		GossipProtocolImpl res=null;
		try {
			res=(GossipProtocolImpl)super.clone();
		} catch (CloneNotSupportedException e) {}
		return res;
	}
}
