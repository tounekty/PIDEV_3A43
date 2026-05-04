package com.mindcare.model;

import java.time.LocalDateTime;

public class PatientFile {
    private Integer id;
    private String traitementsEnCours;
    private String allergies;
    private String contactUrgenceNom;
    private String contactUrgenceTel;
    private String antecedentsPersonnels;
    private String antecedentsFamiliaux;
    private String motifConsultation;
    private String objectifsTherapeutiques;
    private String notesGenerales;
    private String niveauRisque;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer studentId;

    public PatientFile() {}

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTraitementsEnCours() {
        return traitementsEnCours != null ? traitementsEnCours : "[Vide]";
    }

    public void setTraitementsEnCours(String traitementsEnCours) {
        this.traitementsEnCours = traitementsEnCours;
    }

    public String getAllergies() {
        return allergies != null ? allergies : "[Vide]";
    }

    public void setAllergies(String allergies) {
        this.allergies = allergies;
    }

    public String getContactUrgenceNom() {
        return contactUrgenceNom != null ? contactUrgenceNom : "[Vide]";
    }

    public void setContactUrgenceNom(String contactUrgenceNom) {
        this.contactUrgenceNom = contactUrgenceNom;
    }

    public String getContactUrgenceTel() {
        return contactUrgenceTel != null ? contactUrgenceTel : "[Vide]";
    }

    public void setContactUrgenceTel(String contactUrgenceTel) {
        this.contactUrgenceTel = contactUrgenceTel;
    }

    public String getAntecedentsPersonnels() {
        return antecedentsPersonnels != null ? antecedentsPersonnels : "[Vide]";
    }

    public void setAntecedentsPersonnels(String antecedentsPersonnels) {
        this.antecedentsPersonnels = antecedentsPersonnels;
    }

    public String getAntecedentsFamiliaux() {
        return antecedentsFamiliaux != null ? antecedentsFamiliaux : "[Vide]";
    }

    public void setAntecedentsFamiliaux(String antecedentsFamiliaux) {
        this.antecedentsFamiliaux = antecedentsFamiliaux;
    }

    public String getMotifConsultation() {
        return motifConsultation != null ? motifConsultation : "[Vide]";
    }

    public void setMotifConsultation(String motifConsultation) {
        this.motifConsultation = motifConsultation;
    }

    public String getObjectifsTherapeutiques() {
        return objectifsTherapeutiques != null ? objectifsTherapeutiques : "[Vide]";
    }

    public void setObjectifsTherapeutiques(String objectifsTherapeutiques) {
        this.objectifsTherapeutiques = objectifsTherapeutiques;
    }

    public String getNotesGenerales() {
        return notesGenerales != null ? notesGenerales : "[Vide]";
    }

    public void setNotesGenerales(String notesGenerales) {
        this.notesGenerales = notesGenerales;
    }

    public String getNiveauRisque() {
        return niveauRisque != null ? niveauRisque : "[Vide]";
    }

    public void setNiveauRisque(String niveauRisque) {
        this.niveauRisque = niveauRisque;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }
}
