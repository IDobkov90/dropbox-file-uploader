package com.example.dropbox_file_uploader.controller;

import com.example.dropbox_file_uploader.service.DropboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller responsible for handling file upload operations and managing the connection with Dropbox.
 * This controller serves the main page and provides information about the Dropbox connection status.
 */
@Controller
@RequestMapping("/")
public class FileUploadController {
    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);

    private final DropboxService dropboxService;

    /**
     * Constructs a new FileUploadController with the specified Dropbox service.
     *
     * @param dropboxService the service used to interact with Dropbox API
     */
    public FileUploadController(DropboxService dropboxService) {
        this.dropboxService = dropboxService;
    }

    /**
     * Handles requests to the root endpoint and renders the index page.
     * Tests the connection to Dropbox and adds the connection status to the model.
     * If the connection fails, an error message is added to the model.
     *
     * @param model the Spring MVC model to which attributes are added for rendering in the view
     * @return the name of the view to render (index)
     */
    @GetMapping("/")
    public String index(Model model) {
        try {
            boolean connectionStatus = dropboxService.testConnection();
            model.addAttribute("connectionStatus", connectionStatus);

            if (!connectionStatus) {
                model.addAttribute("errorMessage", "Не може да се осъществи връзка с Dropbox. Моля, проверете конфигурацията.");
            }
        } catch (Exception e) {
            logger.error("Error testing Dropbox connection", e);
            model.addAttribute("connectionStatus", false);
            model.addAttribute("errorMessage", "Грешка при проверка на връзката: " + e.getMessage());
        }

        return "index";
    }
}