# Guide IA Ressource avec Groq

Ce module permet de creer une ressource manuellement ou de generer un brouillon d'article avec Groq.
Groq genere le titre et la description. Nano Banana genere l'image de la ressource selon le contexte.

Il n'y a plus de generateur local. Le bouton `Generer avec Groq` appelle directement l'API Groq.

## Creer une cle Groq

1. Va sur https://console.groq.com/keys
2. Connecte-toi ou cree un compte Groq.
3. Clique sur `Create API Key`.
4. Copie la cle.

## Configuration PowerShell

Avant de lancer l'application, configure les cles :

```powershell
$env:GROQ_API_KEY="ta_cle_groq"
$env:GEMINI_API_KEY="ta_cle_gemini"
```

Le modele par defaut est `llama-3.3-70b-versatile`. Tu peux le changer :

```powershell
$env:GROQ_MODEL="llama-3.3-70b-versatile"
$env:GEMINI_IMAGE_MODEL="gemini-2.5-flash-image"
```

Puis lance l'application :

```powershell
mvn javafx:run
```

## Utilisation

1. Ouvre `Ressources`.
2. Clique sur `Nouvelle Ressource`.
3. Dans `Assistant IA`, ecris ton prompt.
4. Clique sur `Generer avec Groq`.
5. L'application remplit le titre, la description et le lien image.
6. Une confirmation apparait.
7. Clique sur `Publier maintenant` pour enregistrer directement.
8. Clique sur `Modifier avant` si tu veux corriger le brouillon avant publication.

## Exemple de prompt

```text
Cree une ressource avec le titre "Sante mentale des etudiants".
Explique les causes du stress, les signes importants et donne des conseils simples.
Le ton doit etre rassurant, professionnel et facile a comprendre.
```

L'IA ne sauvegarde pas directement sans validation. Elle remplit le formulaire, puis tu choisis si tu veux publier maintenant ou modifier avant.

## Ajout manuel avec image

Quand tu ajoutes une ressource manuellement, tu peux choisir dans `Image de la ressource` :

- `Lien image` : colle directement une URL ou un chemin d'image.
- `Image IA Nano Banana` : ecris un prompt image, puis clique sur `Generer image Nano Banana`.

L'image generee est sauvegardee dans le dossier `generated-images/` du projet, puis le champ `Lien visuel` est rempli automatiquement.
