# DOCX Processing with Apache POI

This document explains how to set up and use Apache POI for processing DOCX files in the Teaching Plan Generator.

## Dependencies

We've added the following dependencies to `pom.xml`:

```xml
<!-- Apache POI dependencies for Office documents -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi</artifactId>
    <version>5.2.5</version>
</dependency>
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

## Setup Instructions

1. After adding these dependencies to `pom.xml`, you need to refresh Maven to download and install them:

   - If using Eclipse/STS:
     - Right-click on the project → Maven → Update Project...
   
   - If using IntelliJ IDEA:
     - Click on the "Maven" tab on the right side
     - Click the "Refresh" button

   - If using command line:
     ```
     mvn clean install
     ```

2. Once Maven has installed the dependencies, restart your IDE to ensure the new libraries are recognized.

## Implementation Details

We have implemented two main methods for DOCX processing:

1. `readDocxContent(String filePath)` - Reads content from a DOCX file:
   - Extracts text from paragraphs
   - Extracts text from tables
   - Returns the content as a String

2. `saveContentToDocx(String content, String filePath)` - Saves content to a DOCX file:
   - Creates a new DOCX document
   - Splits the content by line breaks
   - Creates paragraphs for each line
   - Sets basic formatting (font family and size)
   - Saves the document to the specified path

## Potential Issues & Solutions

If you encounter linter errors or compile errors:

1. **Missing imports**: Make sure Maven has properly installed the dependencies
2. **Runtime errors**: Check the file paths to ensure they exist and are accessible
3. **Empty files**: When saving, ensure the content is not null or empty
4. **File permissions**: Ensure the application has read/write access to the file paths

## Extended Features (Future Enhancements)

1. **Rich text formatting** - Support for bold, italic, underline, etc.
2. **Document styles** - Apply pre-defined styles to maintain consistent formatting
3. **Images support** - Include images in the document
4. **Headers and footers** - Add headers and footers to the document
5. **Table of contents** - Generate a table of contents based on document headings

## Usage Example

```java
// Reading a DOCX file
String content = readDocxContent("/path/to/document.docx");

// Modifying content
content = content.replace("old text", "new text");

// Saving as a new DOCX file
saveContentToDocx(content, "/path/to/new-document.docx");
``` 