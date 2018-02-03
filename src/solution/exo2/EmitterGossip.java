package solution.exo2;

import manet.Message;
import manet.positioning.Position;
import manet.positioning.PositionProtocol;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;
import solution.exo1.EmitterImpl;

public abstract class EmitterGossip extends EmitterImpl {

	private int recvGossip;
	private int sendGossip;

	public EmitterGossip(String prefix) {
		super(prefix);
		reset();
	}

	public void emit(Node host, Message msg, boolean canSend) {
		int positionprotocol_pid = Configuration.lookupPid(pp_PID);
		PositionProtocol ppEmitter = (PositionProtocol) host.getProtocol(positionprotocol_pid);
		Position positionEmitter = ppEmitter.getCurrentPosition();

		//If node received the message for the first time, flood to all neighbors
		if(recvGossip == 0 && canSend) {
			for(int i = 0; i < Network.size(); i++) {
				Node n = Network.get(i);
				if(n.getID() != host.getID()) {
					PositionProtocol ppNode = (PositionProtocol) n.getProtocol(positionprotocol_pid);
					Position positionNode = ppNode.getCurrentPosition();
					double distance = Math.hypot(	positionEmitter.getX()-positionNode.getX(),
							positionEmitter.getY()-positionNode.getY());

					if (distance <= scope) {
						//host now belongs to emitters
						sendGossip += 1;
						EDSimulator.add(latency, msg, n, msg.getPid());
					}

				}
			}
		}
		//Emit is called from a node that received the gossip
		recvGossip+=1;
	}

	public boolean broadcastDone() {
		return (recvGossip == sendGossip || recvGossip == -1);
	}

	public void reset() {
		recvGossip = 0;
		sendGossip = 0;
	}

	public int getRecv() {
		return recvGossip;
	}

	public int getSend() {
		return sendGossip;
	}

	//emit without counting the emitter node as a previous receiver
	public void emitSpontaneous(Node host, Message msg) {
		emit(host, msg, true);
		recvGossip-=1;
	}

	public Object clone() {
		EmitterGossip res = null;
		res = (EmitterGossip) super.clone();
		res.recvGossip = recvGossip;
		res.sendGossip = sendGossip;
		return res;
	}
}