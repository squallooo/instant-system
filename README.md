# API de parkings — Test technique Instant System

Application serveur réalisée dans le cadre du processus d'évaluation d’Instant System. Elle expose une API REST permettant de consulter les parkings d’une ville et de rechercher ceux situés à proximité d’un point.

L’objectif principal est de conserver le même contrat d’API pour le client mobile ou Web, même lorsque les villes utilisent des sources de données et des formats différents. Aucune interface mobile ou Web n’est développée ; Swagger UI permet de découvrir et de tester l’API.

**Temps consacré à l’exercice : 22 heures.**

## Périmètre réalisé

| Fonctionnalité | Poitiers | Nantes |
| --- | --- | --- |
| Liste des parkings | Oui | Oui, avec pagination de la source |
| Détail par identifiant | Oui | Oui |
| Capacité et coordonnées | Oui | Oui, selon les données disponibles |
| Nombre de places disponibles | Fourni par la source temps réel | Non fourni par le jeu de données utilisé |
| Recherche par proximité | Déléguée à l’API Poitiers | Filtrage local par distance, résultats triés du plus proche au plus éloigné |

`Nantes est une seconde intégration destinée à vérifier que l’architecture accepte un autre format de données sans modifier les routes exposées. L’inscription, la connexion JWT et la déconnexion sont des compléments au besoin initial.`

Sources utilisées :

- [Grand Poitiers — parkings en temps réel](https://data.grandpoitiers.fr/data-fair/api/v1/datasets/mobilites-stationnement-des-parkings-en-temps-reel/lines).
- [Nantes Métropole — parkings publics](https://data.nantesmetropole.fr/explore/dataset/244400404_parkings-publics-nantes/api/).

## Démarrage

### Prérequis

- JDK 25, conformément au `pom.xml` ; vérifier `JAVA_HOME` et `java -version`.
- Une connexion Internet pour télécharger les dépendances et interroger les API des villes.
- Le wrapper Maven est fourni. Un Maven installé localement peut également être utilisé.

Depuis la racine du projet, sous Linux ou macOS :

```sh
./mvnw spring-boot:run
```

Sous Windows PowerShell :

```powershell
.\mvnw.cmd spring-boot:run
```

L’application écoute par défaut sur `http://localhost:8080`.

- [Swagger UI](http://localhost:8080/swagger-ui/index.html) : documentation interactive et essais.
- [OpenAPI JSON](http://localhost:8080/v3/api-docs) : description de l’API.
- [Vérification simple du serveur](http://localhost:8080/api/hello) : accessible sans authentification ; ne vérifie pas les API externes.

Après un changement de dépendance Maven, arrêter puis relancer complètement l’application. Le redémarrage automatique DevTools ne suffit pas toujours à charger les nouvelles bibliothèques.

### Configuration locale

Les paramètres se trouvent dans `src/main/resources/application.properties`.

| Paramètre | Configuration actuelle |
| --- | --- |
| Base de données | H2 en mémoire (`jdbc:h2:mem:testdb`) |
| Durée de validité du JWT | `security.jwt.expiration-time=3600000`, soit une heure |
| Clé de signature JWT | `security.jwt.secret-key`, décodée en Base64 par le service |
| Journalisation applicative | Niveau `INFO` |
| url api Poitiers | `https://data.grandpoitiers.fr/data-fair/api/v1/datasets/mobilites-stationnement-des-parkings-en-temps-reel`
| url api Nantes | `https://data.nantesmetropole.fr/api/explore/v2.1/catalog/datasets/244400404_parkings-publics-nantes`

Aucune base externe n’est nécessaire. Les comptes disparaissent à l’arrêt de l’application. La clé JWT présente dans le fichier est une configuration de démonstration : pour un déploiement, la remplacer et la fournir par variable d’environnement (`SECURITY_JWT_SECRET_KEY`) ou gestionnaire de secrets. Ne pas réutiliser la clé du dépôt.

## Essayer l’API

### 1. Créer un compte

Depuis Postman une collection est incluses dans le projet sous src/main/resources/static/instant system parkings.postman_collection.json
(elle sert aussi pour les testes de non regression)

Depuis Swagger ou Postman, appeler `POST /api/auth/signup` avec un corps JSON :

```json
{
  "email": "demo@example.com",
  "name": "Demo",
  "lastName": "Test",
  "pwd": "Demo-password-123!"
}
```

### 2. Se connecter

Appeler `POST /api/auth/login` :

```json
{
  "email": "demo@example.com",
  "pwd": "Demo-password-123!"
}
```

Récupérer le champ `token` de la réponse. Le champ `expiresInMs` représente la durée de validité en millisecondes (par exemple `3600000` pour une heure), et non une date absolue. Le champ `role` vaut actuellement `USER`, sans gestion de permissions par rôle.

Dans Swagger, cliquer sur **Authorize**, puis coller uniquement le token, sans le préfixe `Bearer`. Dans Postman, choisir **Bearer Token**. Les appels protégés doivent contenir l’en-tête `Authorization: Bearer <token>`.

### 3. Consulter les parkings

| Méthode | Route | Usage |
| --- | --- | --- |
| GET | `/api/available-cities` | Villes prises en charge |
| GET | `/api/{city}/parkings` | Liste des parkings |
| GET | `/api/{city}/{parking_id}` | Détail d’un parking |
| GET | `/api/{city}/find_nearby_parkings?geoPoint=latitude,longitude&radiusMeters=500` | Recherche autour d’un point, rayon en mètres |
| GET | `/api/user/users` | Liste des utilisateurs, sans mot de passe |
| DELETE | `/api/auth/logout` | Révocation du token envoyé |

Exemples :

- `/api/Poitiers/parkings`
- `/api/Poitiers/find_nearby_parkings?geoPoint=46.58,0.34&radiusMeters=500`
- `/api/Nantes/parkings`

Le nom de ville est insensible à la casse. Pour consulter un détail, utiliser un identifiant obtenu dans la liste. Le paramètre `radiusMeters` indique le rayon en mètres.

Après le logout, la réutilisation du token révoqué renvoie **401**. Une nouvelle connexion fournit un nouveau token.

## Architecture et choix techniques

### Un adaptateur par source de données

Le parcours d’une requête est le suivant :

```text
ApiController → ParkingService → ParkingClientFactory
                                      └─ IParkingApiClient
                                           ├─ PoitierApiClient
                                           └─ NantesApiClient
```

- `ApiController` expose les routes REST et les réponses HTTP.
- `ParkingService` coordonne les opérations et choisit le comportement de recherche à proximité.
- `ParkingClientFactory` construit un registre à partir des implémentations injectées par Spring et sélectionne le client associé à la ville.
- `IParkingApiClient` définit le contrat commun des adaptateurs.
- Chaque client convertit les DTO propres à sa source vers `ParkingDto`, le format exposé aux consommateurs de l’API.

Cette séparation isole les particularités des fournisseurs : URL, noms de champs, pagination et capacités de recherche. Ajouter une ville consiste à créer un composant implémentant `IParkingApiClient`, ses DTO de lecture et ses tests. Le contrôleur et les routes n’ont pas besoin de changer. L’ajout

Les clients utilisent le `RestClient` configuré par Spring. Ils conservent ainsi `RestClientLoggingInterceptor`, qui journalise les requêtes sortantes et leurs statuts HTTP. Il intègre aussi un custmizer qui permet de régler les durées de lectures et de Timeout

### Authentification et Sécurité
L'application supporte la `création de compte utilisateur` et le `login`
Une authentification basé sur `token JWT`est utilisée pour sécuriser les endpoints

### Recherche à proximité

Lorsque la source le permet, le filtrage géographique lui est délégué. Pour Poitiers, l’adaptateur transforme le point reçu en `latitude,longitude` vers le format `longitude:latitude:rayon` attendu par le paramètre `geo_distance`.

Pour les sources sans recherche géographique, comme Nantes, le service charge les parkings et calcule leur distance à vol d’oiseau avec la formule de Haversine. Il conserve ceux situés dans le rayon demandé, limite incluse, puis les trie par distance croissante. Les parkings dont les coordonnées sont absentes ou invalides sont ignorés pour cette recherche.

Avant tout appel externe, les coordonnées doivent contenir deux nombres finis : une latitude entre -90 et 90 et une longitude entre -180 et 180. Le rayon doit être strictement positif, en mètres. Une requête invalide renvoie 400 ; elle n’est plus remplacée silencieusement par un point à zéro.

Les informations non disponibles restent à `null` : adresse, statut, horaires et prix pour Poitiers ; disponibilité en temps réel, statut, horaires et prix pour Nantes. La capacité et les places libres fournies par Poitiers restent inchangées, y compris lorsqu’elles valent zéro.

### Cache
J'ai choisi (pour illuster le point) d'utiliser un cache spring avec Caffeine pour cacher l'appel des parkings d'une ville et choisis 30s comme durée (qui correspond à celle de Poitiers)

### Compte et utilisateurs

`IUserService` sépare le contrat de gestion des utilisateurs de son implémentation. Cette séparation permettrait de remplacer le stockage ou le mécanisme d’identité sans dépendre d’une architecture supposée d’Instant System.
Je me contente d'une gestion simple des utilisateurs avec une creation (compte qui permet d'avoir un token) pour pouvoir utiliser l'api cela simule aussi une app mobile ou tout autre moyen consommateur de l'api

### Technologies

- **Java 25 et Spring Boot 4.1.1** : API REST, injection de dépendances et configuration de l’application.
- **RestClient** : appels HTTP synchrones, adaptés à la simplicité de l’exercice.
- **Spring Data JPA et H2** : stockage local des utilisateurs sans installation supplémentaire.
- **Spring Security, BCrypt et JJWT** : hachage des mots de passe et authentification Bearer.
- **springdoc-openapi** : documentation générée à partir des contrôleurs et accès Swagger.
- **Caffeine** : high-performance cache library pour Java
- **JUnit et Mockito** : tests isolés des services et des contrôleurs.



### Authentification et erreurs

Le mécanisme JWT reprend un principe rencontré dans un précédent contexte professionnel, où je l’utilisais côté consommateur. Dans cet exercice, il est implémenté dans `JwtService`, `JwtAuthenticationFilter` et `AppConfiguration`.

Les mots de passe sont hachés avec BCrypt à l’inscription. La connexion passe par `AuthenticationManager`. Les requêtes utilisent une configuration sans session serveur ; CSRF est désactivé pour cette API utilisant le token dans l’en-tête, sans authentification par cookie.

Le logout ajoute le token à une liste de révocation en mémoire jusqu’à son expiration. Ce choix simplifie la démonstration, mais ne convient pas tel quel à plusieurs instances.

`ControllerExceptionHandler` centralise une partie des erreurs : identifiants incorrects (401), requêtes mal formées (400), parking introuvable (404), email déjà utilisé (409), ainsi qu’une réponse générique pour les erreurs internes (500). La contrainte unique `uk_users_email` protège les inscriptions, y compris simultanées. Sa violation est traduite en `EmailAlreadyInUseException` ; les autres erreurs d’intégrité ne sont pas présentées comme des doublons d’email. La normalisation des emails (espaces et casse) reste à définir.

## Tests

Exécuter la suite :

```sh
./mvnw test
```

Sous Windows : `.\mvnw.cmd test` ; avec Maven installé : `mvn test`.

Les tests couvrent notamment :

- Le choix du client par ville et les cas de ville inconnue.
- Les services de parkings, la recherche déléguée, le filtrage local par distance et la validation des coordonnées et du rayon.
- La conversion des réponses Poitiers et Nantes, les résultats vides, les identifiants inconnus, les erreurs externes et la pagination Nantes.
- Les réponses des contrôleurs et plusieurs traductions d’exceptions en statuts HTTP via MockMvc.
- Le hachage des mots de passe, les connexions acceptées ou refusées, la validation et la révocation des JWT.
- Les utilitaires géographiques et le chargement du contexte Spring.

Les API externes sont simulées avec `MockRestServiceServer` : ces tests ne dépendent pas de la disponibilité des plateformes municipales. Ils servent également de base de non-régression. Le test de contexte vérifie le démarrage des composants, mais ne remplace pas un parcours HTTP complet avec la chaîne de sécurité.

Les essais sur les sources réelles et via Swagger restent manuels. Des tests de contrat avec les fournisseurs et des tests de bout en bout inscription → connexion → appel protégé → logout compléteraient cette couverture.

### Test de non regression 
une collection est incluses dans le projet sous src/main/resources/static/instant system parkings.postman_collection.json
Elle sert pour les testes de non regression soit en faisant un run collection depuis l'application postman ou un lancement par ligne de commande en utilisant newman (il faut installer la dependance)

## Limites connues et suites envisagées

### Fonctionnalités et qualité des données

- Harmoniser les unités du calcul de boîte englobante avant une éventuelle utilisation : cet utilitaire n’intervient pas dans le filtrage local actuel, qui utilise Haversine en mètres. 
- Le détail Nantes recherche actuellement dans la liste complète : acceptable pour ce petit catalogue, à remplacer par une requête filtrée si le volume augmente. (en plus l'api est choisi pour avoir un cas où les parking dans un rayon ne sont pas proposés et doivent être calculé manuellement)
- Avoir un mecanisme/middleware qui permet d'harmoniser les données (e.g l'adresse du parking peut être fournis par un api d'une ville et non pas par une autre / utiliser les points gps fournis pour la remplir par exemple).

### Robustesse et sécurité

- Configurer explicitement les délais des appels externes et distinguer indisponibilité, timeout et absence de résultat + monitoring avec selftest ou equivalent pour suivre l'état de tout les fournisseur et anticiper les . (pour plus de résilientce)
- Compléter les droits d'accès "Role" des Users et la validation des données selon la stratégie de Instant System
- Remplacer H2 en mémoire par une base persistante avec migrations, externaliser les secrets et déployer derrière HTTPS.
- Persister ou partager les révocations de tokens. Elles disparaissent au redémarrage et ne sont pas communes à plusieurs instances ; un JWT non expiré peut redevenir utilisable si le compte associé existe encore ou est recréé. Normalement un serveur central s'occupe de l'authentification et la gestion des token (mais ce n'ai pas le sujet de l'exercice)

### Évolution du projet

- Séparer les contrôleurs des parkings et de l’authentification.
- Aller plus loin dans la gestion du cache pour une vraie application
- Compléter l’observabilité : durée et taux d’erreur des appels fournisseurs, corrélation des logs et supervision. Monitoring complet
- Automatiser compilation et tests en intégration continue, puis ajouter des tests de charge et de résilience avant un déploiement réel.

L’architecture actuelle permet ces evolutions anisi que La capacité à absorber une forte charge et à fonctionner sur plusieurs instances.


## Utilisation de l'IA et Temps de Développement 

J'ai fait le choix d'utiliser l'IA de manière ponctuelle, comme un outil d'assistance et de productivité, principalement pour des tâches secondaires et non déterminantes.
L'essentiel du projet a été conçu et développé de manière autonome. Les choix techniques, la conception de l'architecture, les design patterns et l'implémentation de la logique métier résultent de ma propre réflexion.
L'IA a été utilisée pour accompagner le développement, et non pour orienter ou prendre les décisions techniques et architecturales.
Cette approche m'a permis de bénéficier des gains de productivité offerts par l'IA tout en conservant une maîtrise complète du projet.

### Temps de Développement 

Le projet a été réalisé en environ 22 heures de travail effectif.
Le temps de développement a principalement été consacré à la conception d'une architecture modulaire et extensible, à l'intégration et à la normalisation des API externes, ainsi qu'à l'implémentation de la logique métier.
Une attention particulière a également été portée à la sécurisation des endpoints, à l'optimisation des performances via le cache et à la documentation de l'API.


