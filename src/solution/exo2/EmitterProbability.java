package solution.exo2;

import manet.Message;
import manet.positioning.Position;
import manet.positioning.PositionProtocol;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

public class EmitterProbability extends EmitterGossip{

	private static final String PAR_PROBABILITY = "probability";

	private static double probability;

	public EmitterProbability(String prefix) {
		super(prefix);
		EmitterProbability.probability=Configuration.getDouble(prefix+"."+PAR_PROBABILITY);
	}

	public void emit(Node host, Message msg) {
		int positionprotocol_pid = Configuration.lookupPid(pp_PID);
		PositionProtocol ppEmitter = (PositionProtocol) host.getProtocol(positionprotocol_pid);
		Position positionEmitter = ppEmitter.getCurrentPosition();

		//If node received the message for the first time, flood to all neighbors
		if(recvGossip == 0) {
			for(int i = 0; i < Network.size(); i++) {
				Node n = Network.get(i);
				if(n.getID() != host.getID()) {
					PositionProtocol ppNode = (PositionProtocol) n.getProtocol(positionprotocol_pid);
					Position positionNode = ppNode.getCurrentPosition();
					double distance = Math.hypot(	positionEmitter.getX()-positionNode.getX(),
							positionEmitter.getY()-positionNode.getY());
					if (distance <= scope && CommonState.r.nextDouble() < probability) {
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
}
