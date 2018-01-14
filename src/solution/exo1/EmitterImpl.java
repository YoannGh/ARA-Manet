package solution.exo1;

import manet.Message;
import manet.communication.Emitter;
import manet.positioning.Position;
import manet.positioning.PositionProtocol;
import manet.positioning.PositionProtocolImpl;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

public class EmitterImpl implements Emitter {

	private static final String pp_PID = "positionprotocolimpl";
	
	private static final String PAR_LATENCY="latency";
	private static final String PAR_SCOPE="scope";
	
	private final int my_pid;
	
	private int latency;
	private int scope;
	
	public EmitterImpl(String prefix){
		String tmp[]=prefix.split("\\.");
		my_pid=Configuration.lookupPid(tmp[tmp.length-1]);
		this.latency=Configuration.getInt(prefix+"."+PAR_LATENCY);
		this.scope=Configuration.getInt(prefix+"."+PAR_SCOPE);
	}

	@Override
	public void emit(Node host, Message msg) {
		int positionprotocol_pid = Configuration.lookupPid(pp_PID);
		PositionProtocol ppEmitter = (PositionProtocol) host.getProtocol(positionprotocol_pid);
		Position positionEmitter = ppEmitter.getCurrentPosition();

		for(int i = 0; i < Network.size(); i++) {
			Node n = Network.get(i);
			if(n.getID() != host.getID()) {
				PositionProtocol ppNode = (PositionProtocol) n.getProtocol(positionprotocol_pid);
				Position positionNode = ppNode.getCurrentPosition();
				double distance = Math.hypot(	positionEmitter.getX()-positionNode.getX(),
												positionEmitter.getY()-positionNode.getY());

				if (distance <= scope) {
					System.out.println("Distance entre " + host.getID() + " et " + n.getID() + ": " + distance);
					EDSimulator.add(latency, msg, n, msg.getPid());
				}
			}
		}
	}

	@Override
	public int getLatency() {
		return latency;
	}

	@Override
	public int getScope() {
		return scope;
	}
	
	public Object clone(){
		EmitterImpl res=null;
		try {
			res=(EmitterImpl)super.clone();
			res.latency = latency;
			res.scope = scope;
		} catch (CloneNotSupportedException e) {}
		return res;
	}

}