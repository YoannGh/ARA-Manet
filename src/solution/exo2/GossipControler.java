package solution.exo2;

import java.util.ArrayList;
import java.util.List;

import manet.algorithm.gossip.GossipProtocol;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import solution.exo1.DensityControler;

public class GossipControler implements Control{

	private static final String PAR_GOSSIPPID ="gossipprotocol";
	private static final String PAR_EMITTER ="emitter";
	private static final String PAR_N ="N";

	private int gossip_pid;
	private int emitter_pid;
	private int N;

	private int currBroadcast = 0;
	private int totalAtt = 0;
	private int totalER = 0;
	private int currRecv = 0;
	private int currSend = 0;
	private List<Double> att_trace;
	private List<Double> er_trace;

	public GossipControler(String prefix) {
		this.N = Configuration.getInt(prefix+"."+PAR_N);
		this.emitter_pid = Configuration.getPid(prefix+"."+PAR_EMITTER);
		this.gossip_pid = Configuration.getPid(prefix+"."+PAR_GOSSIPPID);
		att_trace = new ArrayList<>();
		er_trace = new ArrayList<>();
	}

	public void processAtt() {
		double att = 0;
		att = currRecv/Network.size()*100;
		totalAtt+= att;
		att_trace.add(att);
	}

	public void processER() {
		double res;
		double recv = currRecv;
		double send = currSend;
		res = (recv - send)/ recv*100;
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
		if(broadcastDone()) {
			if(currBroadcast > 0) {
				processAtt();
				processER();
			}
			if(currBroadcast < N) {
				Node initialNode = Network.get(CommonState.r.nextInt(Network.size()));
				GossipProtocol gp = (GossipProtocol) initialNode.getProtocol(gossip_pid);
				System.out.println("initializing gossip#" + currBroadcast + " from node " + initialNode.getID());
				gp.initiateGossip(initialNode, currBroadcast, initialNode.getID());
				currBroadcast+=1;
				return false;
			}
			else if(currBroadcast == N) {
				System.out.println("Avgatt = " + totalAtt/att_trace.size()+ "%");
				System.out.println("Avger = " + totalER/er_trace.size()+ "%");
				System.out.println("Eatt = " + standardVariationAtt());
				System.out.println("Eer = " + standardVariationER());
				System.out.println("Done");
				System.exit(1);
			}
		}

		return false;
	}

	//Broadcast is done when the sum of all recv equals the sum of all send - 1 (initiator node)
	private boolean broadcastDone() {
		int totalrecv = 0;
		int totalsend = 0;
		currRecv = currSend = 0;
		for(int i = 0 ; i< Network.size() ; i++){
			Node n = Network.get(i);
			EmitterGossip eg = (EmitterGossip) n.getProtocol(emitter_pid);
			totalrecv += eg.getRecv();
			totalsend += eg.getSend();
			currRecv += (eg.getRecv() == 0) ? 0: 1;
			currSend += (eg.getSend() == 0) ? 0: 1;
		}
		return (totalsend == 0) || (totalsend+1 == totalrecv);
	}

	public Object clone(){
		GossipControler res=null;
		try {
			res = (GossipControler) super.clone();
			res.gossip_pid = gossip_pid;
			res.emitter_pid = emitter_pid;
			res.N = N;
			res.currBroadcast = currBroadcast;
			res.totalAtt = totalAtt;
			res.totalER = totalER;
			res.currRecv = currRecv;
			res.currSend = currSend;
			res.att_trace = new ArrayList<>(att_trace);
			res.er_trace = new ArrayList<>(er_trace);
		} catch (CloneNotSupportedException e) {}
		return res;
	}

}
