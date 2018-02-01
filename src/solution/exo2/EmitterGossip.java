package solution.exo2;

import solution.exo1.EmitterImpl;

public abstract class EmitterGossip extends EmitterImpl{
	int recvGossip;
	int sendGossip;

	public EmitterGossip(String prefix) {
		super(prefix);
		reset();
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
}