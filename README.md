# A la découverte de gRPC glmf

Ce projet contient les sources de l'article "A la découverte de gRPC" pour GNU Linux Magazine.

Pour compiler et lancer la démo, il faut au préalable installer Java 11 et maven 3.6.0 ou supérieur.
Les exemples fonctionneraient avec une version de Java 8 mais pour c'est aussi l'occasion de voir si ça marche avec la dernière version de Java.

Le projet contient 2 exemples : 
## Helloworld
Cet exemple permet de faire un serveur simple qui réponds au client pour l'appel RPC 'bonjour'.
C'est un cas d'usage nominal et simple qui représente ce qui se fait classiquement comme appel gRPC en mode synchrone question/réponse (unidirectionnel).

Pour lancer l'exemple, une fois compilé via un `mvn compile` il faut, dans 2 terminaux différents :
 
Lancer le serveur : 
```
mvn exec:java@run-helloserveur
```
Lancer le client : 
```
mvn exec:java@run-helloclient
```
 

## PingPong
Cet exemple permet de faire une partie de ping pong multi balle entre un serveur (pong) et un client (ping).
On peut choisir d'utiliser autant de balles qu'on veut.

La communication sera bidirectionnelle et asynchrone (mot clé stream dans le pingpong.proto).

Pour le lancement, une fois compilé, il faut ouvrir 2 terminaux puis :

Lancer le serveur : 
```
mvn exec:java@run-pongserveur
```
Lancer le client :
``` 
mvn exec:java@run-pingclient
```
