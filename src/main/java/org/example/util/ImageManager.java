package org.example.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Gestionnaire pour les images avec ID unique et stockage local
 */
public class ImageManager {
    private static final String IMAGES_FOLDER = "images_uploaded";
    private static final String SEPARATOR = "_";
    
    static {
        // Créer le dossier des images au démarrage
        createImagesFolder();
    }
    
    /**
     * Créer le dossier des images s'il n'existe pas
     */
    public static void createImagesFolder() {
        try {
            Path folderPath = Paths.get(IMAGES_FOLDER);
            if (!Files.exists(folderPath)) {
                Files.createDirectories(folderPath);
                System.out.println("✓ Dossier images créé: " + folderPath.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Erreur création dossier images: " + e.getMessage());
        }
    }
    
    /**
     * Générer un ID unique pour une image
     * Format: IMG_[timestamp]_[uuid]
     */
    public static String generateImageId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return "IMG" + SEPARATOR + timestamp + SEPARATOR + uuid;
    }
    
    /**
     * Copier une image dans le dossier du projet et retourner son chemin
     * @param sourceFile Fichier source sélectionné
     * @return ImageInfo avec ID, chemin et nom du fichier
     */
    public static ImageInfo uploadImage(File sourceFile) throws IOException {
        if (sourceFile == null || !sourceFile.exists()) {
            throw new IOException("Fichier source invalide");
        }
        
        // Extraire l'extension
        String originalName = sourceFile.getName();
        String extension = getFileExtension(originalName);
        
        // Générer ID unique
        String imageId = generateImageId();
        
        // Créer le nom du fichier: ID.extension
        String fileName = imageId + "." + extension;
        
        // Chemin du fichier destination
        Path destinationPath = Paths.get(IMAGES_FOLDER, fileName);
        
        // Copier le fichier
        Files.copy(sourceFile.toPath(), destinationPath);
        
        System.out.println("✓ Image uploadée: " + fileName);
        
        return new ImageInfo(imageId, destinationPath.toString(), fileName);
    }
    
    /**
     * Obtenir l'extension d'un fichier
     */
    public static String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf(".");
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return "jpg"; // Par défaut
    }
    
    /**
     * Vérifier si le chemin de l'image existe
     */
    public static boolean imageExists(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return false;
        }
        File file = new File(imagePath);
        return file.exists();
    }
    
    /**
     * Supprimer une image du dossier
     */
    public static void deleteImage(String imagePath) {
        try {
            if (imagePath != null && !imagePath.isBlank()) {
                File file = new File(imagePath);
                if (file.exists()) {
                    Files.delete(file.toPath());
                    System.out.println("✓ Image supprimée: " + imagePath);
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur suppression image: " + e.getMessage());
        }
    }
    
    /**
     * Classe pour retourner les infos de l'image uploadée
     */
    public static class ImageInfo {
        public final String id;          // ID unique
        public final String path;        // Chemin complet du fichier
        public final String fileName;    // Nom du fichier
        
        public ImageInfo(String id, String path, String fileName) {
            this.id = id;
            this.path = path;
            this.fileName = fileName;
        }
        
        @Override
        public String toString() {
            return "ID: " + id + " | Path: " + path;
        }
    }
}
