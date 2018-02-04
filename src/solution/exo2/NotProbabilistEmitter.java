package solution.exo2;

import java.util.AbstractSet;
import java.util.List;
import java.util.TreeSet;

import manet.Message;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Node;
import peersim.edsim.EDSimulator;
import solution.exo1.NeighborProtocolImpl;

public abstract class NotProbabilistEmitter extends EmitterGossip {

	private static final String PAR_NEIGHBORPID ="neighborprotocol";
	private static final String PAR_GOSSIPPID ="gossipprotocol";
	private static final String PAR_ACTIVATE ="notprobabilist";
	private static final String PAR_TIMERMIN ="timermin";
	private static final String PAR_TIMERMAX ="timermax";

	private static final String MSG_TAG_GOSSIP = "gossip";
	private static final String MSG_TAG_TIMER = "timer";

	private boolean activate;
	private int neighbor_pid;
	private int gossip_pid;
	private int timer_min;
	private int timer_max;

	private AbstractSet<Long> lx;

	public NotProbabilistEmitter(String prefix) {
		super(prefix);
		this.activate = Configuration.getBoolean(prefix+"."+PAR_ACTIVATE);

		if(activate) {
			this.neighbor_pid = Configuration.getPid(prefix+"."+PAR_NEIGHBORPID);
			this.gossip_pid = Configuration.getPid(prefix+"."+PAR_GOSSIPPID);
			this.timer_min = Configuration.getInt(prefix+"."+PAR_TIMERMIN);
			this.timer_max = Configuration.getInt(prefix+"."+PAR_TIMERMAX);
			lx = new TreeSet<Long>();
		}
	}

	public void emit(Node host, Message msg, boolean canSend) {

		if(activate == false) {
			super.emit(host, msg, canSend);
			return;
		}

		NeighborProtocolImpl npi = (NeighborProtocolImpl) host.getProtocol(neighbor_pid);

		//if send is false and this is the first receiving, set the random timer and set lx
		if(canSend == false && super.getRecv() == 0) {
			//Set lx
			for(long id : npi.getNeighbors())
				lx.add(id);

			//Set timer
			int timer = (int) (CommonState.r.nextDouble()*((double)timer_max-(double)timer_min)+(double)timer_min);
			GossipProtocolImpl gp = (GossipProtocolImpl) host.getProtocol(gossip_pid);
			Message timerMsg = new Message(host.getID(), -1, MSG_TAG_TIMER, gp.getBroadcastId(), msg.getPid());
			EDSimulator.add(timer, timerMsg, host, msg.getPid());
		}
		//at each reception, update the list of neighbors that potentially never received msg
		if (msg.getContent() instanceof List<?>) {
			List<Long> recv = (List<Long>) msg.getContent();
			for(long id : recv)
				lx.remove(id);
		}
		//before sending the message, we have to add our current neighbors
		Message neiMsg = new Message(host.getID(), -1, MSG_TAG_GOSSIP, npi.getNeighbors(), msg.getPid());

		super.emit(host, neiMsg, canSend);
	}

	public void processTimer(Node host, Message msg) {
		if(lx.size() > 0) {
			Message tmpMsg = new Message(msg.getIdSrc(), msg.getIdSrc(), MSG_TAG_GOSSIP, msg.getContent(), msg.getPid());
			super.emitSpontaneous(host, tmpMsg);
		}
	}

	public Object clone() {
		NotProbabilistEmitter res = null;
		res = (NotProbabilistEmitter) super.clone();
		res.activate = activate;
		res.neighbor_pid = neighbor_pid;
		res.gossip_pid = gossip_pid;
		res.timer_min = timer_min;
		res.timer_max = timer_max;
		if(activate) {
			res.lx = new TreeSet<>(lx);
		}
		return res;
	}

}
