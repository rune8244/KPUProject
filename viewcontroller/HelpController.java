package com.eteks.homeview3d.viewcontroller;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.text.BadLocationException;
import javax.swing.text.ChangedCharSetException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import com.eteks.homeview3d.model.UserPreferences;
import com.eteks.homeview3d.tools.ResourceURLContent;

public class HelpController implements Controller {
  public enum Property {HELP_PAGE, BROWSER_PAGE, 
      PREVIOUS_PAGE_ENABLED, NEXT_PAGE_ENABLED, HIGHLIGHTED_TEXT}

  private static final String SEARCH_RESULT_PROTOCOL = "search";
  
  private final UserPreferences       preferences;
  private final ViewFactory           viewFactory;
  private final PropertyChangeSupport propertyChangeSupport;
  private final List<URL>             history;
  private int                         historyIndex;
  private HelpView                    helpView;
  
  private URL helpPage;
  private URL browserPage;
  private boolean previousPageEnabled;
  private boolean nextPageEnabled;
  private String  highlightedText;
  
  public HelpController(UserPreferences preferences, 
                        ViewFactory viewFactory) {
    this.preferences = preferences;
    this.viewFactory = viewFactory;    
    this.propertyChangeSupport = new PropertyChangeSupport(this);
    this.history = new ArrayList<URL>();
    this.historyIndex = -1;
    showPage(getHelpIndexPageURL());
  }

  
  public HelpView getView() {
    if (this.helpView == null) {
      this.helpView = this.viewFactory.createHelpView(this.preferences, this);
      addLanguageListener(this.preferences);
    }
    return this.helpView;
  }

 
  public void displayView() {
    getView().displayView();
  }

  public void addPropertyChangeListener(Property property, PropertyChangeListener listener) {
    this.propertyChangeSupport.addPropertyChangeListener(property.name(), listener);
  }

  
  public void removePropertyChangeListener(Property property, PropertyChangeListener listener) {
    this.propertyChangeSupport.removePropertyChangeListener(property.name(), listener);
  }

  
  private void setHelpPage(URL helpPage) {
    if (helpPage != this.helpPage) {
      URL oldHelpPage = this.helpPage;
      this.helpPage = helpPage;
      this.propertyChangeSupport.firePropertyChange(Property.HELP_PAGE.name(), oldHelpPage, helpPage);
    }
  }
 
  public URL getHelpPage() {
    return this.helpPage;
  }

 
  private void setBrowserPage(URL browserPage) {
    if (browserPage != this.browserPage) {
      URL oldBrowserPage = this.browserPage;
      this.browserPage = browserPage;
      this.propertyChangeSupport.firePropertyChange(Property.BROWSER_PAGE.name(), oldBrowserPage, browserPage);
    }
  }
  
  public URL getBrowserPage() {
    return this.browserPage;
  }

 
  private void setPreviousPageEnabled(boolean previousPageEnabled) {
    if (previousPageEnabled != this.previousPageEnabled) {
      this.previousPageEnabled = previousPageEnabled;
      this.propertyChangeSupport.firePropertyChange(Property.PREVIOUS_PAGE_ENABLED.name(), 
          !previousPageEnabled, previousPageEnabled);
    }
  }
  
  
  public boolean isPreviousPageEnabled() {
    return this.previousPageEnabled;
  }

  private void setNextPageEnabled(boolean nextPageEnabled) {
    if (nextPageEnabled != this.nextPageEnabled) {
      this.nextPageEnabled = nextPageEnabled;
      this.propertyChangeSupport.firePropertyChange(Property.NEXT_PAGE_ENABLED.name(), 
          !nextPageEnabled, nextPageEnabled);
    }
  }
  
 
  public boolean isNextPageEnabled() {
    return this.nextPageEnabled;
  }

  
  public void setHighlightedText(String highlightedText) {
    if (highlightedText != this.highlightedText
        && (highlightedText == null || !highlightedText.equals(this.highlightedText))) {
      String oldHighlightedText = this.highlightedText;
      this.highlightedText = highlightedText;
      this.propertyChangeSupport.firePropertyChange(Property.HIGHLIGHTED_TEXT.name(), 
          oldHighlightedText, highlightedText);
    }
  }
  
 
  public String getHighlightedText() {
    return getHelpPage() == null || SEARCH_RESULT_PROTOCOL.equals(getHelpPage().getProtocol()) 
        ? null
        : this.highlightedText;
  }

 
  private void addLanguageListener(UserPreferences preferences) {
    preferences.addPropertyChangeListener(UserPreferences.Property.LANGUAGE, 
        new LanguageChangeListener(this));
  }

 
  private static class LanguageChangeListener implements PropertyChangeListener {
    private WeakReference<HelpController> helpController;

    public LanguageChangeListener(HelpController helpController) {
      this.helpController = new WeakReference<HelpController>(helpController);
    }
    
    public void propertyChange(PropertyChangeEvent ev) {     
      HelpController helpController = this.helpController.get();
      if (helpController == null) {
        ((UserPreferences)ev.getSource()).removePropertyChangeListener(
            UserPreferences.Property.LANGUAGE, this);
      } else {
        helpController.history.clear();
        helpController.historyIndex = -1;
        helpController.showPage(helpController.getHelpIndexPageURL());
      }
    }
  }
  
  
  public void showPrevious() {
    setHelpPage(this.history.get(--this.historyIndex));
    setPreviousPageEnabled(this.historyIndex > 0);
    setNextPageEnabled(true);
  }

  
  public void showNext() {
    setHelpPage(this.history.get(++this.historyIndex));
    setPreviousPageEnabled(true);
    setNextPageEnabled(this.historyIndex < this.history.size() - 1);
  }

 
  public void showPage(URL page) {
    if (isBrowserPage(page)) {
      setBrowserPage(page);
    } else if (this.historyIndex == -1
            || !this.history.get(this.historyIndex).equals(page)) {
      setHelpPage(page);
      for (int i = this.history.size() - 1; i > this.historyIndex; i--) {
        this.history.remove(i);
      }
      this.history.add(page);
      setPreviousPageEnabled(++this.historyIndex > 0);
      setNextPageEnabled(false);
    }
  }
  
 
  protected boolean isBrowserPage(URL page) {
    String protocol = page.getProtocol();
    return protocol.equals("http") || protocol.equals("https");
  }
  
  
  private URL getHelpIndexPageURL() {
    String helpIndex = this.preferences.getLocalizedString(HelpController.class, "helpIndex");
    try {
      return new URL(helpIndex);
    } catch (MalformedURLException ex) {
      String classPackage = HelpController.class.getName();
      classPackage = classPackage.substring(0, classPackage.lastIndexOf(".")).replace('.', '/');
      String helpIndexWithoutLeadingSlash = helpIndex.startsWith("/") 
          ? helpIndex.substring(1) 
          : classPackage + '/' + helpIndex;
      for (ClassLoader classLoader : this.preferences.getResourceClassLoaders()) {
        try {
          return new ResourceURLContent(classLoader, helpIndexWithoutLeadingSlash).getURL();
        } catch (IllegalArgumentException ex2) {
        }
      }
      try {
        return new ResourceURLContent(HelpController.class, helpIndex).getURL();
      } catch (IllegalArgumentException ex2) {
        ex2.printStackTrace();
        return new ResourceURLContent(HelpController.class, "resources/help/en/index.html").getURL();
      }
    }
  }
  
  public void search(String searchedText) {
    URL helpIndex = getHelpIndexPageURL();
    String [] searchedWords = getLowerCaseSearchedWords(searchedText);
    List<HelpDocument> helpDocuments = searchInHelpDocuments(helpIndex, searchedWords);
    URL applicationIconUrl = null;
    try {
      applicationIconUrl = new ResourceURLContent(HelpController.class, "resources/help/images/applicationIcon32.png").getURL();
    } catch (Exception ex) {
    }
    final StringBuilder htmlText = new StringBuilder(
        "<html><head><meta http-equiv='content-type' content='text/html;charset=UTF-8'><link href='" 
        + new ResourceURLContent(HelpController.class, "resources/help/help.css").getURL()
        + "' rel='stylesheet'></head><body bgcolor='#ffffff'>\n"
        + "<div id='banner'><div id='helpheader'>"
        + "  <a class='bread' href='" + helpIndex + "'> " 
        +        this.preferences.getLocalizedString(HelpController.class, "helpTitle") + "</a>"
        + "</div></div>"
        + "<div id='mainbox' align='left'>"
        + "  <table width='100%' border='0' cellspacing='0' cellpadding='0'>"
        + "    <tr valign='bottom' height='32'>"
        + "      <td width='3' height='32'>&nbsp;</td>"
        + (applicationIconUrl != null 
              ? "<td width='32' height='32'><img src='" + applicationIconUrl + "' height='32' width='32'></td>" 
              : "")
        + "      <td width='8' height='32'>&nbsp;&nbsp;</td>"
        + "      <td valign='bottom' height='32'><font id='topic'>" 
        +            this.preferences.getLocalizedString(HelpController.class, "searchResult") + "</font></td>"
        + "    </tr>"
        + "    <tr height='10'><td colspan='4' height='10'>&nbsp;</td></tr>"
        + "  </table>"
        + "  <table width='100%' border='0' cellspacing='0' cellpadding='3'>");
    
    if (helpDocuments.size() == 0) {
      String searchNotFound = this.preferences.getLocalizedString(HelpController.class, "searchNotFound", searchedText); 
      htmlText.append("<tr><td><p>" + searchNotFound + "</td></tr>");
    } else {
      String searchFound = this.preferences.getLocalizedString(HelpController.class, "searchFound", searchedText); 
      htmlText.append("<tr><td colspan='2'><p>" + searchFound + "</td></tr>");
      
      URL searchRelevanceImage = new ResourceURLContent(HelpController.class, "resources/searchRelevance.gif").getURL();
      for (HelpDocument helpDocument : helpDocuments) {
        htmlText.append("<tr><td valign='middle' nowrap><a href='" + helpDocument.getBase() + "'>" 
            + helpDocument.getTitle() + "</a></td><td valign='middle'>");
        for (int i = 0; i < helpDocument.getRelevance() && i < 50; i++) {
          htmlText.append("<img src='" + searchRelevanceImage + "' width='4' height='12'>");
        }
        htmlText.append("</td></tr>");
      }
    }
    htmlText.append("</table></div></body></html>");

    try {
      showPage(new URL(null, SEARCH_RESULT_PROTOCOL + "://" + htmlText.hashCode(), new URLStreamHandler() {
          @Override
          protected URLConnection openConnection(URL url) throws IOException {
            return new URLConnection(url) {
                @Override
                public void connect() throws IOException {
                }
                
                @Override
                public InputStream getInputStream() throws IOException {
                  return new ByteArrayInputStream(
                      htmlText.toString().getBytes("UTF-8"));
                }
              };
          }
        }));
    } catch (MalformedURLException ex) {
    }
  }

  private String [] getLowerCaseSearchedWords(String searchedText) {
    String [] searchedWords = searchedText.split("\\s");
    for (int i = 0; i < searchedWords.length; i++) {
      searchedWords [i] = searchedWords [i].toLowerCase().trim();
    }
    return searchedWords;
  }

 
  private List<HelpDocument> searchInHelpDocuments(URL helpIndex, String [] searchedWords) {
    List<URL> parsedDocuments = new ArrayList<URL>(); 
    parsedDocuments.add(helpIndex);
    
    List<HelpDocument> helpDocuments = new ArrayList<HelpDocument>();
    for (int i = 0; i < parsedDocuments.size(); i++) {
      try {
        URL helpDocumentUrl = parsedDocuments.get(i);
        HelpDocument helpDocument = new HelpDocument(helpDocumentUrl, searchedWords);
        helpDocument.parse();
        // If searched text was found add it to returned documents list
        if (helpDocument.getRelevance() > 0) {
          helpDocuments.add(helpDocument);
        }
        for (URL url : helpDocument.getReferencedDocuments()) {
          String lowerCaseFile = url.getFile().toLowerCase();
          if (lowerCaseFile.endsWith(".html")
              && !parsedDocuments.contains(url)) {
            parsedDocuments.add(url);
          } 
        } 
      } catch (IOException ex) {
      }
    }
    Collections.sort(helpDocuments, new Comparator<HelpDocument>() {
        public int compare(HelpDocument document1, HelpDocument document2) {
          return document2.getRelevance() - document1.getRelevance();
        }
      });
    return helpDocuments;
  }

  private class HelpDocument extends HTMLDocument {
    private Set<URL>     referencedDocuments = new HashSet<URL>();
    private String []    searchedWords;
    private int          relevance;
    private String       title = "";

    public HelpDocument(URL helpDocument, String [] searchedWords) {
      this.searchedWords = searchedWords;
      setBase(helpDocument);
    }

    
    public void parse() throws IOException {
      HTMLEditorKit html = new HTMLEditorKit();
      Reader urlReader = null;
      try {
        urlReader = new InputStreamReader(getBase().openStream(), "ISO-8859-1");
        putProperty("IgnoreCharsetDirective", Boolean.FALSE);
        try {
          html.read(urlReader, this, 0);
        } catch (ChangedCharSetException ex) {
          String mimeType = ex.getCharSetSpec();
          String encoding = mimeType.substring(mimeType.indexOf("=") + 1).trim();
          urlReader.close();
          urlReader = new InputStreamReader(getBase().openStream(), encoding);
          putProperty("IgnoreCharsetDirective", Boolean.TRUE);
          html.read(urlReader, this, 0);
        }
      } catch (BadLocationException ex) {
      } finally {
        if (urlReader != null) {
          try {
            urlReader.close();
          } catch (IOException ex) {
          }
        }
      }
    }

    public Set<URL> getReferencedDocuments() {
      return this.referencedDocuments;
    }
    
    public int getRelevance() {
      return this.relevance;
    }
    
    public String getTitle() {
      return this.title;
    }
    
    private void addReferencedDocument(String referencedDocument) {
      try {        
        URL url = new URL(getBase(), referencedDocument);
        if (!isBrowserPage(url)) {
          URL urlWithNoAnchor = new URL(
              url.getProtocol(), url.getHost(), url.getPort(), url.getFile());
          this.referencedDocuments.add(urlWithNoAnchor);
        }
      } catch (MalformedURLException e) {
      }
    }

    @Override
    public HTMLEditorKit.ParserCallback getReader(int pos) {
      return new HelpReader();
    }

    private class HelpReader extends HTMLEditorKit.ParserCallback {
      private boolean inTitle;

      @Override
      public void handleStartTag(HTML.Tag tag,
                                 MutableAttributeSet att, int pos) {
        if (tag.equals(HTML.Tag.A)) { 
          String attribute = (String)att.getAttribute(HTML.Attribute.HREF);
          if (attribute != null) {
            addReferencedDocument(attribute);
          }
        } else if (tag.equals(HTML.Tag.TITLE)) {
          this.inTitle = true;
        } 
      }
      
      @Override
      public void handleEndTag(Tag tag, int pos) {
        if (tag.equals(HTML.Tag.TITLE)) {
          this.inTitle = false;
        }
      }
      
      @Override
      public void handleSimpleTag(Tag tag, MutableAttributeSet att, int pos) {
        if (tag.equals(HTML.Tag.META)) {
          String nameAttribute = (String)att.getAttribute(HTML.Attribute.NAME); 
          String contentAttribute = (String)att.getAttribute(HTML.Attribute.CONTENT);
          if ("keywords".equalsIgnoreCase(nameAttribute)
              && contentAttribute != null) {
            searchWords(contentAttribute);
          }
        }
      }
      
      @Override
      public void handleText(char [] data, int pos) {
        String text = new String(data);
        if (this.inTitle) {
          title += text;
        }
        searchWords(text);
      }

      private void searchWords(String text) {
        String lowerCaseText = text.toLowerCase();
        for (String searchedWord : searchedWords) {
          for (int index = 0; index < lowerCaseText.length(); index += searchedWord.length() + 1) {
            index = lowerCaseText.indexOf(searchedWord, index);
            if (index == -1) {
              break;
            } else {
              relevance++;
              if (this.inTitle) {
                relevance++;
              }
            }
          }
        }
      }
    }
  }
}
