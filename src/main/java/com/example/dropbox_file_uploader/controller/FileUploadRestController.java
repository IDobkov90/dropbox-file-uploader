package com.example.dropbox_file_uploader.controller;

import com.dropbox.core.DbxException;
import com.example.dropbox_file_uploader.model.dto.ApiResponse;
import com.example.dropbox_file_uploader.service.DropboxService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.IOException;
import java.util.Map;

/**
 * REST controller responsible for handling file upload operations to Dropbox.
 * Provides endpoints for uploading files and testing the Dropbox connection.
 */
@RestController
@RequestMapping("/")
public class FileUploadRestController {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadRestController.class);

    private final DropboxService dropboxService;

    /**
     * Constructs a new FileUploadRestController with the specified DropboxService.
     *
     * @param dropboxService The service used for Dropbox operations
     */
    public FileUploadRestController(DropboxService dropboxService) {
        this.dropboxService = dropboxService;
    }

    /**
     * Handles file upload requests through a flexible approach that can accommodate
     * various client implementations. This endpoint extracts the file and optional
     * custom filename from the multipart request, validates the file, and uploads it to Dropbox.
     * <p>
     * The method supports:
     * - File size validation (max 10MB)
     * - File type validation (images and PDFs only)
     * - Custom filename specification
     * - Robust error handling for various failure scenarios
     *
     * @param request The HTTP request containing the multipart file data and optional parameters
     * @return A ResponseEntity containing an ApiResponse with the upload result:
     * - 200 OK with success message and file path if upload is successful
     * - 400 Bad Request if file is missing, empty, or invalid
     * - 503 Service Unavailable if Dropbox service is unavailable
     * - 500 Internal Server Error for other unexpected errors
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "application/json;charset=UTF-8")
    public ResponseEntity<ApiResponse> uploadFile(
            HttpServletRequest request) {

        logger.debug("Content-Type: {}", request.getContentType());

        MultipartFile file = null;
        String cyrillicFileName = null;

        try {
            if (request instanceof MultipartHttpServletRequest) {
                MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;

                Map<String, String[]> paramMap = multipartRequest.getParameterMap();
                StringBuilder paramLog = new StringBuilder("All parameters: ");
                for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
                    paramLog.append(entry.getKey()).append("=");
                    for (String value : entry.getValue()) {
                        paramLog.append(value).append(", ");
                    }
                }
                logger.debug(paramLog.toString());

                Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
                StringBuilder fileLog = new StringBuilder("All files: ");
                for (Map.Entry<String, MultipartFile> entry : fileMap.entrySet()) {
                    fileLog.append(entry.getKey()).append("=").append(entry.getValue().getOriginalFilename()).append(", ");
                }
                logger.debug(fileLog.toString());

                file = multipartRequest.getFile("file");
                if (file == null) {

                    for (String key : fileMap.keySet()) {
                        file = multipartRequest.getFile(key);
                        if (file != null) {
                            logger.debug("Found file with parameter name: {}", key);
                            break;
                        }
                    }
                }

                cyrillicFileName = multipartRequest.getParameter("filename");
                if (cyrillicFileName == null || cyrillicFileName.isEmpty()) {
                    for (String key : paramMap.keySet()) {
                        String value = multipartRequest.getParameter(key);
                        if (value != null && !value.isEmpty() && !key.equals("file")) {
                            cyrillicFileName = value;
                            logger.debug("Found filename with parameter name: {}", key);
                            break;
                        }
                    }
                }
            } else {
                logger.error("Request is not a MultipartHttpServletRequest");
            }
        } catch (Exception e) {
            logger.error("Error processing multipart request", e);
        }

        if (file == null) {
            logger.error("File is null - not properly sent from client");
            return ResponseEntity.badRequest().body(ApiResponse.error("Файлът не е изпратен правилно. Моля, проверете заявката."));
        }

        logger.debug("Uploading file: name={}, size={}, contentType={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Моля, изберете файл за качване"));
        }

        String contentType = file.getContentType();
        if (contentType == null || !(contentType.startsWith("image/") || contentType.equals("application/pdf"))) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Неподдържан тип файл. Моля, изберете изображение или PDF."));
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Файлът е твърде голям (максимум 10MB)"));
        }

        if (cyrillicFileName != null && !cyrillicFileName.isEmpty()) {
            logger.debug("Custom filename provided: {}", cyrillicFileName);
        }

        try {
            String uploadedPath = dropboxService.uploadFile(file, cyrillicFileName);
            logger.info("File uploaded successfully to: {}", uploadedPath);
            return ResponseEntity.ok(ApiResponse.success("Файлът е качен успешно!", uploadedPath));
        } catch (DbxException e) {
            logger.error("Dropbox API error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error("Грешка при качване на файла в Dropbox: " + e.getMessage()));
        } catch (IOException e) {
            logger.error("IO error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Грешка при четене на файла: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Неочаквана грешка: " + e.getMessage()));
        }
    }

    /**
     * Tests the connection to the Dropbox service.
     *
     * @return A ResponseEntity containing an ApiResponse with the connection test result:
     * - 200 OK with success message if connection is successful
     * - 503 Service Unavailable if connection fails
     */
    @GetMapping("/test-connection")
    public ResponseEntity<ApiResponse> testConnection() {
        boolean connected = dropboxService.testConnection();

        if (connected) {
            return ResponseEntity.ok(ApiResponse.success("Връзката с Dropbox е успешна", null));
        } else {
            return ResponseEntity.status(503).body(ApiResponse.error("Грешка при връзка с Dropbox"));
        }
    }

    /**
     * Alternative endpoint for file uploads that uses Spring's annotation-based parameter binding.
     * This provides a more straightforward implementation compared to the primary upload endpoint,
     * but with the same validation and functionality.
     *
     * @param file             The multipart file to be uploaded
     * @param cyrillicFileName Optional custom filename to use when storing the file (can contain Cyrillic characters)
     * @return A ResponseEntity containing an ApiResponse with the upload result:
     * - 200 OK with success message and file path if upload is successful
     * - 400 Bad Request if file is missing, empty, or invalid
     * - 503 Service Unavailable if Dropbox service is unavailable
     * - 500 Internal Server Error for other unexpected errors
     */
    @PostMapping(value = "/api/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "application/json;charset=UTF-8")
    public ResponseEntity<ApiResponse> uploadFileAlternative(
            @RequestPart(value = "file") MultipartFile file,
            @RequestParam(value = "filename", required = false) String cyrillicFileName) {

        logger.debug("Alternative upload endpoint called");
        logger.debug("File: {}, Filename: {}", file != null ? file.getOriginalFilename() : "null", cyrillicFileName);

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Моля, изберете файл за качване"));
        }

        String contentType = file.getContentType();
        if (contentType == null || !(contentType.startsWith("image/") || contentType.equals("application/pdf"))) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Неподдържан тип файл. Моля, изберете изображение или PDF."));
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Файлът е твърде голям (максимум 10MB)"));
        }

        try {
            String uploadedPath = dropboxService.uploadFile(file, cyrillicFileName);
            logger.info("File uploaded successfully to: {}", uploadedPath);
            return ResponseEntity.ok(ApiResponse.success("Файлът е качен успешно!", uploadedPath));
        } catch (DbxException e) {
            logger.error("Dropbox API error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error("Грешка при качване на файла в Dropbox: " + e.getMessage()));
        } catch (IOException e) {
            logger.error("IO error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Грешка при четене на файла: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Неочаквана грешка: " + e.getMessage()));
        }
    }
}