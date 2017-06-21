package com.eteks.homeview3d.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.DefaultFocusTraversalPolicy;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Element;
import javax.swing.text.Highlighter;
import javax.swing.text.Position;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.BlockView;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTMLEditorKit.HTMLFactory;

import com.eteks.homeview3d.model.UserPreferences;
import com.eteks.homeview3d.tools.OperatingSystem;
import com.eteks.homeview3d.viewcontroller.HelpController;
import com.eteks.homeview3d.viewcontroller.HelpView;

public class HelpPane extends JRootPane implements HelpView {
  private enum ActionType {SHOW_PREVIOUS, SHOW_NEXT, SEARCH, CLOSE}

  private final UserPreferences preferences;
  private JFrame                frame;
  private JLabel                searchLabel;
  private JTextField            searchTextField;
  private JEditorPane           helpEditorPane;
  
  public HelpPane(UserPreferences preferences, 
                  final HelpController controller) {
    this.preferences = preferences;
    createActions(preferences, controller);
    createComponents(preferences, controller);
    setMnemonics(preferences);
    layoutComponents();
    addLanguageListener(preferences);
    if (controller != null) {
      addHyperlinkListener(controller);
      installKeyboardActions();
    }
    
    setPage(controller.getHelpPage());
    controller.addPropertyChangeListener(HelpController.Property.HELP_PAGE, 
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            setPage(controller.getHelpPage());
            highlightText(controller.getHighlightedText());
          }
        });
    controller.addPropertyChangeListener(HelpController.Property.BROWSER_PAGE, 
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            SwingTools.showDocumentInBrowser(controller.getBrowserPage());
          }
        });
  }

  // 컨트롤러 액션 생성
  private void createActions(UserPreferences preferences, 
                             final HelpController controller) {
    ActionMap actions = getActionMap();    
    try {
      final ControllerAction showPreviousAction = new ControllerAction(
          preferences, HelpPane.class, ActionType.SHOW_PREVIOUS.name(), controller, "showPrevious");
      showPreviousAction.setEnabled(controller.isPreviousPageEnabled());
      controller.addPropertyChangeListener(HelpController.Property.PREVIOUS_PAGE_ENABLED, 
          new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
              showPreviousAction.setEnabled(controller.isPreviousPageEnabled());
            }
          });
      actions.put(ActionType.SHOW_PREVIOUS, showPreviousAction);
      
      final ControllerAction showNextAction = new ControllerAction(
          preferences, HelpPane.class, ActionType.SHOW_NEXT.name(), controller, "showNext");
      showNextAction.setEnabled(controller.isNextPageEnabled());
      controller.addPropertyChangeListener(HelpController.Property.NEXT_PAGE_ENABLED, 
          new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
              showNextAction.setEnabled(controller.isNextPageEnabled());
            }
          });
      actions.put(ActionType.SHOW_NEXT, showNextAction);
      
      actions.put(ActionType.SEARCH, new ResourceAction(preferences, HelpPane.class, ActionType.SEARCH.name()) {
          @Override
          public void actionPerformed(ActionEvent ev) {
            final Cursor previousCursor = getCursor();
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            try {
              String searchedText = searchTextField.getText().trim();
              if (searchedText.length() > 0) {
                controller.search(searchedText);
              }
            } finally {
              setCursor(previousCursor);
            }
          }
        });
    } catch (NoSuchMethodException ex) {
      throw new RuntimeException(ex);
    }
    actions.put(ActionType.CLOSE, new ResourceAction(
          preferences, HelpPane.class, ActionType.CLOSE.name(), true) {
        @Override
        public void actionPerformed(ActionEvent ev) {
          frame.setVisible(false);
        }
      });
  }

  // 속성 추가
  private void addLanguageListener(UserPreferences preferences) {
    preferences.addPropertyChangeListener(UserPreferences.Property.LANGUAGE, 
        new LanguageChangeListener(this));
  }

private static class LanguageChangeListener implements PropertyChangeListener {
    private WeakReference<HelpPane> helpPane;

    public LanguageChangeListener(HelpPane helpPane) {
      this.helpPane = new WeakReference<HelpPane>(helpPane);
    }
    
    public void propertyChange(PropertyChangeEvent ev) {
      HelpPane helpPane = this.helpPane.get();
      UserPreferences preferences = (UserPreferences)ev.getSource();
      if (helpPane == null) {
        preferences.removePropertyChangeListener(
            UserPreferences.Property.LANGUAGE, this);
      } else {
        if (helpPane.frame != null) {
          helpPane.frame.setTitle(preferences.getLocalizedString(HelpPane.class, "helpFrame.title"));
          helpPane.frame.applyComponentOrientation(
              ComponentOrientation.getOrientation(Locale.getDefault()));
        }
        helpPane.searchLabel.setText(SwingTools.getLocalizedLabelText(preferences, HelpPane.class, "searchLabel.text"));
        helpPane.searchTextField.setText("");
        helpPane.setMnemonics(preferences);
      }
    }
  }
  
  // 표시 요소 생성
  private void createComponents(UserPreferences preferences, final HelpController controller) {
    this.searchLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences, HelpPane.class, "searchLabel.text"));
    this.searchTextField = new JTextField(12);
    if (OperatingSystem.isMacOSXLeopardOrSuperior()) {
      this.searchTextField.putClientProperty("JTextField.variant", "search");
      this.searchTextField.putClientProperty("JTextField.Search.FindAction",
          getActionMap().get(ActionType.SEARCH));
    } 
    this.searchTextField.addActionListener(getActionMap().get(ActionType.SEARCH));
    this.searchTextField.getDocument().addDocumentListener(new DocumentListener() {
        public void changedUpdate(DocumentEvent ev) {
          getActionMap().get(ActionType.SEARCH).setEnabled(searchTextField.getText().trim().length() > 0);
          controller.setHighlightedText(searchTextField.getText());
        }
    
        public void insertUpdate(DocumentEvent ev) {
          changedUpdate(ev);
        }
    
        public void removeUpdate(DocumentEvent ev) {
          changedUpdate(ev);
        }
      });
    
    this.helpEditorPane = new JEditorPane();
    this.helpEditorPane.setBorder(null);
    this.helpEditorPane.setEditable(false);
    this.helpEditorPane.setContentType("text/html");
    this.helpEditorPane.putClientProperty(JEditorPane.W3C_LENGTH_UNITS, Boolean.TRUE);
    this.helpEditorPane.setHighlighter(new DefaultHighlighter());
    PropertyChangeListener highlightingTextListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          highlightText(controller.getHighlightedText());
        }
      };
    controller.addPropertyChangeListener(HelpController.Property.HIGHLIGHTED_TEXT, highlightingTextListener);
    this.helpEditorPane.addPropertyChangeListener("page", highlightingTextListener);
    final float resolutionScale = SwingTools.getResolutionScale();
    if (resolutionScale != 1) {
      final ViewFactory htmlFactory = new HTMLFactory() {
          @Override
          public View create(Element element) {
            AttributeSet attributes = element.getAttributes();
            if (attributes.getAttribute(AbstractDocument.ElementNameAttribute) == null
                && attributes.getAttribute(StyleConstants.NameAttribute) == HTML.Tag.HTML) {
              return new BlockView(element, View.Y_AXIS) {
                  @Override
                  protected void layout(int width, int height) {
                    super.layout(Math.round(width / resolutionScale), Math.round(height * resolutionScale));
                  }

                  @Override
                  public void paint(Graphics g, Shape allocation) {
                    Graphics2D g2D = (Graphics2D)g;
                    AffineTransform oldTransform = g2D.getTransform();
                    g2D.scale(resolutionScale, resolutionScale);
                    super.paint(g2D, allocation);
                    g2D.setTransform(oldTransform);
                  }

                  @Override
                  public float getMinimumSpan(int axis) {
                    return super.getMinimumSpan(axis) * resolutionScale;
                  }

                  @Override
                  public float getMaximumSpan(int axis) {
                    return super.getMaximumSpan(axis) * resolutionScale;
                  }

                  @Override
                  public float getPreferredSpan(int axis) {
                    return super.getPreferredSpan(axis) * resolutionScale;
                  }

                  @Override
                  public Shape modelToView(int pos, Shape shape, Position.Bias b) throws BadLocationException {
                    Rectangle bounds = shape.getBounds();
                    shape = super.modelToView(pos, bounds, b);
                    bounds = shape.getBounds();
                    bounds.x *= resolutionScale;
                    bounds.y *= resolutionScale;
                    bounds.width *= resolutionScale;
                    bounds.height *= resolutionScale;
                    return bounds;
                  }

                  @Override
                  public int viewToModel(float x, float y, Shape shape, Position.Bias[] bias) {
                    Rectangle bounds = shape.getBounds();
                    x /= resolutionScale;
                    y /= resolutionScale;
                    bounds.x /= resolutionScale;
                    bounds.y /= resolutionScale;
                    bounds.width /= resolutionScale;
                    bounds.height /= resolutionScale;
                    return super.viewToModel(x, y, bounds, bias);
                  }
                };
            }
            return super.create(element);
          }
        };
      this.helpEditorPane.setEditorKit(new HTMLEditorKit() {
          @Override
          public ViewFactory getViewFactory() {
            return htmlFactory;
          }
        });
    }
    
    setFocusTraversalPolicy(new DefaultFocusTraversalPolicy() {
        @Override
        public Component getDefaultComponent(Container container) {
          return helpEditorPane;
        }
      });
  }

  private void highlightText(String highlightedText) {
    DefaultHighlighter.DefaultHighlightPainter highlightPainter = 
        new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 204, 51));
    this.helpEditorPane.getHighlighter().removeAllHighlights();
    if (highlightedText != null) {
      ArrayList<String> highlightedWords = new ArrayList<String>();
      for (String highlightedWord : highlightedText.split("\\s")) {
        if (highlightedWord.length() > 0) {
          highlightedWords.add(highlightedWord);
        }
      }                            
      highlightWords(this.helpEditorPane.getDocument().getDefaultRootElement(), 
          highlightedWords.toArray(new String [highlightedWords.size()]), highlightPainter);
    } 
  }

  private void highlightWords(Element element, 
                              String [] highlightedWords, 
                              Highlighter.HighlightPainter highlightPainter) {
    if (element.isLeaf()) {
      int startOffset = element.getStartOffset();
      try {
        String text = element.getDocument().getText(element.getStartOffset(), 
            element.getEndOffset() - startOffset).toLowerCase();
        for (String highlightedWord : highlightedWords) {
          for (int index = 0; index < text.length() - 1 &&  (index = text.indexOf(highlightedWord, index)) >= 0; index += highlightedWord.length() + 1) {
            this.helpEditorPane.getHighlighter().addHighlight(
                startOffset + index, startOffset + index + highlightedWord.length(), highlightPainter);
          }
        }
      } catch (BadLocationException ex) {
      }
    } else {
      for (int i = 0, n = element.getElementCount(); i < n; i++) {
        highlightWords(element.getElement(i), highlightedWords, highlightPainter);
      }
    }
  }

  private void setMnemonics(UserPreferences preferences) {
    if (!OperatingSystem.isMacOSX()) {
      this.searchLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(
          preferences.getLocalizedString(HelpPane.class, "searchLabel.mnemonic")).getKeyCode());
      this.searchLabel.setLabelFor(this.searchTextField);
    }
  }
  
  private void layoutComponents() {
    final JToolBar toolBar = new JToolBar();
    toolBar.setFloatable(false);
    toolBar.setLayout(new GridBagLayout());
    ActionMap actions = getActionMap();    
    final JButton previousButton = new JButton(actions.get(ActionType.SHOW_PREVIOUS));
    final JButton nextButton = new JButton(actions.get(ActionType.SHOW_NEXT));
    toolBar.add(previousButton);
    toolBar.add(nextButton);
    layoutToolBarButtons(toolBar, previousButton, nextButton);
    toolBar.addPropertyChangeListener("componentOrientation", 
        new PropertyChangeListener () {
          public void propertyChange(PropertyChangeEvent evt) {
            layoutToolBarButtons(toolBar, previousButton, nextButton);
          }
        });
    toolBar.add(new JLabel(),
        new GridBagConstraints(2, 0, 1, 1, 1, 0, GridBagConstraints.CENTER, 
            GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    
    if (!OperatingSystem.isMacOSXLeopardOrSuperior()) {
      toolBar.add(this.searchLabel,
          new GridBagConstraints(3, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, 
              GridBagConstraints.NONE, new Insets(0, 0, 0, 5), 0, 0));
    }
    toolBar.add(this.searchTextField,
        new GridBagConstraints(4, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, 
            GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    this.searchTextField.setMaximumSize(this.searchTextField.getPreferredSize());
    if (!OperatingSystem.isMacOSXLeopardOrSuperior()) {
      toolBar.add(new JButton(actions.get(ActionType.SEARCH)),
          new GridBagConstraints(5, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, 
              GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));          
    }
    for (int i = 0, n = toolBar.getComponentCount(); i < n; i++) {      
      Component component = toolBar.getComponent(i);
      if (component instanceof JButton) {
        component.setFocusable(false);
      }
    }
    
    getContentPane().add(toolBar, BorderLayout.NORTH);
    getContentPane().add(new JScrollPane(this.helpEditorPane), BorderLayout.CENTER);
  }

private void layoutToolBarButtons(JToolBar toolBar, 
                                    JButton previousButton,
                                    JButton nextButton) {
    int buttonPadY;
    int buttonsTopBottomInset;
    if (OperatingSystem.isMacOSXLeopardOrSuperior() 
        && OperatingSystem.isJavaVersionGreaterOrEqual("1.7")) {
      buttonPadY = 6;
      buttonsTopBottomInset = -2;
    } else {
      buttonPadY = 0;
      buttonsTopBottomInset = 0;
    }
    ComponentOrientation orientation = toolBar.getComponentOrientation();
    GridBagLayout layout = (GridBagLayout)toolBar.getLayout();
    GridBagConstraints firstButtonConstraints = new GridBagConstraints(
        0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, 
        GridBagConstraints.NONE, new Insets(buttonsTopBottomInset, 0, buttonsTopBottomInset, 0), 0, buttonPadY);
    GridBagConstraints secondButtonContraints = new GridBagConstraints(
        1, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, 
        GridBagConstraints.NONE, new Insets(buttonsTopBottomInset, 0, buttonsTopBottomInset, 5), 0, buttonPadY);
    layout.setConstraints(orientation.isLeftToRight() ? previousButton : nextButton, 
        firstButtonConstraints);
    layout.setConstraints(orientation.isLeftToRight() ? nextButton : previousButton, 
        secondButtonContraints);
    if (OperatingSystem.isMacOSXLeopardOrSuperior()) {
      previousButton.putClientProperty("JButton.buttonType", "segmentedTextured");
      previousButton.putClientProperty("JButton.segmentPosition", "first");
      nextButton.putClientProperty("JButton.buttonType", "segmentedTextured");
      nextButton.putClientProperty("JButton.segmentPosition", "last");
    }
    toolBar.revalidate();
  }
    
  private void addHyperlinkListener(final HelpController controller) {
    this.helpEditorPane.addHyperlinkListener(new HyperlinkListener() {
        public void hyperlinkUpdate(HyperlinkEvent ev) {
          if (ev.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            controller.showPage(ev.getURL());
          }
        }
      });
  }

  private void installKeyboardActions() {
    ActionMap actions = getActionMap();
    InputMap inputMap = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    inputMap.put((KeyStroke)actions.get(ActionType.SHOW_PREVIOUS).getValue(Action.ACCELERATOR_KEY), 
        ActionType.SHOW_PREVIOUS);
    inputMap.put((KeyStroke)actions.get(ActionType.SHOW_NEXT).getValue(Action.ACCELERATOR_KEY), 
        ActionType.SHOW_NEXT);
    inputMap.put((KeyStroke)actions.get(ActionType.CLOSE).getValue(Action.ACCELERATOR_KEY), 
        ActionType.CLOSE);
    inputMap.put((KeyStroke)actions.get(ActionType.SEARCH).getValue(Action.ACCELERATOR_KEY), 
        ActionType.SEARCH);
  }

  // 패널 표시
  public void displayView() {
    if (this.frame == null) {
      this.frame = new JFrame() {
          {
            setRootPane(HelpPane.this);
          }
        };
      this.frame.setIconImage(new ImageIcon(HelpPane.class.getResource(
          this.preferences.getLocalizedString(HelpPane.class, "helpFrame.icon"))).getImage());
      this.frame.setTitle(this.preferences.getLocalizedString(HelpPane.class, "helpFrame.title"));
      this.frame.applyComponentOrientation(ComponentOrientation.getOrientation(Locale.getDefault()));
      computeFrameBounds(this.frame);
      this.frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    }
    
    this.frame.setVisible(true);
    this.frame.setState(JFrame.NORMAL);
    this.frame.toFront();
  }
  
 private void computeFrameBounds(JFrame frame) {
    frame.setLocationByPlatform(true);
    Dimension screenSize = getToolkit().getScreenSize();
    Insets screenInsets = getToolkit().getScreenInsets(getGraphicsConfiguration());
    screenSize.width -= screenInsets.left + screenInsets.right;
    screenSize.height -= screenInsets.top + screenInsets.bottom;
    frame.setSize(Math.min(2 * screenSize.width / 3, Math.round(800 * SwingTools.getResolutionScale())), 
        screenSize.height * 4 / 5);
  }
  
  private void setPage(URL url) {
    try {
      this.helpEditorPane.setPage(url);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }
}
