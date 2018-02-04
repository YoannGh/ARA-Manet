package solution.exo2;

import manet.Message;
import manet.positioning.Position;
import manet.positioning.PositionProtocol;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;

public class EmitterDistance extends NotProbabilistEmitter{

	private static final String PAR_GOSSIP_IMPL ="gossipprotocol";

	private int gossipprotocol_pid;

	public EmitterDistance(String prefix) {
		super(prefix);
		this.gossipprotocol_pid = Configuration.getPid(prefix+"."+PAR_GOSSIP_IMPL);
	}

	private double distance(Node a, Node b) {
		int positionprotocol_pid = Configuration.lookupPid(pp_PID);
		PositionProtocol ppEmitter = (PositionProtocol) a.getProtocol(positionprotocol_pid);
		Position positionEmitter = ppEmitter.getCurrentPosition();
		PositionProtocol ppNode = (PositionProtocol) b.getProtocol(positionprotocol_pid);
		Position positionNode = ppNode.getCurrentPosition();
		return Math.hypot(	positionEmitter.getX()-positionNode.getX(),	positionEmitter.getY()-positionNode.getY());
	}

	//Distance algorithm send according the distance between the node and the sender of the msg
	public void emit(Node host, Message msg) {
		boolean p;
		GossipProtocolImpl gpi = (GossipProtocolImpl) host.getProtocol(gossipprotocol_pid);
		long idSender = gpi.getSender();
		//if initiator node send to all
		if(idSender == -1) {
			p = true;
		}
		else {
			p = scope > 0 ? (CommonState.r.nextDouble() < distance(host, Network.get((int) idSender))/scope) : false;
		}
		super.emit(host, msg, p);
	}

	public Object clone() {
		EmitterDistance res = null;
		res = (EmitterDistance) super.clone();
		res.gossipprotocol_pid = gossipprotocol_pid;
		return res;
	}
}