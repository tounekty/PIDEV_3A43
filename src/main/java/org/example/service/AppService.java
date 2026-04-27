package org.example.service;

import org.example.model.Journal;
import org.example.model.Mood;
import org.example.repository.UserRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.example.config.AppConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Centralized application service managing all business logic.
 */
public class AppService {
    private static final AppService INSTANCE = new AppService();
    
    private final MoodService moodService = new MoodService();
    private final JournalService journalService = new JournalService();
    private final AlertService alertService = new AlertService();
    private final PreventiveWellnessAssistantService wellnessAssistantService = new PreventiveWellnessAssistantService();
    private final HuggingFaceJournalAnalysisService huggingFaceJournalAnalysisService = new HuggingFaceJournalAnalysisService();
    private final ZenQuotesService zenQuotesService = new ZenQuotesService();
    private final LibreTranslateService libreTranslateService = new LibreTranslateService();
    private final WiktionaryEmotionService wiktionaryEmotionService = new WiktionaryEmotionService();
    private final EmailApiService emailApiService = new EmailApiService();
    private final UserRepository userRepository = new UserRepository();

    private AppService() {
    }

    public static AppService getInstance() {
        return INSTANCE;
    }

    // Mood operations
    public Mood createMood(String moodType, LocalDate moodDate, String note, String sleepTime, String wakeTime, Double sleepHours) throws ServiceException {
        Mood mood = moodService.createMood(moodType, moodDate, note, sleepTime, wakeTime, sleepHours);
        sendAdminMoodEmailIfNeeded(mood);
        return mood;
    }

    public Mood createMood(String moodType, LocalDate moodDate, String note, Integer stressLevel, Integer energyLevel) throws ServiceException {
        Mood mood = moodService.createMood(moodType, moodDate, note, stressLevel, energyLevel);
        sendAdminMoodEmailIfNeeded(mood);
        return mood;
    }

    public Mood createMood(String moodType, LocalDate moodDate, String note, Integer stressLevel, Integer energyLevel,
                           Integer userId) throws ServiceException {
        Mood mood = moodService.createMood(moodType, moodDate, note, stressLevel, energyLevel, userId);
        sendAdminMoodEmailIfNeeded(mood);
        return mood;
    }

    public boolean saveMoodAdminComment(int id, String adminComment) throws ServiceException {
        Mood mood = getMoodById(id);
        if (mood == null) {
            throw new ServiceException("Mood not found.");
        }
        return moodService.updateMood(id, mood.getMoodType(), mood.getMoodDate(), mood.getNote(), mood.getStressLevel(), mood.getEnergyLevel(),
                mood.getSleepTime(), mood.getWakeTime(), mood.getSleepHours(), adminComment, mood.isSupportEmailSent());
    }

    public boolean updateMood(int id, String moodType, LocalDate moodDate, String note, String sleepTime, String wakeTime, Double sleepHours) throws ServiceException {
        Mood existing = getMoodById(id);
        boolean supportEmailSent = existing != null && existing.isSupportEmailSent();
        return moodService.updateMood(id, moodType, moodDate, note, null, null, sleepTime, wakeTime, sleepHours, null, supportEmailSent);
    }

    public boolean updateMood(int id, String moodType, LocalDate moodDate, String note, Integer stressLevel, Integer energyLevel) throws ServiceException {
        Mood existing = getMoodById(id);
        boolean supportEmailSent = existing != null && existing.isSupportEmailSent();
        String adminComment = existing == null ? null : existing.getAdminComment();
        return moodService.updateMood(id, moodType, moodDate, note, stressLevel, energyLevel, null, null, null, adminComment, supportEmailSent);
    }

    public boolean deleteMood(int id) throws ServiceException {
        return moodService.deleteMood(id);
    }

    public List<Mood> getAllMoods() throws ServiceException {
        return moodService.getAllMoods();
    }

    public Mood getMoodById(int id) throws ServiceException {
        return moodService.getMoodById(id);
    }

    public List<Mood> getMoodsByDate(LocalDate date) throws ServiceException {
        return moodService.getMoodsByDate(date);
    }

    public MoodStatistics getMoodStatistics() throws ServiceException {
        return moodService.getMoodStatistics();
    }

    public EmailApiService.EmailResult sendSupportEmailToStudent(Mood mood, String adminMessage) throws ServiceException {
        return sendSupportEmailToStudent(mood, adminMessage, null);
    }

    public EmailApiService.EmailResult sendSupportEmailToStudent(Mood mood, String adminMessage, Integer excludedUserId) throws ServiceException {
        if (mood == null) {
            throw new ServiceException("Select a mood first.");
        }
        String studentEmail = resolveStudentEmail(mood, excludedUserId);
        EmailApiService.EmailResult result = emailApiService.sendSupportEmailToStudent(mood, adminMessage, studentEmail);
        if (result.sent()) {
            moodService.markSupportEmailSent(mood.getId());
        }
        return result;
    }

    // Journal operations
    public Journal createJournal(String title, String content, LocalDate entryDate, Integer moodId) throws ServiceException {
        return journalService.createJournal(title, content, entryDate, moodId);
    }

    public boolean saveJournalAdminComment(int id, String adminComment) throws ServiceException {
        Journal journal = getJournalById(id);
        if (journal == null) {
            throw new ServiceException("Journal not found.");
        }
        return journalService.updateJournal(
                id,
                journal.getTitle(),
                journal.getContent(),
                journal.getEntryDate(),
                journal.getMoodId(),
                adminComment
        );
    }

    public boolean updateJournal(int id, String title, String content, LocalDate entryDate, Integer moodId) throws ServiceException {
        return journalService.updateJournal(id, title, content, entryDate, moodId);
    }

    public boolean deleteJournal(int id) throws ServiceException {
        return journalService.deleteJournal(id);
    }

    public List<Journal> getAllJournals() throws ServiceException {
        return journalService.getAllJournals();
    }

    public Journal getJournalById(int id) throws ServiceException {
        return journalService.getJournalById(id);
    }

    public List<Journal> getJournalsByDate(LocalDate date) throws ServiceException {
        return journalService.getJournalsByDate(date);
    }

    public List<Journal> getJournalsByMoodId(int moodId) throws ServiceException {
        return journalService.getJournalsByMoodId(moodId);
    }

    public HuggingFaceJournalAnalysisService.JournalAiAnalysis analyzeJournalWithHuggingFace(Journal journal) throws ServiceException {
        if (journal == null) {
            throw new ServiceException("Select a journal entry first.");
        }
        return huggingFaceJournalAnalysisService.analyze(journal);
    }

    public PreventiveWellnessAssistantService.WellnessReport generatePreventiveWellnessReport() throws ServiceException {
        try {
            return wellnessAssistantService.analyze(getAllMoods(), getAllJournals(), LocalDate.now());
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("Failed to generate preventive wellness report: " + e.getMessage(), e);
        }
    }

    public ZenQuotesService.Quote getRandomZenQuote() {
        return zenQuotesService.getRandomQuote();
    }

    public ZenQuotesService.Quote getZenQuoteForJournal(Journal journal) throws ServiceException {
        if (journal == null) {
            throw new ServiceException("Select a journal entry first.");
        }
        return zenQuotesService.getQuoteForJournal(journal);
    }

    public List<LibreTranslateService.TranslationResult> translateJournal(Journal journal) throws ServiceException {
        if (journal == null) {
            throw new ServiceException("Select a journal entry first.");
        }
        return libreTranslateService.translateJournal(journal);
    }

    public List<WiktionaryEmotionService.EmotionWordDefinition> explainJournalEmotionWords(Journal journal) throws ServiceException {
        if (journal == null) {
            throw new ServiceException("Select a journal entry first.");
        }
        return wiktionaryEmotionService.explainJournal(journal);
    }

    public long countJournalsThisMonth() throws ServiceException {
        return journalService.countThisMonth();
    }

    private void sendAdminMoodEmailIfNeeded(Mood mood) {
        String category = moodEmailCategory(mood == null ? null : mood.getMoodType());
        if (category != null) {
            EmailApiService.EmailResult result = emailApiService.sendMoodAlertToAdmin(mood, category);
            if (result.skipped()) {
                try {
                    String adminEmail = userRepository.findFirstAdminEmail();
                    if (adminEmail != null && !adminEmail.isBlank()) {
                        emailApiService.sendEmailToAdmin(mood, category, adminEmail);
                    }
                } catch (Exception ignored) {
                    // Mood creation should not fail just because optional email alerts are unavailable.
                }
            }
        }
    }

    private String resolveStudentEmail(Mood mood, Integer excludedUserId) throws ServiceException {
        String configuredEmail = firstEnv("MAIL_STUDENT_TO", "STUDENT_EMAIL");
        if (configuredEmail != null) {
            return configuredEmail;
        }
        try {
            String dbEmail = userRepository.findEmailForMoodId(mood.getId(), excludedUserId);
            if (dbEmail != null && !dbEmail.isBlank()) {
                return dbEmail;
            }
            throw new ServiceException("No student email found in the database.");
        } catch (java.sql.SQLException e) {
            throw new ServiceException("Could not load student email: " + e.getMessage(), e);
        }
    }

    private String firstEnv(String... names) {
        return AppConfig.first(names);
    }

    private String moodEmailCategory(String moodType) {
        if (moodType == null) {
            return null;
        }
        String lower = moodType.toLowerCase();
        if (containsAny(lower, "sad", "angry", "depressed", "anxious", "stressed", "bad")) {
            return "bad mood";
        }
        if (containsAny(lower, "happy", "joyful", "great", "excellent", "good", "grateful", "excited", "content")) {
            return "good mood";
        }
        return null;
    }

    private boolean containsAny(String value, String... terms) {
        for (String term : terms) {
            if (value.contains(term)) {
                return true;
            }
        }
        return false;
    }

    // Export operations
    public String exportAsCSV() throws ServiceException {
        try {
            StringBuilder csv = new StringBuilder();
            
            // Moods section
            csv.append("=== MOODS ===\n");
            csv.append("ID,Type,Date,Stress Level,Energy Level,Note\n");
            for (Mood mood : getAllMoods()) {
                csv.append(String.format("%d,\"%s\",%s,%s,%s,\"%s\"\n",
                        mood.getId(),
                        mood.getMoodType(),
                        mood.getMoodDate(),
                        mood.getStressLevel() == null ? "" : mood.getStressLevel(),
                        mood.getEnergyLevel() == null ? "" : mood.getEnergyLevel(),
                        mood.getNote() == null ? "" : mood.getNote().replace("\"", "\"\"")));
            }
            
            csv.append("\n=== JOURNALS ===\n");
            csv.append("ID,Title,Entry Date,Mood ID,Content\n");
            for (Journal journal : getAllJournals()) {
                csv.append(String.format("%d,\"%s\",%s,%s,\"%s\"\n",
                        journal.getId(),
                        journal.getTitle().replace("\"", "\"\""),
                        journal.getEntryDate(),
                        journal.getMoodId() == null ? "" : journal.getMoodId(),
                        journal.getContent().replace("\"", "\"\"")));
            }
            
            return csv.toString();
        } catch (ServiceException e) {
            throw new ServiceException("Failed to export data: " + e.getMessage(), e);
        }
    }

    public void saveExportToFile(String filename) throws ServiceException {
        try {
            if (filename == null || filename.trim().isEmpty()) {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
                filename = "mood_journal_backup_" + timestamp + ".csv";
            }
            
            Path filepath = Paths.get(filename);
            String csvContent = exportAsCSV();
            Files.writeString(filepath, csvContent);
        } catch (IOException e) {
            throw new ServiceException("Failed to save export: " + e.getMessage(), e);
        }
    }

    public void savePdfExportToFile(String filename) throws ServiceException {
        try (PDDocument document = new PDDocument()) {
            if (filename == null || filename.trim().isEmpty()) {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
                filename = "mood_journal_backup_" + timestamp + ".pdf";
            }

            ExportPageWriter writer = new ExportPageWriter(document);
            writer.writeHeading("MindCare Backup");
            writer.writeLine("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writer.writeBlankLine();
            writer.writeLine(getBackupSummary().replace("•", "-"));
            writer.writeBlankLine();

            writer.writeHeading("Moods");
            for (Mood mood : getAllMoods()) {
                writer.writeLine(String.format("ID: %d | Type: %s | Date: %s | Stress: %s | Energy: %s",
                        mood.getId(), safe(mood.getMoodType()), String.valueOf(mood.getMoodDate()),
                        formatLevel(mood.getStressLevel()), formatLevel(mood.getEnergyLevel())));
                writer.writeLine("Note: " + safe(mood.getNote()));
                writer.writeLine("Admin Comment: " + safe(mood.getAdminComment()));
                writer.writeBlankLine();
            }

            writer.writeHeading("Journals");
            for (Journal journal : getAllJournals()) {
                writer.writeLine(String.format("ID: %d | Title: %s | Date: %s | Mood ID: %s",
                        journal.getId(),
                        safe(journal.getTitle()),
                        String.valueOf(journal.getEntryDate()),
                        journal.getMoodId() == null ? "-" : String.valueOf(journal.getMoodId())));
                writer.writeLine("Content: " + safe(journal.getContent()));
                writer.writeLine("Admin Comment: " + safe(journal.getAdminComment()));
                writer.writeBlankLine();
            }

            writer.finish();
            document.save(filename);
        } catch (IOException | ServiceException e) {
            throw new ServiceException("Failed to save PDF export: " + e.getMessage(), e);
        }
    }

    public String getBackupSummary() throws ServiceException {
        try {
            long moodCount = getAllMoods().size();
            long journalCount = getAllJournals().size();
            long thisMonth = countJournalsThisMonth();
            
            return String.format("Backup Summary:\n• Total Moods: %d\n• Total Journals: %d\n• Journals This Month: %d",
                    moodCount, journalCount, thisMonth);
        } catch (ServiceException e) {
            throw new ServiceException("Failed to generate summary: " + e.getMessage(), e);
        }
    }

    // Alert operations
    public List<MoodAlert> detectAllAlerts() throws ServiceException {
        return alertService.detectAlerts(getAllMoods());
    }

    public int getAlertCount() throws ServiceException {
        return detectAllAlerts().size();
    }

    public List<MoodAlert> getCriticalAlerts() throws ServiceException {
        return detectAllAlerts().stream()
                .filter(alert -> alert.getSeverity() >= 4)
                .toList();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String formatLevel(Integer level) {
        return level == null ? "-" : String.valueOf(level);
    }

    private static final class ExportPageWriter {
        private static final float PAGE_MARGIN = 50f;
        private static final float START_Y = 760f;
        private static final float BOTTOM_Y = 60f;
        private static final float BODY_FONT_SIZE = 11f;
        private static final float HEADING_FONT_SIZE = 15f;
        private static final float LINE_HEIGHT = 16f;
        private static final float CONTENT_WIDTH = PDRectangle.A4.getWidth() - (PAGE_MARGIN * 2);

        private final PDDocument document;
        private PDPage page;
        private PDPageContentStream stream;
        private float currentY;

        private ExportPageWriter(PDDocument document) throws IOException {
            this.document = document;
            addPage();
        }

        private void writeHeading(String text) throws IOException {
            ensureSpace(LINE_HEIGHT * 2);
            beginText(PDType1Font.HELVETICA_BOLD, HEADING_FONT_SIZE);
            stream.showText(text);
            endText();
            currentY -= LINE_HEIGHT + 6f;
        }

        private void writeLine(String text) throws IOException {
            writeWrappedText(text, PDType1Font.HELVETICA, BODY_FONT_SIZE);
        }

        private void writeBlankLine() {
            currentY -= 8f;
        }

        private void writeWrappedText(String text, PDType1Font font, float fontSize) throws IOException {
            String[] paragraphs = (text == null ? "-" : text).split("\\R", -1);
            for (String paragraph : paragraphs) {
                StringBuilder line = new StringBuilder();
                String[] words = paragraph.split(" ");
                if (words.length == 0) {
                    ensureSpace(LINE_HEIGHT);
                    currentY -= LINE_HEIGHT;
                    continue;
                }
                for (String word : words) {
                    String candidate = line.length() == 0 ? word : line + " " + word;
                    float width = font.getStringWidth(candidate) / 1000 * fontSize;
                    if (width > CONTENT_WIDTH && line.length() > 0) {
                        drawTextLine(line.toString(), font, fontSize);
                        line = new StringBuilder(word);
                    } else {
                        line = new StringBuilder(candidate);
                    }
                }
                drawTextLine(line.toString(), font, fontSize);
            }
        }

        private void drawTextLine(String text, PDType1Font font, float fontSize) throws IOException {
            ensureSpace(LINE_HEIGHT);
            beginText(font, fontSize);
            stream.showText(text);
            endText();
            currentY -= LINE_HEIGHT;
        }

        private void beginText(PDType1Font font, float fontSize) throws IOException {
            stream.beginText();
            stream.setFont(font, fontSize);
            stream.newLineAtOffset(PAGE_MARGIN, currentY);
        }

        private void endText() throws IOException {
            stream.endText();
        }

        private void ensureSpace(float requiredHeight) throws IOException {
            if (currentY - requiredHeight < BOTTOM_Y) {
                addPage();
            }
        }

        private void addPage() throws IOException {
            closeCurrentStream();
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            currentY = START_Y;
        }

        private void finish() throws IOException {
            closeCurrentStream();
        }

        private void closeCurrentStream() throws IOException {
            if (stream != null) {
                stream.close();
                stream = null;
            }
        }
    }
}
