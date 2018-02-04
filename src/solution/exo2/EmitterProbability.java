package solution.exo2;

import manet.Message;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Node;

public class EmitterProbability extends EmitterGossip{

	private static final String PAR_PROBABILITY = "probability";

	private double probability;

	public EmitterProbability(String prefix) {
		super(prefix);
		this.probability = Configuration.getDouble(prefix + "." + PAR_PROBABILITY);
	}

	//Probability algorithm send in pseudorandomness according to probability parameter
	public void emit(Node host, Message msg) {
		boolean canSend = CommonState.r.nextDouble() < probability;
		super.emit(host, msg, canSend);
	}

	public Object clone() {
		EmitterProbability res = null;
		res = (EmitterProbability) super.clone();
		res.probability = probability;
		return res;
	}
}
