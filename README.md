# Site in a Photo!

SIP, or Site in a photo consists of a generator and a browser extension.
The idea is simply converting HTML (webpages) to PNG images, thus you can simply upload the image anywhere on the web, and then use the extension to convert it back
The generator can embed scripts, stylesheets, and even images!

The Generator is made with Java, while the extension is made with JavaScript

### The Generator

To compile the generator, you need to compile it with the following libraries

 - Apache Commons IO
 - HTML Compressor
 - Java JSON
 - JSoup
 
You can always download the compiled JAR and run `java -jar sip.jar` to use it.


### The extension

Open up the extnesions page in Chrome, tick the Developer Mode, and either drop the folder there, or use the buttons to install/compile the extension.
