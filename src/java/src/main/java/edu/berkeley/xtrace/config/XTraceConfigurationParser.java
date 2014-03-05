package edu.berkeley.xtrace.config;
import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


public class XTraceConfigurationParser {

  private static final String VALUE_ENABLED = "Enabled";
  private static final String VALUE_DISABLED = "Disabled";

  private static class XTraceConfigurationHandler extends DefaultHandler {
    
    private XTraceConfiguration configuration = new XTraceConfiguration();
    
    private static final String TAG_CONFIGURATION = "Configuration";
    private static final String TAG_XTRACE = "XTrace";
    private static final String TAG_CAUSALITY = "Causality";
    private static final String TAG_LOGGING = "Logging";
    private static final String TAG_FILTERS = "Filters";

    private int depth = 0;
    private DefaultHandler subHandler = null;
    private String currentChars = "";
    
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
      if (subHandler!=null)
        subHandler.characters(ch, start, length);
      else
        currentChars += String.copyValueOf(ch, start, length);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      currentChars = "";
      
      if (depth==0) {
        // Make sure document starts correctly        
        if (!TAG_CONFIGURATION.equals(qName))
          throw new SAXException("Unexpected start of configuration.  Expecting " + TAG_CONFIGURATION + ", found " + qName);
        
      } else if (depth==1) {        
        // Handle the tags that are allowed at this level
        if        (TAG_FILTERS.equals(qName)) subHandler = new XTraceFiltersHandler(configuration.loglevels);
        else if   (TAG_XTRACE.equals(qName));
        else if   (TAG_CAUSALITY.equals(qName));
        else if   (TAG_LOGGING.equals(qName));
        else      throw new SAXException("Unexpected start of tag.  Found " + qName);
        
      } else if (depth > 1) {
        if (subHandler!=null) {
          // If there's a subhandler, any depth is valid, pass off the call
          subHandler.startElement(uri, localName, qName, attributes);
        
        } else {
          // We don't handle any nested tags in this handler, so throw an exception if we find some
          if (depth != 1) throw new SAXException("Unexpected nested tag.  Found " + qName);
        }
      }
      
      depth++;
    }
    
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
      depth--;
      currentChars = currentChars.trim();

      if (depth==0) {
        // Make sure document ends correctly        
        if (!TAG_CONFIGURATION.equals(qName))
          throw new SAXException("Unexpected end of configuration.  Expecting " + TAG_CONFIGURATION + ", found " + qName);
      } else if (depth==1) {
        subHandler = null;

        // Handle the tags that are allowed at this level
        if        (TAG_FILTERS.equals(qName));
        else if   (TAG_XTRACE.equals(qName)) configuration.setEnabled(!VALUE_DISABLED.equals(currentChars));
        else if   (TAG_CAUSALITY.equals(qName)) configuration.causality = !VALUE_DISABLED.equals(currentChars);
        else if   (TAG_LOGGING.equals(qName)) configuration.logging = !VALUE_DISABLED.equals(currentChars);
        else      throw new SAXException("Unexpected end of tag.  Found " + qName);
        
      } else if (depth > 1) {        
        if (subHandler!=null) {
          // We're still inside the subhandler, pass off the call to it
          subHandler.endElement(uri, localName, qName);          
          
        } else {
          // Nested tags are otherwise not allowed.  The startElement tag should have excepted before getting to this point.
          throw new SAXException("Unexpected nested tag for end of element.  Found " + qName);
        }
      }
      
      currentChars = "";
    }
    
  }
  
  private static class XTraceFiltersHandler extends DefaultHandler {
    
    private static final String TAG_DEFAULT = "Default";
    
    private XTraceLogLevel loglevels;
    
    private int depth = 0;
    private String currentChars = "";
    
    public XTraceFiltersHandler(XTraceLogLevel loglevels) {
      this.loglevels = loglevels;
    }

    @Override
    public void characters(char[] ch, int start, int length) {
      currentChars += String.copyValueOf(ch, start, length);
    }
    
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      currentChars = "";
      
      if (depth==0) {
        // Check the element is allowed
        if        (TAG_DEFAULT.equals(qName));
        else if   (VALUE_ENABLED.equals(qName));
        else if   (VALUE_DISABLED.equals(qName));
        else      throw new SAXException("Unexpected start of tag.  Found " + qName);
        
      } else {
        // Shouldn't be any nested elements so throw an exception if we encounter nesting
        throw new SAXException("Unexpected start of element: " + qName);
      }
      
      depth++;
    }
    
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
      depth--;
      currentChars = currentChars.trim();
      
      if (depth==0) {
        // Check the element is allowed
        try {
          if        (TAG_DEFAULT.equals(qName)) loglevels.defaultEnabled = !VALUE_DISABLED.equals(currentChars);
          else if   (VALUE_ENABLED.equals(qName)) loglevels.on(Class.forName(currentChars));
          else if   (VALUE_DISABLED.equals(qName)) loglevels.off(Class.forName(currentChars));
          else      throw new SAXException("Unexpected element: " + qName);
          
        } catch (ClassNotFoundException e) {
          throw new SAXException("ClassNotFoundException", e);
        }
      } else {
        // Nested tags are otherwise not allowed.  The startElement tag should have excepted before getting to this point.
        throw new SAXException("Unexpected nested tag for end of element.  Found " + qName);        
      }
      
      currentChars = "";
    }
    
  }
  
  /**
   * Parses the given file and returns an XTraceConfiguration representative of the file.
   * @param f A file to parse
   * @return An XTraceConfiguration
   * @throws InvalidConfigurationException If the file is malformed
   * @throws IOException If there is a problem reading the file
   */
  public static XTraceConfiguration parse(File f) throws InvalidConfigurationException, IOException {
    try {
      XTraceConfigurationHandler handler = new XTraceConfigurationHandler();
      SAXParserFactory.newInstance().newSAXParser().parse(f, handler);
      return handler.configuration;
    } catch (SAXException e) {
      throw new InvalidConfigurationException("Exception parsing configuration.  " + e.getMessage());
    } catch (ParserConfigurationException e) {
      throw new InvalidConfigurationException("Exception setting up parser for parsing");
    }
  }

  @SuppressWarnings("serial")
  public static class InvalidConfigurationException extends Exception {

    public InvalidConfigurationException(String string) {
      super(string);
    }
  }
  
}
