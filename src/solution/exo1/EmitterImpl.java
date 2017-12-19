package solution.exo1;

import manet.Message;
import manet.communication.Emitter;
import manet.positioning.PositionProtocolImpl;
import peersim.config.Configuration;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

public class EmitterImpl implements Emitter {
	
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
		EDSimulator.add(latency, msg.getTag(), host, msg.getPid());
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
		} catch (CloneNotSupportedException e) {}
		return res;
	}

}
