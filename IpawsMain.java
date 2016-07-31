package com.interop.ipawsui;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.TimerTask;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import net.sourceforge.peers.media.MediaMode;
import net.sourceforge.peers.sip.RFC3261;
import net.sourceforge.peers.sip.Utils;
import net.sourceforge.peers.sip.core.useragent.SipListener;
import net.sourceforge.peers.sip.core.useragent.UserAgent;
import net.sourceforge.peers.sip.syntaxencoding.SipHeaderFieldName;
import net.sourceforge.peers.sip.syntaxencoding.SipHeaderFieldValue;
import net.sourceforge.peers.sip.syntaxencoding.SipHeaders;
import net.sourceforge.peers.sip.syntaxencoding.SipURI;
import net.sourceforge.peers.sip.syntaxencoding.SipUriSyntaxException;
import net.sourceforge.peers.sip.transport.SipRequest;
import net.sourceforge.peers.sip.transport.SipResponse;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.interop.api.TurtleAPI.TurtleAPIException;
import com.interop.turtleUtil.ColorUtil;
import com.interop.turtleUtil.GSTVideo;
import com.interop.turtleUtil.GuiUtil;
import com.interop.turtleUtil.TurtleParacleteUtil;

public class IpawsMain {
	private static IpawsMain self = null;
	private Logger logger = Logger.getLogger(IpawsMain.class);
	private static String separator = System.getProperty("line.separator");
	private static final int MOUSE_CLICK_AND_HOLD_THRESHOLD = 200; // milliseonds

	private ResourceBundle defaults = ResourceBundle.getBundle("IpawsMain");
	private ResourceBundle res = ResourceBundle
			.getBundle("com.interop.ipawsui.MsgIpaws");

	private static final int defaultWidth = 1300;
	private static final int defaultHeight = 600;
	private static final String colorResourceXML = "/com/interop/iris/Color.xml";

	private static Color red = null;
	private static Color green = null;
	private static Color yellowGreen = null;
	private static Color gray = null;
	private static Color black = null;

	private static Cursor handCursor = null;
	private static Font superLargeFont = null;
	private static Font boldSmallFont = null;
	private static Font boldMediumFont = null;
	private static Font smallFont = null;
	private static Font mediumFont = null;
	private static Font largeFont = null;
	private GuiUtil guiUtil;

	private Shell shell;
	private Composite toolbarComp;
	private Label statusLabel;
	private boolean loggedin;

	private Point viewSize;
	private Point viewLocation;

	private Image openMicrophoneImage;
	private Image mutedMicrophoneImage;
	private Image microphoneMutedImage;
	private Image microphoneActiveImage;
	private Image volumeImage;
	private Image videoOnImage;
	private Image videoOffImage;
	private Image disconnectButtonInactiveImage;

	private Image videoButtonImage;
	private Image moreImage;
	private Image appLogo;
	private Composite mainTab;
	private Button videoToggleButton;
	private Button micButton;

	private String userID;
	private String password;
	private String serverURL;
	private boolean loggedIn = false;
	private IpawsAPI api;
	
	public static void main(String[] args) {
		IpawsMain app = IpawsMain.getInstance();
		app.init();
	}

	public static synchronized IpawsMain getInstance() {
		if (self == null) {
			self = new IpawsMain();
		}
		return self;
	}

	private IpawsMain() {

	}

	private void init() {
		if (viewSize == null) {
			viewSize = new Point(defaultWidth, defaultHeight);
		}

		init(viewSize);
	}

	private void init(Point viewSize) {
		logger.info("Paraclete/IPAWS UI version 1.0");
		
		String version = System.getProperty("java.version");
		logger.info("Java Version: " + version);

		loadDefaults();
		
		File errorFile = new File("./error.log");
		if (errorFile.exists()) {
			errorFile.delete();
		}

		Display display = new Display();
		guiUtil = GuiUtil.getInstance(display, res);
		shell = new Shell(display);
		shell.addListener(SWT.Close, (event) -> {
			logout();
			System.exit(0);
		});

		shell.setText(guiUtil.getMessage("ipawsTitle.title",
				com.interop.version.Version.getVersion()));

		if (!TurtleParacleteUtil.getInstance().hasMicrophone()) {
			// system has no microphone
			// guiUtil.popupErrorAndWait("No microphone is detected on this system. Please attach a microphone and restart.");
			final Shell dialog = new Shell(shell, SWT.APPLICATION_MODAL
					| SWT.DIALOG_TRIM);
			dialog.setText("No Microphone!");
			dialog.setLayout(new GridLayout(1, true));
			dialog.setSize(250, 200);

			final Label label = new Label(dialog, SWT.NONE);
			label.setText("No microphone is detected on this system.\nPlease attach a microphone and restart.");

			new Label(dialog, SWT.NONE);

			final Button okButton = new Button(dialog, SWT.PUSH);
			// okButton.setBounds(20, 35, 40, 25);
			okButton.setText("OK");
			okButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					shell.dispose();
					System.exit(1);
				}
			});
			dialog.addDisposeListener(new DisposeListener() {

				@Override
				public void widgetDisposed(DisposeEvent arg0) {
					shell.dispose();
					System.exit(1);
				}

			});

			dialog.pack();
			dialog.open();
			while (!dialog.isDisposed()) {
				if (!display.readAndDispatch())
					display.sleep();
			}

		}

		loadResource();

		shell.setLayout(new GridLayout(1, true));
		shell.setBackground(ColorUtil.getInstance().getBackgoundColor());
		shell.setBackgroundMode(SWT.INHERIT_DEFAULT);

		statusLabel = new Label(shell, SWT.NONE);
		statusLabel.setFont(smallFont);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalAlignment = GridData.FILL;
		statusLabel.setLayoutData(gd);
		statusLabel.setBackground(shell.getDisplay().getSystemColor(
				SWT.COLOR_DARK_GREEN));

		if (!loggedIn) {

			processLogin();
			while (!loggedin) {
				processLogin();
			}

			layoutScreen();
			shell.layout();
			shell.setSize(viewSize);
			shell.open();
			if (viewLocation != null) {
				guiUtil.setLocation(shell, viewLocation);
			} else {
				guiUtil.center(shell);
			}

			logger.info("User login and initialization completed.");

			while (!shell.isDisposed()) {

				if (!display.readAndDispatch())
					display.sleep();
			}

		}
	}

	private void layoutScreen() {
		// top toolbar
		Composite comp = new Composite(shell, SWT.None);
		comp.setBackground(shell.getDisplay().getSystemColor(
				SWT.COLOR_WIDGET_BACKGROUND));
		GridLayout compLayout = new GridLayout(2, false);
		comp.setLayout(compLayout);
		GridData gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalAlignment = GridData.FILL;
		comp.setLayoutData(gd);

		toolbarComp = new Composite(comp, SWT.None);
		toolbarComp.setBackground(shell.getDisplay().getSystemColor(
				SWT.COLOR_WIDGET_BACKGROUND));
		GridData gd0 = new GridData();
		gd0.grabExcessHorizontalSpace = false;
		gd0.horizontalAlignment = GridData.CENTER;
		toolbarComp.setLayoutData(gd0);

		RowLayout layout = new RowLayout();
		layout.spacing = 20;
		layout.justify = false;
		layout.center = true;
		layout.marginLeft = 30;
		toolbarComp.setLayout(layout);

		layoutToolbar(toolbarComp);

		// bottom area
		mainTab = new Composite(shell, SWT.NONE);
		GridData gd1 = new GridData(GridData.CENTER, GridData.FILL, true, true);
		mainTab.setBackground(red);
		mainTab.setLayoutData(gd1);
		// mainTab.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		mainTab.setLayout(new GridLayout(1, true));

	}

	private void layoutToolbar(Composite comp) {
		Composite parent = new Composite(comp, SWT.NONE);
		parent.setLayout(new GridLayout(1, true));

		Composite logoComp = new Composite(parent, SWT.NONE);
		GridData gd = new GridData();
		gd.horizontalAlignment = GridData.CENTER;
		logoComp.setLayoutData(gd);
		logoComp.setLayout(new RowLayout());

		Label label = new Label(logoComp, SWT.NONE);
		label.setText(guiUtil.getMessage("ipawsTitle.title"));
		label.setFont(superLargeFont);

		label = new Label(logoComp, SWT.None);
		label.setImage(appLogo);

		
	}

	
	private void logout() {
		// savePreferencesFromPromptToFile();
		loggedin = false;
		cleanupEverything();

		shell.setText(guiUtil.getMessage("ipawsTitle.title",
				com.interop.version.Version.getVersion()));

	}

	private void cleanupEverything() {
		shell.getDisplay().syncExec(() -> {

			red.dispose();
			green.dispose();
			yellowGreen.dispose();
			gray.dispose();
			black.dispose();
			handCursor.dispose();
			boldSmallFont.dispose();
			boldMediumFont.dispose();
			smallFont.dispose();
			mediumFont.dispose();
			largeFont.dispose();
			ColorUtil.getInstance(colorResourceXML).clear();

		});
	}

	/**
	 * @TODO
	 */
	private void processLogin() {
		Shell popup = new Shell(Display.getCurrent(), SWT.TOOL | SWT.APPLICATION_MODAL);
		GridLayout layout = new GridLayout(2, false);
		layout.horizontalSpacing = 20;
		layout.verticalSpacing = 20;
		layout.marginLeft = 10;
		layout.marginRight = 10;
		popup.setLayout(layout);
		
		Label label;
		
		label = new Label(popup, SWT.NONE);
		label.setText(guiUtil.getMessage("loginScreen.title"));
		label.setFont(boldMediumFont);
		GridData titleGD = new GridData();
		titleGD.horizontalSpan = 2;
		label.setLayoutData(titleGD);
		
		
		label = new Label(popup, SWT.NONE);
		label.setText(guiUtil.getMessage("userID.title"));
		Text userIDTxt = new Text(popup, SWT.BORDER);
		GridData userIDGD = new GridData();
		userIDGD.widthHint = 200;
		userIDTxt.setLayoutData(userIDGD);
		
		label = new Label(popup, SWT.NONE);
		label.setText(guiUtil.getMessage("password.title"));
		Text passwordTxt = new Text(popup, SWT.BORDER);
		passwordTxt.setEchoChar('*');
		GridData passwordGD = new GridData();
		passwordGD.widthHint = 200;
		passwordTxt.setLayoutData(passwordGD);
		
		Button cancel = new Button(popup, SWT.PUSH);
		cancel.setText(guiUtil.getMessage("cancelButton.title"));
		GridData cancelGD = new GridData();
		cancelGD.widthHint = 100;
		cancelGD.horizontalAlignment = GridData.CENTER;
		cancel.setLayoutData(cancelGD);
		cancel.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				shell.close();
			}
		});
		
		Button login = new Button(popup, SWT.PUSH);
		login.setText(guiUtil.getMessage("loginButton.title"));
		GridData loginGD = new GridData();
		loginGD.widthHint = 100;
		loginGD.horizontalAlignment = GridData.CENTER;
		login.setLayoutData(loginGD);
		login.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				try {
					api.login(userIDTxt.getText(), passwordTxt.getText());
				} catch (TurtleAPIException e) {
					e.printStackTrace();
				}
			}
		});
		
		popup.pack();
		guiUtil.centerToParent(popup, shell.getBounds());
		popup.open();

		loggedin = true;
	}

	private void loadResource() {
		InputStream is = shell.getClass().getResourceAsStream(
				"/com/interop/iris/iris_small.png");
		appLogo = new Image(shell.getDisplay(), is);
		shell.setImage(appLogo);

		handCursor = shell.getDisplay().getSystemCursor(SWT.CURSOR_HAND);
		FontData fontData = new FontData();
		fontData.setHeight(8);
		fontData.setStyle(SWT.BOLD);
		boldSmallFont = new Font(Display.getCurrent(), fontData);

		fontData.setHeight(10);
		boldMediumFont = new Font(Display.getCurrent(), fontData);

		fontData = new FontData();
		fontData.setHeight(8);
		smallFont = new Font(Display.getCurrent(), fontData);
		fontData = new FontData();
		fontData.setHeight(10);
		mediumFont = new Font(Display.getCurrent(), fontData);
		fontData = new FontData();
		fontData.setHeight(16);
		largeFont = new Font(Display.getCurrent(), fontData);

		fontData = new FontData();
		fontData.setHeight(40);
		superLargeFont = new Font(Display.getCurrent(), fontData);

		try {
			is = shell.getClass().getResourceAsStream(
					"/com/interop/iris/video.png");
			videoButtonImage = new Image(shell.getDisplay(), is);
			is.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			is = shell.getClass().getResourceAsStream(
					"/com/interop/iris/open_microphone.png");
			openMicrophoneImage = new Image(shell.getDisplay(), is);
			is.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			is = shell.getClass().getResourceAsStream(
					"/com/interop/iris/muted_microphone.png");
			mutedMicrophoneImage = new Image(shell.getDisplay(), is);
			is.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			is = shell.getClass().getResourceAsStream(
					"/com/interop/iris/microphone_muted.png");
			microphoneMutedImage = new Image(shell.getDisplay(), is);
			is.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			is = shell.getClass().getResourceAsStream(
					"/com/interop/iris/microphone_active.png");
			microphoneActiveImage = new Image(shell.getDisplay(), is);
			is.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		is = shell.getClass().getResourceAsStream(
				"/com/interop/iris/audio_volume.png");
		volumeImage = new Image(shell.getDisplay(), is);
		try {
			is.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		is = shell.getClass().getResourceAsStream(
				"/com/interop/iris/video_on.png");
		videoOnImage = new Image(shell.getDisplay(), is);
		try {
			is.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		is = shell.getClass().getResourceAsStream(
				"/com/interop/iris/video_off.png");
		videoOffImage = new Image(shell.getDisplay(), is);
		try {
			is.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		is = shell.getClass().getResourceAsStream("/com/interop/iris/more.png");
		moreImage = new Image(shell.getDisplay(), is);
		try {
			is.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			is = shell.getClass().getResourceAsStream(
					"/com/interop/iris/unplug_inactive.png");
			disconnectButtonInactiveImage = new Image(shell.getDisplay(), is);
			is.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		red = Display.getDefault().getSystemColor(SWT.COLOR_RED);
		green = Display.getDefault().getSystemColor(SWT.COLOR_GREEN);
		yellowGreen = ColorUtil.getInstance(colorResourceXML)
				.getAudioInBackgroundColor();
		gray = Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
		black = Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
	}

	private void loadDefaults() {
		serverURL = defaults.getString("server");
		userID = defaults.getString("userID");
		password = defaults.getString("password");
		
		if (serverURL.startsWith("http://")) {
			serverURL = serverURL.replace("http://", "https://");
		}
		else if (!serverURL.startsWith("https://")) {
			serverURL = "https://" + serverURL;
		}
		
		api = new IpawsAPI(serverURL, userID, password);
		
	}

	private void setStatus(String message) {
		System.out.println("STAUTS: " + message);

		shell.getDisplay().asyncExec(() -> {
			statusLabel.setText(message);
		});
	}


}
