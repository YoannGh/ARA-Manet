package solution.exo2;

import java.util.ArrayList;

import manet.algorithm.gossip.GossipProtocol;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;
import solution.exo1.DensityControler;
import solution.exo1.NeighborProtocolImpl;

public class GossipControler implements Control{

	private static final String PAR_GOSSIPPID ="gossipprotocol";
	private static final String PAR_N ="N";

	private static int gossip_pid;
	private static int N;
	private int currBroadcast = 0;
	private int totalAtt = 0;
	private int totalER = 0;
	private ArrayList<Double> att_trace = new ArrayList<Double>();
	private ArrayList<Double> er_trace = new ArrayList<Double>();

	public GossipControler(String prefix) {
		this.gossip_pid=Configuration.getInt(prefix+"."+PAR_GOSSIPPID);
		this.N = Configuration.getInt(prefix+"."+PAR_N);
	}

	public void processAtt() {
		double att = 0;
		for(int i = 0 ; i< Network.size() ; i++){
			Node n = Network.get(i);
			GossipProtocolImpl gpi = (GossipProtocolImpl) n.getProtocol(gossip_pid);
			if(gpi.getAtt())
				att+=1;
			gpi.setGossip(0);
		}
		att = att/Network.size()*100;
		totalAtt+= att;
		att_trace.add(att);
	}

	public double processER() {
		double recv = 0;
		double send = 0;
		double res;

		for(int i = 0 ; i< Network.size() ; i++){
			Node n = Network.get(i);
			GossipProtocolImpl gpi = (GossipProtocolImpl) n.getProtocol(gossip_pid);
			if(gpi.getGossip())
				recv+=1;
			if(gpi.getGossip())
				send+=1;
			gpi.setGossip(0);
			gpi.setGossip(0);
		}
		res = (recv - send)/ recv;
		totalER+= res;
		er_trace.add(res);
	}

	private double standardVariationAtt() {
		double avgAtt = totalAtt/att_trace.size();
		double sv = 0;
		for(double i : att_trace){
			sv += Math.pow((i - avgAtt), 2);
		}
		sv = sv/att_trace.size();
		return Math.sqrt(sv);
	}

	private double standardVariationER() {
		double avgER = totalER/er_trace.size();
		double sv = 0;
		for(double i : er_trace){
			sv += Math.pow((i - avgER), 2);
		}
		sv = sv/er_trace.size();
		return Math.sqrt(sv);
	}




	@Override
	public boolean execute() {
		if()
		if( broadcastDone() && (currBroadcast < N)) {
			currBroadcast+=1;
			Node initialNode = Network.get(CommonState.r.nextInt(Network.size()));
			GossipProtocol gp = (GossipProtocol) initialNode.getProtocol(gossip_pid);
			gp.initiateGossip(initialNode, currBroadcast, initialNode.getID());
		}

		if(broadcastDone() && currBroadcast == N) {
			System.out.println("Avgatt = " + totalAtt/att_trace.size());
			System.out.println("Avger = " + totalER/er_trace.size());
			System.out.println("Eatt = " + standardVariationAtt());
			System.out.println("Eer = " + standardVariationER());
		}

		return false;
	}


	public Object clone(){
		DensityControler res=null;
		try {
			res=(DensityControler)super.clone();
		} catch (CloneNotSupportedException e) {}
		return res;
	}
}
