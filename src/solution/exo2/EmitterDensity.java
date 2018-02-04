package solution.exo2;

import manet.Message;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Node;
import solution.exo1.NeighborProtocolImpl;

public class EmitterDensity extends NotProbabilistEmitter {

	private static final String PAR_K = "k";
	private static final String PAR_NEIGHBORPID ="neighborprotocol";

	private static final String MSG_TAG_GOSSIP = "gossip";

	private double k;
	private int neighbor_pid;

	public EmitterDensity(String prefix) {
		super(prefix);
		this.k = Configuration.getDouble(prefix+"."+PAR_K);
		this.neighbor_pid = Configuration.getPid(prefix+"."+PAR_NEIGHBORPID);
	}

	//Probability algorithm send according to actual density
	public void emit(Node host, Message msg) {
		if(msg.getTag() == MSG_TAG_GOSSIP) {
			int V;
			boolean p;
			NeighborProtocolImpl npi = (NeighborProtocolImpl) host.getProtocol(neighbor_pid);
			V = npi.getNeighbors().size();
			p = V > 0 ? CommonState.r.nextDouble() < k/V : false;
			//p = V > 0 ? CommonState.r.nextDouble() < k*V : false;
			super.emit(host, msg, p);		
		}
		else {
			super.emit(host, msg);
		}
	}

	public Object clone() {
		EmitterDensity res = null;
		res = (EmitterDensity) super.clone();
		res.k = k;
		res.neighbor_pid = neighbor_pid;
		return res;
	}
}
