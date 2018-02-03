package solution.exo2;

import manet.Message;
import peersim.core.Node;

public class EmitterFlooding extends EmitterGossip{

	public EmitterFlooding(String prefix) {
		super(prefix);
	}

	//Flooding algorithm always send if he can
	public void emit(Node host, Message msg) {
		super.emit(host, msg, true);
	}

	public Object clone() {
		return super.clone();
	}
}
