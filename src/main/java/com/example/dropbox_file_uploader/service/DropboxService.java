package com.example.dropbox_file_uploader.service;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.CreateFolderErrorException;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * Service for interacting with Dropbox API to upload files and manage folders.
 * This service provides functionality to upload files to a specified Dropbox folder,
 * create necessary folder structures, and test connection to Dropbox.
 */
@Service
public class DropboxService {
    private static final Logger logger = LoggerFactory.getLogger(DropboxService.class);

    private final DbxClientV2 dropboxClient;

    @Value("${dropbox.target-folder}")
    private String targetFolder;

    @Value("${dropbox.fix.encoding:false}")
    private boolean fixEncoding;

    /**
     * Constructs a new DropboxService with the specified Dropbox client.
     *
     * @param dropboxClient The Dropbox client used for API operations
     */
    public DropboxService(DbxClientV2 dropboxClient) {
        this.dropboxClient = dropboxClient;
    }

    /**
     * Uploads a file to Dropbox with an optional custom file name.
     * If a custom file name is provided, the original file extension is preserved.
     * The file is uploaded to the configured target folder in Dropbox.
     *
     * @param file           The MultipartFile to upload to Dropbox
     * @param customFileName Optional custom name for the file in Dropbox (can be null)
     * @return The path where the file was uploaded in Dropbox
     * @throws IOException              If there's an error reading the file
     * @throws DbxException             If there's an error with the Dropbox API
     * @throws IllegalArgumentException If the file is empty or the file name is null
     */
    public String uploadFile(MultipartFile file, String customFileName) throws IOException, DbxException {
        if (file.isEmpty()) {
            logger.error("Attempted to upload an empty file");
            throw new IllegalArgumentException("File is empty");
        }

        String originalFileName = file.getOriginalFilename();
        String fileName = (customFileName != null && !customFileName.isEmpty())
                ? customFileName
                : originalFileName;

        if (fileName == null) {
            logger.error("File name is null");
            throw new IllegalArgumentException("File name cannot be null");
        }

        if (customFileName != null && !customFileName.isEmpty() && originalFileName != null) {
            String extension = getFileExtension(originalFileName);
            if (!customFileName.toLowerCase().endsWith(extension.toLowerCase())) {
                fileName = customFileName + extension;
            }
        }

        fileName = sanitizeFileName(fileName);

        String processedTargetFolder = fixEncoding ? fixCyrillicEncoding(targetFolder) : targetFolder;
        ensureFolderExists(processedTargetFolder);

        String fullPath = formatDropboxPath(processedTargetFolder, fileName);

        logger.debug("Uploading to path: {}", fullPath);

        try (InputStream in = file.getInputStream()) {
            FileMetadata metadata = dropboxClient.files().uploadBuilder(fullPath)
                    .withMode(WriteMode.OVERWRITE)
                    .uploadAndFinish(in);

            logger.info("File uploaded: {}", metadata.getPathDisplay());
            return metadata.getPathDisplay();
        } catch (DbxException e) {
            logger.error("Dropbox API error while uploading file: {}", fullPath, e);
            throw new DbxException("Failed to upload file to Dropbox: " + fullPath, e);
        } catch (IOException e) {
            logger.error("IO error while reading file for upload: {}", fileName, e);
            throw new IOException("Error reading file for upload: " + fileName, e);
        }
    }

    /**
     * Formats a Dropbox path by ensuring it starts with a slash, replacing backslashes with forward slashes,
     * removing duplicate slashes, and ensuring the folder path doesn't end with a slash.
     *
     * @param folder   The folder path to format
     * @param fileName The file name to append to the folder path
     * @return A properly formatted Dropbox path
     */
    private String formatDropboxPath(String folder, String fileName) {
        if (!folder.startsWith("/")) {
            folder = "/" + folder;
        }

        folder = folder.replace("\\", "/");

        folder = folder.replace("//", "/");

        if (folder.endsWith("/")) {
            folder = folder.substring(0, folder.length() - 1);
        }

        return folder + "/" + fileName;
    }

    /**
     * Ensures that the specified folder path exists in Dropbox.
     * Creates each component of the path if it doesn't already exist.
     *
     * @param folderPath The folder path to ensure exists
     * @throws DbxException If there's an error creating the folder structure
     */
    private void ensureFolderExists(String folderPath) throws DbxException {
        String[] pathComponents = folderPath.split("/");
        StringBuilder currentPath = new StringBuilder();

        for (String component : pathComponents) {
            if (component.isEmpty()) continue;

            if (!currentPath.isEmpty()) {
                currentPath.append("/");
            } else {
                currentPath.append("/");
            }
            currentPath.append(component);

            String path = currentPath.toString();

            try {
                dropboxClient.files().createFolderV2(path);
                logger.info("Created folder: {}", path);
            } catch (CreateFolderErrorException e) {
                if (e.errorValue.isPath() && e.errorValue.getPathValue().isConflict()) {
                    logger.info("Folder already exists: {}", path);
                } else {
                    logger.error("Error creating folder: {}", path, e);
                    throw e;
                }
            }
        }
    }

    /**
     * Sanitizes a file name by replacing characters that are not allowed in file names.
     *
     * @param fileName The file name to sanitize
     * @return A sanitized file name with invalid characters replaced by underscores
     */
    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    /**
     * Extracts the file extension from a file name.
     *
     * @param fileName The file name to extract the extension from
     * @return The file extension including the dot, or an empty string if no extension is found
     */
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return fileName.substring(lastDotIndex);
    }

    /**
     * Tests the connection to Dropbox by attempting to retrieve the current account information.
     *
     * @return true if the connection is successful, false otherwise
     */
    public boolean testConnection() {
        try {
            dropboxClient.users().getCurrentAccount();
            logger.info("Successfully connected to Dropbox");
            return true;
        } catch (DbxException e) {
            logger.error("Failed to connect to Dropbox", e);
            return false;
        }
    }

    /**
     * Fixes potential encoding issues with Cyrillic characters in the path.
     * This is useful when the path contains Cyrillic characters that might be incorrectly encoded.
     *
     * @param path The path that might contain incorrectly encoded Cyrillic characters
     * @return The path with corrected encoding for Cyrillic characters
     */
    private String fixCyrillicEncoding(String path) {
        try {
            byte[] bytes = path.getBytes("ISO-8859-1");
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.error("Error fixing Cyrillic encoding", e);
            return path;
        }
    }
}