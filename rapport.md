# Exercice 1

## Question 1

```
Initialement le noeud n'est pas en mouvement.

move:
Si le noeud n'est pas en mouvement:
  - calcul aléatoire de la vitesse du noeud
  - calcul de sa prochaine destination en se basant sur la stratégie de destination

Calcul de la distance courante restante à parcourir
Calcul de la distance totale à parcourir par le noeud jusqu'à sa prochaine destination.

Si le noeud est arrivé à destination:
	il s'arrête et s'envoit un Event de type pause qui va relancer l'algorithme
	move après que le temps 'pause' soit écoulé.
Sinon:
	Le noeud se déplace vers sa destination et s'envoit un Event pour déclencher son prochain déplacement.
```

## Question 2

```
random.seed 6
network.size 42
simulation.endtime 200000

protocol.positionprotocolimpl PositionProtocolImpl
protocol.positionprotocolimpl.maxspeed 100
protocol.positionprotocolimpl.minspeed 10
protocol.positionprotocolimpl.width 1000
protocol.positionprotocolimpl.height 1000
protocol.positionprotocolimpl.pause 3000

initial_position_strategy Strategy1InitNext
initial_position_strategy.positionprotocol positionprotocolimpl
next_destination_strategy Strategy1InitNext
next_destination_strategy.positionprotocol positionprotocolimpl

control.graphicalmonitor GraphicalMonitor
control.graphicalmonitor.positionprotocol positionprotocolimpl
control.graphicalmonitor.time_slow 0.0002
control.graphicalmonitor.step 1

init.init_module Initialisation
```

## Question 3
Le SD définit une position pseudo aléatoire comprise entre les paramètres width et height définis dans le fichier de config.
La SPI de la Stratégie 1 appelle simplement le SD avec une vitesse de zéro.


## Question 4

Le SD de la stratégie 2 retourne la position actuelle du noeud. Les noeuds sont donc statiques.

```java
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
```