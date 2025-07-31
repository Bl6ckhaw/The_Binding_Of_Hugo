# The Binding of Hugo

Un jeu d’action-aventure inspiré de The Binding of Isaac, développé en Java avec JavaFX.

## 🎮 But du jeu

Explore des salles générées aléatoirement, affronte des ennemis et des boss, collecte des récompenses pour améliorer ton personnage, et tente de survivre le plus longtemps possible !

## 🕹️ Comment jouer

- **Déplacements** : Avancer [Z] ; Reculer [S] ; Gauche [Q] ; Droite [D]
- **Tirer** : Flèches directionnelles (↑ ↓ ← →)
- **Objectif** : Élimine tous les ennemis de chaque salle, récupère des récompenses, et bats le boss final !
- **HUD** : En haut à gauche, tu vois ta vie, tes dégâts, ta vitesse et la taille de tes projectiles.

## 📦 Installation

### 1. Prérequis

- **Java 17 ou supérieur**  
  [Télécharger Java](https://adoptium.net/)
- **JavaFX SDK 21**  
  [Télécharger JavaFX](https://gluonhq.com/products/javafx/)

### 2. Cloner le projet

```powershell
git clone https://github.com/Bl6ckhaw/The_Binding_Of_Hugo.git
cd The_Binding_Of_Hugo
```

### 3. Compiler le projet

Adapte le chemin du JavaFX SDK à ton installation :

```powershell
javac --module-path "C:\Program Files\javafx-sdk-21.0.8\lib" --add-modules javafx.controls,javafx.fxml -d out src\*.java
```

### 4. Créer le JAR

```powershell
jar --create --file TheBindingOfHugo.jar -C out .
```

### 5. Lancer le jeu

```powershell
java --module-path "C:\Program Files\javafx-sdk-21.0.8\lib" --add-modules javafx.controls,javafx.fxml -jar TheBindingOfHugo.jar
```

## 📁 Structure du projet

```
The_Binding_Of_Hugo/
│
├── src/                  # Code source Java
│   ├── GameApp.java
│   ├── UIManager.java
│   ├── Player.java
│   ├── Enemy.java
│   ├── BossEnemy.java
│   ├── ... (autres classes)
│
├── out/                  # Fichiers compilés (.class)
├── dist/                 # Installeur généré par jpackage
├── TheBindingOfHugo.jar  # Archive exécutable du jeu
└── README.md
```

## 🔗 Liens utiles

- [JavaFX Documentation](https://openjfx.io/)
- [Télécharger Java](https://adoptium.net/)
- [Le projet sur GitHub](https://github.com/Bl6ckhaw/The_Binding_Of_Hugo)

---
