package com.kgm.ui.util;

import com.kgm.ui.component.UploadCard;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;
import java.util.function.Consumer;

public final class FileDialogHandler {
    private FileDialogHandler() {
    }

    public enum FileType {
        EXCEL("Excel files", "xlsx", "xls"),
        PDF("PDF files", "pdf"),
        IMAGE("Image files", "png", "jpg", "jpeg", "gif"),
        ALL("All files", "*");

        private final String description;
        private final String[] extensions;

        FileType(String description, String... extensions) {
            this.description = description;
            this.extensions = extensions;
        }

        public String getDescription() {
            return description;
        }

        public String[] getExtensions() {
            return extensions;
        }
    }

    public static class FileDialogConfig {
        private Component parent;
        private String dialogTitle;
        private FileType fileType = FileType.ALL;
        private String defaultFileName;
        private boolean selectMultipleFiles;
        private File currentDirectory;
        private String uploadTitle;
        private String uploadDescription;

        public FileDialogConfig withParent(Component parent) {
            this.parent = parent;
            return this;
        }

        public FileDialogConfig withTitle(String title) {
            this.dialogTitle = title;
            return this;
        }

        public FileDialogConfig withFileType(FileType fileType) {
            this.fileType = fileType == null ? FileType.ALL : fileType;
            return this;
        }

        public FileDialogConfig withDefaultFileName(String fileName) {
            this.defaultFileName = fileName;
            return this;
        }

        public FileDialogConfig allowMultipleSelection(boolean allow) {
            this.selectMultipleFiles = allow;
            return this;
        }

        public FileDialogConfig withCurrentDirectory(File directory) {
            this.currentDirectory = directory;
            return this;
        }

        public FileDialogConfig withUploadCard(String title, String description) {
            this.uploadTitle = title;
            this.uploadDescription = description;
            return this;
        }

        public FileDialogConfig copy() {
            FileDialogConfig copy = new FileDialogConfig();
            copy.parent = parent;
            copy.dialogTitle = dialogTitle;
            copy.fileType = fileType;
            copy.defaultFileName = defaultFileName;
            copy.selectMultipleFiles = selectMultipleFiles;
            copy.currentDirectory = currentDirectory;
            copy.uploadTitle = uploadTitle;
            copy.uploadDescription = uploadDescription;
            return copy;
        }

        public Component getParent() {
            return parent;
        }

        public String getDialogTitle() {
            return dialogTitle;
        }

        public FileType getFileType() {
            return fileType;
        }

        public String getDefaultFileName() {
            return defaultFileName;
        }

        public boolean isSelectMultipleFiles() {
            return selectMultipleFiles;
        }

        public File getCurrentDirectory() {
            return currentDirectory;
        }

        public String getUploadTitle() {
            return uploadTitle;
        }

        public String getUploadDescription() {
            return uploadDescription;
        }
    }

    public static void openUploadDialog(FileDialogConfig config, Consumer<File[]> onFilesSelected) {
        FileDialogConfig effectiveConfig = config == null ? new FileDialogConfig() : config.copy();
        JDialog dialog = new JDialog(ownerWindow(effectiveConfig.getParent()), dialogTitle(effectiveConfig, "Select File"));
        dialog.setModal(true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        UploadCard uploadCard = new UploadCard(effectiveConfig, selectedFiles -> {
            if (selectedFiles.length > 0) {
                dialog.dispose();
                onFilesSelected.accept(selectedFiles);
            }
        });
        dialog.setContentPane(uploadCard);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(420, 300));
        dialog.setLocationRelativeTo(effectiveConfig.getParent());
        dialog.setVisible(true);
    }

    public static void openFileDialog(FileDialogConfig config, Consumer<File[]> onFilesSelected) {
        FileDialogConfig effectiveConfig = config == null ? new FileDialogConfig() : config;
        FileDialog dialog = nativeDialog(effectiveConfig, FileDialog.LOAD, "Open File");
        dialog.setMultipleMode(effectiveConfig.isSelectMultipleFiles());
        dialog.setVisible(true);

        File[] selectedFiles = effectiveConfig.isSelectMultipleFiles()
                ? dialog.getFiles()
                : selectedFile(dialog);
        if (selectedFiles.length > 0) {
            onFilesSelected.accept(selectedFiles);
        }
        dialog.dispose();
    }

    public static void saveFileDialog(FileDialogConfig config, Consumer<File> onFileSelected) {
        FileDialogConfig effectiveConfig = config == null ? new FileDialogConfig() : config;
        FileDialog dialog = nativeDialog(effectiveConfig, FileDialog.SAVE, "Save File");
        dialog.setVisible(true);

        File[] selectedFiles = selectedFile(dialog);
        if (selectedFiles.length > 0) {
            onFileSelected.accept(applyDefaultExtension(selectedFiles[0], effectiveConfig.getFileType()));
        }
        dialog.dispose();
    }

    public static boolean isValidFileType(File file, FileType fileType) {
        if (file == null || fileType == null || fileType == FileType.ALL) {
            return true;
        }

        String fileName = file.getName().toLowerCase();
        for (String extension : fileType.getExtensions()) {
            if ("*".equals(extension) || fileName.endsWith("." + extension.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public static String fileTypeDescription(FileType fileType) {
        if (fileType == null || fileType == FileType.ALL) {
            return "Any file type";
        }
        return fileType.getDescription() + " (" + formatExtensions(fileType.getExtensions()) + ")";
    }

    private static FileDialog nativeDialog(FileDialogConfig config, int mode, String fallbackTitle) {
        Window owner = ownerWindow(config.getParent());
        FileDialog dialog = owner instanceof Dialog ownerDialog
                ? new FileDialog(ownerDialog, dialogTitle(config, fallbackTitle), mode)
                : new FileDialog(owner instanceof Frame ownerFrame ? ownerFrame : null, dialogTitle(config, fallbackTitle), mode);

        if (config.getCurrentDirectory() != null) {
            dialog.setDirectory(config.getCurrentDirectory().getAbsolutePath());
        }
        if (config.getDefaultFileName() != null && !config.getDefaultFileName().isBlank()) {
            dialog.setFile(config.getDefaultFileName());
        }
        FilenameFilter filter = filenameFilter(config.getFileType());
        if (filter != null) {
            dialog.setFilenameFilter(filter);
        }
        return dialog;
    }

    private static Window ownerWindow(Component parent) {
        return parent == null ? null : SwingUtilities.getWindowAncestor(parent);
    }

    private static String dialogTitle(FileDialogConfig config, String fallbackTitle) {
        return config.getDialogTitle() == null || config.getDialogTitle().isBlank()
                ? fallbackTitle
                : config.getDialogTitle();
    }

    private static FilenameFilter filenameFilter(FileType fileType) {
        if (fileType == null || fileType == FileType.ALL) {
            return null;
        }
        return (directory, name) -> hasAllowedExtension(name, fileType);
    }

    private static boolean hasAllowedExtension(String name, FileType fileType) {
        String fileName = name == null ? "" : name.toLowerCase();
        for (String extension : fileType.getExtensions()) {
            if ("*".equals(extension) || fileName.endsWith("." + extension.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static File[] selectedFile(FileDialog dialog) {
        String selectedName = dialog.getFile();
        String directory = dialog.getDirectory();
        if (selectedName == null || selectedName.isBlank() || directory == null) {
            return new File[0];
        }
        return new File[]{new File(directory, selectedName)};
    }

    private static File applyDefaultExtension(File file, FileType fileType) {
        if (file == null || fileType == null || fileType == FileType.ALL || isValidFileType(file, fileType)) {
            return file;
        }
        String[] extensions = fileType.getExtensions();
        if (extensions.length == 0 || "*".equals(extensions[0])) {
            return file;
        }
        return new File(file.getAbsolutePath() + "." + extensions[0].toLowerCase());
    }

    private static String formatExtensions(String[] extensions) {
        StringBuilder text = new StringBuilder();
        for (int index = 0; index < extensions.length; index++) {
            if (index > 0) {
                text.append(", ");
            }
            text.append("*.").append(extensions[index]);
        }
        return text.toString();
    }
}
