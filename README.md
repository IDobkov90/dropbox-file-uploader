# Dropbox File Uploader

An application for uploading files to Dropbox using Spring Boot.
## Features


- Upload images and PDF files to Dropbox
- Drag-and-drop interface
- Progress tracking during upload
- Support for Cyrillic characters in filenames
- File type and size validation

## Requirements

- Java 17 or newer version
- Maven
- Dropbox account and API token

## Project Setup

1. Clone the repository
   git clone https://github.com/IDobkov90/dropbox-file-uploader.git


2. Create a .env file in the root directory of the project with the following content:
   DROPBOX_ACCESS_TOKEN=your_access_token


3. Start the application:


## Configuration

The main settings can be modified in application.properties:

- dropbox.target-folder - The target directory in Dropbox
- server.port - Server port
- spring.servlet.multipart.max-file-size - Maximum file size for upload

