package com.example.dropbox_file_uploader.model.dto;

/**
 * Data transfer object representing an API response.
 * Contains information about the success status of an operation,
 * a descriptive message, and an optional path.
 */
public class ApiResponse {

    private boolean success;
    private String message;
    private String path;

    /**
     * Default constructor for ApiResponse.
     */
    public ApiResponse() {
    }

    /**
     * Checks if the API operation was successful.
     * 
     * @return true if the operation was successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Sets the success status of the API operation.
     * 
     * @param success true if the operation was successful, false otherwise
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * Gets the descriptive message of the API response.
     * 
     * @return the message describing the result of the operation
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the descriptive message of the API response.
     * 
     * @param message the message describing the result of the operation
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Gets the path associated with the API response.
     * 
     * @return the path related to the operation, or null if not applicable
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the path associated with the API response.
     * 
     * @param path the path related to the operation
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Creates a successful API response with the specified message and path.
     * 
     * @param message the success message to include in the response
     * @param path the path associated with the successful operation
     * @return a new ApiResponse object with success status set to true
     */
    public static ApiResponse success(String message, String path) {
        ApiResponse response = new ApiResponse();
        response.setSuccess(true);
        response.setMessage(message);
        response.setPath(path);
        return response;
    }

    /**
     * Creates an error API response with the specified error message.
     * 
     * @param message the error message to include in the response
     * @return a new ApiResponse object with success status set to false
     */
    public static ApiResponse error(String message) {
        ApiResponse response = new ApiResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}