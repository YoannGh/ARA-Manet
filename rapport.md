# Exercice 1

## Question 1

Initialement le noeud n'est pas en mouvement.

move:
Si le noeud n'est pas en mouvement:
	calcul aléatoire de la vitesse du noeud
	calcul de sa prochaine destination en se basant sur la stratégie de destination

Calcul de la distance courante restante à parcourir
Calcul de la distance totale à parcourir par le noeud jusqu'à sa prochaine destination.

Si le noeud est arrivé à destination:
	il s'arrête et s'envoit un Event de type pause qui va relancer l'algorithme
	move après que le temps 'pause' soit écoulé.
Sinon:
	Le noeud se déplace vers sa destination et s'envoit un Event pour déclencher son prochain déplacement.

